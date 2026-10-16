package com.gestor_ventures.front.ui.caja

import com.gestor_ventures.back.model.PeriodoPredefinido
import java.time.LocalDateTime

/**
 * HU-23. Las jornadas de caja que ya terminaron.
 *
 * Mientras [cargando] no se resuelva no se dice que no hay nada: asegurar "no tienes cierres"
 * antes de haber mirado es peor que esperar un momento.
 */
data class HistorialCajaUiState(
    val cierres: List<CierreUi> = emptyList(),
    val periodo: PeriodoPredefinido = PeriodoPredefinido.Semana,
    val cargando: Boolean = true,
) {
    val vacio: Boolean get() = !cargando && cierres.isEmpty()

    /** Cuántas jornadas hubo en el periodo. */
    val cantidad: Int get() = cierres.size

    /**
     * En cuántas no cuadró la caja. Es el número que de verdad se mira: diez cierres no dicen
     * nada, tres descuadres sí.
     */
    val conDiferencia: Int get() = cierres.count { !it.cuadra }
}

/** Una jornada del historial, ya traducida a lo que la fila muestra. */
data class CierreUi(
    val id: Long,
    val apertura: LocalDateTime,
    val cierre: LocalDateTime,
    val montoInicial: Double,
    val esperado: Double,
    val real: Double,
    val diferencia: Double,
    val cuadra: Boolean,
    val sobra: Boolean,
    val nota: String? = null,
)
