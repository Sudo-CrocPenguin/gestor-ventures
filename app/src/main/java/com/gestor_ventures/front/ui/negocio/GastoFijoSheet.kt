package com.gestor_ventures.front.ui.negocio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
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
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.NumericTextStyle

/**
 * HU-06. Hoja para agregar un gasto fijo sin salir del onboarding: nombre, monto y cada
 * cuánto se paga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoFijoSheet(
    formulario: FormularioGastoFijo,
    onNombreChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFrecuenciaChange: (Frecuencia) -> Unit,
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.gasto_fijo_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.gasto_fijo_explicacion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            GvTextField(
                label = stringResource(R.string.gasto_fijo_nombre),
                value = formulario.nombre,
                onValueChange = onNombreChange,
                placeholder = stringResource(R.string.gasto_fijo_nombre_placeholder),
            )

            GvTextField(
                label = stringResource(R.string.form_monto),
                value = formulario.montoFormateado,
                onValueChange = onMontoChange,
                placeholder = stringResource(R.string.form_monto_placeholder),
                keyboardType = KeyboardType.Number,
                textStyle = NumericTextStyle.copy(fontSize = 17.sp),
                leading = { TextoMoneda(stringResource(R.string.form_moneda_simbolo)) },
                trailing = { TextoMoneda(stringResource(R.string.form_moneda)) },
            )

            Column {
                Text(
                    text = stringResource(R.string.gasto_fijo_frecuencia),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Frecuencia.entries.forEach { frecuencia ->
                        GvChip(
                            text = stringResource(frecuencia.labelRes()),
                            selected = frecuencia == formulario.frecuencia,
                            onClick = { onFrecuenciaChange(frecuencia) },
                        )
                    }
                }
            }

            GvPrimaryButton(
                text = stringResource(R.string.gasto_fijo_guardar),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}

@Composable
private fun TextoMoneda(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
