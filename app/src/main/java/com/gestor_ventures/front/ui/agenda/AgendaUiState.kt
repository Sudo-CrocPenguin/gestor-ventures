package com.gestor_ventures.front.ui.agenda

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cita
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.ErrorAgenda
import com.gestor_ventures.back.model.EstadoCita
import com.gestor_ventures.back.model.EstadoPedido
import com.gestor_ventures.back.model.Pedido
import com.gestor_ventures.back.model.TipoActividad
import java.time.LocalDate
import java.time.LocalTime

enum class SeccionAgenda { Citas, Pedidos }

data class AgendaUiState(
    val tipoActividad: TipoActividad? = null,
    val seccion: SeccionAgenda = SeccionAgenda.Citas,
    val citas: List<Cita> = emptyList(),
    val pedidos: List<Pedido> = emptyList(),
    val clientes: List<Cliente> = emptyList(),
    val hoy: LocalDate = LocalDate.now(),
    val cargando: Boolean = true,
    val formularioCita: FormularioCita? = null,
    val formularioPedido: FormularioPedido? = null,
    val pedidoElegido: Pedido? = null,
    val error: ErrorAgenda? = null,
) {
    val secciones: List<SeccionAgenda>
        get() = when (tipoActividad) {
            TipoActividad.SERVICIOS -> listOf(SeccionAgenda.Citas)
            TipoActividad.PRODUCTOS -> listOf(SeccionAgenda.Pedidos)
            TipoActividad.MIXTO -> SeccionAgenda.entries
            null -> emptyList()
        }

    val seccionVisible: SeccionAgenda?
        get() = seccion.takeIf { it in secciones } ?: secciones.firstOrNull()

    val sinClientes: Boolean get() = !cargando && clientes.isEmpty()

    val vacio: Boolean
        get() = !cargando && when (seccionVisible) {
            SeccionAgenda.Citas -> citas.isEmpty()
            SeccionAgenda.Pedidos -> pedidos.isEmpty()
            null -> true
        }
}

data class FormularioCita(
    val clienteId: Long? = null,
    val fecha: LocalDate? = null,
    val hora: LocalTime? = null,
    val descripcion: String = "",
) {
    val puedeGuardar: Boolean get() = clienteId != null && fecha != null && hora != null
}

data class FormularioProducto(
    val nombre: String = "",
    val cantidad: String = "1",
)

data class FormularioPedido(
    val clienteId: Long? = null,
    val fechaEntrega: LocalDate? = null,
    val productos: List<FormularioProducto> = listOf(FormularioProducto()),
) {
    val puedeGuardar: Boolean
        get() = clienteId != null && fechaEntrega != null &&
            productos.any { it.nombre.isNotBlank() }
}

@StringRes
fun ErrorAgenda.mensajeRes(): Int = when (this) {
    ErrorAgenda.ClienteNoExiste -> R.string.agenda_error_cliente
    ErrorAgenda.NoAplicaAlNegocio -> R.string.agenda_error_no_aplica
    ErrorAgenda.CitaEnElPasado -> R.string.agenda_error_cita_pasado
    ErrorAgenda.HorarioOcupado -> R.string.agenda_error_horario_ocupado
    ErrorAgenda.DescripcionMuyLarga -> R.string.agenda_error_descripcion_larga
    ErrorAgenda.SinProductos -> R.string.agenda_error_sin_productos
    ErrorAgenda.ProductoSinNombre -> R.string.agenda_error_producto_sin_nombre
    ErrorAgenda.ProductoMuyLargo -> R.string.agenda_error_producto_largo
    ErrorAgenda.CantidadNoPositiva -> R.string.agenda_error_cantidad
    ErrorAgenda.EntregaEnElPasado -> R.string.agenda_error_entrega_pasado
    ErrorAgenda.PedidoNoExiste -> R.string.agenda_error_pedido_no_existe
    ErrorAgenda.PedidoCancelado -> R.string.agenda_error_pedido_cancelado
}

@StringRes
fun EstadoCita.labelRes(): Int = when (this) {
    EstadoCita.PROGRAMADA -> R.string.agenda_cita_programada
    EstadoCita.COMPLETADA -> R.string.agenda_cita_completada
    EstadoCita.CANCELADA -> R.string.agenda_cita_cancelada
}

@StringRes
fun EstadoPedido.labelRes(): Int = when (this) {
    EstadoPedido.PENDIENTE -> R.string.agenda_pedido_pendiente
    EstadoPedido.EN_PROCESO -> R.string.agenda_pedido_en_proceso
    EstadoPedido.ENTREGADO -> R.string.agenda_pedido_entregado
    EstadoPedido.CANCELADO -> R.string.agenda_pedido_cancelado
}
