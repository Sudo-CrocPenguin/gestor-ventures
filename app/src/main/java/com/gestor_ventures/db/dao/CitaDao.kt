package com.gestor_ventures.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.CitaEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

data class CitaConCliente(
    @Embedded val cita: CitaEntity,
    @ColumnInfo(name = "nombre_cliente") val nombreCliente: String,
)

@Dao
interface CitaDao {

    @Insert
    suspend fun insertar(cita: CitaEntity): Long

    @Update
    suspend fun actualizar(cita: CitaEntity)

    @Query("SELECT * FROM citas WHERE cita_id = :citaId")
    suspend fun obtener(citaId: Long): CitaEntity?

    @Query(
        """
        SELECT citas.*, clientes.nombre AS nombre_cliente
        FROM citas
        INNER JOIN clientes ON clientes.cliente_id = citas.cliente_id
        WHERE citas.negocio_id = :negocioId AND citas.fecha BETWEEN :desde AND :hasta
        ORDER BY citas.fecha, citas.hora
        """,
    )
    fun observarEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<CitaConCliente>>

    @Query(
        """
        SELECT COUNT(*) FROM citas
        WHERE negocio_id = :negocioId AND fecha = :fecha AND hora = :hora
          AND estado_cita != 'CANCELADA' AND cita_id != :exceptoId
        """,
    )
    suspend fun contarEnHorario(
        negocioId: Long,
        fecha: LocalDate,
        hora: LocalTime,
        exceptoId: Long = 0,
    ): Int
}
