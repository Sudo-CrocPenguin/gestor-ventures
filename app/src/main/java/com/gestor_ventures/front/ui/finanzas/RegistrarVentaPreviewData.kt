package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import java.time.LocalDateTime

/** Datos de ejemplo para los @Preview del formulario de venta. */
object RegistrarVentaPreviewData {

    private val momento: LocalDateTime = LocalDateTime.of(2026, 9, 12, 9, 45)

    val detallada = RegistrarVentaUiState(
        tipoRegistro = TipoRegistroVentaUi.Detallado,
        monto = "85000",
        productoServicio = "Torta personalizada",
        metodoPago = MetodoPagoUi.Transferencia,
        fechaHora = momento,
    )

    val rapida = RegistrarVentaUiState(
        tipoRegistro = TipoRegistroVentaUi.Rapido,
        monto = "148500",
        nota = "Ventas del día en el punto",
        fechaHora = momento,
    )
}
