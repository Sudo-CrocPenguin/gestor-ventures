package com.gestor_ventures.front.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HU-05. Lo delicado del color de marca: que el texto se lea encima de cualquier color que el
 * usuario elija. El mínimo 4.5 es el que pide la guía de accesibilidad WCAG.
 */
class ContrasteTest {

    private val minimo = 4.5

    @Test
    fun cualquierColorDeLaPaletaTieneTintaLegible() {
        ColorMarca.entries.forEach { marca ->
            val fondo = hexAColor(marca.hex) ?: hexAColor("#1B2A4A")!!
            val tinta = tintaSobre(fondo)

            assertTrue(
                "El texto sobre ${marca.name} no se lee: ${contraste(fondo, tinta)}",
                contraste(fondo, tinta) >= minimo,
            )
        }
    }

    @Test
    fun sobreUnColorOscuroLaTintaEsBlanca() {
        assertEquals(Color.White, tintaSobre(hexAColor("#1B2A4A")!!))
    }

    @Test
    fun sobreUnPastelClaroLaTintaNoEsBlanca() {
        val menta = hexAColor("#B8E0D2")!!

        assertNotEquals(Color.White, tintaSobre(menta))
    }

    @Test
    fun elFondoSuaveSigueSiendoLegible() {
        ColorMarca.entries.forEach { marca ->
            val color = hexAColor(marca.hex) ?: hexAColor("#1B2A4A")!!
            val suaveClaro = suave(color, oscuro = false)

            assertTrue(
                "El texto sobre el fondo suave de ${marca.name} no se lee",
                contraste(suaveClaro, tintaSobre(suaveClaro)) >= minimo,
            )
        }
    }

    @Test
    fun elContrasteDeExtremosEsElMaximo() {
        assertEquals(21.0, contraste(Color.White, Color.Black), 0.1)
        assertEquals(1.0, contraste(Color.White, Color.White), 0.01)
    }

    @Test
    fun hexAColor_soloAceptaHexadecimalesDeSeisCifras() {
        assertEquals(Color(0xFFB8E0D2), hexAColor("#B8E0D2"))
        assertEquals(Color(0xFFB8E0D2), hexAColor("b8e0d2"))
        assertNull(hexAColor(null))
        assertNull(hexAColor("#123"))
        assertNull(hexAColor("no es un color"))
    }

    @Test
    fun colorMarcaDeHex_devuelveElPredeterminadoSiNoLoConoce() {
        assertEquals(ColorMarca.Menta, colorMarcaDeHex("#B8E0D2"))
        assertEquals(ColorMarca.Sistema, colorMarcaDeHex(null))
        assertEquals(ColorMarca.Sistema, colorMarcaDeHex("#123456"))
    }
}
