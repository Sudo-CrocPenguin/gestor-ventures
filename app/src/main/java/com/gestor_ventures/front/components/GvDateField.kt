package com.gestor_ventures.front.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.front.util.formatLongDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val FieldShape = RoundedCornerShape(13.dp)

/**
 * Campo de fecha: se ve como los demás campos y al tocarlo abre el calendario del sistema.
 * Sirve para la meta de ahorro y para cualquier vencimiento que venga después.
 */
@Composable
fun GvDateField(
    label: String,
    fecha: LocalDate?,
    onFechaChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    var mostrandoCalendario by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, FieldShape)
                .clickable(role = Role.Button) { mostrandoCalendario = true }
                .padding(horizontal = 13.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = fecha?.let(::formatLongDate) ?: placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = if (fecha == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }

    if (mostrandoCalendario) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.aMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { mostrandoCalendario = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estado.selectedDateMillis?.aFechaLocal()?.let(onFechaChange)
                        mostrandoCalendario = false
                    },
                ) {
                    Text(stringResource(R.string.aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrandoCalendario = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        ) {
            DatePicker(state = estado)
        }
    }
}

/** El calendario de Material trabaja en UTC; la app, en la zona del teléfono. */
private fun LocalDate.aMillis(): Long =
    atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

private fun Long.aFechaLocal(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()
