package com.gestor_ventures.front.ui.negocio

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorObligacion
import com.gestor_ventures.back.model.Obligacion
import com.gestor_ventures.front.util.formatMiles
import java.time.LocalDate

/**
 * HU-07. Los compromisos del negocio: lo que debe y para cuándo.
 *
 * [proximas] sale aparte de [obligaciones] a propósito: es la respuesta a "¿qué tengo que pagar
 * ya?", y esa pregunta no se responde leyendo una lista completa.
 */
data class ObligacionesUiState(
    val obligaciones: List<Obligacion> = emptyList(),
    val proximas: List<Obligacion> = emptyList(),
    val totalPendiente: Double = 0.0,
    val hoy: LocalDate = LocalDate.now(),
    val cargando: Boolean = true,
    val formulario: FormularioObligacion? = null,
    /** La obligación que el usuario tocó, mientras elige qué hacer con ella. */
    val acciones: Obligacion? = null,
    val error: ErrorObligacion? = null,
) {
    val vacio: Boolean get() = !cargando && obligaciones.isEmpty()

    /**
     * La lista completa solo aporta cuando hay algo que no esté ya arriba. Si todo lo que hay
     * vence pronto, repetirlo sería mostrar dos veces lo mismo.
     */
    val mostrarTodas: Boolean get() = obligaciones.size > proximas.size
}

/**
 * Formulario de una obligación. [obligacionId] nulo significa que se está registrando; con id,
 * que se le está corrigiendo algo a una que ya existe.
 */
data class FormularioObligacion(
    val obligacionId: Long? = null,
    val nombre: String = "",
    val monto: String = "",
    val fechaVencimiento: LocalDate? = null,
) {
    val esEdicion: Boolean get() = obligacionId != null

    val montoValor: Long get() = monto.toLongOrNull() ?: 0L

    val montoFormateado: String get() = if (monto.isEmpty()) "" else formatMiles(montoValor)

    /** La fecha es obligatoria: una obligación sin vencimiento no se puede vigilar. */
    val puedeGuardar: Boolean
        get() = nombre.isNotBlank() && montoValor > 0L && fechaVencimiento != null
}

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorObligacion.mensajeRes(): Int = when (this) {
    ErrorObligacion.NombreVacio -> R.string.obligacion_error_nombre
    ErrorObligacion.MontoNoPositivo -> R.string.obligacion_error_monto
}
