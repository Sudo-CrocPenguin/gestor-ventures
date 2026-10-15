package com.gestor_ventures.back.model

import java.time.LocalDateTime

/**
 * HU-11 y HU-12. Una venta tal como la entiende la app, sin nada de Room.
 *
 * Las dos modalidades son el mismo registro: en la rápida solo hay monto y fecha, así que
 * [productoServicio], [metodoPago] y [clienteId] van nulos.
 */
data class Venta(
    val id: Long,
    val negocioId: Long,
    val tipoRegistro: TipoRegistroVenta,
    val monto: Double,
    val fechaHora: LocalDateTime,
    val productoServicio: String? = null,
    val metodoPago: MetodoPago? = null,
    val clienteId: Long? = null,
    /** Apunte libre del negocio. La venta rápida lo usa para no perder el detalle del día. */
    val nota: String? = null,
)

/** HU-11/HU-12. Detallada pide qué se vendió y cómo pagaron; rápida solo el total. */
enum class TipoRegistroVenta { DETALLADO, RAPIDO }

/** HU-11. Cómo pagó el cliente. */
enum class MetodoPago { EFECTIVO, TARJETA, TRANSFERENCIA, OTRO }

/** Lo vendido en un periodo: es lo que necesita el resumen del inicio (HU-16). */
data class ResumenVentas(val total: Double, val cantidad: Int)

/** Reglas que debe cumplir una venta para quedar registrada (HU-11, HU-12). */
enum class ErrorVenta {
    MontoNoPositivo,
    ProductoVacio,
    MetodoPagoFaltante,
    FechaEnElFuturo,
}
