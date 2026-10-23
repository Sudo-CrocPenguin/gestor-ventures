package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.RecordatorioEntity

/**
 * HU-26. Acceso a la tabla `recordatorios`: cuánto antes avisar de una cita o de una entrega.
 *
 * Cada cita o pedido tiene a lo sumo un recordatorio. Al cambiarlo no se edita: se borra el
 * viejo y se crea uno nuevo, que es lo que permite programar de nuevo el aviso desde cero.
 */
@Dao
interface RecordatorioDao {

    @Insert
    suspend fun insertar(recordatorio: RecordatorioEntity): Long

    @Update
    suspend fun actualizar(recordatorio: RecordatorioEntity)

    @Delete
    suspend fun eliminar(recordatorio: RecordatorioEntity)

    /** Lo usa el aviso, cuando le toca sonar, para saber de qué cita o pedido se trata. */
    @Query("SELECT * FROM recordatorios WHERE recordatorio_id = :recordatorioId")
    suspend fun obtener(recordatorioId: Long): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE cita_id = :citaId LIMIT 1")
    suspend fun obtenerDeCita(citaId: Long): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE pedido_id = :pedidoId LIMIT 1")
    suspend fun obtenerDePedido(pedidoId: Long): RecordatorioEntity?

    /** El aviso ya salió: no se vuelve a mandar. */
    @Query("UPDATE recordatorios SET enviado = 1 WHERE recordatorio_id = :recordatorioId")
    suspend fun marcarEnviado(recordatorioId: Long)
}
