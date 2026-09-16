package com.gestor_ventures.front.ui.inicio

import java.time.LocalDate
import java.time.LocalTime

/** Estado de la pantalla de inicio del Líder: saludo, resumen del día y cajas abiertas. */
data class InicioUiState(
    val nombreUsuario: String = "",
    val fecha: LocalDate = LocalDate.now(),
    val alertasNuevas: Int = 0,
    val resumenHoy: ResumenHoyUi = ResumenHoyUi(),
    val cajasActivas: List<CajaActivaUi> = emptyList(),
)

/** HU-16. Totales del día para la tarjeta "Resumen de hoy". */
data class ResumenHoyUi(
    val ventas: Double = 0.0,
    /** Variación porcentual de las ventas frente a ayer; null si ayer no hubo ventas. */
    val variacionVentasVsAyer: Int? = null,
    val gastos: Double = 0.0,
    val cantidadGastos: Int = 0,
    /** Ventas por franja horaria del día, para la mini gráfica. */
    val tendenciaVentas: List<Double> = emptyList(),
    /** HU-08. Progreso de la meta de ahorro activa, de 0 a 1; null si no hay meta activa. */
    val progresoMetaAhorro: Float? = null,
)

/** HU-19/HU-20. Caja abierta de un miembro del equipo con su saldo esperado. */
data class CajaActivaUi(
    val cajaId: Long,
    val responsable: String,
    val turno: String,
    val horaApertura: LocalTime,
    val montoInicial: Double,
    val saldoEsperado: Double,
)
