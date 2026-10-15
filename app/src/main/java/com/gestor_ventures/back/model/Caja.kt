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

/**
 * HU-23. Una jornada que ya terminó.
 *
 * Es un modelo aparte de [Caja] y no la misma clase con campos nulables: una caja cerrada
 * siempre tiene hora de cierre y monto contado, y una abierta nunca. Mezclarlas obligaría a
 * preguntar "¿y si es nulo?" en cada pantalla que las use.
 */
data class CajaCerrada(
    val id: Long,
    val montoInicial: Double,
    val apertura: LocalDateTime,
    val cierre: LocalDateTime,
    val montoReal: Double,
    val nota: String? = null,
)

/**
 * HU-20 y HU-23. La cuenta del saldo de una jornada, en un solo lugar.
 *
 * La usan la caja en curso y el historial. Escrita dos veces, el día que alguien cambie una se
 * va a olvidar de la otra, y la app mostraría dos cifras distintas para lo mismo.
 */
fun saldoDeCaja(montoInicial: Double, ventas: Double, gastos: Double, costos: Double): Double =
    montoInicial + ventas - gastos - costos

/**
 * HU-22. Lo que se contó al cerrar contra lo que el sistema esperaba.
 *
 * La diferencia es real menos esperado, como lo pide la historia: positiva significa que sobró
 * plata y negativa que faltó. No se guarda en la tabla porque se puede volver a calcular en
 * cualquier momento a partir de la jornada, y un dato copiado es un dato que se desactualiza.
 */
data class DiferenciaDeCaja(
    val esperado: Double,
    val real: Double,
) {
    val diferencia: Double get() = real - esperado

    /** Se compara con una tolerancia de un peso: los centavos de los Double no son un faltante. */
    val cuadra: Boolean get() = kotlin.math.abs(diferencia) < 1.0

    val sobra: Boolean get() = !cuadra && diferencia > 0

    val falta: Boolean get() = !cuadra && diferencia < 0
}

/** Reglas que debe cumplir abrir o cerrar una caja (HU-19, HU-21). */
enum class ErrorCaja {
    /** Cero sí se admite —se puede arrancar sin efectivo—, pero no se arranca debiendo. */
    MontoNegativo,

    /** Ya hay una jornada sin cerrar. Dos cajas abiertas se pelean los mismos movimientos. */
    YaHayCajaAbierta,

    /** Se intentó cerrar sin tener nada abierto: la jornada ya se cerró desde otro lado. */
    NoHayCajaAbierta,
}
