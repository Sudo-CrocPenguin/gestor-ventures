package com.gestor_ventures.front.ui.finanzas

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ProgresoMeta
import com.gestor_ventures.back.model.ResumenFinanciero
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvProgressBar
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatMesLargo
import com.gestor_ventures.front.util.formatMesYAnio
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun ResumenFinancieroRoute(viewModel: ResumenFinancieroViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ResumenFinancieroScreen(uiState = uiState)
}

/**
 * HU-16. Cómo le fue al negocio este mes y cuánto de eso se puede sacar hoy.
 *
 * El orden responde a tres preguntas seguidas: qué pasó (ingresos y egresos), qué quedó
 * (la ganancia) y de eso qué es mío (lo disponible, después de descontar lo comprometido).
 */
@Composable
fun ResumenFinancieroScreen(
    uiState: ResumenFinancieroUiState,
    modifier: Modifier = Modifier,
) {
    val resumen = uiState.resumen ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Un mes sin un solo movimiento se dice con una frase; un tablero de ceros no dice nada.
        if (resumen.sinMovimientos) {
            GvInfoNote(stringResource(R.string.resumen_vacio))
        } else {
            MovimientosCard(resumen)
            DisponibleCard(resumen)
            Nota(enRojo = resumen.disponible < 0)
        }

        resumen.progresoMeta?.let { MetaCard(it) }
    }
}

