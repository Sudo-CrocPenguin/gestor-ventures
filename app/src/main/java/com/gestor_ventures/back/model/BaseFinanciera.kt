package com.gestor_ventures.back.model

import java.time.LocalDate

/** HU-06. Pago que se repite cada periodo: arriendo, servicios, internet. */
data class GastoFijo(
    val id: Long,
    val nombre: String,
    val monto: Double,
    val frecuencia: Frecuencia,
) {
    /**
     * HU-06/HU-16. Lo que este gasto le cuesta al negocio en un mes.
     *
     * Sin esto no se pueden sumar: un arriendo mensual de $800.000 y unos empaques semanales de
     * $50.000 no valen lo mismo, aunque los dos digan "gasto fijo".
     */
    val montoMensual: Double get() = monto * frecuencia.vecesAlAno / MesesPorAno
}

/**
 * HU-06. Cada cuánto se paga un gasto fijo.
 *
 * Las veces al año son la forma de llevarlos todos a la misma medida. Se usa el año y no el mes
 * porque las semanas no caben enteras en un mes: 52 semanas al año sí es exacto.
 */
enum class Frecuencia(val vecesAlAno: Double) {
    SEMANAL(52.0),
    QUINCENAL(26.0),
    MENSUAL(12.0),
    ANUAL(1.0),
}

/** HU-08. Cuánto quiere ahorrar el negocio y para cuándo. */
data class MetaAhorro(
    val id: Long,
    val montoObjetivo: Double,
    val fechaLimite: LocalDate,
    /** Desde cuándo se mide el progreso: lo de antes de la meta no cuenta para ella. */
    val fechaCreacion: LocalDate,
)

/** Reglas que debe cumplir la base financiera del negocio (HU-06, HU-08, HU-09). */
enum class ErrorBaseFinanciera {
    NombreGastoVacio,
    MontoNoPositivo,
    MetaSinMonto,
    FechaLimiteNoPosterior,
    PorcentajeFueraDeRango,
}

const val MesesPorAno = 12.0

/**
 * Promedio de días por mes. Evita saltos raros entre meses de 28 y de 31 días cuando hay que
 * medir "cuántos meses" hay en un rango de fechas cualquiera.
 */
const val DiasPorMes = 30.44
