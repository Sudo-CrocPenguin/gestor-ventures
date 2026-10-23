package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Cita
import com.gestor_ventures.back.model.ErrorAgenda
import com.gestor_ventures.back.model.EstadoCita
import com.gestor_ventures.back.model.EstadoPedido
import com.gestor_ventures.back.model.MaxCaracteresDescripcionCita
import com.gestor_ventures.back.model.MaxCaracteresProducto
import com.gestor_ventures.back.model.Pedido
import com.gestor_ventures.back.model.ProductoPedido
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.CitaConCliente
import com.gestor_ventures.db.dao.CitaDao
import com.gestor_ventures.db.dao.ClienteDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.dao.PedidoConDetalle
import com.gestor_ventures.db.dao.PedidoDao
import com.gestor_ventures.db.entity.CitaEntity
import com.gestor_ventures.db.entity.DetallePedidoEntity
import com.gestor_ventures.db.entity.PedidoEntity
import com.gestor_ventures.db.enums.TipoActividad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import com.gestor_ventures.db.enums.EstadoCita as EstadoCitaDb
import com.gestor_ventures.db.enums.EstadoPedido as EstadoPedidoDb

/**
 * HU-24 y HU-25. Única puerta de entrada a la agenda: citas y pedidos.
 *
 * Acá viven las reglas. La pantalla avisa lo que puede, pero ninguna ruta de la app puede
 * guardar una cita o un pedido que no las cumpla.
 */
