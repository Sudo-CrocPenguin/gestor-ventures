package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.ResultadoNegocio
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-05. Registra el negocio del usuario.
 *
 * El guardado va por [NegocioRepository]: este ViewModel no sabe que existe una base de datos.
 */
@HiltViewModel
class RegistrarNegocioViewModel @Inject constructor(
    private val negocioRepository: NegocioRepository,
    private val sesionRepository: SesionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrarNegocioUiState())
    val uiState: StateFlow<RegistrarNegocioUiState> = _uiState.asStateFlow()

    /** Avisa una sola vez que el negocio quedó creado, para que la pantalla navegue. */
    private val _negocioCreado = Channel<Long>(Channel.BUFFERED)
    val negocioCreado: Flow<Long> = _negocioCreado.receiveAsFlow()

    fun onNombreChange(texto: String) {
        _uiState.update { it.copy(nombre = texto, error = null) }
    }

    fun onCategoriaChange(texto: String) {
        _uiState.update { it.copy(categoria = texto, error = null) }
    }

    fun onTipoActividadChange(tipo: TipoActividad) {
        _uiState.update { it.copy(tipoActividad = tipo) }
    }

    /**
     * HU-05. El color con el que se verá la app, en hexadecimal; null deja el azul de siempre.
     */
    fun onColorMarcaChange(hex: String?) {
        _uiState.update { it.copy(colorMarca = hex) }
    }

    fun guardar() {
        val estado = _uiState.value
        if (!estado.puedeGuardar) return

        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val resultado = negocioRepository.crearNegocio(
                usuarioId = sesionRepository.usuarioId(),
                nombre = estado.nombre,
                tipoActividad = estado.tipoActividad,
                categoria = estado.categoria,
                // El porcentaje de reinversión se configura en HU-09; arranca en cero.
                porcentajeReinversion = 0.0,
                colorMarca = estado.colorMarca,
            )
            when (resultado) {
                is ResultadoNegocio.Exito -> {
                    _uiState.update { it.copy(guardando = false) }
                    _negocioCreado.send(resultado.negocioId)
                }

                is ResultadoNegocio.Invalido -> {
                    _uiState.update { it.copy(guardando = false, error = resultado.error) }
                }
            }
        }
    }
}
