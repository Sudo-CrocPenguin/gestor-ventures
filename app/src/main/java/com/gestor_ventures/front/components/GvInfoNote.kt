package com.gestor_ventures.front.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Nota explicativa sobre fondo suave, para aclarar qué hace una pantalla o un campo. */
@Composable
fun GvInfoNote(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(13.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    )
}
