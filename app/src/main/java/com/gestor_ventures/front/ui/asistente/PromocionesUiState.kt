package com.gestor_ventures.front.ui.asistente

import com.gestor_ventures.back.model.ErrorAsistente
import com.gestor_ventures.back.model.Sugerencia

/** HU-35. Estado de la pantalla de ideas de promociones. */
data class PromocionesUiState(
    val sugerencias: List<Sugerencia> = emptyList(),
    val generando: Boolean = false,
    val error: ErrorAsistente? = null,
    val cargando: Boolean = true,
) {
    /** Las que todavía no se guardaron ni se descartaron. */
    val nuevas: List<Sugerencia> get() = sugerencias.filterNot { it.guardada }

    val guardadas: List<Sugerencia> get() = sugerencias.filter { it.guardada }

    val vacio: Boolean get() = !cargando && sugerencias.isEmpty() && !generando
}
