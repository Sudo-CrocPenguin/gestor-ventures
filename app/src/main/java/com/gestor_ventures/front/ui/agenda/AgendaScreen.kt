package com.gestor_ventures.front.ui.agenda

import android.content.Intent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import com.gestor_ventures.back.model.enlaceDeWhatsApp
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
    val contexto = LocalContext.current

    AgendaScreen(
        uiState = uiState,
        onVistaChange = viewModel::onVistaChange,
        onAnterior = viewModel::anterior,
        onSiguiente = viewModel::siguiente,
        onHoy = viewModel::irAHoy,
        onAbrirDia = viewModel::abrirDia,
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
        onAbrirCita = { viewModel.abrirCita(it.id) },
        onCerrarCita = viewModel::cerrarCita,
        onEditarCita = viewModel::editarCitaElegida,
        onEditarPedido = viewModel::editarPedidoElegido,
        onPedirCancelar = viewModel::pedirConfirmacionDeCancelar,
        onConfirmarCancelar = viewModel::confirmarCancelacion,
        onDescartarCancelar = viewModel::descartarCancelacion,
        onAbrirWhatsApp = { enlace ->
            viewModel.cerrarAviso()
            contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace.toUri()))
        },
        onCerrarAviso = viewModel::cerrarAviso,
    )
}

/**
 * HU-24, HU-25 y HU-27. La pestaña Agenda, como calendario de día, semana o mes.
 *
 * Citas y pedidos se ven juntos, porque la pregunta es "¿cuánto trabajo tengo?" y no "¿cuántas
 * citas?". Abajo hay un botón por cada tipo de agenda que tenga el negocio.
 */
@Composable
fun AgendaScreen(
    uiState: AgendaUiState,
    onVistaChange: (VistaAgenda) -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onHoy: () -> Unit,
    onAbrirDia: (LocalDate) -> Unit,
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
    onAbrirCita: (Cita) -> Unit,
    onCerrarCita: () -> Unit,
    onEditarCita: () -> Unit,
    onEditarPedido: () -> Unit,
    onPedirCancelar: () -> Unit,
    onConfirmarCancelar: () -> Unit,
    onDescartarCancelar: () -> Unit,
    onAbrirWhatsApp: (String) -> Unit,
    onCerrarAviso: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GvSegmentedToggle(
                opciones = VistaAgenda.entries,
                seleccionada = uiState.vista,
                etiqueta = { stringResource(it.labelRes()) },
                onSeleccionar = onVistaChange,
            )

            EncabezadoDePeriodo(
                vista = uiState.vista,
                rango = uiState.rango,
                incluyeHoy = uiState.incluyeHoy,
                onAnterior = onAnterior,
                onSiguiente = onSiguiente,
                onHoy = onHoy,
            )

            AnimatedVisibility(visible = uiState.sinClientes) {
                GvInfoNote(stringResource(R.string.agenda_sin_clientes))
            }

            AnimatedVisibility(visible = uiState.vacio && !uiState.sinClientes) {
                GvInfoNote(stringResource(R.string.agenda_periodo_vacio))
            }

            when (uiState.vista) {
                VistaAgenda.Mes -> CuadriculaDelMes(
                    dias = uiState.dias,
                    hoy = uiState.hoy,
                    cuantosEn = uiState::cuantosEn,
                    onDiaClick = onAbrirDia,
                )
                VistaAgenda.Dia, VistaAgenda.Semana -> if (!uiState.vacio) {
                    uiState.dias.forEach { dia ->
                        Dia(
                            dia = dia,
                            citas = uiState.citasDe(dia),
                            pedidos = uiState.pedidosDe(dia),
                            mostrarTitulo = uiState.vista == VistaAgenda.Semana,
                            onAbrirCita = onAbrirCita,
                            onAbrirPedido = onAbrirPedido,
                        )
                    }
                }
            }
        }

        if (uiState.ofreceCitas || uiState.ofrecePedidos) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (uiState.ofreceCitas) {
                        GvPrimaryButton(
                            text = stringResource(R.string.agenda_agendar_cita),
                            onClick = onAgendarCita,
                            enabled = !uiState.sinClientes,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (uiState.ofrecePedidos) {
                        GvPrimaryButton(
                            text = stringResource(R.string.agenda_registrar_pedido),
                            onClick = onRegistrarPedido,
                            enabled = !uiState.sinClientes,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
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
        val abierto = pedido.estado != EstadoPedido.CANCELADO
        GvAccionesSheet(
            titulo = pedido.nombreCliente,
            acciones = EstadosQueSeEligen.map { estado ->
                AccionSheet(
                    texto = stringResource(estado.labelRes()),
                    iconRes = R.drawable.ic_check,
                    habilitada = pedido.estado != estado && abierto,
                    onClick = { onCambiarEstadoPedido(estado) },
                )
            } + listOf(
                AccionSheet(
                    texto = stringResource(R.string.agenda_editar),
                    iconRes = R.drawable.ic_pencil,
                    habilitada = abierto,
                    onClick = onEditarPedido,
                ),
                AccionSheet(
                    texto = stringResource(R.string.agenda_cancelar_pedido),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    habilitada = abierto,
                    onClick = onPedirCancelar,
                ),
            ),
            onCerrar = onCerrarPedido,
        )
    }

    uiState.citaElegida?.let { cita ->
        val abierta = cita.estado != EstadoCita.CANCELADA
        GvAccionesSheet(
            titulo = cita.nombreCliente,
            acciones = listOf(
                AccionSheet(
                    texto = stringResource(R.string.agenda_editar),
                    iconRes = R.drawable.ic_pencil,
                    habilitada = abierta,
                    onClick = onEditarCita,
                ),
                AccionSheet(
                    texto = stringResource(R.string.agenda_cancelar_cita),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    habilitada = abierta,
                    onClick = onPedirCancelar,
                ),
            ),
            onCerrar = onCerrarCita,
        )
    }

    uiState.porCancelar?.let { elegido ->
        ConfirmarCancelacion(
            elegido = elegido,
            onConfirmar = onConfirmarCancelar,
            onDescartar = onDescartarCancelar,
        )
    }

    uiState.aviso?.let { aviso ->
        AvisarAlCliente(aviso = aviso, onAbrirWhatsApp = onAbrirWhatsApp, onCerrar = onCerrarAviso)
    }
}

/** HU-28. Pregunta antes de cancelar una cita o un pedido. */
@Composable
private fun ConfirmarCancelacion(
    elegido: PorCancelar,
    onConfirmar: () -> Unit,
    onDescartar: () -> Unit,
) {
    val titulo = when (elegido) {
        is PorCancelar.DeCita ->
            stringResource(R.string.agenda_cancelar_cita_titulo, elegido.cita.nombreCliente)
        is PorCancelar.DePedido ->
            stringResource(R.string.agenda_cancelar_pedido_titulo, elegido.pedido.nombreCliente)
    }
    AlertDialog(
        onDismissRequest = onDescartar,
        title = { Text(titulo) },
        text = { Text(stringResource(R.string.agenda_cancelar_ayuda)) },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.agenda_cancelar_confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDescartar) {
                Text(stringResource(R.string.agenda_cancelar_volver))
            }
        },
    )
}

