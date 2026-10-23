package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.db.dao.CostoDaoFalso
import com.gestor_ventures.db.dao.GastoDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-40. Ingresos y egresos repartidos en el tiempo.
 *
 * Lo delicado es que cada movimiento caiga en su tramo y en uno solo: si se colaran de un tramo
 * a otro, la tendencia mostraría subidas y bajadas que nunca ocurrieron.
 */
class CalcularTendenciaTest {

    // Jueves 15 de octubre de 2026.
    private val hoy = LocalDateTime.of(2026, 10, 15, 12, 0)
    private var momento = hoy
    private val reloj = Reloj { momento }

    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()

    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)

    private val calcularTendencia = CalcularTendencia(
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = costoRepository,
        reloj = reloj,
    )

    private val negocioId = 1L

    private suspend fun <T> enElMomento(instante: LocalDateTime, accion: suspend () -> T): T {
        val volverA = momento
        momento = instante
        return try {
            accion()
        } finally {
            momento = volverA
        }
    }

    private suspend fun vender(monto: Double, cuando: LocalDateTime) = enElMomento(cuando) {
        ventaRepository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = monto,
            fechaHora = cuando,
            productoServicio = "Torta",
            metodoPago = MetodoPago.EFECTIVO,
        )
    }

    private suspend fun gastar(monto: Double, cuando: LocalDateTime) = enElMomento(cuando) {
        gastoRepository.registrarGasto(negocioId, "Domicilio", monto, cuando)
    }

    private suspend fun costear(monto: Double, cuando: LocalDateTime) = enElMomento(cuando) {
        costoRepository.registrarCosto(negocioId, "Harina", monto, cuando)
    }

    private suspend fun mensual() = calcularTendencia(negocioId, EscalaDeTiempo.Mensual).first()

    private suspend fun semanal() = calcularTendencia(negocioId, EscalaDeTiempo.Semanal).first()

    // ---------- Los tramos ----------

    @Test
    fun laEscalaMensualMuestraSeisMesesTerminandoEnElDeHoy() {
        val tramos = tramosDe(EscalaDeTiempo.Mensual, hoy.toLocalDate())

        assertEquals(6, tramos.size)
        assertEquals(LocalDate.of(2026, 5, 1), tramos.first().desde)
        assertEquals(LocalDate.of(2026, 10, 1), tramos.last().desde)
        assertEquals(LocalDate.of(2026, 10, 31), tramos.last().hasta)
    }

    @Test
    fun laEscalaSemanalMuestraOchoSemanasQueArrancanEnLunes() {
        val tramos = tramosDe(EscalaDeTiempo.Semanal, hoy.toLocalDate())

        assertEquals(8, tramos.size)
        // 15 de octubre de 2026 es jueves: su semana arranca el lunes 12.
        assertEquals(LocalDate.of(2026, 10, 12), tramos.last().desde)
        assertEquals(LocalDate.of(2026, 10, 18), tramos.last().hasta)
        assertTrue(tramos.all { it.desde.dayOfWeek == java.time.DayOfWeek.MONDAY })
    }

    // ---------- El reparto ----------

    @Test
    fun sinMovimientosTodosLosTramosVienenEnCero() = runTest {
        val puntos = mensual()

        assertEquals(6, puntos.size)
        assertTrue(puntos.all { it.ingresos == 0.0 && it.egresos == 0.0 })
    }

    @Test
    fun cadaVentaCaeEnElMesQueLeToca() = runTest {
        vender(100_000.0, hoy.withMonth(8).withDayOfMonth(10))
        vender(250_000.0, hoy.withDayOfMonth(2))

        val puntos = mensual()

        assertEquals(100_000.0, puntos[3].ingresos, 0.001)
        assertEquals(250_000.0, puntos[5].ingresos, 0.001)
        // Y en ningún otro: un movimiento está en un tramo y en uno solo.
        assertEquals(350_000.0, puntos.sumOf { it.ingresos }, 0.001)
    }

    @Test
    fun losGastosYLosCostosSeGuardanAparte() = runTest {
        gastar(12_000.0, hoy.withDayOfMonth(3))
        costear(8_000.0, hoy.withDayOfMonth(4))

        val punto = mensual().last()

        // HU-16 los distingue: un costo es lo que cuesta producir, un gasto es todo lo demás.
        // Quien mira la tendencia necesita saber cuál de los dos se le está disparando.
        assertEquals(12_000.0, punto.gastos, 0.001)
        assertEquals(8_000.0, punto.costos, 0.001)
        assertEquals(0.0, punto.ingresos, 0.001)
    }

    @Test
    fun laLineaDeEgresosSumaLosDos() = runTest {
        gastar(12_000.0, hoy.withDayOfMonth(3))
        costear(8_000.0, hoy.withDayOfMonth(4))

        // La comparación de un vistazo sigue siendo "cuánto entró contra cuánto salió".
        assertEquals(20_000.0, mensual().last().egresos, 0.001)
    }

    @Test
    fun unGastoNoSeCuentaComoCostoNiAlReves() = runTest {
        gastar(12_000.0, hoy.withDayOfMonth(3))

        val punto = mensual().last()

        assertEquals(12_000.0, punto.gastos, 0.001)
        assertEquals(0.0, punto.costos, 0.001)
    }

    @Test
    fun laGananciaDeUnTramoEsLoQueEntroMenosLoQueSalio() = runTest {
        vender(100_000.0, hoy.withDayOfMonth(2))
        gastar(30_000.0, hoy.withDayOfMonth(3))

        assertEquals(70_000.0, mensual().last().ganancia, 0.001)
    }

    @Test
    fun unTramoPuedeQuedarEnNegativo() = runTest {
        vender(10_000.0, hoy.withDayOfMonth(2))
        gastar(40_000.0, hoy.withDayOfMonth(3))

        // Se muestra tal cual: un mes en rojo es justo lo que hay que ver.
        assertEquals(-30_000.0, mensual().last().ganancia, 0.001)
    }

    @Test
    fun loDeHaceMasDeSeisMesesNoEntraEnElGrafico() = runTest {
        vender(999_000.0, hoy.minusMonths(8))
        vender(50_000.0, hoy.withDayOfMonth(2))

        assertEquals(50_000.0, mensual().sumOf { it.ingresos }, 0.001)
    }

    @Test
    fun enSemanalCadaVentaCaeEnSuSemana() = runTest {
        // Lunes 12 y el martes de la semana anterior.
        vender(30_000.0, hoy.withDayOfMonth(12))
        vender(20_000.0, hoy.withDayOfMonth(6))

        val puntos = semanal()

        assertEquals(30_000.0, puntos.last().ingresos, 0.001)
        assertEquals(20_000.0, puntos[6].ingresos, 0.001)
    }

    @Test
    fun elGraficoSeRehaceSoloAlRegistrarAlgoNuevo() = runTest {
        assertEquals(0.0, mensual().last().ingresos, 0.001)

        // HU-40: nadie le avisa al gráfico; sale de Flows y se entera solo.
        vender(75_000.0, hoy.withDayOfMonth(2))

        assertEquals(75_000.0, mensual().last().ingresos, 0.001)
    }
}
