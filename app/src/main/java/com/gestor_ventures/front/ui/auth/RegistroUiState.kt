package com.gestor_ventures.front.ui.auth

/** HU-01. Estado del formulario de creación de cuenta. */
data class RegistroUiState(
    val nombre: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val confirmarContrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val mostrarConfirmarContrasena: Boolean = false,
    val aceptaTerminos: Boolean = false,
) {
    /**
     * De 0 a 3, solo para la barra visual. La regla real (8-25 caracteres, mayúscula,
     * minúscula y símbolo) la exige `AuthRepository` al conectar el front con el back.
     */
    val fortalezaContrasena: Int
        get() {
            var puntos = 0
            if (contrasena.length >= 8) puntos++
            if (contrasena.any { it.isDigit() }) puntos++
            if (contrasena.any { it.isUpperCase() } && contrasena.any { it.isLowerCase() }) puntos++
            return puntos
        }

    val puedeCrearCuenta: Boolean
        get() = nombre.isNotBlank() &&
            correo.isNotBlank() &&
            contrasena.isNotBlank() &&
            contrasena == confirmarContrasena &&
            aceptaTerminos
}
