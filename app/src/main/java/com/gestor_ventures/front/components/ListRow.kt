package com.gestor_ventures.front.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Fila de lista: ícono en recuadro, título, subtítulo y contenido opcional a la derecha. */
@Composable
fun ListRow(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconContainerColor, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}
