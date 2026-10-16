package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.RangoFechas
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** HU-40. Cada cuánto se agrupan los movimientos del gráfico. */
enum class EscalaDeTiempo { Semanal, Mensual }

/** Cuántos tramos se muestran de cada escala: suficientes para ver una tendencia, no tantos que no se lean. */
private const val SemanasAMostrar = 8
private const val MesesAMostrar = 6

/** HU-40. Un tramo del gráfico: lo que entró y lo que salió en ese pedazo de tiempo. */
data class PuntoDeTendencia(
    val inicio: LocalDate,
    val ingresos: Double,
    val egresos: Double,
) {
    /** Lo que quedó en ese tramo. Puede ser negativo, y así se muestra. */
    val ganancia: Double get() = ingresos - egresos
}

/**
 * HU-40. Ingresos y egresos a lo largo del tiempo, para ver la tendencia.
 *
 * Trae los movimientos del periodo completo una sola vez y los reparte en tramos acá, en vez de
 * pedirle a la base de datos un total por tramo. Son tres consultas en lugar de veinticuatro, y
 * sobre todo son tres `Flow`: al registrar una venta el gráfico se redibuja solo, que es lo que
 * pide el criterio de "actualizar automáticamente".
 *
 * Los egresos juntan gastos y costos porque la pregunta del gráfico es "¿cuánto entró y cuánto
 * salió?", y para esa pregunta los dos son lo mismo. El detalle sigue en Finanzas.
 */
@Singleton
class CalcularTendencia @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val gastoRepository: GastoRepository,
    private val costoRepository: CostoRepository,
    private val reloj: Reloj,
) {

    operator fun invoke(
        negocioId: Long,
        escala: EscalaDeTiempo,
    ): Flow<List<PuntoDeTendencia>> {
        val tramos = tramosDe(escala, hoy())
        val desde = tramos.first().desde
        val hasta = tramos.last().hasta

        return combine(
            ventaRepository.ventasEntre(negocioId, desde, hasta),
            gastoRepository.gastosEntre(negocioId, desde, hasta),
            costoRepository.costosEntre(negocioId, desde, hasta),
        ) { ventas, gastos, costos ->
            tramos.map { tramo ->
                PuntoDeTendencia(
                    inicio = tramo.desde,
                    ingresos = ventas
                        .filter { it.fechaHora.toLocalDate() in tramo }
                        .sumOf { it.monto },
                    egresos = gastos.filter { it.fecha.toLocalDate() in tramo }.sumOf { it.monto } +
                        costos.filter { it.fecha.toLocalDate() in tramo }.sumOf { it.monto },
                )
            }
        }
    }

    fun hoy(): LocalDate = reloj.ahora().toLocalDate()
}

/**
 * Los tramos del gráfico, del más viejo al más nuevo, terminando en el que corre hoy.
 *
 * El último tramo va completo aunque todavía no haya terminado: no existen movimientos con
 * fecha futura, así que los días que faltan suman cero y no desordenan nada.
 */
internal fun tramosDe(escala: EscalaDeTiempo, hoy: LocalDate): List<RangoFechas> = when (escala) {
    EscalaDeTiempo.Semanal -> {
        val estaSemana = hoy.with(DayOfWeek.MONDAY)
        (SemanasAMostrar - 1 downTo 0).map { atras ->
            val lunes = estaSemana.minusWeeks(atras.toLong())
            RangoFechas(lunes, lunes.plusDays(6))
        }
    }

    EscalaDeTiempo.Mensual -> {
        val esteMes = hoy.withDayOfMonth(1)
        (MesesAMostrar - 1 downTo 0).map { atras ->
            val primero = esteMes.minusMonths(atras.toLong())
            RangoFechas(primero, primero.withDayOfMonth(primero.lengthOfMonth()))
        }
    }
}

/** Si un día cae dentro del tramo, con los dos extremos incluidos. */
private operator fun RangoFechas.contains(dia: LocalDate): Boolean =
    !dia.isBefore(desde) && !dia.isAfter(hasta)
