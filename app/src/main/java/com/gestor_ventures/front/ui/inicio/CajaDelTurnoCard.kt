package com.gestor_ventures.front.ui.inicio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatPesos

/**
 * HU-19 y HU-20. La caja del turno con su saldo esperado.
 *
 * Es la misma cifra que muestra la pantalla de caja, y sale del mismo cálculo: si acá se
 * sumara por otro lado, tarde o temprano las dos pantallas dirían números distintos.
 */
@Composable
fun CajaDelTurnoCard(
    caja: CajaAbiertaUi?,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.inicio_caja_del_turno)) {
            if (caja != null) {
                StatusPill(
                    text = stringResource(R.string.caja_estado_abierta),
                    containerColor = GestorVenturesTheme.colors.successContainer,
                    contentColor = GestorVenturesTheme.colors.success,
                )
            }
        }

        if (caja == null) {
            Text(
                text = stringResource(R.string.inicio_sin_cajas_abiertas),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ListRow(
                title = stringResource(R.string.caja_saldo_esperado),
                subtitle = stringResource(
                    R.string.inicio_caja_apertura,
                    formatHour(caja.horaApertura),
                    formatPesos(caja.montoInicial),
                ),
                iconRes = R.drawable.ic_box,
                trailing = { MoneyText(monto = caja.saldoEsperado) },
            )
        }
    }
}
