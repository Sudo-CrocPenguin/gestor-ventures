package com.gestor_ventures.back.model

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
