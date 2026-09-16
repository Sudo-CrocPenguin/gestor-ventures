package com.gestor_ventures.front.ui.finanzas

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvReadOnlyField
import com.gestor_ventures.front.components.GvSegmentedToggle
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatLongDate

@Composable
fun RegistrarVentaRoute(
    onBack: () -> Unit,
    onVentaGuardada: (TipoRegistroVentaUi) -> Unit,
    viewModel: RegistrarVentaViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RegistrarVentaScreen(
        uiState = uiState,
        onBack = onBack,
        onTipoRegistroChange = viewModel::onTipoRegistroChange,
        onMontoChange = viewModel::onMontoChange,
        onProductoServicioChange = viewModel::onProductoServicioChange,
        onClienteChange = viewModel::onClienteChange,
        onMetodoPagoChange = viewModel::onMetodoPagoChange,
        onNotaChange = viewModel::onNotaChange,
        onGuardar = {
            val tipo = uiState.tipoRegistro
            viewModel.guardar()
            onVentaGuardada(tipo)
        },
    )
}

/**
 * HU-11/HU-12. Registro de venta en dos modalidades:
 * - **Detallada**: monto, qué se vendió, cliente (opcional) y método de pago.
 * - **Rápida**: solo el total vendido y una nota opcional.
 *
 * La fecha y la hora las pone el sistema; se muestran para que el usuario sepa con qué
 * quedará registrada la venta.
 */
@Composable
fun RegistrarVentaScreen(
    uiState: RegistrarVentaUiState,
    onBack: () -> Unit,
    onTipoRegistroChange: (TipoRegistroVentaUi) -> Unit,
    onMontoChange: (String) -> Unit,
    onProductoServicioChange: (String) -> Unit,
    onClienteChange: (String) -> Unit,
    onMetodoPagoChange: (MetodoPagoUi) -> Unit,
    onNotaChange: (String) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(
            titulo = stringResource(R.string.registrar_venta_titulo),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GvSegmentedToggle(
                opciones = TipoRegistroVentaUi.entries,
                seleccionada = uiState.tipoRegistro,
                etiqueta = { stringResource(it.labelRes) },
                onSeleccionar = onTipoRegistroChange,
            )

            // Debajo del selector: es el contexto de la venta, no un dato que se escriba.
            GvReadOnlyField(
                label = stringResource(R.string.venta_fecha_hora),
                value = formatLongDate(uiState.fechaHora.toLocalDate()),
                iconRes = R.drawable.ic_calendar,
                valueEnd = formatHour(uiState.fechaHora.toLocalTime()),
                iconEndRes = R.drawable.ic_clock,
            )

            AnimatedVisibility(visible = !uiState.esDetallada) {
                GvInfoNote(stringResource(R.string.venta_rapida_explicacion))
            }

            GvTextField(
                label = stringResource(
                    if (uiState.esDetallada) R.string.venta_monto else R.string.venta_total_vendido,
                ),
                value = uiState.montoFormateado,
                onValueChange = onMontoChange,
                placeholder = stringResource(R.string.form_monto_placeholder),
                keyboardType = KeyboardType.Number,
                textStyle = NumericTextStyle.copy(fontSize = 17.sp),
                leading = { TextoAuxiliar(stringResource(R.string.form_moneda_simbolo)) },
                trailing = { TextoAuxiliar(stringResource(R.string.form_moneda)) },
            )

            if (uiState.esDetallada) {
                GvTextField(
                    label = stringResource(R.string.venta_producto),
                    value = uiState.productoServicio,
                    onValueChange = onProductoServicioChange,
                    placeholder = stringResource(R.string.venta_producto_placeholder),
                )
                GvTextField(
                    label = stringResource(R.string.venta_cliente),
                    value = uiState.cliente,
                    onValueChange = onClienteChange,
                    placeholder = stringResource(R.string.venta_cliente_placeholder),
                )
                MetodoPago(
                    seleccionado = uiState.metodoPago,
                    onMetodoPagoChange = onMetodoPagoChange,
                )
            } else {
                GvTextField(
                    label = stringResource(R.string.venta_nota),
                    value = uiState.nota,
                    onValueChange = onNotaChange,
                    placeholder = stringResource(R.string.venta_nota_placeholder),
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            GvPrimaryButton(
                text = stringResource(R.string.registrar_venta_guardar),
                onClick = onGuardar,
                enabled = uiState.puedeGuardar,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun MetodoPago(
    seleccionado: MetodoPagoUi,
    onMetodoPagoChange: (MetodoPagoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.form_metodo_pago),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetodoPagoUi.entries.forEach { metodo ->
                GvChip(
                    text = stringResource(metodo.labelRes),
                    selected = metodo == seleccionado,
                    onClick = { onMetodoPagoChange(metodo) },
                )
            }
        }
    }
}

/** "$" y "COP" que acompañan al monto. */
@Composable
private fun TextoAuxiliar(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Detallada", widthDp = 380, heightDp = 820)
@Preview(
    name = "Detallada (oscuro)",
    widthDp = 380,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RegistrarVentaDetalladaPreview() {
    VistaPrevia(RegistrarVentaPreviewData.detallada)
}

@Preview(name = "Rápida", widthDp = 380, heightDp = 820)
@Composable
private fun RegistrarVentaRapidaPreview() {
    VistaPrevia(RegistrarVentaPreviewData.rapida)
}

@Composable
private fun VistaPrevia(uiState: RegistrarVentaUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RegistrarVentaScreen(
                uiState = uiState,
                onBack = {},
                onTipoRegistroChange = {},
                onMontoChange = {},
                onProductoServicioChange = {},
                onClienteChange = {},
                onMetodoPagoChange = {},
                onNotaChange = {},
                onGuardar = {},
            )
        }
    }
}
