package com.gestor_ventures.front.ui.agenda

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cita
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.EstadoCita
import com.gestor_ventures.back.model.EstadoPedido
import com.gestor_ventures.back.model.Pedido
import com.gestor_ventures.back.model.ProductoPedido
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.front.components.AccionSheet
import com.gestor_ventures.front.components.GvAccionesSheet
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvSegmentedToggle
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatLongDate
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun AgendaRoute(viewModel: AgendaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AgendaScreen(
        uiState = uiState,
        onSeccionChange = viewModel::onSeccionChange,
        onAgendarCita = viewModel::abrirFormularioCita,
        onRegistrarPedido = viewModel::abrirFormularioPedido,
        onAbrirPedido = { viewModel.abrirPedido(it.id) },
        onCerrarPedido = viewModel::cerrarPedido,
        onCambiarEstadoPedido = viewModel::cambiarEstadoPedido,
        onClienteChange = viewModel::onClienteChange,
        onFechaCitaChange = viewModel::onFechaCitaChange,
        onHoraCitaChange = viewModel::onHoraCitaChange,
        onDescripcionCitaChange = viewModel::onDescripcionCitaChange,
        onGuardarCita = viewModel::guardarCita,
        onFechaEntregaChange = viewModel::onFechaEntregaChange,
        onProductoNombreChange = viewModel::onProductoNombreChange,
        onProductoCantidadChange = viewModel::onProductoCantidadChange,
        onAgregarProducto = viewModel::agregarProducto,
        onQuitarProducto = viewModel::quitarProducto,
        onGuardarPedido = viewModel::guardarPedido,
        onCerrarFormulario = viewModel::cerrarFormulario,
    )
}

/**
 * HU-24 y HU-25. La pestaña Agenda: lo que viene por delante y el botón para agregar más.
 *
 * El botón de abajo cambia con la sección: "Agendar cita" o "Registrar pedido".
 */
@Composable
fun AgendaScreen(
    uiState: AgendaUiState,
    onSeccionChange: (SeccionAgenda) -> Unit,
    onAgendarCita: () -> Unit,
    onRegistrarPedido: () -> Unit,
    onAbrirPedido: (Pedido) -> Unit,
    onCerrarPedido: () -> Unit,
    onCambiarEstadoPedido: (EstadoPedido) -> Unit,
    onClienteChange: (Long) -> Unit,
    onFechaCitaChange: (LocalDate) -> Unit,
    onHoraCitaChange: (LocalTime) -> Unit,
    onDescripcionCitaChange: (String) -> Unit,
    onGuardarCita: () -> Unit,
    onFechaEntregaChange: (LocalDate) -> Unit,
    onProductoNombreChange: (Int, String) -> Unit,
    onProductoCantidadChange: (Int, String) -> Unit,
    onAgregarProducto: () -> Unit,
    onQuitarProducto: (Int) -> Unit,
    onGuardarPedido: () -> Unit,
    onCerrarFormulario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seccion = uiState.seccionVisible

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.secciones.size > 1 && seccion != null) {
                GvSegmentedToggle(
                    opciones = uiState.secciones,
                    seleccionada = seccion,
                    etiqueta = { stringResource(it.labelRes()) },
                    onSeleccionar = onSeccionChange,
                )
            }

            AnimatedVisibility(visible = uiState.sinClientes) {
                GvInfoNote(stringResource(R.string.agenda_sin_clientes))
            }

            AnimatedVisibility(visible = uiState.vacio && !uiState.sinClientes) {
                GvInfoNote(
                    stringResource(
                        if (seccion == SeccionAgenda.Pedidos) R.string.agenda_pedidos_vacio
                        else R.string.agenda_citas_vacio,
                    ),
                )
            }

            when (seccion) {
                SeccionAgenda.Citas -> uiState.citas.forEach { FilaCita(it) }
                SeccionAgenda.Pedidos -> uiState.pedidos.forEach { pedido ->
                    FilaPedido(pedido, onClick = { onAbrirPedido(pedido) })
                }
                null -> Unit
            }
        }

        if (seccion != null) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                GvPrimaryButton(
                    text = stringResource(
                        if (seccion == SeccionAgenda.Pedidos) R.string.agenda_registrar_pedido
                        else R.string.agenda_agendar_cita,
                    ),
                    onClick = if (seccion == SeccionAgenda.Pedidos) onRegistrarPedido else onAgendarCita,
                    enabled = !uiState.sinClientes,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    uiState.formularioCita?.let { formulario ->
        CitaSheet(
            formulario = formulario,
            clientes = uiState.clientes,
            error = uiState.error,
            onClienteChange = onClienteChange,
            onFechaChange = onFechaCitaChange,
            onHoraChange = onHoraCitaChange,
            onDescripcionChange = onDescripcionCitaChange,
            onGuardar = onGuardarCita,
            onCerrar = onCerrarFormulario,
        )
    }

    uiState.formularioPedido?.let { formulario ->
        PedidoSheet(
            formulario = formulario,
            clientes = uiState.clientes,
            error = uiState.error,
            onClienteChange = onClienteChange,
            onFechaEntregaChange = onFechaEntregaChange,
            onProductoNombreChange = onProductoNombreChange,
            onProductoCantidadChange = onProductoCantidadChange,
            onAgregarProducto = onAgregarProducto,
            onQuitarProducto = onQuitarProducto,
            onGuardar = onGuardarPedido,
            onCerrar = onCerrarFormulario,
        )
    }

    uiState.pedidoElegido?.let { pedido ->
        GvAccionesSheet(
            titulo = pedido.nombreCliente,
            acciones = EstadosQueSeEligen.map { estado ->
                AccionSheet(
                    texto = stringResource(estado.labelRes()),
                    iconRes = R.drawable.ic_check,
                    habilitada = pedido.estado != estado && pedido.estado != EstadoPedido.CANCELADO,
                    onClick = { onCambiarEstadoPedido(estado) },
                )
            },
            onCerrar = onCerrarPedido,
        )
    }
}

