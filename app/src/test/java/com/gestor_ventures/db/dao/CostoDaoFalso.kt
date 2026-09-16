package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.CostoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria y filtra por rango igual que la
 * consulta real, bordes incluidos.
 */
class CostoDaoFalso : CostoDao {

    val costos = MutableStateFlow<List<CostoEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(costo: CostoEntity): Long {
        val id = siguienteId++
        costos.value = costos.value + costo.copy(costoId = id)
        return id
    }

    override suspend fun actualizar(costo: CostoEntity) {
        costos.value = costos.value.map { if (it.costoId == costo.costoId) costo else it }
    }

    override suspend fun eliminar(costo: CostoEntity) {
        costos.value = costos.value.filterNot { it.costoId == costo.costoId }
    }

    override suspend fun obtener(costoId: Long): CostoEntity? =
        costos.value.firstOrNull { it.costoId == costoId }

    override fun observarEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<List<CostoEntity>> = costos.map { lista ->
        lista.enRango(negocioId, desde, hasta)
            .sortedWith(compareByDescending<CostoEntity> { it.fechaRegistro }.thenByDescending { it.costoId })
    }

    override fun observarTotalEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<Double> = costos.map { lista ->
        lista.enRango(negocioId, desde, hasta).sumOf { it.montoCosto }
    }

    override fun observarTotalPorCategoriaEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<List<TotalPorCategoria>> = costos.map { lista ->
        lista.enRango(negocioId, desde, hasta)
            .groupBy { it.categoriaId }
            .map { (categoriaId, costos) ->
                TotalPorCategoria(categoriaId, costos.sumOf { it.montoCosto })
            }
            .sortedByDescending { it.total }
    }

    private fun List<CostoEntity>.enRango(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ) = filter {
        it.negocioId == negocioId &&
            !it.fechaRegistro.isBefore(desde) &&
            !it.fechaRegistro.isAfter(hasta)
    }
}
