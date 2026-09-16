package com.gestor_ventures.front.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.aHsl
import com.gestor_ventures.front.theme.colorAHex
import com.gestor_ventures.front.theme.hexAColor
import com.gestor_ventures.front.theme.tintaSobre

private val PanelShape = RoundedCornerShape(14.dp)
private const val TonosDelArcoiris = 6

/**
 * Selector de color completo: panel de tono/intensidad, riel de colores y campo hexadecimal
 * para quien ya sabe el código exacto de su marca.
 *
 * Trabaja en HSL (tono, saturación, luz) porque es como piensa una persona el color: "este
 * mismo azul pero más claro". El hexadecimal es solo la entrada y la salida.
 */
@Composable
fun GvColorPickerDialog(
    colorInicial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val inicial = remember(colorInicial) { aHsl(colorInicial) }
    var tono by remember { mutableFloatStateOf(inicial.tono) }
    var saturacion by remember { mutableFloatStateOf(inicial.saturacion.coerceAtLeast(0.15f)) }
    var luz by remember { mutableFloatStateOf(inicial.luz) }
    var hexTexto by remember { mutableStateOf(colorAHex(colorInicial).removePrefix("#")) }

    val color = Color.hsl(tono, saturacion.coerceIn(0f, 1f), luz.coerceIn(0f, 1f))

    // El hexadecimal se reescribe al mover el panel, y el panel se mueve al escribir un
    // hexadecimal válido. Separar las dos entradas evita que se persigan entre sí.
    fun moverPanel(nuevaSaturacion: Float, nuevaLuz: Float) {
        saturacion = nuevaSaturacion.coerceIn(0f, 1f)
        luz = nuevaLuz.coerceIn(0f, 1f)
        hexTexto = colorAHex(Color.hsl(tono, saturacion, luz)).removePrefix("#")
    }

    fun moverTono(nuevoTono: Float) {
        tono = nuevoTono.coerceIn(0f, 359.99f)
        hexTexto = colorAHex(Color.hsl(tono, saturacion, luz)).removePrefix("#")
    }

    fun escribirHex(texto: String) {
        hexTexto = texto.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            .take(6)
            .uppercase()
        hexAColor(hexTexto)?.let { elegido ->
            val hsl = aHsl(elegido)
            tono = hsl.tono
            saturacion = hsl.saturacion
            luz = hsl.luz
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_personalizado_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.color_personalizado_ayuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PanelDeColor(
                    tono = tono,
                    saturacion = saturacion,
                    luz = luz,
                    onCambio = ::moverPanel,
                )
                RielDeTonos(tono = tono, onCambio = ::moverTono)
                CampoHexadecimal(
                    hex = hexTexto,
                    color = color,
                    onHexChange = ::escribirHex,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(color) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = GestorVenturesTheme.colors.acento,
                ),
            ) {
                Text(stringResource(R.string.color_personalizado_usar))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.cancelar))
            }
        },
    )
}

/**
 * Cuadro donde se elige qué tan vivo (eje horizontal) y qué tan claro (eje vertical) es el color.
 */
@Composable
private fun PanelDeColor(
    tono: Float,
    saturacion: Float,
    luz: Float,
    onCambio: (saturacion: Float, luz: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(PanelShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, PanelShape)
            .arrastrable { posicion, tamano ->
                onCambio(posicion.x / tamano.x, 1f - posicion.y / tamano.y)
            },
    ) {
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.hsl(tono, 0f, 0.5f), Color.hsl(tono, 1f, 0.5f)),
            ),
        )
        drawRect(
            Brush.verticalGradient(listOf(Color.White, Color.Transparent, Color.Black)),
        )

        val centro = Offset(saturacion * size.width, (1f - luz) * size.height)
        drawCircle(Color.Black.copy(alpha = 0.4f), 9.dp.toPx(), centro, style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, 9.dp.toPx(), centro, style = Stroke(2.dp.toPx()))
    }
}

/** Riel del arcoíris: elige el tono base sobre el que trabaja el panel. */
@Composable
private fun RielDeTonos(
    tono: Float,
    onCambio: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val arcoiris = remember {
        (0..TonosDelArcoiris).map { Color.hsl(it * 360f / TonosDelArcoiris % 360f, 1f, 0.5f) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(CircleShape)
            .arrastrable { posicion, tamano -> onCambio(posicion.x / tamano.x * 360f) },
    ) {
        drawRect(Brush.horizontalGradient(arcoiris))

        val centro = Offset((tono / 360f) * size.width, size.height / 2f)
        val radio = size.height / 2f - 3.dp.toPx()
        drawCircle(Color.Black.copy(alpha = 0.4f), radio, centro, style = Stroke(4.dp.toPx()))
        drawCircle(Color.White, radio, centro, style = Stroke(2.5f.dp.toPx()))
    }
}

/** El código exacto, para quien llega con el manual de marca en la mano. */
@Composable
private fun CampoHexadecimal(
    hex: String,
    color: Color,
    onHexChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        GvTextField(
            label = stringResource(R.string.color_personalizado_hex),
            value = hex,
            onValueChange = onHexChange,
            placeholder = "1B2A4A",
            keyboardType = KeyboardType.Ascii,
            leading = {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier.weight(1f),
        )
        // Muestra grande del color con una letra encima: se ve de una si el texto se leerá.
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
                .padding(bottom = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.color_personalizado_muestra),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                color = tintaSobre(color),
            )
        }
    }
}

/**
 * Arrastrar y tocar valen lo mismo en estos controles: el dedo elige un punto, no un gesto.
 * Entrega la posición y el tamaño del área para que cada control los interprete a su manera.
 */
private fun Modifier.arrastrable(onPosicion: (posicion: Offset, tamano: Offset) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val inicio = awaitFirstDown(requireUnconsumed = false)
            // Se mide al tocar, no al componer: antes del primer dibujo el tamaño es cero.
            val tamano = Offset(size.width.toFloat(), size.height.toFloat())
            if (tamano.x <= 0f || tamano.y <= 0f) return@awaitEachGesture

            onPosicion(inicio.position, tamano)
            drag(inicio.id) { cambio ->
                onPosicion(cambio.position, tamano)
                cambio.consume()
            }
        }
    }
