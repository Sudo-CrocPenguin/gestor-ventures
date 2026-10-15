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

/**
 * HU-20. Final del periodo de una caja que sigue abierta.
 *
 * No se usa "ahora" porque un `Flow` no se entera de que el reloj avanzó: la consulta se arma
 * una sola vez y una venta registrada un minuto después quedaría fuera del rango, con el saldo
 * congelado. Un tope lejano no deja entrar nada que no haya pasado ya, porque ninguna venta,
 * gasto ni costo puede quedar con fecha futura: las tres validaciones lo impiden.
 */
val JornadaSinCerrar: LocalDateTime = LocalDateTime.of(9999, 12, 31, 23, 59, 59)

/** Reglas que debe cumplir una apertura de caja (HU-19). */
enum class ErrorCaja {
    /** Cero sí se admite —se puede arrancar sin efectivo—, pero no se arranca debiendo. */
    MontoNegativo,

    /** Ya hay una jornada sin cerrar. Dos cajas abiertas se pelean los mismos movimientos. */
    YaHayCajaAbierta,
}
