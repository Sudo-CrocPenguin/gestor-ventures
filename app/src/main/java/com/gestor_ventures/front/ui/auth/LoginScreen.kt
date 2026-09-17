package com.gestor_ventures.front.ui.auth

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvCheckbox
import com.gestor_ventures.front.components.GvPasswordToggle
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.GestorVenturesTheme

@Composable
fun LoginRoute(
    onIniciarSesion: () -> Unit,
    onOlvidasteContrasena: () -> Unit,
    onCrearCuenta: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        uiState = uiState,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onMostrarContrasenaChange = viewModel::onMostrarContrasenaChange,
        onRecordarmeChange = viewModel::onRecordarmeChange,
        onIniciarSesion = {
            viewModel.iniciarSesion()
            onIniciarSesion()
        },
        onOlvidasteContrasena = onOlvidasteContrasena,
        onCrearCuenta = onCrearCuenta,
    )
}

/** HU-02. Pantalla de inicio de sesión. */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onMostrarContrasenaChange: () -> Unit,
    onRecordarmeChange: (Boolean) -> Unit,
    onIniciarSesion: () -> Unit,
    onOlvidasteContrasena: () -> Unit,
    onCrearCuenta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            Encabezado()
            Spacer(Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.login_titulo),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.login_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )

            Spacer(Modifier.height(28.dp))

            GvTextField(
                label = stringResource(R.string.auth_correo),
                value = uiState.correo,
                onValueChange = onCorreoChange,
                placeholder = stringResource(R.string.login_correo_placeholder),
                keyboardType = KeyboardType.Email,
            )

            Spacer(Modifier.height(16.dp))

            GvTextField(
                label = stringResource(R.string.auth_contrasena),
                value = uiState.contrasena,
                onValueChange = onContrasenaChange,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (uiState.mostrarContrasena) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailing = {
                    GvPasswordToggle(
                        mostrar = uiState.mostrarContrasena,
                        onToggle = onMostrarContrasenaChange,
                    )
                },
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GvCheckbox(
                    checked = uiState.recordarme,
                    onCheckedChange = onRecordarmeChange,
                    label = stringResource(R.string.login_recordarme),
                )
                Text(
                    text = stringResource(R.string.login_olvidaste_contrasena),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClick = onOlvidasteContrasena,
                    ),
                )
            }

            Spacer(Modifier.height(20.dp))

            GvPrimaryButton(
                text = stringResource(R.string.login_iniciar_sesion),
                onClick = onIniciarSesion,
                enabled = uiState.puedeIniciarSesion,
            )

            Spacer(Modifier.height(22.dp))
            DivisorConTexto(stringResource(R.string.login_continua_con))
            Spacer(Modifier.height(22.dp))

            BotonGoogle(onClick = {})

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.login_no_tienes_cuenta) + " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.login_crear_cuenta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onCrearCuenta),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Encabezado(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GV",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = stringResource(R.string.auth_eslogan),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Línea — texto — línea, para separar el login por correo del de terceros. */
@Composable
internal fun DivisorConTexto(texto: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }
}

/** El botón de "continuar con Google" es igual en login y registro. */
@Composable
internal fun BotonGoogle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.login_google),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Preview(name = "Claro", widthDp = 375, heightDp = 900)
@Preview(name = "Oscuro", widthDp = 375, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenPreview() {
    GestorVenturesTheme {
        LoginScreen(
            uiState = LoginUiState(correo = "sebastian@correo.com", contrasena = "12345678"),
            onCorreoChange = {},
            onContrasenaChange = {},
            onMostrarContrasenaChange = {},
            onRecordarmeChange = {},
            onIniciarSesion = {},
            onOlvidasteContrasena = {},
            onCrearCuenta = {},
        )
    }
}
