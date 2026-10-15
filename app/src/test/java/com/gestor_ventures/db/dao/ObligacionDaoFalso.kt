package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.ObligacionEntity
import com.gestor_ventures.db.enums.EstadoPago
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria y ordena igual que la consulta
 * real, con las pendientes primero y la que vence antes arriba.
 */
class ObligacionDaoFalso : ObligacionDao {

    val obligaciones = MutableStateFlow<List<ObligacionEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(obligacion: ObligacionEntity): Long {
        val id = siguienteId++
        obligaciones.value = obligaciones.value + obligacion.copy(obligacionId = id)
        return id
    }

    override suspend fun actualizar(obligacion: ObligacionEntity) {
        obligaciones.value = obligaciones.value.map {
            if (it.obligacionId == obligacion.obligacionId) obligacion else it
        }
    }

    override suspend fun eliminar(obligacion: ObligacionEntity) {
        obligaciones.value = obligaciones.value.filterNot {
            it.obligacionId == obligacion.obligacionId
        }
    }

    override suspend fun obtener(obligacionId: Long): ObligacionEntity? =
        obligaciones.value.firstOrNull { it.obligacionId == obligacionId }

    override fun observarDeNegocio(negocioId: Long): Flow<List<ObligacionEntity>> =
        obligaciones.map { lista ->
            lista.filter { it.negocioId == negocioId }
                .sortedWith(
                    compareBy<ObligacionEntity> { it.estadoPago == EstadoPago.PAGADA }
                        .thenBy { it.fechaVencimiento }
                        .thenBy { it.obligacionId },
                )
        }

    override fun observarProximasAVencer(
        negocioId: Long,
        hasta: LocalDate,
    ): Flow<List<ObligacionEntity>> = obligaciones.map { lista ->
        lista.filter {
            it.negocioId == negocioId &&
                it.estadoPago == EstadoPago.PENDIENTE &&
                !it.fechaVencimiento.isAfter(hasta)
        }.sortedWith(compareBy<ObligacionEntity> { it.fechaVencimiento }.thenBy { it.obligacionId })
    }

    override fun observarTotalPendiente(negocioId: Long): Flow<Double> =
        obligaciones.map { lista ->
            lista.filter { it.negocioId == negocioId && it.estadoPago == EstadoPago.PENDIENTE }
                .sumOf { it.monto }
        }
}
