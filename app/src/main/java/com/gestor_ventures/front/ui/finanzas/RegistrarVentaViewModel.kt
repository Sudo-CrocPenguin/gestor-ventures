package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoVenta
import com.gestor_ventures.back.repository.ClienteRepository
import com.gestor_ventures.back.repository.MetodoPagoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.model.aDominio
import com.gestor_ventures.front.model.aUi
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
import javax.inject.Inject

/** Máximo de dígitos del monto, para que no se desborde la caja ni el Long. */
private const val MaxDigitosMonto = 12

/**
 * HU-11/HU-12. Maneja el formulario de venta en sus dos modalidades.
 *
 * El guardado va por [VentaRepository]: este ViewModel no sabe que existe una base de datos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RegistrarVentaViewModel @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val clienteRepository: ClienteRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    private val reloj: Reloj,
    private val metodoPagoRepository: MetodoPagoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrarVentaUiState(fechaHora = reloj.ahora()))
    val uiState: StateFlow<RegistrarVentaUiState> = _uiState.asStateFlow()

    init {
        // HU-29/HU-30. Los clientes del negocio, para poder decir a quién se le vendió.
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) {
                        flowOf(emptyList())
                    } else {
                        clienteRepository.clientesDeNegocio(negocioId)
                    }
                }
                .collect { clientes -> _uiState.update { it.copy(clientes = clientes) } }
        }

        // HU-11. Solo se ofrecen los métodos que el negocio tiene activos. Si el elegido se apagó,
        // el formulario pasa al primero que quede, para no guardar con uno que ya no se acepta.
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) flowOf(null) else metodoPagoRepository.activos(negocioId)
                }
                .collect { activos ->
                    val disponibles = activos?.map { it.aUi() }?.ifEmpty { null } ?: MetodoPagoUi.entries
                    _uiState.update {
                        it.copy(
                            metodosDisponibles = disponibles,
                            metodoPago = if (it.metodoPago in disponibles) it.metodoPago else disponibles.first(),
                        )
                    }
                }
        }
    }

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
     * HU-29/HU-30. Elige a quién se le vendió. Volver a tocar al mismo lo quita: es la forma
     * de dejar la venta sin cliente, que es lo normal en un mostrador.
     */
    fun onClienteChange(clienteId: Long?) {
        _uiState.update { estado ->
            estado.copy(clienteId = if (clienteId == estado.clienteId) null else clienteId)
        }
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
                clienteId = estado.clienteId,
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
