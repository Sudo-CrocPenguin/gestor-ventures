package com.gestor_ventures.front.ui.main

import com.gestor_ventures.front.model.UsuarioUi
import com.gestor_ventures.front.ui.menu.MenuLateralPreviewData

/**
 * Lo que todavía no sale de la base de datos.
 *
 * TEMPORAL — el usuario viene de aquí hasta que exista HU-01 (registro e inicio de sesión), y
 * el contador de notificaciones hasta HU-40/HU-41. Los negocios ya son reales.
 */
object MainPreviewData {

    val usuario: UsuarioUi = MenuLateralPreviewData.usuario

    const val NOTIFICACIONES_SIN_LEER = 2

    val uiState = MainUiState(
        usuario = usuario,
        negocios = MenuLateralPreviewData.negocios,
        negocioActivoId = "dulce",
        notificacionesSinLeer = NOTIFICACIONES_SIN_LEER,
        cargando = false,
    )
}
