package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.ComparacionDeGastos
import com.gestor_ventures.back.model.RangoFechas
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.model.VariacionCategoria
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * HU-37. Compara lo gastado por categoría este mes contra el mes anterior.
 *
 * La cuenta la hace la app y no el asistente: así las cifras son exactas, y el asistente solo
 * tiene que explicarlas.
 */
class CompararGastosPorCategoria @Inject constructor(
    private val gastoRepository: GastoRepository,
    private val costoRepository: CostoRepository,
    private val categoriaRepository: CategoriaRepository,
) {

    /**
     * Compara este mes hasta hoy contra los mismos días del mes anterior: el 1 al 22 de
     * octubre contra el 1 al 22 de septiembre. Comparar medio mes contra un mes entero haría
     * parecer que todo bajó.
     *
     * Si el mes anterior es más corto (hoy es 31 y el anterior tuvo 30 días), se toma completo.
     */
    suspend operator fun invoke(negocioId: Long, hoy: LocalDate): ComparacionDeGastos {
        val actual = RangoFechas(hoy.withDayOfMonth(1), hoy)
        val inicioAnterior = actual.desde.minusMonths(1)
        val finAnterior = inicioAnterior.plusDays(minOf(hoy.dayOfMonth, inicioAnterior.lengthOfMonth()).toLong() - 1)
        val anterior = RangoFechas(inicioAnterior, finAnterior)

        val nombres = categoriaRepository.categoriasDeNegocio(negocioId).first().associateBy { it.id }
        val totalesActuales = totalesPorCategoria(negocioId, actual)
        val totalesAnteriores = totalesPorCategoria(negocioId, anterior)

        val variaciones = (totalesActuales.keys + totalesAnteriores.keys).map { clave ->
            VariacionCategoria(
                nombre = clave.categoriaId?.let { nombres[it]?.nombre } ?: SinCategoria,
                tipo = clave.tipo,
                actual = totalesActuales[clave] ?: 0.0,
                anterior = totalesAnteriores[clave] ?: 0.0,
            )
        }.sortedByDescending { it.diferencia }

        return ComparacionDeGastos(actual = actual, anterior = anterior, variaciones = variaciones)
    }

    /**
     * Lo gastado en cada categoría dentro del rango. Gastos y costos van por separado aunque
     * no tengan categoría: "sin categoría" en gastos y en costos son dos montones distintos.
     */
    private suspend fun totalesPorCategoria(negocioId: Long, rango: RangoFechas): Map<Clave, Double> {
        val gastos = gastoRepository.gastosEntre(negocioId, rango.desde, rango.hasta).first()
            .groupBy { Clave(it.categoriaId, TipoCategoria.GASTO) }
            .mapValues { (_, lista) -> lista.sumOf { it.monto } }
        val costos = costoRepository.costosEntre(negocioId, rango.desde, rango.hasta).first()
            .groupBy { Clave(it.categoriaId, TipoCategoria.COSTO) }
            .mapValues { (_, lista) -> lista.sumOf { it.monto } }
        return gastos + costos
    }

    private data class Clave(val categoriaId: Long?, val tipo: TipoCategoria)

    private companion object {
        const val SinCategoria = "Sin categoría"
    }
}
