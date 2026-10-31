package com.gestor_ventures.front.ui.clientes

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.TipoRecordatorio
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvSoftButton
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatDiaCorto
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDateTime

@Composable
fun ClienteDetalleRoute(onBack: () -> Unit, viewModel: ClienteDetalleViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    val abrirChat: (String) -> Unit = { enlace ->
        contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace.toUri()))
    }

    // HU-32. WhatsApp se abre cuando el recordatorio ya quedó guardado, no al tocar el botón.
    LaunchedEffect(viewModel) {
        viewModel.abrirWhatsApp.collect { enlace -> abrirChat(enlace) }
    }

    ClienteDetalleScreen(
        uiState = uiState,
        onBack = onBack,
        onAbrirWhatsApp = abrirChat,
        onAbrirRecordatorio = viewModel::abrirRecordatorio,
        onTipoRecordatorioChange = viewModel::onTipoRecordatorioChange,
        onMensajeRecordatorioChange = viewModel::onMensajeRecordatorioChange,
        onEnviarRecordatorio = viewModel::enviarRecordatorio,
        onCerrarRecordatorio = viewModel::cerrarRecordatorio,
    )
}

/**
 * HU-30, HU-31 y HU-32. El perfil de un cliente: cómo ubicarlo, qué le ha comprado al negocio
 * y qué se le ha recordado.
 *
 * El total acumulado va arriba y grande porque es lo que responde "¿cuánto vale este cliente
 * para mí?". El detalle de cada compra va debajo, para cuando esa cifra sorprende.
 */
@Composable
fun ClienteDetalleScreen(
    uiState: ClienteDetalleUiState,
    onBack: () -> Unit,
    onAbrirWhatsApp: (String) -> Unit,
    onAbrirRecordatorio: (String) -> Unit,
    onTipoRecordatorioChange: (TipoRecordatorio, String) -> Unit,
    onMensajeRecordatorioChange: (String) -> Unit,
    onEnviarRecordatorio: () -> Unit,
    onCerrarRecordatorio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cliente = uiState.cliente
    val plantilla = plantillasDeRecordatorio(
        cliente = cliente?.nombre.orEmpty(),
        negocio = uiState.negocio,
    )

    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = cliente?.nombre.orEmpty(), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (cliente == null) return@Column

            Contacto(cliente)

            GvCard {
                GvCardHeader(title = stringResource(R.string.cliente_total_acumulado))
                Text(
                    text = formatPesos(uiState.totalAcumulado),
                    style = NumericTextStyle.copy(fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.cliente_cuantas_compras,
                        uiState.compras.size,
                        uiState.compras.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // HU-31. Acá es "su perfil", que es donde el criterio pide el botón.
            uiState.whatsapp?.let { enlace ->
                GvPrimaryButton(
                    text = stringResource(R.string.cliente_accion_whatsapp),
                    onClick = { onAbrirWhatsApp(enlace) },
                )
            }

            // HU-32. Abre con la plantilla del tipo que la hoja trae marcado, para que el
            // mensaje esté escrito desde el primer momento.
            if (uiState.puedeRecordar) {
                GvSoftButton(
                    text = stringResource(R.string.cliente_accion_recordatorio),
                    onClick = { onAbrirRecordatorio(plantilla(FormularioRecordatorio().tipo)) },
                    iconRes = R.drawable.ic_bell,
                )
            }

            Text(
                text = stringResource(R.string.cliente_historial_compras),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (uiState.sinCompras) {
                GvInfoNote(stringResource(R.string.cliente_sin_compras))
            }

            uiState.compras.forEach { compra ->
                key(compra.id) { FilaDeCompra(compra) }
            }

            // HU-32. El historial solo aparece cuando hay algo que contar: un título con nada
            // debajo parece que la app perdió los recordatorios.
            if (uiState.recordatorios.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.cliente_recordatorio_historial),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )

                uiState.recordatorios.forEach { recordatorio ->
                    key(recordatorio.id) { FilaDeRecordatorio(recordatorio) }
                }
            }

            // Lo que falta de la historia, dicho de frente en vez de que parezca un olvido.
            if (!uiState.cargando) {
                GvInfoNote(stringResource(R.string.cliente_citas_pendientes))
            }
        }

        uiState.recordatorio?.let { formulario ->
            RecordatorioSheet(
                cliente = cliente?.nombre.orEmpty(),
                formulario = formulario,
                error = uiState.errorRecordatorio,
                onTipoChange = { tipo -> onTipoRecordatorioChange(tipo, plantilla(tipo)) },
                onMensajeChange = onMensajeRecordatorioChange,
                onEnviar = onEnviarRecordatorio,
                onCerrar = onCerrarRecordatorio,
            )
        }
    }
}

