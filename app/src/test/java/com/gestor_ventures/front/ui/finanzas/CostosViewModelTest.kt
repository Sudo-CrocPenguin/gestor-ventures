package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.ErrorCosto
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.usecase.CalcularMargen
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CategoriaDaoFalso
import com.gestor_ventures.db.dao.CostoDaoFalso
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-13: los costos del mes y el margen que dejan los productos. */
@OptIn(ExperimentalCoroutinesApi::class)
class CostosViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val reloj = Reloj { ahora }

    private val costoDao = CostoDaoFalso()
    private val ventaDao = VentaDaoFalso()
    private val categoriaDao = CategoriaDaoFalso()
    private val negocioDao = NegocioDaoFalso()

    private val costoRepository = CostoRepository(costoDao, reloj)
    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val categoriaRepository = CategoriaRepository(categoriaDao)
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        sesionRepositoryDePrueba(),
    )

    private lateinit var viewModel: CostosViewModel

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
        viewModel = CostosViewModel(
            costoRepository,
            categoriaRepository,
            negocioActivo,
            CalcularMargen(ventaRepository, costoRepository),
        )
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private suspend fun vender(producto: String, monto: Double) = ventaRepository.registrarVenta(
        negocioId = 1L,
        tipoRegistro = TipoRegistroVenta.DETALLADO,
        monto = monto,
        fechaHora = ahora,
        productoServicio = producto,
        metodoPago = MetodoPago.EFECTIVO,
    )

    @Test
    fun sinCostosLoDiceEnVezDeMostrarUnaListaEnBlanco() = runTest(dispatcher) {
        advanceUntilIdle()

        assertTrue(estado.vacio)
        assertEquals(0.0, estado.total, 0.001)
        assertTrue(estado.margenes.isEmpty())
    }

    @Test
    fun registrarUnCostoLoDejaEnLaListaYSumaAlTotal() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onProductoChange("Torta de chocolate")
        viewModel.onMontoChange("18000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals("Torta de chocolate", estado.costos.single().costo.productoServicio)
        assertEquals(18_000.0, estado.total, 0.001)
        assertNull(estado.formulario)
    }

    @Test
    fun elFormularioNuevoProponeLaFechaDeHoy() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()

        assertEquals(ahora, estado.formulario?.fecha)
    }

    @Test
    fun elMargenAparaceCuandoHayVentaYCosto() = runTest(dispatcher) {
        vender("Torta de chocolate", 45_000.0)
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onProductoChange("torta de chocolate")
        viewModel.onMontoChange("18000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        val margen = estado.margenes.single()
        assertEquals(45_000.0, margen.vendido, 0.001)
        assertEquals(27_000.0, margen.margen, 0.001)
    }

    @Test
    fun elFormularioSoloOfreceCategoriasDeCosto() = runTest(dispatcher) {
        categoriaRepository.crearCategoria(1L, "Insumos", TipoCategoria.COSTO)
        categoriaRepository.crearCategoria(1L, "Transporte", TipoCategoria.GASTO)
        advanceUntilIdle()

        assertEquals(listOf("Insumos"), estado.categorias.map { it.nombre })
    }

    @Test
    fun elMontoSoloAceptaDigitos() = runTest(dispatcher) {
        viewModel.abrirFormularioNuevo()

        viewModel.onMontoChange("18.000 COP")

        assertEquals("18000", estado.formulario?.monto)
        assertEquals("18.000", estado.formulario?.montoFormateado)
    }

    @Test
    fun unaFechaFuturaDejaLaHojaAbiertaConElAviso() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onProductoChange("Torta")
        viewModel.onMontoChange("18000")
        viewModel.onFechaChange(ahora.plusDays(1))
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(ErrorCosto.FechaEnElFuturo, estado.error)
        assertNotNull(estado.formulario)
        assertTrue(estado.costos.isEmpty())
    }

    @Test
    fun tocarUnCostoAbreSusOpcionesYNoLoEditaDeUna() = runTest(dispatcher) {
        costoRepository.registrarCosto(1L, "Torta", 18_000.0, ahora)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.costos.single())

        assertNotNull(estado.acciones)
        assertNull(estado.formulario)
    }

    @Test
    fun elegirEditarAbreElFormularioLleno() = runTest(dispatcher) {
        costoRepository.registrarCosto(1L, "Torta", 18_000.0, ahora)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.costos.single())
        viewModel.editarElCostoElegido()

        assertNull(estado.acciones)
        assertEquals("Torta", estado.formulario?.productoServicio)
        assertEquals("18000", estado.formulario?.monto)
    }

    @Test
    fun editarCorrigeElCostoEnVezDeCrearOtro() = runTest(dispatcher) {
        costoRepository.registrarCosto(1L, "Torta", 18_000.0, ahora)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.costos.single())
        viewModel.editarElCostoElegido()
        viewModel.onMontoChange("20000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(1, estado.costos.size)
        assertEquals(20_000.0, estado.total, 0.001)
    }

    @Test
    fun eliminarSacaElCostoYBajaElTotal() = runTest(dispatcher) {
        costoRepository.registrarCosto(1L, "Torta", 18_000.0, ahora)
        costoRepository.registrarCosto(1L, "Galletas", 8_000.0, ahora)
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.costos.first())
        viewModel.eliminarElCostoElegido()
        advanceUntilIdle()

        assertEquals(1, estado.costos.size)
        assertEquals(18_000.0, estado.total, 0.001)
    }

    @Test
    fun cerrarElFormularioNoDejaRastro() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onProductoChange("Torta")
        viewModel.cerrarFormulario()

        assertNull(estado.formulario)
        viewModel.abrirFormularioNuevo()
        assertEquals("", estado.formulario?.productoServicio)
    }
}
