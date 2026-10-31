package com.gestor_ventures.front.ui.tour

import androidx.lifecycle.ViewModel
import com.gestor_ventures.back.repository.SesionRepository
import com.gestor_ventures.back.repository.TourRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * El tour de bienvenida: se abre solo la primera vez que una cuenta entra a la app y se puede
 * volver a ver desde el menú. Queda "visto" al cerrarlo, sea terminándolo u omitiéndolo.
 */
@HiltViewModel
class TourViewModel @Inject constructor(
    private val tourRepository: TourRepository,
    private val sesionRepository: SesionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TourUiState())
    val uiState: StateFlow<TourUiState> = _uiState.asStateFlow()

    /** La app ya tiene negocio y pestañas a la vista: si esta cuenta no lo ha visto, se abre. */
    fun mostrarSiEsLaPrimeraVez(esLider: Boolean) {
        val usuarioId = sesionRepository.usuarioId() ?: return
        if (!_uiState.value.visible && !tourRepository.yaLoVio(usuarioId)) {
            _uiState.value = TourUiState(visible = true, esLider = esLider)
        }
    }

    /** Desde el menú: lo vuelve a abrir desde el principio. */
    fun reiniciar(esLider: Boolean) {
        _uiState.value = TourUiState(visible = true, esLider = esLider)
    }

    fun siguiente() {
        if (_uiState.value.esUltimo) cerrar() else _uiState.update { it.copy(paso = it.paso + 1) }
    }

    fun anterior() {
        _uiState.update { it.copy(paso = (it.paso - 1).coerceAtLeast(0)) }
    }

    fun cerrar() {
        sesionRepository.usuarioId()?.let(tourRepository::marcarVisto)
        _uiState.value = TourUiState()
    }
}
