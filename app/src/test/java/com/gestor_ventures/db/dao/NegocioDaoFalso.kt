package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.NegocioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria y respeta el orden por nombre,
 * igual que la consulta real. Evita tener que levantar Room en cada prueba.
 */
class NegocioDaoFalso : NegocioDao {

    val negocios = MutableStateFlow<List<NegocioEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(negocio: NegocioEntity): Long {
        val id = siguienteId++
        negocios.value = negocios.value + negocio.copy(negocioId = id)
        return id
    }

    override suspend fun actualizar(negocio: NegocioEntity) {
        negocios.value = negocios.value.map {
            if (it.negocioId == negocio.negocioId) negocio else it
        }
    }

    override fun observar(negocioId: Long): Flow<NegocioEntity?> =
        negocios.map { lista -> lista.firstOrNull { it.negocioId == negocioId } }

    override suspend fun obtener(negocioId: Long): NegocioEntity? =
        negocios.value.firstOrNull { it.negocioId == negocioId }

    override fun observarDeUsuario(usuarioId: Long): Flow<List<NegocioEntity>> =
        negocios.map { lista ->
            lista.filter { it.usuarioId == usuarioId }.sortedBy { it.nombreNegocio.lowercase() }
        }

    override suspend fun contarDeUsuario(usuarioId: Long): Int =
        negocios.value.count { it.usuarioId == usuarioId }
}
