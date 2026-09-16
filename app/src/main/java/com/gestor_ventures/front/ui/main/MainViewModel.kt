package com.gestor_ventures.front.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {

    // Datos de ejemplo hasta que existan los repositorios de usuario y negocio (ARCHITECTURE.md §9).
    private val _uiState = MutableStateFlow(MainPreviewData.uiState)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Cambia el negocio activo desde el menú lateral. */
    fun seleccionarNegocio(negocioId: String) {
        _uiState.update { it.copy(negocioActivoId = negocioId) }
    }
}
