package com.gestor_ventures.front.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ButtonShape = RoundedCornerShape(14.dp)

/**
 * Acción rápida: ícono en recuadro + texto. Con [highlighted] se pinta con el color
 * primario para marcar la acción principal de la pantalla.
 */
@Composable
fun QuickActionButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (highlighted) colors.primary else colors.surface
    val borderColor = if (highlighted) colors.primary else colors.outline
    val contentColor = if (highlighted) colors.onPrimary else colors.onSurface
    val iconContainerColor = if (highlighted) colors.onPrimary.copy(alpha = 0.18f) else colors.primaryContainer
    val iconColor = if (highlighted) colors.onPrimary else colors.primary

    Row(
        modifier = modifier
            .clip(ButtonShape)
            .background(containerColor)
            .border(1.dp, borderColor, ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconContainerColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp),
            )
        }
        // Centrado en el espacio que queda junto al ícono; si no cabe, el texto se encoge.
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 10.sp,
                maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
            ),
        )
    }
}
