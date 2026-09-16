package com.gestor_ventures.back.model

import java.time.LocalDate

/** HU-06. Pago que se repite cada periodo: arriendo, servicios, internet. */
data class GastoFijo(
    val id: Long,
    val nombre: String,
    val monto: Double,
    val frecuencia: Frecuencia,
)

/** HU-06. Cada cuánto se paga un gasto fijo. */
enum class Frecuencia { SEMANAL, QUINCENAL, MENSUAL, ANUAL }

/** HU-08. Cuánto quiere ahorrar el negocio y para cuándo. */
data class MetaAhorro(
    val id: Long,
    val montoObjetivo: Double,
    val fechaLimite: LocalDate,
)

/** Reglas que debe cumplir la base financiera del negocio (HU-06, HU-08, HU-09). */
enum class ErrorBaseFinanciera {
    NombreGastoVacio,
    MontoNoPositivo,
    MetaSinMonto,
    FechaLimiteNoPosterior,
    PorcentajeFueraDeRango,
}
