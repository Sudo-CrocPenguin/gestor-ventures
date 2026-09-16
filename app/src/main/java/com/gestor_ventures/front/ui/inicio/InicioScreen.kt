package com.gestor_ventures.front.ui.inicio

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestor_ventures.R
import com.gestor_ventures.front.components.QuickActionButton
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatLongDate
import java.time.LocalDate

@Composable
fun InicioRoute(
    onVerFinanzas: () -> Unit,
    onRegistrarVenta: () -> Unit,
    onAbrirCaja: () -> Unit,
    viewModel: InicioViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InicioScreen(
        uiState = uiState,
        onVerFinanzas = onVerFinanzas,
        onRegistrarVenta = onRegistrarVenta,
        onAbrirCaja = onAbrirCaja,
    )
}

@Composable
fun InicioScreen(
    uiState: InicioUiState,
    onVerFinanzas: () -> Unit,
    onRegistrarVenta: () -> Unit,
    onAbrirCaja: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Saludo(
            nombreUsuario = uiState.nombreUsuario,
            fecha = uiState.fecha,
            alertasNuevas = uiState.alertasNuevas,
        )
        ResumenHoyCard(resumen = uiState.resumenHoy, onVerFinanzas = onVerFinanzas)
        AccionesRapidas(onRegistrarVenta = onRegistrarVenta, onAbrirCaja = onAbrirCaja)
        CajasActivasCard(cajas = uiState.cajasActivas)
    }
}

@Composable
private fun Saludo(
    nombreUsuario: String,
    fecha: LocalDate,
    alertasNuevas: Int,
    modifier: Modifier = Modifier,
) {
    val fechaTexto = formatLongDate(fecha)
    val detalle = if (alertasNuevas > 0) {
        stringResource(
            R.string.inicio_fecha_y_alertas,
            fechaTexto,
            pluralStringResource(R.plurals.inicio_alertas_nuevas, alertasNuevas, alertasNuevas),
        )
    } else {
        fechaTexto
    }

    Column(modifier.padding(top = 2.dp)) {
        Text(
            text = stringResource(R.string.inicio_saludo, nombreUsuario),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun AccionesRapidas(
    onRegistrarVenta: () -> Unit,
    onAbrirCaja: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickActionButton(
            text = stringResource(R.string.inicio_registrar_venta),
            iconRes = R.drawable.ic_plus,
            onClick = onRegistrarVenta,
            highlighted = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        QuickActionButton(
            text = stringResource(R.string.inicio_abrir_caja),
            iconRes = R.drawable.ic_box,
            onClick = onAbrirCaja,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Preview(name = "Claro", widthDp = 380, heightDp = 780)
@Preview(name = "Oscuro", widthDp = 380, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InicioScreenPreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            InicioScreen(
                uiState = InicioPreviewData.uiState,
                onVerFinanzas = {},
                onRegistrarVenta = {},
                onAbrirCaja = {},
            )
        }
    }
}
