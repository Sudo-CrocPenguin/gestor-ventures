package com.gestor_ventures.front.ui.finanzas

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvSegmentedToggle

/**
 * Las dos caras de lo que sale del negocio: lo que se va por funcionar (HU-14) y lo que se va
 * por producir lo que se vende (HU-13).
 *
 * Son dos preguntas distintas y el emprendedor mira una a la vez, igual que en categorías.
 */
enum class SeccionFinanzas(@param:StringRes val labelRes: Int) {
    Gastos(R.string.finanzas_seccion_gastos),
    Costos(R.string.finanzas_seccion_costos),
}

/**
 * Pestaña de Finanzas. HU-16 le pondrá el resumen del periodo encima de este selector.
 */
@Composable
fun FinanzasRoute(modifier: Modifier = Modifier) {
    var seccion by rememberSaveable { mutableStateOf(SeccionFinanzas.Gastos) }

    Column(modifier.fillMaxSize()) {
        GvSegmentedToggle(
            opciones = SeccionFinanzas.entries,
            seleccionada = seccion,
            etiqueta = { stringResource(it.labelRes) },
            onSeleccionar = { seccion = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Cada sección trae su propio contenido desplazable y su botón fijo abajo.
        Box(Modifier.weight(1f)) {
            when (seccion) {
                SeccionFinanzas.Gastos -> GastosRoute()
                SeccionFinanzas.Costos -> CostosRoute()
            }
        }
    }
}
