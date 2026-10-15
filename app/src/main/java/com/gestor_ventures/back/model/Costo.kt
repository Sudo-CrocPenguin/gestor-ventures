package com.gestor_ventures.back.model

import java.time.LocalDateTime

/**
 * HU-13. Lo que cuesta producir lo que se vende: los insumos de una torta, la tela de un
 * vestido. Distinto del gasto general de HU-14, que es de funcionar.
 */
data class Costo(
    val id: Long,
    val productoServicio: String,
    val monto: Double,
    val fecha: LocalDateTime,
    val categoriaId: Long? = null,
)

/**
 * HU-13. Lo que deja un producto: lo que se vendió menos lo que costó producirlo.
 *
 * [margen] puede ser negativo, y eso es justamente lo que el emprendedor necesita ver: significa
 * que ese producto le está costando más de lo que le deja.
 */
data class MargenProducto(
    val productoServicio: String,
    val vendido: Double,
    val costeado: Double,
) {
    val margen: Double get() = vendido - costeado

    /** Qué porcentaje de lo vendido queda como margen; null si todavía no se ha vendido nada. */
    val porcentaje: Double? get() = if (vendido <= 0.0) null else margen / vendido * 100
}

/** Reglas que debe cumplir un costo (HU-13). */
enum class ErrorCosto {
    ProductoVacio,
    MontoNoPositivo,
    FechaEnElFuturo,
}
