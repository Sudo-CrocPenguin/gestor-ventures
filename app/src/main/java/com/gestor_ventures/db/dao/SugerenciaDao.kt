package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.SugerenciaEntity
import com.gestor_ventures.db.enums.EstadoSugerencia
import kotlinx.coroutines.flow.Flow

@Dao
interface SugerenciaDao {

    @Insert
    suspend fun insertar(sugerencia: SugerenciaEntity): Long

    @Query(
        """
        SELECT * FROM sugerencias
        WHERE negocio_id = :negocioId AND estado_sugerencia != 'DESCARTADA'
        ORDER BY fecha_generacion DESC, sugerencia_id DESC
        """,
    )
    fun observarVisibles(negocioId: Long): Flow<List<SugerenciaEntity>>

    @Query("UPDATE sugerencias SET estado_sugerencia = :estado WHERE sugerencia_id = :sugerenciaId")
    suspend fun actualizarEstado(sugerenciaId: Long, estado: EstadoSugerencia)
}
