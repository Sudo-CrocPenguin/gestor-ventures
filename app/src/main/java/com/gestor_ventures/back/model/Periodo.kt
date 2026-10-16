package com.gestor_ventures.back.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * El periodo que se está mirando. Vive aparte de los movimientos porque lo usan también el
 * historial de cajas (HU-23) y cualquier pantalla que vaya a resumir un rango.
 */

/** HU-17. El periodo que se está mirando, con los dos extremos incluidos. */
data class RangoFechas(val desde: LocalDate, val hasta: LocalDate) {
    val esUnSoloDia: Boolean get() = desde == hasta
}

/**
 * HU-17. Los periodos que se eligen de un toque.
 *
 * Todos terminan hoy y no al final del periodo: no existen movimientos futuros —ni una venta ni
 * un gasto se dejan registrar con fecha posterior a hoy— así que un rango que se estire hacia
 * adelante solo agregaría días vacíos.
 */
enum class PeriodoPredefinido { Hoy, Ayer, Semana, Mes }

/**
 * Resuelve el periodo contra el día en que se está parado.
 *
 * "Semana" y "Mes" son la semana y el mes en curso, no los últimos siete o treinta días: es lo
 * que una persona quiere decir cuando dice "cómo voy este mes".
 */
fun PeriodoPredefinido.rango(hoy: LocalDate): RangoFechas = when (this) {
    PeriodoPredefinido.Hoy -> RangoFechas(hoy, hoy)
    PeriodoPredefinido.Ayer -> RangoFechas(hoy.minusDays(1), hoy.minusDays(1))
    PeriodoPredefinido.Semana -> RangoFechas(hoy.with(DayOfWeek.MONDAY), hoy)
    PeriodoPredefinido.Mes -> RangoFechas(hoy.withDayOfMonth(1), hoy)
}

/**
 * HU-17 y HU-23. El periodo elegido: uno de los rápidos, o uno escrito a mano.
 *
 * Son dos cosas distintas y no un enum con un caso extra: un periodo rápido se resuelve contra
 * el día de hoy y uno personalizado ya trae sus fechas. Mezclarlos obligaría a tener un
 * "Personalizado" sin fechas dentro, que no significa nada.
 */
sealed interface SeleccionDePeriodo {

    /** Uno de los de un toque: hoy, ayer, esta semana, este mes. */
    data class Rapido(val periodo: PeriodoPredefinido) : SeleccionDePeriodo

    /**
     * Uno escrito a mano. El constructor es privado a propósito: se crea con [personalizado],
     * que es lo que impide armar un rango que termine antes de empezar.
     */
    data class Personalizado internal constructor(
        val desde: LocalDate,
        val hasta: LocalDate,
    ) : SeleccionDePeriodo
}

/**
 * HU-17. Arma un periodo escrito a mano, o devuelve null si las fechas vienen al revés.
 *
 * La regla vive acá y no en la pantalla para que no haya forma de pedirle a la base de datos un
 * rango imposible, venga de donde venga.
 */
fun personalizado(desde: LocalDate, hasta: LocalDate): SeleccionDePeriodo.Personalizado? =
    if (desde.isAfter(hasta)) null else SeleccionDePeriodo.Personalizado(desde, hasta)

/** Las fechas concretas del periodo elegido. */
fun SeleccionDePeriodo.rango(hoy: LocalDate): RangoFechas = when (this) {
    is SeleccionDePeriodo.Rapido -> periodo.rango(hoy)
    is SeleccionDePeriodo.Personalizado -> RangoFechas(desde, hasta)
}
