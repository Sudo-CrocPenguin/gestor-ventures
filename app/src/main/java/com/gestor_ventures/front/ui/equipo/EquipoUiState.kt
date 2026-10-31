package com.gestor_ventures.front.ui.equipo

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ErrorEquipo
import com.gestor_ventures.back.model.MiembroEquipo

/** Quien lidera el negocio: la persona que tiene la sesión abierta. */
data class LiderUi(val nombre: String, val correo: String)

/** El formulario para invitar a un vendedor. Abierto cuando no es null. */
data class FormularioInvitacion(
    val correo: String = "",
    val error: ErrorEquipo? = null,
) {
    val puedeEnviar: Boolean get() = correo.isNotBlank()
}

/**
 * Pantalla Equipo: el Líder y sus vendedores. [elegido] es el vendedor sobre el que se abrió el
 * menú de acciones.
 */
data class EquipoUiState(
    val lider: LiderUi? = null,
    val vendedores: List<MiembroEquipo> = emptyList(),
    val invitacion: FormularioInvitacion? = null,
    val elegido: MiembroEquipo? = null,
    val sinNegocio: Boolean = false,
) {
    val sinVendedores: Boolean get() = vendedores.isEmpty()
}

@StringRes
fun ErrorEquipo.mensajeRes(): Int = when (this) {
    ErrorEquipo.CorreoInvalido -> R.string.equipo_error_correo
    ErrorEquipo.EsElLider -> R.string.equipo_error_lider
    ErrorEquipo.YaEstaEnElEquipo -> R.string.equipo_error_repetido
}
