package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.AlertaEntity
import com.gestor_ventures.db.enums.TipoAlerta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/** DAO de mentiras para las pruebas de JVM: guarda en memoria y ordena igual que la consulta real. */
class AlertaDaoFalso : AlertaDao {

    val alertas = MutableStateFlow<List<AlertaEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(alerta: AlertaEntity): Long {
        val id = siguienteId++
        alertas.value = alertas.value + alerta.copy(alertaId = id)
        return id
    }

    override fun observarDeNegocio(negocioId: Long): Flow<List<AlertaEntity>> =
        alertas.map { lista ->
            lista.filter { it.negocioId == negocioId }
                .sortedWith(
                    compareByDescending<AlertaEntity> { it.fechaGeneracion }
                        .thenByDescending { it.alertaId },
                )
        }

    override suspend fun contarDesde(
        negocioId: Long,
        tipo: TipoAlerta,
        desde: LocalDateTime,
    ): Int = alertas.value.count {
        it.negocioId == negocioId && it.tipoAlerta == tipo && !it.fechaGeneracion.isBefore(desde)
    }

    override suspend fun marcarLeida(alertaId: Long) {
        alertas.value = alertas.value.map {
            if (it.alertaId == alertaId) it.copy(leida = true) else it
        }
    }

    override suspend fun marcarTodasLeidas(negocioId: Long) {
        alertas.value = alertas.value.map {
            if (it.negocioId == negocioId) it.copy(leida = true) else it
        }
    }

    override suspend fun idsDeNegocios(): List<Long> =
        alertas.value.map { it.negocioId }.distinct()
}
