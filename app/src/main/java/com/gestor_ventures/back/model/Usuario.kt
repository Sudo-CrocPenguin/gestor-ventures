package com.gestor_ventures.back.model

/**
 * HU-01 a HU-04. El emprendedor que está usando la app, tal como lo entiende `front/`.
 *
 * Solo lleva el perfil: lo de autenticación —hash, tokens, códigos de recuperación— se queda en
 * la entidad de Room y no sale de `back/`. Una pantalla no tiene por qué poder leer eso.
 */
data class Usuario(
    val id: Long,
    val nombre: String,
    val correo: String,
    val fotoPerfilUrl: String? = null,
    val telefono: String? = null,
) {
    /**
     * Cómo llamarlo cuando hay poco espacio: "Sebastián Orrego" → "Sebastián". El saludo del
     * inicio tutea, y tutear con nombre y apellido suena a carta del banco.
     */
    val primerNombre: String get() = nombre.trim().substringBefore(' ')
}
