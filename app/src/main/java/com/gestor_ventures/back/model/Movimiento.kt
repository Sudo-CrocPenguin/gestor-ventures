package com.gestor_ventures.back.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-17. Cualquier cosa que movió plata en el negocio, para poder mirarlas juntas.
 *
 * Envuelve el registro original en vez de aplanarlo a campos sueltos: el historial necesita
 * mostrar cada tipo a su manera —una venta dice cómo pagaron, un gasto dice en qué categoría
 * quedó— y con una sola clase de campos nulos la pantalla terminaría adivinando.
 */
sealed interface Movimiento {

    val id: Long
    val monto: Double
    val fechaHora: LocalDateTime

    /** Entra plata o sale. Es lo único que el historial necesita saber sin abrir el registro. */
    val entra: Boolean

    data class DeVenta(val venta: Venta) : Movimiento {
        override val id: Long get() = venta.id
        override val monto: Double get() = venta.monto
        override val fechaHora: LocalDateTime get() = venta.fechaHora
        override val entra: Boolean get() = true
    }

    data class DeGasto(val gasto: Gasto) : Movimiento {
        override val id: Long get() = gasto.id
        override val monto: Double get() = gasto.monto

        /**
         * Un gasto se registra por día, sin hora. Al mezclarlo con ventas queda al comienzo de
         * su día, así que dentro de una misma fecha las ventas aparecen por encima.
         */
        override val fechaHora: LocalDateTime get() = gasto.fecha.atStartOfDay()
        override val entra: Boolean get() = false
    }

    data class DeCosto(val costo: Costo) : Movimiento {
        override val id: Long get() = costo.id
        override val monto: Double get() = costo.monto
        override val fechaHora: LocalDateTime get() = costo.fecha
        override val entra: Boolean get() = false
    }
}

/**
 * HU-17. Qué movimientos mirar.
 *
 * [Salidas] junta gastos y costos porque la pregunta que responde es "¿en qué se me fue la
 * plata?", y para esa pregunta los dos son lo mismo. El detalle de cuál es cuál sigue en las
 * secciones de Finanzas.
 */
enum class FiltroMovimientos { Todos, Ventas, Salidas }

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
