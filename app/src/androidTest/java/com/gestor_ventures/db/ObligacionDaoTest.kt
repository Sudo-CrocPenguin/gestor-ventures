package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.dao.ObligacionDao
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.entity.ObligacionEntity
import com.gestor_ventures.db.enums.EstadoPago
import com.gestor_ventures.db.enums.TipoActividad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-07. Obligaciones contra una base de datos real en memoria.
 *
 * Lo que se prueba acá es el orden y el filtro: una deuda importa por cuándo vence, y una
 * vencida es la más urgente de todas.
 */
class ObligacionDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var obligacionDao: ObligacionDao

    private val hoy: LocalDate = LocalDate.of(2026, 9, 16)
    private val ahora: LocalDateTime = hoy.atTime(10, 0)

    private var negocioId: Long = 0

    @Before
    fun crearBaseDeDatos() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        obligacionDao = db.obligacionDao()

        negocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = ahora,
            ),
        )
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    @Test
    fun laObligacionSeGuardaPendienteConSuVencimiento() = runTest {
        val id = obligacionDao.insertar(obligacion("Cuota del horno", 250_000.0, hoy.plusDays(10)))

        val guardada = obligacionDao.obtener(id)

        assertEquals("Cuota del horno", guardada?.nombreObligacion)
        assertEquals(250_000.0, guardada?.monto)
        assertEquals(hoy.plusDays(10), guardada?.fechaVencimiento)
        assertEquals(EstadoPago.PENDIENTE, guardada?.estadoPago)
    }

    @Test
    fun lasObligacionesSalenPorLaQueVencePrimero() = runTest {
        obligacionDao.insertar(obligacion("Tercera", 10_000.0, hoy.plusDays(30)))
        obligacionDao.insertar(obligacion("Primera", 20_000.0, hoy.plusDays(2)))
        obligacionDao.insertar(obligacion("Segunda", 15_000.0, hoy.plusDays(10)))

        val nombres = obligacionDao.observarDeNegocio(negocioId).first().map { it.nombreObligacion }

        assertEquals(listOf("Primera", "Segunda", "Tercera"), nombres)
    }

    @Test
    fun lasPagadasQuedanAbajoAunqueVenzanAntes() = runTest {
        obligacionDao.insertar(
            obligacion("Ya pagada", 10_000.0, hoy.minusDays(5), EstadoPago.PAGADA),
        )
        obligacionDao.insertar(obligacion("Pendiente", 20_000.0, hoy.plusDays(20)))

        val nombres = obligacionDao.observarDeNegocio(negocioId).first().map { it.nombreObligacion }

        assertEquals(listOf("Pendiente", "Ya pagada"), nombres)
    }

    @Test
    fun lasProximasAVencerIncluyenLasYaVencidas() = runTest {
        obligacionDao.insertar(obligacion("Vencida", 30_000.0, hoy.minusDays(3)))
        obligacionDao.insertar(obligacion("Esta semana", 20_000.0, hoy.plusDays(4)))
        obligacionDao.insertar(obligacion("El otro mes", 50_000.0, hoy.plusDays(45)))

        val proximas = obligacionDao
            .observarProximasAVencer(negocioId, hoy.plusDays(7))
            .first()

        // La vencida va de primera: es la más urgente, no una que haya que esconder.
        assertEquals(listOf("Vencida", "Esta semana"), proximas.map { it.nombreObligacion })
    }

    @Test
    fun lasProximasAVencerIgnoranLasYaPagadas() = runTest {
        obligacionDao.insertar(obligacion("Pagada", 30_000.0, hoy.plusDays(2), EstadoPago.PAGADA))
        obligacionDao.insertar(obligacion("Pendiente", 20_000.0, hoy.plusDays(3)))

        val proximas = obligacionDao.observarProximasAVencer(negocioId, hoy.plusDays(7)).first()

        assertEquals(listOf("Pendiente"), proximas.map { it.nombreObligacion })
    }

    @Test
    fun elTotalPendienteNoCuentaLoQueYaSePago() = runTest {
        obligacionDao.insertar(obligacion("Cuota", 250_000.0, hoy.plusDays(10)))
        obligacionDao.insertar(obligacion("Préstamo", 100_000.0, hoy.plusDays(20)))
        obligacionDao.insertar(obligacion("Pagada", 80_000.0, hoy.minusDays(5), EstadoPago.PAGADA))

        assertEquals(350_000.0, obligacionDao.observarTotalPendiente(negocioId).first(), 0.001)
    }

    @Test
    fun sinObligacionesElTotalEsCeroYNoNulo() = runTest {
        assertEquals(0.0, obligacionDao.observarTotalPendiente(negocioId).first(), 0.001)
    }

    @Test
    fun marcarComoPagadaLaSacaDeLoPendiente() = runTest {
        val id = obligacionDao.insertar(obligacion("Cuota", 250_000.0, hoy.plusDays(10)))

        obligacionDao.actualizar(obligacionDao.obtener(id)!!.copy(estadoPago = EstadoPago.PAGADA))

        assertEquals(0.0, obligacionDao.observarTotalPendiente(negocioId).first(), 0.001)
        assertEquals(EstadoPago.PAGADA, obligacionDao.obtener(id)?.estadoPago)
    }

    @Test
    fun cadaNegocioVeSoloSusObligaciones() = runTest {
        val otroNegocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Bella Piel",
                tipoActividad = TipoActividad.SERVICIOS,
                categoriaNegocio = "Belleza",
                porcentajeReinversion = 0.0,
                fechaCreacion = ahora,
            ),
        )
        obligacionDao.insertar(obligacion("Mía", 250_000.0, hoy.plusDays(10)))
        obligacionDao.insertar(
            obligacion("Ajena", 999_000.0, hoy.plusDays(10), negocio = otroNegocioId),
        )

        assertEquals(250_000.0, obligacionDao.observarTotalPendiente(negocioId).first(), 0.001)
    }

    private fun obligacion(
        nombre: String,
        monto: Double,
        vencimiento: LocalDate,
        estado: EstadoPago = EstadoPago.PENDIENTE,
        negocio: Long = negocioId,
    ) = ObligacionEntity(
        negocioId = negocio,
        nombreObligacion = nombre,
        monto = monto,
        fechaVencimiento = vencimiento,
        estadoPago = estado,
        fechaRegistro = ahora,
    )
}
