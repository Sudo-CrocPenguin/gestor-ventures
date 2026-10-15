package com.gestor_ventures.front.ui.inicio

import java.time.LocalDate
import java.time.LocalTime

/** Estado de la pantalla de inicio del Líder: saludo, resumen del día y cajas abiertas. */
data class InicioUiState(
    val nombreUsuario: String = "",
    val fecha: LocalDate = LocalDate.now(),
    val alertasNuevas: Int = 0,
    val resumenHoy: ResumenHoyUi = ResumenHoyUi(),
    /** HU-20. La jornada en curso, o null si no hay ninguna abierta. */
    val cajaAbierta: CajaAbiertaUi? = null,
    /** HU-08. La meta llegó al 100 % y el usuario todavía no ha definido la siguiente. */
    val metaCumplida: Boolean = false,
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

/**
 * HU-19 y HU-20. La jornada de caja abierta, con lo que debería haber en ella.
 *
 * Sin responsable ni turno: el mockup los muestra porque viene de la idea de varios vendedores
 * con un Líder, pero la tabla `cajas` cuelga del negocio y no de una persona. Mientras eso no
 * exista, inventarlo sería mostrar un dato que nadie guardó.
 */
data class CajaAbiertaUi(
    val horaApertura: LocalTime,
    val montoInicial: Double,
    val saldoEsperado: Double,
)
