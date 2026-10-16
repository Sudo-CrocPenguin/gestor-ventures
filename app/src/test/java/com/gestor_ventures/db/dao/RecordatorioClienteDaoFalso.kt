package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.RecordatorioClienteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO de mentiras para las pruebas de JVM: guarda en memoria y ordena igual que la consulta real. */
class RecordatorioClienteDaoFalso : RecordatorioClienteDao {

    val recordatorios = MutableStateFlow<List<RecordatorioClienteEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(recordatorio: RecordatorioClienteEntity): Long {
        val id = siguienteId++
        recordatorios.value = recordatorios.value + recordatorio.copy(recordatorioClienteId = id)
        return id
    }

    override fun observarDeCliente(clienteId: Long): Flow<List<RecordatorioClienteEntity>> =
        recordatorios.map { lista ->
            lista.filter { it.clienteId == clienteId }
                .sortedWith(
                    compareByDescending<RecordatorioClienteEntity> { it.fechaEnvio }
                        .thenByDescending { it.recordatorioClienteId },
                )
        }
}
