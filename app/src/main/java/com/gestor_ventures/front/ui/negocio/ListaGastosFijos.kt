package com.gestor_ventures.front.ui.negocio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.GastoFijo
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.MoneyText

/**
 * HU-06. Los gastos fijos del negocio con su total.
 *
 * La usan el paso 2 del onboarding y la pantalla de configuración. Tocar un gasto abre lo que
 * se puede hacer con él; el botón de agregar lo pone cada pantalla donde le corresponde, que no
 * es el mismo sitio en un formulario largo que en una lista.
 */
@Composable
fun ListaGastosFijos(
    gastos: List<GastoFijo>,
    total: Double,
    onAcciones: (GastoFijo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.base_gastos_fijos).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.1.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (gastos.isNotEmpty()) {
                MoneyText(monto = total, style = MaterialTheme.typography.labelMedium)
            }
        }

        gastos.forEach { gasto ->
            FilaGastoFijo(gasto = gasto, onClick = { onAcciones(gasto) })
        }
    }
}

@Composable
private fun FilaGastoFijo(
    gasto: GastoFijo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etiqueta = stringResource(R.string.base_acciones_gasto, gasto.nombre)

    GvCard(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { contentDescription = etiqueta },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = gasto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(gasto.frecuencia.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(monto = gasto.monto)
        }
    }
}
