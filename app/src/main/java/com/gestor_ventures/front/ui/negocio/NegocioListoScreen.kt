package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gestor_ventures.R
import com.gestor_ventures.back.model.MetaAhorro
import com.gestor_ventures.back.model.Negocio
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatMesYAnio
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun NegocioListoRoute(
    onIrAlNegocio: () -> Unit,
    viewModel: NegocioListoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NegocioListoScreen(uiState = uiState, onIrAlNegocio = onIrAlNegocio)
}

/** HU-05. Cierre del onboarding: confirma que el negocio quedó listo y resume su configuración. */
@Composable
fun NegocioListoScreen(
    uiState: NegocioListoUiState,
    onIrAlNegocio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val negocio = uiState.negocio

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(GestorVenturesTheme.colors.successContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = GestorVenturesTheme.colors.success,
                    modifier = Modifier.size(46.dp),
                )
            }

            Text(
                text = stringResource(R.string.listo_titulo),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 22.dp),
            )
            Text(
                text = stringResource(R.string.listo_subtitulo, negocio?.nombre.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            GvCard(modifier = Modifier.padding(top = 24.dp)) {
                Resumen(
                    etiqueta = stringResource(R.string.listo_actividad),
                    valor = negocio?.categoria.orEmpty(),
                )
                Separador()
                Resumen(
                    etiqueta = stringResource(R.string.listo_tipo),
                    valor = negocio?.tipoActividad?.let { stringResource(it.tituloRes()) }.orEmpty(),
                )
                uiState.meta?.let { meta ->
                    Separador()
                    Resumen(
                        etiqueta = stringResource(R.string.listo_meta),
                        valor = stringResource(
                            R.string.listo_meta_valor,
                            formatPesos(meta.montoObjetivo),
                            formatMesYAnio(meta.fechaLimite),
                        ),
                        numerico = true,
                    )
                }
                Separador()
                Resumen(
                    etiqueta = stringResource(R.string.listo_reinversion),
                    valor = stringResource(
                        R.string.porcentaje,
                        negocio?.porcentajeReinversion?.toInt() ?: 0,
                    ),
                    numerico = true,
                )
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.listo_ir_al_negocio),
                onClick = onIrAlNegocio,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun Resumen(etiqueta: String, valor: String, numerico: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.titleSmall.let {
                if (numerico) it.merge(NumericTextStyle) else it
            },
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun Separador() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(vertical = 11.dp),
    )
}

/** El título del tipo de actividad ya está definido para la pantalla del paso 1. */
private fun TipoActividad.tituloRes(): Int =
    tiposDeActividad.first { it.tipo == this }.tituloRes

@Preview(name = "Claro", widthDp = 380, heightDp = 800)
@Preview(name = "Oscuro", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NegocioListoScreenPreview() {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NegocioListoScreen(
                uiState = NegocioListoUiState(
                    negocio = Negocio(
                        id = 1,
                        nombre = "Dulce Antojo",
                        tipoActividad = TipoActividad.MIXTO,
                        categoria = "Repostería artesanal",
                        porcentajeReinversion = 20.0,
                        colorMarca = null,
                        fechaCreacion = LocalDateTime.of(2026, 9, 16, 10, 0),
                    ),
                    meta = MetaAhorro(
                        id = 1,
                        montoObjetivo = 2_000_000.0,
                        fechaLimite = LocalDate.of(2026, 12, 31),
                        fechaCreacion = LocalDate.of(2026, 9, 16),
                    ),
                ),
                onIrAlNegocio = {},
            )
        }
    }
}
