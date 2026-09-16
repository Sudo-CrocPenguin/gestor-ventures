package com.gestor_ventures.front.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.front.theme.GestorVenturesTheme

private val OptionShape = RoundedCornerShape(14.dp)

/**
 * Opción de una lista donde solo se elige una: ícono, título, explicación y marca de
 * seleccionada. Se usa para el tipo de actividad del negocio.
 */
@Composable
fun GvSelectableOption(
    titulo: String,
    descripcion: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(OptionShape)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) GestorVenturesTheme.colors.acento else colors.outline,
                shape = OptionShape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = if (selected) colors.primary else colors.primaryContainer,
                    shape = RoundedCornerShape(11.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected) colors.onPrimary else GestorVenturesTheme.colors.acento,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = colors.onSurface,
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = if (selected) 6.dp else 1.5.dp,
                    color = if (selected) GestorVenturesTheme.colors.acento else colors.outline,
                    shape = CircleShape,
                ),
        )
    }
}
