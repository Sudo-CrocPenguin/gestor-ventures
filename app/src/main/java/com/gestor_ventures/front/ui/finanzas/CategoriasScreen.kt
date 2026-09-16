package com.gestor_ventures.front.ui.finanzas

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvSegmentedToggle
import com.gestor_ventures.front.components.GvSoftButton
import com.gestor_ventures.front.theme.GestorVenturesTheme

@Composable
fun CategoriasRoute(
    onBack: () -> Unit,
    viewModel: CategoriasViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CategoriasScreen(
        uiState = uiState,
        onBack = onBack,
        onTipoChange = viewModel::onTipoChange,
        onAgregar = viewModel::abrirFormularioNuevo,
        onEditar = viewModel::abrirFormularioDe,
        onNombreChange = viewModel::onNombreChange,
        onGuardarFormulario = viewModel::guardarFormulario,
        onCerrarFormulario = viewModel::cerrarFormulario,
        onPedirEliminar = viewModel::pedirEliminar,
        onConfirmarEliminar = viewModel::confirmarEliminar,
        onCancelarEliminar = viewModel::cancelarEliminar,
    )
}

/**
 * HU-15. Las categorías del negocio, en dos pestañas: gastos y costos.
 *
 * Tocar una categoría la abre para corregirle el nombre.
 */
@Composable
fun CategoriasScreen(
    uiState: CategoriasUiState,
    onBack: () -> Unit,
    onTipoChange: (TipoCategoria) -> Unit,
    onAgregar: () -> Unit,
    onEditar: (Categoria) -> Unit,
    onNombreChange: (String) -> Unit,
    onGuardarFormulario: () -> Unit,
    onCerrarFormulario: () -> Unit,
    onPedirEliminar: (Categoria) -> Unit,
    onConfirmarEliminar: () -> Unit,
    onCancelarEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.menu_categorias), onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GvSegmentedToggle(
                opciones = TipoCategoria.entries,
                seleccionada = uiState.tipo,
                etiqueta = { stringResource(it.labelRes()) },
                onSeleccionar = onTipoChange,
            )

            Text(
                text = stringResource(uiState.tipo.explicacionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.categorias_vacio))
            }

            uiState.categorias.forEach { categoria ->
                FilaCategoria(
                    categoria = categoria,
                    onEditar = { onEditar(categoria) },
                    onEliminar = { onPedirEliminar(categoria) },
                )
            }

            GvSoftButton(
                text = stringResource(R.string.categorias_agregar),
                onClick = onAgregar,
                iconRes = R.drawable.ic_plus,
            )
        }
    }

    uiState.formulario?.let { formulario ->
        CategoriaSheet(
            formulario = formulario,
            tipo = uiState.tipo,
            error = uiState.error,
            onNombreChange = onNombreChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )
    }

    uiState.porEliminar?.let { categoria ->
        ConfirmarEliminar(
            categoria = categoria,
            onConfirmar = onConfirmarEliminar,
            onCancelar = onCancelarEliminar,
        )
    }
}

@Composable
private fun FilaCategoria(
    categoria: Categoria,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GvCard(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditar),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = categoria.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEliminar) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = stringResource(
                        R.string.categoria_eliminar,
                        categoria.nombre,
                    ),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Borrar una categoría no borra lo que estaba clasificado con ella, pero eso no es evidente:
 * hay que decirlo antes de borrar, no después.
 */
@Composable
private fun ConfirmarEliminar(
    categoria: Categoria,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.categoria_eliminar_titulo, categoria.nombre)) },
        text = { Text(stringResource(R.string.categoria_eliminar_ayuda)) },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.categoria_eliminar_confirmar))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelar,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.cancelar))
            }
        },
    )
}

@Preview(name = "Categorías", widthDp = 380, heightDp = 720)
@Preview(
    name = "Categorías (oscuro)",
    widthDp = 380,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CategoriasScreenPreview() {
    VistaPrevia(
        CategoriasUiState(
            cargando = false,
            categorias = listOf(
                Categoria(1, "Arriendo", TipoCategoria.GASTO),
                Categoria(2, "Servicios públicos", TipoCategoria.GASTO),
                Categoria(3, "Transporte", TipoCategoria.GASTO),
            ),
        ),
    )
}

@Preview(name = "Sin categorías", widthDp = 380, heightDp = 720)
@Composable
private fun CategoriasVacioPreview() {
    VistaPrevia(CategoriasUiState(cargando = false, tipo = TipoCategoria.COSTO))
}

@Composable
private fun VistaPrevia(uiState: CategoriasUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CategoriasScreen(
                uiState = uiState,
                onBack = {},
                onTipoChange = {},
                onAgregar = {},
                onEditar = {},
                onNombreChange = {},
                onGuardarFormulario = {},
                onCerrarFormulario = {},
                onPedirEliminar = {},
                onConfirmarEliminar = {},
                onCancelarEliminar = {},
            )
        }
    }
}
