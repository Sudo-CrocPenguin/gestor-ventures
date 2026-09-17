package com.gestor_ventures.front.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.ResultadoAuth
import com.gestor_ventures.back.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** HU-03. Pide a Firebase el enlace de recuperación; no abre sesión ni navega sola. */
@HiltViewModel
class RecuperarContrasenaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecuperarContrasenaUiState())
    val uiState: StateFlow<RecuperarContrasenaUiState> = _uiState.asStateFlow()

    fun onCorreoChange(texto: String) {
        _uiState.update { it.copy(correo = texto, error = null, enviado = false) }
    }

    fun enviarCodigo() {
        val estado = _uiState.value
        if (!estado.puedeEnviarCodigo) return

        _uiState.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = authRepository.recuperarContrasena(estado.correo)) {
                is ResultadoAuth.Exito -> _uiState.update { it.copy(enviando = false, enviado = true) }
                is ResultadoAuth.Invalido ->
                    _uiState.update { it.copy(enviando = false, error = resultado.error) }
            }
        }
    }
}
