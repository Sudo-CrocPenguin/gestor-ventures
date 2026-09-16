package com.gestor_ventures.front.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.front.components.GvAvatar
import com.gestor_ventures.front.model.UsuarioUi

/** Cabecera del menú: avatar, nombre y correo de quien tiene la sesión abierta. */
@Composable
fun CabeceraUsuario(usuario: UsuarioUi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        GvAvatar(
            iniciales = usuario.iniciales,
            size = 50.dp,
            shape = RoundedCornerShape(14.dp),
        )
        Column {
            Text(
                text = usuario.nombre,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = usuario.correo,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
