package com.gestor_ventures.front.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.SeleccionDePeriodo
import com.gestor_ventures.front.model.FormularioRango
import com.gestor_ventures.front.model.labelRes
import java.time.LocalDate

/**
 * HU-17 y HU-23. Los periodos de un toque más el rango escrito a mano.
 *
 * Lo usan el historial de movimientos y el de cierres de caja. Si cada uno tuviera el suyo, la
 * misma pregunta —"¿de qué periodo?"— se respondería distinto en dos pantallas de la misma app.
 */
@Composable
fun GvChipsDePeriodo(
    seleccion: SeleccionDePeriodo,
    onRapido: (PeriodoPredefinido) -> Unit,
    onPersonalizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeriodoPredefinido.entries.forEach { periodo ->
            GvChip(
                text = stringResource(periodo.labelRes()),
                selected = seleccion == SeleccionDePeriodo.Rapido(periodo),
                onClick = { onRapido(periodo) },
            )
        }

        GvChip(
            text = stringResource(R.string.periodo_personalizado),
            selected = seleccion is SeleccionDePeriodo.Personalizado,
            onClick = onPersonalizar,
        )
    }
}

/**
 * HU-17. Hoja para escribir un rango de fechas.
 *
 * El botón de aplicar queda apagado mientras el rango no sirva, y el porqué se dice debajo: un
 * botón muerto sin explicación es lo que hace que la gente crea que la app se trabó.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GvRangoSheet(
    formulario: FormularioRango,
    onDesdeChange: (LocalDate) -> Unit,
    onHastaChange: (LocalDate) -> Unit,
    onAplicar: () -> Unit,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.periodo_rango_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            GvDateField(
                label = stringResource(R.string.periodo_desde),
                fecha = formulario.desde,
                onFechaChange = onDesdeChange,
                placeholder = stringResource(R.string.periodo_fecha_placeholder),
            )

            GvDateField(
                label = stringResource(R.string.periodo_hasta),
                fecha = formulario.hasta,
                onFechaChange = onHastaChange,
                placeholder = stringResource(R.string.periodo_fecha_placeholder),
            )

            AnimatedVisibility(visible = formulario.invertido) {
                GvInfoNote(
                    text = stringResource(R.string.periodo_error_invertido),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            GvPrimaryButton(
                text = stringResource(R.string.periodo_aplicar),
                onClick = onAplicar,
                enabled = formulario.valido,
            )
        }
    }
}
