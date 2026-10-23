package com.gestor_ventures.front.ui.asistente

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorAsistente
import com.gestor_ventures.back.model.MensajeAsistente
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvTextField
import com.gestor_ventures.front.theme.GestorVenturesTheme
import java.time.LocalDateTime

@Composable
fun AsistenteRoute(onVerPromociones: () -> Unit, viewModel: AsistenteViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AsistenteScreen(
        uiState = uiState,
        onPreguntaChange = viewModel::onPreguntaChange,
        onEnviar = viewModel::enviar,
        onSugerencia = viewModel::usarSugerencia,
        onNuevaConversacion = viewModel::nuevaConversacion,
        onVerPromociones = onVerPromociones,
    )
}

/**
 * HU-34. El chat con el asistente: las preguntas a la derecha, las respuestas a la izquierda y
 * el campo para escribir abajo. Sin conversación, muestra preguntas de ejemplo.
 */
@Composable
fun AsistenteScreen(
    uiState: AsistenteUiState,
    onPreguntaChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onSugerencia: (String) -> Unit,
    onNuevaConversacion: () -> Unit,
    onVerPromociones: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Cada mensaje nuevo baja la lista hasta el final, como en cualquier chat.
    val lista = rememberLazyListState()
    val cantidad = uiState.mensajes.size * 2 + if (uiState.enviando) 2 else 0
    LaunchedEffect(cantidad) {
        if (cantidad > 0) lista.animateScrollToItem(cantidad - 1)
    }

    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onVerPromociones, enabled = !uiState.sinNegocio) {
                Text(stringResource(R.string.asistente_promociones))
            }
            if (!uiState.conversacionVacia) {
                TextButton(onClick = onNuevaConversacion, enabled = !uiState.enviando) {
                    Text(stringResource(R.string.asistente_nueva_conversacion))
                }
            }
        }

        Box(Modifier.weight(1f)) {
            if (uiState.conversacionVacia) {
                Bienvenida(
                    sinNegocio = uiState.sinNegocio,
                    onSugerencia = onSugerencia,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    state = lista,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.mensajes, key = { it.id }) { mensaje ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Burbuja(texto = mensaje.pregunta, delUsuario = true)
                            Burbuja(texto = mensaje.respuesta, delUsuario = false)
                        }
                    }
                    uiState.preguntaEnCurso?.let { pregunta ->
                        item(key = "en_curso") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Burbuja(texto = pregunta, delUsuario = true)
                                Pensando()
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = uiState.error != null) {
            GvInfoNote(
                text = uiState.error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GvTextField(
                    label = stringResource(R.string.asistente_pregunta),
                    value = uiState.pregunta,
                    onValueChange = onPreguntaChange,
                    placeholder = stringResource(R.string.asistente_pregunta_placeholder),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onEnviar,
                    enabled = uiState.puedeEnviar,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (uiState.puedeEnviar) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(13.dp),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = stringResource(R.string.asistente_enviar),
                        tint = if (uiState.puedeEnviar) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp).rotate(180f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Bienvenida(
    sinNegocio: Boolean,
    onSugerencia: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.asistente_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.asistente_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (sinNegocio) {
            GvInfoNote(stringResource(R.string.asistente_sin_negocio))
            return@Column
        }
        preguntasSugeridas.forEach { sugerenciaRes ->
            val texto = stringResource(sugerenciaRes)
            GvChip(text = texto, selected = false, onClick = { onSugerencia(texto) })
        }
    }
}

@Composable
private fun Burbuja(texto: String, delUsuario: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (delUsuario) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = conNegritas(texto),
            style = MaterialTheme.typography.bodyMedium,
            color = if (delUsuario) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (delUsuario) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun Pensando(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.asistente_pensando),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Gemini escribe con formato de Markdown: **negritas** y viñetas con asterisco. Acá se
 * convierten en negritas de verdad y en "•", para que no se vean los asteriscos.
 */
internal fun conNegritas(texto: String): AnnotatedString = buildAnnotatedString {
    texto.replace(Regex("(?m)^\\s*[*-]\\s+"), "• ")
        .split("**")
        .forEachIndexed { indice, trozo ->
            if (indice % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(trozo) }
            else append(trozo)
        }
}

@Preview(name = "Conversación", widthDp = 380, heightDp = 760)
@Preview(name = "Conversación (oscuro)", widthDp = 380, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsistenteConversacionPreview() {
    VistaPrevia(
        AsistenteUiState(
            mensajes = listOf(
                MensajeAsistente(
                    id = 1,
                    sesionId = 1,
                    pregunta = "¿Cómo me fue esta semana?",
                    respuesta = "Esta semana llevas **$ 1.240.000** en 38 ventas, un 12 % más que la anterior. " +
                        "El sábado fue tu mejor día.",
                    fechaHora = LocalDateTime.of(2026, 10, 22, 10, 0),
                ),
            ),
            preguntaEnCurso = "¿Y el mes pasado?",
        ),
    )
}

@Preview(name = "Bienvenida", widthDp = 380, heightDp = 760)
@Composable
private fun AsistenteBienvenidaPreview() {
    VistaPrevia(AsistenteUiState(error = ErrorAsistente.SinConexion))
}

@Composable
private fun VistaPrevia(uiState: AsistenteUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AsistenteScreen(
                uiState = uiState,
                onPreguntaChange = {},
                onEnviar = {},
                onSugerencia = {},
                onNuevaConversacion = {},
                onVerPromociones = {},
            )
        }
    }
}
