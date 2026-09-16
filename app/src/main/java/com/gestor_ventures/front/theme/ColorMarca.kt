package com.gestor_ventures.front.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gestor_ventures.R
import kotlin.math.roundToInt

/**
 * HU-05. Atajos de color de marca del onboarding (mockups/fase3-navegable.html).
 *
 * Son solo los tres de un toque: el azul propio de la app y dos pasteles. Quien quiera el color
 * exacto de su marca lo elige en el selector, y por eso lo que se guarda en la base de datos es
 * el hexadecimal y no el nombre de la opción: así cabe cualquier color.
 *
 * [hex] nulo es el color propio de la app: si el usuario deja "Azul marino", todo se sigue viendo
 * como lo diseñamos. El texto que va encima de cada color se calcula en [tintaSobre] para que
 * siempre se lea.
 */
enum class ColorMarca(val hex: String?, @param:StringRes val nombreRes: Int) {
    Sistema(null, R.string.color_azul_marino),
    RosaCuarzo("#F4C2C2", R.string.color_rosa_cuarzo),
    Salvia("#CFE0C3", R.string.color_salvia),
}

/**
 * Color con el que se dibuja la muestra del predeterminado: el azul propio de la app, que no
 * cambia aunque el usuario esté previsualizando otro color.
 */
@Composable
fun colorBaseDeMarca(): Color = if (isSystemInDarkTheme()) PrimaryDark else PrimaryLight

/**
 * El atajo al que corresponde un hexadecimal guardado, o null si el usuario eligió un color
 * propio en el selector.
 */
fun presetDeHex(hex: String?): ColorMarca? =
    ColorMarca.entries.firstOrNull { it.hex.equals(hex, ignoreCase = true) }

/** Convierte "#B8E0D2" en un Color; null si el texto no es un hexadecimal válido. */
fun hexAColor(hex: String?): Color? {
    val limpio = hex?.trim()?.removePrefix("#") ?: return null
    if (limpio.length != 6) return null
    val valor = limpio.toLongOrNull(radix = 16) ?: return null
    return Color(valor or 0xFF000000L)
}

/** El camino de vuelta: el hexadecimal que se guarda en la base de datos, como "#B8E0D2". */
fun colorAHex(color: Color): String {
    fun canal(v: Float) = (v * 255f).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(canal(color.red), canal(color.green), canal(color.blue))
}
