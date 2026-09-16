package com.gestor_ventures.front.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gestor_ventures.front.theme.GestorVenturesTheme

/** Mini gráfica de tendencia: línea con relleno degradado y un punto en el último valor. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = GestorVenturesTheme.colors.acento,
) {
    if (values.size < 2) return

    Canvas(modifier) {
        val dotRadius = 3.5.dp.toPx()
        val min = values.min()
        val range = (values.max() - min).takeIf { it > 0 } ?: 1.0
        val usableHeight = size.height - dotRadius * 2
        val stepX = (size.width - dotRadius) / (values.size - 1)

        val points = values.mapIndexed { index, value ->
            val normalized = ((value - min) / range).toFloat()
            Offset(x = index * stepX, y = dotRadius + usableHeight * (1f - normalized))
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }

        drawPath(
            path = area,
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0f))),
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(color = color, radius = dotRadius, center = points.last())
    }
}
