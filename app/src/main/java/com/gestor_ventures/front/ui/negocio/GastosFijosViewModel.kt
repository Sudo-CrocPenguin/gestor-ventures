package com.gestor_ventures.front.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.GastoFijo
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Máximo de dígitos de un monto, igual que en el resto de formularios. */
private const val MaxDigitosMonto = 12

/**
 * HU-06. Gastos fijos del negocio activo: verlos, agregarlos, corregirlos y borrarlos.
 *
 * Es la misma conversación del paso 2 del onboarding, pero acá el negocio no llega por la ruta
 * sino del negocio con el que el usuario está trabajando.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GastosFijosViewModel @Inject constructor(
    private val repository: BaseFinancieraRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GastosFijosUiState())
    val uiState: StateFlow<GastosFijosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) {
                        flowOf(emptyList())
                    } else {
                        repository.gastosFijosDeNegocio(negocioId)
                    }
                }
                .collect { gastos ->
                    _uiState.update { it.copy(gastosFijos = gastos, cargando = false) }
                }
        }
    }

    // ---------- El formulario ----------

    fun abrirFormularioNuevo() {
        _uiState.update { it.copy(formularioGasto = FormularioGastoFijo(), error = null) }
    }

    fun abrirFormularioDe(gasto: GastoFijo) {
        _uiState.update { it.copy(formularioGasto = formularioDe(gasto), error = null) }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formularioGasto = null) }
    }

    fun onNombreChange(texto: String) {
        _uiState.update {
            it.copy(formularioGasto = it.formularioGasto?.copy(nombre = texto), error = null)
        }
    }

    fun onMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update {
            it.copy(formularioGasto = it.formularioGasto?.copy(monto = digitos), error = null)
        }
    }

    fun onFrecuenciaChange(frecuencia: Frecuencia) {
        _uiState.update {
            it.copy(formularioGasto = it.formularioGasto?.copy(frecuencia = frecuencia))
        }
    }

    /** Guarda el formulario: crea uno nuevo o corrige el que se abrió, según traiga id. */
    fun guardarFormulario() {
        val formulario = _uiState.value.formularioGasto ?: return
        if (!formulario.puedeGuardar) return

        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            val monto = formulario.montoValor.toDouble()

            val error = if (formulario.gastoFijoId == null) {
                repository.agregarGastoFijo(
                    negocioId = negocioId,
                    nombre = formulario.nombre,
                    monto = monto,
                    frecuencia = formulario.frecuencia,
                )
            } else {
                repository.editarGastoFijo(
                    gastoFijoId = formulario.gastoFijoId,
                    nombre = formulario.nombre,
                    monto = monto,
                    frecuencia = formulario.frecuencia,
                )
            }

            // Con error la hoja se queda abierta: si se cerrara, el usuario perdería lo escrito.
            _uiState.update {
                it.copy(formularioGasto = if (error == null) null else it.formularioGasto, error = error)
            }
        }
    }

    fun eliminar(gastoFijoId: Long) {
        viewModelScope.launch { repository.eliminarGastoFijo(gastoFijoId) }
    }
}
