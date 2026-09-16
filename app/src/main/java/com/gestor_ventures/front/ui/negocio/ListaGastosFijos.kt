package com.gestor_ventures.front.ui.negocio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.GastoFijo
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvSoftButton
import com.gestor_ventures.front.components.MoneyText

/**
 * HU-06. Los gastos fijos del negocio con su total y el botón para agregar otro.
 *
 * La usan el paso 2 del onboarding y la pantalla de configuración. [onEditar] es opcional
 * porque en el onboarding el usuario acaba de escribir el gasto: ahí corregir es volver a
 * escribirlo, y el lugar para administrarlos es la configuración.
 */
@Composable
fun ListaGastosFijos(
    gastos: List<GastoFijo>,
    total: Double,
    onAgregar: () -> Unit,
    onEliminar: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onEditar: ((GastoFijo) -> Unit)? = null,
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
            FilaGastoFijo(
                gasto = gasto,
                onEditar = onEditar,
                onEliminar = { onEliminar(gasto.id) },
            )
        }

        GvSoftButton(
            text = stringResource(R.string.base_agregar_gasto),
            onClick = onAgregar,
            iconRes = R.drawable.ic_plus,
        )
    }
}

@Composable
private fun FilaGastoFijo(
    gasto: GastoFijo,
    onEditar: ((GastoFijo) -> Unit)?,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onEditar == null) {
                        Modifier
                    } else {
                        Modifier.clickable { onEditar(gasto) }
                    },
                ),
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
            IconButton(onClick = onEliminar) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = stringResource(
                        R.string.base_eliminar_gasto,
                        gasto.nombre,
                    ),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
