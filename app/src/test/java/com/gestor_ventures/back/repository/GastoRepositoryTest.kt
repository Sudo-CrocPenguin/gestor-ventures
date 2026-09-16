package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorGasto
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.GastoDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * HU-14. Reglas de un gasto antes de que llegue a la base de datos.
 *
 * El reloj está fijo para que "hoy" y "el futuro" signifiquen siempre lo mismo.
 */
class GastoRepositoryTest {

    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy: LocalDate = ahora.toLocalDate()
    private val dao = GastoDaoFalso()
    private val repository = GastoRepository(dao, Reloj { ahora })

    private val negocioId = 1L

    private suspend fun registrar(
        descripcion: String = "Domicilio",
        monto: Double = 12_000.0,
        fecha: LocalDate = hoy,
        categoriaId: Long? = null,
    ) = repository.registrarGasto(negocioId, descripcion, monto, fecha, categoriaId)

    @Test
    fun registrarGasto_loGuarda() = runTest {
        assertNull(registrar(descripcion = "Domicilio", monto = 12_000.0))

        val gasto = repository.gastosDelMes(negocioId).first().single()
        assertEquals("Domicilio", gasto.descripcion)
        assertEquals(12_000.0, gasto.monto, 0.001)
        assertEquals(hoy, gasto.fecha)
    }

    @Test
    fun laDescripcionEsObligatoriaYSeGuardaSinEspaciosDeSobra() = runTest {
        assertEquals(ErrorGasto.DescripcionVacia, registrar(descripcion = "   "))

        registrar(descripcion = "  Domicilio  ")
        assertEquals("Domicilio", repository.gastosDelMes(negocioId).first().single().descripcion)
    }

    @Test
    fun elMontoTieneQueSerPositivo() = runTest {
        assertEquals(ErrorGasto.MontoNoPositivo, registrar(monto = 0.0))
        assertEquals(ErrorGasto.MontoNoPositivo, registrar(monto = -5_000.0))
        assertEquals(emptyList<Any>(), repository.gastosDelMes(negocioId).first())
    }

    @Test
    fun noSePuedeRegistrarUnGastoDeManana() = runTest {
        assertEquals(ErrorGasto.FechaEnElFuturo, registrar(fecha = hoy.plusDays(1)))
    }

    @Test
    fun unGastoDeLaSemanaPasadaSiSePuede() = runTest {
        // La gente registra el gasto días después de haberlo pagado.
        assertNull(registrar(fecha = hoy.minusDays(7)))
    }

    @Test
    fun elGastoPuedeQuedarSinCategoria() = runTest {
        registrar(categoriaId = null)

        assertNull(repository.gastosDelMes(negocioId).first().single().categoriaId)
    }

    @Test
    fun elTotalDelMesSumaSoloLoDeEsteMes() = runTest {
        registrar(monto = 12_000.0, fecha = hoy)
        registrar(monto = 8_000.0, fecha = hoy.withDayOfMonth(1))
        registrar(monto = 99_000.0, fecha = hoy.minusMonths(1))

        assertEquals(20_000.0, repository.totalDelMes(negocioId).first(), 0.001)
    }

    @Test
    fun elMesPasadoSePuedeConsultarAparte() = runTest {
        registrar(monto = 99_000.0, fecha = hoy.minusMonths(1))

        val mesPasado = YearMonth.from(hoy.minusMonths(1))
        assertEquals(99_000.0, repository.totalDelMes(negocioId, mesPasado).first(), 0.001)
    }

    @Test
    fun unMesSinGastosTotalizaCero() = runTest {
        assertEquals(0.0, repository.totalDelMes(negocioId).first(), 0.001)
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
    fun editarGasto_cambiaLoQueSeCorrigeYNoCreaOtro() = runTest {
        registrar(descripcion = "Domicilio", monto = 12_000.0)
        val gasto = repository.gastosDelMes(negocioId).first().single()

        repository.editarGasto(
            gastoId = gasto.id,
            descripcion = "Domicilio del sábado",
            monto = 15_000.0,
            fecha = hoy,
            categoriaId = 3L,
        )

        val editado = repository.gastosDelMes(negocioId).first().single()
        assertEquals(gasto.id, editado.id)
        assertEquals("Domicilio del sábado", editado.descripcion)
        assertEquals(15_000.0, editado.monto, 0.001)
        assertEquals(3L, editado.categoriaId)
    }

    @Test
    fun editarGasto_exigeLoMismoQueRegistrarlo() = runTest {
        registrar(descripcion = "Domicilio", monto = 12_000.0)
        val gasto = repository.gastosDelMes(negocioId).first().single()

        assertEquals(
            ErrorGasto.MontoNoPositivo,
            repository.editarGasto(gasto.id, "Domicilio", 0.0, hoy),
        )
        assertEquals(
            ErrorGasto.FechaEnElFuturo,
            repository.editarGasto(gasto.id, "Domicilio", 15_000.0, hoy.plusDays(1)),
        )
        // El gasto original queda intacto: un intento inválido no daña lo que ya estaba bien.
        assertEquals(12_000.0, repository.gastosDelMes(negocioId).first().single().monto, 0.001)
    }

    @Test
    fun editarUnGastoQueYaNoExisteNoRompeNada() = runTest {
        assertNull(repository.editarGasto(99L, "Domicilio", 12_000.0, hoy))
    }

    @Test
    fun eliminarGasto_loSacaDeLaLista() = runTest {
        registrar()
        val gasto = repository.gastosDelMes(negocioId).first().single()

        repository.eliminarGasto(gasto.id)

        assertEquals(emptyList<Any>(), repository.gastosDelMes(negocioId).first())
    }
}
