package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.ClienteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria y ordena igual que la consulta
 * real, mayúsculas incluidas. Evita tener que levantar Room en cada prueba.
 */
class ClienteDaoFalso : ClienteDao {

    val clientes = MutableStateFlow<List<ClienteEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(cliente: ClienteEntity): Long {
        val id = siguienteId++
        clientes.value = clientes.value + cliente.copy(clienteId = id)
        return id
    }

    override suspend fun actualizar(cliente: ClienteEntity) {
        clientes.value = clientes.value.map {
            if (it.clienteId == cliente.clienteId) cliente else it
        }
    }

    override suspend fun eliminar(cliente: ClienteEntity) {
        clientes.value = clientes.value.filterNot { it.clienteId == cliente.clienteId }
    }

    override suspend fun obtener(clienteId: Long): ClienteEntity? =
        clientes.value.firstOrNull { it.clienteId == clienteId }

    override fun observarDeNegocio(negocioId: Long): Flow<List<ClienteEntity>> =
        clientes.map { lista ->
            lista.filter { it.negocioId == negocioId }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nombre })
        }

    override suspend fun existeConTelefono(
        negocioId: Long,
        telefono: String,
        exceptoId: Long,
    ): Int = clientes.value.count {
        it.negocioId == negocioId && it.telefono == telefono && it.clienteId != exceptoId
    }

    override suspend fun existeConCorreo(negocioId: Long, correo: String, exceptoId: Long): Int =
        clientes.value.count {
            it.negocioId == negocioId &&
                it.correo.equals(correo, ignoreCase = true) &&
                it.clienteId != exceptoId
        }
}
