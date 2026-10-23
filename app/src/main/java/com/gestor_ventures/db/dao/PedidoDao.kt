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

data class PedidoConDetalle(
    @Embedded val pedido: PedidoEntity,
    @ColumnInfo(name = "nombre_cliente") val nombreCliente: String,
    @Relation(parentColumn = "pedido_id", entityColumn = "pedido_id")
    val detalle: List<DetallePedidoEntity>,
)

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

    @Transaction
    suspend fun insertarConDetalle(pedido: PedidoEntity, detalle: List<DetallePedidoEntity>): Long {
        val pedidoId = insertarPedido(pedido)
        insertarDetalle(detalle.map { it.copy(pedidoId = pedidoId) })
        return pedidoId
    }

    @Transaction
    suspend fun actualizarConDetalle(pedido: PedidoEntity, detalle: List<DetallePedidoEntity>) {
        actualizarPedido(pedido)
        borrarDetalle(pedido.pedidoId)
        insertarDetalle(detalle.map { it.copy(detallePedidoId = 0, pedidoId = pedido.pedidoId) })
    }

    @Query("UPDATE pedidos SET estado_pedido = :estado WHERE pedido_id = :pedidoId")
    suspend fun actualizarEstado(pedidoId: Long, estado: EstadoPedido)

    @Query("SELECT * FROM pedidos WHERE pedido_id = :pedidoId")
    suspend fun obtener(pedidoId: Long): PedidoEntity?

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
