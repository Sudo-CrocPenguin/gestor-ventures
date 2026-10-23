package com.gestor_ventures.front.model

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.PeriodoPredefinido
import java.time.LocalDate

/**
 * Cómo se llama cada periodo en pantalla.
 *
 * Vive acá y no dentro de una pantalla porque lo usan dos: el historial de movimientos (HU-17)
 * y el de cajas (HU-23). Si cada una tuviera su traducción, el día que "Semana" pase a decir
 * "Esta semana" lo diría en una sola.
 */
@StringRes
fun PeriodoPredefinido.labelRes(): Int = when (this) {
    PeriodoPredefinido.Hoy -> R.string.periodo_hoy
    PeriodoPredefinido.Ayer -> R.string.periodo_ayer
    PeriodoPredefinido.Semana -> R.string.periodo_semana
    PeriodoPredefinido.Mes -> R.string.periodo_mes
}

/**
 * HU-17. Las dos fechas mientras el usuario las elige.
 *
 * Se guardan sueltas y no como un rango porque a medio llenar todavía no son uno: falta una, o
 * están al revés. El rango de verdad lo arma `personalizado()` en back, que es quien decide si
 * sirve.
 */
data class FormularioRango(
    val desde: LocalDate? = null,
    val hasta: LocalDate? = null,
) {
    /** Las dos fechas puestas y en el orden correcto. */
    val valido: Boolean get() = desde != null && hasta != null && !desde.isAfter(hasta)

    /** Las dos puestas pero al revés: es lo único que hay que explicarle al usuario. */
    val invertido: Boolean get() = desde != null && hasta != null && desde.isAfter(hasta)
}
