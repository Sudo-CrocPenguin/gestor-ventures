package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.GastoEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * HU-14. Gastos generales del negocio: lo que se paga una vez y no se repite solo, a
 * diferencia de los gastos fijos de HU-06.
 *
 * Igual que en ventas, las consultas reciben el rango de fechas ya calculado: el resumen del
 * mes, el de la semana o el del día salen de la misma consulta.
 */
@Dao
interface GastoDao {

    @Insert
    suspend fun insertar(gasto: GastoEntity): Long

    @Update
    suspend fun actualizar(gasto: GastoEntity)

    @Delete
    suspend fun eliminar(gasto: GastoEntity)

    @Query("SELECT * FROM gastos WHERE gasto_id = :gastoId")
    suspend fun obtener(gastoId: Long): GastoEntity?

    /**
     * Gastos del rango, del más reciente al más antiguo. Desempata por id porque dos gastos
     * del mismo día saldrían en cualquier orden si solo se mirara la fecha.
     */
    @Query(
        """
        SELECT * FROM gastos
        WHERE negocio_id = :negocioId AND fecha BETWEEN :desde AND :hasta
        ORDER BY fecha DESC, gasto_id DESC
        """,
    )
    fun observarEntre(
        negocioId: Long,
        desde: LocalDate,
        hasta: LocalDate,
    ): Flow<List<GastoEntity>>

    /** HU-16: cuánto se gastó en el periodo. El COALESCE evita que un mes sin gastos dé nulo. */
    @Query(
        """
        SELECT COALESCE(SUM(monto), 0) FROM gastos
        WHERE negocio_id = :negocioId AND fecha BETWEEN :desde AND :hasta
        """,
    )
    fun observarTotalEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<Double>

    /**
     * HU-15: cuánto se ha gastado en cada categoría dentro del rango.
     *
     * Los gastos sin categoría se agrupan todos juntos, con el id en nulo: es información
     * legítima ("hay $200.000 sin clasificar"), no un error que haya que esconder.
     */
    @Query(
        """
        SELECT categoria_id AS categoriaId, SUM(monto) AS total FROM gastos
        WHERE negocio_id = :negocioId AND fecha BETWEEN :desde AND :hasta
        GROUP BY categoria_id
        ORDER BY total DESC
        """,
    )
    fun observarTotalPorCategoriaEntre(
        negocioId: Long,
        desde: LocalDate,
        hasta: LocalDate,
    ): Flow<List<TotalPorCategoria>>
}

/** Lo acumulado en una categoría. [categoriaId] nulo es el montón de lo que nadie clasificó. */
data class TotalPorCategoria(
    val categoriaId: Long?,
    val total: Double,
)
