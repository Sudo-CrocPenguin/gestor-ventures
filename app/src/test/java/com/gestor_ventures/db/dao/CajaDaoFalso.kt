package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.CajaEntity
import com.gestor_ventures.db.enums.EstadoCaja
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria y filtra igual que la consulta
 * real, incluido el detalle de devolver la jornada más reciente cuando hubiera varias abiertas.
 */
class CajaDaoFalso : CajaDao {

    val cajas = MutableStateFlow<List<CajaEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(caja: CajaEntity): Long {
        val id = siguienteId++
        cajas.value = cajas.value + caja.copy(cajaId = id)
        return id
    }

    override suspend fun obtenerAbierta(negocioId: Long): CajaEntity? =
        abiertaDe(cajas.value, negocioId)

    override fun observarAbierta(negocioId: Long): Flow<CajaEntity?> =
        cajas.map { lista -> abiertaDe(lista, negocioId) }

    private fun abiertaDe(lista: List<CajaEntity>, negocioId: Long): CajaEntity? =
        lista.filter { it.negocioId == negocioId && it.estadoCaja == EstadoCaja.ABIERTA }
            .maxWithOrNull(compareBy({ it.fechaHoraApertura }, { it.cajaId }))
}
