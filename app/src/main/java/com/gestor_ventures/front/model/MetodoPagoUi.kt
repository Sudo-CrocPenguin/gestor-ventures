package com.gestor_ventures.front.model

import androidx.annotation.StringRes
import com.gestor_ventures.R
import com.gestor_ventures.back.model.MetodoPago

/**
 * HU-11. Métodos de pago que ofrece la UI. El dominio admite además OTRO, que no se ofrece
 * porque no le dice nada al usuario: si aparece en una venta vieja, se muestra como efectivo.
 */
enum class MetodoPagoUi(@param:StringRes val labelRes: Int) {
    Efectivo(R.string.metodo_pago_efectivo),
    Transferencia(R.string.metodo_pago_transferencia),
    Tarjeta(R.string.metodo_pago_tarjeta),
}

/** El método de pago como lo entiende el dominio, para poder guardarlo. */
fun MetodoPagoUi.aDominio(): MetodoPago = when (this) {
    MetodoPagoUi.Efectivo -> MetodoPago.EFECTIVO
    MetodoPagoUi.Transferencia -> MetodoPago.TRANSFERENCIA
    MetodoPagoUi.Tarjeta -> MetodoPago.TARJETA
}
