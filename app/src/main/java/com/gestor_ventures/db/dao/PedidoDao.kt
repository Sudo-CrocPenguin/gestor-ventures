package com.gestor_ventures.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.gestor_ventures.db.entity.DetallePedidoEntity
import com.gestor_ventures.db.entity.PedidoEntity
import com.gestor_ventures.db.enums.EstadoPedido
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** HU-25. Un pedido con el nombre de su cliente y los productos que lleva. */
data class PedidoConDetalle(
    @Embedded val pedido: PedidoEntity,
    @ColumnInfo(name = "nombre_cliente") val nombreCliente: String,
    @Relation(parentColumn = "pedido_id", entityColumn = "pedido_id")
    val detalle: List<DetallePedidoEntity>,
)

/**
 * HU-25. Acceso a las tablas `pedidos` y `detalle_pedidos`.
 *
 * El pedido y sus productos se guardan siempre juntos, en una transacción: o queda todo o no
 * queda nada. Así nunca hay un pedido sin productos ni productos sueltos sin pedido.
 */
@Dao
interface PedidoDao {

    @Insert
    suspend fun insertarPedido(pedido: PedidoEntity): Long

    @Insert
    suspend fun insertarDetalle(detalle: List<DetallePedidoEntity>)

    @Update
    suspend fun actualizarPedido(pedido: PedidoEntity)

    @Query("DELETE FROM detalle_pedidos WHERE pedido_id = :pedidoId")
    suspend fun borrarDetalle(pedidoId: Long)

    /** Crea el pedido y le pone su id a cada producto. Devuelve el id del pedido. */
    @Transaction
    suspend fun insertarConDetalle(pedido: PedidoEntity, detalle: List<DetallePedidoEntity>): Long {
        val pedidoId = insertarPedido(pedido)
        insertarDetalle(detalle.map { it.copy(pedidoId = pedidoId) })
        return pedidoId
    }

    /**
     * Cambia el pedido y reemplaza todos sus productos por los nuevos. Es más simple que
     * comparar cuáles cambiaron, y con pocos productos por pedido no cuesta nada.
     */
    @Transaction
    suspend fun actualizarConDetalle(pedido: PedidoEntity, detalle: List<DetallePedidoEntity>) {
        actualizarPedido(pedido)
        borrarDetalle(pedido.pedidoId)
        insertarDetalle(detalle.map { it.copy(detallePedidoId = 0, pedidoId = pedido.pedidoId) })
    }

    /** HU-25. Pendiente, en proceso o entregado. */
    @Query("UPDATE pedidos SET estado_pedido = :estado WHERE pedido_id = :pedidoId")
    suspend fun actualizarEstado(pedidoId: Long, estado: EstadoPedido)

    @Query("SELECT * FROM pedidos WHERE pedido_id = :pedidoId")
    suspend fun obtener(pedidoId: Long): PedidoEntity?

    /** Los pedidos que se entregan en un rango de días, en orden de entrega. Se actualiza solo. */
    @Transaction
    @Query(
        """
        SELECT pedidos.*, clientes.nombre AS nombre_cliente
        FROM pedidos
        INNER JOIN clientes ON clientes.cliente_id = pedidos.cliente_id
        WHERE pedidos.negocio_id = :negocioId AND pedidos.fecha_entrega BETWEEN :desde AND :hasta
        ORDER BY pedidos.fecha_entrega, pedidos.pedido_id
        """,
    )
    fun observarEntre(
        negocioId: Long,
        desde: LocalDate,
        hasta: LocalDate,
    ): Flow<List<PedidoConDetalle>>
}
