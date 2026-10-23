package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.AlertaEntity
import com.gestor_ventures.db.enums.TipoAlerta
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * HU-38. Las alertas que la app genera al revisar el negocio.
 *
 * No hay borrar: una alerta es algo que pasó, y marcarla como leída es lo que dice que ya se
 * atendió.
 */
@Dao
interface AlertaDao {

    @Insert
    suspend fun insertar(alerta: AlertaEntity): Long

    /** Las alertas del negocio, de la más nueva a la más vieja. Se actualiza sola. */
    @Query(
        """
        SELECT * FROM alertas WHERE negocio_id = :negocioId
        ORDER BY fecha_generacion DESC, alerta_id DESC
        """,
    )
    fun observarDeNegocio(negocioId: Long): Flow<List<AlertaEntity>>

    /** Si ya se avisó ese tipo de alerta desde [desde]: es lo que evita repetirla todos los días. */
    @Query(
        """
        SELECT COUNT(*) FROM alertas
        WHERE negocio_id = :negocioId AND tipo_alerta = :tipo AND fecha_generacion >= :desde
        """,
    )
    suspend fun contarDesde(negocioId: Long, tipo: TipoAlerta, desde: LocalDateTime): Int

    @Query("UPDATE alertas SET leida = 1 WHERE alerta_id = :alertaId")
    suspend fun marcarLeida(alertaId: Long)

    @Query("UPDATE alertas SET leida = 1 WHERE negocio_id = :negocioId")
    suspend fun marcarTodasLeidas(negocioId: Long)

    /**
     * Todos los negocios guardados en el teléfono, para la revisión diaria. Vive acá y no en
     * `NegocioDao` porque solo la usan las alertas.
     */
    @Query("SELECT negocio_id FROM negocios")
    suspend fun idsDeNegocios(): List<Long>
}
