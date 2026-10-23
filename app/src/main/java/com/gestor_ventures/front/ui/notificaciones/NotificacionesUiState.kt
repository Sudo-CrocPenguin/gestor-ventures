package com.gestor_ventures.front.ui.notificaciones

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Alerta
import com.gestor_ventures.back.model.TipoAlerta

data class NotificacionesUiState(
    val alertas: List<Alerta> = emptyList(),
    val cargando: Boolean = true,
) {
    val vacio: Boolean get() = !cargando && alertas.isEmpty()

    val hayNoLeidas: Boolean get() = alertas.any { !it.leida }
}

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
