package com.gestor_ventures.front.ui.negocio

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.GastoFijo
import com.gestor_ventures.front.util.formatMiles

/**
 * HU-06. Formulario emergente de un gasto fijo, compartido por el paso 2 del onboarding y por
 * la pantalla de configuración: es la misma pregunta en los dos lados.
 *
 * [gastoFijoId] nulo significa que se está creando; con id, que se está corrigiendo uno que ya
 * existe.
 */
data class FormularioGastoFijo(
    val gastoFijoId: Long? = null,
    val nombre: String = "",
    val monto: String = "",
    val frecuencia: Frecuencia = Frecuencia.MENSUAL,
) {
    val esEdicion: Boolean get() = gastoFijoId != null

    val montoValor: Long get() = monto.toLongOrNull() ?: 0L

    val montoFormateado: String
        get() = if (monto.isEmpty()) "" else formatMiles(montoValor)

    val puedeGuardar: Boolean get() = nombre.isNotBlank() && montoValor > 0L
}

/** Abre el formulario con los datos de un gasto que ya existe, listos para corregir. */
fun formularioDe(gasto: GastoFijo) = FormularioGastoFijo(
    gastoFijoId = gasto.id,
    nombre = gasto.nombre,
    monto = gasto.monto.toLong().toString(),
    frecuencia = gasto.frecuencia,
)

/** Cómo se llama cada frecuencia en pantalla. */
@StringRes
fun Frecuencia.labelRes(): Int = when (this) {
    Frecuencia.SEMANAL -> R.string.frecuencia_semanal
    Frecuencia.QUINCENAL -> R.string.frecuencia_quincenal
    Frecuencia.MENSUAL -> R.string.frecuencia_mensual
    Frecuencia.ANUAL -> R.string.frecuencia_anual
}