/** Los datos con los que se ubica al cliente, y sus notas si las tiene. */
@Composable
private fun Contacto(cliente: Cliente, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.cliente_contacto))

        // Sin el "(opcional)" del formulario: acá ya no hay nada que decidir, solo que leer.
        Linea(stringResource(R.string.cliente_telefono_perfil), cliente.telefono)
        Linea(stringResource(R.string.cliente_correo_perfil), cliente.correo)

        cliente.notas?.let { notas ->
            Text(
                text = notas,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Un dato de contacto. Si no lo dejó, se dice en vez de mostrar un renglón vacío. */
@Composable
private fun Linea(etiqueta: String, valor: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor ?: stringResource(R.string.cliente_sin_dato),
            style = MaterialTheme.typography.bodyMedium,
            color = if (valor == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun FilaDeCompra(compra: CompraUi, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        ListRow(
            title = compra.titulo.ifBlank { stringResource(R.string.cliente_compra_sin_detalle) },
            subtitle = stringResource(
                R.string.cliente_compra_cuando,
                formatDiaCorto(compra.fechaHora.toLocalDate()),
                formatHour(compra.fechaHora.toLocalTime()),
            ),
            iconRes = R.drawable.ic_plus,
            iconContainerColor = GestorVenturesTheme.colors.ingresoContainer,
            iconColor = GestorVenturesTheme.colors.ingreso,
            trailing = {
                MoneyText(monto = compra.monto, color = GestorVenturesTheme.colors.ingreso)
            },
        )
    }
}

/**
 * HU-32. Un recordatorio ya mandado.
 *
 * Se muestra el mensaje y no solo el tipo: al volver a escribirle, lo que sirve es qué se le
 * dijo, no que se le dijo algo. Recortado a tres líneas para que el historial siga siendo una
 * lista y no una sucesión de párrafos.
 */
@Composable
private fun FilaDeRecordatorio(recordatorio: RecordatorioUi, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(text = stringResource(recordatorio.tipo.etiquetaRes()))
            Text(
                text = stringResource(
                    R.string.cliente_recordatorio_cuando,
                    formatDiaCorto(recordatorio.fechaEnvio.toLocalDate()),
                    formatHour(recordatorio.fechaEnvio.toLocalTime()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = recordatorio.mensaje,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(name = "Perfil del cliente", widthDp = 380, heightDp = 860)
@Preview(
    name = "Perfil del cliente (oscuro)",
    widthDp = 380,
    heightDp = 860,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ClienteDetallePreview() {
    val hoy = LocalDateTime.of(2026, 10, 16, 11, 30)
    VistaPreviaDetalle(
        ClienteDetalleUiState(
            cargando = false,
            negocio = "Dulce Antojo",
            cliente = Cliente(
                id = 1,
                nombre = "Laura Gómez",
                telefono = "3001112233",
                notas = "Prefiere sin azúcar",
            ),
            compras = listOf(
                CompraUi(3, "Torta personalizada", 85_000.0, hoy),
                CompraUi(2, "Cupcakes x6", 7_500.0, hoy.minusDays(4)),
                CompraUi(1, "", 25_000.0, hoy.minusDays(11)),
            ),
            recordatorios = listOf(
                RecordatorioUi(
                    id = 2,
                    tipo = TipoRecordatorio.Cita,
                    mensaje = "Hola Laura 👋\n\nTe recuerdo tu cita de decoración el martes " +
                        "a las 11:00 a. m.\n\n¡Te espero!\nDulce Antojo",
                    fechaEnvio = hoy.minusDays(1),
                ),
                RecordatorioUi(
                    id = 1,
                    tipo = TipoRecordatorio.PagoPendiente,
                    mensaje = "Hola Laura 👋\n\nTe recuerdo el saldo pendiente de 40.000.",
                    fechaEnvio = hoy.minusDays(9),
                ),
            ),
        ),
    )
}

@Preview(name = "Cliente sin compras", widthDp = 380, heightDp = 860)
@Composable
private fun ClienteSinComprasPreview() {
    VistaPreviaDetalle(
        ClienteDetalleUiState(
            cargando = false,
            cliente = Cliente(id = 2, nombre = "Camila Torres"),
        ),
    )
}

@Composable
private fun VistaPreviaDetalle(uiState: ClienteDetalleUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ClienteDetalleScreen(
                uiState = uiState,
                onBack = {},
                onAbrirWhatsApp = {},
                onAbrirRecordatorio = {},
                onTipoRecordatorioChange = { _, _ -> },
                onMensajeRecordatorioChange = {},
                onEnviarRecordatorio = {},
                onCerrarRecordatorio = {},
            )
        }
    }
}
