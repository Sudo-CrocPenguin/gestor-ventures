package com.gestor_ventures.front.ui.notificaciones

import com.gestor_ventures.back.model.Alerta
import com.gestor_ventures.back.notificaciones.AjustesNotificaciones

/** HU-38 y HU-41. Estado de la pantalla Notificaciones: qué recibir y las alertas del negocio. */
data class NotificacionesUiState(
    val alertas: List<Alerta> = emptyList(),
    val cargando: Boolean = true,
    /** HU-41. Los interruptores de qué avisos recibir. */
    val ajustes: AjustesNotificaciones = AjustesNotificaciones(),
) {
    val vacio: Boolean get() = !cargando && alertas.isEmpty()

    val hayNoLeidas: Boolean get() = alertas.any { !it.leida }
}
