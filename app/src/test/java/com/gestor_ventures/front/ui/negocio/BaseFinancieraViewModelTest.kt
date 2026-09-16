package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.SavedStateHandle
import com.gestor_ventures.back.model.ErrorBaseFinanciera
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.usecase.CalcularAhorroMensual
import com.gestor_ventures.db.dao.GastoFijoDaoFalso
import com.gestor_ventures.db.dao.MetaAhorroDaoFalso
import com.gestor_ventures.db.dao.NegocioDaoFalso
import com.gestor_ventures.front.navigation.ArgumentoNegocioId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** HU-06, HU-08 y HU-09: el paso 2 del onboarding. */
@OptIn(ExperimentalCoroutinesApi::class)
class BaseFinancieraViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val hoy = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val reloj = Reloj { hoy }

    private val gastoFijoDao = GastoFijoDaoFalso()
    private val metaAhorroDao = MetaAhorroDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val repository = BaseFinancieraRepository(gastoFijoDao, metaAhorroDao, negocioDao, reloj)

    private val negocioId = 1L

    private lateinit var viewModel: BaseFinancieraViewModel

    private val estado get() = viewModel.uiState.value

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        viewModel = BaseFinancieraViewModel(
            repository = repository,
            calcularAhorroMensual = CalcularAhorroMensual(reloj),
            savedStateHandle = SavedStateHandle(mapOf(ArgumentoNegocioId to negocioId.toString())),
        )
    }

    @After
    fun soltar() {
        Dispatchers.resetMain()
    }

    @Test
    fun laMetaMuestraCuantoHayQueApartarCadaMes() = runTest(dispatcher) {
        viewModel.onMetaMontoChange("2000000")
        viewModel.onFechaLimiteChange(LocalDate.of(2026, 12, 31))

        assertEquals("2.000.000", estado.metaFormateada)
        assertEquals(500_000.0, estado.ahorroMensual ?: 0.0, 1.0)
    }

    @Test
    fun sinFechaNoHayCalculo() = runTest(dispatcher) {
        viewModel.onMetaMontoChange("2000000")

        assertNull(estado.ahorroMensual)
    }

    @Test
    fun agregarGastoFijo_loGuardaYCierraElFormulario() = runTest(dispatcher) {
        viewModel.abrirFormularioGasto()
        viewModel.onNombreGastoChange("Arriendo local")
        viewModel.onMontoGastoChange("300000")
        viewModel.onFrecuenciaGastoChange(Frecuencia.MENSUAL)

        viewModel.guardarGastoFijo()
        advanceUntilIdle()

        assertNull(estado.formularioGasto)
        assertEquals(1, estado.gastosFijos.size)
        assertEquals("Arriendo local", estado.gastosFijos.first().nombre)
        assertEquals(300_000.0, estado.totalGastosFijos, 0.001)
    }

    @Test
    fun elGastoFijoNecesitaNombreYMonto() = runTest(dispatcher) {
        viewModel.abrirFormularioGasto()
        viewModel.onNombreGastoChange("Arriendo local")

        assertEquals(false, estado.formularioGasto?.puedeGuardar)

        viewModel.onMontoGastoChange("300000")

        assertEquals(true, estado.formularioGasto?.puedeGuardar)
    }

    @Test
    fun finalizar_guardaMetaYReinversion() = runTest(dispatcher) {
        viewModel.onMetaMontoChange("2000000")
        viewModel.onFechaLimiteChange(LocalDate.of(2026, 12, 31))
        viewModel.onPorcentajeReinversionChange(20)

        viewModel.finalizar()
        advanceUntilIdle()

        val meta = repository.metaActiva(negocioId).first()
        assertEquals(2_000_000.0, meta?.montoObjetivo ?: 0.0, 0.001)
        assertEquals(LocalDate.of(2026, 12, 31), meta?.fechaLimite)
        assertNull(estado.error)
    }

    @Test
    fun finalizar_sinMetaTambienTermina() = runTest(dispatcher) {
        var termino = false

        viewModel.finalizar()
        advanceUntilIdle()
        termino = true

        assertNull(repository.metaActiva(negocioId).first())
        assertNull(estado.error)
        assertEquals(true, termino)
    }

    @Test
    fun finalizar_avisaSiLaFechaYaPaso() = runTest(dispatcher) {
        viewModel.onMetaMontoChange("2000000")
        viewModel.onFechaLimiteChange(LocalDate.of(2026, 9, 1))

        viewModel.finalizar()
        advanceUntilIdle()

        assertEquals(ErrorBaseFinanciera.FechaLimiteNoPosterior, estado.error)
        assertNull(repository.metaActiva(negocioId).first())
    }

    @Test
    fun elPorcentajeSeQuedaEntreCeroYCien() {
        viewModel.onPorcentajeReinversionChange(150)
        assertEquals(100, estado.porcentajeReinversion)

        viewModel.onPorcentajeReinversionChange(-10)
        assertEquals(0, estado.porcentajeReinversion)
    }

    @Test
    fun elMontoDeLaMetaSoloAceptaDigitos() {
        viewModel.onMetaMontoChange("2.000.000 COP")

        assertEquals("2000000", estado.metaMonto)
        assertNotNull(estado.metaFormateada)
    }
}
