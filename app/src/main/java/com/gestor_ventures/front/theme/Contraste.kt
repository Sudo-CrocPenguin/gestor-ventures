package com.gestor_ventures.front.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Cálculos de contraste para que cualquier color de marca que elija el usuario siga siendo
 * legible. Es el mismo criterio del mockup: si sobre el color no se lee el blanco, se usa una
 * versión oscura del mismo tono.
 *
 * El umbral 4.5 es el mínimo que pide la guía de accesibilidad WCAG para texto normal.
 */
private const val ContrasteMinimo = 4.5

/** Luminancia relativa según WCAG: cuánta luz emite un color a ojos de una persona. */
internal fun luminancia(color: Color): Double {
    fun canal(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * canal(color.red) + 0.7152 * canal(color.green) + 0.0722 * canal(color.blue)
}

/** Relación de contraste entre dos colores: va de 1 (iguales) a 21 (blanco contra negro). */
fun contraste(uno: Color, otro: Color): Double {
    val a = luminancia(uno)
    val b = luminancia(otro)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}

/**
 * Color de texto que se lee sobre [fondo]: blanco si alcanza, y si no, el mismo tono oscurecido
 * hasta que contraste lo suficiente.
 */
fun tintaSobre(fondo: Color): Color {
    if (contraste(fondo, Color.White) >= ContrasteMinimo) return Color.White

    val hsl = aHsl(fondo)
    var luz = min(hsl.luz, 0.40f)
    val saturacion = max(hsl.saturacion, 0.25f)
    repeat(45) {
        val candidato = deHsl(hsl.tono, saturacion, luz)
        if (contraste(candidato, fondo) >= ContrasteMinimo) return candidato
        luz -= 0.02f
        if (luz <= 0.04f) return@repeat
    }
    return Color(0xFF111111)
}

/**
 * Versión del color de marca que sí se puede usar como texto o ícono sobre [fondo].
 *
 * Un pastel claro funciona de relleno, pero como letra sobre blanco no se lee: en ese caso se
 * oscurece (o se aclara, en modo oscuro) hasta que contraste lo suficiente.
 */
fun acentoSobre(marca: Color, fondo: Color, oscuro: Boolean): Color {
    if (contraste(marca, fondo) >= ContrasteMinimo) return marca

    val hsl = aHsl(marca)
    var luz = hsl.luz
    val paso = if (oscuro) 0.02f else -0.02f
    repeat(45) {
        luz = (luz + paso).coerceIn(0.04f, 0.96f)
        val candidato = deHsl(hsl.tono, max(hsl.saturacion, 0.25f), luz)
        if (contraste(candidato, fondo) >= ContrasteMinimo) return candidato
    }
    return if (oscuro) Color.White else Color(0xFF111111)
}

/** Versión suave del color, para fondos de tarjetas y resaltados. */
fun suave(color: Color, oscuro: Boolean): Color {
    val hsl = aHsl(color)
    return if (oscuro) {
        deHsl(hsl.tono, min(hsl.saturacion, 0.32f), 0.14f)
    } else {
        deHsl(hsl.tono, min(hsl.saturacion, 0.55f), 0.94f)
    }
}

/** Variante un poco más intensa, para el degradado de la barra de progreso. */
fun variante(color: Color, oscuro: Boolean): Color {
    val hsl = aHsl(color)
    val luz = if (oscuro) min(hsl.luz + 0.12f, 0.92f) else max(hsl.luz - 0.10f, 0.08f)
    return deHsl(hsl.tono, hsl.saturacion, luz)
}

internal data class Hsl(val tono: Float, val saturacion: Float, val luz: Float)

internal fun aHsl(color: Color): Hsl {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maximo = maxOf(r, g, b)
    val minimo = minOf(r, g, b)
    val delta = maximo - minimo
    val luz = (maximo + minimo) / 2f

    if (delta == 0f) return Hsl(0f, 0f, luz)

    val saturacion = delta / (1f - abs(2f * luz - 1f))
    val tono = when (maximo) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return Hsl(if (tono < 0f) tono + 360f else tono, saturacion, luz)
}

internal fun deHsl(tono: Float, saturacion: Float, luz: Float): Color {
    val c = (1f - abs(2f * luz - 1f)) * saturacion
    val x = c * (1f - abs((tono / 60f) % 2f - 1f))
    val m = luz - c / 2f
    val (r, g, b) = when {
        tono < 60f -> Triple(c, x, 0f)
        tono < 120f -> Triple(x, c, 0f)
        tono < 180f -> Triple(0f, c, x)
        tono < 240f -> Triple(0f, x, c)
        tono < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
    )
}
