package com.gestor_ventures.front.ui.asistente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.ResultadoSugerencia
import com.gestor_ventures.back.model.Sugerencia
import com.gestor_ventures.back.repository.AsistenteRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.SugerirPromocion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-35. Las ideas de promociones del negocio activo: pedir una nueva, guardarla o
 * descartarla. La lista se actualiza sola con cada cambio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PromocionesViewModel @Inject constructor(
    private val sugerirPromocion: SugerirPromocion,
    private val asistenteRepository: AsistenteRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromocionesUiState())
    val uiState: StateFlow<PromocionesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.flatMapLatest { negocioId ->
                if (negocioId == null) flowOf(emptyList()) else asistenteRepository.sugerencias(negocioId)
            }.collect { sugerencias ->
                _uiState.update { it.copy(sugerencias = sugerencias, cargando = false) }
            }
        }
    }

    /** Pide una idea nueva. Si falla, se muestra el aviso y se puede volver a pedir. */
    fun sugerir() {
        if (_uiState.value.generando) return
        _uiState.update { it.copy(generando = true, error = null) }

        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first()
            if (negocioId == null) {
                _uiState.update { it.copy(generando = false) }
                return@launch
            }
            when (val resultado = sugerirPromocion(negocioId)) {
                is ResultadoSugerencia.Generada -> _uiState.update { it.copy(generando = false) }
                is ResultadoSugerencia.Fallo -> _uiState.update {
                    it.copy(generando = false, error = resultado.error)
                }
            }
        }
    }

    fun guardar(sugerencia: Sugerencia) {
        viewModelScope.launch { asistenteRepository.guardarSugerencia(sugerencia.id) }
    }

    fun descartar(sugerencia: Sugerencia) {
        viewModelScope.launch { asistenteRepository.descartarSugerencia(sugerencia.id) }
    }
}
