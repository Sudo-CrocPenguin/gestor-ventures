package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.repository.MetodoPagoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.aDominio
import com.gestor_ventures.front.model.aUi
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

/** HU-11. Enciende y apaga los métodos de pago del negocio activo. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MetodosPagoViewModel @Inject constructor(
    private val repository: MetodoPagoRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetodosPagoUiState())
    val uiState: StateFlow<MetodosPagoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) flowOf(null) else repository.activos(negocioId)
                }
                .collect { activos ->
                    _uiState.update {
                        it.copy(
                            activos = activos?.map { metodo -> metodo.aUi() }?.toSet() ?: it.activos,
                            sinNegocio = activos == null,
                        )
                    }
                }
        }
    }

    fun onCambio(metodo: MetodoPagoUi, activo: Boolean) {
        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            val cambio = repository.cambiar(negocioId, metodo.aDominio(), activo)
            _uiState.update { it.copy(avisoUltimo = !cambio) }
        }
    }
}
