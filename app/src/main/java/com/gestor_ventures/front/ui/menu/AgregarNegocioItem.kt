package com.gestor_ventures.front.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R

/** Fila punteada para crear un negocio nuevo, al final de la lista de negocios. */
@Composable
fun AgregarNegocioItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bordeColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 11.dp, vertical = 6.dp)
            .clip(MenuItemShape)
            .bordeDiscontinuo(bordeColor, 13.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .bordeDiscontinuo(bordeColor, 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = stringResource(R.string.menu_agregar_negocio),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Contorno de línea discontinua; Compose no lo trae, se dibuja con un PathEffect. */
private fun Modifier.bordeDiscontinuo(color: Color, radio: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radio.toPx()),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f)),
        ),
    )
}
