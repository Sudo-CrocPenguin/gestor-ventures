package com.gestor_ventures.front.model

import androidx.annotation.StringRes
import com.gestor_ventures.R

/**
 * Rol del usuario dentro de un negocio. Define qué ve cada quien (mockups/fase1-navegable.html,
 * tabla "Qué ve cada rol"): el Líder administra el negocio completo y el Vendedor solo su turno.
 *
 * Todavía no existe en `db/`: las tablas actuales no modelan el equipo. Cuando se agregue, este
 * enum se mapea desde la entidad correspondiente.
 */
enum class RolNegocio(@param:StringRes val labelRes: Int) {
    Lider(R.string.rol_lider),
    Vendedor(R.string.rol_vendedor),
}
