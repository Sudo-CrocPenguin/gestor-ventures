package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorCliente
import com.gestor_ventures.db.dao.ClienteDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HU-29. Reglas de un cliente antes de que llegue a la base de datos.
 *
 * Lo que de verdad se fija acá: que el teléfono se guarde siempre igual aunque se escriba de
 * formas distintas, y que no queden dos fichas de la misma persona en un mismo negocio.
 */
class ClienteRepositoryTest {

    private val dao = ClienteDaoFalso()
    private val repository = ClienteRepository(dao)

    private val negocioId = 1L
    private val otroNegocio = 2L

    private suspend fun clientes() = repository.clientesDeNegocio(negocioId).first()

    @Test
    fun registraAlClienteConTodosSusDatos() = runTest {
        val error = repository.crearCliente(
            negocioId = negocioId,
            nombre = "Laura Gómez",
            telefono = "3001112233",
            correo = "laura@correo.com",
            notas = "Prefiere sin azúcar",
        )

        assertNull(error)
        val cliente = clientes().single()
        assertEquals("Laura Gómez", cliente.nombre)
        assertEquals("3001112233", cliente.telefono)
        assertEquals("laura@correo.com", cliente.correo)
        assertEquals("Prefiere sin azúcar", cliente.notas)
    }

    @Test
    fun bastaConElNombre() = runTest {
        assertNull(repository.crearCliente(negocioId, "Andrés"))

        val cliente = clientes().single()
        assertNull(cliente.telefono)
        assertNull(cliente.correo)
        assertTrue(!cliente.tieneContacto)
    }

    @Test
    fun elNombreNoPuedeIrEnBlanco() = runTest {
        assertEquals(ErrorCliente.NombreVacio, repository.crearCliente(negocioId, "   "))
        assertTrue(clientes().isEmpty())
    }

    @Test
    fun elNombreTieneTope() = runTest {
        assertEquals(
            ErrorCliente.NombreMuyLargo,
            repository.crearCliente(negocioId, "a".repeat(51)),
        )
    }

    @Test
    fun elTelefonoSeGuardaEnPurosDigitos() = runTest {
        repository.crearCliente(negocioId, "Laura", telefono = "(300) 111-2233")

        // Si se guardara tal cual, el mismo numero escrito de otra forma seria otro cliente.
        assertEquals("3001112233", clientes().single().telefono)
    }

    @Test
    fun elMismoTelefonoEscritoDistintoSeDetectaComoRepetido() = runTest {
        repository.crearCliente(negocioId, "Laura", telefono = "3001112233")

        val error = repository.crearCliente(negocioId, "Laurita", telefono = "300 111 2233")

        assertEquals(ErrorCliente.TelefonoRepetido, error)
        assertEquals(1, clientes().size)
    }

    @Test
    fun unTelefonoDemasiadoCortoNoEsUnTelefono() = runTest {
        assertEquals(
            ErrorCliente.TelefonoInvalido,
            repository.crearCliente(negocioId, "Laura", telefono = "12345"),
        )
    }

    @Test
    fun elCorreoTieneQueParecerUnCorreo() = runTest {
        assertEquals(
            ErrorCliente.CorreoInvalido,
            repository.crearCliente(negocioId, "Laura", correo = "laura.correo"),
        )
    }

    @Test
    fun elCorreoSeGuardaEnMinusculasYNoSeRepite() = runTest {
        repository.crearCliente(negocioId, "Laura", correo = "Laura@Correo.com")

        assertEquals("laura@correo.com", clientes().single().correo)
        assertEquals(
            ErrorCliente.CorreoRepetido,
            repository.crearCliente(negocioId, "Otra", correo = "laura@correo.com"),
        )
    }

    @Test
    fun elMismoContactoEnOtroNegocioNoEstaRepetido() = runTest {
        repository.crearCliente(negocioId, "Laura", telefono = "3001112233")

        // La misma persona puede comprarle a los dos negocios: son dos fichas distintas.
        assertNull(repository.crearCliente(otroNegocio, "Laura", telefono = "3001112233"))
    }

    @Test
    fun editarCorrigeLaFichaYNoCreaOtra() = runTest {
        repository.crearCliente(negocioId, "Laura", telefono = "3001112233")
        val id = clientes().single().id

        val error = repository.editarCliente(
            clienteId = id,
            nombre = "Laura Gómez",
            telefono = "3009998877",
            notas = "Cambió de número",
        )

        assertNull(error)
        val cliente = clientes().single()
        assertEquals("Laura Gómez", cliente.nombre)
        assertEquals("3009998877", cliente.telefono)
    }

    @Test
    fun editarSinCambiarElTelefonoNoChocaConsigoMismo() = runTest {
        repository.crearCliente(negocioId, "Laura", telefono = "3001112233")
        val id = clientes().single().id

        val error = repository.editarCliente(
            clienteId = id,
            nombre = "Laura",
            telefono = "3001112233",
            notas = "Le gusta el chocolate",
        )

        assertNull(error)
        assertEquals("Le gusta el chocolate", clientes().single().notas)
    }

    @Test
    fun editarNoDejaQuedarseConElTelefonoDeOtro() = runTest {
        repository.crearCliente(negocioId, "Laura", telefono = "3001112233")
        repository.crearCliente(negocioId, "Andrés", telefono = "3009998877")
        val andres = clientes().first { it.nombre == "Andrés" }

        val error = repository.editarCliente(andres.id, "Andrés", telefono = "3001112233")

        assertEquals(ErrorCliente.TelefonoRepetido, error)
        assertEquals("3009998877", repository.obtenerCliente(andres.id)?.telefono)
    }

    @Test
    fun editarUnClienteQueYaNoExisteNoRompeNada() = runTest {
        assertNull(repository.editarCliente(99L, "Fantasma"))
        assertTrue(clientes().isEmpty())
    }

    @Test
    fun unCampoEnBlancoSeGuardaComoNoMeLoDio() = runTest {
        repository.crearCliente(negocioId, "Laura", telefono = "  ", correo = "", notas = "  ")

        // Nulo, no cadena vacía: "no me lo dio" y "me dio nada" tienen que ser lo mismo.
        val cliente = clientes().single()
        assertNull(cliente.telefono)
        assertNull(cliente.correo)
        assertNull(cliente.notas)
    }

    @Test
    fun laListaVaPorNombreSinImportarMayusculas() = runTest {
        repository.crearCliente(negocioId, "diego")
        repository.crearCliente(negocioId, "Ana")
        repository.crearCliente(negocioId, "camila")

        assertEquals(listOf("Ana", "camila", "diego"), clientes().map { it.nombre })
    }

    @Test
    fun eliminarSacaAlClienteDeLaLista() = runTest {
        repository.crearCliente(negocioId, "Laura")
        val id = clientes().single().id

        repository.eliminarCliente(id)

        assertTrue(clientes().isEmpty())
        assertNull(repository.obtenerCliente(id))
    }
}
