package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.CategoriaEntity
import com.gestor_ventures.db.enums.TipoCategoria
import kotlinx.coroutines.flow.Flow

/**
 * HU-15. Categorías propias del negocio para clasificar gastos y costos.
 *
 * Cada negocio tiene las suyas: "Insumos" de una repostería no es la misma cuenta que la de
 * otro negocio, aunque se llame igual.
 *
 * Borrar una categoría no borra lo que estaba clasificado con ella: la llave foránea está en
 * SET_NULL, así que esos gastos y costos quedan sin categoría pero siguen contando en las
 * finanzas. Perder un gasto por borrar una etiqueta sería mucho peor.
 */
@Dao
interface CategoriaDao {

    @Insert
    suspend fun insertar(categoria: CategoriaEntity): Long

    @Update
    suspend fun actualizar(categoria: CategoriaEntity)

    @Delete
    suspend fun eliminar(categoria: CategoriaEntity)

    @Query("SELECT * FROM categorias WHERE categoria_id = :categoriaId")
    suspend fun obtener(categoriaId: Long): CategoriaEntity?

    /** Todas las del negocio, ordenadas por nombre para que la lista no baile. */
    @Query(
        """
        SELECT * FROM categorias
        WHERE negocio_id = :negocioId
        ORDER BY nombre_categoria COLLATE NOCASE ASC
        """,
    )
    fun observarDeNegocio(negocioId: Long): Flow<List<CategoriaEntity>>

    /** Las de un solo tipo: es lo que necesita el selector del formulario de gasto o de costo. */
    @Query(
        """
        SELECT * FROM categorias
        WHERE negocio_id = :negocioId AND tipo_categoria = :tipo
        ORDER BY nombre_categoria COLLATE NOCASE ASC
        """,
    )
    fun observarDeNegocioPorTipo(
        negocioId: Long,
        tipo: TipoCategoria,
    ): Flow<List<CategoriaEntity>>

    /**
     * Si el negocio ya tiene una categoría con ese nombre y tipo. Sirve para no dejar crear
     * "Insumos" dos veces, que después nadie sabe en cuál de las dos clasificó qué.
     */
    @Query(
        """
        SELECT COUNT(*) FROM categorias
        WHERE negocio_id = :negocioId
          AND tipo_categoria = :tipo
          AND nombre_categoria = :nombre COLLATE NOCASE
          AND categoria_id != :exceptoId
        """,
    )
    suspend fun existeConNombre(
        negocioId: Long,
        tipo: TipoCategoria,
        nombre: String,
        exceptoId: Long = 0,
    ): Int
}
