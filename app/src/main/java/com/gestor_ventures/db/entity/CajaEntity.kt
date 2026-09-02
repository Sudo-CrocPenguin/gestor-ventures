package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gestor_ventures.db.enums.EstadoCaja
import java.time.LocalDateTime

/**
 * HU-19 a HU-23. Una caja por jornada de trabajo.
 *
 * HU-20 (saldo esperado en tiempo real) y HU-22 (diferencia al cierre) NO se persisten:
 * se calculan en tiempo de consulta como
 *   saldo_esperado = monto_inicial + SUM(ventas.monto) − SUM(costos.monto_costo) − SUM(gastos.monto)
 * sobre las ventas/costos/gastos del mismo negocio ocurridos entre fecha_hora_apertura y
 * fecha_hora_cierre (o el momento actual, si la caja sigue abierta). Esto evita una tabla
 * "movimientos_caja" que duplicaría montos que ya existen en ventas/costos/gastos.
 */
@Entity(
    tableName = "cajas",
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
data class CajaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "caja_id")
    val cajaId: Long = 0,

    @ColumnInfo(name = "negocio_id")
    val negocioId: Long,

    @ColumnInfo(name = "monto_inicial")
    val montoInicial: Double, // >= 0

    @ColumnInfo(name = "monto_real_cierre")
    val montoRealCierre: Double? = null, // >= 0, se completa al cerrar (HU-21)

    @ColumnInfo(name = "fecha_hora_apertura")
    val fechaHoraApertura: LocalDateTime,

    @ColumnInfo(name = "fecha_hora_cierre")
    val fechaHoraCierre: LocalDateTime? = null,

    @ColumnInfo(name = "estado_caja")
    val estadoCaja: EstadoCaja = EstadoCaja.ABIERTA,

    @ColumnInfo(name = "nota_diferencia")
    val notaDiferencia: String? = null
)
