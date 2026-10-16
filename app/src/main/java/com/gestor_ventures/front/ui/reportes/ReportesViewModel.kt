package com.gestor_ventures.front.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.CalcularTendencia
import com.gestor_ventures.back.usecase.EscalaDeTiempo
import com.gestor_ventures.back.usecase.PuntoDeTendencia
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
import javax.inject.Inject

/**
 * HU-40. El gráfico de ingresos y egresos del negocio activo.
 *
 * La escala vive en su propio flujo para que al tocar un chip la consulta se rehaga sola con
 * los tramos nuevos. Y como los totales vienen por `Flow`, registrar una venta redibuja el
 * gráfico sin que nadie le avise: es el criterio de "actualizar automáticamente".
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportesViewModel @Inject constructor(
    private val calcularTendencia: CalcularTendencia,
    negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportesUiState())
    val uiState: StateFlow<ReportesUiState> = _uiState.asStateFlow()

    private val escala = MutableStateFlow(EscalaDeTiempo.Mensual)

    init {
        viewModelScope.launch {
            combine(negocioActivoRepository.negocioActivoId, escala) { id, elegida -> id to elegida }
                .flatMapLatest { (negocioId, elegida) ->
                    if (negocioId == null) {
                        flowOf(emptyList())
                    } else {
                        calcularTendencia(negocioId, elegida)
                    }
                }
                .collect { puntos ->
                    _uiState.update { it.copy(puntos = puntos.map(::aPuntoUi), cargando = false) }
                }
        }
    }

    /** El chip se marca al instante; el gráfico se redibuja cuando lleguen los datos. */
    fun onEscalaChange(nueva: EscalaDeTiempo) {
        escala.value = nueva
        _uiState.update { it.copy(escala = nueva) }
    }

    private fun aPuntoUi(punto: PuntoDeTendencia) = PuntoUi(
        inicio = punto.inicio,
        ingresos = punto.ingresos,
        gastos = punto.gastos,
        costos = punto.costos,
    )
}
