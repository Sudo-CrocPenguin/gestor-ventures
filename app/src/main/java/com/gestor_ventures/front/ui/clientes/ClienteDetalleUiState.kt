package com.gestor_ventures.front.ui.clientes

import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.enlaceDeWhatsApp
import java.time.LocalDateTime

/**
 * HU-30. El perfil de un cliente: sus datos y lo que le ha comprado al negocio.
 *
 * Todavía solo trae ventas. Las citas (HU-24) y los pedidos (HU-25) son de la Épica 5 y aún no
 * existen; cuando existan se suman a esta misma lista, no se hace otra pantalla.
 */
data class ClienteDetalleUiState(
    val cliente: Cliente? = null,
    val compras: List<CompraUi> = emptyList(),
    val cargando: Boolean = true,
) {
    /** El cliente existe pero nunca le ha comprado nada. */
    val sinCompras: Boolean get() = !cargando && cliente != null && compras.isEmpty()

    /** HU-30. Lo que ha dejado este cliente desde que se registró. */
    val totalAcumulado: Double get() = compras.sumOf { it.monto }

    /** HU-31. A dónde escribirle, o null si no dejó teléfono. */
    val whatsapp: String? get() = enlaceDeWhatsApp(cliente?.telefono)
}

/** Una compra del cliente, ya lista para la fila. */
data class CompraUi(
    val id: Long,
    val titulo: String,
    val monto: Double,
    val fechaHora: LocalDateTime,
)
