package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.TipoRecordatorioCliente
import java.time.LocalDateTime

/**
 * HU-29 a HU-33.
 *
 * Los índices únicos sobre (negocio_id, telefono) y (negocio_id, correo) evitan clientes
 * duplicados dentro del mismo negocio. SQLite trata cada NULL como distinto en un índice
 * único, así que un cliente sin teléfono o sin correo no rompe la restricción.
 */
@Entity(
    tableName = "clientes",
    foreignKeys = [
        ForeignKey(entity = NegocioEntity::class, parentColumns = ["negocio_id"], childColumns = ["negocio_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("negocio_id"),
        Index(value = ["negocio_id", "telefono"], unique = true),
        Index(value = ["negocio_id", "correo"], unique = true)
    ]
)
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cliente_id")
    val clienteId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    val nombre: String,

    val telefono: String? = null, // único dentro del negocio, solo dígitos

    val correo: String? = null, // único dentro del negocio, formato válido

    val notas: String? = null
)

/** HU-32. Recordatorio manual enviado a un cliente (cita o pago pendiente). */
@Entity(
    tableName = "recordatorios_clientes",
    foreignKeys = [
        ForeignKey(entity = ClienteEntity::class, parentColumns = ["cliente_id"], childColumns = ["cliente_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("cliente_id")]
)
data class RecordatorioClienteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "recordatorio_cliente_id")
    val recordatorioClienteId: Long = 0,

    @ColumnInfo(name = "cliente_id")
    val clienteId: Long,

    @ColumnInfo(name = "tipo_recordatorio")
    val tipoRecordatorio: TipoRecordatorioCliente,

    val mensaje: String,

    @ColumnInfo(name = "fecha_envio")
    val fechaEnvio: LocalDateTime
)
