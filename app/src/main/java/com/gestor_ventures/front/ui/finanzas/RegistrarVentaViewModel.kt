package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

/** Máximo de dígitos del monto, para que no se desborde la caja ni el Long. */
private const val MaxDigitosMonto = 12

/**
 * HU-11/HU-12. Maneja el formulario de venta en sus dos modalidades.
 *
 * [ahora] se inyecta para poder fijar el reloj en las pruebas; en la app es la hora real.
 */
class RegistrarVentaViewModel(
    private val ahora: () -> LocalDateTime = LocalDateTime::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrarVentaUiState(fechaHora = ahora()))
    val uiState: StateFlow<RegistrarVentaUiState> = _uiState.asStateFlow()

    /** Alterna entre venta detallada y rápida conservando lo que el usuario ya escribió. */
    fun onTipoRegistroChange(tipo: TipoRegistroVentaUi) {
        _uiState.update { it.copy(tipoRegistro = tipo) }
    }

    /** Acepta solo dígitos: el usuario escribe "25000" y la UI muestra "25.000". */
    fun onMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update { it.copy(monto = digitos) }
    }

    fun onProductoServicioChange(texto: String) {
        _uiState.update { it.copy(productoServicio = texto) }
    }

    fun onClienteChange(texto: String) {
        _uiState.update { it.copy(cliente = texto) }
    }

    /** El método de pago solo puede ser uno de [MetodoPagoUi], no texto libre. */
    fun onMetodoPagoChange(metodo: MetodoPagoUi) {
        _uiState.update { it.copy(metodoPago = metodo) }
    }

    fun onNotaChange(texto: String) {
        _uiState.update { it.copy(nota = texto) }
    }

    /**
     * Registra la venta con la fecha y hora del momento (el usuario no las escribe) y deja el
     * formulario listo para la siguiente, en la misma modalidad.
     *
     * Todavía no persiste: falta `VentaRepository` (ARCHITECTURE.md §9). Cuando exista, recibirá
     * este estado ya validado, incluida [RegistrarVentaUiState.fechaHora].
     */
    fun guardar() {
        val momento = ahora()
        _uiState.update { anterior ->
            RegistrarVentaUiState(tipoRegistro = anterior.tipoRegistro, fechaHora = momento)
        }
    }
}
