package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.EstadoCita
import com.gestor_ventures.db.enums.EstadoPedido
import java.time.LocalDate
import java.time.LocalTime

/** HU-24 (issue combinada #23). Agenda para negocios de tipo SERVICIOS o MIXTO. */
@Entity(
    tableName = "citas",
    foreignKeys = [
        ForeignKey(entity = NegocioEntity::class, parentColumns = ["negocio_id"], childColumns = ["negocio_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ClienteEntity::class, parentColumns = ["cliente_id"], childColumns = ["cliente_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("negocio_id"), Index("cliente_id")]
)
data class CitaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cita_id")
    val citaId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "cliente_id")
    val clienteId: Long,

    val fecha: LocalDate, // posterior al momento actual

    val hora: LocalTime,

    val descripcion: String? = null,

    @ColumnInfo(name = "estado_cita")
    val estadoCita: EstadoCita = EstadoCita.PROGRAMADA
)

/** HU-25 (issue combinada #23). Agenda para negocios de tipo PRODUCTOS o MIXTO. */
@Entity(
    tableName = "pedidos",
    foreignKeys = [
        ForeignKey(entity = NegocioEntity::class, parentColumns = ["negocio_id"], childColumns = ["negocio_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ClienteEntity::class, parentColumns = ["cliente_id"], childColumns = ["cliente_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("negocio_id"), Index("cliente_id")]
)
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "pedido_id")
    val pedidoId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "cliente_id")
    val clienteId: Long,

    @ColumnInfo(name = "fecha_entrega")
    val fechaEntrega: LocalDate,

    @ColumnInfo(name = "estado_pedido")
    val estadoPedido: EstadoPedido = EstadoPedido.PENDIENTE
)

/**
 * Ítems del pedido. Se separa de [PedidoEntity] porque "productos" es un atributo
 * multivaluado (un pedido puede tener varios productos): en 1FN cada columna debe
 * contener un único valor atómico, así que la lista pasa a filas independientes.
 */
@Entity(
    tableName = "detalle_pedidos",
    foreignKeys = [
        ForeignKey(entity = PedidoEntity::class, parentColumns = ["pedido_id"], childColumns = ["pedido_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("pedido_id")]
)
data class DetallePedidoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "detalle_pedido_id")
    val detallePedidoId: Long = 0,

    @ColumnInfo(name = "pedido_id")
    val pedidoId: Long,

    @ColumnInfo(name = "producto_servicio")
    val productoServicio: String,

    val cantidad: Int // > 0
)

/**
 * HU-26. Recordatorio de una cita o de un pedido.
 * Regla de negocio: exactamente uno de [citaId] / [pedidoId] debe tener valor;
 * se valida en el repositorio, ya que Room no expresa una restricción XOR entre columnas.
 */
@Entity(
    tableName = "recordatorios",
    foreignKeys = [
        ForeignKey(entity = CitaEntity::class, parentColumns = ["cita_id"], childColumns = ["cita_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PedidoEntity::class, parentColumns = ["pedido_id"], childColumns = ["pedido_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("cita_id"), Index("pedido_id")]
)
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "recordatorio_id")
    val recordatorioId: Long = 0,

    @ColumnInfo(name = "cita_id")
    val citaId: Long? = null,

    @ColumnInfo(name = "pedido_id")
    val pedidoId: Long? = null,

    @ColumnInfo(name = "tiempo_anticipacion")
    val tiempoAnticipacion: Int, // minutos

    val enviado: Boolean = false
)
