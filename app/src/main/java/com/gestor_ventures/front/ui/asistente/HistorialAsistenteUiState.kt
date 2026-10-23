package com.gestor_ventures.front.ui.asistente

import com.gestor_ventures.back.model.ConversacionAsistente

/** HU-39. Estado del historial de conversaciones. */
data class HistorialAsistenteUiState(
    val conversaciones: List<ConversacionAsistente> = emptyList(),
    val cargando: Boolean = true,
    /** La conversación que está a punto de borrarse, esperando confirmación. */
    val porEliminar: ConversacionAsistente? = null,
) {
    val vacio: Boolean get() = !cargando && conversaciones.isEmpty()
}
