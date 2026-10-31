package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gestor_ventures.db.entity.MetodoPagoInactivoEntity
import com.gestor_ventures.db.enums.MetodoPago
import kotlinx.coroutines.flow.Flow

/** Qué métodos de pago apagó cada negocio. Solo se guardan los apagados. */
@Dao
interface MetodoPagoDao {

    @Query("SELECT metodo_pago FROM metodos_pago_inactivos WHERE negocio_id = :negocioId")
    fun observarInactivos(negocioId: Long): Flow<List<MetodoPago>>

    @Query("SELECT metodo_pago FROM metodos_pago_inactivos WHERE negocio_id = :negocioId")
    suspend fun inactivos(negocioId: Long): List<MetodoPago>

    /** Apagar dos veces el mismo método no duplica la fila: el índice es único. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun desactivar(metodo: MetodoPagoInactivoEntity)

    @Query("DELETE FROM metodos_pago_inactivos WHERE negocio_id = :negocioId AND metodo_pago = :metodo")
    suspend fun activar(negocioId: Long, metodo: MetodoPago)
}
