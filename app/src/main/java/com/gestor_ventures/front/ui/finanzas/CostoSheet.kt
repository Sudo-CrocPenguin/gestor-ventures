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
import com.gestor_ventures.back.model.ErrorCosto
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvDateField
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.NumericTextStyle
import java.time.LocalDate

/**
 * HU-13. Hoja para registrar o corregir un costo: qué producto, cuánto costó, cuándo y en qué
 * categoría entra.
 *
 * El nombre del producto tiene que coincidir con el de la venta para que el margen cuadre; por
 * eso la hoja lo dice en vez de dejar que el usuario lo descubra cuando el margen le salga mal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostoSheet(
    formulario: FormularioCosto,
    categorias: List<Categoria>,
    error: ErrorCosto?,
    onProductoChange: (String) -> Unit,
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
            Column {
                Text(
                    text = stringResource(
                        if (formulario.esEdicion) R.string.costo_titulo_editar else R.string.costo_titulo_nuevo,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.costo_explicacion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            GvTextField(
                label = stringResource(R.string.costo_producto),
                value = formulario.productoServicio,
                onValueChange = onProductoChange,
                placeholder = stringResource(R.string.costo_producto_placeholder),
            )

            GvTextField(
                label = stringResource(R.string.costo_monto),
                value = formulario.montoFormateado,
                onValueChange = onMontoChange,
                placeholder = stringResource(R.string.form_monto_placeholder),
                keyboardType = KeyboardType.Number,
                textStyle = NumericTextStyle.copy(fontSize = 17.sp),
                leading = { TextoAuxiliarCosto(stringResource(R.string.form_moneda_simbolo)) },
                trailing = { TextoAuxiliarCosto(stringResource(R.string.form_moneda)) },
            )

            GvDateField(
                label = stringResource(R.string.costo_fecha),
                fecha = formulario.fecha.toLocalDate(),
                onFechaChange = onFechaChange,
            )

            CategoriasDeCosto(
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
                    if (formulario.esEdicion) R.string.costo_guardar_cambios else R.string.costo_guardar,
                ),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}

@Composable
private fun CategoriasDeCosto(
    categorias: List<Categoria>,
    seleccionada: Long?,
    onCategoriaChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.costo_categoria),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (categorias.isEmpty()) {
            GvInfoNote(stringResource(R.string.costo_sin_categorias))
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
private fun TextoAuxiliarCosto(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
