package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.GastoFijoEntity
import kotlinx.coroutines.flow.Flow

/** HU-06. Gastos que se repiten cada periodo: arriendo, servicios, internet. */
@Dao
interface GastoFijoDao {

    @Insert
    suspend fun insertar(gastoFijo: GastoFijoEntity): Long

    @Update
    suspend fun actualizar(gastoFijo: GastoFijoEntity)

    @Delete
    suspend fun eliminar(gastoFijo: GastoFijoEntity)

    @Query("SELECT * FROM gastos_fijos WHERE negocio_id = :negocioId ORDER BY fecha_registro DESC")
    fun observarDeNegocio(negocioId: Long): Flow<List<GastoFijoEntity>>

    @Query("SELECT * FROM gastos_fijos WHERE gasto_fijo_id = :gastoFijoId")
    suspend fun obtener(gastoFijoId: Long): GastoFijoEntity?

    /** HU-16: los gastos fijos entran en el cálculo del dinero disponible. */
    @Query("SELECT COALESCE(SUM(monto), 0) FROM gastos_fijos WHERE negocio_id = :negocioId")
    fun observarTotalDeNegocio(negocioId: Long): Flow<Double>
}
