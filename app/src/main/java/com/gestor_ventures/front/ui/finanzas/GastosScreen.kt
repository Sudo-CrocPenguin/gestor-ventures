package com.gestor_ventures.front.ui.finanzas

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Gasto
import com.gestor_ventures.front.components.AccionSheet
import com.gestor_ventures.front.components.GvAccionesSheet
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatLongDate
import com.gestor_ventures.front.util.formatMesLargo
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun GastosRoute(viewModel: GastosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GastosScreen(
        uiState = uiState,
        onAgregar = viewModel::abrirFormularioNuevo,
        onAbrirAcciones = viewModel::abrirAcciones,
        onEditar = viewModel::editarElGastoElegido,
        onEliminar = viewModel::eliminarElGastoElegido,
        onCerrarAcciones = viewModel::cerrarAcciones,
        onDescripcionChange = viewModel::onDescripcionChange,
        onMontoChange = viewModel::onMontoChange,
        onFechaChange = viewModel::onFechaChange,
        onCategoriaChange = viewModel::onCategoriaChange,
        onGuardarFormulario = viewModel::guardarFormulario,
        onCerrarFormulario = viewModel::cerrarFormulario,
    )
}

/**
 * HU-14. Los gastos generales del mes, con su total.
 *
 * Es la primera sección de la pestaña de Finanzas; HU-16 le pondrá el resumen encima.
 */
@Composable
fun GastosScreen(
    uiState: GastosUiState,
    onAgregar: () -> Unit,
    onAbrirAcciones: (GastoUi) -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onCerrarAcciones: () -> Unit,
    onDescripcionChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    onCategoriaChange: (Long?) -> Unit,
    onGuardarFormulario: () -> Unit,
    onCerrarFormulario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // La lista se desplaza; el botón no. Registrar es la acción principal de la pantalla y
        // tiene que estar siempre a la mano, aunque el mes lleve treinta gastos.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Encabezado(mes = uiState.mes, total = uiState.total)

            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.gastos_vacio))
            }

            uiState.gastos.forEach { gastoUi ->
                FilaGasto(gastoUi = gastoUi, onClick = { onAbrirAcciones(gastoUi) })
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.gastos_agregar),
                onClick = onAgregar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    uiState.acciones?.let { gastoUi ->
        GvAccionesSheet(
            titulo = gastoUi.gasto.descripcion,
            acciones = listOf(
                AccionSheet(
                    texto = stringResource(R.string.gasto_accion_editar),
                    iconRes = R.drawable.ic_pencil,
                    onClick = onEditar,
                ),
                AccionSheet(
                    texto = stringResource(R.string.gasto_accion_eliminar),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    onClick = onEliminar,
                ),
            ),
            onCerrar = onCerrarAcciones,
        )
    }

    uiState.formulario?.let { formulario ->
        GastoSheet(
            formulario = formulario,
            categorias = uiState.categorias,
            error = uiState.error,
            onDescripcionChange = onDescripcionChange,
            onMontoChange = onMontoChange,
            onFechaChange = onFechaChange,
            onCategoriaChange = onCategoriaChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )
    }
}

@Composable
private fun Encabezado(mes: YearMonth, total: Double, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.gastos_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatMesLargo(mes.atDay(1)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(monto = total, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** La fila muestra el gasto; lo que se puede hacer con él sale al tocarla. */
@Composable
private fun FilaGasto(
    gastoUi: GastoUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etiqueta = stringResource(R.string.gasto_acciones_de, gastoUi.gasto.descripcion)

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
                    text = gastoUi.gasto.descripcion,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = detalle(gastoUi),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(monto = gastoUi.gasto.monto)
        }
    }
}

/** "Jueves 10 de septiembre · Transporte", o solo la fecha si no tiene categoría. */
@Composable
private fun detalle(gastoUi: GastoUi): String {
    val fecha = formatLongDate(gastoUi.gasto.fecha)
    val categoria = gastoUi.categoria ?: stringResource(R.string.gasto_sin_categoria)
    return "$fecha · $categoria"
}

@Preview(name = "Gastos", widthDp = 380, heightDp = 760)
@Preview(
    name = "Gastos (oscuro)",
    widthDp = 380,
    heightDp = 760,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GastosScreenPreview() {
    val mes = YearMonth.of(2026, 9)
    VistaPrevia(
        GastosUiState(
            mes = mes,
            cargando = false,
            total = 47_000.0,
            gastos = listOf(
                GastoUi(Gasto(1, "Domicilio de insumos", 12_000.0, mes.atDay(10), 1), "Transporte"),
                GastoUi(Gasto(2, "Facturas y stickers", 30_000.0, mes.atDay(7), 2), "Papelería"),
                GastoUi(Gasto(3, "Varios", 5_000.0, mes.atDay(3)), null),
            ),
        ),
    )
}

@Preview(name = "Sin gastos", widthDp = 380, heightDp = 760)
@Composable
private fun GastosVacioPreview() {
    VistaPrevia(GastosUiState(mes = YearMonth.of(2026, 9), cargando = false))
}

@Composable
private fun VistaPrevia(uiState: GastosUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            GastosScreen(
                uiState = uiState,
                onAgregar = {},
                onAbrirAcciones = {},
                onEditar = {},
                onEliminar = {},
                onCerrarAcciones = {},
                onDescripcionChange = {},
                onMontoChange = {},
                onFechaChange = {},
                onCategoriaChange = {},
                onGuardarFormulario = {},
                onCerrarFormulario = {},
            )
        }
    }
}
