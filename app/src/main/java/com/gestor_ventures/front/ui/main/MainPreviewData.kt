package com.gestor_ventures.front.ui.main

import com.gestor_ventures.front.ui.menu.MenuLateralPreviewData

/** Datos de ejemplo para los @Preview y para arrancar la UI mientras no existan los repositorios. */
object MainPreviewData {
    val uiState = MainUiState(
        usuario = MenuLateralPreviewData.usuario,
        negocios = MenuLateralPreviewData.negocios,
        negocioActivoId = "dulce",
        notificacionesSinLeer = 2,
    )
}