@Singleton
class AgendaRepository @Inject constructor(
    private val citaDao: CitaDao,
    private val pedidoDao: PedidoDao,
    private val clienteDao: ClienteDao,
    private val negocioDao: NegocioDao,
    private val reloj: Reloj,
) {

    /** Las citas de un rango de días. También las usará el calendario (HU-27). */
    fun citasEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Cita>> =
        citaDao.observarEntre(negocioId, desde, hasta).map { filas -> filas.map(::aCita) }

    /** Los pedidos que se entregan en un rango de días. */
    fun pedidosEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Pedido>> =
        pedidoDao.observarEntre(negocioId, desde, hasta).map { filas -> filas.map(::aPedido) }

    /**
     * HU-24. Agenda una cita si cumple las reglas, o devuelve qué regla no cumple.
     *
     * La hora se guarda sin segundos: "las 3:00" es la misma cita aunque el reloj marque
     * 3:00:27, y así la revisión de horario ocupado compara lo que el usuario ve.
     */
    suspend fun agendarCita(
        negocioId: Long,
        clienteId: Long,
        fecha: LocalDate,
        hora: LocalTime,
        descripcion: String?,
    ): ErrorAgenda? {
        val descripcionLimpia = descripcion?.trim()?.takeIf { it.isNotEmpty() }
        validarCita(negocioId, clienteId, fecha, hora, descripcionLimpia)?.let { return it }

        citaDao.insertar(
            CitaEntity(
                negocioId = negocioId,
                clienteId = clienteId,
                fecha = fecha,
                hora = hora.withSecond(0).withNano(0),
                descripcion = descripcionLimpia,
            ),
        )
        return null
    }

    /** HU-25. Registra un pedido con sus productos si cumple las reglas. */
    suspend fun registrarPedido(
        negocioId: Long,
        clienteId: Long,
        fechaEntrega: LocalDate,
        productos: List<ProductoPedido>,
    ): ErrorAgenda? {
        val productosLimpios = productos.map { it.copy(nombre = it.nombre.trim()) }
        validarPedido(negocioId, clienteId, fechaEntrega, productosLimpios)?.let { return it }

        pedidoDao.insertarConDetalle(
            pedido = PedidoEntity(
                negocioId = negocioId,
                clienteId = clienteId,
                fechaEntrega = fechaEntrega,
            ),
            detalle = productosLimpios.map {
                DetallePedidoEntity(pedidoId = 0, productoServicio = it.nombre, cantidad = it.cantidad)
            },
        )
        return null
    }

    /**
     * HU-25. Pasa un pedido a pendiente, en proceso o entregado. Un pedido cancelado ya no
     * cambia: si se quiere de nuevo, es un pedido nuevo.
     */
    suspend fun cambiarEstadoPedido(pedidoId: Long, estado: EstadoPedido): ErrorAgenda? {
        val actual = pedidoDao.obtener(pedidoId) ?: return ErrorAgenda.PedidoNoExiste
        if (actual.estadoPedido == EstadoPedidoDb.CANCELADO) return ErrorAgenda.PedidoCancelado

        pedidoDao.actualizarEstado(pedidoId, estado.aDb())
        return null
    }

    /** El día de hoy según el reloj de la app, para saber desde cuándo mostrar la agenda. */
    fun hoy(): LocalDate = reloj.ahora().toLocalDate()

    /**
     * Las reglas de una cita: el cliente es del negocio, el negocio da servicios, la fecha y
     * hora todavía no pasaron, la descripción no pasa de 200 caracteres y nadie más tiene ese
     * horario.
     *
     * "Mismo horario" es el mismo día a la misma hora exacta: la tabla no guarda cuánto dura
     * cada cita, así que no hay forma de saber si dos se pisan a medias.
     */
    private suspend fun validarCita(
        negocioId: Long,
        clienteId: Long,
        fecha: LocalDate,
        hora: LocalTime,
        descripcion: String?,
        exceptoId: Long = 0,
    ): ErrorAgenda? {
        val horaExacta = hora.withSecond(0).withNano(0)
        return when {
            !clienteEsDelNegocio(negocioId, clienteId) -> ErrorAgenda.ClienteNoExiste
            !ofrece(negocioId, TipoActividad.SERVICIOS) -> ErrorAgenda.NoAplicaAlNegocio
            !fecha.atTime(horaExacta).isAfter(reloj.ahora()) -> ErrorAgenda.CitaEnElPasado
            descripcion != null && descripcion.length > MaxCaracteresDescripcionCita ->
                ErrorAgenda.DescripcionMuyLarga
            citaDao.contarEnHorario(negocioId, fecha, horaExacta, exceptoId) > 0 ->
                ErrorAgenda.HorarioOcupado
            else -> null
        }
    }

    /**
     * Las reglas de un pedido: el cliente es del negocio, el negocio vende productos, la entrega
     * es hoy o después, y lleva al menos un producto, cada uno con nombre y cantidad mayor que 0.
     */
    private suspend fun validarPedido(
        negocioId: Long,
        clienteId: Long,
        fechaEntrega: LocalDate,
        productos: List<ProductoPedido>,
    ): ErrorAgenda? = when {
        !clienteEsDelNegocio(negocioId, clienteId) -> ErrorAgenda.ClienteNoExiste
        !ofrece(negocioId, TipoActividad.PRODUCTOS) -> ErrorAgenda.NoAplicaAlNegocio
        fechaEntrega.isBefore(reloj.ahora().toLocalDate()) -> ErrorAgenda.EntregaEnElPasado
        productos.isEmpty() -> ErrorAgenda.SinProductos
        productos.any { it.nombre.isEmpty() } -> ErrorAgenda.ProductoSinNombre
        productos.any { it.nombre.length > MaxCaracteresProducto } -> ErrorAgenda.ProductoMuyLargo
        productos.any { it.cantidad <= 0 } -> ErrorAgenda.CantidadNoPositiva
        else -> null
    }

    /** Un cliente de otro negocio no se puede agendar acá, aunque el id exista. */
    private suspend fun clienteEsDelNegocio(negocioId: Long, clienteId: Long): Boolean =
        clienteDao.obtener(clienteId)?.negocioId == negocioId

    /** Si el negocio hace ese tipo de trabajo. Un negocio mixto hace los dos. */
    private suspend fun ofrece(negocioId: Long, tipo: TipoActividad): Boolean {
        val actividad = negocioDao.obtener(negocioId)?.tipoActividad ?: return false
        return actividad == tipo || actividad == TipoActividad.MIXTO
    }

    private fun aCita(fila: CitaConCliente) = Cita(
        id = fila.cita.citaId,
        clienteId = fila.cita.clienteId,
        nombreCliente = fila.nombreCliente,
        fecha = fila.cita.fecha,
        hora = fila.cita.hora,
        descripcion = fila.cita.descripcion,
        estado = fila.cita.estadoCita.aDominio(),
    )

    private fun aPedido(fila: PedidoConDetalle) = Pedido(
        id = fila.pedido.pedidoId,
        clienteId = fila.pedido.clienteId,
        nombreCliente = fila.nombreCliente,
        fechaEntrega = fila.pedido.fechaEntrega,
        productos = fila.detalle
            .sortedBy { it.detallePedidoId }
            .map { ProductoPedido(nombre = it.productoServicio, cantidad = it.cantidad) },
        estado = fila.pedido.estadoPedido.aDominio(),
    )
}

private fun EstadoCitaDb.aDominio(): EstadoCita = when (this) {
    EstadoCitaDb.PROGRAMADA -> EstadoCita.PROGRAMADA
    EstadoCitaDb.COMPLETADA -> EstadoCita.COMPLETADA
    EstadoCitaDb.CANCELADA -> EstadoCita.CANCELADA
}

private fun EstadoPedido.aDb(): EstadoPedidoDb = when (this) {
    EstadoPedido.PENDIENTE -> EstadoPedidoDb.PENDIENTE
    EstadoPedido.EN_PROCESO -> EstadoPedidoDb.EN_PROCESO
    EstadoPedido.ENTREGADO -> EstadoPedidoDb.ENTREGADO
    EstadoPedido.CANCELADO -> EstadoPedidoDb.CANCELADO
}

private fun EstadoPedidoDb.aDominio(): EstadoPedido = when (this) {
    EstadoPedidoDb.PENDIENTE -> EstadoPedido.PENDIENTE
    EstadoPedidoDb.EN_PROCESO -> EstadoPedido.EN_PROCESO
    EstadoPedidoDb.ENTREGADO -> EstadoPedido.ENTREGADO
    EstadoPedidoDb.CANCELADO -> EstadoPedido.CANCELADO
}
