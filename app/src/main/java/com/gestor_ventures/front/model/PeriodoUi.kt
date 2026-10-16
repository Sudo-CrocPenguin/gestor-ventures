package com.gestor_ventures.front.model

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.PeriodoPredefinido

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
