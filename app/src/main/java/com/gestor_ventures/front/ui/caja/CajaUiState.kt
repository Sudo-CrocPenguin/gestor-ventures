package com.gestor_ventures.front.ui.caja

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.front.util.formatMiles
import java.time.LocalDateTime

/**
 * HU-19. La jornada de caja del negocio activo.
 *
 * La pantalla tiene dos caras y [caja] decide cuál se ve: con una jornada en curso muestra con
 * qué arrancó y desde cuándo; sin ninguna, invita a abrirla. Mientras [cargando] no se resuelva
 * no se muestra ninguna de las dos, para no asegurar que no hay caja antes de haber mirado.
 */
data class CajaUiState(
    val caja: CajaAbiertaUi? = null,
    val cargando: Boolean = true,
    val formulario: FormularioApertura? = null,
    val error: ErrorCaja? = null,
) {
    val sinCaja: Boolean get() = !cargando && caja == null
}

/** Lo que la pantalla muestra de una jornada abierta. */
data class CajaAbiertaUi(
    val montoInicial: Double,
    val apertura: LocalDateTime,
)

/**
 * Formulario de apertura. El monto se guarda en dígitos crudos y se formatea aparte: así el
 * usuario ve "50.000" mientras escribe y el ViewModel sigue teniendo un número que convertir.
 */
data class FormularioApertura(
    val montoInicial: String = "",
) {
    val montoFormateado: String
        get() = if (montoInicial.isEmpty()) "" else formatMiles(montoInicial.toLongOrNull() ?: 0L)

    /**
     * Exige que el campo tenga algo escrito, aunque sea un cero. Abrir con cero es válido —se
     * puede arrancar el turno sin efectivo—, pero tiene que ser una decisión y no un campo que
     * se quedó vacío.
     */
    val puedeAbrir: Boolean get() = montoInicial.isNotBlank()
}

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorCaja.mensajeRes(): Int = when (this) {
    ErrorCaja.MontoNegativo -> R.string.caja_error_monto
    ErrorCaja.YaHayCajaAbierta -> R.string.caja_error_ya_abierta
}
