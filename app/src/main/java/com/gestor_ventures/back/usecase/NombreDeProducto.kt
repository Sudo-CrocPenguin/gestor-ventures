package com.gestor_ventures.back.usecase

import java.text.Normalizer

/**
 * HU-13. Cuándo dos textos escritos a mano son el mismo producto.
 *
 * La venta y el costo guardan el producto como texto libre, así que el mismo producto entra
 * escrito de formas distintas: "Torta de Maracuyá" hoy y "torta de maracuya" mañana. Para
 * cruzarlos se compara una versión normalizada —sin mayúsculas, sin tildes y sin espacios de
 * sobra—, pero lo que se le muestra al usuario es siempre como él lo escribió.
 *
 * Es una convención, no una verdad: "Torta grande" y "Torta" siguen siendo productos distintos.
 * Si algún día hace falta más precisión, lo que toca es una tabla de productos, no una regla
 * más astuta acá.
 */
fun claveDeProducto(productoServicio: String): String =
    Normalizer.normalize(productoServicio.trim().lowercase(), Normalizer.Form.NFD)
        .replace(MarcasDeAcento, "")
        .replace(EspaciosSeguidos, " ")

private val MarcasDeAcento = "\\p{Mn}+".toRegex()

private val EspaciosSeguidos = "\\s+".toRegex()
