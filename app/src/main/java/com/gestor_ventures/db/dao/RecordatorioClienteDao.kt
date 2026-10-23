package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.RecordatorioClienteEntity
import kotlinx.coroutines.flow.Flow

/**
 * HU-32. Los recordatorios que ya se le mandaron a un cliente.
 *
 * Se guardan después de enviarlos, no antes: lo que queda registrado es lo que de verdad se
 * dijo, incluidas las correcciones que el usuario le haya hecho al mensaje predefinido.
 *
 * No hay editar ni borrar a propósito. Un recordatorio es algo que pasó: el mensaje ya salió
 * de la app y cambiarlo acá no lo cambia en el teléfono de nadie.
 */
@Dao
interface RecordatorioClienteDao {

    @Insert
    suspend fun insertar(recordatorio: RecordatorioClienteEntity): Long

    /** Los de un cliente, del más reciente al más antiguo. */
    @Query(
        """
        SELECT * FROM recordatorios_clientes
        WHERE cliente_id = :clienteId
        ORDER BY fecha_envio DESC, recordatorio_cliente_id DESC
        """,
    )
    fun observarDeCliente(clienteId: Long): Flow<List<RecordatorioClienteEntity>>
}
