package com.gestor_ventures.front.ui.inicio

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvTextLink
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.util.formatHour

/** Cuántos pendientes caben en el inicio; el resto se ve en la agenda. */
private const val MaxPendientesEnInicio = 4

/**
 * Lo que toca hoy en la agenda: citas por atender y pedidos por entregar.
 *
 * Es distinto de "Últimas ventas de hoy": las ventas son lo que ya pasó y esto es lo que falta
 * por hacer. Por eso va aparte, y se muestra aunque no haya nada, para que el "no tienes
 * pendientes" también sea una respuesta.
 */
@Composable
fun PendientesHoyCard(
    pendientes: List<PendienteHoyUi>,
    onVerAgenda: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.inicio_pendientes_hoy)) {
            GvTextLink(text = stringResource(R.string.inicio_ver_agenda), onClick = onVerAgenda)
        }

        if (pendientes.isEmpty()) {
            Text(
                text = stringResource(R.string.inicio_sin_pendientes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@GvCard
        }

        pendientes.take(MaxPendientesEnInicio).forEachIndexed { indice, pendiente ->
            key(pendiente.esCita, pendiente.id) {
                if (indice > 0) SeparadorDeFilas()
                ListRow(
                    title = pendiente.cliente,
                    subtitle = subtituloDe(pendiente),
                    iconRes = if (pendiente.esCita) R.drawable.ic_calendar else R.drawable.ic_box,
                )
            }
        }

        if (pendientes.size > MaxPendientesEnInicio) {
            Text(
                text = pluralStringResource(
                    R.plurals.inicio_pendientes_mas,
                    pendientes.size - MaxPendientesEnInicio,
                    pendientes.size - MaxPendientesEnInicio,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun subtituloDe(pendiente: PendienteHoyUi): String {
    val hora = pendiente.hora?.let(::formatHour)
    return when {
        hora != null && pendiente.detalle.isNotBlank() -> "$hora · ${pendiente.detalle}"
        hora != null -> hora
        pendiente.detalle.isNotBlank() -> stringResource(R.string.inicio_pedido_para_hoy, pendiente.detalle)
        else -> stringResource(R.string.inicio_pedido_hoy)
    }
}
