package com.gestor_ventures.back.model

import java.time.LocalDateTime

/** HU-34. El asistente de IA: las preguntas, sus respuestas y lo que puede salir mal. */

/** Una pregunta más larga ya no es una pregunta: es un texto que el asistente no necesita. */
const val MaxCaracteresPregunta = 500

/** Una pregunta con su respuesta, dentro de una conversación ([sesionId]). */
data class MensajeAsistente(
    val id: Long,
    val sesionId: Long,
    val pregunta: String,
    val respuesta: String,
    val fechaHora: LocalDateTime,
)

/** Por qué no hubo respuesta. Son errores, no mensajes: el texto lo pone `front/`. */
enum class ErrorAsistente {
    PreguntaVacia,
    PreguntaMuyLarga,
    /** La clave de Gemini no está en `local.properties`. */
    SinApiKey,
    ApiKeyInvalida,
    SinConexion,
    /** El nivel gratuito de Gemini limita cuántas preguntas por minuto se pueden hacer. */
    LimiteAlcanzado,
    ServicioNoDisponible,
    RespuestaVacia,
}

/** La respuesta del asistente, o por qué no la hubo. */
sealed interface ResultadoAsistente {
    data class Respuesta(val mensaje: MensajeAsistente) : ResultadoAsistente
    data class Fallo(val error: ErrorAsistente) : ResultadoAsistente
}
