package com.gestor_ventures.front.ui.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * HU-03. Solo el estado del formulario: [enviarCodigo] todavía no habla con `AuthRepository`
 * (eso llega cuando se conecte el front con el back).
 */
@HiltViewModel
class RecuperarContrasenaViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RecuperarContrasenaUiState())
    val uiState: StateFlow<RecuperarContrasenaUiState> = _uiState.asStateFlow()

    fun onCorreoChange(texto: String) {
        _uiState.update { it.copy(correo = texto) }
    }

    fun enviarCodigo() {
        // TODO: conectar con AuthRepository.recuperarContrasena cuando el front se conecte al back.
    }
}
