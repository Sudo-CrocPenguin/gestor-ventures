package com.gestor_ventures.front.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cliente

private val FieldShape = RoundedCornerShape(13.dp)

/**
 * HU-24 y HU-25. Campo para elegir a qué cliente (HU-29) es la cita o el pedido. Se ve como los
 * demás campos y al tocarlo despliega la lista de clientes del negocio.
 */
@Composable
fun SelectorDeCliente(
    clientes: List<Cliente>,
    clienteId: Long?,
    onClienteChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }
    val elegido = clientes.firstOrNull { it.id == clienteId }

    Column(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.agenda_cliente),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FieldShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, FieldShape)
                    .clickable(role = Role.Button) { abierto = true }
                    .padding(horizontal = 13.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = elegido?.nombre ?: stringResource(R.string.agenda_cliente_placeholder),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = if (elegido == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                clientes.forEach { cliente ->
                    DropdownMenuItem(
                        text = { Text(cliente.nombre) },
                        onClick = {
                            onClienteChange(cliente.id)
                            abierto = false
                        },
                    )
                }
            }
        }
    }
}
