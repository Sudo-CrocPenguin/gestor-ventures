package com.gestor_ventures.front.ui.clientes

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.ErrorRecordatorio
import com.gestor_ventures.back.model.TipoRecordatorio
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
    /** HU-32. Con qué nombre firma el mensaje: el del negocio, no el del dueño. */
    val negocio: String = "",
    val compras: List<CompraUi> = emptyList(),
    val recordatorios: List<RecordatorioUi> = emptyList(),
    /** HU-32. El recordatorio que se está escribiendo; null si la hoja está cerrada. */
    val recordatorio: FormularioRecordatorio? = null,
    val errorRecordatorio: ErrorRecordatorio? = null,
    val cargando: Boolean = true,
) {
    /** El cliente existe pero nunca le ha comprado nada. */
    val sinCompras: Boolean get() = !cargando && cliente != null && compras.isEmpty()

    /** HU-30. Lo que ha dejado este cliente desde que se registró. */
    val totalAcumulado: Double get() = compras.sumOf { it.monto }

    /** HU-31. A dónde escribirle, o null si no dejó teléfono. */
    val whatsapp: String? get() = enlaceDeWhatsApp(cliente?.telefono)

    /**
     * HU-32. Si se le puede mandar un recordatorio.
     *
     * Es la misma condición que la de escribirle (HU-31): el recordatorio sale por WhatsApp, y
     * sin teléfono (HU-29) no hay a dónde mandarlo.
     */
    val puedeRecordar: Boolean get() = whatsapp != null
}

/**
 * HU-32. El recordatorio mientras se escribe.
 *
 * El mensaje arranca de una plantilla y queda editable: lo que se manda es lo que el usuario
 * decidió decir. En blanco no se envía — un WhatsApp vacío no recuerda nada.
 */
data class FormularioRecordatorio(
    val tipo: TipoRecordatorio = TipoRecordatorio.Cita,
    val mensaje: String = "",
) {
    val puedeEnviar: Boolean get() = mensaje.isNotBlank()
}

/** Una compra del cliente, ya lista para la fila. */
data class CompraUi(
    val id: Long,
    val titulo: String,
    val monto: Double,
    val fechaHora: LocalDateTime,
)

/** HU-32. Un recordatorio que ya se le mandó, para la fila del historial. */
data class RecordatorioUi(
    val id: Long,
    val tipo: TipoRecordatorio,
    val mensaje: String,
    val fechaEnvio: LocalDateTime,
)

/** HU-32. Cómo se llama cada tipo de recordatorio en el historial. */
@StringRes
fun TipoRecordatorio.etiquetaRes(): Int = when (this) {
    TipoRecordatorio.Cita -> R.string.cliente_recordatorio_etiqueta_cita
    TipoRecordatorio.PagoPendiente -> R.string.cliente_recordatorio_etiqueta_pago
}

/** HU-32. Por qué no se pudo mandar el recordatorio. */
@StringRes
fun ErrorRecordatorio.mensajeRes(): Int = when (this) {
    ErrorRecordatorio.MensajeVacio -> R.string.cliente_recordatorio_error_vacio
    ErrorRecordatorio.ClienteSinTelefono -> R.string.cliente_recordatorio_error_sin_telefono
}
