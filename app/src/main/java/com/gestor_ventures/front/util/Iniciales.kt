package com.gestor_ventures.front.util

/**
 * Iniciales para los avatares: las dos primeras palabras del nombre.
 * "Dulce Antojo" → "DA", "Sebastián" → "S".
 */
fun String.aIniciales(): String = trim()
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
