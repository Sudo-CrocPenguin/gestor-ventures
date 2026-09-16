package com.gestor_ventures.front.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Qué se puede hacer con un elemento de una lista, después de tocarlo.
 *
 * Evita llenar cada fila de iconitos: la fila muestra el dato y el menú muestra las acciones,
 * con el nombre de lo que se va a tocar al frente para que nadie borre la fila equivocada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GvAccionesSheet(
    titulo: String,
    acciones: List<AccionSheet>,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            acciones.forEach { accion ->
                FilaAccion(accion)
            }
        }
    }
}

/** Una opción del menú. [destructiva] la pinta en rojo, para que no se confunda con las demás. */
data class AccionSheet(
    val texto: String,
    @param:DrawableRes val iconRes: Int,
    val destructiva: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun FilaAccion(accion: AccionSheet, modifier: Modifier = Modifier) {
    val color = if (accion.destructiva) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = accion.onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(accion.iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = accion.texto,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}
