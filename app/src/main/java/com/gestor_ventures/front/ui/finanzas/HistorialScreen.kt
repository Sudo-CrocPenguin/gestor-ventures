package com.gestor_ventures.front.ui.finanzas

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.FiltroMovimientos
import com.gestor_ventures.back.model.Gasto
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Movimiento
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.model.Venta
import com.gestor_ventures.front.model.MetodoPagoUi as MetodoPagoUiModel
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.model.aUi
import com.gestor_ventures.front.components.AccionSheet
import com.gestor_ventures.front.components.GvAccionesSheet
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvSegmentedToggle
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatDiaCorto
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun HistorialRoute(onBack: () -> Unit, viewModel: HistorialViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistorialScreen(
        uiState = uiState,
        onBack = onBack,
        onPeriodoChange = viewModel::onPeriodoChange,
        onFiltroChange = viewModel::onFiltroChange,
        onAbrirAcciones = viewModel::abrirAcciones,
        onCerrarAcciones = viewModel::cerrarAcciones,
        onPedirEliminar = viewModel::pedirConfirmacionDeEliminar,
        onCancelarEliminar = viewModel::cancelarEliminar,
        onConfirmarEliminar = viewModel::confirmarEliminar,
        onEditar = viewModel::editarElMovimientoElegido,
        onMontoChange = viewModel::onMontoChange,
        onTituloChange = viewModel::onTituloChange,
        onFechaChange = viewModel::onFechaChange,
        onCategoriaChange = viewModel::onCategoriaChange,
        onTipoRegistroChange = viewModel::onTipoRegistroChange,
        onMetodoPagoChange = viewModel::onMetodoPagoChange,
        onNotaChange = viewModel::onNotaChange,
        onGuardarFormulario = viewModel::guardarFormulario,
        onCerrarFormulario = viewModel::cerrarFormulario,
    )
}

/**
 * HU-17. Todo lo que movió plata en un periodo, junto y en orden.
 *
 * Es la pantalla a la que se entra a arreglar algo: un monto mal escrito, una venta que se
 * registró dos veces. Por eso cada fila se puede tocar y por eso borrar pide confirmación.
 */
@Composable
fun HistorialScreen(
    uiState: HistorialUiState,
    onBack: () -> Unit,
    onPeriodoChange: (PeriodoPredefinido) -> Unit,
    onFiltroChange: (FiltroMovimientos) -> Unit,
    onAbrirAcciones: (Movimiento) -> Unit,
    onCerrarAcciones: () -> Unit,
    onPedirEliminar: () -> Unit,
    onCancelarEliminar: () -> Unit,
    onConfirmarEliminar: () -> Unit,
    onEditar: () -> Unit,
    onMontoChange: (String) -> Unit,
    onTituloChange: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    onCategoriaChange: (Long?) -> Unit,
    onTipoRegistroChange: (TipoRegistroVentaUi) -> Unit,
    onMetodoPagoChange: (MetodoPagoUi) -> Unit,
    onNotaChange: (String) -> Unit,
    onGuardarFormulario: () -> Unit,
    onCerrarFormulario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.historial_titulo), onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Periodos(seleccionado = uiState.periodo, onSeleccionar = onPeriodoChange)

            GvSegmentedToggle(
                opciones = FiltroMovimientos.entries,
                seleccionada = uiState.filtro,
                etiqueta = { stringResource(it.labelRes()) },
                onSeleccionar = onFiltroChange,
            )

            TotalesDelPeriodo(ingresos = uiState.ingresos, salidas = uiState.salidas)

            when {
                uiState.periodoVacio -> GvInfoNote(stringResource(R.string.historial_vacio))
                uiState.vacio -> GvInfoNote(stringResource(R.string.historial_sin_filtro))
                else -> uiState.movimientos.forEach { movimiento ->
                    FilaMovimiento(
                        movimiento = movimiento,
                        categoria = uiState.nombreDeCategoria(movimiento.categoriaId()),
                        conFecha = uiState.variosDias,
                        onClick = { onAbrirAcciones(movimiento) },
                    )
                }
            }
        }
    }

    uiState.acciones?.let { movimiento ->
        GvAccionesSheet(
            titulo = tituloDe(movimiento),
            acciones = listOf(
                AccionSheet(
                    texto = stringResource(R.string.historial_accion_editar),
                    iconRes = R.drawable.ic_pencil,
                    onClick = onEditar,
                ),
                AccionSheet(
                    texto = stringResource(R.string.historial_accion_eliminar),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    onClick = onPedirEliminar,
                ),
            ),
            onCerrar = onCerrarAcciones,
        )
    }

    uiState.porEliminar?.let { movimiento ->
        ConfirmarEliminarMovimiento(
            movimiento = movimiento,
            onConfirmar = onConfirmarEliminar,
            onCancelar = onCancelarEliminar,
        )
    }

    // Cada tipo se corrige con la hoja de su seccion: el historial no inventa formularios.
    when (val formulario = uiState.formulario) {
        is FormularioMovimiento.DeVenta -> VentaSheet(
            formulario = formulario.campos,
            onTipoRegistroChange = onTipoRegistroChange,
            onMontoChange = onMontoChange,
            onProductoServicioChange = onTituloChange,
            onMetodoPagoChange = onMetodoPagoChange,
            onNotaChange = onNotaChange,
            onFechaChange = onFechaChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )

        is FormularioMovimiento.DeGasto -> GastoSheet(
            formulario = formulario.campos,
            categorias = uiState.categorias.filter { it.tipo == TipoCategoria.GASTO },
            error = formulario.error,
            onDescripcionChange = onTituloChange,
            onMontoChange = onMontoChange,
            onFechaChange = onFechaChange,
            onCategoriaChange = onCategoriaChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )

        is FormularioMovimiento.DeCosto -> CostoSheet(
            formulario = formulario.campos,
            categorias = uiState.categorias.filter { it.tipo == TipoCategoria.COSTO },
            error = formulario.error,
            onProductoChange = onTituloChange,
            onMontoChange = onMontoChange,
            onFechaChange = onFechaChange,
            onCategoriaChange = onCategoriaChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )

        null -> Unit
    }
}

