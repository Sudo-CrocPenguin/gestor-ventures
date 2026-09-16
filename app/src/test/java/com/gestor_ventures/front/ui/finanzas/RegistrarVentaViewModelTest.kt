package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** HU-11/HU-12: criterios de aceptación del registro de ventas. */
class RegistrarVentaViewModelTest {

    private val momento = LocalDateTime.of(2026, 9, 12, 9, 45)
    private var reloj = momento
    private val viewModel = RegistrarVentaViewModel(ahora = { reloj })

    private val estado get() = viewModel.uiState.value

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
    fun clienteYNota_sonOpcionales() {
        viewModel.onMontoChange("25000")
        viewModel.onProductoServicioChange("Cupcakes x12")

        assertTrue(estado.cliente.isEmpty())
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
    fun guardar_sellaLaFechaYHoraDelSistema() {
        viewModel.onMontoChange("25000")
        viewModel.onProductoServicioChange("Torta personalizada")
        reloj = momento.plusHours(3)

        viewModel.guardar()

        assertEquals(momento.plusHours(3), estado.fechaHora)
    }

    @Test
    fun guardar_dejaElFormularioEnBlancoEnLaMismaModalidad() {
        viewModel.onTipoRegistroChange(TipoRegistroVentaUi.Rapido)
        viewModel.onMontoChange("148500")
        viewModel.onNotaChange("Ventas del día")

        viewModel.guardar()

        assertEquals(TipoRegistroVentaUi.Rapido, estado.tipoRegistro)
        assertEquals("", estado.monto)
        assertEquals("", estado.nota)
        assertFalse(estado.puedeGuardar)
    }
}
