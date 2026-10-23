package com.gestor_ventures.front.ui.asistente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.ConversacionAsistente
import com.gestor_ventures.back.repository.AsistenteRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistorialAsistenteViewModel @Inject constructor(
    private val asistenteRepository: AsistenteRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialAsistenteUiState())
    val uiState: StateFlow<HistorialAsistenteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.flatMapLatest { negocioId ->
                if (negocioId == null) flowOf(emptyList()) else asistenteRepository.conversaciones(negocioId)
            }.collect { conversaciones ->
                _uiState.update { it.copy(conversaciones = conversaciones, cargando = false) }
            }
        }
    }

    fun pedirConfirmacionDeEliminar(conversacion: ConversacionAsistente) {
        _uiState.update { it.copy(porEliminar = conversacion) }
    }

    fun cancelarEliminar() {
        _uiState.update { it.copy(porEliminar = null) }
    }

    fun confirmarEliminar() {
        val conversacion = _uiState.value.porEliminar ?: return
        _uiState.update { it.copy(porEliminar = null) }
        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            asistenteRepository.eliminarConversacion(negocioId, conversacion.sesionId)
        }
    }
}
