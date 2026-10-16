package com.gestor_ventures.front.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.front.theme.GestorVenturesTheme

/** Una serie del gráfico: su nombre, sus valores y cómo se dibuja. */
data class SerieDeTendencia(
    val nombre: String,
    val valores: List<Double>,
    val color: Color,
    /** Punteada para la segunda serie: el color solo no alcanza para quien no lo distingue. */
    val punteada: Boolean = false,
)

/**
 * HU-40. Dos series en el tiempo, con leyenda y etiquetas abajo.
 *
 * Las dos se dibujan contra el mismo máximo y no cada una contra el suyo: si cada una se
 * estirara a su altura, dos cifras muy distintas se verían iguales y el gráfico mentiría justo
 * en lo que se mira, que es cuál va por encima.
 *
 * El eje empieza en cero por la misma razón: un gráfico que arranca en el mínimo exagera
 * cualquier subida.
 */
@Composable
fun GvGraficoDeTendencia(
    series: List<SerieDeTendencia>,
    etiquetas: List<String>,
    modifier: Modifier = Modifier,
) {
    val grilla = MaterialTheme.colorScheme.outline
    val maximo = series.flatMap { it.valores }.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            series.forEach { serie ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box9(serie.color)
                    Text(
                        text = serie.nombre,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val radio = 3.5.dp.toPx()
            val alto = size.height - radio * 2

            // Tres líneas de referencia: el cero, la mitad y el techo. Más estorban.
            listOf(0f, 0.5f, 1f).forEach { fraccion ->
                val y = radio + alto * fraccion
                drawLine(
                    color = grilla,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            series.forEach { serie ->
                if (serie.valores.size < 2) return@forEach

                val paso = size.width / (serie.valores.size - 1)
                val puntos = serie.valores.mapIndexed { indice, valor ->
                    val normalizado = (valor / maximo).toFloat().coerceIn(0f, 1f)
                    Offset(x = indice * paso, y = radio + alto * (1f - normalizado))
                }

                if (!serie.punteada) {
                    val area = Path().apply {
                        moveTo(puntos.first().x, size.height)
                        puntos.forEach { lineTo(it.x, it.y) }
                        lineTo(puntos.last().x, size.height)
                        close()
                    }
                    drawPath(
                        path = area,
                        brush = Brush.verticalGradient(
                            listOf(serie.color.copy(alpha = 0.20f), serie.color.copy(alpha = 0f)),
                        ),
                    )
                }

                val linea = Path().apply {
                    moveTo(puntos.first().x, puntos.first().y)
                    puntos.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = linea,
                    color = serie.color,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (serie.punteada) {
                            PathEffect.dashPathEffect(
                                floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                            )
                        } else {
                            null
                        },
                    ),
                )
                drawCircle(color = serie.color, radius = radio, center = puntos.last())
            }
        }

        Row(modifier = Modifier.padding(top = 6.dp)) {
            etiquetas.forEach { etiqueta ->
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** El cuadrito de color de la leyenda. */
@Composable
private fun Box9(color: Color) {
    Text(
        text = "",
        modifier = Modifier
            .size(9.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}

/** Los dos colores del gráfico, para que las pantallas no los elijan cada una por su lado. */
object ColoresDeTendencia {
    val ingresos: Color
        @Composable get() = GestorVenturesTheme.colors.success

    val egresos: Color
        @Composable get() = MaterialTheme.colorScheme.error
}
