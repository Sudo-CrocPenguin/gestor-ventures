package com.gestor_ventures.front.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Dónde está en pantalla cada elemento que el tour puede señalar.
 *
 * Cada pantalla marca sus botones con [tourTarget] y el tour lee de acá, así que ni las
 * pantallas saben que existe un tour ni el tour sabe cómo está armada cada pantalla: se
 * hablan solo por el nombre del elemento.
 */
class TourRegistro {
    val rects = mutableStateMapOf<String, Rect>()
}

/** Sin proveedor (previews, pruebas) las marcas no hacen nada. */
val LocalTourRegistro = compositionLocalOf<TourRegistro?> { null }

/**
 * Marca este elemento como algo que el tour puede señalar con el nombre [id].
 *
 * Se anota la posición en la raíz de la ventana —el mismo espacio en el que el tour dibuja—
 * y se borra cuando el elemento sale de la pantalla, para que el tour no apunte a un fantasma.
 */
fun Modifier.tourTarget(id: String): Modifier = composed {
    val registro = LocalTourRegistro.current
    if (registro == null) {
        this
    } else {
        DisposableEffect(id) { onDispose { registro.rects.remove(id) } }
        this.onGloballyPositioned { registro.rects[id] = it.boundsInRoot() }
    }
}
