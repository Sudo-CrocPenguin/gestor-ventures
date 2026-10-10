package com.gestor_ventures.front.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Ancho máximo de una columna de contenido.
 *
 * Los diseños están pensados para un teléfono. En una tablet, dejar que un campo o una tarjeta
 * se estiren de borde a borde no aprovecha el espacio: obliga al ojo a cruzar media pantalla
 * para ir de la etiqueta al valor, y un campo de texto de 1.200 dp se ve vacío siempre.
 */
val AnchoMaximoContenido = 600.dp

/**
 * Deja el contenido de la pantalla en una columna centrada, sin pasar de [AnchoMaximoContenido].
 *
 * En un teléfono no cambia nada: la pantalla es más angosta que el tope, así que el contenido
 * sigue ocupándola entera. En una tablet el sobrante se reparte a los lados en vez de estirar
 * lo de adentro.
 *
 * Envuelve a los `NavHost`, no a cada pantalla, para que una pantalla nueva nazca bien puesta
 * sin que nadie se acuerde de esto.
 */
@Composable
fun GvAnchoDeLectura(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    // El fondo se pinta acá y no adentro: si no, lo que queda a los lados de la columna
    // se ve del color de la ventana, que en una tablet son dos franjas blancas.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = AnchoMaximoContenido)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            contenido()
        }
    }
}
