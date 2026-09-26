package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gestor_ventures.db.entity.ClienteEntity
import kotlinx.coroutines.flow.Flow

/**
 * HU-29. Los clientes del negocio: quién le compra y cómo ubicarlo.
 *
 * Cada negocio tiene los suyos. Dos negocios del mismo dueño pueden tener a la misma persona
 * como cliente y son dos fichas distintas: lo que le compró a uno no es asunto del otro.
 *
 * El teléfono y el correo no se pueden repetir dentro de un mismo negocio, pero sí pueden ir
 * vacíos: a mucha gente se le vende sin pedirle nada más que el nombre. Por eso los duplicados
 * se consultan con [existeConTelefono] y [existeConCorreo] en vez de dejar que reviente el
 * índice único —así el repositorio puede devolver un error que la pantalla sepa explicar.
 */
@Dao
interface ClienteDao {

    @Insert
    suspend fun insertar(cliente: ClienteEntity): Long

    @Update
    suspend fun actualizar(cliente: ClienteEntity)

    @Delete
    suspend fun eliminar(cliente: ClienteEntity)

    @Query("SELECT * FROM clientes WHERE cliente_id = :clienteId")
    suspend fun obtener(clienteId: Long): ClienteEntity?

    /**
     * Todos los del negocio, por nombre. El COLLATE NOCASE es para que "ana" y "Ana" queden
     * juntas: quien escribe rápido no usa mayúsculas, y la lista no tiene por qué delatarlo.
     */
    @Query(
        """
        SELECT * FROM clientes
        WHERE negocio_id = :negocioId
        ORDER BY nombre COLLATE NOCASE ASC, cliente_id ASC
        """,
    )
    fun observarDeNegocio(negocioId: Long): Flow<List<ClienteEntity>>

    /**
     * Si el negocio ya tiene otro cliente con ese teléfono. [exceptoId] deja fuera al que se
     * está editando: si no, corregirle las notas a alguien fallaría por chocar consigo mismo.
     */
    @Query(
        """
        SELECT COUNT(*) FROM clientes
        WHERE negocio_id = :negocioId AND telefono = :telefono AND cliente_id != :exceptoId
        """,
    )
    suspend fun existeConTelefono(negocioId: Long, telefono: String, exceptoId: Long = 0): Int

    /** Lo mismo con el correo. */
    @Query(
        """
        SELECT COUNT(*) FROM clientes
        WHERE negocio_id = :negocioId AND correo = :correo COLLATE NOCASE
          AND cliente_id != :exceptoId
        """,
    )
    suspend fun existeConCorreo(negocioId: Long, correo: String, exceptoId: Long = 0): Int
}
