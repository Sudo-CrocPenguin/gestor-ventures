package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.ErrorCosto
import com.gestor_ventures.back.model.ErrorGasto
import com.gestor_ventures.back.model.FiltroMovimientos
import com.gestor_ventures.back.model.Movimiento
import com.gestor_ventures.back.model.PeriodoPredefinido

/**
 * HU-17. El historial de movimientos del periodo que se esté mirando.
 *
 * [todos] guarda el periodo completo y el filtro se aplica acá, no en la consulta: los totales
 * de arriba tienen que seguir contando lo que el filtro esconde. Si al mirar solo las ventas
 * desapareciera el total de salidas, el historial dejaría de responder "¿cómo me fue?".
 */
data class HistorialUiState(
    val periodo: PeriodoPredefinido = PeriodoPredefinido.Hoy,
    val filtro: FiltroMovimientos = FiltroMovimientos.Todos,
    val todos: List<Movimiento> = emptyList(),
    /** Para poder decir en qué categoría quedó un gasto, en vez de mostrar su id. */
    val categorias: List<Categoria> = emptyList(),
    val cargando: Boolean = true,
    /** El movimiento que el usuario tocó, mientras elige qué hacer con él. */
    val acciones: Movimiento? = null,
    /** El movimiento que está a punto de borrarse, esperando confirmación. */
    val porEliminar: Movimiento? = null,
    /** El movimiento que se está corrigiendo, con su formulario abierto. */
    val formulario: FormularioMovimiento? = null,
) {
    val movimientos: List<Movimiento>
        get() = when (filtro) {
            FiltroMovimientos.Todos -> todos
            FiltroMovimientos.Ventas -> todos.filter { it.entra }
            FiltroMovimientos.Salidas -> todos.filterNot { it.entra }
        }

    val ingresos: Double get() = todos.filter { it.entra }.sumOf { it.monto }

    val salidas: Double get() = todos.filterNot { it.entra }.sumOf { it.monto }

    val vacio: Boolean get() = !cargando && movimientos.isEmpty()

    /**
     * Si el periodo abarca más de un día, la hora sola no ubica nada: "10:24" no dice si fue
     * hoy o el lunes pasado.
     */
    val variosDias: Boolean
        get() = periodo == PeriodoPredefinido.Semana || periodo == PeriodoPredefinido.Mes

    /** El periodo no tuvo nada, que es distinto de que el filtro no encuentre nada. */
    val periodoVacio: Boolean get() = !cargando && todos.isEmpty()

    fun nombreDeCategoria(categoriaId: Long?): String? =
        categorias.firstOrNull { it.id == categoriaId }?.nombre
}

/**
 * HU-17. El formulario abierto sobre un movimiento del historial.
 *
 * Cada variante reusa el formulario que ya existía en la sección de ese tipo, en vez de
 * inventar uno nuevo: corregir un gasto desde el historial tiene que pedir y validar lo mismo
 * que corregirlo desde Finanzas, o serían dos verdades sobre el mismo dato.
 */
sealed interface FormularioMovimiento {

    data class DeVenta(
        val ventaId: Long,
        val campos: RegistrarVentaUiState,
    ) : FormularioMovimiento

    data class DeGasto(
        val campos: FormularioGasto,
        val error: ErrorGasto? = null,
    ) : FormularioMovimiento

    data class DeCosto(
        val campos: FormularioCosto,
        val error: ErrorCosto? = null,
    ) : FormularioMovimiento
}
