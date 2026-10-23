package com.gestor_ventures.front.ui.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Alerta
import com.gestor_ventures.back.repository.AlertaRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.GenerarAlertas
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
 * HU-38. Las alertas del negocio activo.
 *
 * Al abrir la pantalla se corre la revisión en el momento, para no tener que esperar a la
 * revisión diaria: lo que pasó hoy ya aparece.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificacionesViewModel @Inject constructor(
    private val alertaRepository: AlertaRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    generarAlertas: GenerarAlertas,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacionesUiState())
    val uiState: StateFlow<NotificacionesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.first()?.let { generarAlertas(it) }
        }
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.flatMapLatest { negocioId ->
                if (negocioId == null) flowOf(emptyList()) else alertaRepository.alertas(negocioId)
            }.collect { alertas ->
                _uiState.update { it.copy(alertas = alertas, cargando = false) }
            }
        }
    }

    fun marcarLeida(alerta: Alerta) {
        if (alerta.leida) return
        viewModelScope.launch { alertaRepository.marcarLeida(alerta.id) }
    }

    fun marcarTodasLeidas() {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.first()?.let { alertaRepository.marcarTodasLeidas(it) }
        }
    }
}
