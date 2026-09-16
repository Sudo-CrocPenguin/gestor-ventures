package com.gestor_ventures.front.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
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
 * Tema de la app. No usa color dinámico (Android 12+) para que la marca se vea igual en
 * todos los dispositivos.
 */
@Composable
fun GestorVenturesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val gestorColors = if (darkTheme) DarkGestorColors else LightGestorColors

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
