package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.ConsultaIaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultaIaDao {

    @Insert
    suspend fun insertar(consulta: ConsultaIaEntity): Long

    @Query(
        """
        SELECT * FROM consultas_ia
        WHERE negocio_id = :negocioId AND sesion_id = :sesionId
        ORDER BY fecha_hora, consulta_id
        """,
    )
    fun observarDeSesion(negocioId: Long, sesionId: Long): Flow<List<ConsultaIaEntity>>

    @Query(
        """
        SELECT * FROM consultas_ia
        WHERE negocio_id = :negocioId AND sesion_id = :sesionId
        ORDER BY fecha_hora, consulta_id
        """,
    )
    suspend fun deSesion(negocioId: Long, sesionId: Long): List<ConsultaIaEntity>
}
