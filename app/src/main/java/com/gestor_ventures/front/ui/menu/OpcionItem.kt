package com.gestor_ventures.front.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fila de opción del menú. Se pinta con [colorResaltado] cuando el puntero pasa por encima
 * (mouse) o mientras se mantiene presionada (dedo), como el `:hover` del mockup.
 *
 * Para acciones delicadas se le pasan los colores de error, como hace "Cerrar sesión".
 */
@Composable
fun OpcionItem(
    opcion: OpcionMenu,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    colorResaltado: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val encima by interactionSource.collectIsHoveredAsState()
    val presionada by interactionSource.collectIsPressedAsState()
    val fondo by animateColorAsState(
        targetValue = if (encima || presionada) colorResaltado else Color.Transparent,
        label = "fondoOpcionMenu",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(fondo)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 19.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(opcion.iconRes),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(opcion.labelRes),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = contentColor,
        )
    }
}
