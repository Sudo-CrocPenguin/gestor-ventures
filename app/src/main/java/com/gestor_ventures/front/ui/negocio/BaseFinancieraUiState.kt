package com.gestor_ventures.front.ui.negocio

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorBaseFinanciera
import com.gestor_ventures.back.model.GastoFijo
import com.gestor_ventures.front.util.formatMiles
import java.time.LocalDate

/**
 * HU-06, HU-08 y HU-09. Paso 2 del onboarding: gastos fijos, meta de ahorro y reinversión.
 * Todo es opcional, así que siempre se puede finalizar.
 */
data class BaseFinancieraUiState(
    val gastosFijos: List<GastoFijo> = emptyList(),
    val metaMonto: String = "",
    val fechaLimite: LocalDate? = null,
    val porcentajeReinversion: Int = 0,
    /** Lo que hay que apartar cada mes para llegar a la meta; null si aún no se puede calcular. */
    val ahorroMensual: Double? = null,
    val guardando: Boolean = false,
    val error: ErrorBaseFinanciera? = null,
    val formularioGasto: FormularioGastoFijo? = null,
    /** El gasto fijo que el usuario tocó, mientras elige qué hacer con él. */
    val accionesGasto: GastoFijo? = null,
) {
    val metaFormateada: String
        get() = if (metaMonto.isEmpty()) "" else formatMiles(metaMonto.toLongOrNull() ?: 0L)

    val totalGastosFijos: Double get() = gastosFijos.sumOf { it.monto }
}

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorBaseFinanciera.mensajeRes(): Int = when (this) {
    ErrorBaseFinanciera.NombreGastoVacio -> R.string.base_error_nombre_gasto
    ErrorBaseFinanciera.MontoNoPositivo -> R.string.base_error_monto
    ErrorBaseFinanciera.MetaSinMonto -> R.string.base_error_meta_monto
    ErrorBaseFinanciera.FechaLimiteNoPosterior -> R.string.base_error_fecha_limite
    ErrorBaseFinanciera.PorcentajeFueraDeRango -> R.string.base_error_porcentaje
}
