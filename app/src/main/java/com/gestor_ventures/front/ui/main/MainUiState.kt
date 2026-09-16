package com.gestor_ventures.front.ui.main

import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.UsuarioUi
import com.gestor_ventures.front.ui.menu.MenuLateralUiState

/**
 * Estado del marco de la app: usuario con la sesión abierta, sus negocios y cuál está activo.
 * Es la única fuente de verdad del negocio activo; el menú lateral y la barra superior lo leen
 * de aquí.
 */
data class MainUiState(
    val usuario: UsuarioUi,
    val negocios: List<NegocioUi>,
    val negocioActivoId: String,
    val notificacionesSinLeer: Int = 0,
) {
    val negocioActivo: NegocioUi? get() = negocios.firstOrNull { it.id == negocioActivoId }

    /** Lo que el menú lateral necesita mostrar, derivado de este mismo estado. */
    val menu: MenuLateralUiState
        get() = MenuLateralUiState(
            usuario = usuario,
            negocios = negocios,
            negocioActivoId = negocioActivoId,
        )
}
