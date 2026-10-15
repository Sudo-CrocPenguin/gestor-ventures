package com.gestor_ventures.front.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Avatar con las iniciales de un usuario o negocio ("Dulce Antojo" → "DA"). El tamaño de la
 * letra sale del tamaño del recuadro, para que se vea igual en cualquier medida.
 */
@Composable
fun GvAvatar(
    iniciales: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: RoundedCornerShape = RoundedCornerShape(size / 3.6f),
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciales,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.39f).sp,
            ),
            color = contentColor,
        )
    }
}
