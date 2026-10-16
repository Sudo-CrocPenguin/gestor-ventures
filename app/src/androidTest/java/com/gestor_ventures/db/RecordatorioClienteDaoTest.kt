package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.ClienteDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.dao.RecordatorioClienteDao
import com.gestor_ventures.db.entity.ClienteEntity
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.entity.RecordatorioClienteEntity
import com.gestor_ventures.db.enums.TipoActividad
import com.gestor_ventures.db.enums.TipoRecordatorioCliente
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-32. Los recordatorios que se le mandaron a un cliente.
 *
 * Lo que importa acá es que cada recordatorio quede pegado a su cliente y en orden: el
 * historial de lo que se le dijo a alguien no sirve si trae lo que se le dijo a otro.
 */
class RecordatorioClienteDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var clienteDao: ClienteDao
    private lateinit var recordatorioDao: RecordatorioClienteDao

    private val ahora: LocalDateTime = LocalDateTime.of(2026, 10, 16, 15, 0)
    private var clienteId: Long = 0
    private var otroClienteId: Long = 0

    @Before
    fun crearBaseDeDatos() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        clienteDao = db.clienteDao()
        recordatorioDao = db.recordatorioClienteDao()

        val negocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = ahora,
            ),
        )
        clienteId = clienteDao.insertar(
            ClienteEntity(negocioId = negocioId, nombre = "Ana Barrientos", telefono = "3001112233"),
        )
        otroClienteId = clienteDao.insertar(
            ClienteEntity(negocioId = negocioId, nombre = "Carlos Ruiz"),
        )
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    private fun recordatorio(
        mensaje: String,
        tipo: TipoRecordatorioCliente = TipoRecordatorioCliente.PAGO_PENDIENTE,
        cuando: LocalDateTime = ahora,
        cliente: Long = clienteId,
    ) = RecordatorioClienteEntity(
        clienteId = cliente,
        tipoRecordatorio = tipo,
        mensaje = mensaje,
        fechaEnvio = cuando,
    )

    @Test
    fun elRecordatorioSeGuardaConSuMensajeYSuTipo() = runTest {
        recordatorioDao.insertar(
            recordatorio("Hola Ana, te recuerdo tu pago pendiente."),
        )

        val guardado = recordatorioDao.observarDeCliente(clienteId).first().single()

        assertEquals("Hola Ana, te recuerdo tu pago pendiente.", guardado.mensaje)
        assertEquals(TipoRecordatorioCliente.PAGO_PENDIENTE, guardado.tipoRecordatorio)
        assertEquals(ahora, guardado.fechaEnvio)
    }

    @Test
    fun seGuardaElMensajeCorregidoYNoLaPlantilla() = runTest {
        // HU-32: el usuario puede editar antes de enviar, y lo que queda es lo que de verdad dijo.
        recordatorioDao.insertar(recordatorio("Anita, pásate cuando puedas 🙂"))

        assertEquals(
            "Anita, pásate cuando puedas 🙂",
            recordatorioDao.observarDeCliente(clienteId).first().single().mensaje,
        )
    }

    @Test
    fun losDeUnClienteNoTraenLosDeOtro() = runTest {
        recordatorioDao.insertar(recordatorio("Para Ana"))
        recordatorioDao.insertar(recordatorio("Para Carlos", cliente = otroClienteId))

        assertEquals(
            "Para Ana",
            recordatorioDao.observarDeCliente(clienteId).first().single().mensaje,
        )
    }

    @Test
    fun elHistorialVaDelMasRecienteAlMasAntiguo() = runTest {
        recordatorioDao.insertar(recordatorio("El viejo", cuando = ahora.minusDays(3)))
        recordatorioDao.insertar(recordatorio("El nuevo", cuando = ahora))

        assertEquals(
            listOf("El nuevo", "El viejo"),
            recordatorioDao.observarDeCliente(clienteId).first().map { it.mensaje },
        )
    }

    @Test
    fun losDosTiposSeGuardanIgual() = runTest {
        recordatorioDao.insertar(recordatorio("De cita", tipo = TipoRecordatorioCliente.CITA))
        recordatorioDao.insertar(
            recordatorio("De pago", tipo = TipoRecordatorioCliente.PAGO_PENDIENTE),
        )

        val tipos = recordatorioDao.observarDeCliente(clienteId).first().map { it.tipoRecordatorio }
        assertTrue(tipos.containsAll(TipoRecordatorioCliente.entries))
    }

    @Test
    fun unClienteSinRecordatoriosTraeLaListaVacia() = runTest {
        assertTrue(recordatorioDao.observarDeCliente(otroClienteId).first().isEmpty())
    }

    @Test
    fun borrarAlClienteSeLlevaSusRecordatorios() = runTest {
        recordatorioDao.insertar(recordatorio("Para Ana"))

        clienteDao.eliminar(clienteDao.obtener(clienteId)!!)

        // La llave foránea va en cascada: no tiene sentido guardar lo que se le dijo a alguien
        // que ya no está en la agenda del negocio.
        assertTrue(recordatorioDao.observarDeCliente(clienteId).first().isEmpty())
    }
}
