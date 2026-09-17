package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria e ignora inserciones con un
 * correo repetido, igual que el índice único de la tabla real.
 */
class UsuarioDaoFalso : UsuarioDao {

    val usuarios = MutableStateFlow<List<UsuarioEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(usuario: UsuarioEntity): Long {
        if (usuarios.value.any { it.correo == usuario.correo }) return -1
        val id = siguienteId++
        usuarios.value = usuarios.value + usuario.copy(usuarioId = id)
        return id
    }

    override suspend fun actualizar(usuario: UsuarioEntity) {
        usuarios.value = usuarios.value.map {
            if (it.usuarioId == usuario.usuarioId) usuario else it
        }
    }

    override fun observar(usuarioId: Long): Flow<UsuarioEntity?> =
        usuarios.map { lista -> lista.firstOrNull { it.usuarioId == usuarioId } }

    override suspend fun obtener(usuarioId: Long): UsuarioEntity? =
        usuarios.value.firstOrNull { it.usuarioId == usuarioId }

    override suspend fun obtenerPorCorreo(correo: String): UsuarioEntity? =
        usuarios.value.firstOrNull { it.correo == correo }
}
