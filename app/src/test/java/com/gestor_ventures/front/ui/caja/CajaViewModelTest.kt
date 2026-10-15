package com.gestor_ventures.front.ui.caja

import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.back.usecase.CalcularSaldoEsperado
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-19 y HU-20: abrir la jornada de caja y seguir su saldo. */
@OptIn(ExperimentalCoroutinesApi::class)
class CajaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 10, 7, 8, 0)
    private val reloj = Reloj { ahora }

    private val cajaDao = CajaDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()

    private val repository = CajaRepository(cajaDao, reloj)
    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)

    private val calcularSaldoEsperado = CalcularSaldoEsperado(
        cajaRepository = repository,
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = costoRepository,
    )

    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        sesionRepositoryDePrueba(),
    )

    private lateinit var viewModel: CajaViewModel

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
        viewModel = CajaViewModel(repository, calcularSaldoEsperado, negocioActivo)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private fun abrirCon(monto: String) {
        viewModel.abrirFormulario()
        viewModel.onMontoChange(monto)
        viewModel.abrirCaja()
    }

    @Test
    fun alEmpezarElDiaNoHayJornadaAbierta() = runTest(dispatcher) {
        advanceUntilIdle()

        assertTrue(estado.sinCaja)
        assertNull(estado.caja)
    }

    @Test
    fun abrirCajaDejaLaJornadaALaVistaYCierraLaHoja() = runTest(dispatcher) {
        advanceUntilIdle()

        abrirCon("50000")
        advanceUntilIdle()

        assertEquals(50_000.0, estado.caja?.montoInicial ?: -1.0, 0.001)
        assertEquals(ahora, estado.caja?.apertura)
        assertNull(estado.formulario)
        assertFalse(estado.sinCaja)
    }

    @Test
    fun elMontoSeVeConPuntosDeMilesMientrasSeEscribe() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormulario()
        viewModel.onMontoChange("50000")

        assertEquals("50.000", estado.formulario?.montoFormateado)
    }

    @Test
    fun loQueNoEsNumeroNoEntraAlCampo() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormulario()
        viewModel.onMontoChange("50.000 pesos")

        // Queda solo lo que de verdad es un monto; el resto nunca llega al repositorio.
        assertEquals("50000", estado.formulario?.montoInicial)
    }

    @Test
    fun conElCampoVacioNoSePuedeAbrir() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormulario()

        assertFalse(estado.formulario?.puedeAbrir ?: true)

        viewModel.abrirCaja()
        advanceUntilIdle()
        assertNull(estado.caja)
    }

    @Test
    fun arrancarSinEfectivoSiSePuede() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormulario()
        viewModel.onMontoChange("0")

        // Cero escrito a propósito no es lo mismo que un campo que se quedó vacío.
        assertTrue(estado.formulario?.puedeAbrir ?: false)

        viewModel.abrirCaja()
        advanceUntilIdle()
        assertEquals(0.0, estado.caja?.montoInicial ?: -1.0, 0.001)
    }

    @Test
    fun conUnaCajaAbiertaLaPantallaNoOfreceAbrirOtra() = runTest(dispatcher) {
        advanceUntilIdle()
        abrirCon("50000")
        advanceUntilIdle()

        assertFalse(estado.sinCaja)
        assertNotNull(estado.caja)
    }

    @Test
    fun siAlguienAlcanzaAAbrirDosVecesLaHojaSeQuedaConElAviso() = runTest(dispatcher) {
        advanceUntilIdle()
        abrirCon("50000")
        advanceUntilIdle()

        abrirCon("20000")
        advanceUntilIdle()

        // Lo escrito no se pierde: la hoja sigue abierta para poder corregirlo.
        assertEquals(ErrorCaja.YaHayCajaAbierta, estado.error)
        assertNotNull(estado.formulario)
        assertEquals(50_000.0, estado.caja?.montoInicial ?: -1.0, 0.001)
    }

    @Test
    fun cerrarLaHojaNoDejaRastro() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormulario()
        viewModel.onMontoChange("50000")
        viewModel.cerrarFormulario()

        assertNull(estado.formulario)
        viewModel.abrirFormulario()
        assertEquals("", estado.formulario?.montoInicial)
    }

    @Test
    fun escribirDeNuevoBorraElAvisoAnterior() = runTest(dispatcher) {
        advanceUntilIdle()
        abrirCon("50000")
        advanceUntilIdle()
        abrirCon("20000")
        advanceUntilIdle()

        viewModel.onMontoChange("30000")

        assertNull(estado.error)
    }

    // ---------- HU-20: el saldo esperado ----------

    private suspend fun vender(monto: Double) = ventaRepository.registrarVenta(
        negocioId = 1L,
        tipoRegistro = TipoRegistroVenta.DETALLADO,
        monto = monto,
        fechaHora = ahora,
        productoServicio = "Torta",
        metodoPago = MetodoPago.EFECTIVO,
    )

    @Test
    fun recienAbiertaElSaldoEsLoQueSeConto() = runTest(dispatcher) {
        advanceUntilIdle()
        abrirCon("50000")
        advanceUntilIdle()

        assertEquals(50_000.0, estado.caja?.saldoEsperado ?: -1.0, 0.001)
        assertTrue(estado.caja?.sinMovimientos ?: false)
    }

    @Test
    fun registrarUnaVentaMueveElSaldoSolo() = runTest(dispatcher) {
        advanceUntilIdle()
        abrirCon("50000")
        advanceUntilIdle()

        // HU-20: nadie le avisa a la caja; la pantalla se entera sola.
        vender(25_000.0)
        advanceUntilIdle()

        assertEquals(75_000.0, estado.caja?.saldoEsperado ?: -1.0, 0.001)
        assertEquals(25_000.0, estado.caja?.ventas ?: -1.0, 0.001)
    }

    @Test
    fun elDesgloseAcompanaAlSaldo() = runTest(dispatcher) {
        advanceUntilIdle()
        abrirCon("50000")
        advanceUntilIdle()
        vender(25_000.0)
        gastoRepository.registrarGasto(1L, "Domicilio", 12_000.0, ahora)
        costoRepository.registrarCosto(1L, "Harina", 8_000.0, ahora)
        advanceUntilIdle()

        val caja = estado.caja
        assertEquals(25_000.0, caja?.ventas ?: -1.0, 0.001)
        assertEquals(12_000.0, caja?.gastos ?: -1.0, 0.001)
        assertEquals(8_000.0, caja?.costos ?: -1.0, 0.001)
        assertEquals(55_000.0, caja?.saldoEsperado ?: -1.0, 0.001)
        assertFalse(caja?.sinMovimientos ?: true)
    }

    @Test
    fun sinCajaAbiertaNoHaySaldoQueMostrar() = runTest(dispatcher) {
        advanceUntilIdle()
        vender(25_000.0)
        advanceUntilIdle()

        // La venta existe, pero sin jornada abierta no hay saldo de caja que calcular.
        assertNull(estado.caja)
        assertTrue(estado.sinCaja)
    }
}
