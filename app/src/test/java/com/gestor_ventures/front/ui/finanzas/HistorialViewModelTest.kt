package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.FiltroMovimientos
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Movimiento
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.back.usecase.ListarMovimientos
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CategoriaDaoFalso
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-17. El historial del negocio activo.
 *
 * Es la pantalla a la que se entra a arreglar algo, así que lo que más importa acá es que no
 * borre nada sin preguntar y que los totales no mientan cuando hay un filtro puesto.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    // Miércoles 16 de septiembre de 2026.
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy: LocalDate = ahora.toLocalDate()
    private val reloj = Reloj { ahora }

    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()
    private val negocioDao = NegocioDaoFalso()

    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)
    private val categoriaRepository = CategoriaRepository(CategoriaDaoFalso())
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        sesionRepositoryDePrueba(),
    )

    private val listarMovimientos = ListarMovimientos(
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = costoRepository,
        reloj = reloj,
    )

    private lateinit var viewModel: HistorialViewModel

    private val estado get() = viewModel.uiState.value

    private var negocioId = 0L

    @Before
    fun prepararPantalla() = runTest {
        Dispatchers.setMain(dispatcher)
        negocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = ahora,
            ),
        )
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private fun abrirPantalla() {
        viewModel = HistorialViewModel(
            listarMovimientos = listarMovimientos,
            ventaRepository = ventaRepository,
            gastoRepository = gastoRepository,
            costoRepository = costoRepository,
            categoriaRepository = categoriaRepository,
            negocioActivoRepository = negocioActivo,
        )
    }

    private suspend fun vender(monto: Double, fecha: LocalDateTime = ahora) =
        ventaRepository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = monto,
            fechaHora = fecha,
            productoServicio = "Torta",
            metodoPago = MetodoPago.EFECTIVO,
        )

    private suspend fun gastar(monto: Double, fecha: LocalDate = hoy) =
        gastoRepository.registrarGasto(negocioId, "Transporte", monto, fecha)

    @Test
    fun elHistorialAbreEnHoy() = runTest(dispatcher) {
        vender(50_000.0)
        vender(30_000.0, ahora.minusDays(1))
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(PeriodoPredefinido.Hoy, estado.periodo)
        assertEquals(1, estado.movimientos.size)
    }

    @Test
    fun cambiarDePeriodoTraeOtrosMovimientos() = runTest(dispatcher) {
        vender(50_000.0)
        vender(30_000.0, ahora.minusDays(2))
        abrirPantalla()
        advanceUntilIdle()
        assertEquals(1, estado.movimientos.size)

        viewModel.onPeriodoChange(PeriodoPredefinido.Semana)
        advanceUntilIdle()

        // Hoy es miércoles, así que el lunes entra en la semana en curso.
        assertEquals(2, estado.movimientos.size)
    }

    @Test
    fun elFiltroEscondeFilasPeroNoCambiaLosTotales() = runTest(dispatcher) {
        vender(50_000.0)
        gastar(20_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onFiltroChange(FiltroMovimientos.Ventas)

        assertEquals(1, estado.movimientos.size)
        // Esconder la mitad de la respuesta a "¿cómo me fue?" sería mentir.
        assertEquals(50_000.0, estado.ingresos, 0.001)
        assertEquals(20_000.0, estado.salidas, 0.001)
    }

    @Test
    fun elFiltroDeSalidasDejaGastosYCostos() = runTest(dispatcher) {
        vender(50_000.0)
        gastar(20_000.0)
        costoRepository.registrarCosto(negocioId, "Harina", 10_000.0, ahora)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onFiltroChange(FiltroMovimientos.Salidas)

        assertEquals(2, estado.movimientos.size)
        assertTrue(estado.movimientos.none { it.entra })
    }

    @Test
    fun unPeriodoVacioSeDistingueDeUnFiltroSinResultados() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onFiltroChange(FiltroMovimientos.Salidas)

        // Hay movimientos, solo que ninguno de este tipo: no es lo mismo y no se dice igual.
        assertTrue(estado.vacio)
        assertFalse(estado.periodoVacio)
    }

    @Test
    fun tocarUnMovimientoAbreSusOpcionesYNoLoBorra() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        advanceUntilIdle()

        assertNotNull(estado.acciones)
        assertNull(estado.porEliminar)
        assertEquals(1, estado.movimientos.size)
    }

    @Test
    fun borrarPasaPorUnaConfirmacion() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.pedirConfirmacionDeEliminar()
        advanceUntilIdle()

        // Del menú se pasa a la pregunta, y la venta sigue ahí mientras tanto.
        assertNull(estado.acciones)
        assertNotNull(estado.porEliminar)
        assertEquals(1, estado.movimientos.size)
    }

    @Test
    fun cancelarLaConfirmacionNoBorraNada() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.pedirConfirmacionDeEliminar()
        viewModel.cancelarEliminar()
        advanceUntilIdle()

        assertNull(estado.porEliminar)
        assertEquals(1, estado.movimientos.size)
    }

    @Test
    fun confirmarBorraLaVentaYSuPlataSaleDeLosTotales() = runTest(dispatcher) {
        vender(50_000.0)
        vender(30_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.first())
        viewModel.pedirConfirmacionDeEliminar()
        viewModel.confirmarEliminar()
        advanceUntilIdle()

        assertEquals(1, estado.movimientos.size)
        assertEquals(50_000.0, estado.ingresos, 0.001)
    }

    @Test
    fun borrarUnGastoLoSacaDeLasSalidas() = runTest(dispatcher) {
        vender(50_000.0)
        gastar(20_000.0)
        abrirPantalla()
        advanceUntilIdle()

        val gasto = estado.movimientos.first { it is Movimiento.DeGasto }
        viewModel.abrirAcciones(gasto)
        viewModel.pedirConfirmacionDeEliminar()
        viewModel.confirmarEliminar()
        advanceUntilIdle()

        assertEquals(0.0, estado.salidas, 0.001)
        assertEquals(50_000.0, estado.ingresos, 0.001)
    }

    // ---------- Corregir ----------

    @Test
    fun corregirAbreElFormularioConLoQueYaEstabaGuardado() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.editarElMovimientoElegido()

        val formulario = estado.formulario as FormularioMovimiento.DeVenta
        assertNull(estado.acciones)
        assertEquals("50000", formulario.campos.monto)
        assertEquals("Torta", formulario.campos.productoServicio)
    }

    @Test
    fun corregirElMontoDeUnaVentaMueveLosTotales() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.editarElMovimientoElegido()
        viewModel.onMontoChange("80000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertNull(estado.formulario)
        assertEquals(1, estado.movimientos.size)
        assertEquals(80_000.0, estado.ingresos, 0.001)
    }

    @Test
    fun corregirLaFechaSacaLaVentaDelDiaQueSeEstaViendo() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.editarElMovimientoElegido()
        viewModel.onFechaChange(hoy.minusDays(1))
        viewModel.guardarFormulario()
        advanceUntilIdle()

        // Registrar hoy una venta de ayer es de lo más común al cerrar la jornada.
        assertTrue(estado.movimientos.isEmpty())

        viewModel.onPeriodoChange(PeriodoPredefinido.Ayer)
        advanceUntilIdle()
        assertEquals(1, estado.movimientos.size)
    }

    @Test
    fun corregirConservaLaHoraOriginal() = runTest(dispatcher) {
        vender(50_000.0, ahora.withHour(8))
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.editarElMovimientoElegido()
        viewModel.onFechaChange(hoy.minusDays(1))
        viewModel.guardarFormulario()
        advanceUntilIdle()

        viewModel.onPeriodoChange(PeriodoPredefinido.Ayer)
        advanceUntilIdle()
        assertEquals(8, estado.movimientos.single().fechaHora.hour)
    }

    @Test
    fun unMontoInvalidoDejaLaHojaAbiertaSinPerderLoEscrito() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.editarElMovimientoElegido()
        viewModel.onMontoChange("")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        // El botón está bloqueado, pero si se llegara a guardar la hoja no se cierra.
        assertNotNull(estado.formulario)
        assertEquals(50_000.0, estado.ingresos, 0.001)
    }

    @Test
    fun corregirUnGastoUsaSuPropioFormulario() = runTest(dispatcher) {
        gastar(20_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.editarElMovimientoElegido()

        val formulario = estado.formulario as FormularioMovimiento.DeGasto
        assertEquals("Transporte", formulario.campos.descripcion)
        assertTrue(formulario.campos.esEdicion)

        viewModel.onTituloChange("Domicilio")
        viewModel.onMontoChange("35000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertNull(estado.formulario)
        assertEquals(35_000.0, estado.salidas, 0.001)
    }

    @Test
    fun corregirUnCostoUsaSuPropioFormulario() = runTest(dispatcher) {
        costoRepository.registrarCosto(negocioId, "Harina", 10_000.0, ahora)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.editarElMovimientoElegido()

        val formulario = estado.formulario as FormularioMovimiento.DeCosto
        assertEquals("Harina", formulario.campos.productoServicio)

        viewModel.onMontoChange("15000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(15_000.0, estado.salidas, 0.001)
    }

    @Test
    fun cerrarElFormularioNoGuardaNada() = runTest(dispatcher) {
        vender(50_000.0)
        abrirPantalla()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.movimientos.single())
        viewModel.editarElMovimientoElegido()
        viewModel.onMontoChange("99000")
        viewModel.cerrarFormulario()
        advanceUntilIdle()

        assertNull(estado.formulario)
        assertEquals(50_000.0, estado.ingresos, 0.001)
    }

    @Test
    fun laHoraSolaBastaEnUnDiaPeroNoEnUnaSemana() = runTest(dispatcher) {
        abrirPantalla()
        advanceUntilIdle()

        assertFalse(estado.variosDias)

        viewModel.onPeriodoChange(PeriodoPredefinido.Semana)
        advanceUntilIdle()

        // "10:24" no dice si fue hoy o el lunes pasado.
        assertTrue(estado.variosDias)
    }
}
