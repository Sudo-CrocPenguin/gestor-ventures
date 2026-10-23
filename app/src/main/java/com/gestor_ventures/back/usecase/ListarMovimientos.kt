package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.FiltroMovimientos
import com.gestor_ventures.back.model.Movimiento
import com.gestor_ventures.back.model.RangoFechas
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * HU-17. El historial: todo lo que movió plata en un periodo, junto y en orden.
 *
 * Vive en `usecase` porque cruza tres repositorios y ninguno es su dueño, igual que
 * [CalcularResumenFinanciero]. Cada uno sigue siendo la única puerta a su tabla.
 *
 * El orden es por fecha, de lo más reciente a lo más antiguo, y desempata por tipo y por id:
 * dos movimientos del mismo instante saldrían en cualquier orden si solo se mirara la fecha, y
 * una lista que cambia de orden sola es una lista en la que no se puede tocar nada con
 * confianza.
 */
class ListarMovimientos @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val gastoRepository: GastoRepository,
    private val costoRepository: CostoRepository,
    private val reloj: Reloj,
) {

    operator fun invoke(
        negocioId: Long,
        rango: RangoFechas,
        filtro: FiltroMovimientos = FiltroMovimientos.Todos,
    ): Flow<List<Movimiento>> = combine(
        ventaRepository.ventasEntre(negocioId, rango.desde, rango.hasta),
        gastoRepository.gastosEntre(negocioId, rango.desde, rango.hasta),
        costoRepository.costosEntre(negocioId, rango.desde, rango.hasta),
    ) { ventas, gastos, costos ->
        val movimientos = buildList {
            if (filtro != FiltroMovimientos.Salidas) {
                ventas.forEach { add(Movimiento.DeVenta(it)) }
            }
            if (filtro != FiltroMovimientos.Ventas) {
                gastos.forEach { add(Movimiento.DeGasto(it)) }
                costos.forEach { add(Movimiento.DeCosto(it)) }
            }
        }
        movimientos.sortedWith(MasRecientePrimero)
    }

    /** El día de hoy según el reloj de la app, para resolver los periodos predefinidos. */
    fun hoy(): LocalDate = reloj.ahora().toLocalDate()

    private companion object {
        val MasRecientePrimero = compareByDescending<Movimiento> { it.fechaHora }
            .thenBy { it::class.simpleName }
            .thenByDescending { it.id }
    }
}
