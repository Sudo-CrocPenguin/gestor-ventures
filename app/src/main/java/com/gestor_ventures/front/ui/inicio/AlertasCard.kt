package com.gestor_ventures.front.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Alerta
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvTextLink
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.model.iconRes
import com.gestor_ventures.front.model.tituloRes

/** Cuántos avisos caben en el inicio antes de que deje de ser un resumen. */
private const val MaxAlertasEnInicio = 3

/**
 * HU-38. Los avisos del negocio, en el inicio.
 *
 * Están acá y no solo en notificaciones porque un aviso que hay que ir a buscar no avisa nada:
 * el inicio es la primera pantalla y es donde se mira qué pasó.
 *
 * Se muestran los más recientes y se deja el enlace para los demás. El punto rojo marca los
 * que no se han leído, igual que en la pantalla de notificaciones.
 */
@Composable
fun AlertasCard(
    alertas: List<Alerta>,
    onVerTodas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.alertas_titulo)) {
            if (alertas.size > MaxAlertasEnInicio) {
                GvTextLink(text = stringResource(R.string.inicio_ver_todas), onClick = onVerTodas)
            }
        }

        alertas.take(MaxAlertasEnInicio).forEach { alerta ->
            key(alerta.id) {
                ListRow(
                    title = stringResource(alerta.tipo.tituloRes()),
                    subtitle = alerta.descripcion,
                    subtitleMaxLines = 3,
                    iconRes = alerta.tipo.iconRes(),
                    trailing = if (alerta.leida) null else {
                        { PuntoSinLeer() }
                    },
                )
            }
        }
    }
}

/** El punto que marca un aviso sin leer. */
@Composable
private fun PuntoSinLeer() {
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .size(9.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
    )
}
