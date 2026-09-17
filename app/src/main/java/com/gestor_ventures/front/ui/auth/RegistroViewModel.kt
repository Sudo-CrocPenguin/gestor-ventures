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
 * HU-01. Al tener éxito no hace falta navegar a mano: `AuthRepository.registrar` deja la sesión
 * abierta y la raíz de la app (`AppRoot`) cambia sola de esta pantalla al resto de la app.
 */
@HiltViewModel
class RegistroViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun onNombreChange(texto: String) {
        _uiState.update { it.copy(nombre = texto, error = null) }
    }

    fun onCorreoChange(texto: String) {
        _uiState.update { it.copy(correo = texto, error = null) }
    }

    fun onContrasenaChange(texto: String) {
        _uiState.update { it.copy(contrasena = texto, error = null) }
    }

    fun onConfirmarContrasenaChange(texto: String) {
        _uiState.update { it.copy(confirmarContrasena = texto, error = null) }
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
        val estado = _uiState.value
        if (!estado.puedeCrearCuenta) return

        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = authRepository.registrar(estado.nombre, estado.correo, estado.contrasena)
            when (resultado) {
                is ResultadoAuth.Exito -> _uiState.update { it.copy(cargando = false) }
                is ResultadoAuth.Invalido ->
                    _uiState.update { it.copy(cargando = false, error = resultado.error) }
            }
        }
    }
}
