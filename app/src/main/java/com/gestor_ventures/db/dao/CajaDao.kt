package com.gestor_ventures.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestor_ventures.db.entity.CajaEntity
import kotlinx.coroutines.flow.Flow

/**
 * HU-19 a HU-23. Las jornadas de caja del negocio.
 *
 * Una caja es un periodo con principio y final: se abre contando el efectivo con el que se
 * arranca y se cierra contando el que quedó. Lo que pasa en el medio —ventas, gastos, costos—
 * no se copia acá: ya vive en sus propias tablas y se consulta por el rango de fechas de la
 * caja (ver la nota de [CajaEntity]).
 *
 * La regla de HU-19 es que no puede haber dos cajas abiertas a la vez, y por eso casi todo
 * acá gira alrededor de [obtenerAbierta]: es lo que responde "¿ya hay una jornada en curso?".
 */
@Dao
interface CajaDao {

    @Insert
    suspend fun insertar(caja: CajaEntity): Long

    /**
     * La caja abierta del negocio, si hay alguna. El repositorio la consulta antes de abrir
     * otra, porque abrir una segunda sin cerrar la primera deja dos jornadas reclamando los
     * mismos movimientos y ninguna cuadra al final.
     *
     * El `LIMIT 1` no sobra aunque la regla prometa que solo hay una: si alguna vez llegara a
     * haber dos, es mejor que esto devuelva la más reciente a que reviente.
     */
    @Query(
        """
        SELECT * FROM cajas
        WHERE negocio_id = :negocioId AND estado_caja = 'ABIERTA'
        ORDER BY fecha_hora_apertura DESC, caja_id DESC
        LIMIT 1
        """,
    )
    suspend fun obtenerAbierta(negocioId: Long): CajaEntity?

    /** La misma consulta, pero seguida: la pantalla cambia sola al abrir o cerrar la caja. */
    @Query(
        """
        SELECT * FROM cajas
        WHERE negocio_id = :negocioId AND estado_caja = 'ABIERTA'
        ORDER BY fecha_hora_apertura DESC, caja_id DESC
        LIMIT 1
        """,
    )
    fun observarAbierta(negocioId: Long): Flow<CajaEntity?>
}
