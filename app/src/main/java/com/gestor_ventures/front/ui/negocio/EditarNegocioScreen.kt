package com.gestor_ventures.front.ui.negocio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.front.theme.GestorVenturesTheme

/**
 * HU-10. "Información del negocio" del menú lateral.
 *
 * No tiene pantalla propia: reusa la del registro en modo edición, para que crear y editar un
 * negocio pidan y validen exactamente lo mismo. Igual que en el registro, la pantalla se pinta
 * con el color que se va eligiendo.
 */
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
