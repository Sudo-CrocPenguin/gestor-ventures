package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.ErrorAsistente
import com.gestor_ventures.back.model.MaxCaracteresPregunta
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoAsistente
import com.gestor_ventures.back.model.ResumenFinanciero
import com.gestor_ventures.back.model.SeleccionDePeriodo
import com.gestor_ventures.back.model.Venta
import com.gestor_ventures.back.model.personalizado
import com.gestor_ventures.back.repository.AsistenteRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.flow.first
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToLong

private val LocaleColombia: Locale = Locale.forLanguageTag("es-CO")

/** Un mes de ventas día por día: alcanza para "¿qué día vendo más?" sin mandar de más. */
private const val DiasDeDetalle = 31L

/** Dos meses por semanas: lo justo para comparar esta semana con las anteriores. */
private const val SemanasDeDetalle = 8L

private const val ProductosDestacados = 5

/**
 * HU-34 y HU-36. Le pregunta al asistente por las ventas del negocio y por cuánto puede
 * reinvertir, con sus números reales.
 *
 * Vive en `usecase` porque cruza varios repositorios: arma el contexto con el resumen de HU-16
 * y el detalle de ventas, y se lo pasa a [AsistenteRepository].
 *
 * El periodo no se elige en pantalla: se le mandan al asistente varios cortes (este mes, el
 * anterior, las últimas semanas y los últimos días) y él responde con el que corresponda a la
 * pregunta. Así el usuario pregunta como hablaría: "¿cómo me fue la semana pasada?".
 */
