package com.gestor_ventures.front.ui.auth

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.GestorVenturesTheme

@Composable
fun RecuperarContrasenaRoute(
    onBack: () -> Unit,
    onCodigoEnviado: () -> Unit,
    viewModel: RecuperarContrasenaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RecuperarContrasenaScreen(
        uiState = uiState,
        onBack = onBack,
        onCorreoChange = viewModel::onCorreoChange,
        onEnviarCodigo = {
            viewModel.enviarCodigo()
            onCodigoEnviado()
        },
    )
}

/** HU-03. Pide el correo para mandar el código de recuperación. */
@Composable
fun RecuperarContrasenaScreen(
    uiState: RecuperarContrasenaUiState,
    onBack: () -> Unit,
    onCorreoChange: (String) -> Unit,
    onEnviarCodigo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.cd_volver),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(20.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.recuperar_titulo),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.recuperar_subtitulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )

                Spacer(Modifier.height(24.dp))

                GvTextField(
                    label = stringResource(R.string.auth_correo),
                    value = uiState.correo,
                    onValueChange = onCorreoChange,
                    placeholder = stringResource(R.string.login_correo_placeholder),
                    keyboardType = KeyboardType.Email,
                )

                Spacer(Modifier.height(20.dp))

                GvPrimaryButton(
                    text = stringResource(R.string.recuperar_enviar_codigo),
                    onClick = onEnviarCodigo,
                    enabled = uiState.puedeEnviarCodigo,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.recuperar_volver_a_iniciar_sesion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onBack),
                )
            }
        }
    }
}

@Preview(name = "Claro", widthDp = 375, heightDp = 900)
@Preview(name = "Oscuro", widthDp = 375, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecuperarContrasenaScreenPreview() {
    GestorVenturesTheme {
        RecuperarContrasenaScreen(
            uiState = RecuperarContrasenaUiState(correo = "sebastian@correo.com"),
            onBack = {},
            onCorreoChange = {},
            onEnviarCodigo = {},
        )
    }
}
