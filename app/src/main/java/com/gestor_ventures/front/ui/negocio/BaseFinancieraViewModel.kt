package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.usecase.CalcularAhorroMensual
import com.gestor_ventures.front.navigation.ArgumentoNegocioId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Máximo de dígitos de un monto, igual que en el resto de formularios. */
private const val MaxDigitosMonto = 12

/**
 * HU-06, HU-08 y HU-09. Paso 2 del onboarding.
 *
 * Los gastos fijos se guardan uno por uno apenas se agregan; la meta y la reinversión, al
 * finalizar. El negocio ya existe: llega su id por la ruta.
 */
@HiltViewModel
class BaseFinancieraViewModel @Inject constructor(
    private val repository: BaseFinancieraRepository,
    private val calcularAhorroMensual: CalcularAhorroMensual,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val negocioId: Long = savedStateHandle.get<String>(ArgumentoNegocioId)?.toLongOrNull()
        ?: 0L

    private val _uiState = MutableStateFlow(BaseFinancieraUiState())
    val uiState: StateFlow<BaseFinancieraUiState> = _uiState.asStateFlow()

    /** Avisa una sola vez que la configuración terminó, para pasar a la pantalla de éxito. */
    private val _configuracionLista = Channel<Unit>(Channel.BUFFERED)
    val configuracionLista: Flow<Unit> = _configuracionLista.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.gastosFijosDeNegocio(negocioId).collect { gastos ->
                _uiState.update { it.copy(gastosFijos = gastos) }
            }
        }
    }

    // ---------- Meta de ahorro (HU-08) ----------

    fun onMetaMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update { it.copy(metaMonto = digitos, error = null) }
        recalcularAhorro()
    }

    fun onFechaLimiteChange(fecha: LocalDate) {
        _uiState.update { it.copy(fechaLimite = fecha, error = null) }
        recalcularAhorro()
    }

    private fun recalcularAhorro() {
        _uiState.update { estado ->
            val monto = estado.metaMonto.toLongOrNull()?.toDouble() ?: 0.0
            val fecha = estado.fechaLimite
            estado.copy(
                ahorroMensual = if (fecha == null) null else calcularAhorroMensual(monto, fecha),
            )
        }
    }

    // ---------- Reinversión (HU-09) ----------

    fun onPorcentajeReinversionChange(porcentaje: Int) {
        _uiState.update { it.copy(porcentajeReinversion = porcentaje.coerceIn(0, 100)) }
    }

    // ---------- Gastos fijos (HU-06) ----------

    fun abrirFormularioGasto() {
        _uiState.update { it.copy(formularioGasto = FormularioGastoFijo()) }
    }

    fun cerrarFormularioGasto() {
        _uiState.update { it.copy(formularioGasto = null) }
    }

    fun onNombreGastoChange(texto: String) {
        _uiState.update { it.copy(formularioGasto = it.formularioGasto?.copy(nombre = texto)) }
    }

    fun onMontoGastoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update { it.copy(formularioGasto = it.formularioGasto?.copy(monto = digitos)) }
    }

    fun onFrecuenciaGastoChange(frecuencia: Frecuencia) {
        _uiState.update {
            it.copy(formularioGasto = it.formularioGasto?.copy(frecuencia = frecuencia))
        }
    }

    fun guardarGastoFijo() {
        val formulario = _uiState.value.formularioGasto ?: return
        if (!formulario.puedeGuardar) return

        viewModelScope.launch {
            val error = repository.agregarGastoFijo(
                negocioId = negocioId,
                nombre = formulario.nombre,
                monto = formulario.monto.toLong().toDouble(),
                frecuencia = formulario.frecuencia,
            )
            _uiState.update { it.copy(error = error, formularioGasto = if (error == null) null else formulario) }
        }
    }

    fun eliminarGastoFijo(gastoFijoId: Long) {
        viewModelScope.launch { repository.eliminarGastoFijo(gastoFijoId) }
    }

    // ---------- Finalizar ----------

    /** Guarda meta y reinversión si el usuario las llenó; ambas son opcionales. */
    fun finalizar() {
        val estado = _uiState.value
        if (estado.guardando) return

        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val monto = estado.metaMonto.toLongOrNull()?.toDouble() ?: 0.0
            val fecha = estado.fechaLimite

            val errorMeta = if (monto > 0.0 && fecha != null) {
                repository.definirMetaAhorro(negocioId, monto, fecha)
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
            if (errorReinversion != null) {
                _uiState.update { it.copy(guardando = false, error = errorReinversion) }
                return@launch
            }

            _uiState.update { it.copy(guardando = false) }
            _configuracionLista.send(Unit)
        }
    }
}
