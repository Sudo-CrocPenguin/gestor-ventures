package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.TipoNotificacion
import java.time.LocalDateTime

/**
 * HU-41. [referenciaId] apunta al alerta_id o recordatorio_id que originó la
 * notificación, según [tipoNotificacion]. No lleva FK física porque referencia una
 * tabla distinta según el caso; se resuelve en la capa de repositorio.
 */
@Entity(
    tableName = "notificaciones",
    foreignKeys = [
        ForeignKey(entity = UsuarioEntity::class, parentColumns = ["usuario_id"], childColumns = ["usuario_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("usuario_id")]
)
data class NotificacionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "notificacion_id")
    val notificacionId: Long = 0,

    @ColumnInfo(name = "usuario_id")
    val usuarioId: Long,

    @ColumnInfo(name = "tipo_notificacion")
    val tipoNotificacion: TipoNotificacion,

    @ColumnInfo(name = "referencia_id")
    val referenciaId: Long? = null,

    val contenido: String,

    val leida: Boolean = false,

    @ColumnInfo(name = "fecha_hora_envio")
    val fechaHoraEnvio: LocalDateTime
)
