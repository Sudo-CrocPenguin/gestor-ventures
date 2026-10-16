package com.gestor_ventures.front.ui.reportes

import android.content.res.Configuration
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.usecase.EscalaDeTiempo
import com.gestor_ventures.front.components.ColoresDeTendencia
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvGraficoDeTendencia
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.SerieDeTendencia
import com.gestor_ventures.front.components.TrazoDeLinea
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatDiaCorto
import com.gestor_ventures.front.util.formatMesYAnio
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDate

@Composable
fun ReportesRoute(onBack: () -> Unit, viewModel: ReportesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReportesScreen(uiState = uiState, onBack = onBack, onEscalaChange = viewModel::onEscalaChange)
}

/**
 * HU-40. Cómo vienen los ingresos y los egresos a lo largo del tiempo.
 *
 * El gráfico va arriba porque la pregunta es "¿voy subiendo o bajando?", y eso se responde con
 * la forma de la línea, no con un número. Los totales van debajo, para cuando ya se vio la
 * tendencia y se quiere la cifra.
 */
@Composable
fun ReportesScreen(
    uiState: ReportesUiState,
    onBack: () -> Unit,
    onEscalaChange: (EscalaDeTiempo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.reportes_titulo), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EscalaDeTiempo.entries.forEach { escala ->
                    GvChip(
                        text = stringResource(escala.labelRes()),
                        selected = escala == uiState.escala,
                        onClick = { onEscalaChange(escala) },
                    )
                }
            }

            if (!uiState.cargando) {
                GvCard {
                    GvCardHeader(title = stringResource(R.string.reportes_ingresos_vs_egresos))

                    GvGraficoDeTendencia(
                        series = listOf(
                            SerieDeTendencia(
                                nombre = stringResource(R.string.reportes_ingresos),
                                valores = uiState.puntos.map { it.ingresos },
                                color = ColoresDeTendencia.ingresos,
                            ),
                            SerieDeTendencia(
                                nombre = stringResource(R.string.reportes_gastos),
                                valores = uiState.puntos.map { it.gastos },
                                color = ColoresDeTendencia.gastos,
                                trazo = TrazoDeLinea.Guiones,
                            ),
                            SerieDeTendencia(
                                nombre = stringResource(R.string.reportes_costos),
                                valores = uiState.puntos.map { it.costos },
                                color = ColoresDeTendencia.costos,
                                trazo = TrazoDeLinea.Puntos,
                            ),
                        ),
                        etiquetas = uiState.puntos.map { etiquetaDe(it.inicio, uiState.escala) },
                    )
                }

                TotalesDelPeriodo(uiState)
            }

            if (uiState.vacio) {
                GvInfoNote(stringResource(R.string.reportes_vacio))
            }
        }
    }
}

/** Lo que sumó todo el periodo, debajo del gráfico. */
@Composable
private fun TotalesDelPeriodo(uiState: ReportesUiState, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Dato(
                etiqueta = stringResource(R.string.reportes_ingresos_del_periodo),
                monto = uiState.totalIngresos,
                color = ColoresDeTendencia.ingresos,
                modifier = Modifier.weight(1f),
            )
            Dato(
                etiqueta = stringResource(R.string.reportes_gastos_del_periodo),
                monto = uiState.totalGastos,
                color = ColoresDeTendencia.gastos,
                modifier = Modifier.weight(1f),
            )
        }

        Row(Modifier.padding(top = 12.dp).fillMaxWidth()) {
            Dato(
                etiqueta = stringResource(R.string.reportes_costos_del_periodo),
                monto = uiState.totalCostos,
                color = ColoresDeTendencia.costos,
                modifier = Modifier.weight(1f),
            )
            Dato(
                etiqueta = stringResource(R.string.reportes_egresos_del_periodo),
                monto = uiState.totalEgresos,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outline,
        )

        Dato(
            etiqueta = stringResource(R.string.reportes_ganancia),
            monto = uiState.ganancia,
            // En rojo si quedó en negativo: gastaste más de lo que entró.
            color = if (uiState.ganancia < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun Dato(etiqueta: String, monto: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatPesos(monto),
            style = NumericTextStyle.copy(fontSize = 19.sp),
            color = color,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** "13 oct" por semana, "oct 2026" por mes: cada escala se lee distinto. */
@Composable
private fun etiquetaDe(inicio: LocalDate, escala: EscalaDeTiempo): String = when (escala) {
    EscalaDeTiempo.Semanal -> formatDiaCorto(inicio)
    EscalaDeTiempo.Mensual -> formatMesYAnio(inicio).substringBefore(' ')
}

@Preview(name = "Reportes", widthDp = 380, heightDp = 780)
@Preview(
    name = "Reportes (oscuro)",
    widthDp = 380,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ReportesPreview() {
    val mes = LocalDate.of(2026, 5, 1)
    VistaPreviaReportes(
        ReportesUiState(
            cargando = false,
            puntos = listOf(
                PuntoUi(mes, 820_000.0, 260_000.0, 170_000.0),
                PuntoUi(mes.plusMonths(1), 940_000.0, 300_000.0, 210_000.0),
                PuntoUi(mes.plusMonths(2), 760_000.0, 280_000.0, 190_000.0),
                PuntoUi(mes.plusMonths(3), 1_180_000.0, 320_000.0, 240_000.0),
                PuntoUi(mes.plusMonths(4), 1_050_000.0, 290_000.0, 230_000.0),
                PuntoUi(mes.plusMonths(5), 1_385_000.0, 340_000.0, 270_000.0),
            ),
        ),
    )
}

@Preview(name = "Reportes sin movimientos", widthDp = 380, heightDp = 780)
@Composable
private fun ReportesVacioPreview() {
    val mes = LocalDate.of(2026, 5, 1)
    VistaPreviaReportes(
        ReportesUiState(
            cargando = false,
            puntos = (0..5).map { PuntoUi(mes.plusMonths(it.toLong()), 0.0, 0.0, 0.0) },
        ),
    )
}

@Composable
private fun VistaPreviaReportes(uiState: ReportesUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ReportesScreen(uiState = uiState, onBack = {}, onEscalaChange = {})
        }
    }
}
