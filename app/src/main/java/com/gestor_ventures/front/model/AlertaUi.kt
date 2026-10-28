package com.gestor_ventures.front.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.TipoAlerta

/**
 * HU-38. Cómo se llama y con qué ícono se ve cada tipo de alerta.
 *
 * Vive acá y no dentro de una pantalla porque lo usan dos: notificaciones (HU-41) y el inicio,
 * que muestra los mismos avisos sin obligar a ir a buscarlos. Si cada una tuviera su
 * traducción, el día que "Tus ventas bajaron" cambie de texto lo diría en una sola.
 */
@StringRes
fun TipoAlerta.tituloRes(): Int = when (this) {
    TipoAlerta.CaidaVentas -> R.string.alerta_caida_ventas
    TipoAlerta.GastoElevado -> R.string.alerta_gasto_elevado
    TipoAlerta.MetaEnRiesgo -> R.string.alerta_meta_en_riesgo
}

@DrawableRes
fun TipoAlerta.iconRes(): Int = when (this) {
    TipoAlerta.CaidaVentas -> R.drawable.ic_chart_line
    TipoAlerta.GastoElevado -> R.drawable.ic_card
    TipoAlerta.MetaEnRiesgo -> R.drawable.ic_lightbulb
}
