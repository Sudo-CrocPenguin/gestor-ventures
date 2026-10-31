package com.gestor_ventures.front.ui.finanzas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
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
import com.gestor_ventures.back.model.Cliente
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
    clientes: List<Cliente> = emptyList(),
    clienteId: Long? = null,
    onClienteChange: ((Long?) -> Unit)? = null,
    /** HU-11. Los que el negocio tiene activos; al corregir una venta vieja se ofrecen todos. */
    metodosDisponibles: List<MetodoPagoUi> = MetodoPagoUi.entries,
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
            MetodoDePago(
                seleccionado = metodoPago,
                disponibles = metodosDisponibles,
                onMetodoPagoChange = onMetodoPagoChange,
            )

            // HU-29/HU-30. Solo en la venta detallada: la rápida es un total del día y no
            // tiene a quién apuntarle. El repositorio descarta el cliente igual si es rápida.
            onClienteChange?.let { alElegir ->
                ClienteDeLaVenta(
                    clientes = clientes,
                    seleccionado = clienteId,
                    onClienteChange = alElegir,
                )
            }
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

/**
 * HU-29/HU-30. A quién se le vendió.
 *
 * Es opcional y se dice: en un mostrador, a la mayoría de la gente se le vende sin registrarla,
 * y un campo obligatorio acá haría que cada venta cueste un cliente inventado.
 */
@Composable
private fun ClienteDeLaVenta(
    clientes: List<Cliente>,
    seleccionado: Long?,
    onClienteChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.venta_cliente),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (clientes.isEmpty()) {
            // Sin clientes no hay nada que elegir, pero tampoco es un error: se dice dónde se
            // crean y la venta se guarda igual.
            GvInfoNote(stringResource(R.string.venta_sin_clientes))
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            clientes.forEach { cliente ->
                GvChip(
                    text = cliente.nombre,
                    selected = cliente.id == seleccionado,
                    onClick = { onClienteChange(cliente.id) },
                )
            }
        }
    }
}

@Composable
private fun MetodoDePago(
    seleccionado: MetodoPagoUi,
    disponibles: List<MetodoPagoUi>,
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
            disponibles.forEach { metodo ->
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
