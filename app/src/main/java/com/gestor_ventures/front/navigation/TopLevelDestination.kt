package com.gestor_ventures.front.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gestor_ventures.R

/** Pestañas de la barra inferior. Cada una es la raíz de su propia pila de navegación. */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Inicio("inicio", R.string.destino_inicio, R.drawable.ic_home),
    Finanzas("finanzas", R.string.destino_finanzas, R.drawable.ic_chart_line),
    Agenda("agenda", R.string.destino_agenda, R.drawable.ic_calendar),
    Clientes("clientes", R.string.destino_clientes, R.drawable.ic_users),
    Asistente("asistente", R.string.destino_asistente, R.drawable.ic_lightbulb),
}

/** Pestañas del rol Líder (administrador). El rol Vendedor tendrá su propio conjunto. */
val destinosLider = listOf(
    TopLevelDestination.Inicio,
    TopLevelDestination.Finanzas,
    TopLevelDestination.Agenda,
    TopLevelDestination.Clientes,
    TopLevelDestination.Asistente,
)
