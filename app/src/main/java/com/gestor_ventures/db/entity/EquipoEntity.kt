package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.EstadoMiembro
import java.time.LocalDateTime

/**
 * Un vendedor invitado al equipo de un negocio. El correo no se repite dentro del mismo negocio.
 */
@Entity(
    tableName = "miembros_equipo",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("negocio_id", "correo", unique = true)]
)
data class MiembroEquipoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "miembro_equipo_id")
    val miembroEquipoId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "correo")
    val correo: String,

    @ColumnInfo(name = "estado_miembro")
    val estadoMiembro: EstadoMiembro,

    @ColumnInfo(name = "fecha_invitacion")
    val fechaInvitacion: LocalDateTime
)
