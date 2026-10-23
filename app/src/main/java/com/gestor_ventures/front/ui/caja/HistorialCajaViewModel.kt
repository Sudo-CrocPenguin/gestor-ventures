package com.gestor_ventures.front.ui.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.SeleccionDePeriodo
import com.gestor_ventures.back.model.personalizado
import com.gestor_ventures.back.model.rango
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.CierreDeCaja
import com.gestor_ventures.front.model.FormularioRango
import com.gestor_ventures.back.usecase.ListarCierresDeCaja
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
 * HU-23. El historial de jornadas cerradas del negocio activo.
 *
 * El periodo elegido vive en su propio flujo para que al tocar un chip la consulta se rehaga
 * sola con el rango nuevo, en vez de tener que acordarse de pedirla a mano.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistorialCajaViewModel @Inject constructor(
    private val listarCierres: ListarCierresDeCaja,
    negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialCajaUiState())
    val uiState: StateFlow<HistorialCajaUiState> = _uiState.asStateFlow()

    private val seleccion =
        MutableStateFlow<SeleccionDePeriodo>(SeleccionDePeriodo.Rapido(PeriodoPredefinido.Semana))

    init {
        viewModelScope.launch {
            combine(negocioActivoRepository.negocioActivoId, seleccion) { id, elegido -> id to elegido }
                .flatMapLatest { (negocioId, elegido) ->
                    if (negocioId == null) {
                        flowOf(emptyList())
                    } else {
                        listarCierres(negocioId, elegido.rango(listarCierres.hoy()))
                    }
                }
                .collect { cierres ->
                    _uiState.update {
                        it.copy(cierres = cierres.map(::aCierreUi), cargando = false)
                    }
                }
        }
    }

    /**
     * El chip se marca al instante y la lista llega cuando llegue: esperar a la consulta para
     * pintar el chip haría sentir la pantalla trabada.
     */
    fun onPeriodoChange(nuevo: PeriodoPredefinido) {
        aplicar(SeleccionDePeriodo.Rapido(nuevo))
    }

    /** HU-17. Abre la hoja del rango con lo que ya estuviera elegido, si era personalizado. */
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

    /**
     * Aplica el rango escrito a mano. Si `personalizado` lo rechaza —fechas al revés— no se
     * aplica nada y la hoja se queda abierta con el aviso.
     */
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

    private fun aCierreUi(cierre: CierreDeCaja) = CierreUi(
        id = cierre.caja.id,
        apertura = cierre.caja.apertura,
        cierre = cierre.caja.cierre,
        montoInicial = cierre.caja.montoInicial,
        esperado = cierre.esperado,
        real = cierre.caja.montoReal,
        diferencia = cierre.diferencia.diferencia,
        cuadra = cierre.diferencia.cuadra,
        sobra = cierre.diferencia.sobra,
        nota = cierre.caja.nota,
    )
}
