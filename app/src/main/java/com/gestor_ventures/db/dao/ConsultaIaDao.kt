package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.ConsultaIaEntity
import kotlinx.coroutines.flow.Flow

/**
 * HU-34 y HU-39. Las preguntas al asistente con su respuesta.
 *
 * Cada fila es una pregunta y su respuesta. Las de una misma conversación comparten `sesion_id`,
 * que es lo que permite mostrarlas juntas y mandarle a Gemini lo que ya se habló.
 */
@Dao
interface ConsultaIaDao {

    @Insert
    suspend fun insertar(consulta: ConsultaIaEntity): Long

    /** La conversación en orden, para el chat. Se actualiza sola al llegar cada respuesta. */
    @Query(
        """
        SELECT * FROM consultas_ia
        WHERE negocio_id = :negocioId AND sesion_id = :sesionId
        ORDER BY fecha_hora, consulta_id
        """,
    )
    fun observarDeSesion(negocioId: Long, sesionId: Long): Flow<List<ConsultaIaEntity>>

    /**
     * La misma conversación, leída una sola vez: lo que se le manda a Gemini para que entienda
     * preguntas como "¿y la semana anterior?".
     */
    @Query(
        """
        SELECT * FROM consultas_ia
        WHERE negocio_id = :negocioId AND sesion_id = :sesionId
        ORDER BY fecha_hora, consulta_id
        """,
    )
    suspend fun deSesion(negocioId: Long, sesionId: Long): List<ConsultaIaEntity>
}
