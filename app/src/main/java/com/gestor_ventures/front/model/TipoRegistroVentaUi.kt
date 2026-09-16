package com.gestor_ventures.front.model

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.TipoRegistroVenta

/**
 * HU-11/HU-12. Las dos modalidades de registro de venta: [Detallado] pide qué se vendió y
 * cómo pagaron; [Rapido] solo el total vendido, para cerrar el día rápido.
 *
 * Corresponde al enum del mismo nombre en `back/model`, pero vive aparte porque esta capa
 * carga además su etiqueta traducida. El mapeo entre los dos está abajo.
 */
enum class TipoRegistroVentaUi(@param:StringRes val labelRes: Int) {
    Detallado(R.string.venta_tipo_detallada),
    Rapido(R.string.venta_tipo_rapida),
}

/** La modalidad como la entiende el dominio, para poder guardarla. */
fun TipoRegistroVentaUi.aDominio(): TipoRegistroVenta = when (this) {
    TipoRegistroVentaUi.Detallado -> TipoRegistroVenta.DETALLADO
    TipoRegistroVentaUi.Rapido -> TipoRegistroVenta.RAPIDO
}
