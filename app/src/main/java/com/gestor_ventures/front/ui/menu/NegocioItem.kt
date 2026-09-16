package com.gestor_ventures.front.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.front.components.GvAvatar
import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.RolNegocio

/**
 * Fila de un negocio en el menú. El [activo] se distingue por el fondo resaltado; la etiqueta
 * de rol cierra la fila a la derecha.
 */
@Composable
fun NegocioItem(
    negocio: NegocioUi,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 11.dp, vertical = 2.dp)
            .clip(MenuItemShape)
            .background(if (activo) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .selectable(selected = activo, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GvAvatar(iniciales = negocio.iniciales)
        Text(
            text = negocio.nombre,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        EtiquetaRol(negocio.rol)
    }
}

/** Líder va en sólido y Vendedor en contorno, como en el mockup. */
@Composable
private fun EtiquetaRol(rol: RolNegocio, modifier: Modifier = Modifier) {
    val esLider = rol == RolNegocio.Lider
    val primary = MaterialTheme.colorScheme.primary
    Text(
        text = stringResource(rol.labelRes),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = if (esLider) MaterialTheme.colorScheme.onPrimary else primary,
        modifier = modifier
            .background(if (esLider) primary else Color.Transparent, CircleShape)
            .then(if (esLider) Modifier else Modifier.border(1.5.dp, primary, CircleShape))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
