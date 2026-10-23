package com.gestor_ventures.front.ui.negocio

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorNegocio
import com.gestor_ventures.back.model.TipoActividad

/** HU-05. Estado del formulario de registrar negocio. */
data class RegistrarNegocioUiState(
    val nombre: String = "",
    val categoria: String = "",
    val tipoActividad: TipoActividad = TipoActividad.PRODUCTOS,
    /** Hexadecimal del color de marca; null es el azul propio de la app. */
    val colorMarca: String? = null,
    val guardando: Boolean = false,
    val error: ErrorNegocio? = null,
) {
    /** El botón se habilita cuando hay nombre y actividad; el resto lo valida el repositorio. */
    val puedeGuardar: Boolean
        get() = !guardando && nombre.isNotBlank() && categoria.isNotBlank()
}

/** Categorías sugeridas del mockup, para llenar el campo de un toque. */
val categoriasSugeridas = listOf(
    R.string.negocio_sugerencia_reposteria,
    R.string.negocio_sugerencia_belleza,
    R.string.negocio_sugerencia_ropa,
    R.string.negocio_sugerencia_comida,
    R.string.negocio_sugerencia_servicios,
    R.string.negocio_sugerencia_otro,
)

/**
 * Cómo se muestra en pantalla cada tipo de actividad del dominio. Evita repetir el enum:
 * aquí solo vive su presentación.
 */
data class TipoActividadUi(
    val tipo: TipoActividad,
    @param:StringRes val tituloRes: Int,
    @param:StringRes val descripcionRes: Int,
    @param:DrawableRes val iconRes: Int,
)

val tiposDeActividad = listOf(
    TipoActividadUi(
        tipo = TipoActividad.SERVICIOS,
        tituloRes = R.string.negocio_actividad_servicios,
        descripcionRes = R.string.negocio_actividad_servicios_desc,
        iconRes = R.drawable.ic_calendar,
    ),
    TipoActividadUi(
        tipo = TipoActividad.PRODUCTOS,
        tituloRes = R.string.negocio_actividad_productos,
        descripcionRes = R.string.negocio_actividad_productos_desc,
        iconRes = R.drawable.ic_box,
    ),
    TipoActividadUi(
        tipo = TipoActividad.MIXTO,
        tituloRes = R.string.negocio_actividad_mixto,
        descripcionRes = R.string.negocio_actividad_mixto_desc,
        iconRes = R.drawable.ic_list,
    ),
)

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorNegocio.mensajeRes(): Int = when (this) {
    ErrorNegocio.NombreVacio -> R.string.negocio_error_nombre_vacio
    ErrorNegocio.NombreMuyLargo -> R.string.negocio_error_nombre_largo
    ErrorNegocio.CategoriaVacia -> R.string.negocio_error_categoria_vacia
    ErrorNegocio.PorcentajeFueraDeRango -> R.string.negocio_error_porcentaje
    ErrorNegocio.NegocioNoExiste -> R.string.negocio_error_no_existe
}
