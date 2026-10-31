package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.db.dao.MetodoPagoDao
import com.gestor_ventures.db.entity.MetodoPagoInactivoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.gestor_ventures.db.enums.MetodoPago as MetodoPagoDb

/** Los métodos que se pueden encender y apagar. OTRO no se ofrece: no le dice nada a nadie. */
val MetodosConfigurables = listOf(MetodoPago.EFECTIVO, MetodoPago.TRANSFERENCIA, MetodoPago.TARJETA)

/**
 * Métodos de pago que acepta cada negocio.
 *
 * La regla que importa: siempre queda al menos uno activo. Sin ninguno, el formulario de venta
 * no tendría qué ofrecer y no se podría registrar nada.
 */
@Singleton
class MetodoPagoRepository @Inject constructor(
    private val metodoPagoDao: MetodoPagoDao,
) {

    /** Los métodos activos del negocio, en el orden de [MetodosConfigurables]. */
    fun activos(negocioId: Long): Flow<List<MetodoPago>> =
        metodoPagoDao.observarInactivos(negocioId).map { inactivos ->
            val apagados = inactivos.map { it.aDominio() }.toSet()
            MetodosConfigurables.filter { it !in apagados }
        }

    /** Enciende o apaga un método. Devuelve `false` si era el último activo y no se apagó. */
    suspend fun cambiar(negocioId: Long, metodo: MetodoPago, activo: Boolean): Boolean {
        if (activo) {
            metodoPagoDao.activar(negocioId, metodo.aDb())
            return true
        }
        val apagados = metodoPagoDao.inactivos(negocioId).map { it.aDominio() }.toSet() + metodo
        if (MetodosConfigurables.all { it in apagados }) return false
        metodoPagoDao.desactivar(MetodoPagoInactivoEntity(negocioId = negocioId, metodoPago = metodo.aDb()))
        return true
    }
}

private fun MetodoPagoDb.aDominio() = when (this) {
    MetodoPagoDb.EFECTIVO -> MetodoPago.EFECTIVO
    MetodoPagoDb.TARJETA -> MetodoPago.TARJETA
    MetodoPagoDb.TRANSFERENCIA -> MetodoPago.TRANSFERENCIA
    MetodoPagoDb.OTRO -> MetodoPago.OTRO
}

private fun MetodoPago.aDb() = when (this) {
    MetodoPago.EFECTIVO -> MetodoPagoDb.EFECTIVO
    MetodoPago.TARJETA -> MetodoPagoDb.TARJETA
    MetodoPago.TRANSFERENCIA -> MetodoPagoDb.TRANSFERENCIA
    MetodoPago.OTRO -> MetodoPagoDb.OTRO
}
