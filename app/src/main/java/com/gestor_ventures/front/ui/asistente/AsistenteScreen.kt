package com.gestor_ventures.front.ui.asistente

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorAsistente
import com.gestor_ventures.back.model.MensajeAsistente
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatHour
import java.time.LocalDateTime
import com.gestor_ventures.front.components.tourTarget

@Composable
fun AsistenteRoute(
    onVerPromociones: () -> Unit,
    onVerHistorial: () -> Unit,
    conversacionElegida: Long?,
    onConversacionAbierta: () -> Unit,
    viewModel: AsistenteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(conversacionElegida) {
        conversacionElegida?.let {
            viewModel.abrirConversacion(it)
            onConversacionAbierta()
        }
    }

    AsistenteScreen(
        uiState = uiState,
        onPreguntaChange = viewModel::onPreguntaChange,
        onEnviar = viewModel::enviar,
        onSugerencia = viewModel::usarSugerencia,
        onNuevaConversacion = viewModel::nuevaConversacion,
        onVerPromociones = onVerPromociones,
        onVerHistorial = onVerHistorial,
    )
}

/**
 * HU-34. El chat con el asistente, siguiendo el mockup de la fase 6: encabezado con el avatar
 * del asistente y sus accesos, las respuestas a la izquierda con su avatar, las preguntas a la
 * derecha, la hora de cada mensaje y un campo redondeado para escribir. Sin conversación,
 * muestra preguntas de ejemplo.
 */
@Composable
fun AsistenteScreen(
    uiState: AsistenteUiState,
    onPreguntaChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onSugerencia: (String) -> Unit,
    onNuevaConversacion: () -> Unit,
    onVerPromociones: () -> Unit,
    onVerHistorial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Cada mensaje nuevo baja la lista hasta el final, como en cualquier chat.
    val lista = rememberLazyListState()
    val cantidad = uiState.mensajes.size * 2 + if (uiState.enviando) 2 else 0
    LaunchedEffect(cantidad) {
        if (cantidad > 0) lista.animateScrollToItem(cantidad - 1)
    }

    Column(modifier.fillMaxSize().imePadding()) {
        Encabezado(
            sinNegocio = uiState.sinNegocio,
            hayConversacion = !uiState.conversacionVacia,
            enviando = uiState.enviando,
            onVerPromociones = onVerPromociones,
            onVerHistorial = onVerHistorial,
            onNuevaConversacion = onNuevaConversacion,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Box(Modifier.weight(1f)) {
            if (uiState.conversacionVacia) {
                Bienvenida(
                    sinNegocio = uiState.sinNegocio,
                    onSugerencia = onSugerencia,
                )
            } else {
                LazyColumn(
                    state = lista,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.mensajes, key = { it.id }) { mensaje ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Burbuja(texto = mensaje.pregunta, hora = mensaje.fechaHora, delUsuario = true)
                            Burbuja(texto = mensaje.respuesta, hora = mensaje.fechaHora, delUsuario = false)
                        }
                    }
                    uiState.preguntaEnCurso?.let { pregunta ->
                        item(key = "en_curso") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Burbuja(texto = pregunta, hora = null, delUsuario = true)
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Text(
            text = stringResource(R.string.asistente_aviso),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        )
        Redactor(
            pregunta = uiState.pregunta,
            puedeEnviar = uiState.puedeEnviar,
            onPreguntaChange = onPreguntaChange,
            onEnviar = onEnviar,
        )
    }
}

/** El avatar del asistente: un cuadro redondeado con degradado y la chispa adentro. */
@Composable
private fun AvatarAsistente(tamano: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.32f))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, GestorVenturesTheme.colors.primaryVariant),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_spark),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(tamano * 0.56f),
        )
    }
}

