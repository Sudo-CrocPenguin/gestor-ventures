package com.gestor_ventures.front.ui.auth

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorAuth

/** Texto que ve el usuario para cada regla que rechaza `AuthRepository`. */
@StringRes
fun ErrorAuth.mensajeRes(): Int = when (this) {
    ErrorAuth.NombreVacio -> R.string.auth_error_nombre_vacio
    ErrorAuth.NombreMuyLargo -> R.string.auth_error_nombre_largo
    ErrorAuth.CorreoInvalido -> R.string.auth_error_correo_invalido
    ErrorAuth.CorreoYaRegistrado -> R.string.auth_error_correo_ya_registrado
    ErrorAuth.ContrasenaInvalida -> R.string.auth_error_contrasena_invalida
    ErrorAuth.CredencialesInvalidas -> R.string.auth_error_credenciales_invalidas
    ErrorAuth.CorreoNoRegistrado -> R.string.auth_error_correo_no_registrado
    ErrorAuth.ErrorDeRed -> R.string.auth_error_red
}
