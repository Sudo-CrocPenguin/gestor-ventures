package com.gestor_ventures.front.ui.asistente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.MaxCaracteresPregunta
import com.gestor_ventures.back.model.MensajeAsistente
import com.gestor_ventures.back.model.ResultadoAsistente
import com.gestor_ventures.back.repository.AsistenteRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.ConsultarAsistente
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-34. La conversación con el asistente.
 *
 * El chat escucha la conversación guardada, así que cada respuesta aparece sola apenas se
 * guarda, y cambiar de negocio en el menú cambia de conversación.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AsistenteViewModel @Inject constructor(
    private val consultarAsistente: ConsultarAsistente,
    private val asistenteRepository: AsistenteRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AsistenteUiState())
    val uiState: StateFlow<AsistenteUiState> = _uiState.asStateFlow()

    /** Cada vez que se abre la pestaña empieza una conversación nueva. */
    private val sesion = MutableStateFlow(asistenteRepository.nuevaSesion())

    init {
        viewModelScope.launch {
            combine(negocioActivoRepository.negocioActivoId, sesion) { negocioId, sesionId ->
                negocioId to sesionId
            }.flatMapLatest { (negocioId, sesionId) ->
                if (negocioId == null) {
                    flowOf<List<MensajeAsistente>?>(null)
                } else {
                    asistenteRepository.mensajesDeSesion(negocioId, sesionId)
                }
            }.collect { mensajes ->
                _uiState.update {
                    it.copy(mensajes = mensajes.orEmpty(), sinNegocio = mensajes == null)
                }
            }
        }
    }

    fun onPreguntaChange(texto: String) {
        _uiState.update { it.copy(pregunta = texto.take(MaxCaracteresPregunta), error = null) }
    }

    /** Tocar una pregunta sugerida la manda de una vez. */
    fun usarSugerencia(texto: String) {
        _uiState.update { it.copy(pregunta = texto, error = null) }
        enviar()
    }

    /**
     * Manda la pregunta. El campo se vacía de una vez, para que se sienta enviada; si algo
     * falla, la pregunta vuelve al campo para reenviarla sin escribirla otra vez.
     */
    fun enviar() {
        val estado = _uiState.value
        if (!estado.puedeEnviar) return
        val pregunta = estado.pregunta.trim()

        _uiState.update { it.copy(pregunta = "", preguntaEnCurso = pregunta, error = null) }
        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first()
            if (negocioId == null) {
                _uiState.update { it.copy(pregunta = pregunta, preguntaEnCurso = null) }
                return@launch
            }
            when (val resultado = consultarAsistente(negocioId, sesion.value, pregunta)) {
                is ResultadoAsistente.Respuesta -> _uiState.update { it.copy(preguntaEnCurso = null) }
                is ResultadoAsistente.Fallo -> _uiState.update {
                    it.copy(pregunta = pregunta, preguntaEnCurso = null, error = resultado.error)
                }
            }
        }
    }

    /** Empieza de cero. La conversación anterior queda guardada para el historial (HU-39). */
    fun nuevaConversacion() {
        if (_uiState.value.enviando) return
        sesion.value = asistenteRepository.nuevaSesion()
        _uiState.update { it.copy(pregunta = "", error = null) }
    }
}
