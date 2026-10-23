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

/** Las dos partes de la agenda. Cuál se ve depende del tipo de negocio. */
enum class SeccionAgenda { Citas, Pedidos }

/** HU-24 y HU-25. Estado de la pestaña Agenda. */
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
    /** El pedido que el usuario tocó, mientras elige a qué estado pasarlo. */
    val pedidoElegido: Pedido? = null,
    val citaElegida: Cita? = null,
    val porCancelar: PorCancelar? = null,
    val aviso: AvisoAlCliente? = null,
    val error: ErrorAgenda? = null,
) {
    /** HU-05. Servicios ve citas, productos ve pedidos y un negocio mixto ve las dos. */
    val secciones: List<SeccionAgenda>
        get() = when (tipoActividad) {
            TipoActividad.SERVICIOS -> listOf(SeccionAgenda.Citas)
            TipoActividad.PRODUCTOS -> listOf(SeccionAgenda.Pedidos)
            TipoActividad.MIXTO -> SeccionAgenda.entries
            null -> emptyList()
        }

    /**
     * La sección que se muestra. Si la elegida ya no aplica —porque el usuario cambió el tipo
     * de negocio o de negocio activo— se muestra la primera que sí aplique.
     */
    val seccionVisible: SeccionAgenda?
        get() = seccion.takeIf { it in secciones } ?: secciones.firstOrNull()

    /** Sin clientes no hay a quién agendarle: se avisa y el botón queda apagado. */
    val sinClientes: Boolean get() = !cargando && clientes.isEmpty()

    val vacio: Boolean
        get() = !cargando && when (seccionVisible) {
            SeccionAgenda.Citas -> citas.isEmpty()
            SeccionAgenda.Pedidos -> pedidos.isEmpty()
            null -> true
        }
}

/** HU-24. La cita mientras se escribe. Los campos son opcionales porque al principio están vacíos. */
data class FormularioCita(
    val citaId: Long? = null,
    val clienteId: Long? = null,
    val fecha: LocalDate? = null,
    val hora: LocalTime? = null,
    val descripcion: String = "",
) {
    val puedeGuardar: Boolean get() = clienteId != null && fecha != null && hora != null

    val esEdicion: Boolean get() = citaId != null
}

/** Una fila de producto del pedido. La cantidad es texto porque así se escribe en el campo. */
data class FormularioProducto(
    val nombre: String = "",
    val cantidad: String = "1",
)

/** HU-25. El pedido mientras se escribe. Arranca con una fila de producto vacía. */
data class FormularioPedido(
    val pedidoId: Long? = null,
    val clienteId: Long? = null,
    val fechaEntrega: LocalDate? = null,
    val productos: List<FormularioProducto> = listOf(FormularioProducto()),
) {
    val puedeGuardar: Boolean
        get() = clienteId != null && fechaEntrega != null &&
            productos.any { it.nombre.isNotBlank() }

    val esEdicion: Boolean get() = pedidoId != null
}

sealed interface PorCancelar {
    data class DeCita(val cita: Cita) : PorCancelar
    data class DePedido(val pedido: Pedido) : PorCancelar
}

enum class TipoAviso { CitaCambiada, CitaCancelada, PedidoCambiado, PedidoCancelado }

data class AvisoAlCliente(
    val tipo: TipoAviso,
    val nombreCliente: String,
    val telefono: String,
    val fecha: LocalDate,
    val hora: LocalTime? = null,
)

/** Texto que ve el usuario para cada regla de la agenda. */
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
    ErrorAgenda.CitaNoExiste -> R.string.agenda_error_cita_no_existe
    ErrorAgenda.CitaCancelada -> R.string.agenda_error_cita_cancelada
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
