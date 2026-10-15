package com.gestor_ventures.front.ui.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.CalcularSaldoEsperado
import com.gestor_ventures.back.usecase.SaldoDeCaja
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

/** Tope de dígitos del monto, el mismo que usan los demás campos de dinero. */
private const val MaxDigitosMonto = 12

/**
 * HU-19 y HU-20. Abrir la jornada de caja y seguir su saldo.
 *
 * El saldo lo manda [CalcularSaldoEsperado] y lo que el usuario está escribiendo vive acá: así
 * el campo responde al instante y, al abrir o al registrar una venta, la pantalla cambia sola
 * sin tener que pedir nada de vuelta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CajaViewModel @Inject constructor(
    private val repository: CajaRepository,
    private val calcularSaldoEsperado: CalcularSaldoEsperado,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CajaUiState())
    val uiState: StateFlow<CajaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) flowOf(null) else calcularSaldoEsperado(negocioId)
                }
                .collect { saldo ->
                    _uiState.update { it.copy(caja = saldo?.let(::aCajaUi), cargando = false) }
                }
        }
    }

    fun abrirFormulario() {
        _uiState.update { it.copy(formulario = FormularioApertura(), error = null) }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formulario = null, error = null) }
    }

    /** Solo dígitos: el formato con puntos lo pone la pantalla, no lo escribe el usuario. */
    fun onMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update { estado ->
            estado.copy(
                formulario = estado.formulario?.copy(montoInicial = digitos),
                error = null,
            )
        }
    }

    fun abrirCaja() {
        val formulario = _uiState.value.formulario ?: return
        if (!formulario.puedeAbrir) return

        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            val monto = formulario.montoInicial.toLongOrNull()?.toDouble() ?: return@launch

            val error = repository.abrirCaja(negocioId, monto)

            // Con error la hoja se queda abierta: lo escrito no se pierde y se puede corregir.
            _uiState.update {
                it.copy(error = error, formulario = if (error == null) null else formulario)
            }
        }
    }

    // ---------- HU-21 y HU-22: cerrar ----------

    fun abrirCierre() {
        _uiState.update { it.copy(cierre = FormularioCierre(), error = null) }
    }

    fun cerrarCierre() {
        _uiState.update { it.copy(cierre = null, error = null) }
    }

    fun onMontoRealChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update { estado ->
            estado.copy(cierre = estado.cierre?.copy(montoReal = digitos), error = null)
        }
    }

    fun onNotaChange(texto: String) {
        _uiState.update { estado -> estado.copy(cierre = estado.cierre?.copy(nota = texto)) }
    }

    fun cerrarCaja() {
        val cierre = _uiState.value.cierre ?: return
        if (!cierre.puedeCerrar) return

        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            val monto = cierre.montoValor ?: return@launch

            val error = repository.cerrarCaja(negocioId, monto, cierre.nota)

            // Con error la hoja se queda abierta: lo escrito no se pierde y se puede corregir.
            _uiState.update {
                it.copy(error = error, cierre = if (error == null) null else cierre)
            }
        }
    }

    private fun aCajaUi(saldo: SaldoDeCaja) = CajaAbiertaUi(
        montoInicial = saldo.caja.montoInicial,
        apertura = saldo.caja.fechaHoraApertura,
        ventas = saldo.ventas,
        gastos = saldo.gastos,
        costos = saldo.costos,
        saldoEsperado = saldo.saldoEsperado,
    )
}
