package com.gestor_ventures.front.ui.inicio

import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.model.Venta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** HU-16. La mini gráfica del resumen: las ventas del día repartidas en franjas de tres horas. */
class TendenciaVentasTest {

    private val dia: LocalDate = LocalDate.of(2026, 9, 16)

    @Test
    fun sinVentasNoHayGraficaQueDibujar() {
        // Una línea plana en cero haría creer que el negocio no vendió nada.
        assertTrue(tendenciaPorFranja(emptyList()).isEmpty())
    }

    @Test
    fun cadaVentaCaeEnSuFranjaDeTresHoras() {
        val tendencia = tendenciaPorFranja(
            listOf(
                venta(hora = 0, monto = 1_000.0),
                venta(hora = 4, monto = 2_000.0),
                venta(hora = 23, monto = 3_000.0),
            ),
        )

        assertEquals(FranjasDelDia, tendencia.size)
        assertEquals(1_000.0, tendencia.first(), 0.001)
        assertEquals(2_000.0, tendencia[1], 0.001)
        assertEquals(3_000.0, tendencia.last(), 0.001)
    }

    @Test
    fun lasVentasDeLaMismaFranjaSeSuman() {
        val tendencia = tendenciaPorFranja(
            listOf(
                venta(hora = 9, monto = 10_000.0),
                venta(hora = 11, monto = 5_000.0),
            ),
        )

        assertEquals(15_000.0, tendencia[3], 0.001)
        assertEquals(15_000.0, tendencia.sum(), 0.001)
    }

    private fun venta(hora: Int, monto: Double) = Venta(
        id = 0,
        negocioId = 1,
        tipoRegistro = TipoRegistroVenta.RAPIDO,
        monto = monto,
        fechaHora = LocalDateTime.of(dia, java.time.LocalTime.of(hora, 0)),
    )
}
