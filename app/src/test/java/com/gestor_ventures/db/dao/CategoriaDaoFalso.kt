package com.gestor_ventures.db.dao

import com.gestor_ventures.db.entity.CategoriaEntity
import com.gestor_ventures.db.enums.TipoCategoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * DAO de mentiras para las pruebas de JVM: guarda en memoria y compara los nombres ignorando
 * mayúsculas, igual que el COLLATE NOCASE de la consulta real.
 */
class CategoriaDaoFalso : CategoriaDao {

    val categorias = MutableStateFlow<List<CategoriaEntity>>(emptyList())
    private var siguienteId = 1L

    override suspend fun insertar(categoria: CategoriaEntity): Long {
        val id = siguienteId++
        categorias.value = categorias.value + categoria.copy(categoriaId = id)
        return id
    }

    override suspend fun actualizar(categoria: CategoriaEntity) {
        categorias.value = categorias.value.map {
            if (it.categoriaId == categoria.categoriaId) categoria else it
        }
    }

    override suspend fun eliminar(categoria: CategoriaEntity) {
        categorias.value = categorias.value.filterNot { it.categoriaId == categoria.categoriaId }
    }

    override suspend fun obtener(categoriaId: Long): CategoriaEntity? =
        categorias.value.firstOrNull { it.categoriaId == categoriaId }

    override fun observarDeNegocio(negocioId: Long): Flow<List<CategoriaEntity>> =
        categorias.map { lista -> lista.filter { it.negocioId == negocioId }.ordenadas() }

    override fun observarDeNegocioPorTipo(
        negocioId: Long,
        tipo: TipoCategoria,
    ): Flow<List<CategoriaEntity>> = categorias.map { lista ->
        lista.filter { it.negocioId == negocioId && it.tipoCategoria == tipo }.ordenadas()
    }

    override suspend fun existeConNombre(
        negocioId: Long,
        tipo: TipoCategoria,
        nombre: String,
        exceptoId: Long,
    ): Int = categorias.value.count {
        it.negocioId == negocioId &&
            it.tipoCategoria == tipo &&
            it.nombreCategoria.equals(nombre, ignoreCase = true) &&
            it.categoriaId != exceptoId
    }

    private fun List<CategoriaEntity>.ordenadas() = sortedBy { it.nombreCategoria.lowercase() }
}
