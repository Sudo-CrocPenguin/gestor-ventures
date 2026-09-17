package com.gestor_ventures.front.ui.auth

import com.gestor_ventures.back.model.ErrorAuth

/** HU-01. Estado del formulario de creación de cuenta. */
data class RegistroUiState(
    val nombre: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val confirmarContrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val mostrarConfirmarContrasena: Boolean = false,
    val aceptaTerminos: Boolean = false,
    val cargando: Boolean = false,
    val error: ErrorAuth? = null,
) {
    /**
     * De 0 a 3, solo para la barra visual. La regla real (8-25 caracteres, mayúscula,
     * minúscula y símbolo) la exige `AuthRepository`.
     */
    val fortalezaContrasena: Int
        get() {
            var puntos = 0
            if (contrasena.length >= 8) puntos++
            if (contrasena.any { it.isDigit() }) puntos++
            if (contrasena.any { it.isUpperCase() } && contrasena.any { it.isLowerCase() }) puntos++
            return puntos
        }

    /** Si no coinciden se avisa aparte del error del back, sin gastar una llamada. */
    val contrasenasNoCoinciden: Boolean
        get() = confirmarContrasena.isNotEmpty() && contrasena != confirmarContrasena

    val puedeCrearCuenta: Boolean
        get() = !cargando &&
            nombre.isNotBlank() &&
            correo.isNotBlank() &&
            contrasena.isNotBlank() &&
            contrasena == confirmarContrasena &&
            aceptaTerminos
}
