package com.gestor_ventures.front.ui.agenda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.AnticipacionRecordatorio
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvInfoNote

private val Opciones: List<AnticipacionRecordatorio?> = listOf(null) + AnticipacionRecordatorio.entries

@Composable
fun SelectorDeRecordatorio(
    anticipacion: AnticipacionRecordatorio?,
    notificacionesPermitidas: Boolean,
    ayuda: String,
    onAnticipacionChange: (AnticipacionRecordatorio?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.recordatorio_titulo),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Opciones.forEach { opcion ->
                GvChip(
                    text = stringResource(opcion.labelRes()),
                    selected = opcion == anticipacion,
                    onClick = { onAnticipacionChange(opcion) },
                )
            }
        }
        Text(
            text = ayuda,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        AnimatedVisibility(visible = anticipacion != null && !notificacionesPermitidas) {
            GvInfoNote(
                text = stringResource(R.string.recordatorio_sin_permiso),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
