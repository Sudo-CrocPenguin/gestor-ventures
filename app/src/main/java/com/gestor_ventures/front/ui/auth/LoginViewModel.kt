package com.gestor_ventures.front.ui.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * HU-02. Solo el estado del formulario: [iniciarSesion] todavía no habla con `AuthRepository`
 * (eso llega cuando se conecte el front con el back).
 */
@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onCorreoChange(texto: String) {
        _uiState.update { it.copy(correo = texto) }
    }

    fun onContrasenaChange(texto: String) {
        _uiState.update { it.copy(contrasena = texto) }
    }

    fun onMostrarContrasenaChange() {
        _uiState.update { it.copy(mostrarContrasena = !it.mostrarContrasena) }
    }

    fun onRecordarmeChange(valor: Boolean) {
        _uiState.update { it.copy(recordarme = valor) }
    }

    fun iniciarSesion() {
        // TODO: conectar con AuthRepository.iniciarSesion cuando el front se conecte al back.
    }
}
