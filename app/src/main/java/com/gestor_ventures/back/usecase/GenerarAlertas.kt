package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.AlertaNueva
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoAlerta
import com.gestor_ventures.back.model.pesosEnTexto
import com.gestor_ventures.back.repository.AlertaRepository
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

private const val CaidaMinima = 0.30

private const val SubidaMinima = 50.0

private const val AtrasoMaximoDeLaMeta = 0.20

private const val TiempoMinimoDeLaMeta = 0.25

class GenerarAlertas @Inject constructor(
    private val alertaRepository: AlertaRepository,
    private val ventaRepository: VentaRepository,
    private val baseFinancieraRepository: BaseFinancieraRepository,
    private val calcularResumenFinanciero: CalcularResumenFinanciero,
    private val compararGastosPorCategoria: CompararGastosPorCategoria,
    private val reloj: Reloj,
) {

    suspend operator fun invoke(negocioId: Long): List<AlertaNueva> {
        val hoy = reloj.ahora().toLocalDate()
        val nuevas = listOfNotNull(
            caidaDeVentas(negocioId, hoy)?.takeUnless { yaAvisada(negocioId, it.tipo, hoy.minusDays(6)) },
            gastoElevado(negocioId, hoy)?.takeUnless { yaAvisada(negocioId, it.tipo, hoy.withDayOfMonth(1)) },
            metaEnRiesgo(negocioId, hoy)?.takeUnless { yaAvisada(negocioId, it.tipo, hoy.withDayOfMonth(1)) },
        )
        nuevas.forEach { alertaRepository.registrar(negocioId, it) }
        return nuevas
    }

    suspend fun paraTodosLosNegocios(): List<AlertaNueva> =
        alertaRepository.negocios().flatMap { invoke(it) }

    private suspend fun yaAvisada(negocioId: Long, tipo: TipoAlerta, desde: LocalDate): Boolean =
        alertaRepository.huboDesde(negocioId, tipo, desde.atStartOfDay())

    private suspend fun caidaDeVentas(negocioId: Long, hoy: LocalDate): AlertaNueva? {
        val semana = ventaRepository.totalEntre(negocioId, hoy.minusDays(6), hoy).first()
        val anterior = ventaRepository.totalEntre(negocioId, hoy.minusDays(13), hoy.minusDays(7)).first()
        if (anterior <= 0.0) return null
        val caida = (anterior - semana) / anterior
        if (caida < CaidaMinima) return null

        return AlertaNueva(
            tipo = TipoAlerta.CaidaVentas,
            descripcion = "Tus ventas de los últimos 7 días (${pesosEnTexto(semana)}) bajaron " +
                "${(caida * 100).roundToInt()} % frente a la semana anterior (${pesosEnTexto(anterior)}).",
        )
    }

    private suspend fun gastoElevado(negocioId: Long, hoy: LocalDate): AlertaNueva? {
        val variacion = compararGastosPorCategoria(negocioId, hoy).variaciones
            .filter { (it.porcentaje ?: 0.0) >= SubidaMinima }
            .maxByOrNull { it.diferencia }
            ?: return null

        return AlertaNueva(
            tipo = TipoAlerta.GastoElevado,
            descripcion = "Lo que va del mes en ${variacion.nombre} (${pesosEnTexto(variacion.actual)}) está " +
                "${variacion.porcentaje?.roundToInt()} % por encima de los mismos días del mes pasado " +
                "(${pesosEnTexto(variacion.anterior)}).",
        )
    }

    private suspend fun metaEnRiesgo(negocioId: Long, hoy: LocalDate): AlertaNueva? {
        val meta = baseFinancieraRepository.metaActiva(negocioId).first() ?: return null
        val progreso = calcularResumenFinanciero.progresoDeLaMeta(negocioId).first() ?: return null
        if (progreso.cumplida) return null

        if (meta.fechaLimite.isBefore(hoy)) {
            return AlertaNueva(
                tipo = TipoAlerta.MetaEnRiesgo,
                descripcion = "La fecha de tu meta de ahorro (${fecha(meta.fechaLimite)}) ya pasó y te faltan " +
                    "${pesosEnTexto(progreso.falta)}. Puedes ajustarla en Meta y reinversión.",
            )
        }

        val total = ChronoUnit.DAYS.between(meta.fechaCreacion, meta.fechaLimite).coerceAtLeast(1)
        val transcurrido = ChronoUnit.DAYS.between(meta.fechaCreacion, hoy).toDouble() / total
        if (transcurrido < TiempoMinimoDeLaMeta || progreso.fraccion >= transcurrido - AtrasoMaximoDeLaMeta) return null

        return AlertaNueva(
            tipo = TipoAlerta.MetaEnRiesgo,
            descripcion = "Llevas el ${(progreso.fraccion * 100).roundToInt()} % de tu meta de ahorro y ya pasó " +
                "el ${(transcurrido * 100).roundToInt()} % del tiempo. A este ritmo no llegas para el " +
                "${fecha(meta.fechaLimite)}.",
        )
    }

    private fun fecha(dia: LocalDate): String =
        dia.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-CO")))
}
