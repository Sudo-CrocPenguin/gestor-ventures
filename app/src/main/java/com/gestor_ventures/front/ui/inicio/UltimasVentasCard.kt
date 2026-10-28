package com.gestor_ventures.front.ui.inicio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvTextLink
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatHour

/**
 * HU-11/HU-12. Lo último que se vendió hoy.
 *
 * El resumen de arriba dice cuánto se vendió; esto dice qué se vendió, que es lo que responde
 * "¿y eso de dónde salió?" sin tener que ir al historial.
 *
 * No aparece cuando el día todavía no tiene ventas: una tarjeta vacía ocupa el mismo sitio que
 * una llena y no dice nada.
 */
@Composable
fun UltimasVentasCard(
    ventas: List<VentaDeHoyUi>,
    onVerHistorial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.inicio_ultimas_ventas)) {
            GvTextLink(text = stringResource(R.string.inicio_ver_todas), onClick = onVerHistorial)
        }

        ventas.forEach { venta ->
            key(venta.id) {
                ListRow(
                    title = venta.titulo.ifBlank { stringResource(R.string.inicio_venta_rapida) },
                    subtitle = formatHour(venta.hora),
                    iconRes = R.drawable.ic_plus,
                    iconContainerColor = GestorVenturesTheme.colors.successContainer,
                    iconColor = GestorVenturesTheme.colors.success,
                    trailing = {
                        MoneyText(monto = venta.monto, color = GestorVenturesTheme.colors.success)
                    },
                )
            }
        }
    }
}
