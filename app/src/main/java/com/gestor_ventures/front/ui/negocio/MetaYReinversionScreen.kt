package com.gestor_ventures.front.ui.negocio

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.gestor_ventures.back.model.ProgresoMeta
import com.gestor_ventures.front.components.GvBackTopBar
import com.gestor_ventures.front.components.GvCard
import com.gestor_ventures.front.components.GvCardHeader
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvProgressBar
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.theme.NumericTextStyle
import com.gestor_ventures.front.util.formatMesYAnio
import com.gestor_ventures.front.util.formatPesos
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun MetaYReinversionRoute(
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: MetaYReinversionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.guardado.collect { onGuardado() }
    }

    MetaYReinversionScreen(
        uiState = uiState,
        onBack = onBack,
        onMetaMontoChange = viewModel::onMetaMontoChange,
        onFechaLimiteChange = viewModel::onFechaLimiteChange,
        onPorcentajeChange = viewModel::onPorcentajeReinversionChange,
        onGuardar = viewModel::guardar,
    )
}

/**
 * HU-08 y HU-09. Dónde se define y se corrige la meta de ahorro y la reinversión.
 *
 * Es el paso 2 del onboarding convertido en configuración: los mismos campos, con el progreso
 * de la meta arriba para que cambiar el objetivo no sea una decisión a ciegas.
 */
@Composable
fun MetaYReinversionScreen(
    uiState: MetaYReinversionUiState,
    onBack: () -> Unit,
    onMetaMontoChange: (String) -> Unit,
    onFechaLimiteChange: (LocalDate) -> Unit,
    onPorcentajeChange: (Int) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GvBackTopBar(titulo = stringResource(R.string.menu_meta_ahorro), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.progreso?.let { ProgresoActual(it) }

            CamposMetaAhorro(
                monto = uiState.metaMonto,
                montoFormateado = uiState.metaFormateada,
                fechaLimite = uiState.fechaLimite,
                ahorroMensual = uiState.ahorroMensual,
                onMontoChange = onMetaMontoChange,
                onFechaChange = onFechaLimiteChange,
            )

            CampoReinversion(
                porcentaje = uiState.porcentajeReinversion,
                onPorcentajeChange = onPorcentajeChange,
            )

            GvInfoNote(stringResource(R.string.meta_reinversion_ayuda))

            // Una meta a medias no se puede guardar, y hay que decir por qué está bloqueado.
            AnimatedVisibility(visible = uiState.metaAMedias) {
                Aviso(stringResource(R.string.meta_incompleta))
            }

            AnimatedVisibility(visible = uiState.error != null) {
                Aviso(uiState.error?.let { stringResource(it.mensajeRes()) }.orEmpty())
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            GvPrimaryButton(
                text = stringResource(R.string.meta_guardar),
                onClick = onGuardar,
                enabled = uiState.puedeGuardar,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding(),
            )
        }
    }
}

/** HU-08. Cómo va la meta que ya existe, antes de dejar que se cambie. */
@Composable
private fun ProgresoActual(progreso: ProgresoMeta, modifier: Modifier = Modifier) {
    GvCard(modifier) {
        GvCardHeader(title = stringResource(R.string.meta_progreso_titulo)) {
            Text(
                text = stringResource(R.string.porcentaje, (progreso.fraccion * 100).roundToInt()),
                style = MaterialTheme.typography.labelLarge.merge(NumericTextStyle),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        GvProgressBar(progress = progreso.fraccion)

        Text(
            text = stringResource(
                R.string.resumen_meta_de,
                formatPesos(progreso.acumulado),
                formatPesos(progreso.montoObjetivo),
            ),
            style = MaterialTheme.typography.bodySmall.merge(NumericTextStyle),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp),
        )

        Text(
            text = if (progreso.cumplida) {
                stringResource(R.string.meta_cumplida_corto)
            } else {
                stringResource(
                    R.string.resumen_meta_falta,
                    formatPesos(progreso.falta),
                    formatMesYAnio(progreso.fechaLimite),
                )
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = if (progreso.cumplida) {
                GestorVenturesTheme.colors.success
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun Aviso(texto: String) {
    GvInfoNote(
        text = texto,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Preview(name = "Meta y reinversión", widthDp = 380, heightDp = 860)
@Preview(
    name = "Meta y reinversión (oscuro)",
    widthDp = 380,
    heightDp = 860,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MetaYReinversionPreview() {
    VistaPreviaMeta(
        MetaYReinversionUiState(
            metaMonto = "2000000",
            fechaLimite = LocalDate.of(2026, 12, 31),
            porcentajeReinversion = 20,
            ahorroMensual = 500_000.0,
            progreso = ProgresoMeta(
                montoObjetivo = 2_000_000.0,
                acumulado = 1_200_000.0,
                fechaLimite = LocalDate.of(2026, 12, 31),
            ),
            cargando = false,
        ),
    )
}

@Preview(name = "Sin meta todavía", widthDp = 380, heightDp = 860)
@Composable
private fun MetaVaciaPreview() {
    VistaPreviaMeta(MetaYReinversionUiState(cargando = false))
}

@Composable
private fun VistaPreviaMeta(uiState: MetaYReinversionUiState) {
    GestorVenturesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MetaYReinversionScreen(
                uiState = uiState,
                onBack = {},
                onMetaMontoChange = {},
                onFechaLimiteChange = {},
                onPorcentajeChange = {},
                onGuardar = {},
            )
        }
    }
}
