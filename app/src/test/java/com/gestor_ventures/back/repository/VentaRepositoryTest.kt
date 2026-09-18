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
    fun laVentaRapidaSiGuardaLaNota() = runTest {
        repository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.RAPIDO,
            monto = 8_000.0,
            nota = "  Día de feria  ",
        )

        assertEquals("Día de feria", dao.ventas.value.first().nota)
    }

    @Test
    fun unaNotaEnBlancoSeGuardaComoNada() = runTest {
        repository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.RAPIDO,
            monto = 8_000.0,
            nota = "   ",
        )

        assertNull(dao.ventas.value.first().nota)
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

    // ---------- HU-17: corregir y borrar ----------

    @Test
    fun corregirUnaVentaLaCambiaEnSuSitioYNoCreaOtra() = runTest {
        registrarDetallada(monto = 25_000.0)

        repository.editarVenta(
            ventaId = 1L,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = 52_000.0,
            fechaHora = ahora,
            productoServicio = "Torta grande",
            metodoPago = MetodoPago.TRANSFERENCIA,
        )

        assertEquals(1, dao.ventas.value.size)
        val venta = dao.ventas.value.single()
        assertEquals(52_000.0, venta.monto, 0.001)
        assertEquals("Torta grande", venta.productoServicio)
    }

    @Test
    fun corregirValidaLoMismoQueRegistrar() = runTest {
        registrarDetallada(monto = 25_000.0)

        val enCero = repository.editarVenta(
            ventaId = 1L,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = 0.0,
            fechaHora = ahora,
            productoServicio = "Torta",
            metodoPago = MetodoPago.EFECTIVO,
        )
        val enElFuturo = repository.editarVenta(
            ventaId = 1L,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = 30_000.0,
            fechaHora = ahora.plusDays(1),
            productoServicio = "Torta",
            metodoPago = MetodoPago.EFECTIVO,
        )

        assertEquals(invalido(ErrorVenta.MontoNoPositivo), enCero)
        assertEquals(invalido(ErrorVenta.FechaEnElFuturo), enElFuturo)
        // La venta original queda intacta: un intento inválido no la daña.
        assertEquals(25_000.0, dao.ventas.value.single().monto, 0.001)
    }

    @Test
    fun pasarUnaVentaDeDetalladaARapidaSueltaLoQueYaNoAplica() = runTest {
        registrarDetallada(monto = 25_000.0)

        repository.editarVenta(
            ventaId = 1L,
            tipoRegistro = TipoRegistroVenta.RAPIDO,
            monto = 25_000.0,
            fechaHora = ahora,
            productoServicio = "Torta de chocolate",
            metodoPago = MetodoPago.EFECTIVO,
            clienteId = 7L,
        )

        // Dejarlos colgando sería guardar datos que la pantalla ya no muestra ni deja editar.
        val venta = dao.ventas.value.single()
        assertNull(venta.productoServicio)
        assertNull(venta.metodoPago)
        assertNull(venta.clienteId)
    }

    @Test
    fun corregirLaFechaSacaLaVentaDelDiaEnQueEstaba() = runTest {
        registrarDetallada(monto = 25_000.0)

        repository.editarVenta(
            ventaId = 1L,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = 25_000.0,
            fechaHora = ahora.minusDays(1),
            productoServicio = "Torta",
            metodoPago = MetodoPago.EFECTIVO,
        )

        // Registrar hoy una venta de ayer es de lo más común al cerrar la jornada.
        assertTrue(repository.ventasDelDia(negocioId, ahora.toLocalDate()).first().isEmpty())
        assertEquals(
            25_000.0,
            repository.resumenDelDia(negocioId, ahora.toLocalDate().minusDays(1)).first().total,
            0.001,
        )
    }

    @Test
    fun corregirUnaVentaQueYaNoExisteNoRompeNada() = runTest {
        val resultado = repository.editarVenta(
            ventaId = 99L,
            tipoRegistro = TipoRegistroVenta.DETALLADO,
            monto = 25_000.0,
            fechaHora = ahora,
            productoServicio = "Torta",
            metodoPago = MetodoPago.EFECTIVO,
        )

        // Se pudo borrar desde el historial mientras el formulario estaba abierto.
        assertEquals(ResultadoVenta.Exito(99L), resultado)
        assertTrue(dao.ventas.value.isEmpty())
    }

    @Test
    fun eliminarSacaLaVentaYSuPlataDeLosTotales() = runTest {
        registrarDetallada(monto = 25_000.0)
        registrarDetallada(monto = 15_000.0)

        repository.eliminarVenta(1L)

        assertEquals(1, dao.ventas.value.size)
        val resumen = repository.resumenDelDia(negocioId, ahora.toLocalDate()).first()
        assertEquals(15_000.0, resumen.total, 0.001)
        assertEquals(1, resumen.cantidad)
    }

    @Test
    fun eliminarUnaVentaQueYaNoExisteNoRompeNada() = runTest {
        registrarDetallada(monto = 25_000.0)

        repository.eliminarVenta(99L)

        assertEquals(1, dao.ventas.value.size)
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
