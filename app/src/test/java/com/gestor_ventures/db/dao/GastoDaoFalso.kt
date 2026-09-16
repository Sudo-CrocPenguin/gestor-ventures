package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.GastoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria y filtra por rango igual que la
 * consulta real, bordes incluidos.
 */
class GastoDaoFalso : GastoDao {

    val gastos = MutableStateFlow<List<GastoEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(gasto: GastoEntity): Long {
        val id = siguienteId++
        gastos.value = gastos.value + gasto.copy(gastoId = id)
        return id
    }

    override suspend fun actualizar(gasto: GastoEntity) {
        gastos.value = gastos.value.map { if (it.gastoId == gasto.gastoId) gasto else it }
    }

    override suspend fun eliminar(gasto: GastoEntity) {
        gastos.value = gastos.value.filterNot { it.gastoId == gasto.gastoId }
    }

    override suspend fun obtener(gastoId: Long): GastoEntity? =
        gastos.value.firstOrNull { it.gastoId == gastoId }

    override fun observarEntre(
        negocioId: Long,
        desde: LocalDate,
        hasta: LocalDate,
    ): Flow<List<GastoEntity>> = gastos.map { lista ->
        lista.enRango(negocioId, desde, hasta)
            .sortedWith(compareByDescending<GastoEntity> { it.fecha }.thenByDescending { it.gastoId })
    }

    override fun observarTotalEntre(
        negocioId: Long,
        desde: LocalDate,
        hasta: LocalDate,
    ): Flow<Double> = gastos.map { lista ->
        lista.enRango(negocioId, desde, hasta).sumOf { it.monto }
    }

    override fun observarTotalPorCategoriaEntre(
        negocioId: Long,
        desde: LocalDate,
        hasta: LocalDate,
    ): Flow<List<TotalPorCategoria>> = gastos.map { lista ->
        lista.enRango(negocioId, desde, hasta)
            .groupBy { it.categoriaId }
            .map { (categoriaId, gastos) -> TotalPorCategoria(categoriaId, gastos.sumOf { it.monto }) }
            .sortedByDescending { it.total }
    }

    private fun List<GastoEntity>.enRango(
        negocioId: Long,
        desde: LocalDate,
        hasta: LocalDate,
    ) = filter {
        it.negocioId == negocioId && !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta)
    }
}
