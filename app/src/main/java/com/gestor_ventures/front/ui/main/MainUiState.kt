package com.gestor_ventures.front.ui.main

import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.UsuarioUi
import com.gestor_ventures.front.ui.menu.MenuLateralUiState

/**
 * Estado del marco de la app: usuario con la sesión abierta, sus negocios y cuál está activo.
 * Es la única fuente de verdad del negocio activo; el menú lateral y la barra superior lo leen
 * de aquí.
 *
 * [cargando] es cierto hasta la primera lectura de la base de datos, para no mandar al usuario
 * al onboarding antes de saber si ya tiene negocios.
 */
data class MainUiState(
    val usuario: UsuarioUi,
    val negocios: List<NegocioUi> = emptyList(),
    val negocioActivoId: String? = null,
    val notificacionesSinLeer: Int = 0,
    val cargando: Boolean = true,
) {
    /** Si no hay uno elegido a mano, el activo es el primero de la lista. */
    val negocioActivo: NegocioUi?
        get() = negocios.firstOrNull { it.id == negocioActivoId } ?: negocios.firstOrNull()

    /**
     * Solo cuando de verdad no hay ninguno. Se pide [negocioActivoId] nulo porque, justo
     * después de crear el primero, la lista todavía viene vacía de la base de datos: sin esa
     * condición la app mandaría de vuelta al onboarding recién creado el negocio.
     */
    val sinNegocios: Boolean get() = !cargando && negocios.isEmpty() && negocioActivoId == null

    /** Lo que el menú lateral necesita mostrar, derivado de este mismo estado. */
    val menu: MenuLateralUiState
        get() = MenuLateralUiState(
            usuario = usuario,
            negocios = negocios,
            negocioActivoId = negocioActivo?.id.orEmpty(),
        )
}
