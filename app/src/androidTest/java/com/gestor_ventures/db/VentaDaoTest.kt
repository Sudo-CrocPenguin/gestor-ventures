package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.dao.VentaDao
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.entity.VentaEntity
import com.gestor_ventures.db.enums.MetodoPago
import com.gestor_ventures.db.enums.TipoActividad
import com.gestor_ventures.db.enums.TipoRegistroVenta
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-11 y HU-12. Las ventas contra una base de datos real en memoria.
 *
 * Lo que de verdad se prueba acá es el filtro por día: las fechas se guardan como milisegundos,
 * así que un error de un milisegundo en los bordes dejaría la primera o la última venta del día
 * por fuera del resumen.
 */
class VentaDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var ventaDao: VentaDao

    private val hoy: LocalDate = LocalDate.of(2026, 9, 16)
    private val desde: LocalDateTime = hoy.atStartOfDay()
    private val hasta: LocalDateTime = hoy.atTime(23, 59, 59)

    private var negocioId: Long = 0

    @Before
    fun crearBaseDeDatos() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        ventaDao = db.ventaDao()

        negocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = desde,
            ),
        )
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    @Test
    fun laVentaDetalladaSeGuardaConTodosSusDatos() = runTest {
        val id = ventaDao.insertar(venta(monto = 25_000.0, hora = 10))

        val guardada = ventaDao.obtener(id)

        assertEquals(25_000.0, guardada?.monto)
        assertEquals("Torta de chocolate", guardada?.productoServicio)
        assertEquals(MetodoPago.EFECTIVO, guardada?.metodoPago)
        assertEquals(TipoRegistroVenta.DETALLADO, guardada?.tipoRegistro)
        assertEquals(hoy.atTime(10, 0), guardada?.fechaHora)
    }

    @Test
    fun laVentaRapidaSoloGuardaMontoYFecha() = runTest {
        val id = ventaDao.insertar(
            VentaEntity(
                negocioId = negocioId,
                tipoRegistro = TipoRegistroVenta.RAPIDO,
                monto = 8_000.0,
                fechaHora = hoy.atTime(11, 30),
            ),
        )

        val guardada = ventaDao.obtener(id)

        assertEquals(8_000.0, guardada?.monto)
        assertNull(guardada?.productoServicio)
        assertNull(guardada?.metodoPago)
        assertNull(guardada?.clienteId)
    }

    @Test
    fun soloSalenLasVentasDelRangoPedido() = runTest {
        ventaDao.insertar(venta(monto = 10_000.0, hora = 9))
        ventaDao.insertar(venta(monto = 20_000.0, fecha = hoy.minusDays(1)))
        ventaDao.insertar(venta(monto = 30_000.0, fecha = hoy.plusDays(1)))

        val delDia = ventaDao.observarEntre(negocioId, desde, hasta).first()

        assertEquals(1, delDia.size)
        assertEquals(10_000.0, delDia.first().monto, 0.001)
    }

    @Test
    fun laPrimeraYLaUltimaVentaDelDiaCuentan() = runTest {
        ventaDao.insertar(venta(monto = 5_000.0, fechaHora = hoy.atStartOfDay()))
        ventaDao.insertar(venta(monto = 7_000.0, fechaHora = hoy.atTime(23, 59, 59)))

        val total = ventaDao.observarTotalEntre(negocioId, desde, hasta).first()

        assertEquals(12_000.0, total, 0.001)
    }

    @Test
    fun lasVentasSalenDeLaMasRecienteALaMasAntigua() = runTest {
        ventaDao.insertar(venta(monto = 10_000.0, hora = 8))
        ventaDao.insertar(venta(monto = 30_000.0, hora = 18))
        ventaDao.insertar(venta(monto = 20_000.0, hora = 13))

        val delDia = ventaDao.observarEntre(negocioId, desde, hasta).first()

        assertEquals(listOf(30_000.0, 20_000.0, 10_000.0), delDia.map { it.monto })
    }

    @Test
    fun dosVentasDelMismoMinutoNoSalenEnCualquierOrden() = runTest {
        val primera = ventaDao.insertar(venta(monto = 10_000.0, hora = 15))
        val segunda = ventaDao.insertar(venta(monto = 20_000.0, hora = 15))

        val delDia = ventaDao.observarEntre(negocioId, desde, hasta).first()

        // La última registrada va de primera: es la que el usuario acaba de guardar.
        assertEquals(listOf(segunda, primera), delDia.map { it.ventaId })
    }

    @Test
    fun unDiaSinVentasTotalizaCeroYNoNulo() = runTest {
        val total = ventaDao.observarTotalEntre(negocioId, desde, hasta).first()

        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun elTotalYElConteoIgnoranLasVentasDeOtroNegocio() = runTest {
        val otroNegocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Otro negocio",
                tipoActividad = TipoActividad.SERVICIOS,
                categoriaNegocio = "Belleza",
                porcentajeReinversion = 0.0,
                fechaCreacion = desde,
            ),
        )
        ventaDao.insertar(venta(monto = 10_000.0, hora = 9))
        ventaDao.insertar(venta(monto = 99_000.0, hora = 9, negocio = otroNegocioId))

        assertEquals(10_000.0, ventaDao.observarTotalEntre(negocioId, desde, hasta).first(), 0.001)
        assertEquals(1, ventaDao.contarEntre(negocioId, desde, hasta).first())
    }

    private fun venta(
        monto: Double,
        hora: Int = 12,
        fecha: LocalDate = hoy,
        fechaHora: LocalDateTime = fecha.atTime(hora, 0),
        negocio: Long = negocioId,
    ) = VentaEntity(
        negocioId = negocio,
        tipoRegistro = TipoRegistroVenta.DETALLADO,
        productoServicio = "Torta de chocolate",
        monto = monto,
        metodoPago = MetodoPago.EFECTIVO,
        fechaHora = fechaHora,
    )
}
