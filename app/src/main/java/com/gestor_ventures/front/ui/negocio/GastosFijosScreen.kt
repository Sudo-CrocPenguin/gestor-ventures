package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.GastoFijo
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.theme.GestorVenturesTheme

@Composable
fun GastosFijosRoute(
    onBack: () -> Unit,
    viewModel: GastosFijosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GastosFijosScreen(
        uiState = uiState,
        onBack = onBack,
        onAgregar = viewModel::abrirFormularioNuevo,
        onEditar = viewModel::abrirFormularioDe,
        onEliminar = viewModel::eliminar,
        onNombreChange = viewModel::onNombreChange,
        onMontoChange = viewModel::onMontoChange,
        onFrecuenciaChange = viewModel::onFrecuenciaChange,
        onGuardarFormulario = viewModel::guardarFormulario,
        onCerrarFormulario = viewModel::cerrarFormulario,
    )
}

/**
 * HU-06. Gastos fijos del negocio, fuera del onboarding: se ven, se corrigen y se borran.
 *
 * Tocar un gasto lo abre para editarlo; el mismo formulario del onboarding, ya lleno.
 */
@Composable
fun GastosFijosScreen(
    uiState: GastosFijosUiState,
    onBack: () -> Unit,
    onAgregar: () -> Unit,
    onEditar: (GastoFijo) -> Unit,
    onEliminar: (Long) -> Unit,
    onNombreChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFrecuenciaChange: (Frecuencia) -> Unit,
    onGuardarFormulario: () -> Unit,
    onCerrarFormulario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(
            titulo = stringResource(R.string.menu_gastos_fijos),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.gastos_fijos_explicacion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // El aviso va antes de la lista: si no hay nada, lo primero que se lee es por qué
            // vale la pena agregarlos, y no el botón suelto.
            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.gastos_fijos_vacio))
            }

            ListaGastosFijos(
                gastos = uiState.gastosFijos,
                total = uiState.total,
                onAgregar = onAgregar,
                onEditar = onEditar,
                onEliminar = onEliminar,
            )
        }
    }

    uiState.formularioGasto?.let { formulario ->
        GastoFijoSheet(
            formulario = formulario,
            error = uiState.error,
            onNombreChange = onNombreChange,
            onMontoChange = onMontoChange,
            onFrecuenciaChange = onFrecuenciaChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )
    }
}

@Preview(name = "Con gastos", widthDp = 380, heightDp = 720)
@Preview(
    name = "Con gastos (oscuro)",
    widthDp = 380,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GastosFijosScreenPreview() {
    VistaPrevia(
        GastosFijosUiState(
            cargando = false,
            gastosFijos = listOf(
                GastoFijo(1, "Arriendo local", 300_000.0, Frecuencia.MENSUAL),
                GastoFijo(2, "Internet", 85_000.0, Frecuencia.MENSUAL),
                GastoFijo(3, "Empaques", 40_000.0, Frecuencia.QUINCENAL),
            ),
        ),
    )
}

@Preview(name = "Sin gastos", widthDp = 380, heightDp = 720)
@Composable
private fun GastosFijosVacioPreview() {
    VistaPrevia(GastosFijosUiState(cargando = false))
}

@Composable
private fun VistaPrevia(uiState: GastosFijosUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            GastosFijosScreen(
                uiState = uiState,
                onBack = {},
                onAgregar = {},
                onEditar = {},
                onEliminar = {},
                onNombreChange = {},
                onMontoChange = {},
                onFrecuenciaChange = {},
                onGuardarFormulario = {},
                onCerrarFormulario = {},
            )
        }
    }
}
