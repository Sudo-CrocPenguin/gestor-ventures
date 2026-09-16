package com.gestor_ventures

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.ui.main.MainViewModel
import com.gestor_ventures.front.ui.main.MainRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // El tema toma el color del negocio activo (HU-05): si no tiene, se ve el azul
            // de siempre.
            val viewModel: MainViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            GestorVenturesTheme(colorMarca = uiState.negocioActivo?.colorMarca) {
                MainRoute(viewModel)
            }
        }
    }
}
