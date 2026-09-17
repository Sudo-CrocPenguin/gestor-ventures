package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.gestor_ventures.front.components.AccionSheet
import com.gestor_ventures.front.components.GvAccionesSheet
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.theme.GestorVenturesTheme

@Composable
fun GastosFijosRoute(viewModel: GastosFijosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GastosFijosScreen(
        uiState = uiState,
        onAgregar = viewModel::abrirFormularioNuevo,
        onAbrirAcciones = viewModel::abrirAcciones,
        onEditar = viewModel::editarElGastoElegido,
        onEliminar = viewModel::eliminarElGastoElegido,
        onCerrarAcciones = viewModel::cerrarAcciones,
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
    onAgregar: () -> Unit,
    onAbrirAcciones: (GastoFijo) -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onCerrarAcciones: () -> Unit,
    onNombreChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFrecuenciaChange: (Frecuencia) -> Unit,
    onGuardarFormulario: () -> Unit,
    onCerrarFormulario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // La lista se desplaza; el botón no. Agregar es la acción principal de la pantalla.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.gastos_fijos_explicacion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.gastos_fijos_vacio))
            }

            ListaGastosFijos(
                gastos = uiState.gastosFijos,
                total = uiState.total,
                onAcciones = onAbrirAcciones,
            )
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.base_agregar_gasto),
                onClick = onAgregar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    uiState.acciones?.let { gasto ->
        GvAccionesSheet(
            titulo = gasto.nombre,
            acciones = listOf(
                AccionSheet(
                    texto = stringResource(R.string.base_accion_editar_gasto),
                    iconRes = R.drawable.ic_pencil,
                    onClick = onEditar,
                ),
                AccionSheet(
                    texto = stringResource(R.string.base_accion_eliminar_gasto),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    onClick = onEliminar,
                ),
            ),
            onCerrar = onCerrarAcciones,
        )
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
                onAgregar = {},
                onAbrirAcciones = {},
                onEditar = {},
                onEliminar = {},
                onCerrarAcciones = {},
                onNombreChange = {},
                onMontoChange = {},
                onFrecuenciaChange = {},
                onGuardarFormulario = {},
                onCerrarFormulario = {},
            )
        }
    }
}
