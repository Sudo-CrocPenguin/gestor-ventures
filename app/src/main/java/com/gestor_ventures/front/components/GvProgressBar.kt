package com.gestor_ventures.front.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gestor_ventures.front.theme.GestorVenturesTheme

private val BarShape = RoundedCornerShape(4.dp)

/** Barra de progreso con relleno degradado. [progress] va de 0 a 1. */
@Composable
fun GvProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val fill = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, GestorVenturesTheme.colors.primaryVariant)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(BarShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(fill, BarShape)
        )
    }
}
