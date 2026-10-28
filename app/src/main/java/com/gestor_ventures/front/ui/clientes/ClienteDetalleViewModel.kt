package com.gestor_ventures.front.ui.clientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.ErrorRecordatorio
import com.gestor_ventures.back.model.RecordatorioCliente
import com.gestor_ventures.back.model.TipoRecordatorio
import com.gestor_ventures.back.model.Venta
import com.gestor_ventures.back.model.enlaceDeWhatsApp
import com.gestor_ventures.back.repository.ClienteRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.RecordatorioClienteRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.front.navigation.ArgumentoClienteId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-30 y HU-32. El perfil de un cliente, su historial de compras y los recordatorios que se
 * le han mandado.
 *
 * El cliente, sus ventas y sus recordatorios se siguen por separado y se juntan acá: así,
 * corregirle el nombre desde la lista se ve en el perfil, y registrarle una venta aparece en el
 * historial, sin que ninguna de las dos pantallas tenga que avisarle a la otra.
 */
@HiltViewModel
class ClienteDetalleViewModel @Inject constructor(
    clienteRepository: ClienteRepository,
    ventaRepository: VentaRepository,
    negocioActivoRepository: NegocioActivoRepository,
    private val recordatorioRepository: RecordatorioClienteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val clienteId: Long =
        savedStateHandle.get<String>(ArgumentoClienteId)?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ClienteDetalleUiState())
    val uiState: StateFlow<ClienteDetalleUiState> = _uiState.asStateFlow()

    /**
     * HU-32. Avisa una sola vez que ya quedó constancia del recordatorio y que WhatsApp puede
     * abrirse. Va por aquí y no en el estado para que al volver a la app no se abra otra vez.
     */
    private val _abrirWhatsApp = Channel<String>(Channel.BUFFERED)
    val abrirWhatsApp: Flow<String> = _abrirWhatsApp.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                clienteRepository.observarCliente(clienteId),
                ventaRepository.ventasDeCliente(clienteId),
                recordatorioRepository.recordatoriosDeCliente(clienteId),
                negocioActivoRepository.negocioActivo,
            ) { cliente, ventas, recordatorios, negocio ->
                // Se copia sobre el estado en vez de reemplazarlo: si llega una venta nueva
                // mientras la hoja del recordatorio está abierta, lo escrito no se pierde.
                { anterior: ClienteDetalleUiState ->
                    anterior.copy(
                        cliente = cliente,
                        negocio = negocio?.nombre.orEmpty(),
                        compras = ventas.map(::aCompraUi),
                        recordatorios = recordatorios.map(::aRecordatorioUi),
                        cargando = false,
                    )
                }
            }.collect { aplicar -> _uiState.update(aplicar) }
        }
    }

    // ---------- HU-32: el recordatorio ----------

    /** Abre la hoja con la plantilla del tipo que viene marcado por defecto. */
    fun abrirRecordatorio(plantilla: String) {
        _uiState.update {
            it.copy(recordatorio = FormularioRecordatorio(mensaje = plantilla), errorRecordatorio = null)
        }
    }

    fun cerrarRecordatorio() {
        _uiState.update { it.copy(recordatorio = null, errorRecordatorio = null) }
    }

    /**
     * Cambia el tipo y con él el mensaje: la plantilla del otro tipo dice otra cosa, así que
     * dejar la anterior escrita sería peor que reemplazarla.
     */
    fun onTipoRecordatorioChange(tipo: TipoRecordatorio, plantilla: String) {
        _uiState.update { estado ->
            estado.copy(
                recordatorio = estado.recordatorio?.copy(tipo = tipo, mensaje = plantilla),
                errorRecordatorio = null,
            )
        }
    }

    fun onMensajeRecordatorioChange(texto: String) {
        _uiState.update { estado ->
            estado.copy(
                recordatorio = estado.recordatorio?.copy(mensaje = texto),
                errorRecordatorio = null,
            )
        }
    }

    /**
     * HU-32. Deja constancia del recordatorio y luego abre WhatsApp con el mensaje escrito.
     *
     * Primero se guarda y después se abre: si el guardado falla, la hoja se queda con el texto
     * en vez de mandar un mensaje del que no quedaría rastro en el perfil. Lo que no se puede
     * saber desde acá es si el usuario de verdad lo envió en WhatsApp; lo que se registra es que lo
     * mandó a enviar.
     */
    fun enviarRecordatorio() {
        val estado = _uiState.value
        val formulario = estado.recordatorio ?: return
        val cliente = estado.cliente ?: return

        val enlace = enlaceDeWhatsApp(cliente.telefono, formulario.mensaje)
        if (enlace == null) {
            _uiState.update { it.copy(errorRecordatorio = ErrorRecordatorio.ClienteSinTelefono) }
            return
        }

        viewModelScope.launch {
            val error = recordatorioRepository.registrarEnvio(
                clienteId = cliente.id,
                tipo = formulario.tipo,
                mensaje = formulario.mensaje,
            )
            if (error != null) {
                _uiState.update { it.copy(errorRecordatorio = error) }
                return@launch
            }

            _uiState.update { it.copy(recordatorio = null, errorRecordatorio = null) }
            _abrirWhatsApp.send(enlace)
        }
    }

    /**
     * Una venta rápida no dice qué se vendió —es el total del día—, así que la fila se queda
     * con el monto y la fecha en vez de inventarle un nombre.
     */
    private fun aCompraUi(venta: Venta) = CompraUi(
        id = venta.id,
        titulo = venta.productoServicio.orEmpty(),
        monto = venta.monto,
        fechaHora = venta.fechaHora,
    )

    private fun aRecordatorioUi(recordatorio: RecordatorioCliente) = RecordatorioUi(
        id = recordatorio.id,
        tipo = recordatorio.tipo,
        mensaje = recordatorio.mensaje,
        fechaEnvio = recordatorio.fechaEnvio,
    )
}
