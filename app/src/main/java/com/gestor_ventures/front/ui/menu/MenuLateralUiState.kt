package com.gestor_ventures.front.ui.menu

import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.UsuarioUi

/**
 * Estado del menú lateral. Lo construye quien abre el menú (`MainScreen`) a partir del estado
 * del marco de la app, para que el negocio activo tenga una sola fuente de verdad.
 */
data class MenuLateralUiState(
    val usuario: UsuarioUi,
    val negocios: List<NegocioUi>,
    val negocioActivoId: String,
) {
    val negocioActivo: NegocioUi? get() = negocios.firstOrNull { it.id == negocioActivoId }

    /** Vacía cuando el rol no administra el negocio. */
    val opcionesConfiguracion: List<OpcionMenu>
        get() = negocioActivo?.let { opcionesDeConfiguracion(it.rol) }.orEmpty()
}
