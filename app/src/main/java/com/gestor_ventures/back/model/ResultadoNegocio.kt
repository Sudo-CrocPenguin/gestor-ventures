package com.gestor_ventures.back.model

/** Resultado de crear o editar un negocio (HU-05, HU-10). */
sealed interface ResultadoNegocio {

    data class Exito(val negocioId: Long) : ResultadoNegocio

    data class Invalido(val error: ErrorNegocio) : ResultadoNegocio
}

/**
 * Reglas que un negocio debe cumplir. El repositorio las valida siempre, aunque la pantalla
 * ya avise antes: así ninguna ruta de la app puede guardar datos inválidos.
 *
 * Son errores, no mensajes: el texto que ve el usuario lo pone `front/`.
 */
enum class ErrorNegocio {
    NombreVacio,
    NombreMuyLargo,
    CategoriaVacia,
    PorcentajeFueraDeRango,
    NegocioNoExiste,
}
