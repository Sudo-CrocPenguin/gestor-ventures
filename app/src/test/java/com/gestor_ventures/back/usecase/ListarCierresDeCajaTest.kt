package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.RangoFechas
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.model.rango
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.db.dao.CajaDaoFalso
import com.gestor_ventures.db.dao.CostoDaoFalso
import com.gestor_ventures.db.dao.GastoDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-23. El historial de jornadas cerradas.
 *
 * Lo que de verdad se prueba acá es que la diferencia de cada jornada se vuelva a calcular con
 * sus propios movimientos: si se mezclaran los de dos turnos, el historial señalaría descuadres
 * donde no los hubo.
 */
class ListarCierresDeCajaTest {

    // Jueves 15 de octubre de 2026.
    private var momento = LocalDateTime.of(2026, 10, 15, 8, 0)
    private val reloj = Reloj { momento }

    private val cajaDao = CajaDaoFalso()
    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()

    private val cajaRepository = CajaRepository(cajaDao, reloj)
    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)

    private val listarCierres = ListarCierresDeCaja(
        cajaRepository = cajaRepository,
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = costoRepository,
        reloj = reloj,
    )

    private val negocioId = 1L
    private val otroNegocio = 2L

    private val delMes: RangoFechas
        get() = PeriodoPredefinido.Mes.rango(listarCierres.hoy())

    private suspend fun cierres(rango: RangoFechas = delMes) =
        listarCierres(negocioId, rango).first()

    /**
     * Registra una venta a la hora en que ocurrió, moviendo el reloj mientras tanto.
     *
     * Sin eso, una venta de las 11:00 registrada con el reloj a las 8:00 la rechaza la propia
     * validación de HU-11: nadie puede registrar algo que todavía no pasó.
     */
    private suspend fun vender(monto: Double, aLas: LocalDateTime, negocio: Long = negocioId) {
        enElMomento(aLas) {
            ventaRepository.registrarVenta(
                negocioId = negocio,
                tipoRegistro = TipoRegistroVenta.DETALLADO,
                monto = monto,
                fechaHora = aLas,
                productoServicio = "Torta",
                metodoPago = MetodoPago.EFECTIVO,
            )
        }
    }

    /** Corre el reloj a [instante], hace algo, y lo devuelve a donde estaba. */
    private suspend fun <T> enElMomento(instante: LocalDateTime, accion: suspend () -> T): T {
        val volverA = momento
        momento = instante
        return try {
            accion()
        } finally {
            momento = volverA
        }
    }

    /**
     * Abre y cierra una jornada completa, moviendo el reloj como lo haría el día real.
     *
     * Al terminar lo devuelve a donde estaba: si lo dejara en el pasado, el periodo "Hoy" se
     * resolvería contra la última jornada creada y no contra el día en que se está parado.
     */
    private suspend fun jornada(
        montoInicial: Double,
        abreA: LocalDateTime,
        cierraA: LocalDateTime,
        montoReal: Double,
        nota: String? = null,
        negocio: Long = negocioId,
    ) {
        enElMomento(abreA) { cajaRepository.abrirCaja(negocio, montoInicial) }
        enElMomento(cierraA) { cajaRepository.cerrarCaja(negocio, montoReal, nota) }
    }

    @Test
    fun sinJornadasCerradasElHistorialVieneVacio() = runTest {
        assertTrue(cierres().isEmpty())
    }

    @Test
    fun unaJornadaQueCuadroSaleSinDiferencia() = runTest {
        val dia = momento
        jornada(
            montoInicial = 50_000.0,
            abreA = dia.withHour(8),
            cierraA = dia.withHour(14),
            montoReal = 75_000.0,
        )
        vender(25_000.0, aLas = dia.withHour(11))

        val cierre = cierres().single()

        assertEquals(75_000.0, cierre.esperado, 0.001)
        assertTrue(cierre.diferencia.cuadra)
    }

    @Test
    fun siSeContoDeMenosLaJornadaQuedaConFaltante() = runTest {
        val dia = momento
        jornada(
            montoInicial = 50_000.0,
            abreA = dia.withHour(8),
            cierraA = dia.withHour(14),
            montoReal = 73_500.0,
        )
        vender(25_000.0, aLas = dia.withHour(11))

        val cierre = cierres().single()

        assertEquals(-1_500.0, cierre.diferencia.diferencia, 0.001)
        assertTrue(cierre.diferencia.falta)
    }

    @Test
    fun cadaJornadaCuentaSoloSusPropiosMovimientos() = runTest {
        val dia = momento
        jornada(50_000.0, dia.withHour(8), dia.withHour(13), montoReal = 70_000.0)
        vender(20_000.0, aLas = dia.withHour(10))

        jornada(30_000.0, dia.withHour(14), dia.withHour(20), montoReal = 35_000.0)
        vender(5_000.0, aLas = dia.withHour(16))

        val historial = cierres()

        // Si los dos turnos se mezclaran, el de la mañana reclamaría la venta de la tarde y el
        // historial mostraría descuadres en dos jornadas que en realidad cuadraron.
        assertEquals(2, historial.size)
        assertTrue(historial.all { it.diferencia.cuadra })
    }

    @Test
    fun elHistorialVaDeLaMasRecienteALaMasAntigua() = runTest {
        val dia = momento
        jornada(10_000.0, dia.minusDays(2).withHour(8), dia.minusDays(2).withHour(14), 10_000.0)
        jornada(20_000.0, dia.minusDays(1).withHour(8), dia.minusDays(1).withHour(14), 20_000.0)
        jornada(30_000.0, dia.withHour(8), dia.withHour(14), 30_000.0)

        val historial = cierres()

        assertEquals(
            listOf(30_000.0, 20_000.0, 10_000.0),
            historial.map { it.caja.montoInicial },
        )
    }

    @Test
    fun filtrarPorPeriodoDejaFueraLoDeOtrosDias() = runTest {
        val dia = momento
        jornada(50_000.0, dia.withHour(8), dia.withHour(14), 50_000.0)
        jornada(20_000.0, dia.minusDays(5).withHour(8), dia.minusDays(5).withHour(14), 20_000.0)

        val deHoy = cierres(PeriodoPredefinido.Hoy.rango(listarCierres.hoy()))

        assertEquals(1, deHoy.size)
        assertEquals(50_000.0, deHoy.single().caja.montoInicial, 0.001)
    }

    @Test
    fun elHistorialDeUnNegocioNoTraeElDeOtro() = runTest {
        val dia = momento
        jornada(50_000.0, dia.withHour(8), dia.withHour(14), 50_000.0)
        jornada(30_000.0, dia.withHour(8), dia.withHour(14), 30_000.0, negocio = otroNegocio)

        assertEquals(50_000.0, cierres().single().caja.montoInicial, 0.001)
    }

    @Test
    fun laNotaDelCierreLlegaAlHistorial() = runTest {
        val dia = momento
        jornada(
            montoInicial = 50_000.0,
            abreA = dia.withHour(8),
            cierraA = dia.withHour(14),
            montoReal = 48_000.0,
            nota = "Se dio vuelto de más",
        )

        assertEquals("Se dio vuelto de más", cierres().single().caja.nota)
    }

    @Test
    fun unaJornadaQueCuadroNoTieneNotaYEstaBien() = runTest {
        val dia = momento
        jornada(50_000.0, dia.withHour(8), dia.withHour(14), 50_000.0)

        assertNull(cierres().single().caja.nota)
    }

    @Test
    fun elResumenCuentaCuantasNoCuadraron() = runTest {
        val dia = momento
        jornada(10_000.0, dia.minusDays(2).withHour(8), dia.minusDays(2).withHour(14), 10_000.0)
        jornada(20_000.0, dia.minusDays(1).withHour(8), dia.minusDays(1).withHour(14), 18_000.0)
        jornada(30_000.0, dia.withHour(8), dia.withHour(14), 30_000.0)

        val resumen = cierres().resumen()

        assertEquals(3, resumen.cierres)
        assertEquals(1, resumen.conDiferencia)
    }

    @Test
    fun corregirUnaVentaVieja_corrigeElHistorial() = runTest {
        val dia = momento
        jornada(
            montoInicial = 50_000.0,
            abreA = dia.withHour(8),
            cierraA = dia.withHour(14),
            montoReal = 75_000.0,
        )
        vender(20_000.0, aLas = dia.withHour(11))

        // Con la venta mal registrada, la jornada aparece con $5.000 de sobra.
        assertTrue(cierres().single().diferencia.sobra)

        val venta = ventaRepository.ventasDelDia(negocioId, dia.toLocalDate()).first().single()
        enElMomento(dia.withHour(11)) {
            ventaRepository.editarVenta(
                ventaId = venta.id,
                tipoRegistro = TipoRegistroVenta.DETALLADO,
                monto = 25_000.0,
                fechaHora = dia.withHour(11),
                productoServicio = "Torta",
                metodoPago = MetodoPago.EFECTIVO,
            )
        }

        // Al corregirla, el historial se corrige solo: la diferencia no estaba guardada.
        val corregido = cierres().single()
        assertTrue(corregido.diferencia.cuadra)
        assertFalse(corregido.diferencia.sobra)
    }
}