class ConsultarAsistente @Inject constructor(
    private val asistenteRepository: AsistenteRepository,
    private val negocioRepository: NegocioRepository,
    private val ventaRepository: VentaRepository,
    private val calcularResumenFinanciero: CalcularResumenFinanciero,
    private val reloj: Reloj,
) {

    suspend operator fun invoke(negocioId: Long, sesionId: Long, pregunta: String): ResultadoAsistente {
        val limpia = pregunta.trim()
        if (limpia.isEmpty()) return ResultadoAsistente.Fallo(ErrorAsistente.PreguntaVacia)
        if (limpia.length > MaxCaracteresPregunta) return ResultadoAsistente.Fallo(ErrorAsistente.PreguntaMuyLarga)

        return asistenteRepository.preguntar(
            negocioId = negocioId,
            sesionId = sesionId,
            pregunta = limpia,
            instrucciones = instruccionesPara(negocioId),
        )
    }

    /**
     * Las reglas del asistente y los datos del negocio, en texto.
     *
     * Las reglas importan tanto como los datos: responder solo con lo que está acá, no inventar
     * cifras y decir cuándo un periodo no está, en vez de adivinarlo.
     *
     * HU-36. El monto para reinvertir no lo calcula el asistente: se lo da ya hecho
     * [ResumenFinanciero], con la misma regla de HU-09 que usa el resumen de Finanzas. Así la
     * cifra del chat y la de la pantalla son siempre la misma.
     */
    private suspend fun instruccionesPara(negocioId: Long): String {
        val hoy = reloj.ahora().toLocalDate()
        val negocio = negocioRepository.observarNegocio(negocioId).first()
        val mesAnterior = YearMonth.from(hoy).minusMonths(1)

        val resumenDelMes = calcularResumenFinanciero(
            negocioId,
            SeleccionDePeriodo.Rapido(PeriodoPredefinido.Mes),
        ).first()
        val resumenDelMesAnterior = personalizado(mesAnterior.atDay(1), mesAnterior.atEndOfMonth())
            ?.let { calcularResumenFinanciero(negocioId, it).first() }

        val desde = minOf(hoy.minusDays(DiasDeDetalle - 1), hoy.with(DayOfWeek.MONDAY).minusWeeks(SemanasDeDetalle - 1))
        val ventas = ventaRepository.ventasEntre(negocioId, desde, hoy).first()

        return buildString {
            appendLine(
                "Eres el asistente financiero de la app Gestor Ventures. Hablas con un emprendedor " +
                    "colombiano sobre su negocio. Responde en español, de forma clara y breve " +
                    "(máximo 150 palabras), tuteando y sin tecnicismos.",
            )
            appendLine(
                "Usa SOLO los datos de abajo: son los registros reales del negocio. No inventes " +
                    "cifras. Si la pregunta es sobre un periodo que no está en los datos, dilo y " +
                    "explica qué periodos sí puedes analizar. Los montos están en pesos colombianos.",
            )
            appendLine(
                "Cuando compares periodos, di si subió o bajó y cuánto. Si ves algo que el " +
                    "emprendedor debería saber (un día muy flojo, un producto que jala las ventas), " +
                    "menciónalo en una frase.",
            )
            appendLine(
                "Si te preguntan cuánto pueden reinvertir, usa el monto de reinversión del mes en " +
                    "curso: es el porcentaje de reinversión que el emprendedor configuró aplicado a " +
                    "la ganancia estimada. Explica la cuenta en una línea. Si la ganancia es cero o " +
                    "negativa, di que este mes no hay qué reinvertir y por qué. Si el porcentaje es " +
                    "0 %, dile que lo puede configurar en Meta y reinversión, en el menú.",
            )
            appendLine()
            appendLine("Hoy es ${fechaLarga(hoy)}.")
            negocio?.let { appendLine("Negocio: ${it.nombre} (${it.categoria}).") }
            appendLine()
            appendLine("## Resumen de este mes (${mesLargo(YearMonth.from(hoy))}, hasta hoy)")
            appendResumen(resumenDelMes)
            resumenDelMesAnterior?.let {
                appendLine()
                appendLine("## Resumen del mes anterior (${mesLargo(mesAnterior)})")
                appendResumen(it)
            }
            appendLine()
            appendLine("## Ventas por semana (lunes a domingo)")
            ventasPorSemana(ventas, hoy).forEach { (lunes, total) ->
                appendLine("- Semana del ${fechaCorta(lunes)} al ${fechaCorta(lunes.plusDays(6))}: ${pesos(total)}")
            }
            appendLine()
            appendLine("## Ventas por día (últimos $DiasDeDetalle días)")
            ventasPorDia(ventas, hoy).forEach { (dia, total) ->
                appendLine("- ${fechaCorta(dia)} (${diaDeLaSemana(dia)}): ${pesos(total.first)} en ${total.second} ventas")
            }
            appendLine()
            appendLine("## Lo más vendido (últimos $DiasDeDetalle días)")
            val destacados = productosDestacados(ventas, hoy)
            if (destacados.isEmpty()) appendLine("- Sin ventas con producto registrado.")
            destacados.forEach { (producto, total) ->
                appendLine("- $producto: ${pesos(total.first)} en ${total.second} ventas")
            }
        }
    }

    /**
     * El resumen de un mes en líneas cortas. Van también las obligaciones y el apartado de la
     * meta (HU-36): sin ellos, el asistente no podría explicar por qué lo disponible es menos
     * que la ganancia.
     */
    private fun StringBuilder.appendResumen(resumen: ResumenFinanciero) {
        appendLine("- Ingresos por ventas: ${pesos(resumen.ingresos)}")
        appendLine("- Gastos: ${pesos(resumen.gastos)}")
        appendLine("- Costos de producción: ${pesos(resumen.costos)}")
        appendLine("- Gastos fijos del periodo: ${pesos(resumen.gastosFijos)}")
        appendLine("- Ganancia estimada: ${pesos(resumen.ganancia)}")
        resumen.margen?.let { appendLine("- Margen: ${(it * 100).roundToLong()} %") }
        appendLine("- Porcentaje de reinversión configurado: ${resumen.porcentajeReinversion.roundToLong()} %")
        appendLine("- Monto para reinvertir: ${pesos(resumen.reinversion)}")
        appendLine("- Obligaciones pendientes: ${pesos(resumen.obligacionesPendientes)}")
        appendLine("- Apartado para la meta de ahorro: ${pesos(resumen.apartadoParaMeta)}")
        appendLine("- Dinero disponible: ${pesos(resumen.disponible)}")
    }

    private fun ventasPorSemana(ventas: List<Venta>, hoy: LocalDate): List<Pair<LocalDate, Double>> {
        val lunesActual = hoy.with(DayOfWeek.MONDAY)
        return (SemanasDeDetalle - 1 downTo 0).map { atras ->
            val lunes = lunesActual.minusWeeks(atras)
            lunes to ventas
                .filter { val dia = it.fechaHora.toLocalDate(); !dia.isBefore(lunes) && !dia.isAfter(lunes.plusDays(6)) }
                .sumOf { it.monto }
        }
    }

    private fun ventasPorDia(ventas: List<Venta>, hoy: LocalDate): List<Pair<LocalDate, Pair<Double, Int>>> =
        (DiasDeDetalle - 1 downTo 0).map { atras ->
            val dia = hoy.minusDays(atras)
            val delDia = ventas.filter { it.fechaHora.toLocalDate() == dia }
            dia to (delDia.sumOf { it.monto } to delDia.size)
        }

    /**
     * Lo que más plata dejó en el último mes. "Torta" y "torta" son el mismo producto, así que
     * se agrupa sin importar mayúsculas.
     */
    private fun productosDestacados(ventas: List<Venta>, hoy: LocalDate): List<Pair<String, Pair<Double, Int>>> {
        val desde = hoy.minusDays(DiasDeDetalle - 1)
        return ventas
            .filter { !it.fechaHora.toLocalDate().isBefore(desde) }
            .mapNotNull { venta -> venta.productoServicio?.trim()?.takeIf { it.isNotEmpty() }?.let { it to venta } }
            .groupBy({ it.first.lowercase() }, { it })
            .map { (_, grupo) -> grupo.first().first to (grupo.sumOf { it.second.monto } to grupo.size) }
            .sortedByDescending { it.second.first }
            .take(ProductosDestacados)
    }

    /** "$ 1.240.000", como lo escribiría el emprendedor. */
    private fun pesos(monto: Double): String {
        val simbolos = DecimalFormatSymbols(LocaleColombia).apply { groupingSeparator = '.' }
        return "$ " + DecimalFormat("#,##0", simbolos).format(monto.roundToLong())
    }

    private fun fechaLarga(fecha: LocalDate): String =
        fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", LocaleColombia))

    private fun fechaCorta(fecha: LocalDate): String =
        fecha.format(DateTimeFormatter.ofPattern("d MMM", LocaleColombia))

    private fun diaDeLaSemana(fecha: LocalDate): String =
        fecha.format(DateTimeFormatter.ofPattern("EEEE", LocaleColombia))

    private fun mesLargo(mes: YearMonth): String =
        mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LocaleColombia))
}
