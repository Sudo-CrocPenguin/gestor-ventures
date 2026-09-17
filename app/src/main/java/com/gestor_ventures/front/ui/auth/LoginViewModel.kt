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

/**
 * HU-02. Una vez que [iniciarSesion] tiene éxito no hace falta navegar a mano: la sesión que
 * `AuthRepository`/`SesionRepository` dejan abierta hace que la raíz de la app (`AppRoot`)
 * cambie sola de esta pantalla al resto de la app.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onCorreoChange(texto: String) {
        _uiState.update { it.copy(correo = texto, error = null) }
    }

    fun onContrasenaChange(texto: String) {
        _uiState.update { it.copy(contrasena = texto, error = null) }
    }

    fun onMostrarContrasenaChange() {
        _uiState.update { it.copy(mostrarContrasena = !it.mostrarContrasena) }
    }

    fun onRecordarmeChange(valor: Boolean) {
        _uiState.update { it.copy(recordarme = valor) }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        if (!estado.puedeIniciarSesion) return

        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = authRepository.iniciarSesion(estado.correo, estado.contrasena)) {
                is ResultadoAuth.Exito -> _uiState.update { it.copy(cargando = false) }
                is ResultadoAuth.Invalido ->
                    _uiState.update { it.copy(cargando = false, error = resultado.error) }
            }
        }
    }
}
