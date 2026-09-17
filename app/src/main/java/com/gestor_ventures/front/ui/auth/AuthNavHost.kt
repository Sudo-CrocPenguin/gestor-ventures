package com.gestor_ventures.front.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Rutas del flujo de autenticación (HU-01, HU-02, HU-03).
 *
 * Vive aparte de [com.gestor_ventures.front.navigation.AppNavHost] a propósito: todavía no está
 * conectado a `AuthRepository`/`SesionRepository`, así que no reemplaza el arranque real de la
 * app. Sirve para ver y probar las tres pantallas tal como quedaron en el diseño.
 */
private object AuthRutas {
    const val Login = "auth_login"
    const val Registro = "auth_registro"
    const val RecuperarContrasena = "auth_recuperar_contrasena"
}

@Composable
fun AuthNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthRutas.Login) {
        composable(AuthRutas.Login) {
            LoginRoute(
                onIniciarSesion = {},
                onOlvidasteContrasena = { navController.navigate(AuthRutas.RecuperarContrasena) },
                onCrearCuenta = { navController.navigate(AuthRutas.Registro) },
            )
        }
        composable(AuthRutas.Registro) {
            RegistroRoute(
                onCuentaCreada = {},
                onIniciarSesion = { navController.popBackStack() },
            )
        }
        composable(AuthRutas.RecuperarContrasena) {
            RecuperarContrasenaRoute(
                onBack = { navController.popBackStack() },
                onCodigoEnviado = {},
            )
        }
    }
}
