package com.gestor_ventures.front.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FieldShape = RoundedCornerShape(13.dp)

/**
 * Dato que la pantalla muestra pero el usuario no edita (la fecha y hora que pone el sistema,
 * un saldo calculado…). Usa la misma caja que [GvTextField], pero sin fondo blanco, para que
 * se note que no se escribe ahí.
 *
 * [value] va a la izquierda y [valueEnd], si se pasa, al otro extremo de la misma fila.
 */
@Composable
fun GvReadOnlyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    valueEnd: String? = null,
    @DrawableRes iconEndRes: Int? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.outline, FieldShape)
                .padding(horizontal = 13.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ValorConIcono(
                texto = value,
                iconRes = iconRes,
                modifier = Modifier.weight(1f),
            )
            if (valueEnd != null) {
                ValorConIcono(texto = valueEnd, iconRes = iconEndRes)
            }
        }
    }
}

@Composable
private fun ValorConIcono(
    texto: String,
    @DrawableRes iconRes: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
