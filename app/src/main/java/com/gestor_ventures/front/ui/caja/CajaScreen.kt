package com.gestor_ventures.front.ui.caja

import android.content.res.Configuration
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatLongDate
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDateTime

@Composable
fun CajaRoute(onBack: () -> Unit, viewModel: CajaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CajaScreen(
        uiState = uiState,
        onBack = onBack,
        onAbrirFormulario = viewModel::abrirFormulario,
        onMontoChange = viewModel::onMontoChange,
        onAbrirCaja = viewModel::abrirCaja,
        onCerrarFormulario = viewModel::cerrarFormulario,
    )
}

/**
 * HU-19. La jornada de caja del negocio.
 *
 * Dos caras: con una caja abierta muestra con cuánto arrancó y desde qué hora; sin ninguna,
 * invita a abrirla. Lo que entra y sale durante el turno es HU-20, y cerrarla es HU-21.
 */
@Composable
fun CajaScreen(
    uiState: CajaUiState,
    onBack: () -> Unit,
    onAbrirFormulario: () -> Unit,
    onMontoChange: (String) -> Unit,
    onAbrirCaja: () -> Unit,
    onCerrarFormulario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.caja_titulo), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            // Sin jornada hay poco que mostrar: centrado queda a media altura en vez de
            // pegado arriba, que en una tablet deja media pantalla vacía.
            verticalArrangement = if (uiState.sinCaja) {
                Arrangement.Center
            } else {
                Arrangement.spacedBy(12.dp)
            },
        ) {
            when {
                uiState.caja != null -> JornadaAbierta(uiState.caja)
                uiState.sinCaja -> SinJornada()
            }
        }

        if (uiState.caja == null) {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                GvPrimaryButton(
                    text = stringResource(R.string.caja_abrir),
                    onClick = onAbrirFormulario,
                    enabled = uiState.sinCaja,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    uiState.formulario?.let { formulario ->
        AperturaSheet(
            formulario = formulario,
            error = uiState.error,
            onMontoChange = onMontoChange,
            onAbrir = onAbrirCaja,
            onCerrar = onCerrarFormulario,
        )
    }
}

/** La jornada en curso: con cuánto se arrancó y desde cuándo. */
@Composable
private fun JornadaAbierta(caja: CajaAbiertaUi, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.caja_jornada_en_curso)) {
            StatusPill(
                text = stringResource(R.string.caja_estado_abierta),
                containerColor = GestorVenturesTheme.colors.successContainer,
                contentColor = GestorVenturesTheme.colors.success,
            )
        }

        Text(
            text = stringResource(R.string.caja_monto_inicial_etiqueta),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatPesos(caja.montoInicial),
            style = NumericTextStyle.copy(fontSize = 26.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(
                    R.string.caja_abierta_desde,
                    formatHour(caja.apertura.toLocalTime()),
                    formatLongDate(caja.apertura.toLocalDate()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // Lo que falta de la épica, dicho de frente en vez de dejar la pantalla a medias.
    GvInfoNote(stringResource(R.string.caja_movimientos_pendiente))
}

/** Sin jornada abierta: el estado en el que arranca el día. */
@Composable
private fun SinJornada(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_box),
                contentDescription = null,
                tint = GestorVenturesTheme.colors.acento,
                modifier = Modifier.size(28.dp),
            )
        }

        Text(
            text = stringResource(R.string.caja_vacia_titulo),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.caja_vacia_detalle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Caja abierta", widthDp = 380, heightDp = 780)
@Preview(
    name = "Caja abierta (oscuro)",
    widthDp = 380,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CajaAbiertaPreview() {
    VistaPreviaCaja(
        CajaUiState(
            cargando = false,
            caja = CajaAbiertaUi(
                montoInicial = 50_000.0,
                apertura = LocalDateTime.of(2026, 10, 7, 8, 0),
            ),
        ),
    )
}

@Preview(name = "Sin caja abierta", widthDp = 380, heightDp = 780)
@Composable
private fun CajaVaciaPreview() {
    VistaPreviaCaja(CajaUiState(cargando = false))
}

@Composable
private fun VistaPreviaCaja(uiState: CajaUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CajaScreen(
                uiState = uiState,
                onBack = {},
                onAbrirFormulario = {},
                onMontoChange = {},
                onAbrirCaja = {},
                onCerrarFormulario = {},
            )
        }
    }
}
