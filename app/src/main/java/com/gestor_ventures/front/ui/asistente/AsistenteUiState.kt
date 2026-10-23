package com.gestor_ventures.front.ui.asistente

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorAsistente
import com.gestor_ventures.back.model.MaxCaracteresPregunta
import com.gestor_ventures.back.model.MensajeAsistente

/** HU-34. Estado de la pestaña Asistente. */
data class AsistenteUiState(
    val mensajes: List<MensajeAsistente> = emptyList(),
    val pregunta: String = "",
    /** La pregunta que ya se mandó y espera respuesta. Se muestra en el chat mientras tanto. */
    val preguntaEnCurso: String? = null,
    val error: ErrorAsistente? = null,
    val sinNegocio: Boolean = false,
) {
    val enviando: Boolean get() = preguntaEnCurso != null

    val conversacionVacia: Boolean get() = mensajes.isEmpty() && preguntaEnCurso == null

    val puedeEnviar: Boolean
        get() = !enviando && !sinNegocio && pregunta.isNotBlank() && pregunta.length <= MaxCaracteresPregunta
}

/** Ejemplos para la primera pregunta: muestran qué se le puede preguntar. */
val preguntasSugeridas = listOf(
    R.string.asistente_sugerencia_semana,
    R.string.asistente_sugerencia_mes,
    R.string.asistente_sugerencia_dia,
    R.string.asistente_sugerencia_producto,
)

/** Texto que ve el usuario para cada error del asistente. */
@StringRes
fun ErrorAsistente.mensajeRes(): Int = when (this) {
    ErrorAsistente.PreguntaVacia -> R.string.asistente_error_vacia
    ErrorAsistente.PreguntaMuyLarga -> R.string.asistente_error_larga
    ErrorAsistente.SinApiKey -> R.string.asistente_error_sin_api_key
    ErrorAsistente.ApiKeyInvalida -> R.string.asistente_error_api_key
    ErrorAsistente.SinConexion -> R.string.asistente_error_conexion
    ErrorAsistente.LimiteAlcanzado -> R.string.asistente_error_limite
    ErrorAsistente.ServicioNoDisponible -> R.string.asistente_error_servicio
    ErrorAsistente.RespuestaVacia -> R.string.asistente_error_respuesta_vacia
}
