package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Alerta
import com.gestor_ventures.back.model.AlertaNueva
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoAlerta
import com.gestor_ventures.db.dao.AlertaDao
import com.gestor_ventures.db.entity.AlertaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.gestor_ventures.db.enums.TipoAlerta as TipoAlertaDb

/** HU-38. Guarda las alertas y lleva cuáles ya se leyeron. */
@Singleton
class AlertaRepository @Inject constructor(
    private val alertaDao: AlertaDao,
    private val reloj: Reloj,
) {

    fun alertas(negocioId: Long): Flow<List<Alerta>> =
        alertaDao.observarDeNegocio(negocioId).map { filas -> filas.map(::aAlerta) }

    suspend fun negocios(): List<Long> = alertaDao.idsDeNegocios()

    suspend fun huboDesde(negocioId: Long, tipo: TipoAlerta, desde: LocalDateTime): Boolean =
        alertaDao.contarDesde(negocioId, tipo.aDb(), desde) > 0

    suspend fun registrar(negocioId: Long, alerta: AlertaNueva): Long =
        alertaDao.insertar(
            AlertaEntity(
                negocioId = negocioId,
                tipoAlerta = alerta.tipo.aDb(),
                descripcion = alerta.descripcion,
                fechaGeneracion = reloj.ahora(),
            ),
        )

    suspend fun marcarLeida(alertaId: Long) {
        alertaDao.marcarLeida(alertaId)
    }

    suspend fun marcarTodasLeidas(negocioId: Long) {
        alertaDao.marcarTodasLeidas(negocioId)
    }

    private fun aAlerta(entidad: AlertaEntity) = Alerta(
        id = entidad.alertaId,
        tipo = entidad.tipoAlerta.aDominio(),
        descripcion = entidad.descripcion,
        leida = entidad.leida,
        fechaGeneracion = entidad.fechaGeneracion,
    )
}

private fun TipoAlerta.aDb(): TipoAlertaDb = when (this) {
    TipoAlerta.CaidaVentas -> TipoAlertaDb.CAIDA_VENTAS
    TipoAlerta.GastoElevado -> TipoAlertaDb.GASTO_ELEVADO
    TipoAlerta.MetaEnRiesgo -> TipoAlertaDb.META_EN_RIESGO
}

private fun TipoAlertaDb.aDominio(): TipoAlerta = when (this) {
    TipoAlertaDb.CAIDA_VENTAS -> TipoAlerta.CaidaVentas
    TipoAlertaDb.GASTO_ELEVADO -> TipoAlerta.GastoElevado
    TipoAlertaDb.META_EN_RIESGO -> TipoAlerta.MetaEnRiesgo
}
