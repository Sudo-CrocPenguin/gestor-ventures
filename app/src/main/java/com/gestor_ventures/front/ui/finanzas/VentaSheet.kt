package com.gestor_ventures.front.ui.finanzas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.gestor_ventures.front.components.GvDateField
import com.gestor_ventures.front.components.GvInfoNote
import com.gestor_ventures.front.components.GvPrimaryButton
import com.gestor_ventures.front.components.GvSegmentedToggle
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import java.time.LocalDate

/**
 * HU-17. Hoja para corregir una venta ya registrada.
 *
 * Es el reverso del registro: allá la fecha la pone el sistema porque la venta acaba de pasar;
 * acá se puede mover, porque la corrección más común es justamente esa —registrar hoy algo que
 * fue ayer, al cerrar la jornada—.
 *
 * La hora no se toca: cambiarla no arregla nada que el usuario vaya a notar, y un selector de
 * hora sería un campo más en una hoja que ya es larga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaSheet(
    formulario: RegistrarVentaUiState,
    onTipoRegistroChange: (TipoRegistroVentaUi) -> Unit,
    onMontoChange: (String) -> Unit,
    onProductoServicioChange: (String) -> Unit,
    onMetodoPagoChange: (MetodoPagoUi) -> Unit,
    onNotaChange: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
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
                text = stringResource(R.string.venta_corregir_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            GvSegmentedToggle(
                opciones = TipoRegistroVentaUi.entries,
                seleccionada = formulario.tipoRegistro,
                etiqueta = { stringResource(it.labelRes) },
                onSeleccionar = onTipoRegistroChange,
            )

            GvDateField(
                label = stringResource(R.string.venta_fecha),
                fecha = formulario.fechaHora.toLocalDate(),
                onFechaChange = onFechaChange,
                placeholder = stringResource(R.string.base_fecha_limite_placeholder),
            )

            CamposDeVenta(
                esDetallada = formulario.esDetallada,
                montoFormateado = formulario.montoFormateado,
                productoServicio = formulario.productoServicio,
                metodoPago = formulario.metodoPago,
                nota = formulario.nota,
                onMontoChange = onMontoChange,
                onProductoServicioChange = onProductoServicioChange,
                onMetodoPagoChange = onMetodoPagoChange,
                onNotaChange = onNotaChange,
            )

            AnimatedVisibility(visible = formulario.error != null) {
                GvInfoNote(
                    text = formulario.error?.let { stringResource(it.mensajeRes()) }.orEmpty(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            GvPrimaryButton(
                text = stringResource(R.string.venta_guardar_cambios),
                onClick = onGuardar,
                enabled = formulario.puedeGuardar,
            )
        }
    }
}
