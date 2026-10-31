package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.MetodoPagoInactivoEntity
import com.gestor_ventures.db.enums.MetodoPago
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO de mentiras para las pruebas de JVM: guarda en memoria y respeta el índice único. */
class MetodoPagoDaoFalso : MetodoPagoDao {

    private val filas = MutableStateFlow<List<MetodoPagoInactivoEntity>>(emptyList())

    override fun observarInactivos(negocioId: Long): Flow<List<MetodoPago>> =
        filas.map { lista -> lista.filter { it.negocioId == negocioId }.map { it.metodoPago } }

    override suspend fun inactivos(negocioId: Long): List<MetodoPago> =
        filas.value.filter { it.negocioId == negocioId }.map { it.metodoPago }

    override suspend fun desactivar(metodo: MetodoPagoInactivoEntity) {
        if (filas.value.none { it.negocioId == metodo.negocioId && it.metodoPago == metodo.metodoPago }) {
            filas.value = filas.value + metodo
        }
    }

    override suspend fun activar(negocioId: Long, metodo: MetodoPago) {
        filas.value = filas.value.filterNot { it.negocioId == negocioId && it.metodoPago == metodo }
    }
}
