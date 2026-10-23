package com.gestor_ventures.front.ui.notificaciones

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.gestor_ventures.back.model.Alerta
import com.gestor_ventures.back.model.TipoAlerta
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatLongDate
import java.time.LocalDateTime

@Composable
fun NotificacionesRoute(onBack: () -> Unit, viewModel: NotificacionesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificacionesScreen(
        uiState = uiState,
        onBack = onBack,
        onMarcarLeida = viewModel::marcarLeida,
        onMarcarTodas = viewModel::marcarTodasLeidas,
    )
}

@Composable
fun NotificacionesScreen(
    uiState: NotificacionesUiState,
    onBack: () -> Unit,
    onMarcarLeida: (Alerta) -> Unit,
    onMarcarTodas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.menu_notificaciones), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.alertas_titulo),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (uiState.hayNoLeidas) {
                    TextButton(onClick = onMarcarTodas) {
                        Text(stringResource(R.string.alertas_marcar_todas))
                    }
                }
            }

            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.alertas_vacio))
            }

            uiState.alertas.forEach { alerta ->
                FilaAlerta(alerta = alerta, onMarcarLeida = { onMarcarLeida(alerta) })
            }
        }
    }
}

@Composable
private fun FilaAlerta(alerta: Alerta, onMarcarLeida: () -> Unit, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        ListRow(
            title = stringResource(alerta.tipo.tituloRes()),
            subtitle = alerta.descripcion,
            subtitleMaxLines = 4,
            iconRes = alerta.tipo.iconRes(),
            trailing = if (alerta.leida) null else {
                { Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.error, CircleShape)) }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatLongDate(alerta.fechaGeneracion.toLocalDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!alerta.leida) {
                TextButton(onClick = onMarcarLeida) {
                    Text(stringResource(R.string.alertas_marcar_leida))
                }
            }
        }
    }
}

@Preview(name = "Alertas", widthDp = 380, heightDp = 760)
@Preview(name = "Alertas (oscuro)", widthDp = 380, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotificacionesPreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotificacionesScreen(
                uiState = NotificacionesUiState(
                    cargando = false,
                    alertas = listOf(
                        Alerta(
                            2, TipoAlerta.CaidaVentas,
                            "Tus ventas de los últimos 7 días ($ 820.000) bajaron 34 % frente a la semana anterior ($ 1.240.000).",
                            false, LocalDateTime.of(2026, 10, 22, 8, 0),
                        ),
                        Alerta(
                            1, TipoAlerta.GastoElevado,
                            "Lo que va del mes en Insumos ($ 640.000) está 60 % por encima de los mismos días del mes pasado ($ 400.000).",
                            true, LocalDateTime.of(2026, 10, 15, 8, 0),
                        ),
                    ),
                ),
                onBack = {},
                onMarcarLeida = {},
                onMarcarTodas = {},
            )
        }
    }
}
