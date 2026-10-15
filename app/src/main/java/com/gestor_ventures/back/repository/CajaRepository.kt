package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Caja
import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.CajaDao
import com.gestor_ventures.db.entity.CajaEntity
import com.gestor_ventures.db.enums.EstadoCaja
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

    /**
     * HU-21 y HU-22. Cierra la jornada con el efectivo que el usuario contó de verdad.
     *
     * La hora de cierre la pone el [Reloj], igual que la de apertura. La nota es opcional:
     * cuando la caja cuadra no hay nada que explicar, y exigirla llenaría el historial de
     * "ok" escritos por salir del paso.
     *
     * No recibe el saldo esperado ni guarda la diferencia: las dos cosas salen de los
     * movimientos de la jornada y se pueden volver a calcular cuando haga falta (HU-23). Un
     * número copiado es un número que algún día deja de cuadrar con su origen.
     */
    suspend fun cerrarCaja(negocioId: Long, montoReal: Double, nota: String? = null): ErrorCaja? {
        if (montoReal < 0.0) return ErrorCaja.MontoNegativo

        val abierta = cajaDao.obtenerAbierta(negocioId) ?: return ErrorCaja.NoHayCajaAbierta

        cajaDao.actualizar(
            abierta.copy(
                montoRealCierre = montoReal,
                fechaHoraCierre = reloj.ahora(),
                estadoCaja = EstadoCaja.CERRADA,
                notaDiferencia = nota?.trim()?.takeIf { it.isNotEmpty() },
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
