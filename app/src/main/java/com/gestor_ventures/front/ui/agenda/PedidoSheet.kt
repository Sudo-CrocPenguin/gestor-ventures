package com.gestor_ventures.front.ui.agenda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.ErrorAgenda
import com.gestor_ventures.front.components.GvDateField
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvSoftButton
import com.gestor_ventures.front.components.GvTextField
import java.time.LocalDate

/**
 * HU-25. Hoja para registrar un pedido: cliente, fecha de entrega y los productos, cada uno en
 * su fila con nombre y cantidad. La basura solo aparece cuando hay más de una fila.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoSheet(
    formulario: FormularioPedido,
    clientes: List<Cliente>,
    error: ErrorAgenda?,
    onClienteChange: (Long) -> Unit,
    onFechaEntregaChange: (LocalDate) -> Unit,
    onProductoNombreChange: (Int, String) -> Unit,
    onProductoCantidadChange: (Int, String) -> Unit,
    onAgregarProducto: () -> Unit,
    onQuitarProducto: (Int) -> Unit,
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
                text = stringResource(R.string.agenda_pedido_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SelectorDeCliente(
                clientes = clientes,
                clienteId = formulario.clienteId,
                onClienteChange = onClienteChange,
            )

            GvDateField(
                label = stringResource(R.string.agenda_fecha_entrega),
                fecha = formulario.fechaEntrega,
                onFechaChange = onFechaEntregaChange,
                placeholder = stringResource(R.string.periodo_fecha_placeholder),
            )

            formulario.productos.forEachIndexed { indice, producto ->
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GvTextField(
                        label = stringResource(R.string.agenda_producto),
                        value = producto.nombre,
                        onValueChange = { onProductoNombreChange(indice, it) },
                        placeholder = stringResource(R.string.agenda_producto_placeholder),
                        modifier = Modifier.weight(1f),
                    )
                    GvTextField(
                        label = stringResource(R.string.agenda_cantidad),
                        value = producto.cantidad,
                        onValueChange = { onProductoCantidadChange(indice, it) },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.width(76.dp),
                    )
                    if (formulario.productos.size > 1) {
                        IconButton(onClick = { onQuitarProducto(indice) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trash),
                                contentDescription = stringResource(R.string.agenda_quitar_producto),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            GvSoftButton(
                text = stringResource(R.string.agenda_agregar_producto),
                onClick = onAgregarProducto,
                iconRes = R.drawable.ic_plus,
            )

            AnimatedVisibility(visible = error != null) {
                GvInfoNote(
                    text = error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            GvPrimaryButton(
                text = stringResource(R.string.agenda_pedido_guardar),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}
