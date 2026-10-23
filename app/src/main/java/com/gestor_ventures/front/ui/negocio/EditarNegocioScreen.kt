package com.gestor_ventures.front.ui.negocio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.front.theme.GestorVenturesTheme

@Composable
fun EditarNegocioRoute(
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: EditarNegocioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.guardado.collect { onGuardado() }
    }

    GestorVenturesTheme(colorMarca = uiState.colorMarca) {
        Surface(color = MaterialTheme.colorScheme.background) {
            RegistrarNegocioScreen(
                uiState = uiState,
                onBack = onBack,
                editando = true,
                onNombreChange = viewModel::onNombreChange,
                onCategoriaChange = viewModel::onCategoriaChange,
                onTipoActividadChange = viewModel::onTipoActividadChange,
                onColorMarcaChange = viewModel::onColorMarcaChange,
                onGuardar = viewModel::guardar,
            )
        }
    }
}
