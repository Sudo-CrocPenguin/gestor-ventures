package com.gestor_ventures.db

import androidx.room.TypeConverter
import com.gestor_ventures.db.enums.EstadoCaja
import com.gestor_ventures.db.enums.EstadoCita
import com.gestor_ventures.db.enums.EstadoMeta
import com.gestor_ventures.db.enums.EstadoPago
import com.gestor_ventures.db.enums.EstadoPedido
import com.gestor_ventures.db.enums.EstadoSugerencia
import com.gestor_ventures.db.enums.Frecuencia
import com.gestor_ventures.db.enums.MetodoPago
import com.gestor_ventures.db.enums.TipoActividad
import com.gestor_ventures.db.enums.TipoAlerta
import com.gestor_ventures.db.enums.TipoCategoria
import com.gestor_ventures.db.enums.TipoNotificacion
import com.gestor_ventures.db.enums.TipoRecordatorioCliente
import com.gestor_ventures.db.enums.TipoRegistroVenta
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * SQLite no tiene tipos nativos de fecha/hora ni de enum: las fechas se guardan como
 * epoch millis/epoch day (Long) y los enums como su nombre (String).
 *
 * Requiere Java 8+ desugaring habilitado en el módulo de la app
 * (coreLibraryDesugaringEnabled = true en build.gradle) para usar java.time en minSdk < 26.
 */
class Converters {

    // ---------- Fecha y hora ----------

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? =
        value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): Int? = value?.toSecondOfDay()

    @TypeConverter
    fun toLocalTime(value: Int?): LocalTime? = value?.let { LocalTime.ofSecondOfDay(it.toLong()) }

    // ---------- Enums ----------

    @TypeConverter fun fromTipoActividad(v: TipoActividad?): String? = v?.name
    @TypeConverter fun toTipoActividad(v: String?): TipoActividad? = v?.let { TipoActividad.valueOf(it) }

    @TypeConverter fun fromFrecuencia(v: Frecuencia?): String? = v?.name
    @TypeConverter fun toFrecuencia(v: String?): Frecuencia? = v?.let { Frecuencia.valueOf(it) }

    @TypeConverter fun fromEstadoPago(v: EstadoPago?): String? = v?.name
    @TypeConverter fun toEstadoPago(v: String?): EstadoPago? = v?.let { EstadoPago.valueOf(it) }

    @TypeConverter fun fromEstadoMeta(v: EstadoMeta?): String? = v?.name
    @TypeConverter fun toEstadoMeta(v: String?): EstadoMeta? = v?.let { EstadoMeta.valueOf(it) }

    @TypeConverter fun fromTipoCategoria(v: TipoCategoria?): String? = v?.name
    @TypeConverter fun toTipoCategoria(v: String?): TipoCategoria? = v?.let { TipoCategoria.valueOf(it) }

    @TypeConverter fun fromTipoRegistroVenta(v: TipoRegistroVenta?): String? = v?.name
    @TypeConverter fun toTipoRegistroVenta(v: String?): TipoRegistroVenta? = v?.let { TipoRegistroVenta.valueOf(it) }

    @TypeConverter fun fromMetodoPago(v: MetodoPago?): String? = v?.name
    @TypeConverter fun toMetodoPago(v: String?): MetodoPago? = v?.let { MetodoPago.valueOf(it) }

    @TypeConverter fun fromEstadoCaja(v: EstadoCaja?): String? = v?.name
    @TypeConverter fun toEstadoCaja(v: String?): EstadoCaja? = v?.let { EstadoCaja.valueOf(it) }

    @TypeConverter fun fromEstadoCita(v: EstadoCita?): String? = v?.name
    @TypeConverter fun toEstadoCita(v: String?): EstadoCita? = v?.let { EstadoCita.valueOf(it) }

    @TypeConverter fun fromEstadoPedido(v: EstadoPedido?): String? = v?.name
    @TypeConverter fun toEstadoPedido(v: String?): EstadoPedido? = v?.let { EstadoPedido.valueOf(it) }

    @TypeConverter fun fromTipoRecordatorioCliente(v: TipoRecordatorioCliente?): String? = v?.name
    @TypeConverter fun toTipoRecordatorioCliente(v: String?): TipoRecordatorioCliente? =
        v?.let { TipoRecordatorioCliente.valueOf(it) }

    @TypeConverter fun fromEstadoSugerencia(v: EstadoSugerencia?): String? = v?.name
    @TypeConverter fun toEstadoSugerencia(v: String?): EstadoSugerencia? = v?.let { EstadoSugerencia.valueOf(it) }

    @TypeConverter fun fromTipoAlerta(v: TipoAlerta?): String? = v?.name
    @TypeConverter fun toTipoAlerta(v: String?): TipoAlerta? = v?.let { TipoAlerta.valueOf(it) }

    @TypeConverter fun fromTipoNotificacion(v: TipoNotificacion?): String? = v?.name
    @TypeConverter fun toTipoNotificacion(v: String?): TipoNotificacion? = v?.let { TipoNotificacion.valueOf(it) }
}
