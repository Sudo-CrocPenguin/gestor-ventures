package com.gestor_ventures.front.util

import org.junit.Assert.assertEquals
import org.junit.Test

class InicialesTest {

    @Test
    fun aIniciales_tomaLasDosPrimerasPalabras() {
        assertEquals("DA", "Dulce Antojo".aIniciales())
        assertEquals("SO", "Sebastián Orrego Urrea".aIniciales())
    }

    @Test
    fun aIniciales_conUnaPalabraDevuelveUnaLetra() {
        assertEquals("V", "Valentina".aIniciales())
    }

    @Test
    fun aIniciales_ignoraEspaciosDeMas() {
        assertEquals("BP", "  Bella   Piel  ".aIniciales())
        assertEquals("", "   ".aIniciales())
    }
}
