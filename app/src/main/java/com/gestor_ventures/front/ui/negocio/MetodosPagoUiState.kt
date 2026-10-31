package com.gestor_ventures.front.ui.negocio

import com.gestor_ventures.front.model.MetodoPagoUi

/**
 * HU-11. Qué métodos de pago acepta el negocio activo.
 *
 * [activos] son los que aparecen al registrar una venta. [avisoUltimo] se enciende cuando el
 * usuario intenta apagar el último que queda: siempre tiene que haber al menos uno.
 */
data class MetodosPagoUiState(
    val activos: Set<MetodoPagoUi> = MetodoPagoUi.entries.toSet(),
    val avisoUltimo: Boolean = false,
    val sinNegocio: Boolean = false,
)
