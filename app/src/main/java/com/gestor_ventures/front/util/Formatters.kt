package com.gestor_ventures.front.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LocaleColombia: Locale = Locale.forLanguageTag("es-CO")

private fun formatoMiles(): DecimalFormat {
    val symbols = DecimalFormatSymbols(LocaleColombia).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    return DecimalFormat("#,##0", symbols)
}

/** 148500.0 → "$ 148.500" (pesos colombianos, sin decimales). */
fun formatPesos(monto: Double): String = "$ " + formatoMiles().format(monto)

/** 25000 → "25.000". Para campos donde el "$" va aparte, como el de registrar venta. */
fun formatMiles(valor: Long): String = formatoMiles().format(valor)

/** 2026-09-02 → "Miércoles 2 de septiembre". */
fun formatLongDate(fecha: LocalDate): String =
    fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LocaleColombia))
        .replaceFirstChar { it.titlecase(LocaleColombia) }

/** 2026-12-31 → "dic 2026". Para resúmenes cortos. */
fun formatMesYAnio(fecha: LocalDate): String =
    fecha.format(DateTimeFormatter.ofPattern("MMM yyyy", LocaleColombia))

/** 08:00 → "8:00". */
fun formatHour(hora: LocalTime): String =
    hora.format(DateTimeFormatter.ofPattern("H:mm", LocaleColombia))
