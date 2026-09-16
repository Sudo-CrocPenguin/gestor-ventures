package com.gestor_ventures.front.ui.inicio

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.components.MoneyText
import com.gestor_ventures.front.components.StatusPill
import com.gestor_ventures.front.util.formatHour
import com.gestor_ventures.front.util.formatPesos

/** HU-19/HU-20. Cajas abiertas del equipo con su saldo esperado. */
@Composable
fun CajasActivasCard(
    cajas: List<CajaActivaUi>,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.inicio_cajas_activas)) {
            if (cajas.isNotEmpty()) {
                StatusPill(text = pluralStringResource(R.plurals.inicio_cajas_abiertas, cajas.size, cajas.size))
            }
        }
        if (cajas.isEmpty()) {
            Text(
                text = stringResource(R.string.inicio_sin_cajas_abiertas),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        cajas.forEachIndexed { index, caja ->
            key(caja.cajaId) {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                CajaActivaRow(caja)
            }
        }
    }
}

@Composable
private fun CajaActivaRow(caja: CajaActivaUi) {
    ListRow(
        title = stringResource(R.string.inicio_caja_responsable, caja.responsable, caja.turno),
        subtitle = stringResource(
            R.string.inicio_caja_apertura,
            formatHour(caja.horaApertura),
            formatPesos(caja.montoInicial),
        ),
        iconRes = R.drawable.ic_user,
        trailing = { MoneyText(monto = caja.saldoEsperado) },
    )
}
