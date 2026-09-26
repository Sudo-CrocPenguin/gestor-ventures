package com.gestor_ventures.front.ui.clientes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorCliente
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField

/**
 * HU-29. Hoja para registrar o corregir un cliente.
 *
 * Los tres campos de contacto dicen "opcional" con todas las letras. Es la forma de que nadie
 * invente un teléfono para poder pasar de pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteSheet(
    formulario: FormularioCliente,
    error: ErrorCliente?,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onNotasChange: (String) -> Unit,
    onGuardar: () -> Unit,
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
                text = stringResource(
                    if (formulario.esEdicion) R.string.cliente_titulo_editar
                    else R.string.cliente_titulo_nuevo,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            GvTextField(
                label = stringResource(R.string.cliente_nombre),
                value = formulario.nombre,
                onValueChange = onNombreChange,
                placeholder = stringResource(R.string.cliente_nombre_placeholder),
            )

            GvTextField(
                label = stringResource(R.string.cliente_telefono),
                value = formulario.telefono,
                onValueChange = onTelefonoChange,
                placeholder = stringResource(R.string.cliente_telefono_placeholder),
                keyboardType = KeyboardType.Phone,
            )

            GvTextField(
                label = stringResource(R.string.cliente_correo),
                value = formulario.correo,
                onValueChange = onCorreoChange,
                placeholder = stringResource(R.string.cliente_correo_placeholder),
                keyboardType = KeyboardType.Email,
            )

            GvTextField(
                label = stringResource(R.string.cliente_notas),
                value = formulario.notas,
                onValueChange = onNotasChange,
                placeholder = stringResource(R.string.cliente_notas_placeholder),
            )

            // El aviso va dentro de la hoja: detrás no se vería.
            AnimatedVisibility(visible = error != null) {
                GvInfoNote(
                    text = error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            GvPrimaryButton(
                text = stringResource(
                    if (formulario.esEdicion) R.string.cliente_guardar_cambios
                    else R.string.cliente_guardar,
                ),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}
