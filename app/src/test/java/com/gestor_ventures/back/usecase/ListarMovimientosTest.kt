package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.FiltroMovimientos
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Movimiento
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.RangoFechas
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.model.rango
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
 * HU-17. El historial: ventas, gastos y costos del periodo en una sola lista.
 *
 * Lo que de verdad se fija acá es el orden, porque de él depende poder tocar una fila con
 * confianza, y qué entra en cada periodo.
 */
class ListarMovimientosTest {

    // Miércoles 16 de septiembre de 2026.
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy: LocalDate = ahora.toLocalDate()
    private val reloj = Reloj { ahora }

    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()

    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)

    private val listarMovimientos = ListarMovimientos(
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = costoRepository,
        reloj = reloj,
    )

    private val negocioId = 1L

    private suspend fun vender(monto: Double, fecha: LocalDateTime = ahora) =
        ventaRepository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = monto,
            fechaHora = fecha,
            productoServicio = "Torta",
            metodoPago = MetodoPago.EFECTIVO,
        )

    private suspend fun gastar(monto: Double, fecha: LocalDate = hoy) =
        gastoRepository.registrarGasto(negocioId, "Transporte", monto, fecha)

    private suspend fun costear(monto: Double, fecha: LocalDateTime = ahora) =
        costoRepository.registrarCosto(negocioId, "Harina", monto, fecha)

    private suspend fun movimientos(
        rango: RangoFechas = RangoFechas(hoy, hoy),
        filtro: FiltroMovimientos = FiltroMovimientos.Todos,
    ) = listarMovimientos(negocioId, rango, filtro).first()

    @Test
    fun elHistorialJuntaVentasGastosYCostos() = runTest {
        vender(50_000.0)
        gastar(20_000.0)
        costear(10_000.0)

        val lista = movimientos()

        assertEquals(3, lista.size)
        assertEquals(1, lista.filterIsInstance<Movimiento.DeVenta>().size)
        assertEquals(1, lista.filterIsInstance<Movimiento.DeGasto>().size)
        assertEquals(1, lista.filterIsInstance<Movimiento.DeCosto>().size)
    }

    @Test
    fun loMasRecienteVaPrimero() = runTest {
        vender(10_000.0, ahora.withHour(6))
        vender(30_000.0, ahora.withHour(9))
        vender(20_000.0, ahora.withHour(8))

        assertEquals(
            listOf(30_000.0, 20_000.0, 10_000.0),
            movimientos().map { it.monto },
        )
    }

    @Test
    fun dosMovimientosDelMismoInstanteNoSalenEnCualquierOrden() = runTest {
        vender(10_000.0)
        vender(20_000.0)

        // Sin desempate, dos ventas del mismo minuto cambiarían de orden entre lecturas y la
        // fila que el usuario iba a tocar se le movería debajo del dedo.
        assertEquals(listOf(20_000.0, 10_000.0), movimientos().map { it.monto })
        assertEquals(listOf(20_000.0, 10_000.0), movimientos().map { it.monto })
    }

    @Test
    fun soloEntraLoQueCaeDentroDelRango() = runTest {
        vender(10_000.0, ahora.minusDays(1))
        vender(20_000.0)
        gastar(5_000.0, hoy.minusDays(3))

        val lista = movimientos()

        assertEquals(1, lista.size)
        assertEquals(20_000.0, lista.single().monto, 0.001)
    }

    @Test
    fun elRangoIncluyeSusDosExtremos() = runTest {
        // El primer instante del primer día y el último del último: si los bordes se calcularan
        // mal por un milisegundo, estas dos ventas se caerían del historial.
        vender(10_000.0, hoy.minusDays(2).atStartOfDay())
        vender(20_000.0, hoy.minusDays(1).atTime(23, 59, 59))

        val lista = movimientos(RangoFechas(hoy.minusDays(2), hoy.minusDays(1)))

        assertEquals(2, lista.size)
    }

    @Test
    fun elFiltroDeVentasDejaFueraGastosYCostos() = runTest {
        vender(50_000.0)
        gastar(20_000.0)
        costear(10_000.0)

        val lista = movimientos(filtro = FiltroMovimientos.Ventas)

        assertEquals(1, lista.size)
        assertTrue(lista.single().entra)
    }

    @Test
    fun elFiltroDeSalidasJuntaGastosYCostos() = runTest {
        vender(50_000.0)
        gastar(20_000.0)
        costear(10_000.0)

        val lista = movimientos(filtro = FiltroMovimientos.Salidas)

        // La pregunta es "¿en qué se me fue la plata?", y ahí gasto y costo son lo mismo.
        assertEquals(2, lista.size)
        assertTrue(lista.none { it.entra })
    }

    @Test
    fun dentroDeUnDiaLasVentasQuedanSobreLosGastos() = runTest {
        vender(50_000.0, ahora.withHour(9))
        gastar(20_000.0)

        // El gasto se registra sin hora, así que cae al comienzo de su día.
        val lista = movimientos()

        assertTrue(lista.first() is Movimiento.DeVenta)
        assertTrue(lista.last() is Movimiento.DeGasto)
    }

    @Test
    fun hoyEsSoloHoy() = runTest {
        vender(10_000.0, ahora.minusDays(1))
        vender(20_000.0)

        assertEquals(1, movimientos(PeriodoPredefinido.Hoy.rango(hoy)).size)
    }

    @Test
    fun ayerEsSoloAyerYNoIncluyeHoy() = runTest {
        vender(10_000.0, ahora.minusDays(1))
        vender(20_000.0)

        val lista = movimientos(PeriodoPredefinido.Ayer.rango(hoy))

        assertEquals(1, lista.size)
        assertEquals(10_000.0, lista.single().monto, 0.001)
    }

    @Test
    fun laSemanaEsLaSemanaEnCursoYNoLosUltimosSieteDias() = runTest {
        // Hoy es miércoles: el lunes entra, el domingo anterior no.
        vender(10_000.0, ahora.minusDays(2))
        vender(20_000.0, ahora.minusDays(3))

        val lista = movimientos(PeriodoPredefinido.Semana.rango(hoy))

        assertEquals(1, lista.size)
        assertEquals(10_000.0, lista.single().monto, 0.001)
    }

    @Test
    fun elMesEsElMesEnCursoYNoLosUltimosTreintaDias() = runTest {
        vender(10_000.0, ahora.withDayOfMonth(1))
        vender(20_000.0, ahora.minusMonths(1))

        val lista = movimientos(PeriodoPredefinido.Mes.rango(hoy))

        assertEquals(1, lista.size)
        assertEquals(10_000.0, lista.single().monto, 0.001)
    }

    @Test
    fun unPeriodoSinMovimientosDevuelveLaListaVacia() = runTest {
        vender(20_000.0)

        assertTrue(movimientos(PeriodoPredefinido.Ayer.rango(hoy)).isEmpty())
    }
}
