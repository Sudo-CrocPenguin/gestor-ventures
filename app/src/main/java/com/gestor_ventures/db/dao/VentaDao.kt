package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.VentaEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * HU-11 y HU-12. Ventas del negocio, detalladas y rápidas: las dos son la misma fila y se
 * distinguen por `tipo_registro`.
 *
 * Las consultas reciben el rango de fechas ya calculado en vez de averiguar solas qué día es
 * hoy: así el resumen del día, el de ayer o el de la semana salen de la misma consulta, y la
 * zona horaria se decide en un solo lugar de la capa `back`.
 */
@Dao
interface VentaDao {

    @Insert
    suspend fun insertar(venta: VentaEntity): Long

    /**
     * HU-17. Corrige una venta ya registrada. Hasta ahora una venta era definitiva: un monto mal
     * escrito se quedaba mal, y de ese monto cuelgan el resumen del día, la ganancia del mes y
     * el progreso de la meta.
     */
    @Update
    suspend fun actualizar(venta: VentaEntity)

    @Delete
    suspend fun eliminar(venta: VentaEntity)

    @Query("SELECT * FROM ventas WHERE venta_id = :ventaId")
    suspend fun obtener(ventaId: Long): VentaEntity?

    /**
     * Ventas del rango, de la más reciente a la más antigua. Desempata por id porque dos ventas
     * registradas en el mismo minuto saldrían en cualquier orden si solo se mirara la fecha.
     */
    @Query(
        """
        SELECT * FROM ventas
        WHERE negocio_id = :negocioId AND fecha_hora BETWEEN :desde AND :hasta
        ORDER BY fecha_hora DESC, venta_id DESC
        """,
    )
    fun observarEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<List<VentaEntity>>

    /** Lo vendido en el rango. El COALESCE evita que un día sin ventas devuelva nulo. */
    @Query(
        """
        SELECT COALESCE(SUM(monto), 0) FROM ventas
        WHERE negocio_id = :negocioId AND fecha_hora BETWEEN :desde AND :hasta
        """,
    )
    fun observarTotalEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<Double>

    /** Cuántas ventas se registraron en el rango, para el resumen del inicio. */
    @Query(
        """
        SELECT COUNT(*) FROM ventas
        WHERE negocio_id = :negocioId AND fecha_hora BETWEEN :desde AND :hasta
        """,
    )
    fun contarEntre(negocioId: Long, desde: LocalDateTime, hasta: LocalDateTime): Flow<Int>
}
