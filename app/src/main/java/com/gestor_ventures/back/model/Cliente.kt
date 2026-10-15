package com.gestor_ventures.back.model

/**
 * HU-29. Alguien que le compra al negocio.
 *
 * Solo el nombre es obligatorio. A mucha gente se le vende sin pedirle nada más, y un
 * formulario que exija teléfono y correo termina llenándose de datos inventados o haciendo que
 * el cliente no se registre.
 */
data class Cliente(
    val id: Long,
    val nombre: String,
    val telefono: String? = null,
    val correo: String? = null,
    val notas: String? = null,
) {
    /** Si se le puede escribir por WhatsApp (HU-31) o mandarle un recordatorio (HU-32). */
    val tieneContacto: Boolean get() = telefono != null || correo != null
}

/** Reglas que debe cumplir un cliente (HU-29). */
enum class ErrorCliente {
    NombreVacio,
    NombreMuyLargo,
    TelefonoInvalido,
    CorreoInvalido,
    TelefonoRepetido,
    CorreoRepetido,
}
