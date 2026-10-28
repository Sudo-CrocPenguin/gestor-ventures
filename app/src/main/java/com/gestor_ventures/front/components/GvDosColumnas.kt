package com.gestor_ventures.front.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A partir de qué ancho hay sitio de verdad para dos columnas.
 *
 * Por debajo de esto, partir en dos deja dos columnas angostas que se leen peor que una sola
 * ancha. Con el margen de tablet ya descontado, una tablet acostada pasa el corte y la misma
 * tablet parada no: acostada sobra ancho y falta alto, parada es al revés.
 */
private val AnchoParaDosColumnas = 840.dp

/**
 * Pone [izquierda] y [derecha] lado a lado cuando hay ancho, y una debajo de la otra cuando no.
 *
 * Es la respuesta a que la app se viera como un teléfono estirado: en una tablet acostada el
 * contenido se acababa a media pantalla y el resto quedaba negro. En vez de estirar las
 * tarjetas, se reparten en dos columnas.
 *
 * Mide el espacio que le dan, no la pantalla: así funciona igual dentro de una pantalla
 * completa que dentro de media, y una pantalla no tiene que saber dónde la pusieron.
 */
@Composable
fun GvDosColumnas(
    izquierda: @Composable ColumnScope.() -> Unit,
    derecha: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    espacio: Dp = 12.dp,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth >= AnchoParaDosColumnas) {
            Row(horizontalArrangement = Arrangement.spacedBy(espacio)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(espacio),
                    content = izquierda,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(espacio),
                    content = derecha,
                )
            }
        } else {
            // En una sola columna el orden es el de siempre: primero lo de la izquierda.
            Column(verticalArrangement = Arrangement.spacedBy(espacio)) {
                izquierda()
                derecha()
            }
        }
    }
}
