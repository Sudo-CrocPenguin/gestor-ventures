package com.gestor_ventures.front.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ModoTema

/** Elegir entre claro, oscuro o el del dispositivo. El cambio se aplica al tocar la opción. */
@Composable
fun AparienciaDialog(
    modo: ModoTema,
    onElegir: (ModoTema) -> Unit,
    onCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.apariencia_titulo)) },
        text = {
            Column(Modifier.selectableGroup()) {
                ModoTema.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = opcion == modo, role = Role.RadioButton) { onElegir(opcion) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = opcion == modo, onClick = null, modifier = Modifier.padding(end = 12.dp))
                        Text(
                            text = stringResource(opcion.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text(stringResource(R.string.apariencia_listo)) }
        },
    )
}

private fun ModoTema.labelRes(): Int = when (this) {
    ModoTema.SISTEMA -> R.string.apariencia_sistema
    ModoTema.CLARO -> R.string.apariencia_claro
    ModoTema.OSCURO -> R.string.apariencia_oscuro
}
