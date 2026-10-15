package com.gestor_ventures.front.ui.negocio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvColorPickerDialog
import com.gestor_ventures.front.theme.ColorMarca
import com.gestor_ventures.front.theme.colorAHex
import com.gestor_ventures.front.theme.colorBaseDeMarca
import com.gestor_ventures.front.theme.hexAColor
import com.gestor_ventures.front.theme.presetDeHex
import com.gestor_ventures.front.theme.tintaSobre

private val TamanoMuestra = 44.dp

/**
 * HU-05. Color de marca: los tres atajos de un toque y el círculo para elegir el color exacto.
 * Al elegir uno, la pantalla ya se ve con ese color: es la forma más honesta de mostrar en qué
 * se está metiendo el usuario.
 *
 * Lo que sube y baja es el hexadecimal, no la opción de la paleta: así un color personalizado
 * viaja igual que uno de los atajos.
 */
@Composable
fun ColorMarcaPicker(
    hexSeleccionado: String?,
    onColorChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrandoSelector by rememberSaveable { mutableStateOf(false) }
    val preset = presetDeHex(hexSeleccionado)

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorMarca.entries.forEach { color ->
                Muestra(
                    color = color,
                    seleccionado = color == preset,
                    onClick = { onColorChange(color.hex) },
                )
            }
            MuestraPersonalizada(
                hexPersonalizado = hexSeleccionado.takeIf { preset == null },
                onClick = { mostrandoSelector = true },
            )
        }

        Text(
            text = nombreDelColor(preset, hexSeleccionado),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    if (mostrandoSelector) {
        GvColorPickerDialog(
            colorInicial = hexAColor(hexSeleccionado) ?: colorBaseDeMarca(),
            onDismiss = { mostrandoSelector = false },
            onConfirm = { elegido ->
                mostrandoSelector = false
                onColorChange(colorAHex(elegido))
            },
        )
    }
}

@Composable
private fun nombreDelColor(preset: ColorMarca?, hex: String?): String = when {
    preset == ColorMarca.Sistema ->
        "${stringResource(preset.nombreRes)} · ${stringResource(R.string.negocio_color_predeterminado)}"

    preset != null -> stringResource(preset.nombreRes)
    else -> "${stringResource(R.string.color_personalizado)} · ${hex.orEmpty()}"
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

    Circulo(
        relleno = relleno,
        seleccionado = seleccionado,
        modifier = modifier
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = nombre },
    )
}

/**
 * El círculo del final: si el usuario ya eligió un color propio lo muestra, y si no, enseña el
 * arcoíris que invita a abrir el selector.
 */
@Composable
private fun MuestraPersonalizada(
    hexPersonalizado: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val personalizado = hexAColor(hexPersonalizado)
    val etiqueta = stringResource(R.string.color_personalizado)

    if (personalizado != null) {
        Circulo(
            relleno = personalizado,
            seleccionado = true,
            modifier = modifier
                .clickable(onClick = onClick)
                .semantics { contentDescription = etiqueta },
        )
        return
    }

    val arcoiris = remember {
        Brush.sweepGradient(
            (0..6).map { Color.hsl(it * 60f % 360f, 0.75f, 0.62f) },
        )
    }
    Box(
        modifier = modifier
            .size(TamanoMuestra)
            .clip(CircleShape)
            .background(arcoiris)
            .clickable(onClick = onClick)
            .semantics { contentDescription = etiqueta },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(TamanoMuestra - 12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun Circulo(
    relleno: Color,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
) {
    val contorno = if (seleccionado) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(TamanoMuestra)
            .clip(CircleShape)
            .background(relleno)
            .border(if (seleccionado) 2.dp else 1.dp, contorno, CircleShape),
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
