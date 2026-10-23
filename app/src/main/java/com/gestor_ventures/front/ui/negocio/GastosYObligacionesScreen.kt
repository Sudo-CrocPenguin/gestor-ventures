package com.gestor_ventures.front.ui.negocio

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
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvSegmentedToggle

/**
 * Lo que el negocio tiene comprometido, en sus dos formas: los gastos que se repiten cada
 * periodo (HU-06) y las deudas con fecha de vencimiento (HU-07).
 *
 * Se parecen —las dos son plata que va a salir— pero se miran distinto: el gasto fijo se
 * pregunta "¿cuánto me cuesta el mes?" y la obligación "¿qué tengo que pagar ya?".
 */
enum class SeccionCompromisos(@param:StringRes val labelRes: Int) {
    GastosFijos(R.string.compromisos_seccion_fijos),
    Obligaciones(R.string.compromisos_seccion_obligaciones),
}

@Composable
fun GastosYObligacionesRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var seccion by rememberSaveable { mutableStateOf(SeccionCompromisos.GastosFijos) }

    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.menu_gastos_fijos), onBack = onBack)

        GvSegmentedToggle(
            opciones = SeccionCompromisos.entries,
            seleccionada = seccion,
            etiqueta = { stringResource(it.labelRes) },
            onSeleccionar = { seccion = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Cada sección trae su propio contenido desplazable y su botón fijo abajo.
        Box(Modifier.weight(1f)) {
            when (seccion) {
                SeccionCompromisos.GastosFijos -> GastosFijosRoute()
                SeccionCompromisos.Obligaciones -> ObligacionesRoute()
            }
        }
    }
}
