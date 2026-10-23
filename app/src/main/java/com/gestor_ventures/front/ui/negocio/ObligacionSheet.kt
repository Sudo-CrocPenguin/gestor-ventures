package com.gestor_ventures.front.ui.negocio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorObligacion
import com.gestor_ventures.front.components.GvDateField
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.NumericTextStyle
import java.time.LocalDate

/**
 * HU-07. Hoja para registrar o corregir una obligación: qué se debe, cuánto y para cuándo.
 *
 * La fecha se deja abierta a propósito: una cuota vence en el futuro y una deuda atrasada ya
 * venció, y las dos se tienen que poder registrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObligacionSheet(
    formulario: FormularioObligacion,
    error: ErrorObligacion?,
    onNombreChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(
                        if (formulario.esEdicion) {
                            R.string.obligacion_titulo_editar
                        } else {
                            R.string.obligacion_titulo_nueva
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.obligacion_explicacion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            GvTextField(
                label = stringResource(R.string.obligacion_nombre),
                value = formulario.nombre,
                onValueChange = onNombreChange,
                placeholder = stringResource(R.string.obligacion_nombre_placeholder),
            )

            GvTextField(
                label = stringResource(R.string.form_monto),
                value = formulario.montoFormateado,
                onValueChange = onMontoChange,
                placeholder = stringResource(R.string.form_monto_placeholder),
                keyboardType = KeyboardType.Number,
                textStyle = NumericTextStyle.copy(fontSize = 17.sp),
                leading = { TextoAuxiliarObligacion(stringResource(R.string.form_moneda_simbolo)) },
                trailing = { TextoAuxiliarObligacion(stringResource(R.string.form_moneda)) },
            )

            GvDateField(
                label = stringResource(R.string.obligacion_vencimiento),
                fecha = formulario.fechaVencimiento,
                onFechaChange = onFechaChange,
                placeholder = stringResource(R.string.obligacion_vencimiento_placeholder),
            )

            AnimatedVisibility(visible = error != null) {
                GvInfoNote(
                    text = error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            GvPrimaryButton(
                text = stringResource(
                    if (formulario.esEdicion) {
                        R.string.obligacion_guardar_cambios
                    } else {
                        R.string.obligacion_guardar
                    },
                ),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}

/** "$" y "COP" que acompañan al monto. */
@Composable
private fun TextoAuxiliarObligacion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
