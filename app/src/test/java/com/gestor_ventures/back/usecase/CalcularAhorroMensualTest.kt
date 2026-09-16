package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.Reloj
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** HU-08. El mensaje "necesitas apartar $X al mes" del onboarding. */
class CalcularAhorroMensualTest {

    private val hoy = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val calcular = CalcularAhorroMensual(Reloj { hoy })

    @Test
    fun repartePorLosMesesQueFaltan() {
        // Del 16 de septiembre al 31 de diciembre hay 106 días: unos 4 meses.
        val porMes = calcular(2_000_000.0, LocalDate.of(2026, 12, 31))

        assertEquals(500_000.0, porMes ?: 0.0, 1.0)
    }

    @Test
    fun menosDeUnMesSeApartaDeUnaVez() {
        val porMes = calcular(300_000.0, LocalDate.of(2026, 9, 30))

        assertEquals(300_000.0, porMes ?: 0.0, 1.0)
    }

    @Test
    fun sinMontoNoHayNadaQueCalcular() {
        assertNull(calcular(0.0, LocalDate.of(2026, 12, 31)))
        assertNull(calcular(-5.0, LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun laFechaLimiteDebeSerPosteriorAHoy() {
        assertNull(calcular(1_000_000.0, LocalDate.of(2026, 9, 16)))
        assertNull(calcular(1_000_000.0, LocalDate.of(2026, 9, 15)))
    }
}
