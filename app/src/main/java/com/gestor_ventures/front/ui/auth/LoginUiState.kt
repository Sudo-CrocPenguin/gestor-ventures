package com.gestor_ventures.front.ui.auth

import com.gestor_ventures.back.model.ErrorAuth

/** HU-02. Estado del formulario de inicio de sesión. */
data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val recordarme: Boolean = true,
    val cargando: Boolean = false,
    val error: ErrorAuth? = null,
) {
    /** El formato real (correo válido, contraseña no vacía) lo exige `AuthRepository`. */
    val puedeIniciarSesion: Boolean
        get() = !cargando && correo.isNotBlank() && contrasena.isNotBlank()
}
