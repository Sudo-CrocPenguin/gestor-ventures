package com.gestor_ventures.back.model

import java.time.LocalDateTime

const val MaxCaracteresPregunta = 500

data class MensajeAsistente(
    val id: Long,
    val sesionId: Long,
    val pregunta: String,
    val respuesta: String,
    val fechaHora: LocalDateTime,
)

enum class ErrorAsistente {
    PreguntaVacia,
    PreguntaMuyLarga,
    SinApiKey,
    ApiKeyInvalida,
    SinConexion,
    LimiteAlcanzado,
    ServicioNoDisponible,
    RespuestaVacia,
}

sealed interface ResultadoAsistente {
    data class Respuesta(val mensaje: MensajeAsistente) : ResultadoAsistente
    data class Fallo(val error: ErrorAsistente) : ResultadoAsistente
}
