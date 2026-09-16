package com.gestor_ventures.front.model

import androidx.annotation.StringRes
import com.gestor_ventures.R

/**
 * HU-11. Métodos de pago que ofrece la UI. Corresponden al enum `MetodoPago` de `db/`, pero
 * viven aparte porque `front/` no importa nada de la capa de datos: el mapeo entre los dos lo
 * hará el repositorio cuando exista.
 */
enum class MetodoPagoUi(@param:StringRes val labelRes: Int) {
    Efectivo(R.string.metodo_pago_efectivo),
    Transferencia(R.string.metodo_pago_transferencia),
    Tarjeta(R.string.metodo_pago_tarjeta),
}
