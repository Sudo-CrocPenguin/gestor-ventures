package com.gestor_ventures.front.ui.inicio

import com.gestor_ventures.back.model.Venta

/** Franjas en las que se parte el día para la mini gráfica: tres horas cada una. */
const val FranjasDelDia = 8

private const val HorasPorFranja = 24 / FranjasDelDia

/**
 * Reparte las ventas del día en franjas horarias para la mini gráfica del resumen.
 *
 * Devuelve la lista vacía si no hay nada que dibujar: una línea plana en cero haría creer que
 * el negocio vendió cero todo el día, cuando lo que pasa es que todavía no hay datos.
 */
fun tendenciaPorFranja(ventas: List<Venta>): List<Double> {
    if (ventas.isEmpty()) return emptyList()

    val franjas = DoubleArray(FranjasDelDia)
    ventas.forEach { venta ->
        val franja = (venta.fechaHora.hour / HorasPorFranja).coerceIn(0, FranjasDelDia - 1)
        franjas[franja] += venta.monto
    }
    return franjas.toList()
}
