package com.gestor_ventures.back.model

import java.time.LocalDate

/**
 * HU-07. Un compromiso que el negocio ya adquirió: un préstamo, una cuota, un pago recurrente.
 *
 * No es un gasto: el gasto ya salió de la caja, la obligación todavía no. Por eso se descuenta
 * del dinero disponible (HU-16) en vez de sumarse a los egresos del mes.
 */
data class Obligacion(
    val id: Long,
    val nombre: String,
    val monto: Double,
    val fechaVencimiento: LocalDate,
    val pagada: Boolean,
) {
    /** Se pasó de fecha y sigue sin pagarse: es la más urgente de todas. */
    fun estaVencida(hoy: LocalDate): Boolean = !pagada && fechaVencimiento.isBefore(hoy)

    /** Cuántos días faltan para pagarla; negativo si ya se venció. */
    fun diasParaVencer(hoy: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaVencimiento)
}

/** Reglas que debe cumplir una obligación (HU-07). */
enum class ErrorObligacion {
    NombreVacio,
    MontoNoPositivo,
}
