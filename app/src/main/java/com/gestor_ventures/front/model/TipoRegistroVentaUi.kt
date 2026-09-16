package com.gestor_ventures.front.model

import androidx.annotation.StringRes
import com.gestor_ventures.R

/**
 * HU-11/HU-12. Las dos modalidades de registro de venta: [Detallado] pide qué se vendió,
 * a quién y cómo pagó; [Rapido] solo el total vendido, para cerrar el día rápido.
 *
 * Corresponde al enum `TipoRegistroVenta` de `db/`; el mapeo lo hará el repositorio.
 */
enum class TipoRegistroVentaUi(@param:StringRes val labelRes: Int) {
    Detallado(R.string.venta_tipo_detallada),
    Rapido(R.string.venta_tipo_rapida),
}
