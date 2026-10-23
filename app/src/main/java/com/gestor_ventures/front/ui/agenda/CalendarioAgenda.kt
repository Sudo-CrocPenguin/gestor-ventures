package com.gestor_ventures.front.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor_ventures.R
import com.gestor_ventures.back.model.RangoFechas
import com.gestor_ventures.front.components.GvChip
import com.gestor_ventures.front.theme.GestorVenturesTheme
import com.gestor_ventures.front.util.formatDiaCorto
import com.gestor_ventures.front.util.formatLongDate
import com.gestor_ventures.front.util.formatMesLargo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val LocaleColombia = Locale.forLanguageTag("es-CO")

@Composable
fun EncabezadoDePeriodo(
    vista: VistaAgenda,
    rango: RangoFechas,
    incluyeHoy: Boolean,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onHoy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onAnterior) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.agenda_anterior),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = tituloDe(vista, rango),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSiguiente) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.agenda_siguiente),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f),
            )
        }
        GvChip(text = stringResource(R.string.periodo_hoy), selected = incluyeHoy, onClick = onHoy)
    }
}

private fun tituloDe(vista: VistaAgenda, rango: RangoFechas): String = when (vista) {
    VistaAgenda.Dia -> formatLongDate(rango.desde)
    VistaAgenda.Semana -> "${formatDiaCorto(rango.desde)} – ${formatDiaCorto(rango.hasta)}"
    VistaAgenda.Mes -> formatMesLargo(rango.desde)
}

@Composable
fun CuadriculaDelMes(
    dias: List<LocalDate>,
    hoy: LocalDate,
    cuantosEn: (LocalDate) -> Int,
    onDiaClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primero = dias.firstOrNull() ?: return
    val huecos = primero.dayOfWeek.value - DayOfWeek.MONDAY.value
    val celdas: List<LocalDate?> = List(huecos) { null } + dias
    val semanas = celdas.chunked(7).map { semana -> semana + List(7 - semana.size) { null } }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dia ->
                Text(
                    text = dia.getDisplayName(TextStyle.NARROW, LocaleColombia).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        semanas.forEach { semana ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                semana.forEach { dia ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (dia != null) {
                            CeldaDia(
                                dia = dia,
                                esHoy = dia == hoy,
                                cuantos = cuantosEn(dia),
                                onClick = { onDiaClick(dia) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CeldaDia(dia: LocalDate, esHoy: Boolean, cuantos: Int, onClick: () -> Unit) {
    val forma = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(forma)
            .background(
                if (cuantos > 0) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = if (esHoy) 2.dp else 1.dp,
                color = if (esHoy) GestorVenturesTheme.colors.acento else MaterialTheme.colorScheme.outline,
                shape = forma,
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = dia.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (cuantos > 0) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .background(GestorVenturesTheme.colors.acento, CircleShape)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = cuantos.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
