package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.AlertaEntity
import com.gestor_ventures.db.enums.TipoAlerta
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface AlertaDao {

    @Insert
    suspend fun insertar(alerta: AlertaEntity): Long

    @Query(
        """
        SELECT * FROM alertas WHERE negocio_id = :negocioId
        ORDER BY fecha_generacion DESC, alerta_id DESC
        """,
    )
    fun observarDeNegocio(negocioId: Long): Flow<List<AlertaEntity>>

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

    @Query("SELECT negocio_id FROM negocios")
    suspend fun idsDeNegocios(): List<Long>
}
