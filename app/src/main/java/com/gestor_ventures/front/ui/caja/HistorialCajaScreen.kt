package com.gestor_ventures.front.ui.caja

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.model.labelRes
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatLongDate
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDateTime
import kotlin.math.abs

@Composable
fun HistorialCajaRoute(onBack: () -> Unit, viewModel: HistorialCajaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistorialCajaScreen(
        uiState = uiState,
        onBack = onBack,
        onPeriodoChange = viewModel::onPeriodoChange,
    )
}

/**
 * HU-23. Cómo terminó cada jornada anterior.
 *
 * Lo primero que se ve es cuántas no cuadraron, no cuántas hubo: diez cierres no dicen nada,
 * tres descuadres sí. Cada fila muestra lo esperado y lo contado juntos, porque la diferencia
 * sola no deja revisar de dónde salió.
 */
@Composable
fun HistorialCajaScreen(
    uiState: HistorialCajaUiState,
    onBack: () -> Unit,
    onPeriodoChange: (PeriodoPredefinido) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.historial_caja_titulo), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PeriodoPredefinido.entries.forEach { periodo ->
                    GvChip(
                        text = stringResource(periodo.labelRes()),
                        selected = periodo == uiState.periodo,
                        onClick = { onPeriodoChange(periodo) },
                    )
                }
            }

            if (!uiState.cargando) {
                ResumenDelPeriodo(uiState)
            }

            if (uiState.vacio) {
                GvInfoNote(stringResource(R.string.historial_caja_vacio))
            }

            uiState.cierres.forEach { cierre ->
                key(cierre.id) { FilaDeCierre(cierre) }
            }
        }
    }
}

/** Cuántas jornadas hubo y en cuántas no cuadró la caja. */
@Composable
private fun ResumenDelPeriodo(uiState: HistorialCajaUiState, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Dato(
                etiqueta = stringResource(R.string.historial_caja_cierres),
                valor = uiState.cantidad.toString(),
                modifier = Modifier.weight(1f),
            )
            Dato(
                etiqueta = stringResource(R.string.historial_caja_con_diferencia),
                valor = uiState.conDiferencia.toString(),
                // En rojo solo cuando hay alguna: un cero en rojo asusta sin motivo.
                color = if (uiState.conDiferencia > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Dato(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = NumericTextStyle.copy(fontSize = 22.sp),
            color = color,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Una jornada: cuándo fue, qué se esperaba, qué se contó y si cuadró. */
@Composable
private fun FilaDeCierre(cierre: CierreUi, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        GvCardHeader(title = formatLongDate(cierre.cierre.toLocalDate())) {
            EstadoDelCierre(cierre)
        }

        Text(
            text = stringResource(
                R.string.historial_caja_horario,
                formatHour(cierre.apertura.toLocalTime()),
                formatHour(cierre.cierre.toLocalTime()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.outline,
        )

        Linea(stringResource(R.string.caja_monto_inicial_etiqueta), cierre.montoInicial)
        Linea(stringResource(R.string.historial_caja_esperado), cierre.esperado)
        Linea(stringResource(R.string.historial_caja_contado), cierre.real)

        cierre.nota?.let { nota ->
            Text(
                text = nota,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** "Cuadró", "Sobran $2.500" o "Faltan $1.500": en plata y no en signos. */
@Composable
private fun EstadoDelCierre(cierre: CierreUi) {
    val (texto, color) = when {
        cierre.cuadra -> stringResource(R.string.historial_caja_cuadro) to
            GestorVenturesTheme.colors.success

        cierre.sobra -> stringResource(R.string.caja_sobra, formatPesos(abs(cierre.diferencia))) to
            GestorVenturesTheme.colors.success

        else -> stringResource(R.string.caja_falta, formatPesos(abs(cierre.diferencia))) to
            MaterialTheme.colorScheme.error
    }

    StatusPill(
        text = texto,
        containerColor = color.copy(alpha = 0.15f),
        contentColor = color,
    )
}

@Composable
private fun Linea(etiqueta: String, monto: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatPesos(monto),
            style = NumericTextStyle.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Preview(name = "Historial de caja", widthDp = 380, heightDp = 820)
@Preview(
    name = "Historial de caja (oscuro)",
    widthDp = 380,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HistorialCajaPreview() {
    val dia = LocalDateTime.of(2026, 10, 15, 8, 0)
    VistaPreviaHistorial(
        HistorialCajaUiState(
            cargando = false,
            cierres = listOf(
                CierreUi(
                    id = 3,
                    apertura = dia.withHour(8),
                    cierre = dia.withHour(14).withMinute(30),
                    montoInicial = 50_000.0,
                    esperado = 142_500.0,
                    real = 141_000.0,
                    diferencia = -1_500.0,
                    cuadra = false,
                    sobra = false,
                    nota = "Se dio vuelto de más en una venta",
                ),
                CierreUi(
                    id = 2,
                    apertura = dia.minusDays(1).withHour(8),
                    cierre = dia.minusDays(1).withHour(18),
                    montoInicial = 50_000.0,
                    esperado = 98_000.0,
                    real = 98_000.0,
                    diferencia = 0.0,
                    cuadra = true,
                    sobra = false,
                ),
            ),
        ),
    )
}

@Preview(name = "Historial sin cierres", widthDp = 380, heightDp = 820)
@Composable
private fun HistorialCajaVacioPreview() {
    VistaPreviaHistorial(HistorialCajaUiState(cargando = false))
}

@Composable
private fun VistaPreviaHistorial(uiState: HistorialCajaUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistorialCajaScreen(uiState = uiState, onBack = {}, onPeriodoChange = {})
        }
    }
}
