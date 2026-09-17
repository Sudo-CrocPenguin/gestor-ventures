package com.gestor_ventures.back.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * HU-16. Cómo le fue al negocio en el mes y cuánto de eso se puede sacar hoy.
 *
 * El resumen no guarda ningún número calculado: recibe lo que pasó (ingresos, gastos, costos) y
 * lo que el usuario configuró (gastos fijos, obligaciones, meta, reinversión), y deriva el
 * resto. Así no puede quedar desactualizado con respecto a sus propias partes.
 *
 * El orden en que se descuenta importa y es el del mockup: primero lo que el negocio necesita
 * para funcionar, después lo que ya está comprometido, y lo que sobra es lo disponible.
 */
data class ResumenFinanciero(
    val mes: YearMonth,
    /** HU-11/HU-12. Lo que entró por ventas. */
    val ingresos: Double = 0.0,
    /** HU-14. Lo que se gastó en hacer funcionar el negocio. */
    val gastos: Double = 0.0,
    /** HU-13. Lo que costó producir lo que se vende. */
    val costos: Double = 0.0,
    /** HU-06. Los gastos fijos configurados, llevados a su equivalente de un mes. */
    val gastosFijos: Double = 0.0,
    /** HU-07. Lo que se debe y todavía no se ha pagado. */
    val obligacionesPendientes: Double = 0.0,
    /** HU-08. Lo que toca apartar este mes para llegar a la meta a tiempo. */
    val apartadoParaMeta: Double = 0.0,
    /** HU-09. De cada ganancia, cuánto vuelve al negocio. De 0 a 100. */
    val porcentajeReinversion: Double = 0.0,
    /** HU-08. Cómo va la meta activa; null si el negocio no tiene ninguna. */
    val progresoMeta: ProgresoMeta? = null,
) {
    val egresos: Double get() = gastos + costos + gastosFijos

    val ganancia: Double get() = ingresos - egresos

    /**
     * HU-09. Lo que la app calcula que se puede reinvertir: el porcentaje configurado sobre la
     * ganancia. Un mes en pérdida no deja nada para reinvertir, así que no se calcula sobre un
     * número negativo.
     */
    val reinversion: Double
        get() = if (ganancia <= 0.0) 0.0 else ganancia * porcentajeReinversion / 100.0

    /** Lo que ya tiene dueño antes de que el emprendedor toque la ganancia. */
    val comprometido: Double get() = obligacionesPendientes + apartadoParaMeta + reinversion

    /** HU-16. Lo que se puede sacar del negocio hoy sin quedar corto. Puede ser negativo. */
    val disponible: Double get() = ganancia - comprometido

    /** Qué proporción de lo vendido quedó como ganancia; null si no hubo ventas. */
    val margen: Float? get() = if (ingresos <= 0.0) null else (ganancia / ingresos).toFloat()

    /** Sin un solo movimiento el resumen son ceros, y un tablero de ceros no dice nada. */
    val sinMovimientos: Boolean get() = ingresos == 0.0 && gastos == 0.0 && costos == 0.0
}

/**
 * HU-08. Qué tan cerca está el negocio de su meta de ahorro.
 *
 * [acumulado] es la ganancia que el negocio lleva desde que se definió la meta. No es plata
 * apartada en una cuenta —la app todavía no registra movimientos de ahorro— sino lo que el
 * negocio ha generado y podría haber guardado, que es lo que se puede medir con lo que hay.
 */
data class ProgresoMeta(
    val montoObjetivo: Double,
    val acumulado: Double,
    val fechaLimite: LocalDate,
) {
    val fraccion: Float
        get() = if (montoObjetivo <= 0.0) 0f else (acumulado / montoObjetivo).toFloat().coerceIn(0f, 1f)

    val cumplida: Boolean get() = acumulado >= montoObjetivo

    val falta: Double get() = (montoObjetivo - acumulado).coerceAtLeast(0.0)
}
