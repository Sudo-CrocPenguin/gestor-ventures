package com.gestor_ventures.back.model

import java.time.LocalDate
import java.time.LocalTime

const val MaxCaracteresDescripcionCita = 200

const val MaxCaracteresProducto = 100

data class Cita(
    val id: Long,
    val clienteId: Long,
    val nombreCliente: String,
    val fecha: LocalDate,
    val hora: LocalTime,
    val descripcion: String?,
    val estado: EstadoCita,
)

enum class EstadoCita { PROGRAMADA, COMPLETADA, CANCELADA }

data class Pedido(
    val id: Long,
    val clienteId: Long,
    val nombreCliente: String,
    val fechaEntrega: LocalDate,
    val productos: List<ProductoPedido>,
    val estado: EstadoPedido,
)

data class ProductoPedido(
    val nombre: String,
    val cantidad: Int,
)

enum class EstadoPedido { PENDIENTE, EN_PROCESO, ENTREGADO, CANCELADO }

enum class ErrorAgenda {
    ClienteNoExiste,
    NoAplicaAlNegocio,
    CitaEnElPasado,
    HorarioOcupado,
    DescripcionMuyLarga,
    SinProductos,
    ProductoSinNombre,
    ProductoMuyLargo,
    CantidadNoPositiva,
    EntregaEnElPasado,
    PedidoNoExiste,
    PedidoCancelado,
}
