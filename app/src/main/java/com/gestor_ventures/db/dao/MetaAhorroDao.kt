package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.MetaAhorroEntity
import kotlinx.coroutines.flow.Flow

/**
 * HU-08. Meta de ahorro del negocio; solo una activa a la vez.
 *
 * El orden desempata por id: dos metas creadas en el mismo instante saldrían en cualquier
 * orden si solo se mirara la fecha.
 */
@Dao
interface MetaAhorroDao {

    @Insert
    suspend fun insertar(meta: MetaAhorroEntity): Long

    @Update
    suspend fun actualizar(meta: MetaAhorroEntity)

    @Query(
        """
        SELECT * FROM metas_ahorro
        WHERE negocio_id = :negocioId AND estado_meta = 'ACTIVA'
        ORDER BY fecha_creacion DESC, meta_ahorro_id DESC
        LIMIT 1
        """,
    )
    fun observarActivaDeNegocio(negocioId: Long): Flow<MetaAhorroEntity?>

    @Query(
        """
        SELECT * FROM metas_ahorro
        WHERE negocio_id = :negocioId AND estado_meta = 'ACTIVA'
        ORDER BY fecha_creacion DESC, meta_ahorro_id DESC
        LIMIT 1
        """,
    )
    suspend fun obtenerActivaDeNegocio(negocioId: Long): MetaAhorroEntity?

    @Query(
        "SELECT * FROM metas_ahorro WHERE negocio_id = :negocioId " +
            "ORDER BY fecha_creacion DESC, meta_ahorro_id DESC",
    )
    fun observarDeNegocio(negocioId: Long): Flow<List<MetaAhorroEntity>>
}
