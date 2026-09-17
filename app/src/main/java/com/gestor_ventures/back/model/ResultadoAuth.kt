package com.gestor_ventures.back.model

/** Resultado de registrar, iniciar sesión o recuperar la contraseña (HU-01, HU-02, HU-03). */
sealed interface ResultadoAuth {

    /** [usuarioId] es nulo en HU-03: recuperar contraseña no abre sesión. */
    data class Exito(val usuarioId: Long? = null) : ResultadoAuth

    data class Invalido(val error: ErrorAuth) : ResultadoAuth
}

/**
 * Reglas que debe cumplir una cuenta. `AuthRepository` las valida siempre, aunque la pantalla
 * ya avise antes: así ninguna ruta de la app puede mandar datos inválidos a Firebase.
 *
 * Son errores, no mensajes: el texto que ve el usuario lo pone `front/`.
 */
enum class ErrorAuth {
    NombreVacio,
    NombreMuyLargo,
    CorreoInvalido,
    CorreoYaRegistrado,
    ContrasenaInvalida,
    CredencialesInvalidas,
    CorreoNoRegistrado,
    ErrorDeRed,
}