/** Los periodos de un toque. Se desplazan porque en una pantalla angosta no caben los cuatro. */
@Composable
private fun Periodos(
    seleccionado: PeriodoPredefinido,
    onSeleccionar: (PeriodoPredefinido) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeriodoPredefinido.entries.forEach { periodo ->
            GvChip(
                text = stringResource(periodo.labelRes()),
                selected = periodo == seleccionado,
                onClick = { onSeleccionar(periodo) },
            )
        }
    }
}

/**
 * Lo que entró y lo que salió en el periodo. No cambian con el filtro a propósito: son la
 * respuesta a "¿cómo me fue?", y esconder la mitad al mirar solo las ventas sería mentir.
 */
@Composable
private fun TotalesDelPeriodo(ingresos: Double, salidas: Double, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Total(
                label = stringResource(R.string.historial_ingresos),
                monto = ingresos,
                color = GestorVenturesTheme.colors.success,
                modifier = Modifier.weight(1f),
            )
            Total(
                label = stringResource(R.string.historial_salidas),
                monto = salidas,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outline,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.historial_diferencia),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(
                monto = ingresos - salidas,
                color = if (ingresos - salidas < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    GestorVenturesTheme.colors.success
                },
            )
        }
    }
}

@Composable
private fun Total(
    label: String,
    monto: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            monto = monto,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun FilaMovimiento(
    movimiento: Movimiento,
    categoria: String?,
    conFecha: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titulo = tituloDe(movimiento)
    val etiqueta = stringResource(R.string.historial_acciones_de, titulo)
    val entra = movimiento.entra

    GvCard(modifier) {
        ListRow(
            title = titulo,
            subtitle = subtituloDe(movimiento, categoria, conFecha),
            iconRes = if (entra) R.drawable.ic_plus else R.drawable.ic_minus,
            iconContainerColor = if (entra) {
                GestorVenturesTheme.colors.successContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            iconColor = if (entra) {
                GestorVenturesTheme.colors.success
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier
                .clickable(onClick = onClick)
                .semantics { contentDescription = etiqueta },
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    MoneyText(
                        monto = movimiento.monto,
                        color = if (entra) {
                            GestorVenturesTheme.colors.success
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        text = formatHour(movimiento.fechaHora.toLocalTime()),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

/** Borrar un movimiento no se deshace, y el total del periodo se mueve con él. */
@Composable
private fun ConfirmarEliminarMovimiento(
    movimiento: Movimiento,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.historial_eliminar_titulo, tituloDe(movimiento))) },
        text = { Text(stringResource(R.string.historial_eliminar_ayuda)) },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.historial_eliminar_confirmar))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelar,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.cancelar))
            }
        },
    )
}

// ---------- Cómo se lee cada movimiento ----------

/** Lo que el usuario escribió. Una venta rápida no tiene producto, así que se nombra por lo que es. */
@Composable
private fun tituloDe(movimiento: Movimiento): String = when (movimiento) {
    is Movimiento.DeVenta -> movimiento.venta.productoServicio
        ?: stringResource(R.string.historial_venta_rapida)

    is Movimiento.DeGasto -> movimiento.gasto.descripcion
    is Movimiento.DeCosto -> movimiento.costo.productoServicio
}

