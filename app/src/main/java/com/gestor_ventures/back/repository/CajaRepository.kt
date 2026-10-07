package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Caja
import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.CajaDao
import com.gestor_ventures.db.entity.CajaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-19. La jornada de caja del negocio.
 *
 * La regla que sostiene toda la épica es que no puede haber dos cajas abiertas a la vez: si se
 * abre una segunda sin cerrar la primera, las dos reclaman las mismas ventas y gastos y ninguna
 * cuadra al cerrar. Por eso [abrirCaja] pregunta antes de insertar.
 *
 * La hora de apertura no se le pide al usuario: se toma del [Reloj] en el momento de abrir. Es
 * lo que pide la historia, y además es lo honesto — la hora que importa es cuándo se abrió de
 * verdad, no cuándo alguien dice que se abrió.
 */
@Singleton
class CajaRepository @Inject constructor(
    private val cajaDao: CajaDao,
    private val reloj: Reloj,
) {

    /** La jornada en curso del negocio, o `null` si no hay ninguna abierta. */
    fun cajaAbierta(negocioId: Long): Flow<Caja?> =
        cajaDao.observarAbierta(negocioId).map { entidad -> entidad?.let(::aCaja) }

    /**
     * HU-19. Abre la jornada con el efectivo con el que se arranca.
     *
     * Cero es un monto válido: se puede empezar el turno sin nada en la caja. Lo que no se
     * admite es un negativo, porque no se arranca debiendo.
     */
    suspend fun abrirCaja(negocioId: Long, montoInicial: Double): ErrorCaja? {
        if (montoInicial < 0.0) return ErrorCaja.MontoNegativo

        if (cajaDao.obtenerAbierta(negocioId) != null) return ErrorCaja.YaHayCajaAbierta

        cajaDao.insertar(
            CajaEntity(
                negocioId = negocioId,
                montoInicial = montoInicial,
                fechaHoraApertura = reloj.ahora(),
            ),
        )
        return null
    }

    private fun aCaja(entidad: CajaEntity) = Caja(
        id = entidad.cajaId,
        montoInicial = entidad.montoInicial,
        fechaHoraApertura = entidad.fechaHoraApertura,
    )
}
