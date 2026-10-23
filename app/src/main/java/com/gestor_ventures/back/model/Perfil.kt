package com.gestor_ventures.back.model

/**
 * HU-04. Las reglas del perfil del emprendedor.
 *
 * Viven en `back/model` porque las usan dos capas: el repositorio las aplica siempre al guardar,
 * y la pantalla las usa para avisar antes de que el usuario toque "Guardar".
 */

/** El mismo tope del registro (HU-01): un nombre que se acepta al crear la cuenta se acepta al editarla. */
const val MaxCaracteresNombreUsuario = 50

/** Lo más que puede pesar la foto de perfil: 5 MB. */
const val MaxBytesFotoPerfil = 5L * 1024 * 1024

/**
 * Los formatos de imagen que se aceptan para la foto, con la extensión con la que se guarda
 * cada uno.
 */
val FormatosDeFotoPerfil = mapOf(
    "image/jpeg" to "jpg",
    "image/png" to "png",
    "image/webp" to "webp",
)

/**
 * Lo que puede salir mal al guardar el perfil. Son errores, no mensajes: el texto que ve el
 * usuario lo pone `front/`.
 */
enum class ErrorPerfil {
    NombreVacio,
    NombreMuyLargo,
    TelefonoInvalido,
    FotoFormatoNoSoportado,
    FotoMuyPesada,
    /** La foto no se pudo leer o copiar: el archivo se borró, está dañado o no se tiene acceso. */
    FotoIlegible,
    UsuarioNoExiste,
}

/**
 * Qué se quiere hacer con la foto al guardar.
 *
 * Son tres casos y no un "uri o null" porque null no alcanza a decir si el usuario quitó la foto
 * o si simplemente no la tocó.
 */
sealed interface CambioDeFoto {
    /** Se queda la que ya estaba, o ninguna si no había. */
    data object SinCambios : CambioDeFoto
    /** Se borra la foto y vuelven las iniciales. */
    data object Quitar : CambioDeFoto
    /** Una foto elegida de la galería, todavía sin copiar a la app. */
    data class Nueva(val uri: String) : CambioDeFoto
}
