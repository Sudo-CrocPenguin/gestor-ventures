package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.celularConFormato
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.GestorVenturesTheme

@Composable
fun WhatsAppEmpresaRoute(
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: WhatsAppEmpresaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.guardado.collect { onGuardado() }
    }

    WhatsAppEmpresaScreen(
        uiState = uiState,
        onBack = onBack,
        onNumeroChange = viewModel::onNumeroChange,
        onGuardar = viewModel::guardar,
        onDesvincular = viewModel::desvincular,
    )
}

/**
 * El celular de WhatsApp con el que el negocio le escribe a sus clientes. Los recordatorios de
 * citas se arman a nombre de la empresa y se abren desde la app de WhatsApp Business si está
 * instalada.
 */
@Composable
fun WhatsAppEmpresaScreen(
    uiState: WhatsAppEmpresaUiState,
    onBack: () -> Unit,
    onNumeroChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onDesvincular: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.menu_whatsapp_empresa), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.whatsapp_empresa_explicacion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState.sinNegocio) GvInfoNote(stringResource(R.string.asistente_sin_negocio))

            uiState.vinculado?.let { numero ->
                TarjetaVinculado(
                    nombreNegocio = uiState.nombreNegocio,
                    numero = numero,
                    onDesvincular = onDesvincular,
                )
            }

            GvTextField(
                label = stringResource(R.string.whatsapp_empresa_numero),
                value = uiState.numero,
                onValueChange = onNumeroChange,
                placeholder = stringResource(R.string.whatsapp_empresa_placeholder),
                keyboardType = KeyboardType.Phone,
            )

            AnimatedVisibility(visible = uiState.numeroInvalido) {
                GvInfoNote(
                    text = stringResource(R.string.whatsapp_empresa_invalido),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            // El botón va pegado al formulario y no al borde de abajo: en una tablet, anclado al
            // fondo, dejaba media pantalla en blanco entre el campo y el botón.
            GvPrimaryButton(
                text = stringResource(
                    if (uiState.vinculado == null) R.string.whatsapp_empresa_vincular
                    else R.string.whatsapp_empresa_actualizar,
                ),
                onClick = onGuardar,
                enabled = uiState.puedeGuardar,
            )

            GvInfoNote(stringResource(R.string.whatsapp_empresa_nota))
        }
    }
}

/** El número ya vinculado: ícono, de quién es, el número bien grande y cómo desvincularlo. */
@Composable
private fun TarjetaVinculado(
    nombreNegocio: String,
    numero: String,
    onDesvincular: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(GestorVenturesTheme.colors.ingresoContainer, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_message),
                    contentDescription = null,
                    tint = GestorVenturesTheme.colors.ingreso,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.whatsapp_empresa_vinculado, nombreNegocio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = celularConFormato(numero),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(onClick = onDesvincular) {
                Text(
                    text = stringResource(R.string.whatsapp_empresa_desvincular),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(name = "WhatsApp de la empresa", widthDp = 380, heightDp = 700)
@Preview(name = "WhatsApp de la empresa (oscuro)", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WhatsAppEmpresaPreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            WhatsAppEmpresaScreen(
                uiState = WhatsAppEmpresaUiState(
                    nombreNegocio = "Bella Piel",
                    vinculado = "3001234567",
                    numero = "3001234567",
                ),
                onBack = {},
                onNumeroChange = {},
                onGuardar = {},
                onDesvincular = {},
            )
        }
    }
}
