package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Negocio
import com.gestor_ventures.back.model.ResultadoNegocio
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
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
 * HU-10. Edita los datos del negocio activo que se pidieron al registrarlo (HU-05): nombre,
 * categoría, tipo de actividad y color de marca.
 *
 * Usa el mismo estado que el registro, [RegistrarNegocioUiState], porque el formulario es el
 * mismo. Guarda con [NegocioRepository.actualizarNegocio], que aplica las mismas reglas que al
 * crear el negocio y anota la fecha de actualización.
 *
 * Lo demás de la configuración (gastos fijos, obligaciones, meta, reinversión y categorías) ya
 * se edita en sus propias pantallas del menú.
 */
@HiltViewModel
class EditarNegocioViewModel @Inject constructor(
    private val negocioRepository: NegocioRepository,
    negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrarNegocioUiState())
    val uiState: StateFlow<RegistrarNegocioUiState> = _uiState.asStateFlow()

    /** Avisa una sola vez que los cambios quedaron guardados, para que la pantalla se cierre. */
    private val _guardado = Channel<Unit>(Channel.BUFFERED)
    val guardado: Flow<Unit> = _guardado.receiveAsFlow()

    /** El negocio tal como estaba al abrir la pantalla; null mientras se carga. */
    private var negocio: Negocio? = null

    /** Llena el formulario con los datos actuales del negocio activo. */
    init {
        viewModelScope.launch {
            val actual = negocioActivoRepository.negocioActivo.filterNotNull().first()
            negocio = actual
            _uiState.update {
                it.copy(
                    nombre = actual.nombre,
                    categoria = actual.categoria,
                    tipoActividad = actual.tipoActividad,
                    colorMarca = actual.colorMarca,
                )
            }
        }
    }

    fun onNombreChange(texto: String) {
        _uiState.update { it.copy(nombre = texto, error = null) }
    }

    fun onCategoriaChange(texto: String) {
        _uiState.update { it.copy(categoria = texto, error = null) }
    }

    fun onTipoActividadChange(tipo: TipoActividad) {
        _uiState.update { it.copy(tipoActividad = tipo) }
    }

    fun onColorMarcaChange(hex: String?) {
        _uiState.update { it.copy(colorMarca = hex) }
    }

    /**
     * Guarda los cambios. El porcentaje de reinversión no está en este formulario (se edita en
     * "Meta y reinversión"), así que se manda el que ya tenía para no perderlo.
     *
     * Si el repositorio rechaza algo, el formulario se queda abierto con el aviso.
     */
    fun guardar() {
        val estado = _uiState.value
        val actual = negocio ?: return
        if (!estado.puedeGuardar) return

        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val resultado = negocioRepository.actualizarNegocio(
                negocioId = actual.id,
                nombre = estado.nombre,
                tipoActividad = estado.tipoActividad,
                categoria = estado.categoria,
                porcentajeReinversion = actual.porcentajeReinversion,
                colorMarca = estado.colorMarca,
            )
            when (resultado) {
                is ResultadoNegocio.Exito -> {
                    _uiState.update { it.copy(guardando = false) }
                    _guardado.send(Unit)
                }

                is ResultadoNegocio.Invalido -> {
                    _uiState.update { it.copy(guardando = false, error = resultado.error) }
                }
            }
        }
    }
}
