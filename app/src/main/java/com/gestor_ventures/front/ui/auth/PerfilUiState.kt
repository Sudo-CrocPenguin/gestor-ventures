package com.gestor_ventures.front.ui.auth

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.CambioDeFoto
import com.gestor_ventures.back.model.ErrorPerfil
import com.gestor_ventures.back.model.LargoDeTelefono
import com.gestor_ventures.back.model.MaxCaracteresNombreUsuario

data class PerfilUiState(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val fotoGuardada: String? = null,
    val cambioDeFoto: CambioDeFoto = CambioDeFoto.SinCambios,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: ErrorPerfil? = null,
) {
    val fotoVisible: String?
        get() = when (val cambio = cambioDeFoto) {
            CambioDeFoto.SinCambios -> fotoGuardada
            CambioDeFoto.Quitar -> null
            is CambioDeFoto.Nueva -> cambio.uri
        }

    val telefonoInvalido: Boolean
        get() = telefono.isNotEmpty() && telefono.length !in LargoDeTelefono

    val nombreMuyLargo: Boolean get() = nombre.trim().length > MaxCaracteresNombreUsuario

    val puedeGuardar: Boolean
        get() = !cargando && !guardando && nombre.isNotBlank() && !nombreMuyLargo && !telefonoInvalido
}

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
