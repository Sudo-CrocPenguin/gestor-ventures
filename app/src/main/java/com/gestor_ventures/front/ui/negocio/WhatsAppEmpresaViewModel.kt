package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Vincula, cambia o desvincula el celular de WhatsApp del negocio activo. */
@HiltViewModel
class WhatsAppEmpresaViewModel @Inject constructor(
    private val negocioRepository: NegocioRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsAppEmpresaUiState())
    val uiState: StateFlow<WhatsAppEmpresaUiState> = _uiState.asStateFlow()

    /** Avisa que quedó guardado, para que la pantalla se cierre. */
    private val _guardado = Channel<Unit>(Channel.BUFFERED)
    val guardado: Flow<Unit> = _guardado.receiveAsFlow()

    init {
        viewModelScope.launch {
            val negocio = negocioActivoRepository.negocioActivo.first()
            _uiState.value = WhatsAppEmpresaUiState(
                nombreNegocio = negocio?.nombre.orEmpty(),
                vinculado = negocio?.whatsappEmpresa,
                numero = negocio?.whatsappEmpresa.orEmpty(),
                sinNegocio = negocio == null,
            )
        }
    }

    fun onNumeroChange(texto: String) {
        _uiState.update { it.copy(numero = texto.take(MaxCaracteres), numeroInvalido = false) }
    }

    fun guardar() {
        val estado = _uiState.value
        if (!estado.puedeGuardar) return
        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            val error = negocioRepository.vincularWhatsApp(negocioId, estado.numero)
            if (error == null) _guardado.send(Unit) else _uiState.update { it.copy(numeroInvalido = true) }
        }
    }

    fun desvincular() {
        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            if (negocioRepository.vincularWhatsApp(negocioId, null) == null) {
                _uiState.update { it.copy(vinculado = null, numero = "", numeroInvalido = false) }
            }
        }
    }

    private companion object {
        /** Cabe un número con indicativo, espacios y signos, y no un párrafo. */
        const val MaxCaracteres = 20
    }
}
