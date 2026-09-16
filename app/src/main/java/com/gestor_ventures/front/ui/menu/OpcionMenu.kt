package com.gestor_ventures.front.ui.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.front.model.RolNegocio

/**
 * Opciones del menú lateral. Cada una llevará a su pantalla cuando exista; por ahora la
 * pantalla que la muestra decide qué hacer con ella.
 */
enum class OpcionMenu(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    InformacionNegocio(R.string.menu_info_negocio, R.drawable.ic_building),  // HU-05
    GastosFijos(R.string.menu_gastos_fijos, R.drawable.ic_box),              // HU-06, HU-07
    Categorias(R.string.menu_categorias, R.drawable.ic_list),                // HU-15
    MetodosPago(R.string.menu_metodos_pago, R.drawable.ic_card),             // HU-11
    Equipo(R.string.menu_equipo, R.drawable.ic_team),
    MiPerfil(R.string.menu_mi_perfil, R.drawable.ic_user),                   // HU-04
    Notificaciones(R.string.menu_notificaciones, R.drawable.ic_bell),        // HU-40, HU-41
    CerrarSesion(R.string.menu_cerrar_sesion, R.drawable.ic_logout),         // HU-02
}

/**
 * Configuración del negocio activo. Solo la ve el Líder: el Vendedor no administra el
 * negocio (mockups/fase1-navegable.html, tabla "Qué ve cada rol").
 */
fun opcionesDeConfiguracion(rol: RolNegocio): List<OpcionMenu> = when (rol) {
    RolNegocio.Lider -> listOf(
        OpcionMenu.InformacionNegocio,
        OpcionMenu.GastosFijos,
        OpcionMenu.Categorias,
        OpcionMenu.MetodosPago,
        OpcionMenu.Equipo,
    )
    RolNegocio.Vendedor -> emptyList()
}

/** Opciones que ve cualquier rol. */
val opcionesGenerales = listOf(OpcionMenu.MiPerfil, OpcionMenu.Notificaciones)
