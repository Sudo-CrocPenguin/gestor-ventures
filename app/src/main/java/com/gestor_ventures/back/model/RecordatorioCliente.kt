package com.gestor_ventures.back.model

import java.time.LocalDateTime

/**
 * HU-32. Un recordatorio que ya se le mandó a un cliente.
 *
 * Guarda el mensaje tal como salió, no la plantilla: el usuario puede corregirlo antes de
 * enviarlo, y lo que importa después es qué le dijo de verdad.
 */
data class RecordatorioCliente(
    val id: Long,
    val clienteId: Long,
    val tipo: TipoRecordatorio,
    val mensaje: String,
    val fechaEnvio: LocalDateTime,
)

/**
 * HU-32. Para qué es el recordatorio.
 *
 * Son los dos que pide la historia. El de cita no necesita una cita de verdad en la agenda
 * (Épica 5): el usuario escribe cuándo es dentro del mensaje, como lo haría por WhatsApp.
 */
enum class TipoRecordatorio { Cita, PagoPendiente }

/** Reglas que debe cumplir un recordatorio (HU-32). */
enum class ErrorRecordatorio {
    /** Un mensaje en blanco no es un recordatorio; es un WhatsApp vacío. */
    MensajeVacio,

    /** Sin teléfono no hay a dónde mandarlo (HU-29, HU-31). */
    ClienteSinTelefono,
}
