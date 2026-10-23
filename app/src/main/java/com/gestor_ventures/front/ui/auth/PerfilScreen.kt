package com.gestor_ventures.front.ui.auth

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.gestor_ventures.back.model.ErrorPerfil
import com.gestor_ventures.front.components.GvAvatar
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvReadOnlyField
import com.gestor_ventures.front.components.GvSoftButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.aIniciales

@Composable
fun PerfilRoute(
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.guardado.collect { onGuardado() }
    }

    val elegirFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onFotoElegida(it.toString()) } }

    PerfilScreen(
        uiState = uiState,
        onBack = onBack,
        onNombreChange = viewModel::onNombreChange,
        onTelefonoChange = viewModel::onTelefonoChange,
        onCambiarFoto = {
            elegirFoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onQuitarFoto = viewModel::quitarFoto,
        onGuardar = viewModel::guardar,
    )
}

@Composable
fun PerfilScreen(
    uiState: PerfilUiState,
    onBack: () -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onCambiarFoto: () -> Unit,
    onQuitarFoto: () -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.menu_mi_perfil), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Foto(
                iniciales = uiState.nombre.aIniciales(),
                foto = uiState.fotoVisible,
                onCambiarFoto = onCambiarFoto,
                onQuitarFoto = onQuitarFoto,
            )

            GvTextField(
                label = stringResource(R.string.registro_nombre),
                value = uiState.nombre,
                onValueChange = onNombreChange,
                placeholder = stringResource(R.string.registro_nombre_placeholder),
            )

            GvReadOnlyField(
                label = stringResource(R.string.perfil_correo),
                value = uiState.correo,
            )

            GvTextField(
                label = stringResource(R.string.cliente_telefono),
                value = uiState.telefono,
                onValueChange = onTelefonoChange,
                placeholder = stringResource(R.string.cliente_telefono_placeholder),
                keyboardType = KeyboardType.Phone,
            )

            val aviso = uiState.error?.mensajeRes()
                ?: R.string.perfil_error_telefono.takeIf { uiState.telefonoInvalido }
                ?: R.string.auth_error_nombre_largo.takeIf { uiState.nombreMuyLargo }
            AnimatedVisibility(visible = aviso != null) {
                GvInfoNote(
                    text = aviso?.let { stringResource(it) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.negocio_guardar_cambios),
                onClick = onGuardar,
                enabled = uiState.puedeGuardar,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun Foto(
    iniciales: String,
    foto: String?,
    onCambiarFoto: () -> Unit,
    onQuitarFoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GvAvatar(
            iniciales = iniciales,
            foto = foto,
            size = 96.dp,
            shape = RoundedCornerShape(28.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GvSoftButton(
                text = stringResource(R.string.perfil_cambiar_foto),
                onClick = onCambiarFoto,
                iconRes = R.drawable.ic_pencil,
                modifier = Modifier.weight(1f),
            )
            if (foto != null) {
                TextButton(onClick = onQuitarFoto) {
                    Text(
                        text = stringResource(R.string.perfil_quitar_foto),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.perfil_foto_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Claro", widthDp = 380, heightDp = 760)
@Preview(name = "Oscuro", widthDp = 380, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PerfilScreenPreview() {
    VistaPrevia(
        PerfilUiState(
            nombre = "Sebastián Orrego",
            correo = "sebastian@correo.example",
            telefono = "3001112233",
            cargando = false,
        ),
    )
}

@Preview(name = "Con error", widthDp = 380, heightDp = 760)
@Composable
private fun PerfilErrorPreview() {
    VistaPrevia(
        PerfilUiState(
            nombre = "Sebastián Orrego",
            correo = "sebastian@correo.example",
            cargando = false,
            error = ErrorPerfil.FotoMuyPesada,
        ),
    )
}

@Composable
private fun VistaPrevia(uiState: PerfilUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PerfilScreen(
                uiState = uiState,
                onBack = {},
                onNombreChange = {},
                onTelefonoChange = {},
                onCambiarFoto = {},
                onQuitarFoto = {},
                onGuardar = {},
            )
        }
    }
}
