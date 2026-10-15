package com.gestor_ventures.front.ui.finanzas

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorVenta
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
    val metodoPago: MetodoPagoUi = MetodoPagoUi.Efectivo,
    val nota: String = "",
    val fechaHora: LocalDateTime = LocalDateTime.MIN,
    val guardando: Boolean = false,
    val error: ErrorVentaUi? = null,
) {
    val esDetallada: Boolean get() = tipoRegistro == TipoRegistroVentaUi.Detallado

    val montoValor: Long get() = monto.toLongOrNull() ?: 0L

    /** "25000" → "25.000"; vacío mientras no hay nada escrito. */
    val montoFormateado: String get() = if (monto.isEmpty()) "" else formatMiles(montoValor)

    /**
     * El monto siempre es obligatorio y positivo. En la venta detallada también hay que decir
     * qué se vendió; la nota es opcional.
     */
    val puedeGuardar: Boolean
        get() = !guardando && montoValor > 0L && (!esDetallada || productoServicio.isNotBlank())
}

/**
 * Lo que puede salir mal al guardar, visto desde la pantalla.
 *
 * Las reglas del negocio no deberían llegar acá nunca, porque el botón no se habilita hasta
 * que el formulario está completo; son la red por si alguna vez se abre otro camino.
 */
sealed interface ErrorVentaUi {

    data class Regla(val error: ErrorVenta) : ErrorVentaUi

    /** Sin negocio no hay a quién apuntarle la venta; pasa si se borró el último. */
    data object SinNegocio : ErrorVentaUi
}

/** Texto que ve el usuario para cada cosa que puede fallar. */
@StringRes
fun ErrorVentaUi.mensajeRes(): Int = when (this) {
    is ErrorVentaUi.SinNegocio -> R.string.venta_error_sin_negocio
    is ErrorVentaUi.Regla -> when (error) {
        ErrorVenta.MontoNoPositivo -> R.string.venta_error_monto
        ErrorVenta.ProductoVacio -> R.string.venta_error_producto
        ErrorVenta.MetodoPagoFaltante -> R.string.venta_error_metodo_pago
        ErrorVenta.FechaEnElFuturo -> R.string.venta_error_fecha_futura
    }
}
