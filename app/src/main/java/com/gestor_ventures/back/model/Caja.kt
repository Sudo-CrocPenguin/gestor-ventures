package com.gestor_ventures.back.model

import java.time.LocalDateTime

/**
 * HU-19. La jornada de caja que está en curso.
 *
 * Guarda con cuánto efectivo se arrancó y desde qué hora, que es lo que hace falta para después
 * comparar contra lo que haya al cerrar (HU-21). Lo que entra y sale durante el día no se copia
 * acá: ya vive en ventas, gastos y costos, y se suma por el rango de la jornada (HU-20).
 */
data class Caja(
    val id: Long,
    val montoInicial: Double,
    val fechaHoraApertura: LocalDateTime,
)

/** Reglas que debe cumplir una apertura de caja (HU-19). */
enum class ErrorCaja {
    /** Cero sí se admite —se puede arrancar sin efectivo—, pero no se arranca debiendo. */
    MontoNegativo,

    /** Ya hay una jornada sin cerrar. Dos cajas abiertas se pelean los mismos movimientos. */
    YaHayCajaAbierta,
}
