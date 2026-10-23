package com.gestor_ventures.front.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatPesos

/** Monto en pesos con el estilo numérico del sistema de diseño ("$ 148.500"). */
@Composable
fun MoneyText(
    monto: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = formatPesos(monto),
        modifier = modifier,
        style = style.merge(NumericTextStyle),
        color = color,
        maxLines = 1,
    )
}
