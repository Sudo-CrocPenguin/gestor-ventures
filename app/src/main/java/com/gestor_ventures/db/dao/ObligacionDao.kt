package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.ObligacionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * HU-07. Compromisos que el negocio ya adquirió: préstamos, cuotas, pagos recurrentes.
 *
 * A diferencia de gastos y costos, acá el orden natural no es del más reciente al más antiguo
 * sino del que vence primero: lo que importa de una deuda es cuándo hay que pagarla.
 */
@Dao
interface ObligacionDao {

    @Insert
    suspend fun insertar(obligacion: ObligacionEntity): Long

    @Update
    suspend fun actualizar(obligacion: ObligacionEntity)

    @Delete
    suspend fun eliminar(obligacion: ObligacionEntity)

    @Query("SELECT * FROM obligaciones WHERE obligacion_id = :obligacionId")
    suspend fun obtener(obligacionId: Long): ObligacionEntity?

    /**
     * Todas las del negocio: primero las pendientes, y dentro de cada grupo la que vence antes.
     * Las pagadas quedan abajo porque ya no exigen nada.
     */
    @Query(
        """
        SELECT * FROM obligaciones
        WHERE negocio_id = :negocioId
        ORDER BY estado_pago = 'PAGADA' ASC, fecha_vencimiento ASC, obligacion_id ASC
        """,
    )
    fun observarDeNegocio(negocioId: Long): Flow<List<ObligacionEntity>>

    /**
     * HU-07. Las que aún no se han pagado y vencen hasta [hasta], incluidas las que ya se
     * pasaron de fecha: una obligación vencida es la más urgente de todas, no una que se deba
     * esconder.
     */
    @Query(
        """
        SELECT * FROM obligaciones
        WHERE negocio_id = :negocioId
          AND estado_pago = 'PENDIENTE'
          AND fecha_vencimiento <= :hasta
        ORDER BY fecha_vencimiento ASC, obligacion_id ASC
        """,
    )
    fun observarProximasAVencer(negocioId: Long, hasta: LocalDate): Flow<List<ObligacionEntity>>

    /** HU-16: lo que el negocio debe y todavía no ha pagado, para el dinero disponible. */
    @Query(
        """
        SELECT COALESCE(SUM(monto), 0) FROM obligaciones
        WHERE negocio_id = :negocioId AND estado_pago = 'PENDIENTE'
        """,
    )
    fun observarTotalPendiente(negocioId: Long): Flow<Double>
}