/**
 * HU-28. Ofrece avisarle al cliente del cambio por WhatsApp, con el mensaje ya escrito.
 *
 * La app no manda nada por su cuenta: abre el chat con el texto y el usuario decide si lo
 * envía, igual que en HU-31 y HU-32. Al cliente se le saluda por su primer nombre, como se
 * le escribiría de verdad.
 */
@Composable
private fun AvisarAlCliente(
    aviso: AvisoAlCliente,
    onAbrirWhatsApp: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    val nombre = aviso.nombreCliente.trim().substringBefore(' ')
    val fecha = formatLongDate(aviso.fecha).replaceFirstChar { it.lowercase() }
    val hora = aviso.hora?.let(::formatHour).orEmpty()
    val mensaje = when (aviso.tipo) {
        TipoAviso.CitaCambiada -> stringResource(R.string.agenda_aviso_cita_cambiada, nombre, fecha, hora)
        TipoAviso.CitaCancelada -> stringResource(R.string.agenda_aviso_cita_cancelada, nombre, fecha, hora)
        TipoAviso.PedidoCambiado -> stringResource(R.string.agenda_aviso_pedido_cambiado, nombre, fecha)
        TipoAviso.PedidoCancelado -> stringResource(R.string.agenda_aviso_pedido_cancelado, nombre, fecha)
    }
    val enlace = enlaceDeWhatsApp(aviso.telefono, mensaje) ?: return

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.agenda_aviso_titulo, aviso.nombreCliente)) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = { onAbrirWhatsApp(enlace) }) {
                Text(stringResource(R.string.agenda_aviso_abrir))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(R.string.agenda_aviso_ahora_no))
            }
        },
    )
}

/** Los estados a los que se pasa un pedido a mano. Cancelar es otra acción, con confirmación (HU-28). */
private val EstadosQueSeEligen = listOf(
    EstadoPedido.PENDIENTE,
    EstadoPedido.EN_PROCESO,
    EstadoPedido.ENTREGADO,
)

private fun VistaAgenda.labelRes(): Int = when (this) {
    VistaAgenda.Dia -> R.string.agenda_vista_dia
    VistaAgenda.Semana -> R.string.agenda_vista_semana
    VistaAgenda.Mes -> R.string.agenda_vista_mes
}

/**
 * HU-27. Un día de la agenda: primero las entregas, que no tienen hora, y después las citas en
 * orden. Un día sin nada dice "Libre", para que se vea dónde cabe algo más.
 */
@Composable
private fun Dia(
    dia: LocalDate,
    citas: List<Cita>,
    pedidos: List<Pedido>,
    mostrarTitulo: Boolean,
    onAbrirCita: (Cita) -> Unit,
    onAbrirPedido: (Pedido) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mostrarTitulo) {
            Text(
                text = formatLongDate(dia),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (citas.isEmpty() && pedidos.isEmpty()) {
            Text(
                text = stringResource(R.string.agenda_dia_libre),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        pedidos.forEach { pedido -> FilaPedido(pedido, onClick = { onAbrirPedido(pedido) }) }
        citas.sortedBy { it.hora }.forEach { cita -> FilaCita(cita, onClick = { onAbrirCita(cita) }) }
    }
}

/** Una cita: a quién, cuándo, para qué y en qué estado está. */
@Composable
private fun FilaCita(cita: Cita, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GvCard(modifier.clickable(onClick = onClick)) {
        ListRow(
            title = cita.nombreCliente,
            subtitle = listOfNotNull(
                formatHour(cita.hora),
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
                stringResource(R.string.agenda_entrega),
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
            fecha = LocalDate.of(2026, 10, 24),
            hoy = LocalDate.of(2026, 10, 22),
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
            vista = VistaAgenda.Dia,
            fecha = LocalDate.of(2026, 10, 26),
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
                onVistaChange = {},
                onAnterior = {},
                onSiguiente = {},
                onHoy = {},
                onAbrirDia = {},
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
                onAbrirCita = {},
                onCerrarCita = {},
                onEditarCita = {},
                onEditarPedido = {},
                onPedirCancelar = {},
                onConfirmarCancelar = {},
                onDescartarCancelar = {},
                onAbrirWhatsApp = {},
                onCerrarAviso = {},
            )
        }
    }
}
