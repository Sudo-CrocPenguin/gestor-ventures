package com.gestor_ventures.front.ui.auth

/** HU-02. Estado del formulario de inicio de sesión. */
data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val recordarme: Boolean = true,
) {
    /** Solo valida que haya algo escrito: el formato real lo exige el back al conectar. */
    val puedeIniciarSesion: Boolean
        get() = correo.isNotBlank() && contrasena.isNotBlank()
}
