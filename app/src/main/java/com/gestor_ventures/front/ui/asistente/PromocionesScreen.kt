package com.gestor_ventures.front.ui.asistente

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Sugerencia
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvSoftButton
import com.gestor_ventures.front.theme.GestorVenturesTheme
import java.time.LocalDateTime

@Composable
fun PromocionesRoute(onBack: () -> Unit, viewModel: PromocionesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PromocionesScreen(
        uiState = uiState,
        onBack = onBack,
        onSugerir = viewModel::sugerir,
        onGuardar = viewModel::guardar,
        onDescartar = viewModel::descartar,
    )
}

@Composable
fun PromocionesScreen(
    uiState: PromocionesUiState,
    onBack: () -> Unit,
    onSugerir: () -> Unit,
    onGuardar: (Sugerencia) -> Unit,
    onDescartar: (Sugerencia) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.promociones_titulo), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.promociones_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.promociones_vacio))
            }

            if (uiState.generando) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.promociones_pensando),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = uiState.error != null) {
                GvInfoNote(
                    text = uiState.error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            uiState.nuevas.forEach { sugerencia ->
                TarjetaSugerencia(
                    sugerencia = sugerencia,
                    onGuardar = { onGuardar(sugerencia) },
                    onDescartar = { onDescartar(sugerencia) },
                )
            }

            if (uiState.guardadas.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.promociones_guardadas),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                uiState.guardadas.forEach { sugerencia ->
                    TarjetaSugerencia(
                        sugerencia = sugerencia,
                        onGuardar = null,
                        onDescartar = { onDescartar(sugerencia) },
                    )
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.promociones_sugerir),
                onClick = onSugerir,
                enabled = !uiState.generando,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun TarjetaSugerencia(
    sugerencia: Sugerencia,
    onGuardar: (() -> Unit)?,
    onDescartar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        Text(
            text = conNegritas(sugerencia.contenido),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onGuardar != null) {
                GvSoftButton(
                    text = stringResource(R.string.promociones_guardar),
                    onClick = onGuardar,
                    iconRes = R.drawable.ic_check,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(onClick = onDescartar) {
                Text(
                    text = stringResource(
                        if (onGuardar != null) R.string.promociones_descartar else R.string.promociones_quitar,
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(name = "Promociones", widthDp = 380, heightDp = 760)
@Preview(name = "Promociones (oscuro)", widthDp = 380, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PromocionesPreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PromocionesScreen(
                uiState = PromocionesUiState(
                    cargando = false,
                    sugerencias = listOf(
                        Sugerencia(
                            id = 1,
                            contenido = "**Martes de dos por uno en galletas**\nLos martes son tu día más flojo. " +
                                "Ofrece dos galletas por el precio de una a quienes compren una torta.",
                            guardada = false,
                            fechaGeneracion = LocalDateTime.of(2026, 10, 22, 10, 0),
                        ),
                        Sugerencia(
                            id = 2,
                            contenido = "**Vuelve y te damos un 10 %**\nEscríbeles a los clientes que no compran " +
                                "hace un mes con un descuento para su próxima compra.",
                            guardada = true,
                            fechaGeneracion = LocalDateTime.of(2026, 10, 20, 9, 0),
                        ),
                    ),
                ),
                onBack = {},
                onSugerir = {},
                onGuardar = {},
                onDescartar = {},
            )
        }
    }
}
