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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tope para un formulario: un campo por fila.
 *
 * Estirado a todo el ancho de una tablet, un campo de texto se ve vacío siempre y obliga al
 * ojo a cruzar media pantalla para ir de la etiqueta al valor.
 */
val AnchoMaximoFormulario = 600.dp

/**
 * Tope para el contenido de la app: tarjetas, listas, resúmenes.
 *
 * Más holgado que el de un formulario porque acá hay varias cosas por fila y el contenido
 * aguanta más ancho. Con 600 dp una tablet acostada quedaba con dos franjas muertas a los
 * lados; sin tope, las tarjetas se estiran de borde a borde. Esto es el punto medio.
 */
val AnchoMaximoApp = 1000.dp

/**
 * Deja el contenido de la pantalla en una columna centrada, sin pasar de [maximo].
 *
 * En un teléfono no cambia nada: la pantalla es más angosta que el tope, así que el contenido
 * sigue ocupándola entera. En una tablet el sobrante se reparte a los lados en vez de estirar
 * lo de adentro.
 *
 * Envuelve a los `NavHost`, no a cada pantalla, para que una pantalla nueva nazca bien puesta
 * sin que nadie se acuerde de esto. Cada uno con su tope: [AnchoMaximoFormulario] para el
 * acceso, [AnchoMaximoApp] para el resto.
 */
@Composable
fun GvAnchoDeLectura(
    modifier: Modifier = Modifier,
    maximo: Dp = AnchoMaximoFormulario,
    contenido: @Composable () -> Unit,
) {
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
                .widthIn(max = maximo)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            contenido()
        }
    }
}
