package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.SesionRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.NegocioDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.enums.MetodoPago
import com.gestor_ventures.db.enums.TipoActividad
import com.gestor_ventures.db.enums.TipoRegistroVenta
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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

/** HU-11/HU-12: criterios de aceptación del registro de ventas. */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrarVentaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val momento = LocalDateTime.of(2026, 9, 12, 9, 45)
    private var ahora = momento
    private val reloj = Reloj { ahora }

    private val ventaDao = VentaDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        SesionRepository(),
    )

    private lateinit var viewModel: RegistrarVentaViewModel

    private val estado get() = viewModel.uiState.value
    private val guardadas get() = ventaDao.ventas.value

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
                fechaCreacion = momento,
            ),
        )
        viewModel = crearViewModel()
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() =
        RegistrarVentaViewModel(VentaRepository(ventaDao, reloj), negocioActivo, reloj)

    @Test
    fun arrancaEnVentaDetallada() {
        assertEquals(TipoRegistroVentaUi.Detallado, estado.tipoRegistro)
        assertTrue(estado.esDetallada)
    }

    @Test
    fun sePuedeAlternarEntreDetalladaYRapida_conservandoElMonto() {
        viewModel.onMontoChange("25000")

        viewModel.onTipoRegistroChange(TipoRegistroVentaUi.Rapido)
        assertFalse(estado.esDetallada)
        assertEquals("25000", estado.monto)

        viewModel.onTipoRegistroChange(TipoRegistroVentaUi.Detallado)
        assertTrue(estado.esDetallada)
        assertEquals("25000", estado.monto)
    }

    @Test
    fun monto_soloAceptaDigitos() {
        viewModel.onMontoChange("25.000 COP")
        assertEquals("25000", estado.monto)
    }

    @Test
    fun monto_seMuestraConSeparadorDeMiles() {
        viewModel.onMontoChange("148500")
        assertEquals("148.500", estado.montoFormateado)
    }

    @Test
    fun monto_vacioNoMuestraCero() {
        viewModel.onMontoChange("")
        assertEquals("", estado.montoFormateado)
    }

    @Test
    fun elMontoDebeSerPositivo() {
        viewModel.onProductoServicioChange("Torta personalizada")
        assertFalse(estado.puedeGuardar)

        viewModel.onMontoChange("0")
        assertFalse(estado.puedeGuardar)

        viewModel.onMontoChange("25000")
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun ventaDetallada_exigeDecirQueSeVendio() {
        viewModel.onMontoChange("25000")
        assertFalse(estado.puedeGuardar)

        viewModel.onProductoServicioChange("   ")
        assertFalse(estado.puedeGuardar)

        viewModel.onProductoServicioChange("Torta personalizada")
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun ventaRapida_soloExigeElTotal() {
        viewModel.onTipoRegistroChange(TipoRegistroVentaUi.Rapido)
        viewModel.onMontoChange("148500")

        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun laNotaEsOpcional() {
        viewModel.onMontoChange("25000")
        viewModel.onProductoServicioChange("Cupcakes x12")

        assertTrue(estado.nota.isEmpty())
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun metodoPago_arrancaEnEfectivoYSoloAceptaLosPredefinidos() {
        assertEquals(MetodoPagoUi.Efectivo, estado.metodoPago)

        viewModel.onMetodoPagoChange(MetodoPagoUi.Transferencia)
        assertEquals(MetodoPagoUi.Transferencia, estado.metodoPago)

        assertEquals(
            listOf(MetodoPagoUi.Efectivo, MetodoPagoUi.Transferencia, MetodoPagoUi.Tarjeta),
            MetodoPagoUi.entries.toList(),
        )
    }

    @Test
    fun guardar_dejaLaVentaDetalladaEnElNegocioActivo() = runTest(dispatcher) {
        viewModel.onMontoChange("25000")
        viewModel.onProductoServicioChange("Torta personalizada")
        viewModel.onMetodoPagoChange(MetodoPagoUi.Transferencia)

        viewModel.guardar()
        advanceUntilIdle()

        val venta = guardadas.single()
        assertEquals(1L, venta.negocioId)
        assertEquals(25_000.0, venta.monto, 0.001)
        assertEquals("Torta personalizada", venta.productoServicio)
        assertEquals(MetodoPago.TRANSFERENCIA, venta.metodoPago)
        assertEquals(TipoRegistroVenta.DETALLADO, venta.tipoRegistro)
    }

    @Test
    fun guardar_laVentaRapidaSeQuedaConElTotalYLaNota() = runTest(dispatcher) {
        viewModel.onTipoRegistroChange(TipoRegistroVentaUi.Rapido)
        viewModel.onMontoChange("148500")
        viewModel.onNotaChange("Ventas del día en el punto")

        viewModel.guardar()
        advanceUntilIdle()

        val venta = guardadas.single()
        assertEquals(148_500.0, venta.monto, 0.001)
        assertEquals("Ventas del día en el punto", venta.nota)
        assertEquals(TipoRegistroVenta.RAPIDO, venta.tipoRegistro)
        assertNull(venta.productoServicio)
    }

    @Test
    fun guardar_sellaLaFechaYHoraDelSistema() = runTest(dispatcher) {
        viewModel.onMontoChange("25000")
        viewModel.onProductoServicioChange("Torta personalizada")
        // El formulario estuvo abierto tres horas: vale la hora de guardar, no la de abrirlo.
        ahora = momento.plusHours(3)

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(momento.plusHours(3), guardadas.single().fechaHora)
    }

    @Test
    fun guardar_dejaElFormularioEnBlancoEnLaMismaModalidad() = runTest(dispatcher) {
        viewModel.onTipoRegistroChange(TipoRegistroVentaUi.Rapido)
        viewModel.onMontoChange("148500")
        viewModel.onNotaChange("Ventas del día")

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(TipoRegistroVentaUi.Rapido, estado.tipoRegistro)
        assertEquals("", estado.monto)
        assertEquals("", estado.nota)
        assertFalse(estado.puedeGuardar)
    }

    @Test
    fun guardar_avisaUnaSolaVezParaQueLaPantallaSeCierre() = runTest(dispatcher) {
        // El aviso queda en el buzón aunque nadie lo esté esperando todavía.
        val aviso = async { viewModel.ventaGuardada.first() }

        viewModel.onTipoRegistroChange(TipoRegistroVentaUi.Rapido)
        viewModel.onMontoChange("148500")
        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(TipoRegistroVentaUi.Rapido, aviso.await())
    }

    @Test
    fun tocarGuardarDosVecesNoRegistraLaVentaDosVeces() = runTest(dispatcher) {
        viewModel.onMontoChange("25000")
        viewModel.onProductoServicioChange("Torta personalizada")

        // Dos toques seguidos, antes de que el primero alcance a terminar.
        viewModel.guardar()
        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(1, guardadas.size)
    }

    @Test
    fun sinNingunNegocioNoSeRegistraLaVentaYSeAvisa() = runTest(dispatcher) {
        negocioDao.negocios.value = emptyList()
        viewModel = crearViewModel()
        viewModel.onMontoChange("25000")
        viewModel.onProductoServicioChange("Torta personalizada")

        viewModel.guardar()
        advanceUntilIdle()

        assertTrue(guardadas.isEmpty())
        assertEquals(ErrorVentaUi.SinNegocio, estado.error)
    }
}
