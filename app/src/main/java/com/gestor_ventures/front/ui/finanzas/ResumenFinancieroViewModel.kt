package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.CalcularResumenFinanciero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-16. El resumen del mes del negocio activo.
 *
 * No calcula nada: todo el cruce está en [CalcularResumenFinanciero]. Acá solo se decide de qué
 * negocio se habla y se sigue escuchando, para que el resumen cambie solo cuando se registre
 * una venta o se marque una obligación como pagada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ResumenFinancieroViewModel @Inject constructor(
    negocioActivoRepository: NegocioActivoRepository,
    calcularResumenFinanciero: CalcularResumenFinanciero,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResumenFinancieroUiState())
    val uiState: StateFlow<ResumenFinancieroUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) flowOf(null) else calcularResumenFinanciero(negocioId)
                }
                .collect { resumen ->
                    _uiState.update { it.copy(resumen = resumen, cargando = false) }
                }
        }
    }
}
