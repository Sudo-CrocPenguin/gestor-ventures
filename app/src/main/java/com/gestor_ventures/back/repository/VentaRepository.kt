package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorVenta
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoVenta
import com.gestor_ventures.back.model.ResumenVentas
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.model.Venta
import com.gestor_ventures.db.dao.VentaDao
import com.gestor_ventures.db.entity.VentaEntity
import com.gestor_ventures.db.enums.MetodoPago as MetodoPagoDb
import com.gestor_ventures.db.enums.TipoRegistroVenta as TipoRegistroVentaDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-11 y HU-12. Única puerta de entrada a las ventas.
 *
 * Acá se decide qué es un día: el DAO solo sabe de rangos, así que la frontera entre ayer y hoy
 * se calcula en un único lugar y no en cada pantalla que quiera un resumen.
 */
@Singleton
class VentaRepository @Inject constructor(
    private val ventaDao: VentaDao,
    private val reloj: Reloj,
) {

    /**
     * HU-11/HU-12. Registra la venta si los datos son válidos y devuelve su id.
     *
     * En la venta rápida el producto, el método de pago y el cliente se descartan aunque
     * lleguen con algo: el registro rápido es solo cuánto entró y cuándo.
     */
    suspend fun registrarVenta(
        negocioId: Long,
        tipoRegistro: TipoRegistroVenta,
        monto: Double,
        fechaHora: LocalDateTime = reloj.ahora(),
        productoServicio: String? = null,
        metodoPago: MetodoPago? = null,
        clienteId: Long? = null,
        nota: String? = null,
    ): ResultadoVenta {
        val esDetallada = tipoRegistro == TipoRegistroVenta.DETALLADO
        val productoLimpio = productoServicio?.trim()?.takeIf { it.isNotEmpty() }

        validar(esDetallada, monto, productoLimpio, metodoPago, fechaHora)?.let {
            return ResultadoVenta.Invalido(it)
        }

        val id = ventaDao.insertar(
            VentaEntity(
                negocioId = negocioId,
                tipoRegistro = tipoRegistro.aDb(),
                monto = monto,
                fechaHora = fechaHora,
                productoServicio = productoLimpio.takeIf { esDetallada },
                metodoPago = metodoPago?.aDb().takeIf { esDetallada },
                clienteId = clienteId.takeIf { esDetallada },
                // La nota vale en las dos modalidades: no describe el cobro sino la venta.
                nota = nota?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return ResultadoVenta.Exito(id)
    }

    /**
     * HU-17. Corrige una venta ya registrada, con las mismas reglas que al crearla: un monto en
     * cero o una fecha futura no valen, se esté registrando o corrigiendo.
     *
     * Si la venta ya no existe no hace nada: se pudo borrar desde el historial mientras el
     * formulario estaba abierto, y eso no es un error que el usuario deba resolver.
     *
     * Cambiar de detallada a rápida borra el producto, el método de pago y el cliente. Es lo
     * mismo que hace el registro, y dejarlos colgando sería guardar datos que la pantalla ya no
     * muestra ni deja editar.
     */
    suspend fun editarVenta(
        ventaId: Long,
        tipoRegistro: TipoRegistroVenta,
        monto: Double,
        fechaHora: LocalDateTime,
        productoServicio: String? = null,
        metodoPago: MetodoPago? = null,
        clienteId: Long? = null,
        nota: String? = null,
    ): ResultadoVenta {
        val esDetallada = tipoRegistro == TipoRegistroVenta.DETALLADO
        val productoLimpio = productoServicio?.trim()?.takeIf { it.isNotEmpty() }

        validar(esDetallada, monto, productoLimpio, metodoPago, fechaHora)?.let {
            return ResultadoVenta.Invalido(it)
        }

        val actual = ventaDao.obtener(ventaId) ?: return ResultadoVenta.Exito(ventaId)
        ventaDao.actualizar(
            actual.copy(
                tipoRegistro = tipoRegistro.aDb(),
                monto = monto,
                fechaHora = fechaHora,
                productoServicio = productoLimpio.takeIf { esDetallada },
                metodoPago = metodoPago?.aDb().takeIf { esDetallada },
                clienteId = clienteId.takeIf { esDetallada },
                nota = nota?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return ResultadoVenta.Exito(ventaId)
    }

    /**
     * HU-17. Borra una venta del historial.
     *
     * No queda rastro: el registro se va y con él su plata de todos los totales. Por eso la
     * pantalla pregunta antes, y no al revés.
     */
    suspend fun eliminarVenta(ventaId: Long) {
        ventaDao.obtener(ventaId)?.let { ventaDao.eliminar(it) }
    }

    /** Ventas de un día, de la más reciente a la más antigua. */
    fun ventasDelDia(negocioId: Long, dia: LocalDate = hoy()): Flow<List<Venta>> =
        ventaDao.observarEntre(negocioId, dia.inicio(), dia.fin())
            .map { entidades -> entidades.map(::aVenta) }

    /** HU-13: las ventas del mes, para cruzarlas con los costos y saber el margen. */
    fun ventasDelMes(negocioId: Long, mes: YearMonth = YearMonth.from(reloj.ahora())): Flow<List<Venta>> =
        ventaDao.observarEntre(negocioId, mes.atDay(1).inicio(), mes.atEndOfMonth().fin())
            .map { entidades -> entidades.map(::aVenta) }

    /** HU-16: cuánto se vendió en el mes. */
    fun totalDelMes(negocioId: Long, mes: YearMonth = mesActual()): Flow<Double> =
        totalEntre(negocioId, mes.atDay(1), mes.atEndOfMonth())

    /** HU-08: lo vendido entre dos días, para medir el progreso de la meta desde que se creó. */
    fun totalEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<Double> =
        ventaDao.observarTotalEntre(negocioId, desde.inicio(), hasta.fin())

    /** HU-17: las ventas de un rango de días, de la más reciente a la más antigua. */
    fun ventasEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Venta>> =
        ventaDao.observarEntre(negocioId, desde.inicio(), hasta.fin())
            .map { entidades -> entidades.map(::aVenta) }

    /** Cuánto se vendió en un día y en cuántas ventas: lo que muestra el resumen del inicio. */
    fun resumenDelDia(negocioId: Long, dia: LocalDate = hoy()): Flow<ResumenVentas> =
        combine(
            ventaDao.observarTotalEntre(negocioId, dia.inicio(), dia.fin()),
            ventaDao.contarEntre(negocioId, dia.inicio(), dia.fin()),
        ) { total, cantidad -> ResumenVentas(total = total, cantidad = cantidad) }

    /** El día de hoy según el reloj de la app, que en las pruebas se puede fijar. */
    fun hoy(): LocalDate = reloj.ahora().toLocalDate()

    /** El mes en curso, para que el resumen no tenga que averiguar solo en qué mes está. */
    fun mesActual(): YearMonth = YearMonth.from(reloj.ahora())

    private fun validar(
        esDetallada: Boolean,
        monto: Double,
        productoServicio: String?,
        metodoPago: MetodoPago?,
        fechaHora: LocalDateTime,
    ): ErrorVenta? = when {
        monto <= 0.0 -> ErrorVenta.MontoNoPositivo
        esDetallada && productoServicio == null -> ErrorVenta.ProductoVacio
        esDetallada && metodoPago == null -> ErrorVenta.MetodoPagoFaltante
        // Una venta con fecha futura desordenaría todos los resúmenes del negocio.
        fechaHora.isAfter(reloj.ahora()) -> ErrorVenta.FechaEnElFuturo
        else -> null
    }

    private fun aVenta(entidad: VentaEntity) = Venta(
        id = entidad.ventaId,
        negocioId = entidad.negocioId,
        tipoRegistro = entidad.tipoRegistro.aDominio(),
        monto = entidad.monto,
        fechaHora = entidad.fechaHora,
        productoServicio = entidad.productoServicio,
        metodoPago = entidad.metodoPago?.aDominio(),
        clienteId = entidad.clienteId,
        nota = entidad.nota,
    )
}

/** El día completo, desde el primer milisegundo hasta el último. */
private fun LocalDate.inicio(): LocalDateTime = atStartOfDay()

private fun LocalDate.fin(): LocalDateTime = atTime(LocalTime.MAX)

private fun TipoRegistroVenta.aDb(): TipoRegistroVentaDb = when (this) {
    TipoRegistroVenta.DETALLADO -> TipoRegistroVentaDb.DETALLADO
    TipoRegistroVenta.RAPIDO -> TipoRegistroVentaDb.RAPIDO
}

private fun TipoRegistroVentaDb.aDominio(): TipoRegistroVenta = when (this) {
    TipoRegistroVentaDb.DETALLADO -> TipoRegistroVenta.DETALLADO
    TipoRegistroVentaDb.RAPIDO -> TipoRegistroVenta.RAPIDO
}

private fun MetodoPago.aDb(): MetodoPagoDb = when (this) {
    MetodoPago.EFECTIVO -> MetodoPagoDb.EFECTIVO
    MetodoPago.TARJETA -> MetodoPagoDb.TARJETA
    MetodoPago.TRANSFERENCIA -> MetodoPagoDb.TRANSFERENCIA
    MetodoPago.OTRO -> MetodoPagoDb.OTRO
}

private fun MetodoPagoDb.aDominio(): MetodoPago = when (this) {
    MetodoPagoDb.EFECTIVO -> MetodoPago.EFECTIVO
    MetodoPagoDb.TARJETA -> MetodoPago.TARJETA
    MetodoPagoDb.TRANSFERENCIA -> MetodoPago.TRANSFERENCIA
    MetodoPagoDb.OTRO -> MetodoPago.OTRO
}
