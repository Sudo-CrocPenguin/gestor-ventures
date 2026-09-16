package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorNegocio
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPasoTopBar
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvSelectableOption
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.ColorMarca
import com.gestor_ventures.front.theme.GestorVenturesTheme

/** El onboarding del mockup tiene dos pasos; el segundo (base financiera) es HU-06 a HU-09. */
private const val PasoActual = 1
private const val TotalPasos = 2

@Composable
fun RegistrarNegocioRoute(
    onBack: () -> Unit,
    onNegocioCreado: (Long) -> Unit,
    puedeVolver: Boolean,
    viewModel: RegistrarNegocioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.negocioCreado.collect { negocioId -> onNegocioCreado(negocioId) }
    }

    // La pantalla ya se ve con el color elegido: así el usuario sabe qué está escogiendo.
    GestorVenturesTheme(colorMarca = uiState.colorMarca.hex) {
        Surface(color = MaterialTheme.colorScheme.background) {
            RegistrarNegocioScreen(
                uiState = uiState,
                onBack = onBack,
                puedeVolver = puedeVolver,
                onNombreChange = viewModel::onNombreChange,
                onCategoriaChange = viewModel::onCategoriaChange,
                onTipoActividadChange = viewModel::onTipoActividadChange,
                onColorMarcaChange = viewModel::onColorMarcaChange,
                onGuardar = viewModel::guardar,
            )
        }
    }
}

/**
 * HU-05. Primer paso del onboarding: nombre del negocio, a qué se dedica y tipo de actividad,
 * que es lo que define qué módulos de agenda verá después.
 */
@Composable
fun RegistrarNegocioScreen(
    uiState: RegistrarNegocioUiState,
    onBack: () -> Unit,
    puedeVolver: Boolean = true,
    onNombreChange: (String) -> Unit,
    onCategoriaChange: (String) -> Unit,
    onTipoActividadChange: (TipoActividad) -> Unit,
    onColorMarcaChange: (ColorMarca) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // Si es el primer negocio no hay a dónde volver: la app no funciona sin él.
        GvPasoTopBar(
            paso = PasoActual,
            totalPasos = TotalPasos,
            onBack = onBack,
            mostrarVolver = puedeVolver,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Encabezado()

            // 1. Cómo se llama.
            GvTextField(
                label = stringResource(R.string.negocio_nombre),
                value = uiState.nombre,
                onValueChange = onNombreChange,
                placeholder = stringResource(R.string.negocio_nombre_placeholder),
            )

            // 2. A qué se dedica, con atajos para no tener que escribir.
            Column {
                GvTextField(
                    label = stringResource(R.string.negocio_categoria),
                    value = uiState.categoria,
                    onValueChange = onCategoriaChange,
                    placeholder = stringResource(R.string.negocio_categoria_placeholder),
                )
                Sugerencias(
                    categoriaActual = uiState.categoria,
                    onCategoriaChange = onCategoriaChange,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            // 3. Cómo trabaja: define los módulos de la agenda.
            TipoDeActividad(
                seleccionado = uiState.tipoActividad,
                onTipoActividadChange = onTipoActividadChange,
            )

            // 4. Al final, lo estético: cómo se va a ver la app.
            ColorDeMarca(
                seleccionado = uiState.colorMarca,
                onColorMarcaChange = onColorMarcaChange,
            )

            AnimatedVisibility(visible = uiState.error != null) {
                GvInfoNote(
                    text = uiState.error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.negocio_continuar),
                onClick = onGuardar,
                enabled = uiState.puedeGuardar,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun Encabezado(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.negocio_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.negocio_subtitulo),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun Sugerencias(
    categoriaActual: String,
    onCategoriaChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        EtiquetaSeccion(stringResource(R.string.negocio_sugerencias))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categoriasSugeridas.forEach { sugerenciaRes ->
                val texto = stringResource(sugerenciaRes)
                GvChip(
                    text = texto,
                    selected = texto.equals(categoriaActual.trim(), ignoreCase = true),
                    onClick = { onCategoriaChange(texto) },
                )
            }
        }
    }
}

@Composable
private fun TipoDeActividad(
    seleccionado: TipoActividad,
    onTipoActividadChange: (TipoActividad) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        EtiquetaSeccion(stringResource(R.string.negocio_tipo_actividad))
        Text(
            text = stringResource(R.string.negocio_tipo_actividad_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            tiposDeActividad.forEach { actividad ->
                GvSelectableOption(
                    titulo = stringResource(actividad.tituloRes),
                    descripcion = stringResource(actividad.descripcionRes),
                    iconRes = actividad.iconRes,
                    selected = actividad.tipo == seleccionado,
                    onClick = { onTipoActividadChange(actividad.tipo) },
                )
            }
        }
    }
}

@Composable
private fun ColorDeMarca(
    seleccionado: ColorMarca,
    onColorMarcaChange: (ColorMarca) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        EtiquetaSeccion(stringResource(R.string.negocio_color))
        Text(
            text = stringResource(R.string.negocio_color_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        ColorMarcaPicker(seleccionado = seleccionado, onColorChange = onColorMarcaChange)
    }
}

@Composable
private fun EtiquetaSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 1.1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Preview(name = "Claro", widthDp = 380, heightDp = 860)
@Preview(name = "Oscuro", widthDp = 380, heightDp = 860, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistrarNegocioScreenPreview() {
    VistaPrevia(
        RegistrarNegocioUiState(
            nombre = "Dulce Antojo",
            categoria = "Repostería",
            tipoActividad = TipoActividad.PRODUCTOS,
            colorMarca = ColorMarca.Menta,
        ),
    )
}

@Preview(name = "Con error", widthDp = 380, heightDp = 860)
@Composable
private fun RegistrarNegocioErrorPreview() {
    VistaPrevia(
        RegistrarNegocioUiState(
            nombre = "a".repeat(120),
            categoria = "Repostería",
            error = ErrorNegocio.NombreMuyLargo,
        ),
    )
}

@Composable
private fun VistaPrevia(uiState: RegistrarNegocioUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RegistrarNegocioScreen(
                uiState = uiState,
                onBack = {},
                onNombreChange = {},
                onCategoriaChange = {},
                onTipoActividadChange = {},
                onColorMarcaChange = {},
                onGuardar = {},
            )
        }
    }
}
