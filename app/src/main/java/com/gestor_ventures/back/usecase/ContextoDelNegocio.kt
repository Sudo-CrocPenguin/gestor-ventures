package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResumenFinanciero
import com.gestor_ventures.back.model.SeleccionDePeriodo
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.model.Venta
import com.gestor_ventures.back.model.personalizado
import com.gestor_ventures.back.model.pesosEnTexto
import com.gestor_ventures.back.repository.ClienteRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.flow.first
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

/** Los que más compran: a quienes se les puede premiar la fidelidad. */
private const val ClientesDestacados = 5

/** Un mes sin comprar ya es un cliente que se está yendo. */
private const val DiasSinComprar = 30L

/**
 * HU-34 a HU-37. Las reglas del asistente y los datos reales del negocio, en texto.
 *
 * HU-35. También lleva cómo compran los clientes (HU-30), que es lo que necesita una promoción
 * para tener a quién dirigirse.
 *
 * Vive aparte porque lo usan varias preguntas al asistente: el chat (HU-34, HU-36, HU-37) y
 * las sugerencias de promociones (HU-35). Todos tienen que ver los mismos números.
 *
 * El periodo no se elige en pantalla: se le mandan al asistente varios cortes (este mes, el
 * anterior, las últimas semanas y los últimos días) y él responde con el que corresponda a la
 * pregunta. Así el usuario pregunta como hablaría: "¿cómo me fue la semana pasada?".
 */
class ContextoDelNegocio @Inject constructor(
    private val negocioRepository: NegocioRepository,
    private val ventaRepository: VentaRepository,
    private val calcularResumenFinanciero: CalcularResumenFinanciero,
    private val compararGastosPorCategoria: CompararGastosPorCategoria,
    private val clienteRepository: ClienteRepository,
    private val reloj: Reloj,
) {

    /**
     * Las reglas del asistente y los datos del negocio, en texto.
     *
     * Las reglas importan tanto como los datos: responder solo con lo que está acá, no inventar
     * cifras y decir cuándo un periodo no está, en vez de adivinarlo.
     *
     * HU-36. El monto para reinvertir no lo calcula el asistente: se lo da ya hecho
     * [ResumenFinanciero], con la misma regla de HU-09 que usa el resumen de Finanzas. Así la
     * cifra del chat y la de la pantalla son siempre la misma.
     *
     * HU-37. Igual con los gastos: la comparación por categoría llega hecha desde
     * [CompararGastosPorCategoria], ordenada de lo que más subió a lo que más bajó, para que el
     * asistente nombre primero lo que más pesa.
     */
    suspend fun instruccionesPara(negocioId: Long): String {
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
        val comparacion = compararGastosPorCategoria(negocioId, hoy)
        val clientes = clienteRepository.clientesDeNegocio(negocioId).first()

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
            appendLine(
                "Si te preguntan en qué están gastando demasiado, usa la comparación de gastos y " +
                    "costos por categoría: nombra primero las categorías que más subieron, con " +
                    "cuánto y en qué porcentaje, y sugiere en una frase qué revisar. Aclara que " +
                    "comparas este mes hasta hoy contra los mismos días del mes anterior. Si nada " +
                    "subió, dilo como una buena noticia.",
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
            appendLine(
                "## Gastos y costos por categoría: ${fechaCorta(comparacion.actual.desde)} al " +
                    "${fechaCorta(comparacion.actual.hasta)} contra ${fechaCorta(comparacion.anterior.desde)} " +
                    "al ${fechaCorta(comparacion.anterior.hasta)} (ordenadas de lo que más subió a lo que más bajó)",
            )
            if (comparacion.variaciones.isEmpty()) appendLine("- Sin gastos ni costos en los dos periodos.")
            comparacion.variaciones.forEach { variacion ->
                val tipo = if (variacion.tipo == TipoCategoria.GASTO) "gasto" else "costo"
                val porcentaje = variacion.porcentaje?.let { " (${if (it >= 0) "+" else ""}${it.roundToLong()} %)" }
                    ?: if (variacion.anterior <= 0.0 && variacion.actual > 0.0) " (nueva este mes)" else ""
                appendLine(
                    "- ${variacion.nombre} [$tipo]: ${pesos(variacion.actual)} ahora, " +
                        "${pesos(variacion.anterior)} antes, diferencia ${pesos(variacion.diferencia)}$porcentaje",
                )
            }
            appendLine()
            appendLine("## Lo más vendido (últimos $DiasDeDetalle días)")
            val destacados = productosDestacados(ventas, hoy)
            if (destacados.isEmpty()) appendLine("- Sin ventas con producto registrado.")
            destacados.forEach { (producto, total) ->
                appendLine("- $producto: ${pesos(total.first)} en ${total.second} ventas")
            }
            appendLine()
            appendClientes(clientes, ventas, desde, hoy)
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

    /**
     * HU-35. Los clientes del negocio: cuántos hay, quiénes compran más y quiénes dejaron de
     * comprar. Solo cuentan las ventas con cliente registrado; las ventas rápidas no tienen.
     *
     * Van con su nombre, para que el asistente pueda sugerir a quién escribirle. Eso quiere
     * decir que esos nombres salen del teléfono hacia Gemini.
     */
    private fun StringBuilder.appendClientes(
        clientes: List<Cliente>,
        ventas: List<Venta>,
        desde: LocalDate,
        hoy: LocalDate,
    ) {
        appendLine("## Clientes (desde el ${fechaCorta(desde)})")
        appendLine("- Clientes registrados: ${clientes.size}")
        val conCliente = ventas.filter { it.clienteId != null }
        appendLine("- Ventas con cliente registrado: ${conCliente.size} de ${ventas.size}")
        if (clientes.isEmpty()) return

        val nombres = clientes.associate { it.id to it.nombre }
        val porCliente = conCliente.groupBy { it.clienteId }
        porCliente.entries
            .sortedByDescending { (_, compras) -> compras.sumOf { it.monto } }
            .take(ClientesDestacados)
            .forEach { (clienteId, compras) ->
                val nombre = nombres[clienteId] ?: return@forEach
                appendLine("- Cliente frecuente: $nombre, ${pesos(compras.sumOf { it.monto })} en ${compras.size} compras")
            }

        val limite = hoy.minusDays(DiasSinComprar)
        porCliente
            .filter { (_, compras) -> compras.all { it.fechaHora.toLocalDate().isBefore(limite) } }
            .forEach { (clienteId, compras) ->
                val nombre = nombres[clienteId] ?: return@forEach
                val ultima = compras.maxOf { it.fechaHora.toLocalDate() }
                appendLine("- Dejó de comprar: $nombre, última compra el ${fechaCorta(ultima)}")
            }

        val sinCompras = clientes.count { it.id !in porCliente.keys }
        if (sinCompras > 0) appendLine("- Clientes sin compras en este periodo: $sinCompras")
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
    private fun pesos(monto: Double): String = pesosEnTexto(monto)

    private fun fechaLarga(fecha: LocalDate): String =
        fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", LocaleColombia))

    private fun fechaCorta(fecha: LocalDate): String =
        fecha.format(DateTimeFormatter.ofPattern("d MMM", LocaleColombia))

    private fun diaDeLaSemana(fecha: LocalDate): String =
        fecha.format(DateTimeFormatter.ofPattern("EEEE", LocaleColombia))

    private fun mesLargo(mes: YearMonth): String =
        mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LocaleColombia))
}
