package com.gestor_ventures.back.model

import java.time.LocalDateTime

/**
 * Reloj de la app. Existe para que los repositorios no llamen directo a
 * [LocalDateTime.now]: así las pruebas pueden fijar la hora y verificar qué se guardó.
 */
fun interface Reloj {
    fun ahora(): LocalDateTime
}
