package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.GastoFijo
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPasoTopBar
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvSoftButton
import com.gestor_ventures.front.components.GvDateField
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDate

private const val PasoActual = 2
private const val TotalPasos = 2

@Composable
fun BaseFinancieraRoute(
    onBack: () -> Unit,
    onConfiguracionLista: () -> Unit,
    viewModel: BaseFinancieraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.configuracionLista.collect { onConfiguracionLista() }
    }

    BaseFinancieraScreen(
        uiState = uiState,
        onBack = onBack,
        onAgregarGasto = viewModel::abrirFormularioGasto,
        onEliminarGasto = viewModel::eliminarGastoFijo,
        onMetaMontoChange = viewModel::onMetaMontoChange,
        onFechaLimiteChange = viewModel::onFechaLimiteChange,
        onPorcentajeChange = viewModel::onPorcentajeReinversionChange,
        onFinalizar = viewModel::finalizar,
    )

    uiState.formularioGasto?.let { formulario ->
        GastoFijoSheet(
            formulario = formulario,
            error = uiState.error,
            onNombreChange = viewModel::onNombreGastoChange,
            onMontoChange = viewModel::onMontoGastoChange,
            onFrecuenciaChange = viewModel::onFrecuenciaGastoChange,
            onGuardar = viewModel::guardarGastoFijo,
            onCerrar = viewModel::cerrarFormularioGasto,
        )
    }
}

/**
 * HU-06, HU-08 y HU-09. Paso 2 del onboarding: gastos fijos del mes, meta de ahorro con su
 * fecha límite y cuánto de la ganancia se reinvierte. Todo es opcional.
 */
@Composable
fun BaseFinancieraScreen(
    uiState: BaseFinancieraUiState,
    onBack: () -> Unit,
    onAgregarGasto: () -> Unit,
    onEliminarGasto: (Long) -> Unit,
    onMetaMontoChange: (String) -> Unit,
    onFechaLimiteChange: (LocalDate) -> Unit,
    onPorcentajeChange: (Int) -> Unit,
    onFinalizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvPasoTopBar(paso = PasoActual, totalPasos = TotalPasos, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Encabezado()

            ListaGastosFijos(
                gastos = uiState.gastosFijos,
                total = uiState.totalGastosFijos,
                onAgregar = onAgregarGasto,
                onEliminar = onEliminarGasto,
            )

            Column {
                GvTextField(
                    label = stringResource(R.string.base_meta),
                    value = uiState.metaFormateada,
                    onValueChange = onMetaMontoChange,
                    placeholder = stringResource(R.string.form_monto_placeholder),
                    keyboardType = KeyboardType.Number,
                    textStyle = NumericTextStyle.copy(fontSize = 17.sp),
                    leading = { TextoAyuda(stringResource(R.string.form_moneda_simbolo)) },
                    trailing = { TextoAyuda(stringResource(R.string.form_moneda)) },
                )
                Ayuda(stringResource(R.string.base_meta_ayuda))
            }

            Column {
                GvDateField(
                    label = stringResource(R.string.base_fecha_limite),
                    fecha = uiState.fechaLimite,
                    onFechaChange = onFechaLimiteChange,
                    placeholder = stringResource(R.string.base_fecha_limite_placeholder),
                )
                Ayuda(stringResource(R.string.base_fecha_limite_ayuda))
            }

            AnimatedVisibility(visible = uiState.ahorroMensual != null) {
                GvInfoNote(
                    text = stringResource(
                        R.string.base_ahorro_mensual,
                        formatPesos(uiState.ahorroMensual ?: 0.0),
                    ),
                )
            }

            Reinversion(
                porcentaje = uiState.porcentajeReinversion,
                onPorcentajeChange = onPorcentajeChange,
            )

            // Mientras la hoja está abierta el aviso se muestra dentro de ella, no detrás.
            AnimatedVisibility(visible = uiState.error != null && uiState.formularioGasto == null) {
                GvInfoNote(
                    text = uiState.error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.base_finalizar),
                onClick = onFinalizar,
                enabled = !uiState.guardando,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun Encabezado(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.base_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.base_subtitulo),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun Reinversion(
    porcentaje: Int,
    onPorcentajeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.base_reinversion),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.base_reinversion_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.porcentaje, porcentaje),
                style = MaterialTheme.typography.titleLarge.merge(NumericTextStyle),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = porcentaje.toFloat(),
            onValueChange = { onPorcentajeChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}

@Composable
private fun Ayuda(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun TextoAyuda(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Claro", widthDp = 380, heightDp = 900)
@Preview(name = "Oscuro", widthDp = 380, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BaseFinancieraScreenPreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BaseFinancieraScreen(
                uiState = BaseFinancieraUiState(
                    gastosFijos = listOf(
                        GastoFijo(1, "Arriendo local", 300_000.0, Frecuencia.MENSUAL),
                        GastoFijo(2, "Servicios (luz, agua)", 120_000.0, Frecuencia.MENSUAL),
                    ),
                    metaMonto = "2000000",
                    fechaLimite = LocalDate.of(2026, 12, 31),
                    porcentajeReinversion = 20,
                    ahorroMensual = 500_000.0,
                ),
                onBack = {},
                onAgregarGasto = {},
                onEliminarGasto = {},
                onMetaMontoChange = {},
                onFechaLimiteChange = {},
                onPorcentajeChange = {},
                onFinalizar = {},
            )
        }
    }
}
