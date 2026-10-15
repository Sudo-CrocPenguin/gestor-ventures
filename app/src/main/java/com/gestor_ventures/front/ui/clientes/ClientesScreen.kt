package com.gestor_ventures.front.ui.clientes

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.front.components.AccionSheet
import com.gestor_ventures.front.components.GvAccionesSheet
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.ListRow
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.aIniciales

@Composable
fun ClientesRoute(viewModel: ClientesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientesScreen(
        uiState = uiState,
        onAgregar = viewModel::abrirFormularioNuevo,
        onAbrirAcciones = viewModel::abrirAcciones,
        onEditar = viewModel::editarElElegido,
        onEliminar = viewModel::eliminarElElegido,
        onCerrarAcciones = viewModel::cerrarAcciones,
        onNombreChange = viewModel::onNombreChange,
        onTelefonoChange = viewModel::onTelefonoChange,
        onCorreoChange = viewModel::onCorreoChange,
        onNotasChange = viewModel::onNotasChange,
        onGuardarFormulario = viewModel::guardarFormulario,
        onCerrarFormulario = viewModel::cerrarFormulario,
    )
}

/**
 * HU-29. A quién le vende el negocio.
 *
 * La fila muestra el contacto que haya —teléfono si lo dio, si no el correo— porque eso es lo
 * que sirve para escribirle. Un cliente sin contacto lo dice, en vez de dejar el renglón vacío.
 */
@Composable
fun ClientesScreen(
    uiState: ClientesUiState,
    onAgregar: () -> Unit,
    onAbrirAcciones: (Cliente) -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onCerrarAcciones: () -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onNotasChange: (String) -> Unit,
    onGuardarFormulario: () -> Unit,
    onCerrarFormulario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(visible = uiState.vacio) {
                GvInfoNote(stringResource(R.string.clientes_vacio))
            }

            uiState.clientes.forEach { cliente ->
                FilaCliente(cliente = cliente, onClick = { onAbrirAcciones(cliente) })
            }
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.clientes_agregar),
                onClick = onAgregar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    uiState.acciones?.let { cliente ->
        GvAccionesSheet(
            titulo = cliente.nombre,
            acciones = listOf(
                AccionSheet(
                    texto = stringResource(R.string.cliente_accion_editar),
                    iconRes = R.drawable.ic_pencil,
                    onClick = onEditar,
                ),
                AccionSheet(
                    texto = stringResource(R.string.cliente_accion_eliminar),
                    iconRes = R.drawable.ic_trash,
                    destructiva = true,
                    onClick = onEliminar,
                ),
            ),
            onCerrar = onCerrarAcciones,
        )
    }

    uiState.formulario?.let { formulario ->
        ClienteSheet(
            formulario = formulario,
            error = uiState.error,
            onNombreChange = onNombreChange,
            onTelefonoChange = onTelefonoChange,
            onCorreoChange = onCorreoChange,
            onNotasChange = onNotasChange,
            onGuardar = onGuardarFormulario,
            onCerrar = onCerrarFormulario,
        )
    }
}

@Composable
private fun FilaCliente(cliente: Cliente, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val etiqueta = stringResource(R.string.cliente_acciones_de, cliente.nombre)

    GvCard(modifier) {
        ListRow(
            title = cliente.nombre,
            subtitle = contactoDe(cliente),
            iconRes = R.drawable.ic_user,
            modifier = Modifier
                .clickable(onClick = onClick)
                .semantics { contentDescription = etiqueta },
            trailing = {
                Text(
                    text = cliente.nombre.aIniciales(),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = GestorVenturesTheme.colors.acento,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}

/** El teléfono si lo dio, si no el correo, y si no lo dice: es lo que sirve para escribirle. */
@Composable
private fun contactoDe(cliente: Cliente): String =
    cliente.telefono ?: cliente.correo ?: stringResource(R.string.cliente_sin_contacto)

@Preview(name = "Clientes", widthDp = 380, heightDp = 780)
@Preview(
    name = "Clientes (oscuro)",
    widthDp = 380,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ClientesScreenPreview() {
    VistaPreviaClientes(
        ClientesUiState(
            cargando = false,
            clientes = listOf(
                Cliente(1, "Laura Gómez", telefono = "3001112233", notas = "Prefiere sin azúcar"),
                Cliente(2, "Andrés Ruiz", correo = "andres@correo.com"),
                Cliente(3, "Camila Torres"),
            ),
        ),
    )
}

@Preview(name = "Sin clientes", widthDp = 380, heightDp = 780)
@Composable
private fun ClientesVacioPreview() {
    VistaPreviaClientes(ClientesUiState(cargando = false))
}

@Composable
private fun VistaPreviaClientes(uiState: ClientesUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ClientesScreen(
                uiState = uiState,
                onAgregar = {},
                onAbrirAcciones = {},
                onEditar = {},
                onEliminar = {},
                onCerrarAcciones = {},
                onNombreChange = {},
                onTelefonoChange = {},
                onCorreoChange = {},
                onNotasChange = {},
                onGuardarFormulario = {},
                onCerrarFormulario = {},
            )
        }
    }
}
