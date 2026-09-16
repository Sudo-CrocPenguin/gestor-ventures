package com.gestor_ventures.front.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ContenedorShape = RoundedCornerShape(14.dp)
private val OpcionShape = RoundedCornerShape(11.dp)

/**
 * Selector de una sola opción entre pocas, en forma de pastilla dividida (el `.seg` del
 * mockup). Sirve para alternar modos —venta detallada o rápida— o para filtros cortos.
 */
@Composable
fun <T> GvSegmentedToggle(
    opciones: List<T>,
    seleccionada: T,
    etiqueta: @Composable (T) -> String,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ContenedorShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, ContenedorShape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opciones.forEach { opcion ->
            OpcionSegmento(
                texto = etiqueta(opcion),
                seleccionada = opcion == seleccionada,
                onClick = { onSeleccionar(opcion) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OpcionSegmento(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fondo by animateColorAsState(
        targetValue = if (seleccionada) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "fondoSegmento",
    )
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
        color = if (seleccionada) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(OpcionShape)
            .background(fondo)
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
    )
}
