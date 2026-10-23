package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.db.dao.CostoDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-13. El margen de cada producto: lo vendido menos lo que costó producirlo.
 *
 * Lo que de verdad se fija acá es qué cuenta como "el mismo producto", porque en venta y costo
 * el nombre se escribe a mano.
 */
class CalcularMargenTest {

    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val reloj = Reloj { ahora }

    private val ventaDao = VentaDaoFalso()
    private val costoDao = CostoDaoFalso()
    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)
    private val calcularMargen = CalcularMargen(ventaRepository, costoRepository)

    private val negocioId = 1L

    private suspend fun vender(
        producto: String?,
        monto: Double,
        tipo: TipoRegistroVenta = TipoRegistroVenta.DETALLADO,
        fecha: LocalDateTime = ahora,
    ) = ventaRepository.registrarVenta(
        negocioId = negocioId,
        tipoRegistro = tipo,
        monto = monto,
        fechaHora = fecha,
        productoServicio = producto,
        metodoPago = com.gestor_ventures.back.model.MetodoPago.EFECTIVO,
    )

    private suspend fun costear(producto: String, monto: Double, fecha: LocalDateTime = ahora) =
        costoRepository.registrarCosto(negocioId, producto, monto, fecha)

    @Test
    fun elMargenEsLoVendidoMenosLoCosteado() = runTest {
        vender("Torta de chocolate", 45_000.0)
        costear("Torta de chocolate", 18_000.0)

        val margen = calcularMargen(negocioId).first().single()

        assertEquals("Torta de chocolate", margen.productoServicio)
        assertEquals(45_000.0, margen.vendido, 0.001)
        assertEquals(18_000.0, margen.costeado, 0.001)
        assertEquals(27_000.0, margen.margen, 0.001)
        assertEquals(60.0, margen.porcentaje ?: 0.0, 0.001)
    }

    @Test
    fun elMismoProductoEscritoDistintoEsElMismoProducto() = runTest {
        // Mayúsculas, tildes y espacios de sobra: la gente escribe a mano.
        vender("Torta de Maracuyá", 45_000.0)
        vender("  torta de maracuya  ", 45_000.0)
        costear("TORTA DE MARACUYÁ", 18_000.0)

        val margenes = calcularMargen(negocioId).first()

        assertEquals(1, margenes.size)
        assertEquals(90_000.0, margenes.single().vendido, 0.001)
    }

    @Test
    fun productosDistintosNoSeMezclan() = runTest {
        vender("Torta", 45_000.0)
        vender("Torta grande", 60_000.0)
        costear("Torta", 18_000.0)
        costear("Torta grande", 30_000.0)

        val margenes = calcularMargen(negocioId).first()

        assertEquals(2, margenes.size)
    }

    @Test
    fun elNombreQueSeMuestraEsComoLoEscribioElUsuario() = runTest {
        vender("Torta de Maracuyá", 45_000.0)
        costear("torta de maracuya", 18_000.0)

        assertEquals("Torta de Maracuyá", calcularMargen(negocioId).first().single().productoServicio)
    }

    @Test
    fun unProductoSinVentasDaMargenNegativo() = runTest {
        // Compró los insumos y todavía no ha vendido: está en rojo, y hay que decirlo.
        costear("Torta de chocolate", 18_000.0)

        val margen = calcularMargen(negocioId).first().single()

        assertEquals(0.0, margen.vendido, 0.001)
        assertEquals(-18_000.0, margen.margen, 0.001)
        assertNull(margen.porcentaje)
    }

    @Test
    fun unProductoSinCostosNoAparece() = runTest {
        // Sin costos parecería tener 100% de margen, y eso no es un margen: es una pregunta
        // sin responder.
        vender("Torta de chocolate", 45_000.0)

        assertTrue(calcularMargen(negocioId).first().isEmpty())
    }

    @Test
    fun laVentaRapidaNoSeLeAtribuyeANingunProducto() = runTest {
        costear("Torta de chocolate", 18_000.0)
        vender(producto = null, monto = 150_000.0, tipo = TipoRegistroVenta.RAPIDO)

        val margen = calcularMargen(negocioId).first().single()

        assertEquals(0.0, margen.vendido, 0.001)
    }

    @Test
    fun loQueEstaEnRojoVaPrimero() = runTest {
        vender("Torta", 45_000.0)
        costear("Torta", 18_000.0)
        costear("Galletas", 20_000.0)
        vender("Galletas", 5_000.0)

        val margenes = calcularMargen(negocioId).first()

        assertEquals(listOf("Galletas", "Torta"), margenes.map { it.productoServicio })
    }

    @Test
    fun soloCuentaElMesQueSePide() = runTest {
        vender("Torta", 45_000.0)
        costear("Torta", 18_000.0)
        vender("Torta", 99_000.0, fecha = ahora.minusMonths(1))
        costear("Torta", 40_000.0, fecha = ahora.minusMonths(1))

        val margen = calcularMargen(negocioId).first().single()

        assertEquals(45_000.0, margen.vendido, 0.001)
        assertEquals(18_000.0, margen.costeado, 0.001)
    }

    @Test
    fun sinCostosNoHayNadaQueMostrar() = runTest {
        assertTrue(calcularMargen(negocioId).first().isEmpty())
    }
}
