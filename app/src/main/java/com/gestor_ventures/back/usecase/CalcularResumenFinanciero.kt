package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.DiasPorMes
import com.gestor_ventures.back.model.libreParaAhorrarDe
import com.gestor_ventures.back.model.MetaAhorro
import com.gestor_ventures.back.model.ProgresoMeta
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResumenFinanciero
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.ObligacionRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * HU-16. Junta en un solo número lo que el negocio tiene repartido en cinco pantallas.
 *
 * Vive en `usecase` y no en un repositorio porque cruza seis de ellos: ninguno es su dueño. Las
 * reglas de qué se suma y qué se resta están en [ResumenFinanciero]; acá solo se recoge cada
 * pieza de donde viva.
 *
 * Lo que aporta cada historia:
 * - HU-11/HU-12 los ingresos, HU-13 los costos, HU-14 los gastos del mes.
 * - HU-06 los gastos fijos, ya llevados a su equivalente mensual.
 * - HU-07 lo que se debe y no se ha pagado.
 * - HU-08 cuánto apartar este mes y cómo va la meta.
 * - HU-09 el porcentaje que vuelve al negocio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalcularResumenFinanciero @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val gastoRepository: GastoRepository,
    private val costoRepository: CostoRepository,
    private val baseFinancieraRepository: BaseFinancieraRepository,
    private val obligacionRepository: ObligacionRepository,
    private val negocioRepository: NegocioRepository,
    private val calcularAhorroMensual: CalcularAhorroMensual,
    private val reloj: Reloj,
) {

    operator fun invoke(
        negocioId: Long,
        mes: YearMonth = YearMonth.from(reloj.ahora()),
    ): Flow<ResumenFinanciero> = combine(
        movimientosDel(negocioId, mes),
        compromisosDe(negocioId),
        progresoDeLaMeta(negocioId),
        negocioRepository.observarNegocio(negocioId),
    ) { movimientos, compromisos, progreso, negocio ->
        ResumenFinanciero(
            mes = mes,
            ingresos = movimientos.ingresos,
            gastos = movimientos.gastos,
            costos = movimientos.costos,
            gastosFijos = compromisos.gastosFijos,
            obligacionesPendientes = compromisos.obligaciones,
            apartadoParaMeta = apartadoDe(compromisos.meta),
            porcentajeReinversion = negocio?.porcentajeReinversion ?: 0.0,
            progresoMeta = progreso,
        )
    }

    /** Lo que de verdad pasó en el mes: entró, salió y costó producir. */
    private fun movimientosDel(negocioId: Long, mes: YearMonth): Flow<Movimientos> = combine(
        ventaRepository.totalDelMes(negocioId, mes),
        gastoRepository.totalDelMes(negocioId, mes),
        costoRepository.totalDelMes(negocioId, mes),
    ) { ingresos, gastos, costos -> Movimientos(ingresos, gastos, costos) }

    /** Lo que el usuario dejó configurado y la app tiene que respetar. */
    private fun compromisosDe(negocioId: Long): Flow<Compromisos> = combine(
        baseFinancieraRepository.gastosFijosMensuales(negocioId),
        obligacionRepository.totalPendiente(negocioId),
        baseFinancieraRepository.metaActiva(negocioId),
    ) { gastosFijos, obligaciones, meta -> Compromisos(gastosFijos, obligaciones, meta) }

    /**
     * HU-08. Cuánto lleva el negocio de su meta.
     *
     * Se mide desde el día en que se definió: la ganancia de antes es de otra historia. Los
     * gastos fijos no están registrados como movimientos —son configuración— así que se cobran
     * a razón de su equivalente mensual por el tiempo que lleva corriendo la meta.
     *
     * Es público porque el inicio también muestra el progreso sin mostrar el resumen entero.
     */
    fun progresoDeLaMeta(negocioId: Long): Flow<ProgresoMeta?> =
        baseFinancieraRepository.metaActiva(negocioId).flatMapLatest { meta ->
            if (meta == null) flowOf(null) else acumuladoDesde(negocioId, meta.fechaCreacion)
                .map { acumulado ->
                    ProgresoMeta(
                        montoObjetivo = meta.montoObjetivo,
                        acumulado = acumulado,
                        fechaLimite = meta.fechaLimite,
                    )
                }
        }

    /**
     * Lo que el negocio ha dejado libre desde [desde] hasta hoy, con las mismas reglas de
     * HU-16: la ganancia del periodo menos lo que se debe y menos lo que vuelve al negocio.
     *
     * Se detiene justo antes de apartar para la meta. El dinero disponible de HU-16 ya le restó
     * ese apartado, y medir la meta con un número al que se le quitó el ahorro de la meta sería
     * descontar dos veces lo mismo: el emprendedor guardaría lo que le pedimos todos los meses
     * y el progreso nunca llegaría.
     */
    private fun acumuladoDesde(negocioId: Long, desde: LocalDate): Flow<Double> {
        val hoy = reloj.ahora().toLocalDate()
        val meses = ChronoUnit.DAYS.between(desde, hoy).coerceAtLeast(0) / DiasPorMes

        val gananciaDelPeriodo = combine(
            ventaRepository.totalEntre(negocioId, desde, hoy),
            gastoRepository.totalEntre(negocioId, desde, hoy),
            costoRepository.totalEntre(negocioId, desde, hoy),
            baseFinancieraRepository.gastosFijosMensuales(negocioId),
        ) { ingresos, gastos, costos, gastosFijos ->
            ingresos - gastos - costos - gastosFijos * meses
        }

        return combine(
            gananciaDelPeriodo,
            obligacionRepository.totalPendiente(negocioId),
            negocioRepository.observarNegocio(negocioId),
        ) { ganancia, obligaciones, negocio ->
            val libre = libreParaAhorrarDe(
                ganancia = ganancia,
                obligaciones = obligaciones,
                porcentaje = negocio?.porcentajeReinversion ?: 0.0,
            )
            // Perder plata no es tener un progreso negativo: la meta se queda en cero.
            libre.coerceAtLeast(0.0)
        }
    }

    /** Sin meta no hay nada que apartar, y una meta vencida ya no reparte en meses. */
    private fun apartadoDe(meta: MetaAhorro?): Double = meta
        ?.let { calcularAhorroMensual(it.montoObjetivo, it.fechaLimite) }
        ?: 0.0

    private data class Movimientos(
        val ingresos: Double,
        val gastos: Double,
        val costos: Double,
    )

    private data class Compromisos(
        val gastosFijos: Double,
        val obligaciones: Double,
        val meta: MetaAhorro?,
    )
}
