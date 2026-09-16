package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoVenta
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.model.aDominio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Máximo de dígitos del monto, para que no se desborde la caja ni el Long. */
private const val MaxDigitosMonto = 12

/**
 * HU-11/HU-12. Maneja el formulario de venta en sus dos modalidades.
 *
 * El guardado va por [VentaRepository]: este ViewModel no sabe que existe una base de datos.
 */
@HiltViewModel
class RegistrarVentaViewModel @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    private val reloj: Reloj,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrarVentaUiState(fechaHora = reloj.ahora()))
    val uiState: StateFlow<RegistrarVentaUiState> = _uiState.asStateFlow()

    /** Avisa una sola vez que la venta quedó guardada, para que la pantalla se cierre. */
    private val _ventaGuardada = Channel<TipoRegistroVentaUi>(Channel.BUFFERED)
    val ventaGuardada: Flow<TipoRegistroVentaUi> = _ventaGuardada.receiveAsFlow()

    /** Alterna entre venta detallada y rápida conservando lo que el usuario ya escribió. */
    fun onTipoRegistroChange(tipo: TipoRegistroVentaUi) {
        _uiState.update { it.copy(tipoRegistro = tipo, error = null) }
    }

    /** Acepta solo dígitos: el usuario escribe "25000" y la UI muestra "25.000". */
    fun onMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update { it.copy(monto = digitos, error = null) }
    }

    fun onProductoServicioChange(texto: String) {
        _uiState.update { it.copy(productoServicio = texto, error = null) }
    }

    /** El método de pago solo puede ser uno de [MetodoPagoUi], no texto libre. */
    fun onMetodoPagoChange(metodo: MetodoPagoUi) {
        _uiState.update { it.copy(metodoPago = metodo) }
    }

    fun onNotaChange(texto: String) {
        _uiState.update { it.copy(nota = texto) }
    }

    /**
     * Registra la venta con la fecha y hora del momento: el usuario no las escribe, y si tuvo
     * el formulario abierto un rato, la hora que vale es la de guardar y no la de abrirlo.
     */
    fun guardar() {
        val estado = _uiState.value
        if (!estado.puedeGuardar) return

        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first()
            if (negocioId == null) {
                _uiState.update { it.copy(guardando = false, error = ErrorVentaUi.SinNegocio) }
                return@launch
            }

            val resultado = ventaRepository.registrarVenta(
                negocioId = negocioId,
                tipoRegistro = estado.tipoRegistro.aDominio(),
                monto = estado.montoValor.toDouble(),
                fechaHora = reloj.ahora(),
                productoServicio = estado.productoServicio,
                metodoPago = estado.metodoPago.aDominio(),
                nota = estado.nota,
                // El cliente espera a la épica de clientes: hoy la venta guarda un id de
                // cliente y en el formulario solo hay un nombre suelto.
                clienteId = null,
            )
            when (resultado) {
                is ResultadoVenta.Exito -> {
                    // Lista para la siguiente venta, en la misma modalidad.
                    _uiState.value = RegistrarVentaUiState(
                        tipoRegistro = estado.tipoRegistro,
                        fechaHora = reloj.ahora(),
                    )
                    _ventaGuardada.send(estado.tipoRegistro)
                }

                is ResultadoVenta.Invalido -> _uiState.update {
                    it.copy(guardando = false, error = ErrorVentaUi.Regla(resultado.error))
                }
            }
        }
    }
}
