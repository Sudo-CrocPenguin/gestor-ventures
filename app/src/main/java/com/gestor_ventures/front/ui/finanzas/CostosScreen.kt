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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Costo
import com.gestor_ventures.back.model.MargenProducto
import com.gestor_ventures.front.components.AccionSheet
import com.gestor_ventures.front.components.GvAccionesSheet
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatLongDate
import com.gestor_ventures.front.util.formatMesLargo
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun CostosRoute(viewModel: CostosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CostosScreen(
        uiState = uiState,
        onAgregar = viewModel::abrirFormularioNuevo,
        onAbrirAcciones = viewModel::abrirAcciones,
        onEditar = viewModel::editarElCostoElegido,
        onEliminar = viewModel::eliminarElCostoElegido,
        onCerrarAcciones = viewModel::cerrarAcciones,
        onProductoChange = viewModel::onProductoChange,
        onMontoChange = viewModel::onMontoChange,
        onFechaChange = viewModel::onFechaChange,
        onCategoriaChange = viewModel::onCategoriaChange,
        onGuardarFormulario = viewModel::guardarFormulario,
        onCerrarFormulario = viewModel::cerrarFormulario,
    )
}

/**
 * HU-13. Los costos del mes y el margen que dejan los productos.
 *
 * El margen va primero porque es la respuesta que el emprendedor busca; la lista de costos es
 * de dónde sale esa respuesta.
 */
@Composable
fun CostosScreen(
    uiState: CostosUiState,
    onAgregar: () -> Unit,
    onAbrirAcciones: (CostoUi) -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onCerrarAcciones: () -> Unit,
    onProductoChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFechaChange: (LocalDateTime) -> Unit,
    onCategoriaChange: (Long?) -> Unit,
    onGuardarFormulario: () -> Unit,
    onCerrarFormulario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EncabezadoDelMes(mes = uiState.mes, total = uiState.total)

            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.costos_vacio))
            }

            if (uiState.margenes.isNotEmpty()) {
                Margenes(margenes = uiState.margenes)
            }

            if (uiState.costos.isNotEmpty()) {
                EtiquetaSeccion(stringResource(R.string.costos_registrados))
            }

            uiState.costos.forEach { costoUi ->
                FilaCosto(costoUi = costoUi, onClick = { onAbrirAcciones(costoUi) })
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.costos_agregar),
                onClick = onAgregar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    uiState.acciones?.let { costoUi ->
        GvAccionesSheet(
            titulo = costoUi.costo.productoServicio,
            acciones = listOf(
                AccionSheet(
                    texto = stringResource(R.string.costo_accion_editar),
                    iconRes = R.drawable.ic_pencil,
                    onClick = onEditar,
                ),
                AccionSheet(
                    texto = stringResource(R.string.costo_accion_eliminar),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    onClick = onEliminar,
                ),
            ),
            onCerrar = onCerrarAcciones,
        )
    }

    uiState.formulario?.let { formulario ->
        CostoSheet(
            formulario = formulario,
            categorias = uiState.categorias,
            error = uiState.error,
            onProductoChange = onProductoChange,
            onMontoChange = onMontoChange,
            // El calendario solo cambia el día; la hora del registro se conserva.
            onFechaChange = { fecha -> onFechaChange(fecha.atTime(formulario.fecha.toLocalTime())) },
            onCategoriaChange = onCategoriaChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )
    }
}

@Composable
private fun EncabezadoDelMes(mes: YearMonth, total: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
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

/** HU-13. Lo que deja cada producto, con lo que está en rojo primero. */
@Composable
private fun Margenes(margenes: List<MargenProducto>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        EtiquetaSeccion(stringResource(R.string.costos_margen))

        margenes.forEach { margen ->
            FilaMargen(margen)
        }
    }
}

@Composable
private fun FilaMargen(margen: MargenProducto, modifier: Modifier = Modifier) {
    val enRojo = margen.margen < 0
    val color = if (enRojo) {
        MaterialTheme.colorScheme.error
    } else {
        GestorVenturesTheme.colors.success
    }

    GvCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = margen.productoServicio,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.costos_margen_detalle,
                        formatPesos(margen.vendido),
                        formatPesos(margen.costeado),
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(monto = margen.margen, color = color)
                margen.porcentaje?.let { porcentaje ->
                    Text(
                        text = stringResource(R.string.costos_margen_porcentaje, porcentaje.roundToInt()),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = color,
                    )
                }
            }
        }
    }
}

/** La fila muestra el costo; lo que se puede hacer con él sale al tocarla. */
@Composable
private fun FilaCosto(
    costoUi: CostoUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etiqueta = stringResource(R.string.costo_acciones_de, costoUi.costo.productoServicio)

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
                    text = costoUi.costo.productoServicio,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = detalleDelCosto(costoUi),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(monto = costoUi.costo.monto)
        }
    }
}

@Composable
private fun detalleDelCosto(costoUi: CostoUi): String {
    val fecha = formatLongDate(costoUi.costo.fecha.toLocalDate())
    val categoria = costoUi.categoria ?: stringResource(R.string.costo_sin_categoria)
    return "$fecha · $categoria"
}

@Composable
private fun EtiquetaSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 1.1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview(name = "Costos", widthDp = 380, heightDp = 820)
@Preview(
    name = "Costos (oscuro)",
    widthDp = 380,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CostosScreenPreview() {
    val mes = YearMonth.of(2026, 9)
    VistaPreviaCostos(
        CostosUiState(
            mes = mes,
            cargando = false,
            total = 38_000.0,
            margenes = listOf(
                MargenProducto("Galletas", vendido = 5_000.0, costeado = 20_000.0),
                MargenProducto("Torta de chocolate", vendido = 45_000.0, costeado = 18_000.0),
            ),
            costos = listOf(
                CostoUi(
                    Costo(1, "Torta de chocolate", 18_000.0, mes.atDay(10).atTime(9, 0), 1),
                    "Insumos",
                ),
                CostoUi(Costo(2, "Galletas", 20_000.0, mes.atDay(8).atTime(15, 0)), null),
            ),
        ),
    )
}

@Preview(name = "Sin costos", widthDp = 380, heightDp = 820)
@Composable
private fun CostosVacioPreview() {
    VistaPreviaCostos(CostosUiState(mes = YearMonth.of(2026, 9), cargando = false))
}

@Composable
private fun VistaPreviaCostos(uiState: CostosUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CostosScreen(
                uiState = uiState,
                onAgregar = {},
                onAbrirAcciones = {},
                onEditar = {},
                onEliminar = {},
                onCerrarAcciones = {},
                onProductoChange = {},
                onMontoChange = {},
                onFechaChange = {},
                onCategoriaChange = {},
                onGuardarFormulario = {},
                onCerrarFormulario = {},
            )
        }
    }
}
