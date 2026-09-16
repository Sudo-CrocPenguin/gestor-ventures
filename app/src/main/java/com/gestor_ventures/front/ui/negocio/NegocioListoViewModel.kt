package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.MetaAhorro
import com.gestor_ventures.back.model.Negocio
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.front.navigation.ArgumentoNegocioId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Lo que muestra el resumen final del onboarding. */
data class NegocioListoUiState(
    val negocio: Negocio? = null,
    val meta: MetaAhorro? = null,
)

/** HU-05. Resumen de cómo quedó configurado el negocio recién creado. */
@HiltViewModel
class NegocioListoViewModel @Inject constructor(
    negocioRepository: NegocioRepository,
    baseFinancieraRepository: BaseFinancieraRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val negocioId: Long = savedStateHandle.get<String>(ArgumentoNegocioId)?.toLongOrNull()
        ?: 0L

    val uiState: StateFlow<NegocioListoUiState> = combine(
        negocioRepository.observarNegocio(negocioId),
        baseFinancieraRepository.metaActiva(negocioId),
    ) { negocio, meta ->
        NegocioListoUiState(negocio = negocio, meta = meta)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NegocioListoUiState(),
    )
}
