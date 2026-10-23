package com.gestor_ventures.back.model

data class VariacionCategoria(
    val nombre: String,
    val tipo: TipoCategoria,
    val actual: Double,
    val anterior: Double,
) {
    val diferencia: Double get() = actual - anterior

    val porcentaje: Double? get() = if (anterior <= 0.0) null else diferencia / anterior * 100
}

data class ComparacionDeGastos(
    val actual: RangoFechas,
    val anterior: RangoFechas,
    val variaciones: List<VariacionCategoria>,
)
