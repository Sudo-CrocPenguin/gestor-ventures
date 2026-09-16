package com.gestor_ventures.front.ui.negocio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.front.theme.ColorMarca
import com.gestor_ventures.front.theme.colorBaseDeMarca
import com.gestor_ventures.front.theme.hexAColor
import com.gestor_ventures.front.theme.tintaSobre

/**
 * HU-05. Paleta de colores de marca. Al elegir uno, la pantalla ya se ve con ese color: es la
 * forma más honesta de mostrar en qué se está metiendo el usuario.
 */
@Composable
fun ColorMarcaPicker(
    seleccionado: ColorMarca,
    onColorChange: (ColorMarca) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorMarca.entries.forEach { color ->
                Muestra(
                    color = color,
                    seleccionado = color == seleccionado,
                    onClick = { onColorChange(color) },
                )
            }
        }

        Text(
            text = nombreDelColor(seleccionado),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun nombreDelColor(color: ColorMarca): String {
    val nombre = stringResource(color.nombreRes)
    return if (color == ColorMarca.Sistema) {
        "$nombre · ${stringResource(R.string.negocio_color_predeterminado)}"
    } else {
        nombre
    }
}

@Composable
private fun Muestra(
    color: ColorMarca,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // El predeterminado no tiene hexadecimal: se pinta con el azul propio de la app, no con
    // el color que se esté previsualizando.
    val nombre = stringResource(color.nombreRes)
    val relleno = hexAColor(color.hex) ?: colorBaseDeMarca()
    val contorno = if (seleccionado) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(relleno)
            .border(if (seleccionado) 2.dp else 1.dp, contorno, CircleShape)
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = nombre },
        contentAlignment = Alignment.Center,
    ) {
        if (seleccionado) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.cd_color_elegido),
                tint = tintaSobre(relleno),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
