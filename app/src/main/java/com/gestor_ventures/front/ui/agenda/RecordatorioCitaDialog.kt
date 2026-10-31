package com.gestor_ventures.front.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.celularColombiano
import com.gestor_ventures.back.model.celularConFormato

/**
 * HU-26. El popup del recordatorio de una cita: el mensaje ya escrito, desde qué WhatsApp sale y
 * a quién va, y un solo botón para enviar.
 *
 * Sin WhatsApp de la empresa vinculado no hay desde dónde enviar, así que en su lugar pide
 * vincularlo. La app no manda nada por su cuenta: "Enviar" abre el chat con el texto.
 */
@Composable
fun RecordatorioCitaDialog(
    aviso: AvisoAlCliente,
    mensaje: String,
    onEnviar: () -> Unit,
    onVincular: () -> Unit,
    onCerrar: () -> Unit,
) {
    val empresa = aviso.whatsappEmpresa
    if (empresa == null) {
        AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text(stringResource(R.string.agenda_recordatorio_vincular_titulo)) },
            text = { Text(stringResource(R.string.agenda_recordatorio_vincular_texto)) },
            confirmButton = {
                TextButton(onClick = onVincular) { Text(stringResource(R.string.agenda_recordatorio_vincular)) }
            },
            dismissButton = {
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.agenda_aviso_ahora_no)) }
            },
        )
        return
    }

    val nombre = aviso.nombreCliente.trim()
    val destino = celularColombiano(aviso.telefono)?.let(::celularConFormato) ?: aviso.telefono
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.agenda_recordatorio_titulo, nombre)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.agenda_recordatorio_desde, aviso.nombreNegocio, celularConFormato(empresa)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.agenda_recordatorio_para, nombre, destino),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEnviar) { Text(stringResource(R.string.agenda_recordatorio_enviar)) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text(stringResource(R.string.agenda_recordatorio_cancelar)) }
        },
    )
}
