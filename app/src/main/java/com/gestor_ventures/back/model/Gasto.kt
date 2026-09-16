package com.gestor_ventures.back.model

import java.time.LocalDate

/**
 * HU-14. Gasto general del negocio: lo que se paga una vez y no se repite solo, a diferencia
 * de los gastos fijos de HU-06.
 *
 * [categoriaId] nulo es un gasto sin clasificar. Se permite a propósito: obligar a categorizar
 * en el momento de registrar hace que la gente invente categorías o no registre el gasto.
 */
data class Gasto(
    val id: Long,
    val descripcion: String,
    val monto: Double,
    val fecha: LocalDate,
    val categoriaId: Long? = null,
)

/** HU-15. Lo acumulado en una categoría; [categoriaId] nulo es el montón sin clasificar. */
data class GastoPorCategoria(
    val categoriaId: Long?,
    val total: Double,
)

/** Reglas que debe cumplir un gasto (HU-14). */
enum class ErrorGasto {
    DescripcionVacia,
    MontoNoPositivo,
    FechaEnElFuturo,
}
