package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.VentaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria y filtra por rango igual que la
 * consulta real, bordes incluidos. Evita tener que levantar Room en cada prueba.
 */
class VentaDaoFalso : VentaDao {

    val ventas = MutableStateFlow<List<VentaEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(venta: VentaEntity): Long {
        val id = siguienteId++
        ventas.value = ventas.value + venta.copy(ventaId = id)
        return id
    }

    override suspend fun actualizar(venta: VentaEntity) {
        ventas.value = ventas.value.map { if (it.ventaId == venta.ventaId) venta else it }
    }

    override suspend fun eliminar(venta: VentaEntity) {
        ventas.value = ventas.value.filterNot { it.ventaId == venta.ventaId }
    }

    override suspend fun obtener(ventaId: Long): VentaEntity? =
        ventas.value.firstOrNull { it.ventaId == ventaId }

    override fun observarEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<List<VentaEntity>> = ventas.map { lista ->
        lista.enRango(negocioId, desde, hasta)
            .sortedWith(compareByDescending<VentaEntity> { it.fechaHora }.thenByDescending { it.ventaId })
    }

    override fun observarTotalEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<Double> = ventas.map { lista ->
        lista.enRango(negocioId, desde, hasta).sumOf { it.monto }
    }

    override fun contarEntre(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ): Flow<Int> = ventas.map { lista -> lista.enRango(negocioId, desde, hasta).size }

    private fun List<VentaEntity>.enRango(
        negocioId: Long,
        desde: LocalDateTime,
        hasta: LocalDateTime,
    ) = filter {
        it.negocioId == negocioId && !it.fechaHora.isBefore(desde) && !it.fechaHora.isAfter(hasta)
    }
}
