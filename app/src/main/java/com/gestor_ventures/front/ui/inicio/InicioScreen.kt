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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvDosColumnas
import com.gestor_ventures.front.components.GvTextLink
import com.gestor_ventures.front.components.QuickActionButton
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatLongDate
import java.time.LocalDate
import com.gestor_ventures.front.components.tourTarget
import com.gestor_ventures.front.components.GvPrimaryButton

@Composable
fun InicioRoute(
    onVerFinanzas: () -> Unit,
    onRegistrarVenta: () -> Unit,
    onAbrirCaja: () -> Unit,
    onDefinirMeta: () -> Unit,
    onVerAlertas: () -> Unit,
    onRegistrarNegocio: () -> Unit,
    onVerAgenda: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InicioScreen(
        uiState = uiState,
        onVerFinanzas = onVerFinanzas,
        onRegistrarVenta = onRegistrarVenta,
        onAbrirCaja = onAbrirCaja,
        onDefinirMeta = onDefinirMeta,
        onVerAlertas = onVerAlertas,
        onRegistrarNegocio = onRegistrarNegocio,
        onVerAgenda = onVerAgenda,
    )
}

@Composable
fun InicioScreen(
    uiState: InicioUiState,
    onVerFinanzas: () -> Unit,
    onRegistrarVenta: () -> Unit,
    onAbrirCaja: () -> Unit,
    onDefinirMeta: () -> Unit,
    onVerAlertas: () -> Unit,
    modifier: Modifier = Modifier,
    onRegistrarNegocio: () -> Unit = {},
    onVerAgenda: () -> Unit = {},
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
        // Una cuenta que ya existía, en un dispositivo sin sus negocios: se le explica y se le da el
        // botón, en vez de mandarla a un formulario sin preguntar.
        if (uiState.sinNegocio) {
            SinNegocioCard(onRegistrarNegocio = onRegistrarNegocio)
            return@Column
        }
        // HU-08. El aviso de meta cumplida va antes que las acciones: es la noticia del día. Es
        // del dueño: un vendedor no ve la meta del negocio.
        if (uiState.metaCumplida && !uiState.esVendedor) {
            MetaCumplidaCard(onDefinirMeta = onDefinirMeta)
        }
        // Acostada, la tablet tiene ancho de sobra y alto de menos: el resumen se queda a la
        // izquierda y lo de hacer —las acciones y la caja del turno— pasa a la derecha. Parada
        // vuelve a ser una sola columna, en este mismo orden.
        GvDosColumnas(
            izquierda = {
                if (uiState.esVendedor) {
                    // El vendedor no ve las cifras del negocio: arranca por lo que le toca hoy.
                    PendientesHoyCard(pendientes = uiState.pendientesHoy, onVerAgenda = onVerAgenda)
                } else {
                    ResumenHoyCard(
                        resumen = uiState.resumenHoy,
                        onVerFinanzas = onVerFinanzas,
                        modifier = Modifier.tourTarget("inicio_resumen"),
                    )
                }
            },
            derecha = {
                AccionesRapidas(onRegistrarVenta = onRegistrarVenta, onAbrirCaja = onAbrirCaja)
                CajaDelTurnoCard(caja = uiState.cajaAbierta)

                // Lo que falta por hacer hoy, aparte de lo que ya se vendió.
                if (!uiState.esVendedor) {
                    PendientesHoyCard(pendientes = uiState.pendientesHoy, onVerAgenda = onVerAgenda)
                }

                // HU-38. Los avisos no esperan a que alguien entre a notificaciones. Son del
                // dueño del negocio.
                if (uiState.alertas.isNotEmpty() && !uiState.esVendedor) {
                    AlertasCard(alertas = uiState.alertas, onVerTodas = onVerAlertas)
                }

                // Sin ventas la tarjeta no aparece: vacía ocupa lo mismo y no dice nada.
                if (uiState.ultimasVentas.isNotEmpty()) {
                    UltimasVentasCard(
                        ventas = uiState.ultimasVentas,
                        // El historial vive en Finanzas, que el vendedor no tiene.
                        onVerHistorial = if (uiState.esVendedor) null else onVerFinanzas,
                    )
                }
            },
        )
    }
}

/** La cuenta no tiene negocios en este dispositivo: qué pasa y cómo seguir. */
@Composable
private fun SinNegocioCard(onRegistrarNegocio: () -> Unit, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        Text(
            text = stringResource(R.string.inicio_sin_negocio_titulo),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.inicio_sin_negocio_texto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )
        GvPrimaryButton(
            text = stringResource(R.string.inicio_sin_negocio_accion),
            onClick = onRegistrarNegocio,
        )
    }
}

/**
 * HU-08. El aviso de que la meta se cumplió.
 *
 * Es lo más cerca que se puede estar de una notificación mientras la Épica 8 no exista, y no
 * necesita recordar si ya se mostró: desaparece solo cuando el usuario define la meta
 * siguiente, que es justamente lo que el aviso le pide hacer.
 */
@Composable
private fun MetaCumplidaCard(onDefinirMeta: () -> Unit, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.inicio_meta_cumplida_titulo)) {
            GvTextLink(
                text = stringResource(R.string.inicio_meta_cumplida_accion),
                onClick = onDefinirMeta,
            )
        }
        Text(
            text = stringResource(R.string.inicio_meta_cumplida_detalle),
            style = MaterialTheme.typography.bodySmall,
            color = GestorVenturesTheme.colors.ingreso,
        )
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
            text = if (nombreUsuario.isBlank()) {
                stringResource(R.string.inicio_saludo_sin_nombre)
            } else {
                stringResource(R.string.inicio_saludo, nombreUsuario)
            },
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
                .fillMaxHeight()
                .tourTarget("inicio_venta"),
        )
        QuickActionButton(
            text = stringResource(R.string.inicio_abrir_caja),
            iconRes = R.drawable.ic_box,
            onClick = onAbrirCaja,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .tourTarget("inicio_caja"),
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
                onDefinirMeta = {},
                onVerAlertas = {},
            )
        }
    }
}
