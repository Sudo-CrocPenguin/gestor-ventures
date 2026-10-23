package com.gestor_ventures.front.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvProgressBar
import com.gestor_ventures.front.components.GvTextLink
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.components.Sparkline
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import kotlin.math.abs
import kotlin.math.roundToInt

/** HU-16. Tarjeta "Resumen de hoy": ventas, gastos, tendencia del día y meta de ahorro. */
@Composable
fun ResumenHoyCard(
    resumen: ResumenHoyUi,
    onVerFinanzas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.inicio_resumen_hoy)) {
            GvTextLink(text = stringResource(R.string.inicio_ver_finanzas), onClick = onVerFinanzas)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Kpi(
                label = stringResource(R.string.inicio_ventas),
                monto = resumen.ventas,
                detalle = resumen.variacionVentasVsAyer?.let { variacionTexto(it) },
                montoColor = GestorVenturesTheme.colors.success,
                modifier = Modifier.weight(1f),
            )
            Kpi(
                label = stringResource(R.string.inicio_gastos),
                monto = resumen.gastos,
                detalle = pluralStringResource(R.plurals.inicio_registros, resumen.cantidadGastos, resumen.cantidadGastos),
                modifier = Modifier.weight(1f),
            )
        }
        Sparkline(
            values = resumen.tendenciaVentas,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(40.dp),
        )
        resumen.progresoMetaAhorro?.let { progreso ->
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 13.dp),
                color = MaterialTheme.colorScheme.outline,
            )
            MetaAhorro(progreso = progreso)
        }
    }
}

@Composable
private fun Kpi(
    label: String,
    monto: Double,
    detalle: String?,
    modifier: Modifier = Modifier,
    montoColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            monto = monto,
            style = MaterialTheme.typography.titleLarge,
            color = montoColor,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (detalle != null) {
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

@Composable
private fun variacionTexto(variacion: Int): String =
    if (variacion >= 0) {
        stringResource(R.string.inicio_variacion_sube, variacion)
    } else {
        stringResource(R.string.inicio_variacion_baja, abs(variacion))
    }

@Composable
private fun MetaAhorro(progreso: Float, modifier: Modifier = Modifier) {
    val porcentaje = (progreso.coerceIn(0f, 1f) * 100).roundToInt()
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.inicio_meta_ahorro),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.porcentaje, porcentaje),
                style = MaterialTheme.typography.labelLarge.merge(NumericTextStyle),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        GvProgressBar(progress = progreso)
    }
}
