package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorVenta
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoVenta
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.db.dao.VentaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-11 y HU-12. Reglas de una venta antes de que llegue a la base de datos.
 *
 * El reloj está fijo para que "hoy" y "el futuro" signifiquen siempre lo mismo.
 */
class VentaRepositoryTest {

    private val ahora = LocalDateTime.of(2026, 9, 16, 15, 30)
    private val dao = VentaDaoFalso()
    private val repository = VentaRepository(dao, Reloj { ahora })

    private val negocioId = 1L

    @Test
    fun registraLaVentaDetalladaYDevuelveSuId() = runTest {
        val resultado = registrarDetallada(monto = 25_000.0)

        assertEquals(ResultadoVenta.Exito(1L), resultado)
        assertEquals(1, dao.ventas.value.size)
    }

    @Test
    fun laVentaSeGuardaConLaHoraDelRelojSiNadieLaIndica() = runTest {
        registrarDetallada(monto = 25_000.0)

        assertEquals(ahora, dao.ventas.value.first().fechaHora)
    }

    @Test
    fun elMontoTieneQueSerPositivo() = runTest {
        assertEquals(invalido(ErrorVenta.MontoNoPositivo), registrarDetallada(monto = 0.0))
        assertEquals(invalido(ErrorVenta.MontoNoPositivo), registrarDetallada(monto = -5_000.0))
        assertTrue(dao.ventas.value.isEmpty())
    }

    @Test
    fun laVentaDetalladaNecesitaSaberQueSeVendio() = runTest {
        assertEquals(
            invalido(ErrorVenta.ProductoVacio),
            registrarDetallada(monto = 25_000.0, producto = "   "),
        )
    }

    @Test
    fun laVentaDetalladaNecesitaMetodoDePago() = runTest {
        val resultado = repository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = 25_000.0,
            productoServicio = "Torta",
            metodoPago = null,
        )

        assertEquals(invalido(ErrorVenta.MetodoPagoFaltante), resultado)
    }

    @Test
    fun laVentaRapidaSoloNecesitaElMonto() = runTest {
        val resultado = repository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.RAPIDO,
            monto = 8_000.0,
        )

        assertEquals(ResultadoVenta.Exito(1L), resultado)
    }

    @Test
    fun laVentaRapidaDescartaLoQueNoLeCorresponde() = runTest {
        repository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.RAPIDO,
            monto = 8_000.0,
            productoServicio = "Torta",
            metodoPago = MetodoPago.TARJETA,
            clienteId = 7L,
        )

        val guardada = dao.ventas.value.first()
        assertNull(guardada.productoServicio)
        assertNull(guardada.metodoPago)
        assertNull(guardada.clienteId)
    }

    @Test
    fun noSePuedeRegistrarUnaVentaConFechaFutura() = runTest {
        val resultado = registrarDetallada(monto = 25_000.0, fechaHora = ahora.plusMinutes(1))

        assertEquals(invalido(ErrorVenta.FechaEnElFuturo), resultado)
    }

    @Test
    fun unaVentaDeHaceUnRatoSiSePuedeRegistrar() = runTest {
        val resultado = registrarDetallada(monto = 25_000.0, fechaHora = ahora.minusHours(3))

        assertEquals(ResultadoVenta.Exito(1L), resultado)
    }

    @Test
    fun elProductoSeGuardaSinEspaciosDeSobra() = runTest {
        registrarDetallada(monto = 25_000.0, producto = "  Torta de chocolate  ")

        assertEquals("Torta de chocolate", dao.ventas.value.first().productoServicio)
    }

    @Test
    fun elResumenDelDiaSumaYCuentaSoloLoDeHoy() = runTest {
        registrarDetallada(monto = 25_000.0, fechaHora = ahora.minusHours(6))
        registrarDetallada(monto = 15_000.0, fechaHora = ahora)
        registrarDetallada(monto = 99_000.0, fechaHora = ahora.minusDays(1))

        val resumen = repository.resumenDelDia(negocioId).first()

        assertEquals(40_000.0, resumen.total, 0.001)
        assertEquals(2, resumen.cantidad)
    }

    @Test
    fun unDiaSinVentasResumeEnCero() = runTest {
        val resumen = repository.resumenDelDia(negocioId).first()

        assertEquals(0.0, resumen.total, 0.001)
        assertEquals(0, resumen.cantidad)
    }

    @Test
    fun lasVentasDelDiaLleganComoModeloDeDominio() = runTest {
        registrarDetallada(monto = 25_000.0, producto = "Torta")

        val venta = repository.ventasDelDia(negocioId).first().single()

        assertEquals(TipoRegistroVenta.DETALLADO, venta.tipoRegistro)
        assertEquals(MetodoPago.EFECTIVO, venta.metodoPago)
        assertEquals("Torta", venta.productoServicio)
        assertEquals(25_000.0, venta.monto, 0.001)
    }

    @Test
    fun elCorteEntreAyerYHoyEsLaMedianoche() = runTest {
        // Un minuto separa estas dos ventas, pero caen en días distintos.
        registrarDetallada(monto = 7_000.0, fechaHora = ahora.toLocalDate().atStartOfDay())
        registrarDetallada(
            monto = 99_000.0,
            fechaHora = ahora.toLocalDate().minusDays(1).atTime(23, 59, 59),
        )

        val resumen = repository.resumenDelDia(negocioId).first()

        assertEquals(7_000.0, resumen.total, 0.001)
        assertEquals(1, resumen.cantidad)
    }

    private fun invalido(error: ErrorVenta) = ResultadoVenta.Invalido(error)

    private suspend fun registrarDetallada(
        monto: Double,
        producto: String = "Torta de chocolate",
        fechaHora: LocalDateTime = ahora,
    ) = repository.registrarVenta(
        negocioId = negocioId,
        tipoRegistro = TipoRegistroVenta.DETALLADO,
        monto = monto,
        fechaHora = fechaHora,
        productoServicio = producto,
        metodoPago = MetodoPago.EFECTIVO,
    )
}
