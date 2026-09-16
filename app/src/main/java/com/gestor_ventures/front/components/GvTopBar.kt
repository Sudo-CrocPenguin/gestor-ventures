package com.gestor_ventures.front.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R

/**
 * Barra superior de la app: menú, [titulo] (el negocio activo) y campana de notificaciones
 * con contador.
 */
@Composable
fun GvTopBar(
    titulo: String,
    notificacionesSinLeer: Int,
    onMenuClick: () -> Unit,
    onNotificacionesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                painter = painterResource(R.drawable.ic_menu),
                contentDescription = stringResource(R.string.cd_abrir_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )
        IconButton(onClick = onNotificacionesClick) {
            NotificationBell(notificacionesSinLeer)
        }
    }
}

@Composable
private fun NotificationBell(sinLeer: Int) {
    val description = if (sinLeer > 0) {
        pluralStringResource(R.plurals.cd_notificaciones_sin_leer, sinLeer, sinLeer)
    } else {
        stringResource(R.string.cd_notificaciones)
    }
    BadgedBox(
        badge = {
            if (sinLeer > 0) {
                Badge(
                    modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                ) {
                    Text(text = if (sinLeer > 9) "9+" else sinLeer.toString())
                }
            }
        },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bell),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
