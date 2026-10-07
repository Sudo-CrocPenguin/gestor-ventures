package com.gestor_ventures.front.ui.caja

import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CajaDaoFalso
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

/** HU-19: abrir la jornada de caja con el efectivo con el que se arranca. */
@OptIn(ExperimentalCoroutinesApi::class)
class CajaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 10, 7, 8, 0)
    private val reloj = Reloj { ahora }

    private val cajaDao = CajaDaoFalso()
    private val negocioDao = NegocioDaoFalso()

    private val repository = CajaRepository(cajaDao, reloj)
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
        viewModel = CajaViewModel(repository, negocioActivo)
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
}
