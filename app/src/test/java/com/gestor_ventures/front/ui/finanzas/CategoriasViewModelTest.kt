package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.ErrorCategoria
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.db.dao.CategoriaDaoFalso
import com.gestor_ventures.db.dao.CostoDaoFalso
import com.gestor_ventures.db.dao.GastoDaoFalso
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
    private val hoy = ahora.toLocalDate()

    private val categoriaDao = CategoriaDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val repository = CategoriaRepository(categoriaDao)
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()
    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        sesionRepositoryDePrueba(),
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
        viewModel = CategoriasViewModel(repository, gastoRepository, costoRepository, negocioActivo)
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

        assertEquals(listOf("Transporte"), estado.categorias.map { it.categoria.nombre })

        viewModel.onTipoChange(TipoCategoria.COSTO)
        advanceUntilIdle()

        assertEquals(listOf("Insumos"), estado.categorias.map { it.categoria.nombre })
        assertFalse(estado.vacio)
    }

    @Test
    fun cadaCategoriaMuestraLoQueLlevaEnElMes() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        crear("Papelería", TipoCategoria.GASTO)
        advanceUntilIdle()
        val transporte = estado.categorias.first { it.categoria.nombre == "Transporte" }.categoria
        gastoRepository.registrarGasto(1L, "Taxi", 8_000.0, hoy, transporte.id)
        gastoRepository.registrarGasto(1L, "Domicilio", 12_000.0, hoy, transporte.id)
        advanceUntilIdle()

        val porNombre = estado.categorias.associate { it.categoria.nombre to it.total }
        assertEquals(20_000.0, porNombre["Transporte"] ?: 0.0, 0.001)
        // Una categoría sin movimientos existe igual, en cero.
        assertEquals(0.0, porNombre["Papelería"] ?: -1.0, 0.001)
    }

    @Test
    fun loQueNadieClasificoSeMuestraAparte() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()
        gastoRepository.registrarGasto(1L, "Varios", 5_000.0, hoy, categoriaId = null)
        advanceUntilIdle()

        assertEquals(5_000.0, estado.sinClasificar, 0.001)
        assertEquals(5_000.0, estado.totalDelMes, 0.001)
    }

    @Test
    fun cadaPestanaResumeLoSuyo() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        crear("Insumos", TipoCategoria.COSTO)
        advanceUntilIdle()
        val transporte = estado.categorias.single().categoria
        gastoRepository.registrarGasto(1L, "Taxi", 8_000.0, hoy, transporte.id)
        advanceUntilIdle()

        assertEquals(8_000.0, estado.categorias.single().total, 0.001)

        viewModel.onTipoChange(TipoCategoria.COSTO)
        advanceUntilIdle()

        // Los gastos no se cuelan en el resumen de costos.
        assertEquals(0.0, estado.categorias.single().total, 0.001)
    }

    @Test
    fun laCategoriaNuevaSeCreaEnLaPestanaEnLaQueEstaElUsuario() = runTest(dispatcher) {
        viewModel.onTipoChange(TipoCategoria.COSTO)
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Insumos")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(TipoCategoria.COSTO, estado.categorias.single().categoria.tipo)
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

        viewModel.abrirFormularioDe(estado.categorias.single().categoria)

        assertEquals("Transporte", estado.formulario?.nombre)
        assertTrue(estado.formulario?.esEdicion == true)
    }

    @Test
    fun renombrarCorrigeLaCategoriaEnVezDeCrearOtra() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()
        val original = estado.categorias.single().categoria

        viewModel.abrirFormularioDe(original)
        viewModel.onNombreChange("Domicilios")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        val renombrada = estado.categorias.single().categoria
        assertEquals(original.id, renombrada.id)
        assertEquals("Domicilios", renombrada.nombre)
    }

    @Test
    fun tocarUnaCategoriaAbreSusOpcionesYNoLaEditaDeUna() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.categorias.single().categoria)

        assertNotNull(estado.acciones)
        assertNull(estado.formulario)
    }

    @Test
    fun elegirEditarAbreElFormularioYCierraElMenu() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.categorias.single().categoria)
        viewModel.editarLaElegida()

        assertNull(estado.acciones)
        assertEquals("Transporte", estado.formulario?.nombre)
    }

    @Test
    fun elegirEliminarPideConfirmacionEnVezDeBorrar() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.categorias.single().categoria)
        viewModel.eliminarLaElegida()
        advanceUntilIdle()

        // El menú se cierra, pero todavía hay que advertir qué pasa con lo clasificado.
        assertNull(estado.acciones)
        assertNotNull(estado.porEliminar)
        assertEquals(1, estado.categorias.size)
    }

    @Test
    fun borrarPideConfirmacionAntes() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.pedirEliminar(estado.categorias.single().categoria)

        // Todavía no se borró nada: primero hay que advertir qué pasa con lo clasificado.
        assertNotNull(estado.porEliminar)
        assertEquals(1, estado.categorias.size)
    }

    @Test
    fun cancelarDejaLaCategoriaEnSuSitio() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.pedirEliminar(estado.categorias.single().categoria)
        viewModel.cancelarEliminar()
        advanceUntilIdle()

        assertNull(estado.porEliminar)
        assertEquals(1, estado.categorias.size)
    }

    @Test
    fun confirmarLaBorra() = runTest(dispatcher) {
        crear("Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        viewModel.pedirEliminar(estado.categorias.single().categoria)
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
