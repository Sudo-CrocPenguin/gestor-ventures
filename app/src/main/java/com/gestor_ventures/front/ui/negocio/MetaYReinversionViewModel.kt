package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.CalcularAhorroMensual
import com.gestor_ventures.back.usecase.CalcularResumenFinanciero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Máximo de dígitos de un monto, igual que en el resto de formularios. */
private const val MaxDigitosMonto = 12

/**
 * HU-08 y HU-09. Configuración de la meta de ahorro y la reinversión del negocio activo.
 *
 * Es la misma conversación del paso 2 del onboarding, pero para después: el emprendedor que se
 * saltó ese paso no tenía dónde volver, y el que lo llenó no podía corregirlo nunca.
 *
 * Lo guardado se carga una sola vez, al abrir. Si se recargara con cada cambio del repositorio,
 * lo que el usuario está escribiendo se le borraría debajo de los dedos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MetaYReinversionViewModel @Inject constructor(
    private val repository: BaseFinancieraRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    private val calcularAhorroMensual: CalcularAhorroMensual,
    calcularResumenFinanciero: CalcularResumenFinanciero,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetaYReinversionUiState())
    val uiState: StateFlow<MetaYReinversionUiState> = _uiState.asStateFlow()

    /** Avisa una sola vez que se guardó, para que la pantalla lo confirme y se cierre. */
    private val _guardado = Channel<Unit>(Channel.BUFFERED)
    val guardado: Flow<Unit> = _guardado.receiveAsFlow()

    init {
        viewModelScope.launch {
            val negocio = negocioActivoRepository.negocioActivo.first()
            val meta = negocio?.let { repository.metaActiva(it.id).first() }

            _uiState.update { estado ->
                estado.copy(
                    metaMonto = meta?.montoObjetivo?.toLong()?.toString().orEmpty(),
                    fechaLimite = meta?.fechaLimite,
                    porcentajeReinversion = negocio?.porcentajeReinversion?.toInt() ?: 0,
                    cargando = false,
                )
            }
            recalcularAhorro()
        }

        // El progreso sí se sigue: es un dato de solo lectura y nadie lo está escribiendo.
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) {
                        flowOf(null)
                    } else {
                        calcularResumenFinanciero.progresoDeLaMeta(negocioId)
                    }
                }
                .collect { progreso -> _uiState.update { it.copy(progreso = progreso) } }
        }
    }

    fun onMetaMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update { it.copy(metaMonto = digitos, error = null) }
        recalcularAhorro()
    }

    fun onFechaLimiteChange(fecha: LocalDate) {
        _uiState.update { it.copy(fechaLimite = fecha, error = null) }
        recalcularAhorro()
    }

    fun onPorcentajeReinversionChange(porcentaje: Int) {
        _uiState.update { it.copy(porcentajeReinversion = porcentaje.coerceIn(0, 100), error = null) }
    }

    private fun recalcularAhorro() {
        _uiState.update { estado ->
            val fecha = estado.fechaLimite
            estado.copy(
                ahorroMensual = if (fecha == null) {
                    null
                } else {
                    calcularAhorroMensual(estado.montoValor.toDouble(), fecha)
                },
            )
        }
    }

    /**
     * Guarda las dos. La meta solo se toca si está completa: dejarla vacía significa "todavía
     * no tengo meta", no "bórrala".
     */
    fun guardar() {
        val estado = _uiState.value
        if (!estado.puedeGuardar) return

        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first()
            if (negocioId == null) {
                _uiState.update { it.copy(guardando = false) }
                return@launch
            }

            val fecha = estado.fechaLimite
            val errorMeta = if (estado.montoValor > 0L && fecha != null) {
                repository.definirMetaAhorro(negocioId, estado.montoValor.toDouble(), fecha)
            } else {
                null
            }
            if (errorMeta != null) {
                _uiState.update { it.copy(guardando = false, error = errorMeta) }
                return@launch
            }

            val errorReinversion = repository.definirPorcentajeReinversion(
                negocioId = negocioId,
                porcentaje = estado.porcentajeReinversion.toDouble(),
            )
            _uiState.update { it.copy(guardando = false, error = errorReinversion) }
            if (errorReinversion == null) _guardado.send(Unit)
        }
    }
}
