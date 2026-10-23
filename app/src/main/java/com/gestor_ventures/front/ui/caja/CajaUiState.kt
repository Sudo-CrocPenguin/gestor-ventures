package com.gestor_ventures.front.ui.caja

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.DiferenciaDeCaja
import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.front.util.formatMiles
import java.time.LocalDateTime

/**
 * HU-19 y HU-20. La jornada de caja del negocio activo.
 *
 * La pantalla tiene dos caras y [caja] decide cuál se ve: con una jornada en curso muestra con
 * qué arrancó y desde cuándo; sin ninguna, invita a abrirla. Mientras [cargando] no se resuelva
 * no se muestra ninguna de las dos, para no asegurar que no hay caja antes de haber mirado.
 */
data class CajaUiState(
    val caja: CajaAbiertaUi? = null,
    val cargando: Boolean = true,
    val formulario: FormularioApertura? = null,
    val cierre: FormularioCierre? = null,
    val error: ErrorCaja? = null,
) {
    val sinCaja: Boolean get() = !cargando && caja == null

    /**
     * HU-22. Lo contado contra lo esperado, mientras el usuario escribe.
     *
     * Null hasta que hay algo escrito: antes de eso no hay diferencia que mostrar, y un
     * "faltan $142.500" apenas se abre la hoja sería una alarma falsa.
     */
    val diferencia: DiferenciaDeCaja?
        get() {
            val esperado = caja?.saldoEsperado ?: return null
            val real = cierre?.montoValor ?: return null
            return DiferenciaDeCaja(esperado = esperado, real = real)
        }
}

/**
 * Lo que la pantalla muestra de una jornada abierta.
 *
 * El desglose va al lado del saldo y no escondido: un saldo que no cuadra no le dice nada a
 * nadie si no se puede ver de dónde salió.
 */
data class CajaAbiertaUi(
    val montoInicial: Double,
    val apertura: LocalDateTime,
    val ventas: Double,
    val gastos: Double,
    val costos: Double,
    val saldoEsperado: Double,
) {
    /** Si la jornada todavía no movió plata, el desglose no tiene nada que contar. */
    val sinMovimientos: Boolean get() = ventas == 0.0 && gastos == 0.0 && costos == 0.0
}

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

/**
 * HU-21 y HU-22. Formulario de cierre: lo que de verdad hay en la caja y por qué no cuadra.
 *
 * La nota va acá aunque sea de HU-22 porque es el mismo momento: se escribe mirando la
 * diferencia que acaba de aparecer, no en una pantalla aparte.
 */
data class FormularioCierre(
    val montoReal: String = "",
    val nota: String = "",
) {
    val montoFormateado: String
        get() = if (montoReal.isEmpty()) "" else formatMiles(montoReal.toLongOrNull() ?: 0L)

    /** El monto escrito como número, o null si todavía no hay nada. */
    val montoValor: Double? get() = montoReal.toLongOrNull()?.toDouble()

    /** Igual que al abrir: cero se admite, pero tiene que estar escrito. */
    val puedeCerrar: Boolean get() = montoReal.isNotBlank()
}

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorCaja.mensajeRes(): Int = when (this) {
    ErrorCaja.MontoNegativo -> R.string.caja_error_monto
    ErrorCaja.YaHayCajaAbierta -> R.string.caja_error_ya_abierta
    ErrorCaja.NoHayCajaAbierta -> R.string.caja_error_no_abierta
}
