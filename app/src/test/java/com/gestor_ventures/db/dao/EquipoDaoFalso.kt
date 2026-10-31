package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.MiembroEquipoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO de mentiras para las pruebas de JVM: guarda en memoria y ordena como la consulta real. */
class EquipoDaoFalso : EquipoDao {

    private val miembros = MutableStateFlow<List<MiembroEquipoEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(miembro: MiembroEquipoEntity): Long {
        val id = siguienteId++
        miembros.value = miembros.value + miembro.copy(miembroEquipoId = id)
        return id
    }

    override fun observarDeNegocio(negocioId: Long): Flow<List<MiembroEquipoEntity>> =
        miembros.map { lista ->
            lista.filter { it.negocioId == negocioId }.sortedByDescending { it.fechaInvitacion }
        }

    override suspend fun contarPorCorreo(negocioId: Long, correo: String): Int =
        miembros.value.count { it.negocioId == negocioId && it.correo == correo }

    override suspend fun contarInvitaciones(correo: String): Int =
        miembros.value.count { it.correo == correo }

    override suspend fun aceptarInvitaciones(correo: String) {
        miembros.value = miembros.value.map {
            if (it.correo == correo) it.copy(estadoMiembro = com.gestor_ventures.db.enums.EstadoMiembro.ACTIVO) else it
        }
    }

    override suspend fun eliminar(miembroId: Long) {
        miembros.value = miembros.value.filterNot { it.miembroEquipoId == miembroId }
    }
}
