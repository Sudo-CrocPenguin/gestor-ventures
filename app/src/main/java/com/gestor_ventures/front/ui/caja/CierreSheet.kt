package com.gestor_ventures.front.ui.caja

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gestor_ventures.back.model.DiferenciaDeCaja
import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatPesos
import kotlin.math.abs

/**
 * HU-21 y HU-22. Hoja para cerrar la jornada contando el efectivo que quedó.
 *
 * El aviso de la diferencia aparece a medida que se escribe, antes de confirmar: así el usuario
 * puede volver a contar sin haber cerrado nada. La nota va debajo del aviso porque se escribe
 * mirándolo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreSheet(
    cierre: FormularioCierre,
    saldoEsperado: Double,
    diferencia: DiferenciaDeCaja?,
    error: ErrorCaja?,
    onMontoRealChange: (String) -> Unit,
    onNotaChange: (String) -> Unit,
    onConfirmar: () -> Unit,
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
                text = stringResource(R.string.caja_cerrar_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Lo esperado a la vista mientras se cuenta: es contra esto que se compara.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.caja_saldo_esperado),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatPesos(saldoEsperado),
                    style = NumericTextStyle.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            GvTextField(
                label = stringResource(R.string.caja_monto_real),
                value = cierre.montoFormateado,
                onValueChange = onMontoRealChange,
                placeholder = stringResource(R.string.form_monto_placeholder),
                keyboardType = KeyboardType.Number,
                textStyle = NumericTextStyle.copy(fontSize = 17.sp),
                leading = { TextoAuxiliar(stringResource(R.string.form_moneda_simbolo)) },
                trailing = { TextoAuxiliar(stringResource(R.string.form_moneda)) },
            )

            AnimatedVisibility(visible = diferencia != null) {
                diferencia?.let { AvisoDeDiferencia(it) }
            }

            GvTextField(
                label = stringResource(R.string.caja_nota_diferencia),
                value = cierre.nota,
                onValueChange = onNotaChange,
                placeholder = stringResource(R.string.caja_nota_placeholder),
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
                text = stringResource(R.string.caja_confirmar_cierre),
                onClick = onConfirmar,
                enabled = cierre.puedeCerrar,
            )
        }
    }
}

/**
 * HU-22. Cuánto sobra o falta, dicho en plata y no en signos.
 *
 * "Faltan $1.500" se entiende de una; "−1.500" obliga a pensar contra qué. El color refuerza
 * pero no carga el mensaje solo, porque no todo el mundo lo distingue.
 */
@Composable
private fun AvisoDeDiferencia(diferencia: DiferenciaDeCaja) {
    val (titulo, detalle, color) = when {
        diferencia.cuadra -> Triple(
            stringResource(R.string.caja_cuadra),
            stringResource(R.string.caja_cuadra_detalle),
            GestorVenturesTheme.colors.success,
        )

        diferencia.sobra -> Triple(
            stringResource(R.string.caja_sobra, formatPesos(abs(diferencia.diferencia))),
            stringResource(R.string.caja_sobra_detalle),
            GestorVenturesTheme.colors.success,
        )

        else -> Triple(
            stringResource(R.string.caja_falta, formatPesos(abs(diferencia.diferencia))),
            stringResource(R.string.caja_falta_detalle),
            MaterialTheme.colorScheme.error,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
