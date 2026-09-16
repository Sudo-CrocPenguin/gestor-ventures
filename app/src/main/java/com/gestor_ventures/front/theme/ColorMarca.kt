package com.gestor_ventures.front.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gestor_ventures.R

/**
 * HU-05. Paleta de marca del onboarding (mockups/fase3-navegable.html).
 *
 * [hex] nulo es el color propio de la app: si el usuario elige "Azul marino", todo se sigue
 * viendo como lo diseñamos. Los demás son pasteles claros; el texto que va encima se calcula
 * en [tintaSobre] para que siempre se lea.
 */
enum class ColorMarca(val hex: String?, @param:StringRes val nombreRes: Int) {
    Sistema(null, R.string.color_azul_marino),
    RosaCuarzo("#F4C2C2", R.string.color_rosa_cuarzo),
    Durazno("#F8CBA6", R.string.color_durazno),
    Mantequilla("#F5E6A8", R.string.color_mantequilla),
    Salvia("#CFE0C3", R.string.color_salvia),
    Menta("#B8E0D2", R.string.color_menta),
    Cielo("#BBDDF2", R.string.color_cielo),
    Lavanda("#D5C8EC", R.string.color_lavanda),
    Lila("#E7C6E8", R.string.color_lila),
    Arena("#E8D9C5", R.string.color_arena),
}

/**
 * Color con el que se dibuja la muestra del predeterminado: el azul propio de la app, que no
 * cambia aunque el usuario esté previsualizando otro color.
 */
@Composable
fun colorBaseDeMarca(): Color = if (isSystemInDarkTheme()) PrimaryDark else PrimaryLight

/** Vuelve del hexadecimal guardado en la base de datos a la opción de la paleta. */
fun colorMarcaDeHex(hex: String?): ColorMarca =
    ColorMarca.entries.firstOrNull { it.hex.equals(hex, ignoreCase = true) } ?: ColorMarca.Sistema

/** Convierte "#B8E0D2" en un Color; null si el texto no es un hexadecimal válido. */
fun hexAColor(hex: String?): Color? {
    val limpio = hex?.trim()?.removePrefix("#") ?: return null
    if (limpio.length != 6) return null
    val valor = limpio.toLongOrNull(radix = 16) ?: return null
    return Color(valor or 0xFF000000L)
}
