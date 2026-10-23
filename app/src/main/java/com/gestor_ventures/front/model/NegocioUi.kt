package com.gestor_ventures.front.model

import com.gestor_ventures.front.util.aIniciales

/** Negocio del usuario con el rol que tiene en él. Se usa en el marco de la app y en el menú. */
data class NegocioUi(
    val id: String,
    val nombre: String,
    val categoria: String,
    val rol: RolNegocio,
    /** HU-05. Color de marca en hexadecimal; null = el color por defecto de la app. */
    val colorMarca: String? = null,
) {
    /** "Dulce Antojo" → "DA". */
    val iniciales: String get() = nombre.aIniciales()
}
