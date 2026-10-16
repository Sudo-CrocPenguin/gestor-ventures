package com.gestor_ventures.front.ui.caja

import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.back.usecase.ListarCierresDeCaja
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CajaDaoFalso
import com.gestor_ventures.db.dao.CostoDaoFalso
import com.gestor_ventures.db.dao.GastoDaoFalso
import com.gestor_ventures.db.dao.NegocioDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-23: el historial de jornadas cerradas y su filtro por periodo. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialCajaViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    // Jueves 15 de octubre de 2026.
    private val hoy = LocalDateTime.of(2026, 10, 15, 20, 0)
    private var momento = hoy
    private val reloj = Reloj { momento }

    private val cajaDao = CajaDaoFalso()
    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()
    private val negocioDao = NegocioDaoFalso()

    private val cajaRepository = CajaRepository(cajaDao, reloj)
    private val ventaRepository = VentaRepository(ventaDao, reloj)

    private val listarCierres = ListarCierresDeCaja(
        cajaRepository = cajaRepository,
        ventaRepository = ventaRepository,
        gastoRepository = GastoRepository(gastoDao, reloj),
        costoRepository = CostoRepository(costoDao, reloj),
        reloj = reloj,
    )

    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        sesionRepositoryDePrueba(),
    )

    private lateinit var viewModel: HistorialCajaViewModel

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
                fechaCreacion = hoy,
            ),
        )
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private fun abrirPantalla() {
        viewModel = HistorialCajaViewModel(listarCierres, negocioActivo)
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

    private suspend fun jornada(
        montoInicial: Double,
        abreA: LocalDateTime,
        cierraA: LocalDateTime,
        montoReal: Double,
        nota: String? = null,
    ) {
        enElMomento(abreA) { cajaRepository.abrirCaja(1L, montoInicial) }
        enElMomento(cierraA) { cajaRepository.cerrarCaja(1L, montoReal, nota) }
    }

    private suspend fun vender(monto: Double, aLas: LocalDateTime) = enElMomento(aLas) {
        ventaRepository.registrarVenta(
            negocioId = 1L,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = monto,
            fechaHora = aLas,
            productoServicio = "Torta",
            metodoPago = MetodoPago.EFECTIVO,
        )
    }

    @Test
    fun sinCierresLoDiceEnVezDeMostrarUnaListaEnBlanco() = runTest(dispatcher) {
        abrirPantalla()
        advanceUntilIdle()

        assertTrue(estado.vacio)
        assertEquals(0, estado.cantidad)
    }

    @Test
    fun arrancaEnLaSemanaPorqueElDeHoyCasiSiempreEstariaVacio() = runTest(dispatcher) {
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(PeriodoPredefinido.Semana, estado.periodo)
    }

    @Test
    fun unaJornadaCerradaApareceConSuHorarioYSusMontos() = runTest(dispatcher) {
        jornada(50_000.0, hoy.withHour(8), hoy.withHour(14), montoReal = 75_000.0)
        vender(25_000.0, aLas = hoy.withHour(11))
        abrirPantalla()
        advanceUntilIdle()

        val cierre = estado.cierres.single()
        assertEquals(hoy.withHour(8), cierre.apertura)
        assertEquals(hoy.withHour(14), cierre.cierre)
        assertEquals(50_000.0, cierre.montoInicial, 0.001)
        assertEquals(75_000.0, cierre.esperado, 0.001)
        assertEquals(75_000.0, cierre.real, 0.001)
        assertTrue(cierre.cuadra)
    }

    @Test
    fun unaJornadaConFaltanteSeMarcaComoTal() = runTest(dispatcher) {
        jornada(50_000.0, hoy.withHour(8), hoy.withHour(14), montoReal = 48_000.0)
        abrirPantalla()
        advanceUntilIdle()

        val cierre = estado.cierres.single()
        assertEquals(-2_000.0, cierre.diferencia, 0.001)
        assertFalse(cierre.cuadra)
        assertFalse(cierre.sobra)
    }

    @Test
    fun elResumenCuentaSoloLasQueNoCuadraron() = runTest(dispatcher) {
        jornada(10_000.0, hoy.minusDays(2).withHour(8), hoy.minusDays(2).withHour(14), 10_000.0)
        jornada(20_000.0, hoy.minusDays(1).withHour(8), hoy.minusDays(1).withHour(14), 18_000.0)
        jornada(30_000.0, hoy.withHour(8), hoy.withHour(14), 30_000.0)
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(3, estado.cantidad)
        assertEquals(1, estado.conDiferencia)
    }

    @Test
    fun cambiarDePeriodoCambiaLoQueSeLista() = runTest(dispatcher) {
        jornada(50_000.0, hoy.withHour(8), hoy.withHour(14), 50_000.0)
        jornada(20_000.0, hoy.minusDays(3).withHour(8), hoy.minusDays(3).withHour(14), 20_000.0)
        abrirPantalla()
        advanceUntilIdle()
        assertEquals(2, estado.cantidad)

        viewModel.onPeriodoChange(PeriodoPredefinido.Hoy)
        advanceUntilIdle()

        assertEquals(1, estado.cantidad)
        assertEquals(50_000.0, estado.cierres.single().montoInicial, 0.001)
    }

    @Test
    fun elChipSeMarcaDeInmediatoSinEsperarALaConsulta() = runTest(dispatcher) {
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onPeriodoChange(PeriodoPredefinido.Mes)

        // Sin advanceUntilIdle: esperar a la base de datos para pintar el chip se sentiría
        // como que la pantalla no responde.
        assertEquals(PeriodoPredefinido.Mes, estado.periodo)
    }

    @Test
    fun laJornadaAbiertaNoSaleEnElHistorial() = runTest(dispatcher) {
        jornada(50_000.0, hoy.minusDays(1).withHour(8), hoy.minusDays(1).withHour(14), 50_000.0)
        enElMomento(hoy.withHour(8)) { cajaRepository.abrirCaja(1L, 20_000.0) }
        abrirPantalla()
        advanceUntilIdle()

        // El historial es de lo que ya terminó; la de hoy se mira en la pantalla de caja.
        assertEquals(1, estado.cantidad)
    }

    @Test
    fun laNotaDelCierreLlegaALaFila() = runTest(dispatcher) {
        jornada(
            montoInicial = 50_000.0,
            abreA = hoy.withHour(8),
            cierraA = hoy.withHour(14),
            montoReal = 48_000.0,
            nota = "Se dio vuelto de más",
        )
        abrirPantalla()
        advanceUntilIdle()

        assertEquals("Se dio vuelto de más", estado.cierres.single().nota)
    }

    @Test
    fun unaJornadaQueCuadroNoTraeNota() = runTest(dispatcher) {
        jornada(50_000.0, hoy.withHour(8), hoy.withHour(14), 50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        assertNull(estado.cierres.single().nota)
    }
}
