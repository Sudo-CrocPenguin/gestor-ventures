package com.gestor_ventures.front.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.ui.auth.AuthNavHost
import com.gestor_ventures.front.ui.main.MainRoute
import com.gestor_ventures.front.ui.main.MainViewModel

/**
 * Punto de entrada de la UI: sin sesión (HU-01/02) muestra el login; con sesión, la app.
 *
 * El cambio entre uno y otro es automático, no navegado: [AppRootViewModel.usuarioId] sale de
 * `SesionRepository`, y en cuanto deja de ser `null` (login, registro) o vuelve a serlo (sesión
 * cerrada o expirada por inactividad, HU-02) Compose recompone y cambia de pantalla solo.
 */
@Composable
fun AppRoot(appRootViewModel: AppRootViewModel = hiltViewModel()) {
    val usuarioId by appRootViewModel.usuarioId.collectAsStateWithLifecycle()

    if (usuarioId == null) {
        GestorVenturesTheme {
            AuthNavHost()
        }
    } else {
        val mainViewModel: MainViewModel = hiltViewModel()
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
        // El tema toma el color del negocio activo (HU-05): si no tiene, se ve el azul de siempre.
        GestorVenturesTheme(colorMarca = uiState.negocioActivo?.colorMarca) {
            MainRoute(mainViewModel)
        }
    }
}
