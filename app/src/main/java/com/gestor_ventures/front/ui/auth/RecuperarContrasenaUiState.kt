package com.gestor_ventures.front.ui.auth

/** HU-03. Estado del formulario para pedir la recuperación de contraseña. */
data class RecuperarContrasenaUiState(
    val correo: String = "",
) {
    val puedeEnviarCodigo: Boolean
        get() = correo.isNotBlank()
}
