package com.gestor_ventures.front.ui.finanzas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.theme.NumericTextStyle

/**
 * HU-11/HU-12 y HU-17. Los campos de una venta, sin la pantalla alrededor.
 *
 * Viven aparte porque los piden dos sitios: el registro de una venta nueva y la corrección de
 * una que ya existe, desde el historial. Son los mismos datos con las mismas reglas, y si
 * estuvieran escritos dos veces terminarían separándose.
 *
 * Lo que cambia entre los dos sitios es el contexto —al registrar, la fecha la pone el sistema;
 * al corregir, se puede mover— así que la fecha no está acá: la pone cada pantalla.
 */
@Composable
fun CamposDeVenta(
    esDetallada: Boolean,
    montoFormateado: String,
    productoServicio: String,
    metodoPago: MetodoPagoUi,
    nota: String,
    onMontoChange: (String) -> Unit,
    onProductoServicioChange: (String) -> Unit,
    onMetodoPagoChange: (MetodoPagoUi) -> Unit,
    onNotaChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(visible = !esDetallada) {
            GvInfoNote(stringResource(R.string.venta_rapida_explicacion))
        }

        GvTextField(
            label = stringResource(
                if (esDetallada) R.string.venta_monto else R.string.venta_total_vendido,
            ),
            value = montoFormateado,
            onValueChange = onMontoChange,
            placeholder = stringResource(R.string.form_monto_placeholder),
            keyboardType = KeyboardType.Number,
            textStyle = NumericTextStyle.copy(fontSize = 17.sp),
            leading = { TextoAuxiliarVenta(stringResource(R.string.form_moneda_simbolo)) },
            trailing = { TextoAuxiliarVenta(stringResource(R.string.form_moneda)) },
        )

        if (esDetallada) {
            GvTextField(
                label = stringResource(R.string.venta_producto),
                value = productoServicio,
                onValueChange = onProductoServicioChange,
                placeholder = stringResource(R.string.venta_producto_placeholder),
            )
            MetodoDePago(seleccionado = metodoPago, onMetodoPagoChange = onMetodoPagoChange)
        } else {
            GvTextField(
                label = stringResource(R.string.venta_nota),
                value = nota,
                onValueChange = onNotaChange,
                placeholder = stringResource(R.string.venta_nota_placeholder),
            )
        }
    }
}

@Composable
private fun MetodoDePago(
    seleccionado: MetodoPagoUi,
    onMetodoPagoChange: (MetodoPagoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.form_metodo_pago),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetodoPagoUi.entries.forEach { metodo ->
                GvChip(
                    text = stringResource(metodo.labelRes),
                    selected = metodo == seleccionado,
                    onClick = { onMetodoPagoChange(metodo) },
                )
            }
        }
    }
}

/** "$" y "COP" que acompañan al monto. */
@Composable
internal fun TextoAuxiliarVenta(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
