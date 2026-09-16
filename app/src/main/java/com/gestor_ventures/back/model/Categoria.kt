package com.gestor_ventures.back.model

/**
 * HU-15. Etiqueta propia del negocio para clasificar en qué se va la plata.
 *
 * Cada negocio tiene las suyas: "Insumos" de una repostería no es la misma cuenta que la de
 * otro negocio, aunque se llame igual.
 */
data class Categoria(
    val id: Long,
    val nombre: String,
    val tipo: TipoCategoria,
)

/**
 * HU-15. Un gasto es plata que se va por funcionar (el transporte, la papelería); un costo es
 * plata que se va por producir lo que se vende (los insumos de una torta). Se separan porque
 * el margen solo mira los costos.
 */
enum class TipoCategoria { GASTO, COSTO }

/** Reglas que debe cumplir una categoría (HU-15). */
enum class ErrorCategoria {
    NombreVacio,
    NombreMuyLargo,
    NombreRepetido,
}
