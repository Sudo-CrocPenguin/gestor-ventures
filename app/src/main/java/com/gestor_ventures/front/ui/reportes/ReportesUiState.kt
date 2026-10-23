package com.gestor_ventures.front.ui.reportes

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.usecase.EscalaDeTiempo
import java.time.LocalDate

/**
 * HU-40. Ingresos y egresos a lo largo del tiempo.
 *
 * Mientras [cargando] no se resuelva no se dibuja nada: un gráfico en cero que un instante
 * después salta a su forma real se ve como un parpadeo, y peor, se alcanza a leer mal.
 */
data class ReportesUiState(
    val puntos: List<PuntoUi> = emptyList(),
    val escala: EscalaDeTiempo = EscalaDeTiempo.Mensual,
    val cargando: Boolean = true,
) {
    /** El periodo completo no tuvo un solo movimiento: no hay tendencia que mostrar. */
    val vacio: Boolean
        get() = !cargando && puntos.all { it.ingresos == 0.0 && it.egresos == 0.0 }

    val totalIngresos: Double get() = puntos.sumOf { it.ingresos }

    val totalGastos: Double get() = puntos.sumOf { it.gastos }

    val totalCostos: Double get() = puntos.sumOf { it.costos }

    val totalEgresos: Double get() = totalGastos + totalCostos

    /** Lo que quedó en todo el periodo. Puede ser negativo, y así se muestra. */
    val ganancia: Double get() = totalIngresos - totalEgresos
}

/**
 * Un tramo del gráfico, ya listo para dibujarse.
 *
 * Gastos y costos van aparte porque así los distingue el resumen de HU-16: un costo es lo que
 * cuesta producir lo que se vende y un gasto es todo lo demás. La línea de egresos los suma.
 */
data class PuntoUi(
    val inicio: LocalDate,
    val ingresos: Double,
    val gastos: Double,
    val costos: Double,
) {
    val egresos: Double get() = gastos + costos
}

/** Cómo se llama cada escala en los chips. */
@StringRes
fun EscalaDeTiempo.labelRes(): Int = when (this) {
    EscalaDeTiempo.Semanal -> R.string.reportes_semanal
    EscalaDeTiempo.Mensual -> R.string.reportes_mensual
}
