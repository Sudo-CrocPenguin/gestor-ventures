package com.gestor_ventures.front.ui.asistente

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ConversacionAsistente
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatLongDate
import java.time.LocalDateTime

@Composable
fun HistorialAsistenteRoute(
    onBack: () -> Unit,
    onAbrir: (Long) -> Unit,
    viewModel: HistorialAsistenteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistorialAsistenteScreen(
        uiState = uiState,
        onBack = onBack,
        onAbrir = { onAbrir(it.sesionId) },
        onEliminar = viewModel::pedirConfirmacionDeEliminar,
        onConfirmarEliminar = viewModel::confirmarEliminar,
        onCancelarEliminar = viewModel::cancelarEliminar,
    )
}

@Composable
fun HistorialAsistenteScreen(
    uiState: HistorialAsistenteUiState,
    onBack: () -> Unit,
    onAbrir: (ConversacionAsistente) -> Unit,
    onEliminar: (ConversacionAsistente) -> Unit,
    onConfirmarEliminar: () -> Unit,
    onCancelarEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.historial_asistente_titulo), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.historial_asistente_vacio))
            }
            uiState.conversaciones.forEach { conversacion ->
                GvCard(Modifier.clickable { onAbrir(conversacion) }) {
                    ListRow(
                        title = conversacion.primeraPregunta,
                        subtitle = "${formatLongDate(conversacion.ultimaFecha.toLocalDate())} · " +
                            "${formatHour(conversacion.ultimaFecha.toLocalTime())} · " +
                            pluralStringResource(
                                R.plurals.historial_asistente_preguntas,
                                conversacion.cantidadDePreguntas,
                                conversacion.cantidadDePreguntas,
                            ),
                        iconRes = R.drawable.ic_message,
                        trailing = {
                            IconButton(onClick = { onEliminar(conversacion) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_trash),
                                    contentDescription = stringResource(R.string.historial_asistente_eliminar),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    uiState.porEliminar?.let { conversacion ->
        AlertDialog(
            onDismissRequest = onCancelarEliminar,
            title = { Text(stringResource(R.string.historial_asistente_eliminar_titulo)) },
            text = { Text(stringResource(R.string.historial_asistente_eliminar_ayuda, conversacion.primeraPregunta)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmarEliminar,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.historial_asistente_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelarEliminar) { Text(stringResource(R.string.cancelar)) }
            },
        )
    }
}

@Preview(name = "Historial", widthDp = 380, heightDp = 760)
@Preview(name = "Historial (oscuro)", widthDp = 380, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistorialAsistentePreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistorialAsistenteScreen(
                uiState = HistorialAsistenteUiState(
                    cargando = false,
                    conversaciones = listOf(
                        ConversacionAsistente(2, "¿En qué estoy gastando demasiado?", LocalDateTime.of(2026, 10, 22, 9, 30), 3),
                        ConversacionAsistente(1, "¿Cómo me fue esta semana?", LocalDateTime.of(2026, 10, 20, 18, 5), 1),
                    ),
                ),
                onBack = {},
                onAbrir = {},
                onEliminar = {},
                onConfirmarEliminar = {},
                onCancelarEliminar = {},
            )
        }
    }
}
