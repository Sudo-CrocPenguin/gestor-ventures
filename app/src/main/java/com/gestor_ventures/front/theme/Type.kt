package com.gestor_ventures.front.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R

/**
 * Fuente de toda la app: Inter, la misma del mockup (mockups/fase1-navegable.html).
 * Va empaquetada en `res/font/`, así se ve igual en cualquier teléfono y sin internet
 * (licencia SIL Open Font, copia en `licenses/Inter-OFL.txt`).
 *
 * Cambiar esta línea cambia la tipografía de todas las pantallas, porque ninguna elige
 * fuente por su cuenta.
 */
private val FuenteApp = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Estilos de Material 3 que la app no personaliza; solo se les cambia la fuente. */
private val Base = Typography()

val Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = FuenteApp),
    displayMedium = Base.displayMedium.copy(fontFamily = FuenteApp),
    displaySmall = Base.displaySmall.copy(fontFamily = FuenteApp),
    headlineLarge = Base.headlineLarge.copy(fontFamily = FuenteApp),
    headlineMedium = Base.headlineMedium.copy(fontFamily = FuenteApp),
    headlineSmall = Base.headlineSmall.copy(fontFamily = FuenteApp),
    titleLarge = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FuenteApp,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

/** Estilo para montos y porcentajes: monoespaciada con cifras tabulares, para que alineen. */
val NumericTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
    letterSpacing = (-0.2).sp,
)
