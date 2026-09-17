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
 * Las tres preguntas que se le hacen a las finanzas del negocio: cómo va el mes (HU-16), en qué
 * se va la plata de funcionar (HU-14) y cuánto cuesta producir lo que se vende (HU-13).
 *
 * El resumen va primero porque es la pregunta con la que se entra a esta pestaña; el detalle se
 * mira después y de a una sección a la vez, igual que en categorías.
 */
enum class SeccionFinanzas(@param:StringRes val labelRes: Int) {
    Resumen(R.string.finanzas_seccion_resumen),
    Gastos(R.string.finanzas_seccion_gastos),
    Costos(R.string.finanzas_seccion_costos),
}

/** Pestaña de Finanzas: el resumen del mes y el detalle de lo que lo compone. */
@Composable
fun FinanzasRoute(modifier: Modifier = Modifier) {
    var seccion by rememberSaveable { mutableStateOf(SeccionFinanzas.Resumen) }

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
                SeccionFinanzas.Resumen -> ResumenFinancieroRoute()
                SeccionFinanzas.Gastos -> GastosRoute()
                SeccionFinanzas.Costos -> CostosRoute()
            }
        }
    }
}
