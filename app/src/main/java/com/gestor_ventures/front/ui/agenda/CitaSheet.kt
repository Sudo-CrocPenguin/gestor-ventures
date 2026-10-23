package com.gestor_ventures.front.ui.agenda

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
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.ErrorAgenda
import com.gestor_ventures.front.components.GvDateField
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.components.GvTimeField
import java.time.LocalDate
import java.time.LocalTime

/**
 * HU-24. Hoja para agendar una cita: cliente, fecha, hora y, si se quiere, una descripción.
 * El aviso de error va dentro de la hoja, porque detrás no se vería.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitaSheet(
    formulario: FormularioCita,
    clientes: List<Cliente>,
    error: ErrorAgenda?,
    onClienteChange: (Long) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    onHoraChange: (LocalTime) -> Unit,
    onDescripcionChange: (String) -> Unit,
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
                text = stringResource(R.string.agenda_cita_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SelectorDeCliente(
                clientes = clientes,
                clienteId = formulario.clienteId,
                onClienteChange = onClienteChange,
            )

            GvDateField(
                label = stringResource(R.string.agenda_fecha),
                fecha = formulario.fecha,
                onFechaChange = onFechaChange,
                placeholder = stringResource(R.string.periodo_fecha_placeholder),
            )

            GvTimeField(
                label = stringResource(R.string.agenda_hora),
                hora = formulario.hora,
                onHoraChange = onHoraChange,
                placeholder = stringResource(R.string.periodo_fecha_placeholder),
            )

            GvTextField(
                label = stringResource(R.string.agenda_descripcion),
                value = formulario.descripcion,
                onValueChange = onDescripcionChange,
                placeholder = stringResource(R.string.agenda_descripcion_placeholder),
            )

            AnimatedVisibility(visible = error != null) {
                GvInfoNote(
                    text = error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            GvPrimaryButton(
                text = stringResource(R.string.agenda_cita_guardar),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}
