package com.gestor_ventures.back.model

import java.time.LocalDateTime

/** Un vendedor del equipo de un negocio. */
data class MiembroEquipo(
    val id: Long,
    val correo: String,
    val estado: EstadoMiembro,
    val fechaInvitacion: LocalDateTime,
)

/** Si el vendedor ya aceptó la invitación o todavía no. */
enum class EstadoMiembro { PENDIENTE, ACTIVO }

/** Por qué no se pudo invitar a alguien. El texto lo pone `front/`. */
enum class ErrorEquipo {
    CorreoInvalido,
    EsElLider,
    YaEstaEnElEquipo,
}
