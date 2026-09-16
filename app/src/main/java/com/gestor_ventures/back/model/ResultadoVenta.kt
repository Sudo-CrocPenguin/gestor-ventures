package com.gestor_ventures.back.model

/** Resultado de registrar una venta (HU-11, HU-12). */
sealed interface ResultadoVenta {

    data class Exito(val ventaId: Long) : ResultadoVenta

    data class Invalido(val error: ErrorVenta) : ResultadoVenta
}
