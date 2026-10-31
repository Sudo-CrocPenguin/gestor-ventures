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
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextLink
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatLongDate
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDateTime

@Composable
fun CajaRoute(
    onBack: () -> Unit,
    onVerHistorial: () -> Unit,
    viewModel: CajaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CajaScreen(
        uiState = uiState,
        onBack = onBack,
        onVerHistorial = onVerHistorial,
        onAbrirFormulario = viewModel::abrirFormulario,
        onMontoChange = viewModel::onMontoChange,
        onAbrirCaja = viewModel::abrirCaja,
        onCerrarFormulario = viewModel::cerrarFormulario,
        onAbrirCierre = viewModel::abrirCierre,
        onMontoRealChange = viewModel::onMontoRealChange,
        onNotaChange = viewModel::onNotaChange,
        onConfirmarCierre = viewModel::cerrarCaja,
        onCerrarCierre = viewModel::cerrarCierre,
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
    onVerHistorial: () -> Unit,
    onAbrirFormulario: () -> Unit,
    onMontoChange: (String) -> Unit,
    onAbrirCaja: () -> Unit,
    onCerrarFormulario: () -> Unit,
    onAbrirCierre: () -> Unit,
    onMontoRealChange: (String) -> Unit,
    onNotaChange: (String) -> Unit,
    onConfirmarCierre: () -> Unit,
    onCerrarCierre: () -> Unit,
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

            // HU-23. Se ofrece siempre: lo de días anteriores se consulta igual con la caja
            // abierta que sin ella.
            if (!uiState.cargando) {
                GvTextLink(
                    text = stringResource(R.string.caja_ver_historial),
                    onClick = onVerHistorial,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }

        // Mientras no se sepa si hay caja no se ofrece ninguna de las dos acciones: proponer
        // "abrir" para cambiarlo por "cerrar" un instante después es peor que esperar.
        if (!uiState.cargando) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                GvPrimaryButton(
                    text = stringResource(
                        if (uiState.caja == null) R.string.caja_abrir else R.string.caja_cerrar,
                    ),
                    onClick = if (uiState.caja == null) onAbrirFormulario else onAbrirCierre,
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

    uiState.cierre?.let { cierre ->
        CierreSheet(
            cierre = cierre,
            saldoEsperado = uiState.caja?.saldoEsperado ?: 0.0,
            diferencia = uiState.diferencia,
            error = uiState.error,
            onMontoRealChange = onMontoRealChange,
            onNotaChange = onNotaChange,
            onConfirmar = onConfirmarCierre,
            onCerrar = onCerrarCierre,
        )
    }
}

/**
 * HU-20. La jornada en curso: cuánto debería haber en la caja ahora mismo.
 *
 * El saldo va arriba y grande porque es lo que se compara contra los billetes al cerrar; el
 * desglose va debajo porque un saldo que no cuadra no dice nada sin saber de dónde salió.
 */
@Composable
private fun JornadaAbierta(caja: CajaAbiertaUi, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.caja_jornada_en_curso)) {
            StatusPill(
                text = stringResource(R.string.caja_estado_abierta),
                containerColor = GestorVenturesTheme.colors.ingresoContainer,
                contentColor = GestorVenturesTheme.colors.ingreso,
            )
        }

        Text(
            text = stringResource(R.string.caja_saldo_esperado),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatPesos(caja.saldoEsperado),
            style = NumericTextStyle.copy(fontSize = 30.sp),
            // En rojo si quedó en negativo: es justo el caso que hay que mirar.
            color = if (caja.saldoEsperado < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
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

    GvCard {
        GvCardHeader(title = stringResource(R.string.caja_desglose))

        LineaDelDesglose(
            etiqueta = stringResource(R.string.caja_monto_inicial_etiqueta),
            monto = caja.montoInicial,
        )
        LineaDelDesglose(
            etiqueta = stringResource(R.string.caja_ventas_del_turno),
            monto = caja.ventas,
            entra = true,
        )
        LineaDelDesglose(
            etiqueta = stringResource(R.string.caja_gastos_del_turno),
            monto = caja.gastos,
            entra = false,
        )
        LineaDelDesglose(
            etiqueta = stringResource(R.string.caja_costos_del_turno),
            monto = caja.costos,
            entra = false,
        )

        if (caja.sinMovimientos) {
            Text(
                text = stringResource(R.string.caja_sin_movimientos),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Una línea del desglose. [entra] decide el signo y el color; en el monto inicial va nulo
 * porque no entró ni salió: ya estaba ahí.
 */
@Composable
private fun LineaDelDesglose(etiqueta: String, monto: Double, entra: Boolean? = null) {
    val color = when (entra) {
        true -> GestorVenturesTheme.colors.success
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurface
    }
    val signo = when (entra) {
        true -> "+ "
        false -> "− "
        null -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = signo + formatPesos(monto),
            style = NumericTextStyle.copy(fontSize = 15.sp),
            color = color,
        )
    }
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
                ventas = 92_500.0,
                gastos = 12_000.0,
                costos = 8_000.0,
                saldoEsperado = 122_500.0,
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
                onVerHistorial = {},
                onAbrirFormulario = {},
                onMontoChange = {},
                onAbrirCaja = {},
                onCerrarFormulario = {},
                onAbrirCierre = {},
                onMontoRealChange = {},
                onNotaChange = {},
                onConfirmarCierre = {},
                onCerrarCierre = {},
            )
        }
    }
}
