package com.gestor_ventures.front.ui.inicio

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InicioViewModel : ViewModel() {

    // Datos de ejemplo hasta que existan los repositorios y el caso de uso
    // CalcularResumenFinanciero (ARCHITECTURE.md §9).
    private val _uiState = MutableStateFlow(InicioPreviewData.uiState)
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()
}
