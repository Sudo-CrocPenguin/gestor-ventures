package com.gestor_ventures.back.model

const val MaxCaracteresNombreUsuario = 50

const val MaxBytesFotoPerfil = 5L * 1024 * 1024

val FormatosDeFotoPerfil = mapOf(
    "image/jpeg" to "jpg",
    "image/png" to "png",
    "image/webp" to "webp",
)

enum class ErrorPerfil {
    NombreVacio,
    NombreMuyLargo,
    TelefonoInvalido,
    FotoFormatoNoSoportado,
    FotoMuyPesada,
    FotoIlegible,
    UsuarioNoExiste,
}

sealed interface CambioDeFoto {
    data object SinCambios : CambioDeFoto
    data object Quitar : CambioDeFoto
    data class Nueva(val uri: String) : CambioDeFoto
}
