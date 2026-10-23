package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.ErrorCategoria
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.db.dao.CategoriaDao
import com.gestor_ventures.db.entity.CategoriaEntity
import com.gestor_ventures.db.enums.TipoCategoria as TipoCategoriaDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Tope de caracteres del nombre, como quedó especificado en HU-15. */
private const val MaxCaracteresNombre = 50

/**
 * HU-15. Categorías con las que el negocio clasifica sus gastos y sus costos.
 *
 * La regla que no es obvia es la del nombre repetido: dos categorías "Insumos" en el mismo
 * negocio parten el resumen en dos filas que deberían ser una, y después nadie sabe en cuál
 * de las dos clasificó qué.
 */
@Singleton
class CategoriaRepository @Inject constructor(
    private val categoriaDao: CategoriaDao,
) {

    fun categoriasDeNegocio(negocioId: Long): Flow<List<Categoria>> =
        categoriaDao.observarDeNegocio(negocioId).map { lista -> lista.map(::aCategoria) }

    /** Las de un solo tipo: es lo que ofrece el selector del formulario de gasto o de costo. */
    fun categoriasDeNegocio(negocioId: Long, tipo: TipoCategoria): Flow<List<Categoria>> =
        categoriaDao.observarDeNegocioPorTipo(negocioId, tipo.aDb())
            .map { lista -> lista.map(::aCategoria) }

    /** HU-15. Crea la categoría si el nombre sirve y no está repetido. */
    suspend fun crearCategoria(
        negocioId: Long,
        nombre: String,
        tipo: TipoCategoria,
    ): ErrorCategoria? {
        val nombreLimpio = nombre.trim()
        validar(negocioId, nombreLimpio, tipo)?.let { return it }

        categoriaDao.insertar(
            CategoriaEntity(
                negocioId = negocioId,
                nombreCategoria = nombreLimpio,
                tipoCategoria = tipo.aDb(),
            ),
        )
        return null
    }

    /**
     * HU-15. Corrige el nombre de una categoría. El tipo no se cambia: pasar una categoría de
     * gasto a costo movería de lado todo lo que ya estaba clasificado con ella, y el margen
     * cambiaría sin que nadie hubiera tocado una venta.
     *
     * Si la categoría ya no existe no hace nada: se pudo borrar desde otra pantalla.
     */
    suspend fun renombrarCategoria(categoriaId: Long, nombre: String): ErrorCategoria? {
        val actual = categoriaDao.obtener(categoriaId) ?: return null
        val nombreLimpio = nombre.trim()

        validar(
            negocioId = actual.negocioId,
            nombre = nombreLimpio,
            tipo = actual.tipoCategoria.aDominio(),
            // Ella misma no cuenta como repetida: corregirle una tilde no puede rebotar.
            exceptoId = categoriaId,
        )?.let { return it }

        categoriaDao.actualizar(actual.copy(nombreCategoria = nombreLimpio))
        return null
    }

    /**
     * HU-15. Borra la categoría. Lo que estaba clasificado con ella no se borra: queda sin
     * categoría y sigue contando en las finanzas (la llave foránea está en SET_NULL). Perder un
     * gasto por borrar una etiqueta sería mucho peor que perder la etiqueta.
     */
    suspend fun eliminarCategoria(categoriaId: Long) {
        categoriaDao.obtener(categoriaId)?.let { categoriaDao.eliminar(it) }
    }

    private suspend fun validar(
        negocioId: Long,
        nombre: String,
        tipo: TipoCategoria,
        exceptoId: Long = 0,
    ): ErrorCategoria? = when {
        nombre.isEmpty() -> ErrorCategoria.NombreVacio
        nombre.length > MaxCaracteresNombre -> ErrorCategoria.NombreMuyLargo
        categoriaDao.existeConNombre(negocioId, tipo.aDb(), nombre, exceptoId) > 0 ->
            ErrorCategoria.NombreRepetido

        else -> null
    }

    private fun aCategoria(entidad: CategoriaEntity) = Categoria(
        id = entidad.categoriaId,
        nombre = entidad.nombreCategoria,
        tipo = entidad.tipoCategoria.aDominio(),
    )
}

private fun TipoCategoria.aDb(): TipoCategoriaDb = when (this) {
    TipoCategoria.GASTO -> TipoCategoriaDb.GASTO
    TipoCategoria.COSTO -> TipoCategoriaDb.COSTO
}

private fun TipoCategoriaDb.aDominio(): TipoCategoria = when (this) {
    TipoCategoriaDb.GASTO -> TipoCategoria.GASTO
    TipoCategoriaDb.COSTO -> TipoCategoria.COSTO
}
