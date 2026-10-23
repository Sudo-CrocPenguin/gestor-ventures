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

@Singleton
class AgendaRepository @Inject constructor(
    private val citaDao: CitaDao,
    private val pedidoDao: PedidoDao,
    private val clienteDao: ClienteDao,
    private val negocioDao: NegocioDao,
    private val reloj: Reloj,
) {

    fun citasEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Cita>> =
        citaDao.observarEntre(negocioId, desde, hasta).map { filas -> filas.map(::aCita) }

    fun pedidosEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Pedido>> =
        pedidoDao.observarEntre(negocioId, desde, hasta).map { filas -> filas.map(::aPedido) }

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

    suspend fun cambiarEstadoPedido(pedidoId: Long, estado: EstadoPedido): ErrorAgenda? {
        val actual = pedidoDao.obtener(pedidoId) ?: return ErrorAgenda.PedidoNoExiste
        if (actual.estadoPedido == EstadoPedidoDb.CANCELADO) return ErrorAgenda.PedidoCancelado

        pedidoDao.actualizarEstado(pedidoId, estado.aDb())
        return null
    }

    fun hoy(): LocalDate = reloj.ahora().toLocalDate()

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

    private suspend fun clienteEsDelNegocio(negocioId: Long, clienteId: Long): Boolean =
        clienteDao.obtener(clienteId)?.negocioId == negocioId

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
