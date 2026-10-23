package com.gestor_ventures.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.ConsultaIaEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

data class ResumenSesionIa(
    @ColumnInfo(name = "sesion_id") val sesionId: Long,
    @ColumnInfo(name = "primera_pregunta") val primeraPregunta: String,
    @ColumnInfo(name = "ultima_fecha") val ultimaFecha: LocalDateTime,
    val cantidad: Int,
)

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

    @Query(
        """
        SELECT c.sesion_id,
            (SELECT p.pregunta FROM consultas_ia p
             WHERE p.negocio_id = c.negocio_id AND p.sesion_id = c.sesion_id
             ORDER BY p.fecha_hora, p.consulta_id LIMIT 1) AS primera_pregunta,
            MAX(c.fecha_hora) AS ultima_fecha,
            COUNT(*) AS cantidad
        FROM consultas_ia c
        WHERE c.negocio_id = :negocioId
        GROUP BY c.sesion_id
        ORDER BY ultima_fecha DESC
        """,
    )
    fun observarSesiones(negocioId: Long): Flow<List<ResumenSesionIa>>

    @Query("DELETE FROM consultas_ia WHERE negocio_id = :negocioId AND sesion_id = :sesionId")
    suspend fun eliminarSesion(negocioId: Long, sesionId: Long)
}
