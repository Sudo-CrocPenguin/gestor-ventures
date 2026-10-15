package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.GastoFijoEntity
import com.gestor_ventures.db.entity.MetaAhorroEntity
import com.gestor_ventures.db.enums.EstadoMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO de gastos fijos en memoria, para probar el repositorio sin levantar Room. */
class GastoFijoDaoFalso : GastoFijoDao {

    val gastos = MutableStateFlow<List<GastoFijoEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(gastoFijo: GastoFijoEntity): Long {
        val id = siguienteId++
        gastos.value = gastos.value + gastoFijo.copy(gastoFijoId = id)
        return id
    }

    override suspend fun actualizar(gastoFijo: GastoFijoEntity) {
        gastos.value = gastos.value.map {
            if (it.gastoFijoId == gastoFijo.gastoFijoId) gastoFijo else it
        }
    }

    override suspend fun eliminar(gastoFijo: GastoFijoEntity) {
        gastos.value = gastos.value.filterNot { it.gastoFijoId == gastoFijo.gastoFijoId }
    }

    override fun observarDeNegocio(negocioId: Long): Flow<List<GastoFijoEntity>> =
        gastos.map { lista -> lista.filter { it.negocioId == negocioId } }

    override suspend fun obtener(gastoFijoId: Long): GastoFijoEntity? =
        gastos.value.firstOrNull { it.gastoFijoId == gastoFijoId }

    override fun observarTotalDeNegocio(negocioId: Long): Flow<Double> =
        gastos.map { lista -> lista.filter { it.negocioId == negocioId }.sumOf { it.monto } }
}

/** DAO de metas de ahorro en memoria; la activa más reciente manda, como en la consulta real. */
class MetaAhorroDaoFalso : MetaAhorroDao {

    val metas = MutableStateFlow<List<MetaAhorroEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(meta: MetaAhorroEntity): Long {
        val id = siguienteId++
        metas.value = metas.value + meta.copy(metaAhorroId = id)
        return id
    }

    override suspend fun actualizar(meta: MetaAhorroEntity) {
        metas.value = metas.value.map {
            if (it.metaAhorroId == meta.metaAhorroId) meta else it
        }
    }

    override fun observarActivaDeNegocio(negocioId: Long): Flow<MetaAhorroEntity?> =
        metas.map { lista -> activa(lista, negocioId) }

    override suspend fun obtenerActivaDeNegocio(negocioId: Long): MetaAhorroEntity? =
        activa(metas.value, negocioId)

    override fun observarDeNegocio(negocioId: Long): Flow<List<MetaAhorroEntity>> =
        metas.map { lista ->
            lista.filter { it.negocioId == negocioId }.sortedByDescending { it.metaAhorroId }
        }

    private fun activa(lista: List<MetaAhorroEntity>, negocioId: Long) = lista
        .filter { it.negocioId == negocioId && it.estadoMeta == EstadoMeta.ACTIVA }
        .maxByOrNull { it.metaAhorroId }
}
