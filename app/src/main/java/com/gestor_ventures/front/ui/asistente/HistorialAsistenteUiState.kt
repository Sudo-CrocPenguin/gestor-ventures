package com.gestor_ventures.front.ui.asistente

import com.gestor_ventures.back.model.ConversacionAsistente

data class HistorialAsistenteUiState(
    val conversaciones: List<ConversacionAsistente> = emptyList(),
    val cargando: Boolean = true,
    val porEliminar: ConversacionAsistente? = null,
) {
    val vacio: Boolean get() = !cargando && conversaciones.isEmpty()
}
