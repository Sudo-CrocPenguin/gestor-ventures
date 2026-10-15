package com.gestor_ventures.front.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FormattersTest {

    @Test
    fun formatPesos_usaPuntoComoSeparadorDeMiles() {
        assertEquals("$ 148.500", formatPesos(148_500.0))
        assertEquals("$ 1.250.000", formatPesos(1_250_000.0))
    }

    @Test
    fun formatPesos_noMuestraDecimales() {
        assertEquals("$ 0", formatPesos(0.0))
        assertEquals("$ 1.000", formatPesos(999.6))
    }

    @Test
    fun formatLongDate_enEspanolConMayusculaInicial() {
        assertEquals("Miércoles 2 de septiembre", formatLongDate(LocalDate.of(2026, 9, 2)))
    }

    @Test
    fun formatHour_sinCeroInicial() {
        assertEquals("8:00", formatHour(LocalTime.of(8, 0)))
        assertEquals("13:30", formatHour(LocalTime.of(13, 30)))
    }
}
