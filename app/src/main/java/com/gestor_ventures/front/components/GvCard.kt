package com.gestor_ventures.front.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestor_ventures.front.theme.GestorVenturesTheme

private val CardShape = RoundedCornerShape(16.dp)

/** Tarjeta base del sistema de diseño: superficie, borde fino y sombra suave. */
@Composable
fun GvCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shadowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = CardShape,
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .background(MaterialTheme.colorScheme.surface, CardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CardShape)
            .padding(16.dp),
        content = content,
    )
}

/** Encabezado de tarjeta: título a la izquierda y un elemento opcional a la derecha. */
@Composable
fun GvCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        trailing?.invoke()
    }
}

/** Enlace de texto para acciones secundarias, ej. "Ver finanzas". */
@Composable
fun GvTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = GestorVenturesTheme.colors.acento,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}
