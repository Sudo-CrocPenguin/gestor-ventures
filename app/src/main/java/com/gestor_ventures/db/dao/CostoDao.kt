package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.CostoEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * HU-13. Lo que cuesta producir lo que se vende: los insumos de una torta, la tela de un
 * vestido. Distinto de los gastos generales de HU-14, que son de funcionar.
 *
 * El margen no se calcula acá. Cruzar ventas con costos exige comparar el nombre del producto
 * sin distinguir mayúsculas ni tildes, y eso SQLite no lo sabe hacer con acentos: el cruce vive
 * en la capa `back`, donde sí se puede normalizar el texto.
 */
@Dao
interface CostoDao {

    @Insert
    suspend fun insertar(costo: CostoEntity): Long

    @Update
    suspend fun actualizar(costo: CostoEntity)

    @Delete
    suspend fun eliminar(costo: CostoEntity)

    @Query("SELECT * FROM costos WHERE costo_id = :costoId")
    suspend fun obtener(costoId: Long): CostoEntity?

    /** Costos del rango, del más reciente al más antiguo, con su desempate por id. */
    @Query(
        """
        SELECT * FROM costos
        WHERE negocio_id = :negocioId AND fecha_registro BETWEEN :desde AND :hasta
        ORDER BY fecha_registro DESC, costo_id DESC
        """,
    )
    fun observarEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<List<CostoEntity>>

    /** HU-16: cuánto costó producir en el periodo. */
    @Query(
        """
        SELECT COALESCE(SUM(monto_costo), 0) FROM costos
        WHERE negocio_id = :negocioId AND fecha_registro BETWEEN :desde AND :hasta
        """,
    )
    fun observarTotalEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<Double>

    /** HU-15: cuánto se ha costeado en cada categoría dentro del rango. */
    @Query(
        """
        SELECT categoria_id AS categoriaId, SUM(monto_costo) AS total FROM costos
        WHERE negocio_id = :negocioId AND fecha_registro BETWEEN :desde AND :hasta
        GROUP BY categoria_id
        ORDER BY total DESC
        """,
    )
    fun observarTotalPorCategoriaEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<List<TotalPorCategoria>>
}
