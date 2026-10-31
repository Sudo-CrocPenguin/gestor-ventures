package com.gestor_ventures.front.ui.tour

import androidx.annotation.StringRes
import com.gestor_ventures.R

/** Dónde tiene que estar la app para que se vea lo que explica el paso. */
enum class EscenaTour { Inicio, Finanzas, Agenda, Clientes, Asistente, Menu }

/**
 * Un paso del tour. [objetivo] es el nombre con el que la pantalla marcó el elemento (ver
 * `tourTarget`); sin objetivo, la tarjeta sale al centro y no se resalta nada.
 */
data class PasoTour(
    val escena: EscenaTour,
    val objetivo: String?,
    @param:StringRes val tituloRes: Int,
    @param:StringRes val textoRes: Int,
)

/** El recorrido completo, en el orden en que se ve la app: Inicio, cada pestaña y el menú lateral. */
private val pasosDelLider = listOf(
    PasoTour(EscenaTour.Inicio, null, R.string.tour_bienvenida_titulo, R.string.tour_bienvenida_texto),
    PasoTour(EscenaTour.Inicio, "inicio_resumen", R.string.tour_inicio_resumen_titulo, R.string.tour_inicio_resumen_texto),
    PasoTour(EscenaTour.Inicio, "inicio_venta", R.string.tour_inicio_venta_titulo, R.string.tour_inicio_venta_texto),
    PasoTour(EscenaTour.Inicio, "inicio_caja", R.string.tour_inicio_caja_titulo, R.string.tour_inicio_caja_texto),
    PasoTour(EscenaTour.Inicio, "barra_menu", R.string.tour_barra_menu_titulo, R.string.tour_barra_menu_texto),
    PasoTour(EscenaTour.Inicio, "barra_campana", R.string.tour_barra_campana_titulo, R.string.tour_barra_campana_texto),
    PasoTour(EscenaTour.Finanzas, "tab_finanzas", R.string.tour_finanzas_titulo, R.string.tour_finanzas_texto),
    PasoTour(EscenaTour.Finanzas, "finanzas_secciones", R.string.tour_finanzas_secciones_titulo, R.string.tour_finanzas_secciones_texto),
    PasoTour(EscenaTour.Finanzas, "finanzas_periodos", R.string.tour_finanzas_periodos_titulo, R.string.tour_finanzas_periodos_texto),
    PasoTour(EscenaTour.Agenda, "tab_agenda", R.string.tour_agenda_titulo, R.string.tour_agenda_texto),
    PasoTour(EscenaTour.Agenda, "agenda_vistas", R.string.tour_agenda_vistas_titulo, R.string.tour_agenda_vistas_texto),
    PasoTour(EscenaTour.Agenda, "agenda_acciones", R.string.tour_agenda_acciones_titulo, R.string.tour_agenda_acciones_texto),
    PasoTour(EscenaTour.Clientes, "tab_clientes", R.string.tour_clientes_titulo, R.string.tour_clientes_texto),
    PasoTour(EscenaTour.Clientes, "clientes_buscar", R.string.tour_clientes_buscar_titulo, R.string.tour_clientes_buscar_texto),
    PasoTour(EscenaTour.Clientes, "clientes_agregar", R.string.tour_clientes_agregar_titulo, R.string.tour_clientes_agregar_texto),
    PasoTour(EscenaTour.Asistente, "tab_asistente", R.string.tour_asistente_titulo, R.string.tour_asistente_texto),
    PasoTour(EscenaTour.Asistente, "asistente_acciones", R.string.tour_asistente_acciones_titulo, R.string.tour_asistente_acciones_texto),
    PasoTour(EscenaTour.Asistente, "asistente_redactor", R.string.tour_asistente_redactor_titulo, R.string.tour_asistente_redactor_texto),
    PasoTour(EscenaTour.Menu, "menu_general", R.string.tour_menu_general_titulo, R.string.tour_menu_general_texto),
    PasoTour(EscenaTour.Menu, "menu_negocios", R.string.tour_menu_negocios_titulo, R.string.tour_menu_negocios_texto),
    PasoTour(EscenaTour.Menu, "menu_config", R.string.tour_menu_config_titulo, R.string.tour_menu_config_texto),
    PasoTour(EscenaTour.Inicio, null, R.string.tour_fin_titulo, R.string.tour_fin_texto),
)

/** Lo que un vendedor no ve en la app: el tour no puede señalarle botones que no existen. */
private val ObjetivosSoloDelLider = setOf("inicio_resumen", "clientes_agregar", "menu_config")

/** El recorrido de cada rol: el vendedor no pasa por Finanzas, el asistente ni la configuración. */
fun pasosDelTour(esLider: Boolean): List<PasoTour> =
    if (esLider) {
        pasosDelLider
    } else {
        pasosDelLider.filter {
            it.escena != EscenaTour.Finanzas && it.escena != EscenaTour.Asistente &&
                it.objetivo !in ObjetivosSoloDelLider
        }
    }

/** [visible] es falso cuando el tour ya se vio o todavía no toca mostrarlo. */
data class TourUiState(
    val visible: Boolean = false,
    val paso: Int = 0,
    val esLider: Boolean = true,
) {
    val pasos: List<PasoTour> get() = pasosDelTour(esLider)
    val esUltimo: Boolean get() = paso >= pasos.lastIndex
    val actual: PasoTour get() = pasos[paso.coerceIn(0, pasos.lastIndex)]
}
