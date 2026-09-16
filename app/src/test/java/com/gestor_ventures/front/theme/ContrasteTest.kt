package com.gestor_ventures.front.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HU-05. Lo delicado del color de marca: que el texto se lea encima de cualquier color que el
 * usuario elija. Y desde que existe el selector personalizado, "cualquiera" es literal: no son
 * tres pasteles revisados a mano, es todo el espectro. El mínimo 4.5 es el que pide la guía de
 * accesibilidad WCAG.
 */
class ContrasteTest {

    private val minimo = 4.5

    /** Recorre el espectro como lo recorrería un dedo sobre el selector. */
    private fun cadaColorPosible(prueba: (Color) -> Unit) {
        for (tono in 0 until 360 step 15) {
            for (saturacion in listOf(0f, 0.25f, 0.6f, 1f)) {
                for (luz in listOf(0.05f, 0.2f, 0.4f, 0.5f, 0.65f, 0.8f, 0.95f)) {
                    prueba(deHsl(tono.toFloat(), saturacion, luz))
                }
            }
        }
    }

    @Test
    fun sobreCualquierColorDelSelectorHayTintaLegible() {
        cadaColorPosible { color ->
            val tinta = tintaSobre(color)

            assertTrue(
                "El texto sobre $color no se lee: ${contraste(color, tinta)}",
                contraste(color, tinta) >= minimo,
            )
        }
    }

    @Test
    fun cualquierColorDelSelectorSirveComoTextoSobreElFondoClaro() {
        val fondoClaro = Color.White

        cadaColorPosible { color ->
            val acento = acentoSobre(color, fondoClaro, oscuro = false)

            assertTrue(
                "El acento de $color no se lee sobre blanco: ${contraste(acento, fondoClaro)}",
                contraste(acento, fondoClaro) >= minimo,
            )
        }
    }

    @Test
    fun cualquierColorDelSelectorSirveComoTextoSobreElFondoOscuro() {
        val fondoOscuro = Color(0xFF12161F)

        cadaColorPosible { color ->
            val acento = acentoSobre(color, fondoOscuro, oscuro = true)

            assertTrue(
                "El acento de $color no se lee sobre el fondo oscuro",
                contraste(acento, fondoOscuro) >= minimo,
            )
        }
    }

    @Test
    fun elFondoSuaveDeCualquierColorSigueSiendoLegible() {
        cadaColorPosible { color ->
            listOf(true, false).forEach { oscuro ->
                val suave = suave(color, oscuro)

                assertTrue(
                    "El texto sobre el fondo suave de $color no se lee",
                    contraste(suave, tintaSobre(suave)) >= minimo,
                )
            }
        }
    }

    @Test
    fun sobreUnColorOscuroLaTintaEsBlanca() {
        assertEquals(Color.White, tintaSobre(hexAColor("#1B2A4A")!!))
    }

    @Test
    fun sobreUnPastelClaroLaTintaNoEsBlanca() {
        val pastel = hexAColor("#B8E0D2")!!

        assertNotEquals(Color.White, tintaSobre(pastel))
    }

    @Test
    fun elContrasteDeExtremosEsElMaximo() {
        assertEquals(21.0, contraste(Color.White, Color.Black), 0.1)
        assertEquals(1.0, contraste(Color.White, Color.White), 0.01)
    }
}
