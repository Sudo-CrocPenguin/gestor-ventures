package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorObligacion
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.ObligacionDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** HU-07. Reglas de una obligación y qué cuenta como próxima a vencer. */
class ObligacionRepositoryTest {

    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy: LocalDate = ahora.toLocalDate()
    private val dao = ObligacionDaoFalso()
    private val repository = ObligacionRepository(dao, Reloj { ahora })

    private val negocioId = 1L

    private suspend fun registrar(
        nombre: String = "Cuota del horno",
        monto: Double = 250_000.0,
        vencimiento: LocalDate = hoy.plusDays(10),
    ) = repository.registrarObligacion(negocioId, nombre, monto, vencimiento)

    @Test
    fun registrarObligacion_laGuardaPendiente() = runTest {
        assertNull(registrar())

        val obligacion = repository.obligacionesDeNegocio(negocioId).first().single()
        assertEquals("Cuota del horno", obligacion.nombre)
        assertEquals(250_000.0, obligacion.monto, 0.001)
        assertFalse(obligacion.pagada)
    }

    @Test
    fun elNombreEsObligatorioYSeGuardaSinEspaciosDeSobra() = runTest {
        assertEquals(ErrorObligacion.NombreVacio, registrar(nombre = "   "))

        registrar(nombre = "  Préstamo  ")
        assertEquals("Préstamo", repository.obligacionesDeNegocio(negocioId).first().single().nombre)
    }

    @Test
    fun elMontoTieneQueSerPositivo() = runTest {
        assertEquals(ErrorObligacion.MontoNoPositivo, registrar(monto = 0.0))
        assertEquals(ErrorObligacion.MontoNoPositivo, registrar(monto = -5_000.0))
        assertEquals(emptyList<Any>(), repository.obligacionesDeNegocio(negocioId).first())
    }

    @Test
    fun unaObligacionPuedeVencerEnElFuturo() = runTest {
        // Es el caso normal: una cuota que hay que pagar el otro mes.
        assertNull(registrar(vencimiento = hoy.plusMonths(1)))
    }

    @Test
    fun unaObligacionTambienPuedeEstarVencida() = runTest {
        // Registrar una deuda que ya se pasó de fecha es legítimo, y además urgente.
        assertNull(registrar(vencimiento = hoy.minusDays(10)))

        val obligacion = repository.obligacionesDeNegocio(negocioId).first().single()
        assertTrue(obligacion.estaVencida(hoy))
    }

    @Test
    fun lasProximasAVencerSonLasDeLaSemanaYLasVencidas() = runTest {
        registrar(nombre = "Vencida", vencimiento = hoy.minusDays(3))
        registrar(nombre = "Esta semana", vencimiento = hoy.plusDays(5))
        registrar(nombre = "En el límite", vencimiento = hoy.plusDays(7))
        registrar(nombre = "El otro mes", vencimiento = hoy.plusMonths(1))

        val proximas = repository.proximasAVencer(negocioId).first()

        assertEquals(
            listOf("Vencida", "Esta semana", "En el límite"),
            proximas.map { it.nombre },
        )
    }

    @Test
    fun unaObligacionPagadaYaNoEsProxima() = runTest {
        registrar(nombre = "Cuota", vencimiento = hoy.plusDays(2))
        val obligacion = repository.obligacionesDeNegocio(negocioId).first().single()

        repository.marcarPagada(obligacion.id, pagada = true)

        assertEquals(emptyList<Any>(), repository.proximasAVencer(negocioId).first())
    }

    @Test
    fun marcarPagada_ySePuedeDeshacer() = runTest {
        registrar()
        val obligacion = repository.obligacionesDeNegocio(negocioId).first().single()

        repository.marcarPagada(obligacion.id, pagada = true)
        assertTrue(repository.obligacionesDeNegocio(negocioId).first().single().pagada)

        // Si el usuario se equivocó, puede devolverla a pendiente.
        repository.marcarPagada(obligacion.id, pagada = false)
        assertFalse(repository.obligacionesDeNegocio(negocioId).first().single().pagada)
    }

    @Test
    fun elTotalPendienteNoCuentaLoQueYaSePago() = runTest {
        registrar(nombre = "Cuota", monto = 250_000.0)
        registrar(nombre = "Préstamo", monto = 100_000.0)
        val cuota = repository.obligacionesDeNegocio(negocioId).first().first { it.nombre == "Cuota" }

        repository.marcarPagada(cuota.id, pagada = true)

        assertEquals(100_000.0, repository.totalPendiente(negocioId).first(), 0.001)
    }

    @Test
    fun editarConservaSiEstabaPagada() = runTest {
        registrar(nombre = "Cuota", monto = 250_000.0)
        val obligacion = repository.obligacionesDeNegocio(negocioId).first().single()
        repository.marcarPagada(obligacion.id, pagada = true)

        // Corregirle el monto a una cuota ya pagada no la vuelve a deber.
        repository.editarObligacion(obligacion.id, "Cuota del horno", 260_000.0, hoy.plusDays(10))

        val editada = repository.obligacionesDeNegocio(negocioId).first().single()
        assertEquals(260_000.0, editada.monto, 0.001)
        assertTrue(editada.pagada)
    }

    @Test
    fun editarExigeLoMismoQueRegistrar() = runTest {
        registrar(nombre = "Cuota", monto = 250_000.0)
        val obligacion = repository.obligacionesDeNegocio(negocioId).first().single()

        assertEquals(
            ErrorObligacion.MontoNoPositivo,
            repository.editarObligacion(obligacion.id, "Cuota", 0.0, hoy),
        )
        assertEquals(250_000.0, repository.obligacionesDeNegocio(negocioId).first().single().monto, 0.001)
    }

    @Test
    fun editarUnaQueYaNoExisteNoRompeNada() = runTest {
        assertNull(repository.editarObligacion(99L, "Cuota", 250_000.0, hoy))
    }

    @Test
    fun eliminarLaSacaDeLaLista() = runTest {
        registrar()
        val obligacion = repository.obligacionesDeNegocio(negocioId).first().single()

        repository.eliminarObligacion(obligacion.id)

        assertEquals(emptyList<Any>(), repository.obligacionesDeNegocio(negocioId).first())
    }

    @Test
    fun losDiasParaVencerSeCuentanDesdeHoy() = runTest {
        registrar(vencimiento = hoy.plusDays(3))

        val obligacion = repository.obligacionesDeNegocio(negocioId).first().single()

        assertEquals(3L, obligacion.diasParaVencer(hoy))
        assertFalse(obligacion.estaVencida(hoy))
    }
}
