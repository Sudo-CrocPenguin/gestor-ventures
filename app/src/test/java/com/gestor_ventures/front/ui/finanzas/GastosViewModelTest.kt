package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.ErrorGasto
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CategoriaDaoFalso
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-14: los gastos generales del mes en la pestaña de Finanzas. */
@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy = ahora.toLocalDate()
    private val reloj = Reloj { ahora }

    private val gastoDao = GastoDaoFalso()
    private val categoriaDao = CategoriaDaoFalso()
    private val negocioDao = NegocioDaoFalso()

    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val categoriaRepository = CategoriaRepository(categoriaDao)
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        sesionRepositoryDePrueba(),
    )

    private lateinit var viewModel: GastosViewModel

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
        viewModel = GastosViewModel(gastoRepository, categoriaRepository, negocioActivo)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private suspend fun crearCategoria(nombre: String) =
        categoriaRepository.crearCategoria(1L, nombre, TipoCategoria.GASTO)

    @Test
    fun sinGastosLoDiceEnVezDeMostrarUnaListaEnBlanco() = runTest(dispatcher) {
        advanceUntilIdle()

        assertTrue(estado.vacio)
        assertEquals(0.0, estado.total, 0.001)
    }

    @Test
    fun registrarUnGastoLoDejaEnLaListaYSumaAlTotal() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onDescripcionChange("Domicilio de insumos")
        viewModel.onMontoChange("12000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals("Domicilio de insumos", estado.gastos.single().gasto.descripcion)
        assertEquals(12_000.0, estado.total, 0.001)
        // La hoja se cierra sola cuando el gasto quedó guardado.
        assertNull(estado.formulario)
    }

    @Test
    fun elFormularioNuevoProponeLaFechaDeHoy() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()

        assertEquals(hoy, estado.formulario?.fecha)
    }

    @Test
    fun laListaMuestraElNombreDeLaCategoriaYNoSuId() = runTest(dispatcher) {
        crearCategoria("Transporte")
        advanceUntilIdle()
        val categoria = estado.categorias.single()

        viewModel.abrirFormularioNuevo()
        viewModel.onDescripcionChange("Domicilio")
        viewModel.onMontoChange("12000")
        viewModel.onCategoriaChange(categoria.id)
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals("Transporte", estado.gastos.single().categoria)
    }

    @Test
    fun elGastoSePuedeGuardarSinCategoria() = runTest(dispatcher) {
        crearCategoria("Transporte")
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onDescripcionChange("Varios")
        viewModel.onMontoChange("5000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertNull(estado.gastos.single().categoria)
    }

    @Test
    fun volverATocarLaCategoriaElegidaLaQuita() = runTest(dispatcher) {
        crearCategoria("Transporte")
        advanceUntilIdle()
        val categoria = estado.categorias.single()

        viewModel.abrirFormularioNuevo()
        viewModel.onCategoriaChange(categoria.id)
        assertEquals(categoria.id, estado.formulario?.categoriaId)

        viewModel.onCategoriaChange(categoria.id)
        assertNull(estado.formulario?.categoriaId)
    }

    @Test
    fun elFormularioSoloOfreceCategoriasDeGasto() = runTest(dispatcher) {
        crearCategoria("Transporte")
        categoriaRepository.crearCategoria(1L, "Insumos", TipoCategoria.COSTO)
        advanceUntilIdle()

        assertEquals(listOf("Transporte"), estado.categorias.map { it.nombre })
    }

    @Test
    fun elMontoSoloAceptaDigitos() = runTest(dispatcher) {
        viewModel.abrirFormularioNuevo()

        viewModel.onMontoChange("12.000 COP")

        assertEquals("12000", estado.formulario?.monto)
        assertEquals("12.000", estado.formulario?.montoFormateado)
    }

    @Test
    fun noSeGuardaUnGastoSinDescripcionOSinMonto() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onDescripcionChange("Domicilio")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertTrue(estado.gastos.isEmpty())
        assertNotNull(estado.formulario)
    }

    @Test
    fun unaFechaFuturaDejaLaHojaAbiertaConElAviso() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onDescripcionChange("Domicilio")
        viewModel.onMontoChange("12000")
        viewModel.onFechaChange(hoy.plusDays(1))
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(ErrorGasto.FechaEnElFuturo, estado.error)
        assertNotNull(estado.formulario)
        assertTrue(estado.gastos.isEmpty())
    }

    @Test
    fun elFormularioDeEdicionLlegaConTodoElGasto() = runTest(dispatcher) {
        crearCategoria("Transporte")
        advanceUntilIdle()
        val categoria = estado.categorias.single()
        gastoRepository.registrarGasto(1L, "Domicilio", 12_000.0, hoy, categoria.id)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.gastos.single())
        viewModel.editarElGastoElegido()

        val formulario = estado.formulario
        assertEquals("Domicilio", formulario?.descripcion)
        assertEquals("12000", formulario?.monto)
        assertEquals(hoy, formulario?.fecha)
        assertEquals(categoria.id, formulario?.categoriaId)
        assertTrue(formulario?.esEdicion == true)
    }

    @Test
    fun editarCorrigeElGastoEnVezDeCrearOtro() = runTest(dispatcher) {
        gastoRepository.registrarGasto(1L, "Domicilio", 12_000.0, hoy)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.gastos.single())
        viewModel.editarElGastoElegido()
        viewModel.onMontoChange("15000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(1, estado.gastos.size)
        assertEquals(15_000.0, estado.gastos.single().gasto.monto, 0.001)
        assertEquals(15_000.0, estado.total, 0.001)
    }

    @Test
    fun eliminarSacaElGastoYBajaElTotal() = runTest(dispatcher) {
        gastoRepository.registrarGasto(1L, "Domicilio", 12_000.0, hoy)
        gastoRepository.registrarGasto(1L, "Taxi", 8_000.0, hoy)
        advanceUntilIdle()

        // El último registrado va de primero, así que el taxi encabeza la lista.
        val taxi = estado.gastos.first()
        assertEquals("Taxi", taxi.gasto.descripcion)

        viewModel.abrirAcciones(taxi)
        viewModel.eliminarElGastoElegido()
        advanceUntilIdle()

        assertEquals(listOf("Domicilio"), estado.gastos.map { it.gasto.descripcion })
        assertEquals(12_000.0, estado.total, 0.001)
    }

    @Test
    fun tocarUnGastoAbreSusOpcionesYNoLoEditaDeUna() = runTest(dispatcher) {
        gastoRepository.registrarGasto(1L, "Domicilio", 12_000.0, hoy)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.gastos.single())

        assertNotNull(estado.acciones)
        assertNull(estado.formulario)
    }

    @Test
    fun elMenuSeCierraAlElegirUnaOpcion() = runTest(dispatcher) {
        gastoRepository.registrarGasto(1L, "Domicilio", 12_000.0, hoy)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.gastos.single())
        viewModel.editarElGastoElegido()

        // No tiene sentido dejar el menú encima del formulario que acaba de abrir.
        assertNull(estado.acciones)
        assertNotNull(estado.formulario)
    }

    @Test
    fun cerrarElMenuNoTocaElGasto() = runTest(dispatcher) {
        gastoRepository.registrarGasto(1L, "Domicilio", 12_000.0, hoy)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.gastos.single())
        viewModel.cerrarAcciones()
        advanceUntilIdle()

        assertNull(estado.acciones)
        assertEquals(1, estado.gastos.size)
    }

    @Test
    fun cerrarElFormularioNoDejaRastro() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onDescripcionChange("Domicilio")
        viewModel.cerrarFormulario()

        assertNull(estado.formulario)
        viewModel.abrirFormularioNuevo()
        assertEquals("", estado.formulario?.descripcion)
    }
}
