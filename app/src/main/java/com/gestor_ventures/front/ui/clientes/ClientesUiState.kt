package com.gestor_ventures.front.ui.clientes

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.ErrorCliente

/**
 * HU-29. Los clientes del negocio activo.
 *
 * Todavía no hay búsqueda ni historial de compras: eso es HU-33 y HU-30. Esta pantalla responde
 * solo "¿a quién le vendo y cómo lo ubico?".
 */
data class ClientesUiState(
    val clientes: List<Cliente> = emptyList(),
    val cargando: Boolean = true,
    val formulario: FormularioCliente? = null,
    /** El cliente que el usuario tocó, mientras elige qué hacer con él. */
    val acciones: Cliente? = null,
    val error: ErrorCliente? = null,
) {
    val vacio: Boolean get() = !cargando && clientes.isEmpty()
}

/**
 * Formulario de un cliente. [clienteId] nulo significa que se está registrando; con id, que se
 * le está corrigiendo algo a uno que ya existe.
 */
data class FormularioCliente(
    val clienteId: Long? = null,
    val nombre: String = "",
    val telefono: String = "",
    val correo: String = "",
    val notas: String = "",
) {
    val esEdicion: Boolean get() = clienteId != null

    /**
     * Solo el nombre es obligatorio. Exigir teléfono y correo llena la lista de datos
     * inventados, o hace que el cliente no se registre.
     */
    val puedeGuardar: Boolean get() = nombre.isNotBlank()
}

/** Texto que ve el usuario para cada regla que rechaza el repositorio. */
@StringRes
fun ErrorCliente.mensajeRes(): Int = when (this) {
    ErrorCliente.NombreVacio -> R.string.cliente_error_nombre
    ErrorCliente.NombreMuyLargo -> R.string.cliente_error_nombre_largo
    ErrorCliente.TelefonoInvalido -> R.string.cliente_error_telefono
    ErrorCliente.CorreoInvalido -> R.string.cliente_error_correo
    ErrorCliente.TelefonoRepetido -> R.string.cliente_error_telefono_repetido
    ErrorCliente.CorreoRepetido -> R.string.cliente_error_correo_repetido
}
