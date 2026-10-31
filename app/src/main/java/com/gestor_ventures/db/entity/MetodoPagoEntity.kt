package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.MetodoPago

/**
 * Un método de pago que el negocio apagó. Sin fila, el método está activo: así un negocio nuevo
 * (o uno que ya existía antes de esta tabla) ofrece todos sin tener que configurar nada.
 */
@Entity(
    tableName = "metodos_pago_inactivos",
    foreignKeys = [
        ForeignKey(
            entity = NegocioEntity::class,
            parentColumns = ["negocio_id"],
            childColumns = ["negocio_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("negocio_id", "metodo_pago", unique = true)]
)
data class MetodoPagoInactivoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "metodo_pago_inactivo_id")
    val metodoPagoInactivoId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "metodo_pago")
    val metodoPago: MetodoPago
)
