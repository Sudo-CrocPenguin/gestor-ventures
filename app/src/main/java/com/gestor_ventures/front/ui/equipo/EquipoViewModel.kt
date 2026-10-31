package com.gestor_ventures.front.ui.equipo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.MiembroEquipo
import com.gestor_ventures.back.repository.EquipoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.SesionRepository
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

/** El equipo del negocio activo: quién lo lidera, qué vendedores tiene y a quién se invita. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EquipoViewModel @Inject constructor(
    private val repository: EquipoRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    private val sesionRepository: SesionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EquipoUiState())
    val uiState: StateFlow<EquipoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                negocioActivoRepository.negocioActivoId.flatMapLatest { negocioId ->
                    if (negocioId == null) flowOf(null) else repository.miembros(negocioId)
                },
                sesionRepository.usuarioActual,
            ) { miembros, usuario -> miembros to usuario }
                .collect { (miembros, usuario) ->
                    _uiState.update {
                        it.copy(
                            lider = usuario?.let { u -> LiderUi(nombre = u.nombre, correo = u.correo) },
                            vendedores = miembros.orEmpty(),
                            sinNegocio = miembros == null,
                        )
                    }
                }
        }
    }

    fun abrirInvitacion() {
        _uiState.update { it.copy(invitacion = FormularioInvitacion()) }
    }

    fun cerrarInvitacion() {
        _uiState.update { it.copy(invitacion = null) }
    }

    fun onCorreoChange(texto: String) {
        _uiState.update { estado ->
            estado.copy(invitacion = estado.invitacion?.copy(correo = texto, error = null))
        }
    }

    /** Guarda la invitación si el correo sirve; si no, la hoja se queda abierta con el motivo. */
    fun invitar() {
        val formulario = _uiState.value.invitacion ?: return
        val correoDelLider = _uiState.value.lider?.correo.orEmpty()
        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            val error = repository.invitar(negocioId, formulario.correo, correoDelLider)
            _uiState.update {
                if (error == null) it.copy(invitacion = null)
                else it.copy(invitacion = formulario.copy(error = error))
            }
        }
    }

    fun abrirAcciones(miembro: MiembroEquipo) {
        _uiState.update { it.copy(elegido = miembro) }
    }

    fun cerrarAcciones() {
        _uiState.update { it.copy(elegido = null) }
    }

    fun quitarElegido() {
        val miembro = _uiState.value.elegido ?: return
        _uiState.update { it.copy(elegido = null) }
        viewModelScope.launch { repository.quitar(miembro.id) }
    }
}