/** Los estados a los que se pasa un pedido a mano. Cancelar es otra acción (HU-28). */
private val EstadosQueSeEligen = listOf(
    EstadoPedido.PENDIENTE,
    EstadoPedido.EN_PROCESO,
    EstadoPedido.ENTREGADO,
)

private fun SeccionAgenda.labelRes(): Int = when (this) {
    SeccionAgenda.Citas -> R.string.agenda_citas
    SeccionAgenda.Pedidos -> R.string.agenda_pedidos
}

/** Una cita: a quién, cuándo, para qué y en qué estado está. */
@Composable
private fun FilaCita(cita: Cita, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        ListRow(
            title = cita.nombreCliente,
            subtitle = listOfNotNull(
                "${formatLongDate(cita.fecha)} · ${formatHour(cita.hora)}",
                cita.descripcion,
            ).joinToString("\n"),
            subtitleMaxLines = 2,
            iconRes = R.drawable.ic_calendar,
            trailing = { StatusPill(stringResource(cita.estado.labelRes())) },
        )
    }
}

/** Un pedido: de quién, cuándo se entrega, qué lleva y en qué va. */
@Composable
private fun FilaPedido(pedido: Pedido, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GvCard(modifier.clickable(onClick = onClick)) {
        ListRow(
            title = pedido.nombreCliente,
            subtitle = listOf(
                stringResource(R.string.agenda_entrega_el, formatLongDate(pedido.fechaEntrega)),
                pedido.productos.joinToString(", ") { "${it.cantidad} × ${it.nombre}" },
            ).joinToString("\n"),
            subtitleMaxLines = 2,
            iconRes = R.drawable.ic_box,
            trailing = { StatusPill(stringResource(pedido.estado.labelRes())) },
        )
    }
}

@Preview(name = "Citas", widthDp = 380, heightDp = 760)
@Preview(name = "Citas (oscuro)", widthDp = 380, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AgendaCitasPreview() {
    VistaPrevia(
        AgendaUiState(
            tipoActividad = TipoActividad.MIXTO,
            cargando = false,
            clientes = listOf(Cliente(id = 1, nombre = "Laura Gómez")),
            citas = listOf(
                Cita(1, 1, "Laura Gómez", LocalDate.of(2026, 10, 24), LocalTime.of(10, 0), "Corte y cepillado", EstadoCita.PROGRAMADA),
                Cita(2, 1, "Andrés Ruiz", LocalDate.of(2026, 10, 25), LocalTime.of(15, 30), null, EstadoCita.PROGRAMADA),
            ),
        ),
    )
}

@Preview(name = "Pedidos", widthDp = 380, heightDp = 760)
@Composable
private fun AgendaPedidosPreview() {
    VistaPrevia(
        AgendaUiState(
            tipoActividad = TipoActividad.PRODUCTOS,
            seccion = SeccionAgenda.Pedidos,
            cargando = false,
            clientes = listOf(Cliente(id = 1, nombre = "Laura Gómez")),
            pedidos = listOf(
                Pedido(
                    1, 1, "Laura Gómez", LocalDate.of(2026, 10, 26),
                    listOf(ProductoPedido("Torta de chocolate", 1), ProductoPedido("Galletas", 12)),
                    EstadoPedido.EN_PROCESO,
                ),
            ),
        ),
    )
}

@Composable
private fun VistaPrevia(uiState: AgendaUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AgendaScreen(
                uiState = uiState,
                onSeccionChange = {},
                onAgendarCita = {},
                onRegistrarPedido = {},
                onAbrirPedido = {},
                onCerrarPedido = {},
                onCambiarEstadoPedido = {},
                onClienteChange = {},
                onFechaCitaChange = {},
                onHoraCitaChange = {},
                onDescripcionCitaChange = {},
                onGuardarCita = {},
                onFechaEntregaChange = {},
                onProductoNombreChange = { _, _ -> },
                onProductoCantidadChange = { _, _ -> },
                onAgregarProducto = {},
                onQuitarProducto = {},
                onGuardarPedido = {},
                onCerrarFormulario = {},
            )
        }
    }
}
