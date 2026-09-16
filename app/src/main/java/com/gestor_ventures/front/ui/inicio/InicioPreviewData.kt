package com.gestor_ventures.front.ui.inicio

import java.time.LocalDate
import java.time.LocalTime

/**
 * Datos del mockup para los @Preview.
 *
 * Los dos primeros valores también los usa la pantalla de verdad, marcados como TEMPORAL:
 * el saludo espera a HU-01 (la cuenta del usuario) y las alertas a HU-40 (notificaciones).
 */
object InicioPreviewData {

    const val NombreTemporal = "Sebastián"
    const val AlertasTemporales = 2

    val uiState = InicioUiState(
        nombreUsuario = NombreTemporal,
        fecha = LocalDate.now(),
        alertasNuevas = AlertasTemporales,
        resumenHoy = ResumenHoyUi(
            ventas = 148_500.0,
            variacionVentasVsAyer = 12,
            gastos = 42_000.0,
            cantidadGastos = 3,
            tendenciaVentas = listOf(8_800.0, 13_200.0, 7_700.0, 19_800.0, 16_500.0, 26_400.0, 23_100.0, 33_000.0),
            progresoMetaAhorro = 0.68f,
        ),
        cajasActivas = listOf(
            CajaActivaUi(
                cajaId = 1,
                responsable = "Valentina",
                turno = "mañana",
                horaApertura = LocalTime.of(8, 0),
                montoInicial = 50_000.0,
                saldoEsperado = 92_500.0,
            ),
            CajaActivaUi(
                cajaId = 2,
                responsable = "Carlos",
                turno = "tarde",
                horaApertura = LocalTime.of(13, 0),
                montoInicial = 30_000.0,
                saldoEsperado = 56_000.0,
            ),
        ),
    )
}
