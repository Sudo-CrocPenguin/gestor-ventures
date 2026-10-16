package com.gestor_ventures.back.model

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

        override val fechaHora: LocalDateTime get() = gasto.fecha
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
