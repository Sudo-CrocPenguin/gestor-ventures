package com.gestor_ventures.front.ui.finanzas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import com.gestor_ventures.back.model.ErrorCategoria
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvTextField

/**
 * HU-15. Hoja para crear o renombrar una categoría. El tipo no se pregunta: es el de la
 * pestaña en la que está el usuario, y no se puede cambiar después porque movería de lado todo
 * lo que ya estaba clasificado.
 *
 * El error se muestra acá adentro y no en la pantalla: con la hoja abierta, un aviso detrás de
 * ella no lo ve nadie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaSheet(
    formulario: FormularioCategoria,
    tipo: TipoCategoria,
    error: ErrorCategoria?,
    onNombreChange: (String) -> Unit,
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(
                        if (formulario.esEdicion) {
                            R.string.categoria_titulo_editar
                        } else {
                            R.string.categoria_titulo_nueva
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(tipo.explicacionRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            GvTextField(
                label = stringResource(R.string.categoria_nombre),
                value = formulario.nombre,
                onValueChange = onNombreChange,
                placeholder = stringResource(R.string.categoria_nombre_placeholder),
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
                    if (formulario.esEdicion) {
                        R.string.categoria_guardar_cambios
                    } else {
                        R.string.categoria_guardar
                    },
                ),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}
