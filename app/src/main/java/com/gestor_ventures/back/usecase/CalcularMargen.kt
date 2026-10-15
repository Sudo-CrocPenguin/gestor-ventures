package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.Costo
import com.gestor_ventures.back.model.MargenProducto
import com.gestor_ventures.back.model.Venta
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject

/**
 * HU-13. Lo que deja cada producto en el mes: lo vendido menos lo que costó producirlo.
 *
 * Dos decisiones que vale la pena conocer antes de leer el resultado:
 *
 * - Solo aparecen los productos que tienen algún costo registrado. Un producto sin costos
 *   parecería tener 100% de margen, y eso no es un margen real sino una pregunta sin responder.
 * - Las ventas rápidas no entran: registran el total del día sin decir qué se vendió, así que no
 *   se le pueden atribuir a ningún producto.
 *
 * Los productos se cruzan por [claveDeProducto], porque en venta y costo se escriben a mano.
 */
class CalcularMargen @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val costoRepository: CostoRepository,
) {

    operator fun invoke(
        negocioId: Long,
        mes: YearMonth = costoRepository.mesActual(),
    ): Flow<List<MargenProducto>> = combine(
        ventaRepository.ventasDelMes(negocioId, mes),
        costoRepository.costosDelMes(negocioId, mes),
    ) { ventas, costos -> calcular(ventas, costos) }

    private fun calcular(ventas: List<Venta>, costos: List<Costo>): List<MargenProducto> {
        if (costos.isEmpty()) return emptyList()

        val costeadoPorProducto = costos.groupBy { claveDeProducto(it.productoServicio) }
        val vendidoPorProducto = ventas
            .filter { !it.productoServicio.isNullOrBlank() }
            .groupBy { claveDeProducto(it.productoServicio.orEmpty()) }

        return costeadoPorProducto.map { (clave, costosDelProducto) ->
            val ventasDelProducto = vendidoPorProducto[clave].orEmpty()
            MargenProducto(
                // El nombre que ve el usuario es como lo escribió él, no la clave normalizada.
                productoServicio = ventasDelProducto.firstOrNull()?.productoServicio
                    ?: costosDelProducto.first().productoServicio,
                vendido = ventasDelProducto.sumOf { it.monto },
                costeado = costosDelProducto.sumOf { it.monto },
            )
        }
            // Lo que está costando más de lo que deja va arriba: es lo que hay que revisar.
            .sortedBy { it.margen }
    }
}
