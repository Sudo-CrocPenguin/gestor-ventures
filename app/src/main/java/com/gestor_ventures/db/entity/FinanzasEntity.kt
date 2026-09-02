package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.MetodoPago
import com.gestor_ventures.db.enums.TipoRegistroVenta
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-11/HU-12 (issue combinada #11). Un mismo registro cubre venta detallada y venta
 * rápida; [productoServicio], [clienteId] y [metodoPago] solo aplican cuando
 * [tipoRegistro] = DETALLADO.
 */
@Entity(
    tableName = "ventas",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClienteEntity::class,
            parentColumns = ["cliente_id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("negocio_id"), Index("cliente_id")]
)
data class VentaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "venta_id")
    val ventaId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "cliente_id")
    val clienteId: Long? = null,

    @ColumnInfo(name = "tipo_registro")
    val tipoRegistro: TipoRegistroVenta,

    @ColumnInfo(name = "producto_servicio")
    val productoServicio: String? = null,

    val monto: Double, // > 0

    @ColumnInfo(name = "metodo_pago")
    val metodoPago: MetodoPago? = null,

    @ColumnInfo(name = "fecha_hora")
    val fechaHora: LocalDateTime
)

/** HU-13. Costo directo asociado a un producto/servicio, usado para calcular margen. */
@Entity(
    tableName = "costos",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["categoria_id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("negocio_id"), Index("categoria_id")]
)
data class CostoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "costo_id")
    val costoId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "categoria_id")
    val categoriaId: Long? = null, // HU-15: costos también son categorizables

    @ColumnInfo(name = "producto_servicio")
    val productoServicio: String,

    @ColumnInfo(name = "monto_costo")
    val montoCosto: Double, // > 0

    @ColumnInfo(name = "fecha_registro")
    val fechaRegistro: LocalDateTime
)

/** HU-14. Gasto general del negocio, distinto de los gastos fijos (HU-06). */
@Entity(
    tableName = "gastos",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["categoria_id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("negocio_id"), Index("categoria_id")]
)
data class GastoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "gasto_id")
    val gastoId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "categoria_id")
    val categoriaId: Long? = null,

    val descripcion: String,

    val monto: Double, // > 0

    val fecha: LocalDate
)
