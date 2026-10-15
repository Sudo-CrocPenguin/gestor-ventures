package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorCosto
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.CostoDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

/** HU-13. Reglas de un costo antes de que llegue a la base de datos. */
class CostoRepositoryTest {

    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val dao = CostoDaoFalso()
    private val repository = CostoRepository(dao, Reloj { ahora })

    private val negocioId = 1L

    private suspend fun registrar(
        producto: String = "Torta de chocolate",
        monto: Double = 18_000.0,
        fecha: LocalDateTime = ahora,
        categoriaId: Long? = null,
    ) = repository.registrarCosto(negocioId, producto, monto, fecha, categoriaId)

    @Test
    fun registrarCosto_loGuarda() = runTest {
        assertNull(registrar())

        val costo = repository.costosDelMes(negocioId).first().single()
        assertEquals("Torta de chocolate", costo.productoServicio)
        assertEquals(18_000.0, costo.monto, 0.001)
    }

    @Test
    fun elProductoEsObligatorioYSeGuardaSinEspaciosDeSobra() = runTest {
        assertEquals(ErrorCosto.ProductoVacio, registrar(producto = "   "))

        registrar(producto = "  Torta  ")
        assertEquals("Torta", repository.costosDelMes(negocioId).first().single().productoServicio)
    }

    @Test
    fun elMontoTieneQueSerPositivo() = runTest {
        assertEquals(ErrorCosto.MontoNoPositivo, registrar(monto = 0.0))
        assertEquals(ErrorCosto.MontoNoPositivo, registrar(monto = -5_000.0))
        assertEquals(emptyList<Any>(), repository.costosDelMes(negocioId).first())
    }

    @Test
    fun noSePuedeRegistrarUnCostoDeManana() = runTest {
        assertEquals(ErrorCosto.FechaEnElFuturo, registrar(fecha = ahora.plusDays(1)))
    }

    @Test
    fun unCostoDeLaSemanaPasadaSiSePuede() = runTest {
        assertNull(registrar(fecha = ahora.minusDays(7)))
    }

    @Test
    fun elTotalDelMesSumaSoloLoDeEsteMes() = runTest {
        registrar(monto = 18_000.0)
        registrar(monto = 12_000.0, fecha = ahora.withDayOfMonth(1))
        registrar(monto = 99_000.0, fecha = ahora.minusMonths(1))

        assertEquals(30_000.0, repository.totalDelMes(negocioId).first(), 0.001)
    }

    @Test
    fun elMesPasadoSePuedeConsultarAparte() = runTest {
        registrar(monto = 99_000.0, fecha = ahora.minusMonths(1))

        val mesPasado = YearMonth.from(ahora.minusMonths(1))
        assertEquals(99_000.0, repository.totalDelMes(negocioId, mesPasado).first(), 0.001)
    }

    @Test
    fun elTotalPorCategoriaAgrupaYDejaAparteLoSinClasificar() = runTest {
        registrar(monto = 12_000.0, categoriaId = 1L)
        registrar(monto = 8_000.0, categoriaId = 1L)
        registrar(monto = 30_000.0, categoriaId = 2L)
        registrar(monto = 5_000.0, categoriaId = null)

        val totales = repository.totalPorCategoriaDelMes(negocioId).first()

        assertEquals(listOf(2L, 1L, null), totales.map { it.categoriaId })
        assertEquals(listOf(30_000.0, 20_000.0, 5_000.0), totales.map { it.total })
    }

    @Test
    fun editarCosto_cambiaLoQueSeCorrigeYNoCreaOtro() = runTest {
        registrar(producto = "Torta", monto = 18_000.0)
        val costo = repository.costosDelMes(negocioId).first().single()

        repository.editarCosto(costo.id, "Torta de chocolate", 20_000.0, ahora, categoriaId = 3L)

        val editado = repository.costosDelMes(negocioId).first().single()
        assertEquals(costo.id, editado.id)
        assertEquals("Torta de chocolate", editado.productoServicio)
        assertEquals(20_000.0, editado.monto, 0.001)
        assertEquals(3L, editado.categoriaId)
    }

    @Test
    fun editarCosto_exigeLoMismoQueRegistrarlo() = runTest {
        registrar(producto = "Torta", monto = 18_000.0)
        val costo = repository.costosDelMes(negocioId).first().single()

        assertEquals(
            ErrorCosto.MontoNoPositivo,
            repository.editarCosto(costo.id, "Torta", 0.0, ahora),
        )
        // El costo original queda intacto: un intento inválido no daña lo que ya estaba bien.
        assertEquals(18_000.0, repository.costosDelMes(negocioId).first().single().monto, 0.001)
    }

    @Test
    fun editarUnCostoQueYaNoExisteNoRompeNada() = runTest {
        assertNull(repository.editarCosto(99L, "Torta", 18_000.0, ahora))
    }

    @Test
    fun eliminarCosto_loSacaDeLaLista() = runTest {
        registrar()
        val costo = repository.costosDelMes(negocioId).first().single()

        repository.eliminarCosto(costo.id)

        assertEquals(emptyList<Any>(), repository.costosDelMes(negocioId).first())
    }
}
