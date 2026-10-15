package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.NegocioEntity
import kotlinx.coroutines.flow.Flow

/**
 * HU-05 y HU-10. Acceso a la tabla `negocios`: crear un negocio, editarlo y listar los del
 * usuario para el selector del menú lateral.
 *
 * Las consultas que la UI observa devuelven [Flow]: cuando se inserta o edita un negocio, la
 * pantalla se actualiza sola.
 */
@Dao
interface NegocioDao {

    @Insert
    suspend fun insertar(negocio: NegocioEntity): Long

    @Update
    suspend fun actualizar(negocio: NegocioEntity)

    @Query("SELECT * FROM negocios WHERE negocio_id = :negocioId")
    fun observar(negocioId: Long): Flow<NegocioEntity?>

    @Query("SELECT * FROM negocios WHERE negocio_id = :negocioId")
    suspend fun obtener(negocioId: Long): NegocioEntity?

    @Query("SELECT * FROM negocios WHERE usuario_id = :usuarioId ORDER BY nombre_negocio COLLATE NOCASE")
    fun observarDeUsuario(usuarioId: Long): Flow<List<NegocioEntity>>

    /** Para saber si el usuario ya tiene negocios o hay que mandarlo a crear el primero. */
    @Query("SELECT COUNT(*) FROM negocios WHERE usuario_id = :usuarioId")
    suspend fun contarDeUsuario(usuarioId: Long): Int
}
