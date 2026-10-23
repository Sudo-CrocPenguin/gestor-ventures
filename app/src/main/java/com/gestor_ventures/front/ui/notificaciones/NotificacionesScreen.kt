package com.gestor_ventures.front.ui.notificaciones

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Alerta
import com.gestor_ventures.back.model.TipoAlerta
import com.gestor_ventures.back.notificaciones.Notificador
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvSoftButton
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatLongDate
import java.time.LocalDateTime

@Composable
fun NotificacionesRoute(onBack: () -> Unit, viewModel: NotificacionesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    // HU-41. El permiso del sistema manda sobre los interruptores: se vuelve a revisar al volver a
    // la pantalla, por si el usuario lo cambió en los ajustes del teléfono.
    var permitidas by remember { mutableStateOf(Notificador.puedeNotificar(contexto)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permitidas = Notificador.puedeNotificar(contexto) }
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido -> permitidas = concedido }

    fun pedirSiHaceFalta(activar: Boolean) {
        if (activar && !permitidas && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NotificacionesScreen(
        uiState = uiState,
        notificacionesPermitidas = permitidas,
        onRecordatoriosChange = { activos ->
            viewModel.onRecordatoriosChange(activos)
            pedirSiHaceFalta(activos)
        },
        onAlertasChange = { activas ->
            viewModel.onAlertasChange(activas)
            pedirSiHaceFalta(activas)
        },
        onAbrirAjustes = {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", contexto.packageName, null))
            }
            contexto.startActivity(intent)
        },
        onBack = onBack,
        onMarcarLeida = viewModel::marcarLeida,
        onMarcarTodas = viewModel::marcarTodasLeidas,
    )
}

/**
 * HU-38. Las alertas, de la más nueva a la más vieja. Las que no se han leído llevan un punto
 * rojo y el botón para marcarlas.
 *
 * HU-41. Arriba, qué avisos recibir. Si el teléfono tiene las notificaciones de la app apagadas,
 * se dice ahí mismo y se ofrece ir a los ajustes: si no, el usuario encendería los interruptores
 * y se quedaría esperando avisos que nunca llegan.
 */
@Composable
fun NotificacionesScreen(
    uiState: NotificacionesUiState,
    notificacionesPermitidas: Boolean,
    onRecordatoriosChange: (Boolean) -> Unit,
    onAlertasChange: (Boolean) -> Unit,
    onAbrirAjustes: () -> Unit,
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
            GvCard {
                GvCardHeader(title = stringResource(R.string.notificaciones_que_recibir))
                Interruptor(
                    titulo = stringResource(R.string.notificaciones_recordatorios),
                    detalle = stringResource(R.string.notificaciones_recordatorios_detalle),
                    activo = uiState.ajustes.recordatorios,
                    onCambio = onRecordatoriosChange,
                )
                Interruptor(
                    titulo = stringResource(R.string.notificaciones_alertas),
                    detalle = stringResource(R.string.notificaciones_alertas_detalle),
                    activo = uiState.ajustes.alertas,
                    onCambio = onAlertasChange,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (!notificacionesPermitidas) {
                    GvInfoNote(
                        text = stringResource(R.string.notificaciones_apagadas_sistema),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    GvSoftButton(
                        text = stringResource(R.string.notificaciones_abrir_ajustes),
                        onClick = onAbrirAjustes,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

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

/** Un tipo de aviso con su explicación y su interruptor. */
@Composable
private fun Interruptor(
    titulo: String,
    detalle: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = activo, onCheckedChange = onCambio)
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
                notificacionesPermitidas = false,
                onRecordatoriosChange = {},
                onAlertasChange = {},
                onAbrirAjustes = {},
                onBack = {},
                onMarcarLeida = {},
                onMarcarTodas = {},
            )
        }
    }
}
