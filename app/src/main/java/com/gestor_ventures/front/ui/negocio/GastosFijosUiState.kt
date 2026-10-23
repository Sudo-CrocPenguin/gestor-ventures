package com.gestor_ventures.front.ui.negocio

import com.gestor_ventures.back.model.ErrorBaseFinanciera
import com.gestor_ventures.back.model.GastoFijo

/**
 * HU-06. Configuración de gastos fijos del negocio activo, fuera del onboarding: acá es donde
 * el usuario vuelve cuando le sube el arriendo.
 */
data class GastosFijosUiState(
    val gastosFijos: List<GastoFijo> = emptyList(),
    val cargando: Boolean = true,
    val formularioGasto: FormularioGastoFijo? = null,
    /** El gasto que el usuario tocó, mientras elige qué hacer con él. */
    val acciones: GastoFijo? = null,
    val error: ErrorBaseFinanciera? = null,
) {
    val total: Double get() = gastosFijos.sumOf { it.monto }

    /** Sin gastos no hay nada que listar, pero sí algo que explicar. */
    val vacio: Boolean get() = !cargando && gastosFijos.isEmpty()
}
