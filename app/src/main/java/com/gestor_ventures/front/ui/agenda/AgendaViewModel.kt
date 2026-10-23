package com.gestor_ventures.front.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.AnticipacionRecordatorio
import com.gestor_ventures.back.model.Cita
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.EstadoPedido
import com.gestor_ventures.back.model.Pedido
import com.gestor_ventures.back.model.ProductoPedido
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.back.repository.AgendaRepository
import com.gestor_ventures.back.repository.ClienteRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Una cantidad de más de 4 dígitos es un error de tipeo, no un pedido. */
private const val MaxDigitosCantidad = 4

/**
 * HU-24, HU-25 y HU-27. La agenda del negocio activo, en el periodo que se esté mirando.
 *
 * Escucha las citas, los pedidos y los clientes al mismo tiempo, así que la vista cambia sola
 * al agendar algo, al cambiar un estado, al moverse de periodo o al cambiar de negocio en el
 * menú. Solo se traen los días del periodo, incluidos los que ya pasaron.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val agendaRepository: AgendaRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    clienteRepository: ClienteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AgendaUiState(hoy = agendaRepository.hoy(), fecha = agendaRepository.hoy()),
    )
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()

    /** HU-27. La vista y la fecha elegidas; cambiarlas vuelve a pedir los datos del nuevo periodo. */
    private val periodo = MutableStateFlow(VistaAgenda.Semana to agendaRepository.hoy())

    init {
        viewModelScope.launch {
            combine(negocioActivoRepository.negocioActivo, periodo) { negocio, elegido ->
                negocio to elegido
            }.flatMapLatest { (negocio, elegido) ->
                if (negocio == null) {
                    flowOf(null)
                } else {
                    val rango = elegido.first.rango(elegido.second)
                    combine(
                        agendaRepository.citasEntre(negocio.id, rango.desde, rango.hasta),
                        agendaRepository.pedidosEntre(negocio.id, rango.desde, rango.hasta),
                        clienteRepository.clientesDeNegocio(negocio.id),
                    ) { citas, pedidos, clientes -> Datos(negocio.tipoActividad, citas, pedidos, clientes) }
                }
            }.collect { datos ->
                _uiState.update {
                    it.copy(
                        tipoActividad = datos?.tipoActividad,
                        citas = datos?.citas.orEmpty(),
                        pedidos = datos?.pedidos.orEmpty(),
                        clientes = datos?.clientes.orEmpty(),
                        hoy = agendaRepository.hoy(),
                        cargando = false,
                    )
                }
            }
        }
    }

    /** HU-27. Cambia entre día, semana y mes sin moverse de la fecha. */
    fun onVistaChange(vista: VistaAgenda) {
        cambiarPeriodo(vista, _uiState.value.fecha)
    }

    fun anterior() {
        val estado = _uiState.value
        cambiarPeriodo(estado.vista, estado.vista.mover(estado.fecha, -1))
    }

    fun siguiente() {
        val estado = _uiState.value
        cambiarPeriodo(estado.vista, estado.vista.mover(estado.fecha, 1))
    }

    /** HU-27. Vuelve al periodo que tiene a hoy, en la misma vista. */
    fun irAHoy() {
        cambiarPeriodo(_uiState.value.vista, agendaRepository.hoy())
    }

    /** HU-27. Tocar un día del mes abre la vista de ese día. */
    fun abrirDia(dia: LocalDate) {
        cambiarPeriodo(VistaAgenda.Dia, dia)
    }

    private fun cambiarPeriodo(vista: VistaAgenda, fecha: LocalDate) {
        periodo.value = vista to fecha
        _uiState.update { it.copy(vista = vista, fecha = fecha) }
    }

    fun abrirFormularioCita() {
        val fecha = fechaSugerida()
        _uiState.update { it.copy(formularioCita = FormularioCita(fecha = fecha), error = null) }
    }

    fun abrirFormularioPedido() {
        val fecha = fechaSugerida()
        _uiState.update { it.copy(formularioPedido = FormularioPedido(fechaEntrega = fecha), error = null) }
    }

    /**
     * HU-27. Si se está mirando un solo día, de hoy en adelante, la cita o el pedido nuevo ya
     * trae esa fecha: es casi seguro que es para ese día. En la semana o el mes no se adivina.
     */
    private fun fechaSugerida(): LocalDate? {
        val estado = _uiState.value
        return estado.fecha.takeIf { estado.vista == VistaAgenda.Dia && !it.isBefore(estado.hoy) }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formularioCita = null, formularioPedido = null, error = null) }
    }

    /** El cliente se elige igual en la cita y en el pedido; solo uno de los dos está abierto. */
    fun onClienteChange(clienteId: Long) {
        _uiState.update {
            it.copy(
                formularioCita = it.formularioCita?.copy(clienteId = clienteId),
                formularioPedido = it.formularioPedido?.copy(clienteId = clienteId),
                error = null,
            )
        }
    }

    fun onFechaCitaChange(fecha: LocalDate) {
        _uiState.update { it.copy(formularioCita = it.formularioCita?.copy(fecha = fecha), error = null) }
    }

    fun onHoraCitaChange(hora: LocalTime) {
        _uiState.update { it.copy(formularioCita = it.formularioCita?.copy(hora = hora), error = null) }
    }

    fun onDescripcionCitaChange(texto: String) {
        _uiState.update {
            it.copy(formularioCita = it.formularioCita?.copy(descripcion = texto), error = null)
        }
    }

    fun onAnticipacionChange(anticipacion: AnticipacionRecordatorio?) {
        _uiState.update {
            it.copy(
                formularioCita = it.formularioCita?.copy(anticipacion = anticipacion),
                formularioPedido = it.formularioPedido?.copy(anticipacion = anticipacion),
            )
        }
    }

    fun onFechaEntregaChange(fecha: LocalDate) {
        _uiState.update {
            it.copy(formularioPedido = it.formularioPedido?.copy(fechaEntrega = fecha), error = null)
        }
    }

    fun onProductoNombreChange(indice: Int, texto: String) {
        actualizarProducto(indice) { it.copy(nombre = texto) }
    }

    /** Solo deja escribir números. */
    fun onProductoCantidadChange(indice: Int, texto: String) {
        actualizarProducto(indice) { it.copy(cantidad = texto.filter(Char::isDigit).take(MaxDigitosCantidad)) }
    }

    fun agregarProducto() {
        _uiState.update { estado ->
            val formulario = estado.formularioPedido ?: return@update estado
            estado.copy(formularioPedido = formulario.copy(productos = formulario.productos + FormularioProducto()))
        }
    }

    /** Siempre queda al menos una fila: un pedido sin productos no se puede registrar. */
    fun quitarProducto(indice: Int) {
        _uiState.update { estado ->
            val formulario = estado.formularioPedido ?: return@update estado
            if (formulario.productos.size <= 1) return@update estado
            estado.copy(
                formularioPedido = formulario.copy(
                    productos = formulario.productos.filterIndexed { i, _ -> i != indice },
                ),
                error = null,
            )
        }
    }

    private fun actualizarProducto(indice: Int, cambio: (FormularioProducto) -> FormularioProducto) {
        _uiState.update { estado ->
            val formulario = estado.formularioPedido ?: return@update estado
            estado.copy(
                formularioPedido = formulario.copy(
                    productos = formulario.productos.mapIndexed { i, p -> if (i == indice) cambio(p) else p },
                ),
                error = null,
            )
        }
    }

    /**
     * Agenda la cita, o guarda los cambios si se estaba editando. Después de editar se ofrece
     * avisarle al cliente (HU-28); al agendar una nueva no, porque el cliente ya la pidió.
     *
     * Si el repositorio rechaza la cita, la hoja se queda abierta con el aviso.
     */
    fun guardarCita() {
        val formulario = _uiState.value.formularioCita ?: return
        val clienteId = formulario.clienteId ?: return
        val fecha = formulario.fecha ?: return
        val hora = formulario.hora ?: return

        viewModelScope.launch {
            val error = if (formulario.citaId == null) {
                val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
                agendaRepository.agendarCita(
                    negocioId = negocioId,
                    clienteId = clienteId,
                    fecha = fecha,
                    hora = hora,
                    descripcion = formulario.descripcion,
                    anticipacion = formulario.anticipacion,
                )
            } else {
                agendaRepository.editarCita(
                    citaId = formulario.citaId,
                    clienteId = clienteId,
                    fecha = fecha,
                    hora = hora,
                    descripcion = formulario.descripcion,
                    anticipacion = formulario.anticipacion,
                )
            }
            if (error != null) {
                _uiState.update { it.copy(error = error) }
                return@launch
            }
            cerrarFormulario()
            if (formulario.esEdicion) avisar(TipoAviso.CitaCambiada, clienteId, fecha, hora)
        }
    }

    /**
     * Registra el pedido, o guarda los cambios si se estaba editando; después de editar se
     * ofrece avisarle al cliente (HU-28).
     *
     * Las filas sin nombre se descartan antes de guardar: es la fila vacía que quedó al tocar
     * "Agregar otro producto" de más, no un producto. Si la cantidad no es un número válido se
     * manda 0 y el repositorio la rechaza con su aviso.
     */
    fun guardarPedido() {
        val formulario = _uiState.value.formularioPedido ?: return
        val clienteId = formulario.clienteId ?: return
        val fechaEntrega = formulario.fechaEntrega ?: return
        val productos = formulario.productos
            .filterNot { it.nombre.isBlank() }
            .map { ProductoPedido(nombre = it.nombre, cantidad = it.cantidad.toIntOrNull() ?: 0) }

        viewModelScope.launch {
            val error = if (formulario.pedidoId == null) {
                val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
                agendaRepository.registrarPedido(
                    negocioId = negocioId,
                    clienteId = clienteId,
                    fechaEntrega = fechaEntrega,
                    productos = productos,
                    anticipacion = formulario.anticipacion,
                )
            } else {
                agendaRepository.editarPedido(
                    pedidoId = formulario.pedidoId,
                    clienteId = clienteId,
                    fechaEntrega = fechaEntrega,
                    productos = productos,
                    anticipacion = formulario.anticipacion,
                )
            }
            if (error != null) {
                _uiState.update { it.copy(error = error) }
                return@launch
            }
            cerrarFormulario()
            if (formulario.esEdicion) avisar(TipoAviso.PedidoCambiado, clienteId, fechaEntrega)
        }
    }

    /** HU-28. Tocar una cita abre el menú para editarla o cancelarla. */
    fun abrirCita(citaId: Long) {
        _uiState.update { estado -> estado.copy(citaElegida = estado.citas.firstOrNull { it.id == citaId }) }
    }

    fun cerrarCita() {
        _uiState.update { it.copy(citaElegida = null) }
    }

    /** HU-28. Abre la hoja de la cita ya llena con lo que tiene guardado. */
    fun editarCitaElegida() {
        val cita = _uiState.value.citaElegida ?: return
        _uiState.update {
            it.copy(
                citaElegida = null,
                error = null,
                formularioCita = FormularioCita(
                    citaId = cita.id,
                    clienteId = cita.clienteId,
                    fecha = cita.fecha,
                    hora = cita.hora,
                    descripcion = cita.descripcion.orEmpty(),
                ),
            )
        }
        viewModelScope.launch {
            val anticipacion = agendaRepository.anticipacionDeCita(cita.id)
            _uiState.update {
                it.copy(
                    formularioCita = it.formularioCita
                        ?.takeIf { formulario -> formulario.citaId == cita.id }
                        ?.copy(anticipacion = anticipacion),
                )
            }
        }
    }

    /** HU-28. Abre la hoja del pedido ya llena, con sus productos. */
    fun editarPedidoElegido() {
        val pedido = _uiState.value.pedidoElegido ?: return
        _uiState.update {
            it.copy(
                pedidoElegido = null,
                error = null,
                formularioPedido = FormularioPedido(
                    pedidoId = pedido.id,
                    clienteId = pedido.clienteId,
                    fechaEntrega = pedido.fechaEntrega,
                    productos = pedido.productos.map { producto ->
                        FormularioProducto(nombre = producto.nombre, cantidad = producto.cantidad.toString())
                    },
                ),
            )
        }
        viewModelScope.launch {
            val anticipacion = agendaRepository.anticipacionDePedido(pedido.id)
            _uiState.update {
                it.copy(
                    formularioPedido = it.formularioPedido
                        ?.takeIf { formulario -> formulario.pedidoId == pedido.id }
                        ?.copy(anticipacion = anticipacion),
                )
            }
        }
    }

    /** HU-28. Cancelar no tiene vuelta atrás, así que del menú se pasa a una confirmación. */
    fun pedirConfirmacionDeCancelar() {
        _uiState.update { estado ->
            val elegido = estado.citaElegida?.let(PorCancelar::DeCita)
                ?: estado.pedidoElegido?.let(PorCancelar::DePedido)
                ?: return@update estado
            estado.copy(citaElegida = null, pedidoElegido = null, porCancelar = elegido)
        }
    }

    fun descartarCancelacion() {
        _uiState.update { it.copy(porCancelar = null) }
    }

    /** HU-28. Cancela lo elegido y, si salió bien, ofrece avisarle al cliente. */
    fun confirmarCancelacion() {
        val elegido = _uiState.value.porCancelar ?: return
        _uiState.update { it.copy(porCancelar = null) }

        viewModelScope.launch {
            when (elegido) {
                is PorCancelar.DeCita -> {
                    val cita = elegido.cita
                    if (agendaRepository.cancelarCita(cita.id) == null) {
                        avisar(TipoAviso.CitaCancelada, cita.clienteId, cita.fecha, cita.hora)
                    }
                }
                is PorCancelar.DePedido -> {
                    val pedido = elegido.pedido
                    if (agendaRepository.cancelarPedido(pedido.id) == null) {
                        avisar(TipoAviso.PedidoCancelado, pedido.clienteId, pedido.fechaEntrega)
                    }
                }
            }
        }
    }

    fun cerrarAviso() {
        _uiState.update { it.copy(aviso = null) }
    }

    /**
     * HU-28. Prepara el aviso al cliente. Si no tiene teléfono no hay a quién escribirle por
     * WhatsApp, así que no se muestra nada.
     */
    private fun avisar(tipo: TipoAviso, clienteId: Long, fecha: LocalDate, hora: LocalTime? = null) {
        val cliente = _uiState.value.clientes.firstOrNull { it.id == clienteId } ?: return
        val telefono = cliente.telefono ?: return
        _uiState.update {
            it.copy(
                aviso = AvisoAlCliente(
                    tipo = tipo,
                    nombreCliente = cliente.nombre,
                    telefono = telefono,
                    fecha = fecha,
                    hora = hora,
                ),
            )
        }
    }

    /** Tocar un pedido abre el menú para cambiar su estado. */
    fun abrirPedido(pedidoId: Long) {
        _uiState.update { estado -> estado.copy(pedidoElegido = estado.pedidos.firstOrNull { it.id == pedidoId }) }
    }

    fun cerrarPedido() {
        _uiState.update { it.copy(pedidoElegido = null) }
    }

    fun cambiarEstadoPedido(estado: EstadoPedido) {
        val pedido = _uiState.value.pedidoElegido ?: return
        _uiState.update { it.copy(pedidoElegido = null) }
        viewModelScope.launch { agendaRepository.cambiarEstadoPedido(pedido.id, estado) }
    }

    private data class Datos(
        val tipoActividad: TipoActividad,
        val citas: List<Cita>,
        val pedidos: List<Pedido>,
        val clientes: List<Cliente>,
    )
}
