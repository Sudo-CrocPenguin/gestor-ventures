package com.gestor_ventures.front.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.CambioDeFoto
import com.gestor_ventures.back.model.LargoDeTelefono
import com.gestor_ventures.back.repository.PerfilRepository
import com.gestor_ventures.back.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-04. Edita el perfil de quien tiene la sesión abierta.
 *
 * Los cambios se guardan todos juntos al tocar "Guardar cambios", no uno por uno: así nunca
 * queda un perfil a medio editar.
 */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val perfilRepository: PerfilRepository,
    private val sesionRepository: SesionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    /** Avisa una sola vez que el perfil quedó guardado, para que la pantalla se cierre. */
    private val _guardado = Channel<Unit>(Channel.BUFFERED)
    val guardado: Flow<Unit> = _guardado.receiveAsFlow()

    /** Llena el formulario con el perfil actual. */
    init {
        viewModelScope.launch {
            val usuario = sesionRepository.usuarioActual.filterNotNull().first()
            _uiState.update {
                it.copy(
                    nombre = usuario.nombre,
                    correo = usuario.correo,
                    telefono = usuario.telefono.orEmpty(),
                    fotoGuardada = usuario.fotoPerfilUrl,
                    cargando = false,
                )
            }
        }
    }

    fun onNombreChange(texto: String) {
        _uiState.update { it.copy(nombre = texto, error = null) }
    }

    /** Solo deja escribir números, y no más de 15. */
    fun onTelefonoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(LargoDeTelefono.last)
        _uiState.update { it.copy(telefono = digitos, error = null) }
    }

    /** La foto elegida en la galería. No se copia hasta guardar. */
    fun onFotoElegida(uri: String) {
        _uiState.update { it.copy(cambioDeFoto = CambioDeFoto.Nueva(uri), error = null) }
    }

    fun quitarFoto() {
        _uiState.update { it.copy(cambioDeFoto = CambioDeFoto.Quitar, error = null) }
    }

    /** Si el repositorio rechaza algo, la pantalla se queda abierta con el aviso. */
    fun guardar() {
        val estado = _uiState.value
        if (!estado.puedeGuardar) return
        val usuarioId = sesionRepository.usuarioId() ?: return

        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val error = perfilRepository.actualizarPerfil(
                usuarioId = usuarioId,
                nombre = estado.nombre,
                telefono = estado.telefono,
                foto = estado.cambioDeFoto,
            )
            if (error == null) {
                _uiState.update { it.copy(guardando = false) }
                _guardado.send(Unit)
            } else {
                _uiState.update { it.copy(guardando = false, error = error) }
            }
        }
    }
}