/** Lo que entró y lo que salió, con la ganancia como cierre de la cuenta. */
@Composable
private fun MovimientosCard(resumen: ResumenFinanciero, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.resumen_movimientos)) {
            StatusPill(text = formatMesLargo(resumen.mes.atDay(1)))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(
                label = stringResource(R.string.resumen_ingresos),
                monto = resumen.ingresos,
                color = GestorVenturesTheme.colors.success,
                modifier = Modifier.weight(1f),
            )
            Kpi(
                label = stringResource(R.string.resumen_gastos),
                monto = resumen.gastos,
                modifier = Modifier.weight(1f),
            )
        }

        Separador()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(
                label = stringResource(R.string.resumen_costos),
                monto = resumen.costos,
                modifier = Modifier.weight(1f),
            )
            Kpi(
                label = stringResource(R.string.resumen_gastos_fijos),
                monto = resumen.gastosFijos,
                modifier = Modifier.weight(1f),
            )
        }

        Separador()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Kpi(
                label = stringResource(R.string.resumen_ganancia),
                monto = resumen.ganancia,
                color = colorDeSaldo(resumen.ganancia),
                modifier = Modifier.weight(1f),
            )
            resumen.margen?.let { margen ->
                Column(Modifier.weight(1f)) {
                    Etiqueta(stringResource(R.string.resumen_margen))
                    Text(
                        text = stringResource(R.string.porcentaje, (margen * 100).roundToInt()),
                        style = MaterialTheme.typography.titleLarge.merge(NumericTextStyle),
                        color = colorDeSaldo(resumen.ganancia),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * HU-16. Lo que de verdad se puede sacar, con el descuento a la vista.
 *
 * El desglose no es adorno: un número solo diría "te quedan $500.000" sin explicar por qué no
 * son los $3.000.000 de ganancia que el emprendedor acaba de leer arriba.
 */
@Composable
private fun DisponibleCard(resumen: ResumenFinanciero, modifier: Modifier = Modifier) {
    val acento = GestorVenturesTheme.colors.acento

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Etiqueta(stringResource(R.string.resumen_disponible).uppercase(), color = acento)
        MoneyText(
            monto = resumen.disponible,
            style = MaterialTheme.typography.headlineSmall,
            color = if (resumen.disponible < 0) MaterialTheme.colorScheme.error else acento,
            modifier = Modifier.padding(top = 2.dp),
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = acento.copy(alpha = 0.18f),
        )

        LineaDeDescuento(stringResource(R.string.resumen_ganancia), resumen.ganancia, acento)
        LineaDeDescuento(
            texto = stringResource(R.string.resumen_menos_obligaciones),
            monto = resumen.obligacionesPendientes,
            color = acento,
        )
        LineaDeDescuento(
            texto = stringResource(R.string.resumen_menos_meta),
            monto = resumen.apartadoParaMeta,
            color = acento,
        )
        LineaDeDescuento(
            texto = stringResource(
                R.string.resumen_menos_reinversion,
                resumen.porcentajeReinversion.roundToInt(),
            ),
            monto = resumen.reinversion,
            color = acento,
        )

        // HU-09: sin porcentaje configurado la línea siempre dice cero, y eso no se explica solo.
        if (resumen.porcentajeReinversion == 0.0) {
            Text(
                text = stringResource(R.string.resumen_sin_reinversion),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = acento.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** La frase que cierra el resumen. Cambia de tono cuando el mes no alcanza. */
@Composable
private fun Nota(enRojo: Boolean, modifier: Modifier = Modifier) {
    GvInfoNote(
        text = stringResource(
            if (enRojo) R.string.resumen_nota_en_rojo else R.string.resumen_nota,
        ),
        containerColor = if (enRojo) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (enRojo) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        modifier = modifier,
    )
}

@Composable
private fun LineaDeDescuento(
    texto: String,
    monto: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = color,
        )
        Text(
            text = formatPesos(monto),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp)
                .merge(NumericTextStyle),
            color = color,
        )
    }
}

/** HU-08. Cuánto lleva el negocio de su meta y cuánto le falta. */
@Composable
private fun MetaCard(progreso: ProgresoMeta, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.resumen_meta_titulo)) {
            Text(
                text = stringResource(R.string.porcentaje, (progreso.fraccion * 100).roundToInt()),
                style = MaterialTheme.typography.labelLarge.merge(NumericTextStyle),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        GvProgressBar(progress = progreso.fraccion)

        Text(
            text = stringResource(
                R.string.resumen_meta_de,
                formatPesos(progreso.acumulado),
                formatPesos(progreso.montoObjetivo),
            ),
            style = MaterialTheme.typography.bodySmall.merge(NumericTextStyle),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp),
        )

        Text(
            text = if (progreso.cumplida) {
                stringResource(
                    R.string.resumen_meta_cumplida,
                    formatPesos(progreso.montoObjetivo),
                )
            } else {
                stringResource(
                    R.string.resumen_meta_falta,
                    formatPesos(progreso.falta),
                    formatMesYAnio(progreso.fechaLimite),
                )
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = if (progreso.cumplida) {
                GestorVenturesTheme.colors.success
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 3.dp),
        )

        Text(
            text = stringResource(R.string.resumen_meta_explicacion),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Kpi(
    label: String,
    monto: Double,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Etiqueta(label)
        MoneyText(
            monto = monto,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun Separador() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outline,
    )
}

/** Un mes en pérdida se lee en rojo: es la única forma de que no pase desapercibido. */
@Composable
private fun colorDeSaldo(monto: Double): Color = when {
    monto < 0 -> MaterialTheme.colorScheme.error
    monto > 0 -> GestorVenturesTheme.colors.success
    else -> MaterialTheme.colorScheme.onSurface
}

@Preview(name = "Resumen financiero", widthDp = 380, heightDp = 900)
@Preview(
    name = "Resumen financiero (oscuro)",
    widthDp = 380,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ResumenFinancieroPreview() {
    VistaPreviaResumen(
        ResumenFinanciero(
            mes = YearMonth.of(2026, 9),
            ingresos = 6_135_000.0,
            gastos = 1_860_000.0,
            costos = 1_240_000.0,
            gastosFijos = 1_000_000.0,
            obligacionesPendientes = 670_000.0,
            apartadoParaMeta = 500_000.0,
            porcentajeReinversion = 20.0,
            progresoMeta = ProgresoMeta(
                montoObjetivo = 2_000_000.0,
                acumulado = 1_200_000.0,
                fechaLimite = LocalDate.of(2026, 12, 31),
            ),
        ),
    )
}

@Preview(name = "Mes en pérdida", widthDp = 380, heightDp = 900)
@Composable
private fun ResumenEnPerdidaPreview() {
    VistaPreviaResumen(
        ResumenFinanciero(
            mes = YearMonth.of(2026, 9),
            ingresos = 800_000.0,
            gastos = 600_000.0,
            costos = 400_000.0,
            gastosFijos = 300_000.0,
        ),
    )
}

@Composable
private fun VistaPreviaResumen(resumen: ResumenFinanciero) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ResumenFinancieroScreen(
                ResumenFinancieroUiState(resumen = resumen, cargando = false),
            )
        }
    }
}
