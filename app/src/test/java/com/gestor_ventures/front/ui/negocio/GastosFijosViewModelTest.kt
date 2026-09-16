package com.gestor_ventures.front.ui.negocio

import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.SesionRepository
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.GastoFijoDaoFalso
import com.gestor_ventures.db.dao.MetaAhorroDaoFalso
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

/** HU-06: administrar los gastos fijos del negocio activo, fuera del onboarding. */
@OptIn(ExperimentalCoroutinesApi::class)
class GastosFijosViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val reloj = Reloj { ahora }

    private val gastoFijoDao = GastoFijoDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val repository = BaseFinancieraRepository(
        gastoFijoDao,
        MetaAhorroDaoFalso(),
        negocioDao,
        reloj,
    )
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        SesionRepository(),
    )

    private lateinit var viewModel: GastosFijosViewModel

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
        viewModel = GastosFijosViewModel(repository, negocioActivo)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private suspend fun agregar(nombre: String, monto: Double) =
        repository.agregarGastoFijo(1L, nombre, monto, Frecuencia.MENSUAL)

    @Test
    fun muestraLosGastosDelNegocioActivoConSuTotal() = runTest(dispatcher) {
        agregar("Arriendo", 300_000.0)
        agregar("Internet", 85_000.0)
        advanceUntilIdle()

        assertEquals(2, estado.gastosFijos.size)
        assertEquals(385_000.0, estado.total, 0.001)
        assertFalse(estado.vacio)
    }

    @Test
    fun sinGastosLoDiceEnVezDeMostrarUnaListaEnBlanco() = runTest(dispatcher) {
        advanceUntilIdle()

        assertTrue(estado.vacio)
    }

    @Test
    fun agregarUnGastoLoDejaEnLaLista() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Arriendo local")
        viewModel.onMontoChange("300000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals("Arriendo local", estado.gastosFijos.single().nombre)
        // La hoja se cierra sola cuando el gasto quedó guardado.
        assertNull(estado.formularioGasto)
    }

    @Test
    fun elFormularioDeEdicionLlegaLlenoConElGasto() = runTest(dispatcher) {
        agregar("Arriendo", 300_000.0)
        advanceUntilIdle()

        viewModel.abrirFormularioDe(estado.gastosFijos.single())

        val formulario = estado.formularioGasto
        assertNotNull(formulario)
        assertEquals("Arriendo", formulario?.nombre)
        assertEquals("300000", formulario?.monto)
        assertTrue(formulario?.esEdicion == true)
    }

    @Test
    fun editarCorrigeElGastoEnVezDeCrearOtro() = runTest(dispatcher) {
        agregar("Arriendo", 300_000.0)
        advanceUntilIdle()

        viewModel.abrirFormularioDe(estado.gastosFijos.single())
        viewModel.onMontoChange("350000")
        viewModel.onFrecuenciaChange(Frecuencia.QUINCENAL)
        viewModel.guardarFormulario()
        advanceUntilIdle()

        val gasto = estado.gastosFijos.single()
        assertEquals(350_000.0, gasto.monto, 0.001)
        assertEquals(Frecuencia.QUINCENAL, gasto.frecuencia)
    }

    @Test
    fun elMontoSoloAceptaDigitos() = runTest(dispatcher) {
        viewModel.abrirFormularioNuevo()

        viewModel.onMontoChange("300.000 COP")

        assertEquals("300000", estado.formularioGasto?.monto)
        assertEquals("300.000", estado.formularioGasto?.montoFormateado)
    }

    @Test
    fun noSeGuardaUnGastoSinNombreOSinMonto() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Arriendo")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertTrue(estado.gastosFijos.isEmpty())
        // La hoja sigue abierta: nadie pierde lo que ya escribió.
        assertNotNull(estado.formularioGasto)
    }

    @Test
    fun eliminarSacaElGastoDeLaLista() = runTest(dispatcher) {
        agregar("Arriendo", 300_000.0)
        agregar("Internet", 85_000.0)
        advanceUntilIdle()

        viewModel.eliminar(estado.gastosFijos.first().id)
        advanceUntilIdle()

        assertEquals(1, estado.gastosFijos.size)
    }

    @Test
    fun tocarUnGastoAbreSusOpcionesYNoLoEditaDeUna() = runTest(dispatcher) {
        agregar("Arriendo", 300_000.0)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.gastosFijos.single())

        assertNotNull(estado.acciones)
        assertNull(estado.formularioGasto)
    }

    @Test
    fun elegirEditarAbreElFormularioYCierraElMenu() = runTest(dispatcher) {
        agregar("Arriendo", 300_000.0)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.gastosFijos.single())
        viewModel.editarElGastoElegido()

        assertNull(estado.acciones)
        assertEquals("Arriendo", estado.formularioGasto?.nombre)
        assertTrue(estado.formularioGasto?.esEdicion == true)
    }

    @Test
    fun elegirEliminarLoBorra() = runTest(dispatcher) {
        agregar("Arriendo", 300_000.0)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.gastosFijos.single())
        viewModel.eliminarElGastoElegido()
        advanceUntilIdle()

        assertNull(estado.acciones)
        assertTrue(estado.gastosFijos.isEmpty())
    }

    @Test
    fun cerrarElFormularioNoDejaRastro() = runTest(dispatcher) {
        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Arriendo")

        viewModel.cerrarFormulario()

        assertNull(estado.formularioGasto)
        viewModel.abrirFormularioNuevo()
        assertEquals("", estado.formularioGasto?.nombre)
    }

}
