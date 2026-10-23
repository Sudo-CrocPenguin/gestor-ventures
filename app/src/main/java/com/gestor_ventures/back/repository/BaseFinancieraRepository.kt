package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorBaseFinanciera
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.GastoFijo
import com.gestor_ventures.back.model.MetaAhorro
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.GastoFijoDao
import com.gestor_ventures.db.dao.MetaAhorroDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.entity.GastoFijoEntity
import com.gestor_ventures.db.entity.MetaAhorroEntity
import com.gestor_ventures.db.enums.Frecuencia as FrecuenciaDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-06, HU-08 y HU-09. La base financiera del negocio: sus gastos fijos, su meta de ahorro y
 * qué porcentaje de la ganancia se reinvierte.
 *
 * Los tres viven juntos porque son la misma conversación con el usuario (el paso 2 del
 * onboarding y, después, la configuración del negocio).
 */
@Singleton
class BaseFinancieraRepository @Inject constructor(
    private val gastoFijoDao: GastoFijoDao,
    private val metaAhorroDao: MetaAhorroDao,
    private val negocioDao: NegocioDao,
    private val reloj: Reloj,
) {

    // ---------- HU-06: gastos fijos ----------

    fun gastosFijosDeNegocio(negocioId: Long): Flow<List<GastoFijo>> =
        gastoFijoDao.observarDeNegocio(negocioId).map { lista -> lista.map(::aGastoFijo) }

    /**
     * HU-06/HU-16: cuánto pesa lo fijo en un mes.
     *
     * No es la suma de los montos: se lleva cada gasto a su equivalente mensual primero. Sumar
     * un arriendo mensual con unos empaques semanales daría un número que no es de ningún
     * periodo, y el resumen financiero habla de meses.
     */
    fun gastosFijosMensuales(negocioId: Long): Flow<Double> =
        gastosFijosDeNegocio(negocioId).map { gastos -> gastos.sumOf { it.montoMensual } }

    suspend fun agregarGastoFijo(
        negocioId: Long,
        nombre: String,
        monto: Double,
        frecuencia: Frecuencia,
    ): ErrorBaseFinanciera? {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) return ErrorBaseFinanciera.NombreGastoVacio
        if (monto <= 0.0) return ErrorBaseFinanciera.MontoNoPositivo

        gastoFijoDao.insertar(
            GastoFijoEntity(
                negocioId = negocioId,
                nombreGasto = nombreLimpio,
                monto = monto,
                frecuencia = frecuencia.aDb(),
                fechaRegistro = reloj.ahora(),
            ),
        )
        return null
    }

    /**
     * HU-06. Cambia un gasto fijo que ya existe. Valida lo mismo que al crearlo: un gasto sin
     * nombre o sin monto no sirve, se esté creando o editando.
     *
     * Si el gasto ya no existe no hace nada: se pudo borrar desde otra pantalla mientras esta
     * estaba abierta, y no es un error que el usuario deba resolver.
     */
    suspend fun editarGastoFijo(
        gastoFijoId: Long,
        nombre: String,
        monto: Double,
        frecuencia: Frecuencia,
    ): ErrorBaseFinanciera? {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) return ErrorBaseFinanciera.NombreGastoVacio
        if (monto <= 0.0) return ErrorBaseFinanciera.MontoNoPositivo

        val actual = gastoFijoDao.obtener(gastoFijoId) ?: return null
        gastoFijoDao.actualizar(
            actual.copy(
                nombreGasto = nombreLimpio,
                monto = monto,
                frecuencia = frecuencia.aDb(),
            ),
        )
        return null
    }

    suspend fun eliminarGastoFijo(gastoFijoId: Long) {
        gastoFijoDao.obtener(gastoFijoId)?.let { gastoFijoDao.eliminar(it) }
    }

    // ---------- HU-08: meta de ahorro ----------

    fun metaActiva(negocioId: Long): Flow<MetaAhorro?> =
        metaAhorroDao.observarActivaDeNegocio(negocioId).map { entidad -> entidad?.let(::aMeta) }

    /**
     * Guarda la meta del negocio. La fecha límite debe ser posterior a hoy, como pide HU-08.
     *
     * Corregir una meta que ya existe la cambia en su sitio en vez de crear otra: si cada
     * ajuste dejara una fila nueva, el negocio terminaría con un historial de metas que nunca
     * tuvo. La fecha de creación se conserva a propósito —subir el objetivo no borra lo que ya
     * se llevaba ahorrado— y por eso el progreso sigue contando desde el día original.
     */
    suspend fun definirMetaAhorro(
        negocioId: Long,
        montoObjetivo: Double,
        fechaLimite: LocalDate,
    ): ErrorBaseFinanciera? {
        if (montoObjetivo <= 0.0) return ErrorBaseFinanciera.MetaSinMonto
        if (!fechaLimite.isAfter(reloj.ahora().toLocalDate())) {
            return ErrorBaseFinanciera.FechaLimiteNoPosterior
        }

        val actual = metaAhorroDao.obtenerActivaDeNegocio(negocioId)
        if (actual == null) {
            metaAhorroDao.insertar(
                MetaAhorroEntity(
                    negocioId = negocioId,
                    montoObjetivo = montoObjetivo,
                    fechaLimite = fechaLimite,
                    fechaCreacion = reloj.ahora(),
                ),
            )
        } else {
            metaAhorroDao.actualizar(
                actual.copy(montoObjetivo = montoObjetivo, fechaLimite = fechaLimite),
            )
        }
        return null
    }

    // ---------- HU-09: porcentaje de reinversión ----------

    suspend fun definirPorcentajeReinversion(
        negocioId: Long,
        porcentaje: Double,
    ): ErrorBaseFinanciera? {
        if (porcentaje !in 0.0..100.0) return ErrorBaseFinanciera.PorcentajeFueraDeRango

        val negocio = negocioDao.obtener(negocioId) ?: return null
        negocioDao.actualizar(
            negocio.copy(
                porcentajeReinversion = porcentaje,
                fechaActualizacion = reloj.ahora(),
            ),
        )
        return null
    }

    private fun aGastoFijo(entidad: GastoFijoEntity) = GastoFijo(
        id = entidad.gastoFijoId,
        nombre = entidad.nombreGasto,
        monto = entidad.monto,
        frecuencia = entidad.frecuencia.aDominio(),
    )

    private fun aMeta(entidad: MetaAhorroEntity) = MetaAhorro(
        id = entidad.metaAhorroId,
        montoObjetivo = entidad.montoObjetivo,
        fechaLimite = entidad.fechaLimite,
        fechaCreacion = entidad.fechaCreacion.toLocalDate(),
    )
}

private fun Frecuencia.aDb(): FrecuenciaDb = when (this) {
    Frecuencia.SEMANAL -> FrecuenciaDb.SEMANAL
    Frecuencia.QUINCENAL -> FrecuenciaDb.QUINCENAL
    Frecuencia.MENSUAL -> FrecuenciaDb.MENSUAL
    Frecuencia.ANUAL -> FrecuenciaDb.ANUAL
}

private fun FrecuenciaDb.aDominio(): Frecuencia = when (this) {
    FrecuenciaDb.SEMANAL -> Frecuencia.SEMANAL
    FrecuenciaDb.QUINCENAL -> Frecuencia.QUINCENAL
    FrecuenciaDb.MENSUAL -> Frecuencia.MENSUAL
    FrecuenciaDb.ANUAL -> Frecuencia.ANUAL
}
