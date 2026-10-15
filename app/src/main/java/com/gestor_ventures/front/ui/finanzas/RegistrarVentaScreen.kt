package com.gestor_ventures.front.ui.finanzas

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvReadOnlyField
import com.gestor_ventures.front.components.GvSegmentedToggle
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatLongDate

@Composable
fun RegistrarVentaRoute(
    onBack: () -> Unit,
    onVentaGuardada: (TipoRegistroVentaUi) -> Unit,
    viewModel: RegistrarVentaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // La pantalla se cierra cuando la venta ya quedó guardada, no cuando se toca el botón.
    LaunchedEffect(viewModel) {
        viewModel.ventaGuardada.collect { tipo -> onVentaGuardada(tipo) }
    }

    RegistrarVentaScreen(
        uiState = uiState,
        onBack = onBack,
        onTipoRegistroChange = viewModel::onTipoRegistroChange,
        onMontoChange = viewModel::onMontoChange,
        onProductoServicioChange = viewModel::onProductoServicioChange,
        onMetodoPagoChange = viewModel::onMetodoPagoChange,
        onNotaChange = viewModel::onNotaChange,
        onGuardar = viewModel::guardar,
    )
}

/**
 * HU-11/HU-12. Registro de venta en dos modalidades:
 * - **Detallada**: monto, qué se vendió y método de pago.
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

            CamposDeVenta(
                esDetallada = uiState.esDetallada,
                montoFormateado = uiState.montoFormateado,
                productoServicio = uiState.productoServicio,
                metodoPago = uiState.metodoPago,
                nota = uiState.nota,
                onMontoChange = onMontoChange,
                onProductoServicioChange = onProductoServicioChange,
                onMetodoPagoChange = onMetodoPagoChange,
                onNotaChange = onNotaChange,
            )

            AnimatedVisibility(visible = uiState.error != null) {
                GvInfoNote(
                    text = uiState.error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
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
                onMetodoPagoChange = {},
                onNotaChange = {},
                onGuardar = {},
            )
        }
    }
}
