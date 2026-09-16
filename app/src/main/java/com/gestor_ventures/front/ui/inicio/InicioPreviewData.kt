package com.gestor_ventures.front.ui.inicio

import java.time.LocalDate
import java.time.LocalTime

/** Datos del mockup, para los @Preview y para arrancar la UI mientras no existan los repositorios. */
object InicioPreviewData {
    val uiState = InicioUiState(
        nombreUsuario = "Sebastián",
        fecha = LocalDate.now(),
        alertasNuevas = 2,
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
