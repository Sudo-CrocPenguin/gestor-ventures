package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.SeleccionDePeriodo
import com.gestor_ventures.back.model.personalizado
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.CalcularResumenFinanciero
import com.gestor_ventures.front.model.FormularioRango
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * HU-16. El resumen del mes del negocio activo.
 *
 * No calcula nada: todo el cruce está en [CalcularResumenFinanciero]. Acá solo se decide de qué
 * negocio se habla y se sigue escuchando, para que el resumen cambie solo cuando se registre
 * una venta o se marque una obligación como pagada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ResumenFinancieroViewModel @Inject constructor(
    negocioActivoRepository: NegocioActivoRepository,
    calcularResumenFinanciero: CalcularResumenFinanciero,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResumenFinancieroUiState())
    val uiState: StateFlow<ResumenFinancieroUiState> = _uiState.asStateFlow()

    private val seleccion =
        MutableStateFlow<SeleccionDePeriodo>(SeleccionDePeriodo.Rapido(PeriodoPredefinido.Mes))

    init {
        viewModelScope.launch {
            combine(negocioActivoRepository.negocioActivoId, seleccion) { negocioId, elegido ->
                negocioId to elegido
            }.flatMapLatest { (negocioId, periodo) ->
                if (negocioId == null) flowOf(null) else calcularResumenFinanciero(negocioId, periodo)
            }.collect { resumen ->
                _uiState.update { it.copy(resumen = resumen, cargando = false) }
            }
        }
    }

    fun onPeriodoChange(nuevo: PeriodoPredefinido) {
        aplicar(SeleccionDePeriodo.Rapido(nuevo))
    }

    fun abrirRango() {
        val actual = _uiState.value.seleccion
        _uiState.update {
            it.copy(
                rango = when (actual) {
                    is SeleccionDePeriodo.Personalizado -> FormularioRango(actual.desde, actual.hasta)
                    else -> FormularioRango()
                },
            )
        }
    }

    fun cerrarRango() {
        _uiState.update { it.copy(rango = null) }
    }

    fun onDesdeChange(fecha: LocalDate) {
        _uiState.update { it.copy(rango = it.rango?.copy(desde = fecha)) }
    }

    fun onHastaChange(fecha: LocalDate) {
        _uiState.update { it.copy(rango = it.rango?.copy(hasta = fecha)) }
    }

    fun aplicarRango() {
        val formulario = _uiState.value.rango ?: return
        val desde = formulario.desde ?: return
        val hasta = formulario.hasta ?: return
        val elegido = personalizado(desde, hasta) ?: return

        aplicar(elegido)
        _uiState.update { it.copy(rango = null) }
    }

    private fun aplicar(nuevo: SeleccionDePeriodo) {
        seleccion.value = nuevo
        _uiState.update { it.copy(seleccion = nuevo) }
    }
}
