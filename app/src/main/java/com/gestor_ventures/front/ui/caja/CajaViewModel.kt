package com.gestor_ventures.front.ui.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Caja
import com.gestor_ventures.back.repository.CajaRepository
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

/** Tope de dígitos del monto, el mismo que usan los demás campos de dinero. */
private const val MaxDigitosMonto = 12

/**
 * HU-19. Abrir la jornada de caja del negocio activo.
 *
 * La jornada la manda el repositorio y lo que el usuario está escribiendo vive acá: así el
 * campo responde al instante y, al abrir, la pantalla cambia sola sin tener que pedir nada
 * de vuelta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CajaViewModel @Inject constructor(
    private val repository: CajaRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CajaUiState())
    val uiState: StateFlow<CajaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) flowOf(null) else repository.cajaAbierta(negocioId)
                }
                .collect { caja ->
                    _uiState.update { it.copy(caja = caja?.let(::aCajaUi), cargando = false) }
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

    private fun aCajaUi(caja: Caja) = CajaAbiertaUi(
        montoInicial = caja.montoInicial,
        apertura = caja.fechaHoraApertura,
    )
}
