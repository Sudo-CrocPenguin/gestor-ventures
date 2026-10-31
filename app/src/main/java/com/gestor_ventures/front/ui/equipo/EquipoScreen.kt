package com.gestor_ventures.front.ui.equipo

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.EstadoMiembro
import com.gestor_ventures.back.model.MiembroEquipo
import com.gestor_ventures.front.components.AccionSheet
import com.gestor_ventures.front.components.GvAccionesSheet
import com.gestor_ventures.front.components.GvAvatar
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.aIniciales
import java.time.LocalDateTime

@Composable
fun EquipoRoute(
    onBack: () -> Unit,
    viewModel: EquipoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EquipoScreen(
        uiState = uiState,
        onBack = onBack,
        onInvitar = viewModel::abrirInvitacion,
        onCorreoChange = viewModel::onCorreoChange,
        onEnviarInvitacion = viewModel::invitar,
        onCerrarInvitacion = viewModel::cerrarInvitacion,
        onAbrirAcciones = viewModel::abrirAcciones,
        onQuitar = viewModel::quitarElegido,
        onCerrarAcciones = viewModel::cerrarAcciones,
    )
}

/**
 * El equipo del negocio: el Líder y los vendedores que invitó. Tocar un vendedor permite
 * quitarlo o cancelar su invitación.
 */
@Composable
fun EquipoScreen(
    uiState: EquipoUiState,
    onBack: () -> Unit,
    onInvitar: () -> Unit,
    onCorreoChange: (String) -> Unit,
    onEnviarInvitacion: () -> Unit,
    onCerrarInvitacion: () -> Unit,
    onAbrirAcciones: (MiembroEquipo) -> Unit,
    onQuitar: () -> Unit,
    onCerrarAcciones: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.menu_equipo), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.sinNegocio) GvInfoNote(stringResource(R.string.asistente_sin_negocio))

            Seccion(stringResource(R.string.equipo_lider))
            GvCard {
                uiState.lider?.let { lider ->
                    FilaPersona(
                        nombre = lider.nombre,
                        correo = lider.correo,
                        etiqueta = { StatusPill(text = stringResource(R.string.equipo_rol_lider)) },
                    )
                }
            }

            Seccion(stringResource(R.string.equipo_vendedores))
            if (uiState.sinVendedores) {
                GvInfoNote(stringResource(R.string.equipo_vacio))
            } else {
                GvCard {
                    uiState.vendedores.forEachIndexed { indice, miembro ->
                        if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        FilaPersona(
                            nombre = miembro.correo.substringBefore('@'),
                            correo = miembro.correo,
                            etiqueta = { EtiquetaDeEstado(miembro.estado) },
                            modifier = Modifier.clickable { onAbrirAcciones(miembro) },
                        )
                    }
                }
            }

            GvInfoNote(stringResource(R.string.equipo_nota))
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.equipo_invitar),
                onClick = onInvitar,
                enabled = !uiState.sinNegocio,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    uiState.invitacion?.let { formulario ->
        InvitarSheet(
            formulario = formulario,
            onCorreoChange = onCorreoChange,
            onEnviar = onEnviarInvitacion,
            onCerrar = onCerrarInvitacion,
        )
    }

    uiState.elegido?.let { miembro ->
        GvAccionesSheet(
            titulo = miembro.correo,
            acciones = listOf(
                AccionSheet(
                    texto = stringResource(
                        if (miembro.estado == EstadoMiembro.PENDIENTE) R.string.equipo_cancelar_invitacion
                        else R.string.equipo_quitar,
                    ),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    onClick = onQuitar,
                ),
            ),
            onCerrar = onCerrarAcciones,
        )
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun EtiquetaDeEstado(estado: EstadoMiembro) {
    when (estado) {
        EstadoMiembro.ACTIVO -> StatusPill(
            text = stringResource(R.string.equipo_estado_activo),
            containerColor = GestorVenturesTheme.colors.ingresoContainer,
            contentColor = GestorVenturesTheme.colors.ingreso,
        )
        EstadoMiembro.PENDIENTE -> StatusPill(
            text = stringResource(R.string.equipo_estado_pendiente),
            containerColor = GestorVenturesTheme.colors.warningContainer,
            contentColor = GestorVenturesTheme.colors.warning,
        )
    }
}

@Composable
private fun FilaPersona(
    nombre: String,
    correo: String,
    etiqueta: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GvAvatar(iniciales = nombre.aIniciales(), size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = correo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        etiqueta()
    }
}

/** La hoja para invitar: un solo campo, el correo. El rol de quien se invita es siempre Vendedor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitarSheet(
    formulario: FormularioInvitacion,
    onCorreoChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.equipo_invitar),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            GvInfoNote(stringResource(R.string.equipo_invitar_explicacion))
            GvTextField(
                label = stringResource(R.string.equipo_correo),
                value = formulario.correo,
                onValueChange = onCorreoChange,
                placeholder = stringResource(R.string.cliente_correo_placeholder),
                keyboardType = KeyboardType.Email,
            )
            AnimatedVisibility(visible = formulario.error != null) {
                GvInfoNote(
                    text = formulario.error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            GvPrimaryButton(
                text = stringResource(R.string.equipo_guardar_invitacion),
                onClick = onEnviar,
                enabled = formulario.puedeEnviar,
            )
        }
    }
}

@Preview(name = "Equipo", widthDp = 380, heightDp = 760)
@Preview(name = "Equipo (oscuro)", widthDp = 380, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EquipoPreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EquipoScreen(
                uiState = EquipoUiState(
                    lider = LiderUi("Mariana", "mariana@gmail.com"),
                    vendedores = listOf(
                        MiembroEquipo(1, "valentina@correo.com", EstadoMiembro.ACTIVO, LocalDateTime.now()),
                        MiembroEquipo(2, "ana@correo.com", EstadoMiembro.PENDIENTE, LocalDateTime.now()),
                    ),
                ),
                onBack = {},
                onInvitar = {},
                onCorreoChange = {},
                onEnviarInvitacion = {},
                onCerrarInvitacion = {},
                onAbrirAcciones = {},
                onQuitar = {},
                onCerrarAcciones = {},
            )
        }
    }
}
