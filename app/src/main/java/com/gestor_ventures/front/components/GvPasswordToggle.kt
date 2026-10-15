package com.gestor_ventures.front.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R

/** El ojo que muestra u oculta una contraseña, como [trailing] de un [GvTextField]. */
@Composable
fun GvPasswordToggle(
    mostrar: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggle, modifier = modifier.size(24.dp)) {
        Icon(
            painter = painterResource(if (mostrar) R.drawable.ic_eye_off else R.drawable.ic_eye),
            contentDescription = stringResource(
                if (mostrar) R.string.cd_ocultar_contrasena else R.string.cd_mostrar_contrasena,
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
