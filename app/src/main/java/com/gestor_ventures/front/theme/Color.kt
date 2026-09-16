package com.gestor_ventures.front.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Tokens del sistema de diseño (mockups/fase1-navegable.html). Son internal a propósito:
// las pantallas leen los colores desde MaterialTheme.colorScheme o GestorVenturesTheme.colors.

// Tema claro
internal val PrimaryLight = Color(0xFF1B2A4A)
internal val PrimaryVariantLight = Color(0xFF2A4166)
internal val PrimarySoftLight = Color(0xFFE7ECF5)
internal val OnPrimaryLight = Color(0xFFFFFFFF)
internal val BackgroundLight = Color(0xFFF6F5F2)
internal val SurfaceLight = Color(0xFFFFFFFF)
internal val TextPrimaryLight = Color(0xFF22252C)
internal val TextSecondaryLight = Color(0xFF5C6270)
internal val BorderLight = Color(0xFFDEE1E8)
internal val SuccessLight = Color(0xFF3B6D11)
internal val SuccessSoftLight = Color(0xFFEAF1E1)
internal val WarningLight = Color(0xFF7A5A12)
internal val WarningSoftLight = Color(0xFFF3ECD9)
internal val DangerLight = Color(0xFF9A2B2B)
internal val DangerSoftLight = Color(0xFFF5E6E6)

// Tema oscuro
internal val PrimaryDark = Color(0xFF4C6FA8)
internal val PrimaryVariantDark = Color(0xFF6E8CBE)
internal val PrimarySoftDark = Color(0xFF1B2436)
internal val OnPrimaryDark = Color(0xFF0E1116)
internal val BackgroundDark = Color(0xFF0E1116)
internal val SurfaceDark = Color(0xFF161A22)
internal val TextPrimaryDark = Color(0xFFF1EFE8)
internal val TextSecondaryDark = Color(0xFFA8ADB8)
internal val BorderDark = Color(0xFF2A2E38)
internal val SuccessDark = Color(0xFF8FBE5C)
internal val SuccessSoftDark = Color(0xFF20301A)
internal val WarningDark = Color(0xFFD9BC72)
internal val WarningSoftDark = Color(0xFF312A16)
internal val DangerDark = Color(0xFFD98A8A)
internal val DangerSoftDark = Color(0xFF331C1C)

/** Colores del sistema de diseño que no tienen equivalente en el ColorScheme de Material 3. */
@Immutable
data class GestorColors(
    /**
     * Color de marca para textos e íconos sobre fondos claros. Con el azul de la app es el
     * mismo primario; con un pastel es su versión oscurecida, para que se lea.
     */
    val acento: Color,
    val primaryVariant: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
)

internal val LightGestorColors = GestorColors(
    acento = PrimaryLight,
    primaryVariant = PrimaryVariantLight,
    success = SuccessLight,
    successContainer = SuccessSoftLight,
    warning = WarningLight,
    warningContainer = WarningSoftLight,
)

internal val DarkGestorColors = GestorColors(
    acento = PrimaryDark,
    primaryVariant = PrimaryVariantDark,
    success = SuccessDark,
    successContainer = SuccessSoftDark,
    warning = WarningDark,
    warningContainer = WarningSoftDark,
)
