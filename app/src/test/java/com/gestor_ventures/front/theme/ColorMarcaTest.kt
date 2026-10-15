package com.gestor_ventures.front.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * HU-05. El color de marca viaja como hexadecimal entre la pantalla y la base de datos, así que
 * la ida y la vuelta tienen que dar siempre lo mismo.
 */
class ColorMarcaTest {

    @Test
    fun hexAColor_soloAceptaHexadecimalesDeSeisCifras() {
        assertEquals(Color(0xFFB8E0D2), hexAColor("#B8E0D2"))
        assertEquals(Color(0xFFB8E0D2), hexAColor("b8e0d2"))
        assertNull(hexAColor(null))
        assertNull(hexAColor("#123"))
        assertNull(hexAColor("no es un color"))
    }

    @Test
    fun colorAHex_escribeConNumeralYEnMayusculas() {
        assertEquals("#B8E0D2", colorAHex(Color(0xFFB8E0D2)))
        assertEquals("#000000", colorAHex(Color.Black))
        assertEquals("#FFFFFF", colorAHex(Color.White))
    }

    @Test
    fun elColorElegidoEnElSelectorLlegaIntactoALaBaseDeDatos() {
        for (tono in 0 until 360 step 15) {
            for (luz in listOf(0.15f, 0.5f, 0.85f)) {
                val hex = colorAHex(deHsl(tono.toFloat(), 0.7f, luz))

                // Guardar y volver a leer no puede correr el color ni una cifra.
                assertEquals(hex, colorAHex(hexAColor(hex)!!))
            }
        }
    }

    @Test
    fun presetDeHex_reconoceLosAtajosYDelataAlPersonalizado() {
        assertEquals(ColorMarca.Sistema, presetDeHex(null))
        assertEquals(ColorMarca.RosaCuarzo, presetDeHex("#F4C2C2"))
        assertEquals(ColorMarca.RosaCuarzo, presetDeHex("#f4c2c2"))
        assertEquals(ColorMarca.Salvia, presetDeHex("#CFE0C3"))
        // Un color propio no es ningún atajo: así sabe la pantalla que va en el último círculo.
        assertNull(presetDeHex("#123456"))
    }
}
