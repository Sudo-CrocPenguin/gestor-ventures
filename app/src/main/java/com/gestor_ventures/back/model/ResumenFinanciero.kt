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

    /** HU-09. Lo que la app calcula que se puede reinvertir. */
    val reinversion: Double get() = reinversionDe(ganancia, porcentajeReinversion)

    /** Lo que ya tiene dueño antes de que el emprendedor toque la ganancia. */
    val comprometido: Double get() = obligacionesPendientes + apartadoParaMeta + reinversion

    /**
     * HU-08. Lo que queda libre después de pagar lo que se debe y de devolverle al negocio lo
     * suyo, pero antes de apartar para la meta. Es con esto que se mide el progreso de la meta:
     * usar el dinero disponible sería descontar dos veces el mismo ahorro.
     */
    val libreParaAhorrar: Double
        get() = libreParaAhorrarDe(ganancia, obligacionesPendientes, porcentajeReinversion)

    /** HU-16. Lo que se puede sacar del negocio hoy sin quedar corto. Puede ser negativo. */
    val disponible: Double get() = libreParaAhorrar - apartadoParaMeta

    /** Qué proporción de lo vendido quedó como ganancia; null si no hubo ventas. */
    val margen: Float? get() = if (ingresos <= 0.0) null else (ganancia / ingresos).toFloat()

    /** Sin un solo movimiento el resumen son ceros, y un tablero de ceros no dice nada. */
    val sinMovimientos: Boolean get() = ingresos == 0.0 && gastos == 0.0 && costos == 0.0
}

/**
 * HU-08. Qué tan cerca está el negocio de su meta de ahorro.
 *
 * [acumulado] es el dinero que el negocio ha dejado libre desde que se definió la meta: la
 * ganancia menos las obligaciones pendientes y menos la reinversión, con las mismas reglas de
 * HU-16. No es plata apartada en una cuenta —la app todavía no registra movimientos de ahorro—
 * sino lo que el negocio pudo haber guardado, que es lo que se puede medir con lo que hay.
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

/**
 * HU-09. Lo que se puede reinvertir de una ganancia.
 *
 * Un mes en pérdida no deja nada para reinvertir, así que el porcentaje no se aplica sobre un
 * número en rojo: sería "reinvertir plata que no existe".
 */
fun reinversionDe(ganancia: Double, porcentaje: Double): Double =
    if (ganancia <= 0.0) 0.0 else ganancia * porcentaje / 100.0

/**
 * HU-08. El dinero que queda libre para ahorrar: la ganancia menos lo que se debe y menos lo
 * que vuelve al negocio.
 *
 * Vive fuera de [ResumenFinanciero] porque la meta lo necesita sobre un periodo entero —desde
 * que se definió hasta hoy— y no solo sobre el mes en curso, y la regla tiene que ser la misma
 * en los dos casos.
 */
fun libreParaAhorrarDe(ganancia: Double, obligaciones: Double, porcentaje: Double): Double =
    ganancia - obligaciones - reinversionDe(ganancia, porcentaje)
