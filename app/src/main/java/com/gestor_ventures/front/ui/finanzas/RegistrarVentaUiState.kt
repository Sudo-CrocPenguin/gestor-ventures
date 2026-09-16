package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.util.formatMiles
import java.time.LocalDateTime

/**
 * HU-11/HU-12. Estado del formulario de venta, en sus dos modalidades.
 *
 * [monto] guarda solo dígitos ("25000"); el formato con puntos se calcula acá para que la
 * pantalla no tenga lógica. [fechaHora] es el momento que quedará registrado: lo pone el
 * sistema, el usuario no lo escribe.
 */
data class RegistrarVentaUiState(
    val tipoRegistro: TipoRegistroVentaUi = TipoRegistroVentaUi.Detallado,
    val monto: String = "",
    val productoServicio: String = "",
    val cliente: String = "",
    val metodoPago: MetodoPagoUi = MetodoPagoUi.Efectivo,
    val nota: String = "",
    val fechaHora: LocalDateTime = LocalDateTime.MIN,
) {
    val esDetallada: Boolean get() = tipoRegistro == TipoRegistroVentaUi.Detallado

    private val montoValor: Long get() = monto.toLongOrNull() ?: 0L

    /** "25000" → "25.000"; vacío mientras no hay nada escrito. */
    val montoFormateado: String get() = if (monto.isEmpty()) "" else formatMiles(montoValor)

    /**
     * El monto siempre es obligatorio y positivo. En la venta detallada también hay que decir
     * qué se vendió; el cliente y la nota son opcionales.
     */
    val puedeGuardar: Boolean
        get() = montoValor > 0L && (!esDetallada || productoServicio.isNotBlank())
}
