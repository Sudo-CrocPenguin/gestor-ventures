package com.gestor_ventures.front.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.front.components.TourRegistro
import com.gestor_ventures.front.theme.GestorVenturesTheme
import kotlin.math.roundToInt

/** Cuánto sobresale el resalte del elemento señalado, para que no quede pegado a su borde. */
private val MargenDelResalte = 6.dp

/** Separación entre el elemento señalado y la tarjeta que lo explica. */
private val SeparacionTarjeta = 14.dp

private val AnchoTarjeta = 380.dp

/** Alto aproximado de la tarjeta, para no sacarla de la pantalla al colocarla junto al elemento. */
private val AltoTarjeta = 230.dp

/**
 * El tour: oscurece toda la app salvo el elemento que se explica y pone al lado una tarjeta con
 * su descripción. Se dibuja encima de todo y se traga los toques, para que el usuario recorra
 * la app con los botones del tour y no toque por accidente lo que hay debajo.
 *
 * Quien lo usa se encarga de llevar la app a la escena de cada paso (cambiar de pestaña, abrir
 * el menú); este composable solo señala y explica.
 */
@Composable
fun TourScreen(
    uiState: TourUiState,
    registro: TourRegistro,
    onSiguiente: () -> Unit,
    onAnterior: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!uiState.visible) return
    BackHandler(onBack = onCerrar)

    val paso = uiState.actual
    val densidad = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
    ) {
        val anchoPx = with(densidad) { maxWidth.toPx() }
        val altoPx = with(densidad) { maxHeight.toPx() }
        val margenPx = with(densidad) { MargenDelResalte.toPx() }

        // Un elemento que todavía no se dibujó, o que quedó fuera de la pantalla (dentro de una
        // lista desplazada), no se puede señalar: el paso sale como una tarjeta suelta.
        val pantalla = Rect(0f, 0f, anchoPx, altoPx)
        val objetivo = paso.objetivo
            ?.let { registro.rects[it] }
            ?.takeIf { it.width > 0f && it.height > 0f && it.overlaps(pantalla) }
        val hueco = objetivo?.let {
            Rect(it.left - margenPx, it.top - margenPx, it.right + margenPx, it.bottom + margenPx)
        }

        // El resalte viaja de un elemento al siguiente en vez de saltar. Si el paso no señala
        // nada, se queda donde estaba (invisible) para que el próximo parta de ahí.
        var ultimo by remember { mutableStateOf(Rect.Zero) }
        if (hueco != null) ultimo = hueco
        val animado by animateRectAsState(targetValue = hueco ?: ultimo, label = "resalteDelTour")

        val colorResalte = Color.White
        val radio = with(densidad) { 14.dp.toPx() }
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color.Black.copy(alpha = 0.74f))
            if (hueco != null) {
                drawRoundRect(
                    color = Color.Black,
                    topLeft = animado.topLeft,
                    size = animado.size,
                    cornerRadius = CornerRadius(radio),
                    blendMode = BlendMode.Clear,
                )
                drawRoundRect(
                    color = colorResalte.copy(alpha = 0.9f),
                    topLeft = animado.topLeft,
                    size = animado.size,
                    cornerRadius = CornerRadius(radio),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        val anchoDeLaTarjeta = minOf(maxWidth - 32.dp, AnchoTarjeta)
        val separacionPx = with(densidad) { SeparacionTarjeta.toPx() }
        val altoTarjetaPx = with(densidad) { AltoTarjeta.toPx() }
        val anchoTarjetaPx = with(densidad) { (anchoDeLaTarjeta + 32.dp).toPx() }

        // Dónde va la tarjeta: al lado si el elemento está pegado a la izquierda y sobra sitio
        // (el menú lateral), y si no, debajo o encima según en qué mitad de la pantalla esté.
        val tarjetaModifier = when {
            hueco == null -> Modifier.align(Alignment.Center)
            hueco.right < anchoPx * 0.5f && hueco.center.y < altoPx * 0.8f &&
                anchoPx - hueco.right >= anchoTarjetaPx -> Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (hueco.right + separacionPx).roundToInt(),
                        y = (hueco.center.y - altoTarjetaPx / 2f)
                            .coerceIn(separacionPx, (altoPx - altoTarjetaPx - separacionPx).coerceAtLeast(separacionPx))
                            .roundToInt(),
                    )
                }
            hueco.center.y < altoPx / 2f -> Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (hueco.bottom + separacionPx).roundToInt()) }
            else -> Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, -(altoPx - hueco.top + separacionPx).roundToInt()) }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = tarjetaModifier
                .safeDrawingPadding()
                .widthIn(max = anchoDeLaTarjeta)
                .heightIn(max = maxHeight - 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.tour_progreso, uiState.paso + 1, uiState.pasos.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(
                        text = stringResource(paso.tituloRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(paso.textoRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!uiState.esUltimo) {
                        TextButton(onClick = onCerrar) { Text(stringResource(R.string.tour_omitir)) }
                    }
                    Box(Modifier.weight(1f))
                    if (uiState.paso > 0) {
                        TextButton(onClick = onAnterior) { Text(stringResource(R.string.tour_anterior)) }
                    }
                    Button(
                        onClick = onSiguiente,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            stringResource(
                                if (uiState.esUltimo) R.string.tour_empezar else R.string.tour_siguiente,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Tour", widthDp = 380, heightDp = 780)
@Composable
private fun TourScreenPreview() {
    GestorVenturesTheme {
        TourScreen(
            uiState = TourUiState(visible = true, paso = 0),
            registro = TourRegistro(),
            onSiguiente = {},
            onAnterior = {},
            onCerrar = {},
        )
    }
}
