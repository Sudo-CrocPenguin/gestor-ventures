package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.EstadoMeta
import com.gestor_ventures.db.enums.EstadoPago
import com.gestor_ventures.db.enums.Frecuencia
import com.gestor_ventures.db.enums.TipoActividad
import com.gestor_ventures.db.enums.TipoCategoria
import java.time.LocalDate
import java.time.LocalDateTime

/** HU-05, HU-09, HU-10. Un usuario puede tener varios negocios (relación 1:N). */
@Entity(
    tableName = "negocios",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["usuario_id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("usuario_id")]
)
data class NegocioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "negocio_id")
    val negocioId: Long = 0,

    @ColumnInfo(name = "usuario_id")
    val usuarioId: Long,

    @ColumnInfo(name = "nombre_negocio")
    val nombreNegocio: String, // máx. 100 caracteres

    @ColumnInfo(name = "tipo_actividad")
    val tipoActividad: TipoActividad,

    @ColumnInfo(name = "categoria_negocio")
    val categoriaNegocio: String, // rubro libre (ej. "peluquería"); no confundir con CategoriaEntity (gastos/costos)

    @ColumnInfo(name = "porcentaje_reinversion")
    val porcentajeReinversion: Double, // rango 0–100, validar en capa de aplicación

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: LocalDateTime,

    @ColumnInfo(name = "fecha_actualizacion")
    val fechaActualizacion: LocalDateTime? = null
)

/** HU-06. Gastos recurrentes configurados a nivel de negocio; se suman al resumen (HU-16). */
@Entity(
    tableName = "gastos_fijos",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("negocio_id")]
)
data class GastoFijoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "gasto_fijo_id")
    val gastoFijoId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "nombre_gasto")
    val nombreGasto: String,

    val monto: Double, // > 0

    val frecuencia: Frecuencia,

    @ColumnInfo(name = "fecha_registro")
    val fechaRegistro: LocalDateTime
)

/** HU-07. Préstamos y pagos recurrentes pendientes. */
@Entity(
    tableName = "obligaciones",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("negocio_id")]
)
data class ObligacionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "obligacion_id")
    val obligacionId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "nombre_obligacion")
    val nombreObligacion: String,

    val monto: Double, // > 0

    @ColumnInfo(name = "fecha_vencimiento")
    val fechaVencimiento: LocalDate,

    @ColumnInfo(name = "estado_pago")
    val estadoPago: EstadoPago = EstadoPago.PENDIENTE,

    @ColumnInfo(name = "fecha_registro")
    val fechaRegistro: LocalDateTime
)

/** HU-08. Meta de ahorro del negocio; puede haber varias a lo largo del tiempo. */
@Entity(
    tableName = "metas_ahorro",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("negocio_id")]
)
data class MetaAhorroEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "meta_ahorro_id")
    val metaAhorroId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "monto_objetivo")
    val montoObjetivo: Double, // > 0

    @ColumnInfo(name = "fecha_limite")
    val fechaLimite: LocalDate, // posterior a la fecha actual

    @ColumnInfo(name = "estado_meta")
    val estadoMeta: EstadoMeta = EstadoMeta.ACTIVA,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: LocalDateTime
)

/** HU-15. Categorías propias del negocio para clasificar gastos y costos. */
@Entity(
    tableName = "categorias",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("negocio_id")]
)
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "categoria_id")
    val categoriaId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "nombre_categoria")
    val nombreCategoria: String, // máx. 50 caracteres

    @ColumnInfo(name = "tipo_categoria")
    val tipoCategoria: TipoCategoria
)
