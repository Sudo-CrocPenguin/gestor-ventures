package com.gestor_ventures.front.ui.negocio

import com.gestor_ventures.back.model.ErrorNegocio
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.NegocioDaoFalso
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-05. El ViewModel con un repositorio real y un DAO en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrarNegocioViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = NegocioDaoFalso()
    private val momento = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val repository = NegocioRepository(dao, Reloj { momento })
    private val viewModel = RegistrarNegocioViewModel(repository, sesionRepositoryDePrueba())

    private val estado get() = viewModel.uiState.value

    @Before
    fun fijarDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun soltarDispatcher() {
        Dispatchers.resetMain()
    }

    private fun llenarFormulario(nombre: String = "Dulce Antojo", categoria: String = "Repostería") {
        viewModel.onNombreChange(nombre)
        viewModel.onCategoriaChange(categoria)
    }

    @Test
    fun arrancaVacioYSinPoderGuardar() {
        assertEquals("", estado.nombre)
        assertEquals("", estado.categoria)
        assertEquals(TipoActividad.PRODUCTOS, estado.tipoActividad)
        assertFalse(estado.puedeGuardar)
    }

    @Test
    fun elNombreYLaActividadSonObligatorios() {
        viewModel.onNombreChange("Dulce Antojo")
        assertFalse(estado.puedeGuardar)

        viewModel.onCategoriaChange("Repostería")
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun guardar_creaElNegocioConLoQueEligioElUsuario() = runTest(dispatcher) {
        llenarFormulario()
        viewModel.onTipoActividadChange(TipoActividad.MIXTO)

        viewModel.guardar()
        advanceUntilIdle()

        val negocios = repository.negociosDeUsuario(SemillaTemporal.USUARIO_ID).first()
        assertEquals(1, negocios.size)
        assertEquals("Dulce Antojo", negocios.first().nombre)
        assertEquals("Repostería", negocios.first().categoria)
        assertEquals(TipoActividad.MIXTO, negocios.first().tipoActividad)
    }

    @Test
    fun guardar_avisaConElIdDelNegocioCreado() = runTest(dispatcher) {
        llenarFormulario()

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(1L, viewModel.negocioCreado.first())
    }

    @Test
    fun guardar_muestraElErrorQueDevuelveElRepositorio() = runTest(dispatcher) {
        llenarFormulario(nombre = "a".repeat(101))

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(ErrorNegocio.NombreMuyLargo, estado.error)
        assertFalse(estado.guardando)
        assertTrue(dao.negocios.value.isEmpty())
    }

    @Test
    fun escribirDeNuevo_borraElErrorAnterior() = runTest(dispatcher) {
        llenarFormulario(nombre = "a".repeat(101))
        viewModel.guardar()
        advanceUntilIdle()

        viewModel.onNombreChange("Dulce Antojo")

        assertNull(estado.error)
    }

    @Test
    fun elPorcentajeDeReinversionArrancaEnCero() = runTest(dispatcher) {
        llenarFormulario()

        viewModel.guardar()
        advanceUntilIdle()

        val negocio = repository.negociosDeUsuario(SemillaTemporal.USUARIO_ID).first().first()
        assertEquals(0.0, negocio.porcentajeReinversion, 0.001)
    }
}
