package com.gestor_ventures.front.ui.main

import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.UsuarioUi
import com.gestor_ventures.front.ui.menu.MenuLateralUiState
import com.gestor_ventures.back.model.ModoTema

/**
 * Estado del marco de la app: usuario con la sesión abierta, sus negocios y cuál está activo.
 * Es la única fuente de verdad del negocio activo; el menú lateral y la barra superior lo leen
 * de aquí.
 *
 * [cargando] es cierto hasta la primera lectura de la base de datos, para no mandar al usuario
 * al onboarding antes de saber si ya tiene negocios.
 */
data class MainUiState(
    /** Null mientras la sesión no se ha resuelto: es un estado real, no un usuario en blanco. */
    val usuario: UsuarioUi? = null,
    val negocios: List<NegocioUi> = emptyList(),
    val negocioActivoId: String? = null,
    val notificacionesSinLeer: Int = 0,
    val modoTema: ModoTema = ModoTema.SISTEMA,
    val cargando: Boolean = true,
    /** La cuenta se acaba de crear y todavía no tiene su primer negocio. */
    val onboardingPendiente: Boolean = false,
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

    /**
     * Cuándo la app lleva sola a "Registrar negocio": solo con una cuenta nueva que aún no tiene
     * negocio. Una cuenta que ya existía y entra a un dispositivo sin sus negocios ve el inicio
     * vacío, con el botón para registrar uno; no se le impone el formulario.
     */
    val debeRegistrarNegocio: Boolean get() = sinNegocios && onboardingPendiente

    /** Lo que el menú lateral necesita mostrar, derivado de este mismo estado. */
    val menu: MenuLateralUiState
        get() = MenuLateralUiState(
            usuario = usuario,
            negocios = negocios,
            negocioActivoId = negocioActivo?.id.orEmpty(),
        )
}
