package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.ResumenFinanciero
import com.gestor_ventures.back.model.SeleccionDePeriodo
import com.gestor_ventures.front.model.FormularioRango

/**
 * HU-16. Estado de la sección "Resumen" de Finanzas.
 *
 * Guarda el resumen del back tal cual, sin copiar sus campos uno por uno: todo lo que la
 * pantalla muestra son cuentas que [ResumenFinanciero] ya sabe hacer, y repetirlas acá sería
 * dejar dos versiones de la misma regla.
 */
data class ResumenFinancieroUiState(
    val resumen: ResumenFinanciero? = null,
    val cargando: Boolean = true,
    val seleccion: SeleccionDePeriodo = SeleccionDePeriodo.Rapido(PeriodoPredefinido.Mes),
    val rango: FormularioRango? = null,
) {
    /** Sin negocio activo no hay nada que resumir; pasa apenas se abre la app por primera vez. */
    val sinNegocio: Boolean get() = !cargando && resumen == null
}
