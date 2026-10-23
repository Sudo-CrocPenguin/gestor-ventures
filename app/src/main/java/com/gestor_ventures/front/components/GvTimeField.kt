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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import com.gestor_ventures.front.util.formatHour
import java.time.LocalTime

private val FieldShape = RoundedCornerShape(13.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GvTimeField(
    label: String,
    hora: LocalTime?,
    onHoraChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    var mostrandoReloj by remember { mutableStateOf(false) }

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
                .clickable(role = Role.Button) { mostrandoReloj = true }
                .padding(horizontal = 13.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = hora?.let(::formatHour) ?: placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = if (hora == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }

    if (mostrandoReloj) {
        val estado = rememberTimePickerState(
            initialHour = hora?.hour ?: 9,
            initialMinute = hora?.minute ?: 0,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { mostrandoReloj = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHoraChange(LocalTime.of(estado.hour, estado.minute))
                        mostrandoReloj = false
                    },
                ) {
                    Text(stringResource(R.string.aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrandoReloj = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
            text = { TimePicker(state = estado) },
        )
    }
}
