package com.gestor_ventures.front.ui.inicio

import com.gestor_ventures.back.model.Alerta
import com.gestor_ventures.back.model.TipoAlerta
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Datos del mockup para los @Preview.
 *
 * El nombre también lo usa la pantalla de verdad, marcado como TEMPORAL: el saludo espera a
 * HU-01 (la cuenta del usuario).
 */
object InicioPreviewData {

    const val NombreTemporal = "Sebastián"

    private val ayer: LocalDateTime = LocalDateTime.now().minusDays(1)

    val uiState = InicioUiState(
        nombreUsuario = NombreTemporal,
        fecha = LocalDate.now(),
        resumenHoy = ResumenHoyUi(
            ventas = 148_500.0,
            variacionVentasVsAyer = 12,
            gastos = 42_000.0,
            cantidadGastos = 3,
            tendenciaVentas = listOf(8_800.0, 13_200.0, 7_700.0, 19_800.0, 16_500.0, 26_400.0, 23_100.0, 33_000.0),
            progresoMetaAhorro = 0.68f,
        ),
        cajaAbierta = CajaAbiertaUi(
            horaApertura = LocalTime.of(8, 0),
            montoInicial = 50_000.0,
            saldoEsperado = 122_500.0,
        ),
        alertas = listOf(
            Alerta(
                id = 2,
                tipo = TipoAlerta.CaidaVentas,
                descripcion = "Vendiste 32 % menos que la semana pasada.",
                leida = false,
                fechaGeneracion = ayer,
            ),
            Alerta(
                id = 1,
                tipo = TipoAlerta.GastoElevado,
                descripcion = "Tus gastos de este mes van por encima de lo normal.",
                leida = true,
                fechaGeneracion = ayer.minusDays(2),
            ),
        ),
        ultimasVentas = listOf(
            VentaDeHoyUi(3, "Torta personalizada", 85_000.0, LocalTime.of(14, 20)),
            VentaDeHoyUi(2, "Cupcakes x6", 7_500.0, LocalTime.of(11, 5)),
            VentaDeHoyUi(1, "", 25_000.0, LocalTime.of(9, 40)),
        ),
    )
}
