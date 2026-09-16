package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.ErrorCategoria
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.SesionRepository
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CategoriaDaoFalso
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

/** HU-15: las categorías del negocio activo, separadas en gastos y costos. */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriasViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val reloj = Reloj { ahora }

    private val categoriaDao = CategoriaDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val repository = CategoriaRepository(categoriaDao)
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        SesionRepository(),
    )

    private lateinit var viewModel: CategoriasViewModel

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
        viewModel = CategoriasViewModel(repository, negocioActivo)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private suspend fun crear(nombre: String, tipo: TipoCategoria) =
        repository.crearCategoria(1L, nombre, tipo)

    @Test
    fun arrancaEnGastos() = runTest(dispatcher) {
        advanceUntilIdle()

        assertEquals(TipoCategoria.GASTO, estado.tipo)
        assertTrue(estado.vacio)
    }

    @Test
    fun cadaPestanaMuestraSoloSusCategorias() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        crear("Insumos", TipoCategoria.COSTO)
        advanceUntilIdle()

        assertEquals(listOf("Transporte"), estado.categorias.map { it.nombre })

        viewModel.onTipoChange(TipoCategoria.COSTO)
        advanceUntilIdle()

        assertEquals(listOf("Insumos"), estado.categorias.map { it.nombre })
        assertFalse(estado.vacio)
    }

    @Test
    fun laCategoriaNuevaSeCreaEnLaPestanaEnLaQueEstaElUsuario() = runTest(dispatcher) {
        viewModel.onTipoChange(TipoCategoria.COSTO)
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Insumos")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(TipoCategoria.COSTO, estado.categorias.single().tipo)
        // La hoja se cierra sola cuando la categoría quedó guardada.
        assertNull(estado.formulario)
    }

    @Test
    fun elNombreRepetidoDejaLaHojaAbiertaConElAviso() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("transporte")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(ErrorCategoria.NombreRepetido, estado.error)
        // Sigue abierta: cerrarla sería hacerle perder al usuario lo que escribió.
        assertNotNull(estado.formulario)
        assertEquals(1, estado.categorias.size)
    }

    @Test
    fun elNombreMuyLargoTambienAvisa() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("a".repeat(51))
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(ErrorCategoria.NombreMuyLargo, estado.error)
        assertTrue(estado.categorias.isEmpty())
    }

    @Test
    fun elFormularioDeEdicionLlegaConElNombre() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.abrirFormularioDe(estado.categorias.single())

        assertEquals("Transporte", estado.formulario?.nombre)
        assertTrue(estado.formulario?.esEdicion == true)
    }

    @Test
    fun renombrarCorrigeLaCategoriaEnVezDeCrearOtra() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()
        val original = estado.categorias.single()

        viewModel.abrirFormularioDe(original)
        viewModel.onNombreChange("Domicilios")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        val renombrada = estado.categorias.single()
        assertEquals(original.id, renombrada.id)
        assertEquals("Domicilios", renombrada.nombre)
    }

    @Test
    fun borrarPideConfirmacionAntes() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.pedirEliminar(estado.categorias.single())

        // Todavía no se borró nada: primero hay que advertir qué pasa con lo clasificado.
        assertNotNull(estado.porEliminar)
        assertEquals(1, estado.categorias.size)
    }

    @Test
    fun cancelarDejaLaCategoriaEnSuSitio() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.pedirEliminar(estado.categorias.single())
        viewModel.cancelarEliminar()
        advanceUntilIdle()

        assertNull(estado.porEliminar)
        assertEquals(1, estado.categorias.size)
    }

    @Test
    fun confirmarLaBorra() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.pedirEliminar(estado.categorias.single())
        viewModel.confirmarEliminar()
        advanceUntilIdle()

        assertNull(estado.porEliminar)
        assertTrue(estado.categorias.isEmpty())
    }

    @Test
    fun cerrarElFormularioNoDejaRastro() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Transporte")
        viewModel.cerrarFormulario()

        assertNull(estado.formulario)
        viewModel.abrirFormularioNuevo()
        assertEquals("", estado.formulario?.nombre)
    }
}
