package com.gestor_ventures.front.model

import com.gestor_ventures.front.util.aIniciales

/** Usuario que tiene la sesión abierta, tal como lo muestra la UI. */
data class UsuarioUi(
    val nombre: String,
    val correo: String,
    val fotoPerfil: String? = null,
) {
    /** "Sebastián Orrego" → "SO". */
    val iniciales: String get() = nombre.aIniciales()
}
