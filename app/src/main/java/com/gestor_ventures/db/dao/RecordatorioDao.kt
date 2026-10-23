package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.RecordatorioEntity

@Dao
interface RecordatorioDao {

    @Insert
    suspend fun insertar(recordatorio: RecordatorioEntity): Long

    @Update
    suspend fun actualizar(recordatorio: RecordatorioEntity)

    @Delete
    suspend fun eliminar(recordatorio: RecordatorioEntity)

    @Query("SELECT * FROM recordatorios WHERE recordatorio_id = :recordatorioId")
    suspend fun obtener(recordatorioId: Long): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE cita_id = :citaId LIMIT 1")
    suspend fun obtenerDeCita(citaId: Long): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE pedido_id = :pedidoId LIMIT 1")
    suspend fun obtenerDePedido(pedidoId: Long): RecordatorioEntity?

    @Query("UPDATE recordatorios SET enviado = 1 WHERE recordatorio_id = :recordatorioId")
    suspend fun marcarEnviado(recordatorioId: Long)
}
