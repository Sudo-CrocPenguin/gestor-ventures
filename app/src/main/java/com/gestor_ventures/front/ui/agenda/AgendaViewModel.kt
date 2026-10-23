package com.gestor_ventures.front.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

private const val MesesHaciaAdelante = 12L

private const val MaxDigitosCantidad = 4

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val agendaRepository: AgendaRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    clienteRepository: ClienteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgendaUiState(hoy = agendaRepository.hoy()))
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivo.flatMapLatest { negocio ->
                if (negocio == null) {
                    flowOf(null)
                } else {
                    val hoy = agendaRepository.hoy()
                    val hasta = hoy.plusMonths(MesesHaciaAdelante)
                    combine(
                        agendaRepository.citasEntre(negocio.id, hoy, hasta),
                        agendaRepository.pedidosEntre(negocio.id, hoy, hasta),
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

    fun onSeccionChange(seccion: SeccionAgenda) {
        _uiState.update { it.copy(seccion = seccion) }
    }

    fun abrirFormularioCita() {
        _uiState.update { it.copy(formularioCita = FormularioCita(), error = null) }
    }

    fun abrirFormularioPedido() {
        _uiState.update { it.copy(formularioPedido = FormularioPedido(), error = null) }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formularioCita = null, formularioPedido = null, error = null) }
    }

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

    fun onFechaEntregaChange(fecha: LocalDate) {
        _uiState.update {
            it.copy(formularioPedido = it.formularioPedido?.copy(fechaEntrega = fecha), error = null)
        }
    }

    fun onProductoNombreChange(indice: Int, texto: String) {
        actualizarProducto(indice) { it.copy(nombre = texto) }
    }

    fun onProductoCantidadChange(indice: Int, texto: String) {
        actualizarProducto(indice) { it.copy(cantidad = texto.filter(Char::isDigit).take(MaxDigitosCantidad)) }
    }

    fun agregarProducto() {
        _uiState.update { estado ->
            val formulario = estado.formularioPedido ?: return@update estado
            estado.copy(formularioPedido = formulario.copy(productos = formulario.productos + FormularioProducto()))
        }
    }

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

    fun guardarCita() {
        val formulario = _uiState.value.formularioCita ?: return
        val clienteId = formulario.clienteId ?: return
        val fecha = formulario.fecha ?: return
        val hora = formulario.hora ?: return

        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            val error = agendaRepository.agendarCita(
                negocioId = negocioId,
                clienteId = clienteId,
                fecha = fecha,
                hora = hora,
                descripcion = formulario.descripcion,
            )
            if (error == null) cerrarFormulario() else _uiState.update { it.copy(error = error) }
        }
    }

    fun guardarPedido() {
        val formulario = _uiState.value.formularioPedido ?: return
        val clienteId = formulario.clienteId ?: return
        val fechaEntrega = formulario.fechaEntrega ?: return
        val productos = formulario.productos
            .filterNot { it.nombre.isBlank() }
            .map { ProductoPedido(nombre = it.nombre, cantidad = it.cantidad.toIntOrNull() ?: 0) }

        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
            val error = agendaRepository.registrarPedido(
                negocioId = negocioId,
                clienteId = clienteId,
                fechaEntrega = fechaEntrega,
                productos = productos,
            )
            if (error == null) cerrarFormulario() else _uiState.update { it.copy(error = error) }
        }
    }

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
