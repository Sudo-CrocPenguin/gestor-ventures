package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.EstadoSugerencia
import com.gestor_ventures.db.enums.TipoAlerta
import java.time.LocalDateTime

/**
 * HU-34 y HU-39. Cada fila es un turno de pregunta/respuesta con el asistente.
 * [sesionId] agrupa los turnos de una misma conversación, así que no hace falta una
 * tabla "conversaciones" aparte: el historial de HU-39 es un GROUP BY sesion_id sobre
 * esta misma tabla.
 */
@Entity(
    tableName = "consultas_ia",
    foreignKeys = [
        ForeignKey(entity = NegocioEntity::class, parentColumns = ["negocio_id"], childColumns = ["negocio_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("negocio_id"), Index("sesion_id")]
)
data class ConsultaIaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "consulta_id")
    val consultaId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "sesion_id")
    val sesionId: Long,

    val pregunta: String,

    val respuesta: String,

    @ColumnInfo(name = "fecha_hora")
    val fechaHora: LocalDateTime
)

/** HU-35. Sugerencia de promoción generada por el asistente. */
@Entity(
    tableName = "sugerencias",
    foreignKeys = [
        ForeignKey(entity = NegocioEntity::class, parentColumns = ["negocio_id"], childColumns = ["negocio_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("negocio_id")]
)
data class SugerenciaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sugerencia_id")
    val sugerenciaId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "tipo_sugerencia")
    val tipoSugerencia: String, // categoría libre (no enumerada en la especificación)

    val contenido: String,

    @ColumnInfo(name = "estado_sugerencia")
    val estadoSugerencia: EstadoSugerencia = EstadoSugerencia.PENDIENTE,

    @ColumnInfo(name = "fecha_generacion")
    val fechaGeneracion: LocalDateTime
)

/** HU-38. Alerta automática según el comportamiento financiero del negocio. */
@Entity(
    tableName = "alertas",
    foreignKeys = [
        ForeignKey(entity = NegocioEntity::class, parentColumns = ["negocio_id"], childColumns = ["negocio_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("negocio_id")]
)
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "alerta_id")
    val alertaId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "tipo_alerta")
    val tipoAlerta: TipoAlerta,

    val descripcion: String,

    val leida: Boolean = false,

    @ColumnInfo(name = "fecha_generacion")
    val fechaGeneracion: LocalDateTime
)
