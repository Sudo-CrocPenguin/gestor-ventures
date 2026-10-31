package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.MiembroEquipoEntity
import kotlinx.coroutines.flow.Flow

/** El equipo de vendedores de cada negocio. */
@Dao
interface EquipoDao {

    @Insert
    suspend fun insertar(miembro: MiembroEquipoEntity): Long

    /** Las invitaciones más recientes primero. */
    @Query("SELECT * FROM miembros_equipo WHERE negocio_id = :negocioId ORDER BY fecha_invitacion DESC")
    fun observarDeNegocio(negocioId: Long): Flow<List<MiembroEquipoEntity>>

    @Query("SELECT COUNT(*) FROM miembros_equipo WHERE negocio_id = :negocioId AND correo = :correo")
    suspend fun contarPorCorreo(negocioId: Long, correo: String): Int

    /** Cuántas invitaciones tiene ese correo, en cualquier negocio. */
    @Query("SELECT COUNT(*) FROM miembros_equipo WHERE correo = :correo")
    suspend fun contarInvitaciones(correo: String): Int

    /** El vendedor entró a la app con el correo con el que lo invitaron: la invitación se acepta. */
    @Query("UPDATE miembros_equipo SET estado_miembro = 'ACTIVO' WHERE correo = :correo AND estado_miembro = 'PENDIENTE'")
    suspend fun aceptarInvitaciones(correo: String)

    @Query("DELETE FROM miembros_equipo WHERE miembro_equipo_id = :miembroId")
    suspend fun eliminar(miembroId: Long)
}
