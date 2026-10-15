package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorObligacion
import com.gestor_ventures.back.model.Obligacion
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.ObligacionDao
import com.gestor_ventures.db.entity.ObligacionEntity
import com.gestor_ventures.db.enums.EstadoPago
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-07. Cuántos días antes cuenta como "próxima a vencer".
 *
 * Una semana es el horizonte con el que un emprendedor piensa sus pagos. No está en el criterio
 * de la historia; si el negocio pide otro plazo, se cambia acá y en ningún otro lado.
 */
const val DiasParaVencer = 7L

/**
 * HU-07. Los compromisos que el negocio ya adquirió.
 *
 * La fecha de vencimiento no se valida contra hoy, al revés que en gastos y costos: una cuota
 * que vence el otro mes y una que ya se venció son las dos situaciones que esta historia existe
 * para cubrir. Lo único obligatorio es que tenga fecha, y eso lo garantiza el calendario.
 */
@Singleton
class ObligacionRepository @Inject constructor(
    private val obligacionDao: ObligacionDao,
    private val reloj: Reloj,
) {

    /** Todas las del negocio: primero las pendientes, y dentro de esas la que vence antes. */
    fun obligacionesDeNegocio(negocioId: Long): Flow<List<Obligacion>> =
        obligacionDao.observarDeNegocio(negocioId).map { lista -> lista.map(::aObligacion) }

    /**
     * HU-07. Las que exigen atención: las que vencen dentro de [DiasParaVencer] días y las que
     * ya se pasaron de fecha sin pagarse.
     */
    fun proximasAVencer(negocioId: Long): Flow<List<Obligacion>> =
        obligacionDao.observarProximasAVencer(negocioId, hoy().plusDays(DiasParaVencer))
            .map { lista -> lista.map(::aObligacion) }

    /** HU-16: lo que el negocio debe y todavía no ha pagado. */
    fun totalPendiente(negocioId: Long): Flow<Double> =
        obligacionDao.observarTotalPendiente(negocioId)

    /** HU-07. Registra la obligación si los datos son válidos. */
    suspend fun registrarObligacion(
        negocioId: Long,
        nombre: String,
        monto: Double,
        fechaVencimiento: LocalDate,
    ): ErrorObligacion? {
        val nombreLimpio = nombre.trim()
        validar(nombreLimpio, monto)?.let { return it }

        obligacionDao.insertar(
            ObligacionEntity(
                negocioId = negocioId,
                nombreObligacion = nombreLimpio,
                monto = monto,
                fechaVencimiento = fechaVencimiento,
                fechaRegistro = reloj.ahora(),
            ),
        )
        return null
    }

    /**
     * HU-07. Corrige una obligación. Conserva si estaba pagada: cambiarle el monto a una cuota
     * ya pagada no la vuelve a deber.
     */
    suspend fun editarObligacion(
        obligacionId: Long,
        nombre: String,
        monto: Double,
        fechaVencimiento: LocalDate,
    ): ErrorObligacion? {
        val nombreLimpio = nombre.trim()
        validar(nombreLimpio, monto)?.let { return it }

        val actual = obligacionDao.obtener(obligacionId) ?: return null
        obligacionDao.actualizar(
            actual.copy(
                nombreObligacion = nombreLimpio,
                monto = monto,
                fechaVencimiento = fechaVencimiento,
            ),
        )
        return null
    }

    /**
     * HU-07. Marca la obligación como pagada, o la devuelve a pendiente si el usuario se
     * equivocó. No se borra: una deuda pagada es historia del negocio.
     */
    suspend fun marcarPagada(obligacionId: Long, pagada: Boolean) {
        val actual = obligacionDao.obtener(obligacionId) ?: return
        obligacionDao.actualizar(
            actual.copy(estadoPago = if (pagada) EstadoPago.PAGADA else EstadoPago.PENDIENTE),
        )
    }

    suspend fun eliminarObligacion(obligacionId: Long) {
        obligacionDao.obtener(obligacionId)?.let { obligacionDao.eliminar(it) }
    }

    /** El día de hoy según el reloj de la app, que en las pruebas se puede fijar. */
    fun hoy(): LocalDate = reloj.ahora().toLocalDate()

    private fun validar(nombre: String, monto: Double): ErrorObligacion? = when {
        nombre.isEmpty() -> ErrorObligacion.NombreVacio
        monto <= 0.0 -> ErrorObligacion.MontoNoPositivo
        else -> null
    }

    private fun aObligacion(entidad: ObligacionEntity) = Obligacion(
        id = entidad.obligacionId,
        nombre = entidad.nombreObligacion,
        monto = entidad.monto,
        fechaVencimiento = entidad.fechaVencimiento,
        pagada = entidad.estadoPago == EstadoPago.PAGADA,
    )
}
