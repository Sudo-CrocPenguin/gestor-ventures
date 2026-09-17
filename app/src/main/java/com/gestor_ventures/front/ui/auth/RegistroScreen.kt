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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun RegistroRoute(
    onCuentaCreada: () -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: RegistroViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RegistroScreen(
        uiState = uiState,
        onNombreChange = viewModel::onNombreChange,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onConfirmarContrasenaChange = viewModel::onConfirmarContrasenaChange,
        onMostrarContrasenaChange = viewModel::onMostrarContrasenaChange,
        onMostrarConfirmarContrasenaChange = viewModel::onMostrarConfirmarContrasenaChange,
        onAceptaTerminosChange = viewModel::onAceptaTerminosChange,
        onCrearCuenta = {
            viewModel.crearCuenta()
            onCuentaCreada()
        },
        onIniciarSesion = onIniciarSesion,
    )
}

/** HU-01. Pantalla de creación de cuenta. */
@Composable
fun RegistroScreen(
    uiState: RegistroUiState,
    onNombreChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onConfirmarContrasenaChange: (String) -> Unit,
    onMostrarContrasenaChange: () -> Unit,
    onMostrarConfirmarContrasenaChange: () -> Unit,
    onAceptaTerminosChange: (Boolean) -> Unit,
    onCrearCuenta: () -> Unit,
    onIniciarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            InsigniaApp()
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.registro_titulo),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.registro_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )

            Spacer(Modifier.height(24.dp))

            GvTextField(
                label = stringResource(R.string.registro_nombre),
                value = uiState.nombre,
                onValueChange = onNombreChange,
                placeholder = stringResource(R.string.registro_nombre_placeholder),
            )

            Spacer(Modifier.height(16.dp))

            GvTextField(
                label = stringResource(R.string.auth_correo),
                value = uiState.correo,
                onValueChange = onCorreoChange,
                placeholder = stringResource(R.string.auth_correo_placeholder),
                keyboardType = KeyboardType.Email,
            )

            Spacer(Modifier.height(16.dp))

            GvTextField(
                label = stringResource(R.string.auth_contrasena),
                value = uiState.contrasena,
                onValueChange = onContrasenaChange,
                placeholder = stringResource(R.string.registro_contrasena_placeholder),
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
            IndicadorFortaleza(nivel = uiState.fortalezaContrasena, modifier = Modifier.padding(top = 9.dp))
            Text(
                text = stringResource(R.string.registro_fortaleza_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(16.dp))

            GvTextField(
                label = stringResource(R.string.registro_confirmar_contrasena),
                value = uiState.confirmarContrasena,
                onValueChange = onConfirmarContrasenaChange,
                placeholder = stringResource(R.string.registro_confirmar_contrasena_placeholder),
                keyboardType = KeyboardType.Password,
                visualTransformation = if (uiState.mostrarConfirmarContrasena) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailing = {
                    GvPasswordToggle(
                        mostrar = uiState.mostrarConfirmarContrasena,
                        onToggle = onMostrarConfirmarContrasenaChange,
                    )
                },
            )

            Spacer(Modifier.height(16.dp))

            GvCheckbox(
                checked = uiState.aceptaTerminos,
                onCheckedChange = onAceptaTerminosChange,
                label = stringResource(R.string.registro_acepto_terminos),
            )

            Spacer(Modifier.height(20.dp))

            GvPrimaryButton(
                text = stringResource(R.string.registro_crear_cuenta),
                onClick = onCrearCuenta,
                enabled = uiState.puedeCrearCuenta,
            )

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.registro_ya_tienes_cuenta) + " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.registro_iniciar_sesion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onIniciarSesion),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InsigniaApp(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "GV",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Tres barras: se van llenando con [nivel] (0 a 3) a medida que la contraseña mejora. */
@Composable
private fun IndicadorFortaleza(nivel: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { indice ->
            val activo = indice < nivel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (activo) GestorVenturesTheme.colors.acento else MaterialTheme.colorScheme.outline,
                    ),
            )
        }
    }
}

@Preview(name = "Claro", widthDp = 375, heightDp = 900)
@Preview(name = "Oscuro", widthDp = 375, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistroScreenPreview() {
    GestorVenturesTheme {
        RegistroScreen(
            uiState = RegistroUiState(),
            onNombreChange = {},
            onCorreoChange = {},
            onContrasenaChange = {},
            onConfirmarContrasenaChange = {},
            onMostrarContrasenaChange = {},
            onMostrarConfirmarContrasenaChange = {},
            onAceptaTerminosChange = {},
            onCrearCuenta = {},
            onIniciarSesion = {},
        )
    }
}

@Preview(name = "Con datos", widthDp = 375, heightDp = 900)
@Composable
private fun RegistroScreenConDatosPreview() {
    GestorVenturesTheme {
        RegistroScreen(
            uiState = RegistroUiState(
                nombre = "Sebastián Orrego",
                correo = "sebastian@correo.com",
                contrasena = "Clave1234",
                confirmarContrasena = "Clave1234",
                aceptaTerminos = true,
            ),
            onNombreChange = {},
            onCorreoChange = {},
            onContrasenaChange = {},
            onConfirmarContrasenaChange = {},
            onMostrarContrasenaChange = {},
            onMostrarConfirmarContrasenaChange = {},
            onAceptaTerminosChange = {},
            onCrearCuenta = {},
            onIniciarSesion = {},
        )
    }
}