/**
 * La segunda línea dice qué tipo de movimiento es y el dato que lo distingue, con la fecha
 * delante cuando el periodo abarca varios días.
 */
@Composable
private fun subtituloDe(
    movimiento: Movimiento,
    categoria: String?,
    conFecha: Boolean,
): String {
    val detalle = detalleDe(movimiento, categoria)
    if (!conFecha) return detalle
    return stringResource(
        R.string.historial_con_fecha,
        formatDiaCorto(movimiento.fechaHora.toLocalDate()),
        detalle,
    )
}

@Composable
private fun detalleDe(movimiento: Movimiento, categoria: String?): String = when (movimiento) {
    is Movimiento.DeVenta -> movimiento.venta.metodoPago
        ?.let { stringResource(R.string.historial_venta_con_pago, stringResource(it.aUi().labelRes)) }
        ?: stringResource(R.string.historial_venta_sin_desglose)

    is Movimiento.DeGasto -> conCategoria(R.string.historial_gasto, categoria)
    is Movimiento.DeCosto -> conCategoria(R.string.historial_costo, categoria)
}

@Composable
private fun conCategoria(tipoRes: Int, categoria: String?): String {
    val tipo = stringResource(tipoRes)
    return if (categoria == null) {
        stringResource(R.string.historial_sin_categoria, tipo)
    } else {
        stringResource(R.string.historial_con_categoria, tipo, categoria)
    }
}

private fun Movimiento.categoriaId(): Long? = when (this) {
    is Movimiento.DeVenta -> null
    is Movimiento.DeGasto -> gasto.categoriaId
    is Movimiento.DeCosto -> costo.categoriaId
}

private fun PeriodoPredefinido.labelRes(): Int = when (this) {
    PeriodoPredefinido.Hoy -> R.string.periodo_hoy
    PeriodoPredefinido.Ayer -> R.string.periodo_ayer
    PeriodoPredefinido.Semana -> R.string.periodo_semana
    PeriodoPredefinido.Mes -> R.string.periodo_mes
}

private fun FiltroMovimientos.labelRes(): Int = when (this) {
    FiltroMovimientos.Todos -> R.string.historial_filtro_todos
    FiltroMovimientos.Ventas -> R.string.historial_filtro_ventas
    FiltroMovimientos.Salidas -> R.string.historial_filtro_salidas
}

@Preview(name = "Historial", widthDp = 380, heightDp = 860)
@Preview(
    name = "Historial (oscuro)",
    widthDp = 380,
    heightDp = 860,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HistorialPreview() {
    val hoy = LocalDate.of(2026, 9, 16)
    val ahora = LocalDateTime.of(2026, 9, 16, 10, 24)

    VistaPreviaHistorial(
        HistorialUiState(
            cargando = false,
            categorias = listOf(Categoria(1, "Insumos", TipoCategoria.COSTO)),
            todos = listOf(
                Movimiento.DeVenta(
                    Venta(
                        id = 1,
                        negocioId = 1,
                        tipoRegistro = TipoRegistroVenta.DETALLADO,
                        monto = 85_000.0,
                        fechaHora = ahora,
                        productoServicio = "Torta personalizada",
                        metodoPago = MetodoPago.TRANSFERENCIA,
                    ),
                ),
                Movimiento.DeVenta(
                    Venta(
                        id = 2,
                        negocioId = 1,
                        tipoRegistro = TipoRegistroVenta.RAPIDO,
                        monto = 35_000.0,
                        fechaHora = ahora.withHour(9),
                    ),
                ),
                Movimiento.DeGasto(
                    Gasto(id = 3, descripcion = "Domicilio pedido", monto = 12_000.0, fecha = hoy),
                ),
            ),
        ),
    )
}

@Preview(name = "Historial vacío", widthDp = 380, heightDp = 860)
@Composable
private fun HistorialVacioPreview() {
    VistaPreviaHistorial(HistorialUiState(cargando = false))
}

@Composable
private fun VistaPreviaHistorial(uiState: HistorialUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistorialScreen(
                uiState = uiState,
                onBack = {},
                onPeriodoChange = {},
                onFiltroChange = {},
                onAbrirAcciones = {},
                onCerrarAcciones = {},
                onPedirEliminar = {},
                onCancelarEliminar = {},
                onConfirmarEliminar = {},
                onEditar = {},
                onMontoChange = {},
                onTituloChange = {},
                onFechaChange = {},
                onCategoriaChange = {},
                onTipoRegistroChange = {},
                onMetodoPagoChange = {},
                onNotaChange = {},
                onGuardarFormulario = {},
                onCerrarFormulario = {},
            )
        }
    }
}
