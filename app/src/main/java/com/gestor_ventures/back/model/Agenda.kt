package com.gestor_ventures.back.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * HU-24 y HU-25. La agenda tal como la entiende la app, sin nada de Room.
 *
 * Un negocio de servicios agenda citas; uno de productos registra pedidos con fecha de
 * entrega; uno mixto hace las dos cosas (HU-05).
 */

const val MaxCaracteresDescripcionCita = 200

const val MaxCaracteresProducto = 100

/** HU-24. Una cita con un cliente, a una fecha y hora. */
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

/** HU-25. Un pedido de un cliente, con lo que lleva y cuándo hay que entregarlo. */
data class Pedido(
    val id: Long,
    val clienteId: Long,
    val nombreCliente: String,
    val fechaEntrega: LocalDate,
    val productos: List<ProductoPedido>,
    val estado: EstadoPedido,
)

/** Un producto dentro de un pedido: qué es y cuántos. */
data class ProductoPedido(
    val nombre: String,
    val cantidad: Int,
)

enum class EstadoPedido { PENDIENTE, EN_PROCESO, ENTREGADO, CANCELADO }

/**
 * Reglas que tiene que cumplir una cita o un pedido. El repositorio las valida siempre. Son
 * errores, no mensajes: el texto que ve el usuario lo pone `front/`.
 */
enum class ErrorAgenda {
    ClienteNoExiste,
    /** Citas en un negocio que solo vende productos, o pedidos en uno que solo da servicios. */
    NoAplicaAlNegocio,
    CitaEnElPasado,
    /** Ya hay otra cita, no cancelada, ese día a esa hora. */
    HorarioOcupado,
    DescripcionMuyLarga,
    SinProductos,
    ProductoSinNombre,
    ProductoMuyLargo,
    CantidadNoPositiva,
    EntregaEnElPasado,
    PedidoNoExiste,
    PedidoCancelado,
    CitaNoExiste,
    /** HU-28. Una cita cancelada ya no se edita ni se vuelve a cancelar. */
    CitaCancelada,
}
