package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.theme.GestorVenturesTheme

@Composable
fun MetodosPagoRoute(
    onBack: () -> Unit,
    viewModel: MetodosPagoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MetodosPagoScreen(uiState = uiState, onBack = onBack, onCambio = viewModel::onCambio)
}

/**
 * HU-11. Los métodos de pago que acepta el negocio. Los que se apagan acá no aparecen al
 * registrar una venta; las ventas ya guardadas con ellos no cambian.
 */
@Composable
fun MetodosPagoScreen(
    uiState: MetodosPagoUiState,
    onBack: () -> Unit,
    onCambio: (MetodoPagoUi, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.menu_metodos_pago), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.metodos_pago_explicacion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState.sinNegocio) {
                GvInfoNote(stringResource(R.string.asistente_sin_negocio))
            }

            GvCard {
                MetodoPagoUi.entries.forEachIndexed { indice, metodo ->
                    if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    FilaMetodo(
                        metodo = metodo,
                        activo = metodo in uiState.activos,
                        habilitado = !uiState.sinNegocio,
                        onCambio = { onCambio(metodo, it) },
                    )
                }
            }

            if (uiState.avisoUltimo) {
                GvInfoNote(
                    text = stringResource(R.string.metodos_pago_minimo),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@DrawableRes
private fun MetodoPagoUi.iconRes(): Int = when (this) {
    MetodoPagoUi.Efectivo -> R.drawable.ic_cash
    MetodoPagoUi.Transferencia -> R.drawable.ic_building
    MetodoPagoUi.Tarjeta -> R.drawable.ic_card
}

@Composable
private fun FilaMetodo(
    metodo: MetodoPagoUi,
    activo: Boolean,
    habilitado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = activo, enabled = habilitado, role = Role.Switch, onValueChange = onCambio)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(metodo.iconRes()),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(metodo.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    if (activo) R.string.metodos_pago_activo else R.string.metodos_pago_inactivo,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // El toggleable de la fila maneja el toque y la accesibilidad; el Switch solo se dibuja.
        Switch(checked = activo, onCheckedChange = null, enabled = habilitado)
    }
}

@Preview(name = "Métodos de pago", widthDp = 380, heightDp = 700)
@Preview(name = "Métodos de pago (oscuro)", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MetodosPagoPreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MetodosPagoScreen(
                uiState = MetodosPagoUiState(activos = setOf(MetodoPagoUi.Efectivo), avisoUltimo = true),
                onBack = {},
                onCambio = { _, _ -> },
            )
        }
    }
}
