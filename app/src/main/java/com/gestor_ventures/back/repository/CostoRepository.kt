package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Costo
import com.gestor_ventures.back.model.ErrorCosto
import com.gestor_ventures.back.model.GastoPorCategoria
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.CostoDao
import com.gestor_ventures.db.entity.CostoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-13. Costos de producir lo que el negocio vende.
 *
 * Acá se decide qué es un mes, igual que en gastos: el DAO solo sabe de rangos.
 */
@Singleton
class CostoRepository @Inject constructor(
    private val costoDao: CostoDao,
    private val reloj: Reloj,
) {

    /** HU-13. Registra el costo si los datos son válidos. */
    suspend fun registrarCosto(
        negocioId: Long,
        productoServicio: String,
        monto: Double,
        fecha: LocalDateTime = reloj.ahora(),
        categoriaId: Long? = null,
    ): ErrorCosto? {
        val productoLimpio = productoServicio.trim()
        validar(productoLimpio, monto, fecha)?.let { return it }

        costoDao.insertar(
            CostoEntity(
                negocioId = negocioId,
                categoriaId = categoriaId,
                productoServicio = productoLimpio,
                montoCosto = monto,
                fechaRegistro = fecha,
            ),
        )
        return null
    }

    /**
     * HU-13. Corrige un costo que ya existe. Si ya no existe no hace nada: se pudo borrar desde
     * otra pantalla.
     */
    suspend fun editarCosto(
        costoId: Long,
        productoServicio: String,
        monto: Double,
        fecha: LocalDateTime,
        categoriaId: Long? = null,
    ): ErrorCosto? {
        val productoLimpio = productoServicio.trim()
        validar(productoLimpio, monto, fecha)?.let { return it }

        val actual = costoDao.obtener(costoId) ?: return null
        costoDao.actualizar(
            actual.copy(
                productoServicio = productoLimpio,
                montoCosto = monto,
                fechaRegistro = fecha,
                categoriaId = categoriaId,
            ),
        )
        return null
    }

    suspend fun eliminarCosto(costoId: Long) {
        costoDao.obtener(costoId)?.let { costoDao.eliminar(it) }
    }

    /** Costos de un mes, del más reciente al más antiguo. */
    fun costosDelMes(negocioId: Long, mes: YearMonth = mesActual()): Flow<List<Costo>> =
        costoDao.observarEntre(negocioId, mes.inicio(), mes.fin())
            .map { entidades -> entidades.map(::aCosto) }

    /** HU-16: cuánto costó producir en el mes. */
    fun totalDelMes(negocioId: Long, mes: YearMonth = mesActual()): Flow<Double> =
        costoDao.observarTotalEntre(negocioId, mes.inicio(), mes.fin())

    /** HU-08: lo costeado entre dos días, para medir el progreso de la meta. */
    fun totalEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<Double> =
        costoDao.observarTotalEntre(negocioId, desde.atStartOfDay(), hasta.atTime(LocalTime.MAX))

    /** HU-15: cuánto se costeó en cada categoría durante el mes. */
    fun totalPorCategoriaDelMes(
        negocioId: Long,
        mes: YearMonth = mesActual(),
    ): Flow<List<GastoPorCategoria>> =
        costoDao.observarTotalPorCategoriaEntre(negocioId, mes.inicio(), mes.fin())
            .map { totales -> totales.map { GastoPorCategoria(it.categoriaId, it.total) } }

    /** El mes en curso según el reloj de la app, que en las pruebas se puede fijar. */
    fun mesActual(): YearMonth = YearMonth.from(reloj.ahora())

    fun ahora(): LocalDateTime = reloj.ahora()

    private fun validar(
        productoServicio: String,
        monto: Double,
        fecha: LocalDateTime,
    ): ErrorCosto? = when {
        productoServicio.isEmpty() -> ErrorCosto.ProductoVacio
        monto <= 0.0 -> ErrorCosto.MontoNoPositivo
        fecha.isAfter(reloj.ahora()) -> ErrorCosto.FechaEnElFuturo
        else -> null
    }

    private fun aCosto(entidad: CostoEntity) = Costo(
        id = entidad.costoId,
        productoServicio = entidad.productoServicio,
        monto = entidad.montoCosto,
        fecha = entidad.fechaRegistro,
        categoriaId = entidad.categoriaId,
    )
}

private fun YearMonth.inicio(): LocalDateTime = atDay(1).atStartOfDay()

private fun YearMonth.fin(): LocalDateTime = atEndOfMonth().atTime(LocalTime.MAX)
