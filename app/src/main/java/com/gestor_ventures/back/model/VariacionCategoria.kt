package com.gestor_ventures.back.model

/**
 * HU-37. Cuánto cambió lo gastado en una categoría (HU-15) de un periodo al otro.
 *
 * [tipo] dice si la categoría es de gastos o de costos de producción: las dos se comparan,
 * porque las dos son plata que sale.
 */
data class VariacionCategoria(
    val nombre: String,
    val tipo: TipoCategoria,
    val actual: Double,
    val anterior: Double,
) {
    /** Positiva si subió, negativa si bajó. */
    val diferencia: Double get() = actual - anterior

    /**
     * Cuánto subió o bajó, en porcentaje. Null si antes no había nada: de cero a algo no es
     * un porcentaje, es una categoría nueva.
     */
    val porcentaje: Double? get() = if (anterior <= 0.0) null else diferencia / anterior * 100
}

/** HU-37. La comparación completa: los dos periodos y cada categoría, de lo que más subió a lo que más bajó. */
data class ComparacionDeGastos(
    val actual: RangoFechas,
    val anterior: RangoFechas,
    val variaciones: List<VariacionCategoria>,
)
