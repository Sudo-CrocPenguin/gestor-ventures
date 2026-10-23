package com.gestor_ventures.back.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.round

/**
 * Formatos que valen en toda la app, en un solo sitio.
 *
 * Estaban dentro de `AuthRepository` cuando solo los usaba el registro. Ahora los clientes
 * (HU-29) piden lo mismo, y dos expresiones regulares distintas para "correo válido" terminan
 * aceptando cosas distintas: lo que sirve para crear una cuenta tiene que servir para guardar
 * el correo de un cliente.
 */
val FormatoCorreo = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/**
 * HU-29. Deja el teléfono en puros dígitos.
 *
 * La gente escribe "300 111 2233" o "(604) 444-5566", y eso es lo natural. Guardar el número
 * tal cual haría que el mismo teléfono escrito de dos formas pasara por dos clientes distintos,
 * que es justo lo que el índice único quiere evitar.
 */
fun soloDigitos(telefono: String): String = telefono.filter(Char::isDigit)

/** Un teléfono de menos de 7 dígitos no es un teléfono; de más de 15 no existe (E.164). */
val LargoDeTelefono = 7..15

/**
 * Indicativo de Colombia. La app entera está hecha para Colombia —pesos, formato de fecha,
 * idioma—, así que un número sin indicativo se asume de acá.
 */
const val IndicativoColombia = "57"

/** Largo de un número colombiano sin indicativo: un celular son diez dígitos. */
private const val LargoLocal = 10

/**
 * HU-31. El enlace de WhatsApp de un teléfono guardado, o null si no hay teléfono.
 *
 * WhatsApp necesita el número en formato internacional, y en la app se guarda como lo escribió
 * el usuario, casi siempre sin indicativo. Por eso se le agrega el de Colombia a los números
 * que no lo traen; los más largos de diez dígitos se dejan como están, porque ya lo tienen.
 *
 * Es una suposición, no una certeza: alguien que guarde un número de otro país con menos de
 * diez dígitos va a terminar con un enlace equivocado. Lo correcto sería pedir el indicativo
 * en el formulario (HU-29), y eso es un cambio de la historia, no de esta función.
 */
fun enlaceDeWhatsApp(telefono: String?, mensaje: String? = null): String? {
    val digitos = telefono?.let(::soloDigitos)?.takeIf { it.isNotEmpty() } ?: return null
    val internacional =
        if (digitos.length > LargoLocal) digitos else IndicativoColombia + digitos

    // HU-32: con mensaje, WhatsApp abre el chat con el texto ya escrito. Lo escribe el usuario
    // y lo manda él: la app no envía nada por su cuenta.
    val texto = mensaje?.trim()?.takeIf { it.isNotEmpty() } ?: return "https://wa.me/$internacional"
    return "https://wa.me/$internacional?text=" + codificarParaUrl(texto)
}

/**
 * Codifica el texto para meterlo en la dirección.
 *
 * `URLEncoder` convierte los espacios en "+", que en el texto de un mensaje se vería así
 * literalmente en algunos clientes, así que se dejan como %20.
 */
private fun codificarParaUrl(texto: String): String =
    java.net.URLEncoder.encode(texto, "UTF-8").replace("+", "%20")

fun pesosEnTexto(monto: Double): String {
    val simbolos = DecimalFormatSymbols(Locale.forLanguageTag("es-CO")).apply { groupingSeparator = '.' }
    return "$ " + DecimalFormat("#,##0", simbolos).format(round(monto))
}
