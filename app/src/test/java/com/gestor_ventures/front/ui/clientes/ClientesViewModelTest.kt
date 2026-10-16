package com.gestor_ventures.front.ui.clientes

import com.gestor_ventures.back.model.ErrorCliente
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.ClienteRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.ClienteDaoFalso
import com.gestor_ventures.db.dao.NegocioDaoFalso
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.enums.TipoActividad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-29 y HU-33: registrar, corregir, borrar y buscar los clientes del negocio activo. */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 26, 10, 0)
    private val reloj = Reloj { ahora }

    private val clienteDao = ClienteDaoFalso()
    private val negocioDao = NegocioDaoFalso()

    private val repository = ClienteRepository(clienteDao)
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        sesionRepositoryDePrueba(),
    )

    private lateinit var viewModel: ClientesViewModel

    private val estado get() = viewModel.uiState.value

    @Before
    fun prepararPantalla() = runTest {
        Dispatchers.setMain(dispatcher)
        negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = ahora,
            ),
        )
        viewModel = ClientesViewModel(repository, negocioActivo)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private fun registrar(
        nombre: String,
        telefono: String = "",
        correo: String = "",
        notas: String = "",
    ) {
        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange(nombre)
        viewModel.onTelefonoChange(telefono)
        viewModel.onCorreoChange(correo)
        viewModel.onNotasChange(notas)
        viewModel.guardarFormulario()
    }

    @Test
    fun sinClientesLoDiceEnVezDeMostrarUnaListaEnBlanco() = runTest(dispatcher) {
        advanceUntilIdle()

        assertTrue(estado.vacio)
    }

    @Test
    fun registrarUnClienteLoDejaEnLaLista() = runTest(dispatcher) {
        advanceUntilIdle()

        registrar("Laura Gómez", telefono = "3001112233", notas = "Prefiere sin azúcar")
        advanceUntilIdle()

        val cliente = estado.clientes.single()
        assertEquals("Laura Gómez", cliente.nombre)
        assertEquals("3001112233", cliente.telefono)
        assertNull(estado.formulario)
    }

    @Test
    fun bastaConElNombreParaPoderGuardar() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Andrés")

        assertTrue(estado.formulario?.puedeGuardar ?: false)
    }

    @Test
    fun sinNombreNoSePuedeGuardar() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onTelefonoChange("3001112233")

        assertFalse(estado.formulario?.puedeGuardar ?: true)

        viewModel.guardarFormulario()
        advanceUntilIdle()
        assertTrue(estado.clientes.isEmpty())
    }

    @Test
    fun elTelefonoSeGuardaEnPurosDigitosAunqueSeEscribaConEspacios() = runTest(dispatcher) {
        advanceUntilIdle()

        registrar("Laura", telefono = "(300) 111-2233")
        advanceUntilIdle()

        assertEquals("3001112233", estado.clientes.single().telefono)
    }

    @Test
    fun unTelefonoRepetidoDejaLaHojaAbiertaConElAviso() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura", telefono = "3001112233")
        advanceUntilIdle()

        registrar("Laurita", telefono = "300 111 2233")
        advanceUntilIdle()

        // Lo escrito no se pierde: la hoja sigue abierta para poder corregirlo.
        assertEquals(ErrorCliente.TelefonoRepetido, estado.error)
        assertNotNull(estado.formulario)
        assertEquals(1, estado.clientes.size)
    }

    @Test
    fun tocarUnClienteAbreSusOpcionesYNoLoEditaDeUna() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura")
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.clientes.single())

        assertNotNull(estado.acciones)
        assertNull(estado.formulario)
    }

    @Test
    fun elFormularioDeEdicionLlegaConTodo() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura", telefono = "3001112233", correo = "laura@correo.com", notas = "Sin azúcar")
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.clientes.single())
        viewModel.editarElElegido()

        val formulario = estado.formulario
        assertNull(estado.acciones)
        assertEquals("Laura", formulario?.nombre)
        assertEquals("3001112233", formulario?.telefono)
        assertEquals("laura@correo.com", formulario?.correo)
        assertEquals("Sin azúcar", formulario?.notas)
    }

    @Test
    fun editarCorrigeLaFichaEnVezDeCrearOtra() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura", telefono = "3001112233")
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.clientes.single())
        viewModel.editarElElegido()
        viewModel.onNombreChange("Laura Gómez")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(1, estado.clientes.size)
        assertEquals("Laura Gómez", estado.clientes.single().nombre)
    }

    @Test
    fun editarSinTocarElTelefonoNoRebotaPorRepetido() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura", telefono = "3001112233")
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.clientes.single())
        viewModel.editarElElegido()
        viewModel.onNotasChange("Le gusta el chocolate")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertNull(estado.error)
        assertEquals("Le gusta el chocolate", estado.clientes.single().notas)
    }

    @Test
    fun eliminarLoSacaDeLaLista() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura")
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.clientes.single())
        viewModel.eliminarElElegido()
        advanceUntilIdle()

        assertTrue(estado.clientes.isEmpty())
        assertNull(estado.acciones)
    }

    @Test
    fun cerrarElFormularioNoDejaRastro() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Laura")
        viewModel.cerrarFormulario()

        assertNull(estado.formulario)
        viewModel.abrirFormularioNuevo()
        assertEquals("", estado.formulario?.nombre)
    }

    @Test
    fun laListaVaPorNombreSinImportarMayusculas() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("diego")
        advanceUntilIdle()
        registrar("Ana")
        advanceUntilIdle()
        registrar("camila")
        advanceUntilIdle()

        assertEquals(listOf("Ana", "camila", "diego"), estado.clientes.map { it.nombre })
    }

    // ---------- HU-33: buscar ----------

    @Test
    fun buscarPorNombreDejaSoloLosQueCoinciden() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura Gómez", telefono = "3001112233")
        advanceUntilIdle()
        registrar("Andrés Ruiz", telefono = "3109998877")
        advanceUntilIdle()

        viewModel.onBusquedaChange("laura")
        advanceUntilIdle()

        assertEquals("Laura Gómez", estado.clientes.single().nombre)
    }

    @Test
    fun buscarPorNumeroEncuentraAunqueSeEscribaConEspacios() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura Gómez", telefono = "3001112233")
        advanceUntilIdle()
        registrar("Andrés Ruiz", telefono = "3109998877")
        advanceUntilIdle()

        // El teléfono se guarda en puros dígitos; lo escrito se normaliza igual antes de buscar.
        viewModel.onBusquedaChange("300 111")
        advanceUntilIdle()

        assertEquals("Laura Gómez", estado.clientes.single().nombre)
    }

    @Test
    fun laBusquedaNoDistingueMayusculas() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura Gómez")
        advanceUntilIdle()

        viewModel.onBusquedaChange("GÓMEZ")
        advanceUntilIdle()

        assertEquals(1, estado.clientes.size)
    }

    @Test
    fun borrarLaBusquedaDevuelveLaListaCompleta() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura Gómez")
        advanceUntilIdle()
        registrar("Andrés Ruiz")
        advanceUntilIdle()
        viewModel.onBusquedaChange("laura")
        advanceUntilIdle()

        viewModel.onBusquedaChange("")
        advanceUntilIdle()

        // Un campo en blanco no es un filtro: esconder la lista sería esconder lo que existe.
        assertEquals(2, estado.clientes.size)
    }

    @Test
    fun sinCoincidenciasLoDiceEnVezDeParecerVacio() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura Gómez")
        advanceUntilIdle()

        viewModel.onBusquedaChange("zzz")
        advanceUntilIdle()

        // "No encontré a nadie" y "todavía no tienes clientes" son dos cosas distintas.
        assertTrue(estado.sinResultados)
        assertFalse(estado.vacio)
    }

    @Test
    fun elCampoSePintaAlInstanteSinEsperarLaConsulta() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onBusquedaChange("lau")

        assertEquals("lau", estado.busqueda)
    }

    // ---------- HU-31: WhatsApp ----------

    @Test
    fun elClienteConTelefonoTraeSuEnlaceDeWhatsApp() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Laura Gómez", telefono = "3001112233")
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.clientes.single())

        assertEquals("https://wa.me/573001112233", estado.whatsappDelElegido)
    }

    @Test
    fun elClienteSinTelefonoNoTraeEnlace() = runTest(dispatcher) {
        advanceUntilIdle()
        registrar("Camila Torres")
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.clientes.single())

        // Es lo que apaga el botón: sin teléfono no hay a quién escribirle.
        assertNull(estado.whatsappDelElegido)
    }

    @Test
    fun sinNadieElegidoNoHayEnlace() = runTest(dispatcher) {
        advanceUntilIdle()

        assertNull(estado.whatsappDelElegido)
    }
}
