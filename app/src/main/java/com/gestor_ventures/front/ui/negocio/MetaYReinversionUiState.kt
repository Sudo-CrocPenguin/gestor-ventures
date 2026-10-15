package com.gestor_ventures.front.ui.negocio

import com.gestor_ventures.back.model.ErrorBaseFinanciera
import com.gestor_ventures.back.model.ProgresoMeta
import com.gestor_ventures.front.util.formatMiles
import java.time.LocalDate

/**
 * HU-08 y HU-09. La meta de ahorro y la reinversión del negocio activo, fuera del onboarding.
 *
 * Las dos son opcionales: un negocio puede no tener meta y reinvertir cero. Lo que no se puede
 * es dejar una meta a medias —un monto sin fecha o una fecha sin monto—, porque entonces no hay
 * con qué calcular cuánto apartar al mes.
 */
data class MetaYReinversionUiState(
    val metaMonto: String = "",
    val fechaLimite: LocalDate? = null,
    val porcentajeReinversion: Int = 0,
    /** Lo que hay que apartar cada mes para llegar a la meta; null si aún no se puede calcular. */
    val ahorroMensual: Double? = null,
    /** Cómo va la meta que ya existe. Null mientras no haya ninguna definida. */
    val progreso: ProgresoMeta? = null,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: ErrorBaseFinanciera? = null,
) {
    val metaFormateada: String
        get() = if (metaMonto.isEmpty()) "" else formatMiles(metaMonto.toLongOrNull() ?: 0L)

    val montoValor: Long get() = metaMonto.toLongOrNull() ?: 0L

    /** Un monto sin fecha o una fecha sin monto: falta la mitad de la meta. */
    val metaAMedias: Boolean get() = (montoValor > 0L) != (fechaLimite != null)

    val puedeGuardar: Boolean get() = !guardando && !cargando && !metaAMedias
}
