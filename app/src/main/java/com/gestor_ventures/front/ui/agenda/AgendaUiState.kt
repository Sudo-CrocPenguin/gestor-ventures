package com.gestor_ventures.front.ui.agenda

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cita
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.ErrorAgenda
import com.gestor_ventures.back.model.EstadoCita
import com.gestor_ventures.back.model.EstadoPedido
import com.gestor_ventures.back.model.Pedido
import com.gestor_ventures.back.model.RangoFechas
import com.gestor_ventures.back.model.TipoActividad
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** HU-27. Cómo se mira la agenda: un día, una semana o un mes. */
enum class VistaAgenda { Dia, Semana, Mes }

/**
 * HU-27. Los días que entran en la vista, alrededor de [fecha]. La semana va de lunes a
 * domingo y el mes, del día 1 al último.
 */
fun VistaAgenda.rango(fecha: LocalDate): RangoFechas = when (this) {
    VistaAgenda.Dia -> RangoFechas(fecha, fecha)
    VistaAgenda.Semana -> fecha.with(DayOfWeek.MONDAY).let { RangoFechas(it, it.plusDays(6)) }
    VistaAgenda.Mes -> RangoFechas(fecha.withDayOfMonth(1), fecha.withDayOfMonth(fecha.lengthOfMonth()))
}

/** HU-27. La fecha un periodo hacia atrás o hacia adelante: lo que hacen las flechas. */
fun VistaAgenda.mover(fecha: LocalDate, pasos: Long): LocalDate = when (this) {
    VistaAgenda.Dia -> fecha.plusDays(pasos)
    VistaAgenda.Semana -> fecha.plusWeeks(pasos)
    VistaAgenda.Mes -> fecha.plusMonths(pasos)
}

/** HU-24, HU-25 y HU-27. Estado de la pestaña Agenda. */
data class AgendaUiState(
    val tipoActividad: TipoActividad? = null,
    /** HU-27. Arranca en la semana: es lo que más sirve para ver la carga de trabajo. */
    val vista: VistaAgenda = VistaAgenda.Semana,
    /** HU-27. El día alrededor del cual se arma la vista. */
    val fecha: LocalDate = LocalDate.now(),
    val citas: List<Cita> = emptyList(),
    val pedidos: List<Pedido> = emptyList(),
    val clientes: List<Cliente> = emptyList(),
    val hoy: LocalDate = LocalDate.now(),
    val cargando: Boolean = true,
    val formularioCita: FormularioCita? = null,
    val formularioPedido: FormularioPedido? = null,
    /** El pedido que el usuario tocó, mientras elige a qué estado pasarlo. */
    val pedidoElegido: Pedido? = null,
    /** HU-28. La cita que el usuario tocó, mientras elige si editarla o cancelarla. */
    val citaElegida: Cita? = null,
    /** HU-28. Lo que está a punto de cancelarse, esperando confirmación. */
    val porCancelar: PorCancelar? = null,
    /** HU-28. El aviso para mandarle al cliente después de un cambio, si tiene teléfono. */
    val aviso: AvisoAlCliente? = null,
    val error: ErrorAgenda? = null,
) {
    /** HU-05. Servicios ve citas, productos ve pedidos y un negocio mixto ve las dos. */
    /** HU-05. Un negocio de servicios o mixto agenda citas. */
    val ofreceCitas: Boolean
        get() = tipoActividad == TipoActividad.SERVICIOS || tipoActividad == TipoActividad.MIXTO

    /** HU-05. Un negocio de productos o mixto registra pedidos. */
    val ofrecePedidos: Boolean
        get() = tipoActividad == TipoActividad.PRODUCTOS || tipoActividad == TipoActividad.MIXTO

    val rango: RangoFechas get() = vista.rango(fecha)

    /** Cada día del periodo, en orden. */
    val dias: List<LocalDate>
        get() = generateSequence(rango.desde) { it.plusDays(1) }
            .takeWhile { !it.isAfter(rango.hasta) }
            .toList()

    /** Si el periodo que se mira tiene a hoy: así el chip "Hoy" se ve marcado. */
    val incluyeHoy: Boolean get() = !hoy.isBefore(rango.desde) && !hoy.isAfter(rango.hasta)

    fun citasDe(dia: LocalDate): List<Cita> = citas.filter { it.fecha == dia }

    fun pedidosDe(dia: LocalDate): List<Pedido> = pedidos.filter { it.fechaEntrega == dia }

    /**
     * HU-27. Cuántas citas y entregas tiene un día, para el número de la cuadrícula del mes.
     * Lo cancelado no cuenta: ya no es trabajo.
     */
    fun cuantosEn(dia: LocalDate): Int =
        citas.count { it.fecha == dia && it.estado != EstadoCita.CANCELADA } +
            pedidos.count { it.fechaEntrega == dia && it.estado != EstadoPedido.CANCELADO }

    /** Sin clientes no hay a quién agendarle: se avisa y el botón queda apagado. */
    val sinClientes: Boolean get() = !cargando && clientes.isEmpty()

    /** Ni una cita ni una entrega en todo el periodo. */
    val vacio: Boolean get() = !cargando && citas.isEmpty() && pedidos.isEmpty()
}

/** HU-24. La cita mientras se escribe. Los campos son opcionales porque al principio están vacíos. */
data class FormularioCita(
    /** HU-28. La cita que se está editando; null si es una nueva. */
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
    /** HU-28. El pedido que se está editando; null si es uno nuevo. */
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

/** HU-28. Lo que se va a cancelar: una cita o un pedido. */
sealed interface PorCancelar {
    data class DeCita(val cita: Cita) : PorCancelar
    data class DePedido(val pedido: Pedido) : PorCancelar
}

/** HU-28. Qué le pasó a la cita o al pedido, para escribirle al cliente el mensaje correcto. */
enum class TipoAviso { CitaCambiada, CitaCancelada, PedidoCambiado, PedidoCancelado }

/**
 * HU-28. Lo que hace falta para avisarle al cliente de un cambio por WhatsApp.
 *
 * Guarda los datos y no el mensaje ya escrito: el texto lo arma la pantalla con los recursos de
 * `strings.xml`, que el ViewModel no tiene.
 */
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
