package com.gestor_ventures.front.ui.finanzas

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.Costo
import com.gestor_ventures.back.model.ErrorCosto
import com.gestor_ventures.back.model.MargenProducto
import com.gestor_ventures.front.util.formatMiles
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * HU-13. Costos del mes y el margen que dejan los productos.
 *
 * El margen va arriba porque es la respuesta; la lista de costos es de dónde sale.
 */
data class CostosUiState(
    val mes: YearMonth = YearMonth.now(),
    val costos: List<CostoUi> = emptyList(),
    val total: Double = 0.0,
    val margenes: List<MargenProducto> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val cargando: Boolean = true,
    val formulario: FormularioCosto? = null,
    /** El costo que el usuario tocó, mientras elige qué hacer con él. */
    val acciones: CostoUi? = null,
    val error: ErrorCosto? = null,
) {
    val vacio: Boolean get() = !cargando && costos.isEmpty()
}

/** Un costo como se muestra en la lista, con el nombre de su categoría ya resuelto. */
data class CostoUi(
    val costo: Costo,
    val categoria: String?,
)

/**
 * Formulario de un costo. [costoId] nulo significa que se está registrando; con id, que se le
 * está corrigiendo algo a uno que ya existe.
 */
data class FormularioCosto(
    val costoId: Long? = null,
    val productoServicio: String = "",
    val monto: String = "",
    val fecha: LocalDateTime = LocalDateTime.now(),
    val categoriaId: Long? = null,
) {
    val esEdicion: Boolean get() = costoId != null

    val montoValor: Long get() = monto.toLongOrNull() ?: 0L

    val montoFormateado: String get() = if (monto.isEmpty()) "" else formatMiles(montoValor)

    val puedeGuardar: Boolean get() = productoServicio.isNotBlank() && montoValor > 0L
}

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorCosto.mensajeRes(): Int = when (this) {
    ErrorCosto.ProductoVacio -> R.string.costo_error_producto
    ErrorCosto.MontoNoPositivo -> R.string.costo_error_monto
    ErrorCosto.FechaEnElFuturo -> R.string.costo_error_fecha_futura
}
