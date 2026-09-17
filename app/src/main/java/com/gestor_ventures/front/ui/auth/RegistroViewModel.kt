package com.gestor_ventures.front.ui.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * HU-01. Solo el estado del formulario: [crearCuenta] todavía no habla con `AuthRepository`
 * (eso llega cuando se conecte el front con el back).
 */
@HiltViewModel
class RegistroViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun onNombreChange(texto: String) {
        _uiState.update { it.copy(nombre = texto) }
    }

    fun onCorreoChange(texto: String) {
        _uiState.update { it.copy(correo = texto) }
    }

    fun onContrasenaChange(texto: String) {
        _uiState.update { it.copy(contrasena = texto) }
    }

    fun onConfirmarContrasenaChange(texto: String) {
        _uiState.update { it.copy(confirmarContrasena = texto) }
    }

    fun onMostrarContrasenaChange() {
        _uiState.update { it.copy(mostrarContrasena = !it.mostrarContrasena) }
    }

    fun onMostrarConfirmarContrasenaChange() {
        _uiState.update { it.copy(mostrarConfirmarContrasena = !it.mostrarConfirmarContrasena) }
    }

    fun onAceptaTerminosChange(valor: Boolean) {
        _uiState.update { it.copy(aceptaTerminos = valor) }
    }

    fun crearCuenta() {
        // TODO: conectar con AuthRepository.registrar cuando el front se conecte al back.
    }
}
