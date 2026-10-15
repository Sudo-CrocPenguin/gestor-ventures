package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.DiasPorMes
import com.gestor_ventures.back.model.Reloj
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * HU-08. Cuánto hay que apartar cada mes para llegar a la meta antes de su fecha límite.
 *
 * Es el mensaje del onboarding: "Para llegar a $2.000.000 antes del 31 de diciembre necesitas
 * apartar $574.400 al mes".
 *
 * Los meses que faltan no se redondean. Repartir en más meses de los que hay da una cuota más
 * cómoda de la que sirve: si faltan tres meses y medio y se reparte en cuatro, el usuario aparta
 * lo que le decimos todos los meses y aun así llega corto a la fecha.
 *
 * El único piso es un mes: con menos, la cuenta diría "aparta el triple de la meta este mes", y
 * lo que hay que entender es simplemente que queda menos de un mes.
 */
class CalcularAhorroMensual @Inject constructor(
    private val reloj: Reloj,
) {

    operator fun invoke(montoObjetivo: Double, fechaLimite: LocalDate): Double? {
        if (montoObjetivo <= 0.0) return null

        val hoy = reloj.ahora().toLocalDate()
        if (!fechaLimite.isAfter(hoy)) return null

        val dias = ChronoUnit.DAYS.between(hoy, fechaLimite)
        val meses = (dias / DiasPorMes).coerceAtLeast(1.0)
        return montoObjetivo / meses
    }
}
