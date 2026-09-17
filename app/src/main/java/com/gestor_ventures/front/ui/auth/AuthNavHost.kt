package com.gestor_ventures.front.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Rutas del flujo de autenticación (HU-01, HU-02, HU-03).
 *
 * Ya habla con `AuthRepository`. Al iniciar sesión o registrarse con éxito no hay que navegar
 * a mano: [com.gestor_ventures.front.ui.AppRoot] observa la sesión y cambia sola a
 * [com.gestor_ventures.front.ui.main.MainRoute] cuando deja de ser `null`.
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
                onOlvidasteContrasena = { navController.navigate(AuthRutas.RecuperarContrasena) },
                onCrearCuenta = { navController.navigate(AuthRutas.Registro) },
            )
        }
        composable(AuthRutas.Registro) {
            RegistroRoute(
                onIniciarSesion = { navController.popBackStack() },
            )
        }
        composable(AuthRutas.RecuperarContrasena) {
            RecuperarContrasenaRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
