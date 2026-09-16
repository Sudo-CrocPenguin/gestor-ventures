package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.Reloj
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.ceil

/**
 * HU-08. Cuánto hay que apartar cada mes para llegar a la meta antes de su fecha límite.
 *
 * Es el mensaje del onboarding: "Para llegar a $2.000.000 antes del 31 de diciembre necesitas
 * apartar $500.000 al mes". Los meses que faltan se redondean hacia arriba: si faltan mes y
 * medio, hay que repartir en dos meses.
 */
class CalcularAhorroMensual @Inject constructor(
    private val reloj: Reloj,
) {

    operator fun invoke(montoObjetivo: Double, fechaLimite: LocalDate): Double? {
        if (montoObjetivo <= 0.0) return null

        val hoy = reloj.ahora().toLocalDate()
        if (!fechaLimite.isAfter(hoy)) return null

        val dias = ChronoUnit.DAYS.between(hoy, fechaLimite)
        val meses = ceil(dias / DiasPorMes).coerceAtLeast(1.0)
        return montoObjetivo / meses
    }

    private companion object {
        /** Promedio de días por mes: evita saltos raros entre meses de 28 y de 31 días. */
        const val DiasPorMes = 30.44
    }
}
