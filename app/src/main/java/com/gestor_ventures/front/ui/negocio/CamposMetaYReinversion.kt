package com.gestor_ventures.front.ui.negocio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvDateField
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDate

/**
 * HU-08 y HU-09. Los campos de la meta de ahorro y del porcentaje de reinversión.
 *
 * Viven aparte porque los piden dos pantallas: el paso 2 del onboarding, donde se definen por
 * primera vez, y la configuración del negocio, donde se corrigen después. Son la misma pregunta
 * y tienen que verse y validarse igual en las dos.
 */
@Composable
fun CamposMetaAhorro(
    monto: String,
    montoFormateado: String,
    fechaLimite: LocalDate?,
    ahorroMensual: Double?,
    onMontoChange: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            GvTextField(
                label = stringResource(R.string.base_meta),
                value = montoFormateado,
                onValueChange = onMontoChange,
                placeholder = stringResource(R.string.form_monto_placeholder),
                keyboardType = KeyboardType.Number,
                textStyle = NumericTextStyle.copy(fontSize = 17.sp),
                leading = { TextoAuxiliar(stringResource(R.string.form_moneda_simbolo)) },
                trailing = { TextoAuxiliar(stringResource(R.string.form_moneda)) },
            )
            Ayuda(stringResource(R.string.base_meta_ayuda))
        }

        Column {
            GvDateField(
                label = stringResource(R.string.base_fecha_limite),
                fecha = fechaLimite,
                onFechaChange = onFechaChange,
                placeholder = stringResource(R.string.base_fecha_limite_placeholder),
            )
            Ayuda(stringResource(R.string.base_fecha_limite_ayuda))
        }

        // El cálculo solo aparece cuando hay con qué hacerlo: monto y fecha.
        AnimatedVisibility(visible = ahorroMensual != null && monto.isNotEmpty()) {
            GvInfoNote(
                stringResource(R.string.base_ahorro_mensual, formatPesos(ahorroMensual ?: 0.0)),
            )
        }
    }
}

/**
 * HU-09. El porcentaje de la ganancia que vuelve al negocio.
 *
 * Es un deslizador y no un campo de texto porque la pregunta es "¿más o menos cuánto?", no un
 * número exacto: nadie reinvierte el 37 % de su ganancia.
 */
@Composable
fun CampoReinversion(
    porcentaje: Int,
    onPorcentajeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.base_reinversion),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.base_reinversion_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.porcentaje, porcentaje),
                style = MaterialTheme.typography.titleLarge.merge(NumericTextStyle),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = porcentaje.toFloat(),
            onValueChange = { onPorcentajeChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}

/** Texto explicativo debajo de un campo. */
@Composable
internal fun Ayuda(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** "$" y "COP" que acompañan a un monto. */
@Composable
internal fun TextoAuxiliar(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
