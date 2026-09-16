package com.gestor_ventures.front.ui.finanzas

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.ErrorCategoria
import com.gestor_ventures.back.model.TipoCategoria
import java.time.YearMonth

/**
 * HU-15. Categorías del negocio, separadas en gastos y costos porque son dos cuentas
 * distintas: el usuario mira una a la vez.
 */
data class CategoriasUiState(
    val mes: YearMonth = YearMonth.now(),
    val tipo: TipoCategoria = TipoCategoria.GASTO,
    val categorias: List<CategoriaUi> = emptyList(),
    /** Lo que se registró este mes sin clasificar. Cero cuando está todo clasificado. */
    val sinClasificar: Double = 0.0,
    val cargando: Boolean = true,
    val formulario: FormularioCategoria? = null,
    /** La categoria que el usuario toco, mientras elige que hacer con ella. */
    val acciones: Categoria? = null,
    /** La categoría que el usuario pidió borrar, mientras confirma. */
    val porEliminar: Categoria? = null,
    val error: ErrorCategoria? = null,
) {
    val vacio: Boolean get() = !cargando && categorias.isEmpty()

    /** Lo acumulado este mes en todas las categorías, más lo que quedó sin clasificar. */
    val totalDelMes: Double get() = categorias.sumOf { it.total } + sinClasificar
}

/**
 * HU-15. Una categoría con lo que lleva acumulado en el mes: es el resumen agrupado que pide la
 * historia, puesto donde tiene sentido mirarlo.
 */
data class CategoriaUi(
    val categoria: Categoria,
    val total: Double,
)

/**
 * Formulario de una categoría. [categoriaId] nulo significa que se está creando; con id, que se
 * le está corrigiendo el nombre a una que ya existe.
 */
data class FormularioCategoria(
    val categoriaId: Long? = null,
    val nombre: String = "",
) {
    val esEdicion: Boolean get() = categoriaId != null

    val puedeGuardar: Boolean get() = nombre.isNotBlank()
}

/** Cómo se llama cada tipo en pantalla. */
@StringRes
fun TipoCategoria.labelRes(): Int = when (this) {
    TipoCategoria.GASTO -> R.string.categoria_tipo_gasto
    TipoCategoria.COSTO -> R.string.categoria_tipo_costo
}

/** Qué significa cada tipo, para que el usuario sepa en cuál de las dos pestañas está. */
@StringRes
fun TipoCategoria.explicacionRes(): Int = when (this) {
    TipoCategoria.GASTO -> R.string.categoria_tipo_gasto_ayuda
    TipoCategoria.COSTO -> R.string.categoria_tipo_costo_ayuda
}

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorCategoria.mensajeRes(): Int = when (this) {
    ErrorCategoria.NombreVacio -> R.string.categoria_error_nombre_vacio
    ErrorCategoria.NombreMuyLargo -> R.string.categoria_error_nombre_largo
    ErrorCategoria.NombreRepetido -> R.string.categoria_error_repetida
}
