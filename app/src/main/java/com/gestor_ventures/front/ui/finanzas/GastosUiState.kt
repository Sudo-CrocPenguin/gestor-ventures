package com.gestor_ventures.front.ui.finanzas

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.ErrorGasto
import com.gestor_ventures.back.model.Gasto
import com.gestor_ventures.front.util.formatMiles
import java.time.LocalDate
import java.time.YearMonth

/**
 * HU-14. Gastos generales del mes, con las categorías disponibles para clasificarlos.
 *
 * El total no se recalcula acá: llega del repositorio, que es el que sabe qué entra en el mes.
 */
data class GastosUiState(
    val mes: YearMonth = YearMonth.now(),
    val gastos: List<GastoUi> = emptyList(),
    val total: Double = 0.0,
    val categorias: List<Categoria> = emptyList(),
    val cargando: Boolean = true,
    val formulario: FormularioGasto? = null,
    /** El gasto que el usuario tocó, mientras elige qué hacer con él. */
    val acciones: GastoUi? = null,
    val error: ErrorGasto? = null,
) {
    val vacio: Boolean get() = !cargando && gastos.isEmpty()
}

/**
 * Un gasto como se muestra en la lista: con el nombre de su categoría ya resuelto, porque la
 * fila no tiene por qué ir a buscarlo.
 */
data class GastoUi(
    val gasto: Gasto,
    val categoria: String?,
)

/**
 * Formulario de un gasto. [gastoId] nulo significa que se está registrando; con id, que se le
 * está corrigiendo algo a uno que ya existe.
 */
data class FormularioGasto(
    val gastoId: Long? = null,
    val descripcion: String = "",
    val monto: String = "",
    val fecha: LocalDate = LocalDate.now(),
    val categoriaId: Long? = null,
) {
    val esEdicion: Boolean get() = gastoId != null

    val montoValor: Long get() = monto.toLongOrNull() ?: 0L

    val montoFormateado: String get() = if (monto.isEmpty()) "" else formatMiles(montoValor)

    /** La categoría es opcional: un gasto sin clasificar es mejor que un gasto sin registrar. */
    val puedeGuardar: Boolean get() = descripcion.isNotBlank() && montoValor > 0L
}

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorGasto.mensajeRes(): Int = when (this) {
    ErrorGasto.DescripcionVacia -> R.string.gasto_error_descripcion
    ErrorGasto.MontoNoPositivo -> R.string.gasto_error_monto
    ErrorGasto.FechaEnElFuturo -> R.string.gasto_error_fecha_futura
}
