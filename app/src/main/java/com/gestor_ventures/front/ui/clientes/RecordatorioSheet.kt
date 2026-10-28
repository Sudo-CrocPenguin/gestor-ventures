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
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorRecordatorio
import com.gestor_ventures.back.model.TipoRecordatorio
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvSegmentedToggle
import com.gestor_ventures.front.components.GvTextField

/**
 * HU-32. Hoja para mandarle un recordatorio a un cliente por WhatsApp.
 *
 * El tipo elige la plantilla y la plantilla queda escrita en el campo, no detrás de él: el
 * usuario ve el mensaje completo y lo corrige ahí mismo. Es lo que se manda y lo que queda
 * guardado, así que no hay dos textos que puedan decir cosas distintas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordatorioSheet(
    cliente: String,
    formulario: FormularioRecordatorio,
    error: ErrorRecordatorio?,
    onTipoChange: (TipoRecordatorio) -> Unit,
    onMensajeChange: (String) -> Unit,
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
                text = stringResource(R.string.cliente_recordatorio_titulo, cliente),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = stringResource(R.string.cliente_recordatorio_tipo),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GvSegmentedToggle(
                    opciones = TipoRecordatorio.entries,
                    seleccionada = formulario.tipo,
                    etiqueta = { tipo -> stringResource(tipo.etiquetaRes()) },
                    onSeleccionar = onTipoChange,
                )
            }

            GvTextField(
                label = stringResource(R.string.cliente_recordatorio_mensaje),
                value = formulario.mensaje,
                onValueChange = onMensajeChange,
                multilinea = true,
            )

            // Los corchetes de la plantilla son huecos a llenar, no texto para mandar así.
            GvInfoNote(stringResource(R.string.cliente_recordatorio_mensaje_ayuda))

            // El aviso va dentro de la hoja: detrás no se vería.
            AnimatedVisibility(visible = error != null) {
                GvInfoNote(
                    text = error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            GvPrimaryButton(
                text = stringResource(R.string.cliente_recordatorio_enviar),
                onClick = onEnviar,
                enabled = formulario.puedeEnviar,
            )
        }
    }
}

/**
 * HU-32. Las plantillas de mensaje, ya con el nombre del cliente y la firma del negocio.
 *
 * Las dos se resuelven de una porque los textos salen de `strings.xml` y eso solo se puede
 * hacer dentro de un composable; devolver la función deja que el cambio de tipo pida la que
 * corresponda sin volver a leer recursos.
 *
 * El día y el monto van entre corchetes para que el usuario los escriba: las citas (HU-24) y
 * los pedidos (HU-25) son de la Épica 5 y todavía no hay de dónde sacarlos.
 */
@Composable
fun plantillasDeRecordatorio(cliente: String, negocio: String): (TipoRecordatorio) -> String {
    val cita = stringResource(R.string.cliente_recordatorio_plantilla_cita, cliente, negocio)
    val pago = stringResource(R.string.cliente_recordatorio_plantilla_pago, cliente, negocio)

    return { tipo ->
        when (tipo) {
            TipoRecordatorio.Cita -> cita
            TipoRecordatorio.PagoPendiente -> pago
        }
    }
}
