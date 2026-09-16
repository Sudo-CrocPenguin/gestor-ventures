package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

/** HU-01 a HU-04. Acceso a la tabla `usuarios`. */
@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE usuario_id = :usuarioId")
    fun observar(usuarioId: Long): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios WHERE usuario_id = :usuarioId")
    suspend fun obtener(usuarioId: Long): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE correo = :correo")
    suspend fun obtenerPorCorreo(correo: String): UsuarioEntity?
}