@Composable
private fun Encabezado(
    sinNegocio: Boolean,
    hayConversacion: Boolean,
    enviando: Boolean,
    onVerPromociones: () -> Unit,
    onVerHistorial: () -> Unit,
    onNuevaConversacion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AvatarAsistente(tamano = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.destino_asistente),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.asistente_estado),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.tourTarget("asistente_acciones")) {
            if (hayConversacion) {
                BotonEncabezado(
                    iconRes = R.drawable.ic_plus,
                    descripcion = stringResource(R.string.asistente_nueva_conversacion),
                    habilitado = !enviando,
                    onClick = onNuevaConversacion,
                )
            }
            BotonEncabezado(
                iconRes = R.drawable.ic_lightbulb,
                descripcion = stringResource(R.string.asistente_promociones),
                habilitado = !sinNegocio,
                onClick = onVerPromociones,
            )
            BotonEncabezado(
                iconRes = R.drawable.ic_history,
                descripcion = stringResource(R.string.asistente_historial),
                habilitado = !sinNegocio,
                onClick = onVerHistorial,
            )
        }
    }
}

@Composable
private fun BotonEncabezado(
    iconRes: Int,
    descripcion: String,
    habilitado: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = habilitado) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = descripcion,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(if (habilitado) 1f else 0.4f),
        )
    }
}

@Composable
private fun Bienvenida(
    sinNegocio: Boolean,
    onSugerencia: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AvatarAsistente(tamano = 64.dp)
        Text(
            text = stringResource(R.string.asistente_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.asistente_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        if (sinNegocio) {
            GvInfoNote(stringResource(R.string.asistente_sin_negocio))
            return@Column
        }
        Column(
            modifier = Modifier.padding(top = 10.dp).widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            preguntasSugeridas.forEach { sugerenciaRes ->
                val texto = stringResource(sugerenciaRes)
                Sugerencia(texto = texto, onClick = { onSugerencia(texto) })
            }
        }
    }
}

/** Una pregunta de ejemplo: tocarla la manda directo al asistente. */
@Composable
private fun Sugerencia(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_spark),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Un mensaje del chat. La respuesta lleva el avatar del asistente y la esquina inferior
 * izquierda recta, la pregunta la esquina inferior derecha: así se lee de quién es cada uno
 * sin tener que mirar el color.
 */
@Composable
private fun Burbuja(
    texto: String,
    hora: LocalDateTime?,
    delUsuario: Boolean,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (delUsuario) 16.dp else 4.dp,
        bottomEnd = if (delUsuario) 4.dp else 16.dp,
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (delUsuario) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!delUsuario) AvatarAsistente(tamano = 28.dp)
            Text(
                text = conNegritas(texto),
                style = MaterialTheme.typography.bodyMedium,
                color = if (delUsuario) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .then(
                        if (delUsuario) {
                            Modifier.background(MaterialTheme.colorScheme.primary, forma)
                        } else {
                            Modifier
                                .background(MaterialTheme.colorScheme.surface, forma)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), forma)
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        if (hora != null) {
            Text(
                text = formatHour(hora.toLocalTime()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = if (delUsuario) 0.dp else 36.dp),
            )
        }
    }
}

/** El asistente "escribiendo": tres puntos que suben y bajan, en vez de una ruedita. */
@Composable
private fun Pensando(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "puntos")
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AvatarAsistente(tamano = 28.dp)
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { indice ->
                val opacidad by transicion.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1100
                            0.3f at 0
                            1f at 330
                            0.3f at 660
                        },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = androidx.compose.animation.core.StartOffset(indice * 180),
                    ),
                    label = "punto$indice",
                )
                Box(
                    Modifier
                        .size(7.dp)
                        .alpha(opacidad)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                )
            }
        }
        Text(
            text = stringResource(R.string.asistente_pensando),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

/** El campo para escribir: una píldora con el botón de enviar a un lado. */
@Composable
private fun Redactor(
    pregunta: String,
    puedeEnviar: Boolean,
    onPreguntaChange: (String) -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)
                .tourTarget("asistente_redactor"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                BasicTextField(
                    value = pregunta,
                    onValueChange = onPreguntaChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (puedeEnviar) onEnviar() }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { campo ->
                        if (pregunta.isEmpty()) {
                            Text(
                                text = stringResource(R.string.asistente_pregunta_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        campo()
                    },
                )
            }
            IconButton(
                onClick = onEnviar,
                enabled = puedeEnviar,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (puedeEnviar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = stringResource(R.string.asistente_enviar),
                    tint = if (puedeEnviar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
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
                onVerHistorial = {},
            )
        }
    }
}
