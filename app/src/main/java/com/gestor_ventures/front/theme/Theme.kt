package com.gestor_ventures.front.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimarySoftLight,
    onPrimaryContainer = PrimaryLight,
    // Sin estos, Material pinta sus morados por defecto en controles como el Slider.
    secondary = PrimaryVariantLight,
    onSecondary = OnPrimaryLight,
    secondaryContainer = PrimarySoftLight,
    onSecondaryContainer = PrimaryLight,
    tertiary = PrimaryLight,
    onTertiary = OnPrimaryLight,
    tertiaryContainer = PrimarySoftLight,
    onTertiaryContainer = PrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    // Diálogos y hojas usan estos roles; sin definirlos salen con el lila de Material.
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceLight,
    surfaceContainerHighest = BackgroundLight,
    surfaceTint = PrimaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = DangerLight,
    onError = Color.White,
    errorContainer = DangerSoftLight,
    onErrorContainer = DangerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimarySoftDark,
    onPrimaryContainer = PrimaryDark,
    secondary = PrimaryVariantDark,
    onSecondary = OnPrimaryDark,
    secondaryContainer = PrimarySoftDark,
    onSecondaryContainer = PrimaryDark,
    tertiary = PrimaryDark,
    onTertiary = OnPrimaryDark,
    tertiaryContainer = PrimarySoftDark,
    onTertiaryContainer = PrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = BackgroundDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDark,
    surfaceContainerHighest = BackgroundDark,
    surfaceTint = PrimaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = DangerDark,
    onError = Color.White,
    errorContainer = DangerSoftDark,
    onErrorContainer = DangerDark,
)

private val LocalGestorColors = staticCompositionLocalOf { LightGestorColors }

/**
 * Cambia los colores de marca del esquema por el que eligió el usuario. El texto que va encima
 * y el fondo suave se calculan: un pastel claro necesita tinta oscura para leerse.
 */
private fun ColorScheme.conMarca(marca: Color?, oscuro: Boolean): ColorScheme {
    if (marca == null) return this
    val tinta = tintaSobre(marca)
    val fondoSuave = suave(marca, oscuro)
    return copy(
        primary = marca,
        onPrimary = tinta,
        primaryContainer = fondoSuave,
        onPrimaryContainer = tintaSobre(fondoSuave),
        secondary = variante(marca, oscuro),
        onSecondary = tinta,
        secondaryContainer = fondoSuave,
        onSecondaryContainer = tintaSobre(fondoSuave),
        tertiary = marca,
        onTertiary = tinta,
        tertiaryContainer = fondoSuave,
        onTertiaryContainer = tintaSobre(fondoSuave),
        surfaceTint = marca,
    )
}

private fun GestorColors.conMarca(
    marca: Color?,
    oscuro: Boolean,
    superficie: Color,
): GestorColors {
    if (marca == null) return this
    return copy(
        acento = acentoSobre(marca, superficie, oscuro),
        primaryVariant = variante(marca, oscuro),
    )
}

/**
 * Tema de la app. No usa color dinámico (Android 12+) para que la marca se vea igual en
 * todos los dispositivos.
 *
 * [colorMarca] es el color que el usuario eligió para su negocio (HU-05), en hexadecimal. Si
 * viene nulo —o es el azul de la app— todo se ve como se diseñó desde el principio.
 */
@Composable
fun GestorVenturesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorMarca: String? = null,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkColorScheme else LightColorScheme
    val baseGestor = if (darkTheme) DarkGestorColors else LightGestorColors
    val marca = hexAColor(colorMarca)

    val colorScheme = remember(base, marca) { base.conMarca(marca, darkTheme) }
    val gestorColors = remember(baseGestor, marca) {
        baseGestor.conMarca(marca, darkTheme, base.surface)
    }

    CompositionLocalProvider(LocalGestorColors provides gestorColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

/** Acceso a los colores propios del sistema de diseño, ej. `GestorVenturesTheme.colors.success`. */
object GestorVenturesTheme {
    val colors: GestorColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGestorColors.current
}
