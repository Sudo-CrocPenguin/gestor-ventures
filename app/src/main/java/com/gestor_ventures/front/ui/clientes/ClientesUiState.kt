package com.gestor_ventures.front.ui.clientes

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.ErrorCliente
import com.gestor_ventures.back.model.enlaceDeWhatsApp

/**
 * HU-29. Los clientes del negocio activo.
 *
 * El historial de compras todavía no está: eso es HU-30. Esta pantalla responde "¿a quién le
 * vendo, cómo lo ubico y cómo lo encuentro?".
 */
data class ClientesUiState(
    val clientes: List<Cliente> = emptyList(),
    /** HU-33. Lo que el usuario está buscando, por nombre o por número. */
    val busqueda: String = "",
    val cargando: Boolean = true,
    val formulario: FormularioCliente? = null,
    /** El cliente que el usuario tocó, mientras elige qué hacer con él. */
    val acciones: Cliente? = null,
    val error: ErrorCliente? = null,
) {
    /** El negocio no tiene clientes todavía, que es distinto de que la búsqueda no encuentre. */
    val vacio: Boolean get() = !cargando && clientes.isEmpty() && busqueda.isBlank()

    /** HU-33. Hay clientes, pero ninguno coincide con lo escrito. */
    val sinResultados: Boolean get() = !cargando && clientes.isEmpty() && busqueda.isNotBlank()
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

/**
 * HU-31. El enlace de WhatsApp del cliente que se tocó, o null si no tiene teléfono.
 *
 * Se arma acá y no en la pantalla para que el botón apagado y el enlace salgan del mismo dato:
 * si cada uno lo decidiera por su lado, podrían no estar de acuerdo.
 */
val ClientesUiState.whatsappDelElegido: String?
    get() = enlaceDeWhatsApp(acciones?.telefono)

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
