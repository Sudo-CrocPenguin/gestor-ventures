package com.gestor_ventures.front.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R

/**
 * Barra superior de las pantallas de flujo (registrar venta, abrir caja…): flecha para
 * volver y título, con una línea que la separa del formulario.
 */
@Composable
fun GvBackTopBar(
    titulo: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.cd_volver),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}
