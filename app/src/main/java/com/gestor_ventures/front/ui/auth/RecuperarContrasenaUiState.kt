package com.gestor_ventures.front.ui.auth

import com.gestor_ventures.back.model.ErrorAuth

/** HU-03. Estado del formulario para pedir la recuperación de contraseña. */
data class RecuperarContrasenaUiState(
    val correo: String = "",
    val enviando: Boolean = false,
    val enviado: Boolean = false,
    val error: ErrorAuth? = null,
) {
    val puedeEnviarCodigo: Boolean
        get() = !enviando && correo.isNotBlank()
}
