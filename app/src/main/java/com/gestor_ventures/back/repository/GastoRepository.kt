package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorGasto
import com.gestor_ventures.back.model.Gasto
import com.gestor_ventures.back.model.GastoPorCategoria
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.GastoDao
import com.gestor_ventures.db.entity.GastoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-14. Gastos generales del negocio.
 *
 * Acá se decide qué es un mes: el DAO solo sabe de rangos, así que el corte se calcula en un
 * único lugar y no en cada pantalla que quiera un total.
 */
@Singleton
class GastoRepository @Inject constructor(
    private val gastoDao: GastoDao,
    private val reloj: Reloj,
) {

    /** HU-14. Registra el gasto si los datos son válidos y devuelve su id. */
    suspend fun registrarGasto(
        negocioId: Long,
        descripcion: String,
        monto: Double,
        fecha: LocalDate,
        categoriaId: Long? = null,
    ): ErrorGasto? {
        val descripcionLimpia = descripcion.trim()
        validar(descripcionLimpia, monto, fecha)?.let { return it }

        gastoDao.insertar(
            GastoEntity(
                negocioId = negocioId,
                categoriaId = categoriaId,
                descripcion = descripcionLimpia,
                monto = monto,
                fecha = fecha,
            ),
        )
        return null
    }

    /**
     * HU-14. Corrige un gasto que ya existe. Valida lo mismo que al registrarlo.
     *
     * Si el gasto ya no existe no hace nada: se pudo borrar desde otra pantalla.
     */
    suspend fun editarGasto(
        gastoId: Long,
        descripcion: String,
        monto: Double,
        fecha: LocalDate,
        categoriaId: Long? = null,
    ): ErrorGasto? {
        val descripcionLimpia = descripcion.trim()
        validar(descripcionLimpia, monto, fecha)?.let { return it }

        val actual = gastoDao.obtener(gastoId) ?: return null
        gastoDao.actualizar(
            actual.copy(
                descripcion = descripcionLimpia,
                monto = monto,
                fecha = fecha,
                categoriaId = categoriaId,
            ),
        )
        return null
    }

    suspend fun eliminarGasto(gastoId: Long) {
        gastoDao.obtener(gastoId)?.let { gastoDao.eliminar(it) }
    }

    /** Gastos de un mes, del más reciente al más antiguo. */
    fun gastosDelMes(negocioId: Long, mes: YearMonth = mesActual()): Flow<List<Gasto>> =
        gastoDao.observarEntre(negocioId, mes.atDay(1), mes.atEndOfMonth())
            .map { entidades -> entidades.map(::aGasto) }

    /** HU-16: cuánto se gastó en el mes. */
    fun totalDelMes(negocioId: Long, mes: YearMonth = mesActual()): Flow<Double> =
        gastoDao.observarTotalEntre(negocioId, mes.atDay(1), mes.atEndOfMonth())

    /** HU-17: los gastos de un rango de días, del más reciente al más antiguo. */
    fun gastosEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Gasto>> =
        gastoDao.observarEntre(negocioId, desde, hasta).map { it.map(::aGasto) }

    /** HU-08: lo gastado entre dos días, para medir el progreso de la meta. */
    fun totalEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<Double> =
        gastoDao.observarTotalEntre(negocioId, desde, hasta)

    /** HU-15: cuánto se gastó en cada categoría durante el mes. */
    fun totalPorCategoriaDelMes(
        negocioId: Long,
        mes: YearMonth = mesActual(),
    ): Flow<List<GastoPorCategoria>> =
        gastoDao.observarTotalPorCategoriaEntre(negocioId, mes.atDay(1), mes.atEndOfMonth())
            .map { totales ->
                totales.map { GastoPorCategoria(it.categoriaId, it.total) }
            }

    /** El mes en curso según el reloj de la app, que en las pruebas se puede fijar. */
    fun mesActual(): YearMonth = YearMonth.from(reloj.ahora())

    /** El día de hoy, para que la pantalla proponga la fecha del gasto. */
    fun hoy(): LocalDate = reloj.ahora().toLocalDate()

    private fun validar(
        descripcion: String,
        monto: Double,
        fecha: LocalDate,
    ): ErrorGasto? = when {
        descripcion.isEmpty() -> ErrorGasto.DescripcionVacia
        monto <= 0.0 -> ErrorGasto.MontoNoPositivo
        // Un gasto de mañana desordenaría los resúmenes; uno de la semana pasada es normal,
        // porque la gente registra días después de haber pagado.
        fecha.isAfter(hoy()) -> ErrorGasto.FechaEnElFuturo
        else -> null
    }

    private fun aGasto(entidad: GastoEntity) = Gasto(
        id = entidad.gastoId,
        descripcion = entidad.descripcion,
        monto = entidad.monto,
        fecha = entidad.fecha,
        categoriaId = entidad.categoriaId,
    )
}
