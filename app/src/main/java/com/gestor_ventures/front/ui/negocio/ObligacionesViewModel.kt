package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Obligacion
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.ObligacionRepository
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
import java.time.LocalDate
import javax.inject.Inject

/** Máximo de dígitos de un monto, igual que en el resto de formularios. */
private const val MaxDigitosMonto = 12

/** Lo que la pantalla necesita, junto, para no armar el estado a medias. */
private data class DatosDeObligaciones(
    val obligaciones: List<Obligacion> = emptyList(),
    val proximas: List<Obligacion> = emptyList(),
    val totalPendiente: Double = 0.0,
)

/** HU-07. Las obligaciones del negocio activo. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ObligacionesViewModel @Inject constructor(
    private val repository: ObligacionRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObligacionesUiState(hoy = repository.hoy()))
    val uiState: StateFlow<ObligacionesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.flatMapLatest { negocioId ->
                if (negocioId == null) {
                    flowOf(DatosDeObligaciones())
                } else {
                    combine(
                        repository.obligacionesDeNegocio(negocioId),
                        repository.proximasAVencer(negocioId),
                        repository.totalPendiente(negocioId),
                    ) { todas, proximas, total ->
                        DatosDeObligaciones(todas, proximas, total)
                    }
                }
            }.collect { datos ->
                _uiState.update {
                    it.copy(
                        obligaciones = datos.obligaciones,
                        proximas = datos.proximas,
                        totalPendiente = datos.totalPendiente,
                        cargando = false,
                    )
                }
            }
        }
    }

    // ---------- Qué hacer con una obligación ----------

    fun abrirAcciones(obligacion: Obligacion) {
        _uiState.update { it.copy(acciones = obligacion) }
    }

    fun cerrarAcciones() {
        _uiState.update { it.copy(acciones = null) }
    }

    /** HU-07. Marcar como pagada, o devolverla a pendiente si el usuario se equivocó. */
    fun alternarPagadaDeLaElegida() {
        val obligacion = _uiState.value.acciones ?: return
        cerrarAcciones()
        viewModelScope.launch { repository.marcarPagada(obligacion.id, !obligacion.pagada) }
    }

    fun editarLaElegida() {
        val obligacion = _uiState.value.acciones ?: return
        cerrarAcciones()
        _uiState.update {
            it.copy(
                formulario = FormularioObligacion(
                    obligacionId = obligacion.id,
                    nombre = obligacion.nombre,
                    monto = obligacion.monto.toLong().toString(),
                    fechaVencimiento = obligacion.fechaVencimiento,
                ),
                error = null,
            )
        }
    }

    fun eliminarLaElegida() {
        val obligacion = _uiState.value.acciones ?: return
        cerrarAcciones()
        viewModelScope.launch { repository.eliminarObligacion(obligacion.id) }
    }

    // ---------- El formulario ----------

    fun abrirFormularioNuevo() {
        _uiState.update { it.copy(formulario = FormularioObligacion(), error = null) }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formulario = null, error = null) }
    }

    fun onNombreChange(texto: String) {
        _uiState.update {
            it.copy(formulario = it.formulario?.copy(nombre = texto), error = null)
        }
    }

    fun onMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update {
            it.copy(formulario = it.formulario?.copy(monto = digitos), error = null)
        }
    }

    fun onFechaChange(fecha: LocalDate) {
        _uiState.update {
            it.copy(formulario = it.formulario?.copy(fechaVencimiento = fecha), error = null)
        }
    }

    fun guardarFormulario() {
        val formulario = _uiState.value.formulario ?: return
        val vencimiento = formulario.fechaVencimiento ?: return
        if (!formulario.puedeGuardar) return

        viewModelScope.launch {
            val error = if (formulario.obligacionId == null) {
                val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
                repository.registrarObligacion(
                    negocioId = negocioId,
                    nombre = formulario.nombre,
                    monto = formulario.montoValor.toDouble(),
                    fechaVencimiento = vencimiento,
                )
            } else {
                repository.editarObligacion(
                    obligacionId = formulario.obligacionId,
                    nombre = formulario.nombre,
                    monto = formulario.montoValor.toDouble(),
                    fechaVencimiento = vencimiento,
                )
            }

            // Con error la hoja se queda abierta: si se cerrara, el usuario perdería lo escrito.
            _uiState.update {
                it.copy(formulario = if (error == null) null else it.formulario, error = error)
            }
        }
    }
}
