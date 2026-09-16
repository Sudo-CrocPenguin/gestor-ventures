package com.gestor_ventures.front.ui.finanzas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.ErrorGasto
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvDateField
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.NumericTextStyle
import java.time.LocalDate

/**
 * HU-14. Hoja para registrar o corregir un gasto: qué se pagó, cuánto, cuándo y en qué
 * categoría entra.
 *
 * La categoría es opcional a propósito: un gasto sin clasificar es mejor que un gasto sin
 * registrar. Tocar de nuevo la que ya está elegida la quita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoSheet(
    formulario: FormularioGasto,
    categorias: List<Categoria>,
    error: ErrorGasto?,
    onDescripcionChange: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    onCategoriaChange: (Long?) -> Unit,
    onGuardar: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (formulario.esEdicion) R.string.gasto_titulo_editar else R.string.gasto_titulo_nuevo,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            GvTextField(
                label = stringResource(R.string.gasto_descripcion),
                value = formulario.descripcion,
                onValueChange = onDescripcionChange,
                placeholder = stringResource(R.string.gasto_descripcion_placeholder),
            )

            GvTextField(
                label = stringResource(R.string.form_monto),
                value = formulario.montoFormateado,
                onValueChange = onMontoChange,
                placeholder = stringResource(R.string.form_monto_placeholder),
                keyboardType = KeyboardType.Number,
                textStyle = NumericTextStyle.copy(fontSize = 17.sp),
                leading = { TextoAuxiliarGasto(stringResource(R.string.form_moneda_simbolo)) },
                trailing = { TextoAuxiliarGasto(stringResource(R.string.form_moneda)) },
            )

            GvDateField(
                label = stringResource(R.string.gasto_fecha),
                fecha = formulario.fecha,
                onFechaChange = onFechaChange,
            )

            Categorias(
                categorias = categorias,
                seleccionada = formulario.categoriaId,
                onCategoriaChange = onCategoriaChange,
            )

            AnimatedVisibility(visible = error != null) {
                GvInfoNote(
                    text = error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            GvPrimaryButton(
                text = stringResource(
                    if (formulario.esEdicion) R.string.gasto_guardar_cambios else R.string.gasto_guardar,
                ),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}

@Composable
private fun Categorias(
    categorias: List<Categoria>,
    seleccionada: Long?,
    onCategoriaChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.gasto_categoria),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (categorias.isEmpty()) {
            // Sin categorías no hay nada que elegir, pero tampoco es un error: se dice dónde
            // se crean y el gasto se puede guardar igual.
            GvInfoNote(stringResource(R.string.gasto_sin_categorias))
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categorias.forEach { categoria ->
                GvChip(
                    text = categoria.nombre,
                    selected = categoria.id == seleccionada,
                    onClick = { onCategoriaChange(categoria.id) },
                )
            }
        }
    }
}

/** "$" y "COP" que acompañan al monto. */
@Composable
private fun TextoAuxiliarGasto(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
