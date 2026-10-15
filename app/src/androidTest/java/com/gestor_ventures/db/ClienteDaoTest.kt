package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.ClienteDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.entity.ClienteEntity
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.enums.TipoActividad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-29. Clientes contra una base de datos real en memoria.
 *
 * Lo delicado no es guardar un nombre: es que cada negocio vea solo los suyos, que no se puedan
 * repetir teléfonos o correos dentro del mismo negocio, y que un cliente sin datos de contacto
 * —que es lo más común— no choque con otro igual de incompleto.
 */
class ClienteDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var clienteDao: ClienteDao

    private val ahora: LocalDateTime = LocalDateTime.of(2026, 9, 26, 10, 0)
    private var negocioId: Long = 0

    @Before
    fun crearBaseDeDatos() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        clienteDao = db.clienteDao()

        negocioId = negocioDao.insertar(negocio("Dulce Antojo"))
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    @Test
    fun elClienteSeGuardaConTodosSusDatos() = runTest {
        val id = clienteDao.insertar(
            cliente("Laura Gómez", telefono = "3001112233", correo = "laura@correo.com", notas = "Prefiere sin azúcar"),
        )

        val guardado = clienteDao.obtener(id)

        assertEquals("Laura Gómez", guardado?.nombre)
        assertEquals("3001112233", guardado?.telefono)
        assertEquals("laura@correo.com", guardado?.correo)
        assertEquals("Prefiere sin azúcar", guardado?.notas)
    }

    @Test
    fun unClienteSoloConNombreEsValido() = runTest {
        val id = clienteDao.insertar(cliente("Andrés"))

        val guardado = clienteDao.obtener(id)

        // A mucha gente se le vende sin pedirle nada más que el nombre.
        assertEquals("Andrés", guardado?.nombre)
        assertNull(guardado?.telefono)
        assertNull(guardado?.correo)
    }

    @Test
    fun variosClientesSinContactoNoChocanEntreSi() = runTest {
        clienteDao.insertar(cliente("Andrés"))
        clienteDao.insertar(cliente("Camila"))
        clienteDao.insertar(cliente("Diego"))

        // SQLite trata cada NULL como distinto, así que el índice único no los confunde.
        assertEquals(3, clienteDao.observarDeNegocio(negocioId).first().size)
    }

    @Test
    fun laListaVaPorNombreSinImportarMayusculas() = runTest {
        clienteDao.insertar(cliente("diego"))
        clienteDao.insertar(cliente("Ana"))
        clienteDao.insertar(cliente("camila"))

        val nombres = clienteDao.observarDeNegocio(negocioId).first().map { it.nombre }

        assertEquals(listOf("Ana", "camila", "diego"), nombres)
    }

    @Test
    fun cadaNegocioVeSoloSusClientes() = runTest {
        val otroNegocio = negocioDao.insertar(negocio("Bella Piel"))
        clienteDao.insertar(cliente("Laura"))
        clienteDao.insertar(cliente("Valentina", negocio = otroNegocio))

        assertEquals(listOf("Laura"), clienteDao.observarDeNegocio(negocioId).first().map { it.nombre })
        assertEquals(listOf("Valentina"), clienteDao.observarDeNegocio(otroNegocio).first().map { it.nombre })
    }

    @Test
    fun elTelefonoRepetidoSeDetectaAntesDeGuardar() = runTest {
        clienteDao.insertar(cliente("Laura", telefono = "3001112233"))

        assertEquals(1, clienteDao.existeConTelefono(negocioId, "3001112233"))
        assertEquals(0, clienteDao.existeConTelefono(negocioId, "3009998877"))
    }

    @Test
    fun elMismoTelefonoEnOtroNegocioNoEstaRepetido() = runTest {
        val otroNegocio = negocioDao.insertar(negocio("Bella Piel"))
        clienteDao.insertar(cliente("Laura", telefono = "3001112233"))

        // La misma persona puede ser cliente de los dos negocios: son dos fichas distintas.
        assertEquals(0, clienteDao.existeConTelefono(otroNegocio, "3001112233"))
    }

    @Test
    fun elCorreoRepetidoNoDependeDeLasMayusculas() = runTest {
        clienteDao.insertar(cliente("Laura", correo = "Laura@Correo.com"))

        assertEquals(1, clienteDao.existeConCorreo(negocioId, "laura@correo.com"))
    }

    @Test
    fun editarUnClienteNoLoDejaChocarConsigoMismo() = runTest {
        val id = clienteDao.insertar(cliente("Laura", telefono = "3001112233"))

        // Corregirle las notas sin cambiarle el teléfono no puede contar como duplicado.
        assertEquals(0, clienteDao.existeConTelefono(negocioId, "3001112233", exceptoId = id))
    }

    @Test
    fun actualizarCambiaLosDatosYNoCreaOtraFicha() = runTest {
        val id = clienteDao.insertar(cliente("Laura", telefono = "3001112233"))
        val guardado = clienteDao.obtener(id)!!

        clienteDao.actualizar(guardado.copy(telefono = "3009998877", notas = "Cambió de número"))

        assertEquals(1, clienteDao.observarDeNegocio(negocioId).first().size)
        assertEquals("3009998877", clienteDao.obtener(id)?.telefono)
    }

    @Test
    fun eliminarSacaAlClienteDeLaLista() = runTest {
        val id = clienteDao.insertar(cliente("Laura"))
        val guardado = clienteDao.obtener(id)!!

        clienteDao.eliminar(guardado)

        assertNull(clienteDao.obtener(id))
        assertEquals(0, clienteDao.observarDeNegocio(negocioId).first().size)
    }

    private fun cliente(
        nombre: String,
        telefono: String? = null,
        correo: String? = null,
        notas: String? = null,
        negocio: Long = negocioId,
    ) = ClienteEntity(
        negocioId = negocio,
        nombre = nombre,
        telefono = telefono,
        correo = correo,
        notas = notas,
    )

    private fun negocio(nombre: String) = NegocioEntity(
        usuarioId = SemillaTemporal.USUARIO_ID,
        nombreNegocio = nombre,
        tipoActividad = TipoActividad.PRODUCTOS,
        categoriaNegocio = "Repostería",
        porcentajeReinversion = 0.0,
        fechaCreacion = ahora,
    )
}
