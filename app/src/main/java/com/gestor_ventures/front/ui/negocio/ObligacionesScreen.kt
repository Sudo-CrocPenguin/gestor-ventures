package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Obligacion
import com.gestor_ventures.front.components.AccionSheet
import com.gestor_ventures.front.components.GvAccionesSheet
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatLongDate
import java.time.LocalDate
import kotlin.math.absoluteValue

@Composable
fun ObligacionesRoute(viewModel: ObligacionesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObligacionesScreen(
        uiState = uiState,
        onAgregar = viewModel::abrirFormularioNuevo,
        onAbrirAcciones = viewModel::abrirAcciones,
        onAlternarPagada = viewModel::alternarPagadaDeLaElegida,
        onEditar = viewModel::editarLaElegida,
        onEliminar = viewModel::eliminarLaElegida,
        onCerrarAcciones = viewModel::cerrarAcciones,
        onNombreChange = viewModel::onNombreChange,
        onMontoChange = viewModel::onMontoChange,
        onFechaChange = viewModel::onFechaChange,
        onGuardarFormulario = viewModel::guardarFormulario,
        onCerrarFormulario = viewModel::cerrarFormulario,
    )
}

/**
 * HU-07. Los compromisos del negocio.
 *
 * Lo que vence pronto va arriba y aparte: una lista completa no responde "¿qué tengo que pagar
 * ya?", que es la pregunta con la que el emprendedor entra a esta pantalla.
 */
@Composable
fun ObligacionesScreen(
    uiState: ObligacionesUiState,
    onAgregar: () -> Unit,
    onAbrirAcciones: (Obligacion) -> Unit,
    onAlternarPagada: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onCerrarAcciones: () -> Unit,
    onNombreChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
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
            Encabezado(total = uiState.totalPendiente)

            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.obligaciones_vacio))
            }

            if (uiState.proximas.isNotEmpty()) {
                EtiquetaDeSeccion(stringResource(R.string.obligaciones_proximas))
                uiState.proximas.forEach { obligacion ->
                    FilaObligacion(
                        obligacion = obligacion,
                        hoy = uiState.hoy,
                        onClick = { onAbrirAcciones(obligacion) },
                    )
                }
            }

            if (uiState.mostrarTodas) {
                EtiquetaDeSeccion(stringResource(R.string.obligaciones_todas))
                uiState.obligaciones.forEach { obligacion ->
                    FilaObligacion(
                        obligacion = obligacion,
                        hoy = uiState.hoy,
                        onClick = { onAbrirAcciones(obligacion) },
                    )
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.obligaciones_agregar),
                onClick = onAgregar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    uiState.acciones?.let { obligacion ->
        GvAccionesSheet(
            titulo = obligacion.nombre,
            acciones = listOf(
                AccionSheet(
                    texto = stringResource(
                        if (obligacion.pagada) {
                            R.string.obligacion_accion_pendiente
                        } else {
                            R.string.obligacion_accion_pagada
                        },
                    ),
                    iconRes = R.drawable.ic_check,
                    onClick = onAlternarPagada,
                ),
                AccionSheet(
                    texto = stringResource(R.string.obligacion_accion_editar),
                    iconRes = R.drawable.ic_pencil,
                    onClick = onEditar,
                ),
                AccionSheet(
                    texto = stringResource(R.string.obligacion_accion_eliminar),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    onClick = onEliminar,
                ),
            ),
            onCerrar = onCerrarAcciones,
        )
    }

    uiState.formulario?.let { formulario ->
        ObligacionSheet(
            formulario = formulario,
            error = uiState.error,
            onNombreChange = onNombreChange,
            onMontoChange = onMontoChange,
            onFechaChange = onFechaChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )
    }
}

@Composable
private fun Encabezado(total: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.obligaciones_total_pendiente),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(monto = total, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * La fila dice cuánto falta para pagar, no solo la fecha: "vence en 3 días" se entiende de un
 * vistazo y "26 de septiembre" hay que calcularlo.
 */
@Composable
private fun FilaObligacion(
    obligacion: Obligacion,
    hoy: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etiqueta = stringResource(R.string.obligacion_acciones_de, obligacion.nombre)
    val vencida = obligacion.estaVencida(hoy)
    val color = when {
        obligacion.pagada -> MaterialTheme.colorScheme.onSurfaceVariant
        vencida -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    GvCard(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { contentDescription = etiqueta },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (obligacion.pagada) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.obligacion_pagada),
                    tint = GestorVenturesTheme.colors.success,
                    modifier = Modifier.size(17.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = obligacion.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    textDecoration = if (obligacion.pagada) TextDecoration.LineThrough else null,
                )
                Text(
                    text = cuandoVence(obligacion, hoy),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (vencida) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            MoneyText(monto = obligacion.monto, color = color)
        }
    }
}

/** "Pagada · 26 de septiembre", "Vencida hace 3 días", "Vence hoy", "Vence en 5 días". */
@Composable
private fun cuandoVence(obligacion: Obligacion, hoy: LocalDate): String {
    val fecha = formatLongDate(obligacion.fechaVencimiento)
    if (obligacion.pagada) {
        return stringResource(R.string.obligacion_pagada_el, fecha)
    }

    val dias = obligacion.diasParaVencer(hoy)
    return when {
        dias == 0L -> stringResource(R.string.obligacion_vence_hoy)
        dias < 0L -> pluralStringResource(
            R.plurals.obligacion_vencida_hace,
            dias.absoluteValue.toInt(),
            dias.absoluteValue.toInt(),
        )

        else -> pluralStringResource(R.plurals.obligacion_vence_en, dias.toInt(), dias.toInt())
    }
}

@Composable
private fun EtiquetaDeSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 1.1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview(name = "Obligaciones", widthDp = 380, heightDp = 780)
@Preview(
    name = "Obligaciones (oscuro)",
    widthDp = 380,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ObligacionesScreenPreview() {
    val hoy = LocalDate.of(2026, 9, 16)
    val vencida = Obligacion(1, "Cuota del horno", 250_000.0, hoy.minusDays(3), pagada = false)
    val proxima = Obligacion(2, "Préstamo familiar", 100_000.0, hoy.plusDays(4), pagada = false)
    VistaPreviaObligaciones(
        ObligacionesUiState(
            hoy = hoy,
            cargando = false,
            totalPendiente = 350_000.0,
            proximas = listOf(vencida, proxima),
            obligaciones = listOf(
                vencida,
                proxima,
                Obligacion(3, "Seguro del local", 80_000.0, hoy.minusDays(20), pagada = true),
            ),
        ),
    )
}

@Preview(name = "Sin obligaciones", widthDp = 380, heightDp = 780)
@Composable
private fun ObligacionesVacioPreview() {
    VistaPreviaObligaciones(ObligacionesUiState(cargando = false))
}

@Composable
private fun VistaPreviaObligaciones(uiState: ObligacionesUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ObligacionesScreen(
                uiState = uiState,
                onAgregar = {},
                onAbrirAcciones = {},
                onAlternarPagada = {},
                onEditar = {},
                onEliminar = {},
                onCerrarAcciones = {},
                onNombreChange = {},
                onMontoChange = {},
                onFechaChange = {},
                onGuardarFormulario = {},
                onCerrarFormulario = {},
            )
        }
    }
}
