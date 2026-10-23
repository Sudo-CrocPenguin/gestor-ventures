package com.gestor_ventures.front.ui.auth

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.CambioDeFoto
import com.gestor_ventures.back.model.ErrorPerfil
import com.gestor_ventures.back.model.LargoDeTelefono
import com.gestor_ventures.back.model.MaxCaracteresNombreUsuario

/** HU-04. Estado de la pantalla "Mi perfil". */
data class PerfilUiState(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    /** La foto que ya está guardada en la app; null si no hay. */
    val fotoGuardada: String? = null,
    /** Lo que el usuario hizo con la foto y todavía no guardó. */
    val cambioDeFoto: CambioDeFoto = CambioDeFoto.SinCambios,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: ErrorPerfil? = null,
) {
    /** La foto que se muestra ahora: la nueva si eligió una, ninguna si la quitó, o la guardada. */
    val fotoVisible: String?
        get() = when (val cambio = cambioDeFoto) {
            CambioDeFoto.SinCambios -> fotoGuardada
            CambioDeFoto.Quitar -> null
            is CambioDeFoto.Nueva -> cambio.uri
        }

    /** Para avisar antes de guardar. La regla de verdad la aplica el repositorio. */
    val telefonoInvalido: Boolean
        get() = telefono.isNotEmpty() && telefono.length !in LargoDeTelefono

    val nombreMuyLargo: Boolean get() = nombre.trim().length > MaxCaracteresNombreUsuario

    val puedeGuardar: Boolean
        get() = !cargando && !guardando && nombre.isNotBlank() && !nombreMuyLargo && !telefonoInvalido
}

/** Texto que ve el usuario para cada error del perfil. */
@StringRes
fun ErrorPerfil.mensajeRes(): Int = when (this) {
    ErrorPerfil.NombreVacio -> R.string.auth_error_nombre_vacio
    ErrorPerfil.NombreMuyLargo -> R.string.auth_error_nombre_largo
    ErrorPerfil.TelefonoInvalido -> R.string.perfil_error_telefono
    ErrorPerfil.FotoFormatoNoSoportado -> R.string.perfil_error_foto_formato
    ErrorPerfil.FotoMuyPesada -> R.string.perfil_error_foto_pesada
    ErrorPerfil.FotoIlegible -> R.string.perfil_error_foto_ilegible
    ErrorPerfil.UsuarioNoExiste -> R.string.perfil_error_usuario
}
