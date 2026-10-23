package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.DiferenciaDeCaja
import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.CajaDaoFalso
import com.gestor_ventures.db.enums.EstadoCaja
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-19, HU-21 y HU-22. Reglas de abrir y cerrar caja.
 *
 * Las dos que importan: que no se pueda abrir una caja encima de otra sin cerrar, y que la hora
 * de apertura la ponga el reloj y no el usuario.
 */
class CajaRepositoryTest {

    private val ahora = LocalDateTime.of(2026, 10, 7, 8, 0)
    private var momento = ahora
    private val reloj = Reloj { momento }

    private val dao = CajaDaoFalso()
    private val repository = CajaRepository(dao, reloj)

    private val negocioId = 1L
    private val otroNegocio = 2L

    private suspend fun abierta() = repository.cajaAbierta(negocioId).first()

    @Test
    fun abrirCajaDejaLaJornadaEnCursoConSuMonto() = runTest {
        val error = repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        assertNull(error)
        assertEquals(50_000.0, abierta()?.montoInicial ?: -1.0, 0.001)
    }

    @Test
    fun laHoraDeAperturaLaPoneElRelojYNoElUsuario() = runTest {
        momento = ahora.withHour(6).withMinute(45)

        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        assertEquals(ahora.withHour(6).withMinute(45), abierta()?.fechaHoraApertura)
    }

    @Test
    fun sinAbrirNadaNoHayJornadaEnCurso() = runTest {
        assertNull(abierta())
    }

    @Test
    fun arrancarSinEfectivoEsValido() = runTest {
        val error = repository.abrirCaja(negocioId, montoInicial = 0.0)

        assertNull(error)
        assertEquals(0.0, abierta()?.montoInicial ?: -1.0, 0.001)
    }

    @Test
    fun noSePuedeArrancarDebiendo() = runTest {
        val error = repository.abrirCaja(negocioId, montoInicial = -1_000.0)

        assertEquals(ErrorCaja.MontoNegativo, error)
        assertNull(abierta())
    }

    @Test
    fun noSePuedeAbrirUnaCajaEncimaDeOtra() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        val error = repository.abrirCaja(negocioId, montoInicial = 20_000.0)

        assertEquals(ErrorCaja.YaHayCajaAbierta, error)
    }

    @Test
    fun elSegundoIntentoNoPisaElMontoDelPrimero() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        repository.abrirCaja(negocioId, montoInicial = 20_000.0)

        // Si el rechazo dejara pasar el insert, la jornada arrancaría con un monto que nadie contó.
        assertEquals(50_000.0, abierta()?.montoInicial ?: -1.0, 0.001)
        assertEquals(1, dao.cajas.value.size)
    }

    @Test
    fun cadaNegocioLlevaSuPropiaJornada() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        val error = repository.abrirCaja(otroNegocio, montoInicial = 30_000.0)

        assertNull(error)
        assertNotNull(abierta())
        assertEquals(30_000.0, repository.cajaAbierta(otroNegocio).first()?.montoInicial ?: -1.0, 0.001)
    }

    // ---------- HU-21 y HU-22: cerrar ----------

    @Test
    fun cerrarLaCajaDejaDeTenerJornadaEnCurso() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        val error = repository.cerrarCaja(negocioId, montoReal = 141_000.0)

        assertNull(error)
        assertNull(abierta())
    }

    @Test
    fun laHoraDeCierreLaPoneElRelojYNoElUsuario() = runTest {
        momento = ahora.withHour(8)
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        momento = ahora.withHour(14).withMinute(30)
        repository.cerrarCaja(negocioId, montoReal = 141_000.0)

        val cerrada = dao.cajas.value.single()
        assertEquals(ahora.withHour(14).withMinute(30), cerrada.fechaHoraCierre)
        assertEquals(EstadoCaja.CERRADA, cerrada.estadoCaja)
    }

    @Test
    fun cerrarGuardaElMontoQueSeConto() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        repository.cerrarCaja(negocioId, montoReal = 141_000.0)

        assertEquals(141_000.0, dao.cajas.value.single().montoRealCierre ?: 0.0, 0.001)
    }

    @Test
    fun laNotaDeLaDiferenciaEsOpcional() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        // Cuando la caja cuadra no hay nada que explicar.
        repository.cerrarCaja(negocioId, montoReal = 50_000.0)

        assertNull(dao.cajas.value.single().notaDiferencia)
    }

    @Test
    fun unaNotaEnBlancoNoSeGuardaComoNota() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        repository.cerrarCaja(negocioId, montoReal = 50_000.0, nota = "   ")

        // Espacios no son una explicación: se guardan como "sin nota" y no como texto vacío.
        assertNull(dao.cajas.value.single().notaDiferencia)
    }

    @Test
    fun laNotaSeGuardaCuandoLaHay() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        repository.cerrarCaja(
            negocioId,
            montoReal = 141_000.0,
            nota = "  Se dio vuelto de más en una venta  ",
        )

        assertEquals("Se dio vuelto de más en una venta", dao.cajas.value.single().notaDiferencia)
    }

    @Test
    fun noSePuedeCerrarConUnMontoNegativo() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        val error = repository.cerrarCaja(negocioId, montoReal = -1_000.0)

        assertEquals(ErrorCaja.MontoNegativo, error)
        // El rechazo no cierra la jornada a medias: sigue abierta y sin datos de cierre.
        assertNotNull(abierta())
        assertNull(dao.cajas.value.single().montoRealCierre)
    }

    @Test
    fun cerrarConCeroSiSePuede() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)

        val error = repository.cerrarCaja(negocioId, montoReal = 0.0)

        // Que la caja quede vacía es un dato, no un error: justo ahí hay algo que revisar.
        assertNull(error)
        assertEquals(0.0, dao.cajas.value.single().montoRealCierre ?: -1.0, 0.001)
    }

    @Test
    fun cerrarSinTenerNadaAbiertoAvisaEnVezDeNoHacerNada() = runTest {
        val error = repository.cerrarCaja(negocioId, montoReal = 141_000.0)

        assertEquals(ErrorCaja.NoHayCajaAbierta, error)
    }

    @Test
    fun cerrarNoTocaLaCajaDeOtroNegocio() = runTest {
        repository.abrirCaja(negocioId, montoInicial = 50_000.0)
        repository.abrirCaja(otroNegocio, montoInicial = 30_000.0)

        repository.cerrarCaja(negocioId, montoReal = 141_000.0)

        assertNull(abierta())
        assertNotNull(repository.cajaAbierta(otroNegocio).first())
    }

    // ---------- HU-22: la diferencia ----------

    @Test
    fun siSobraPlataLaDiferenciaEsPositiva() {
        val d = DiferenciaDeCaja(esperado = 142_500.0, real = 145_000.0)

        assertEquals(2_500.0, d.diferencia, 0.001)
        assertTrue(d.sobra)
        assertFalse(d.falta)
    }

    @Test
    fun siFaltaPlataLaDiferenciaEsNegativa() {
        val d = DiferenciaDeCaja(esperado = 142_500.0, real = 141_000.0)

        assertEquals(-1_500.0, d.diferencia, 0.001)
        assertTrue(d.falta)
        assertFalse(d.sobra)
    }

    @Test
    fun unosCentavosDeDiferenciaNoSonUnDescuadre() {
        val d = DiferenciaDeCaja(esperado = 142_500.0, real = 142_500.4)

        // Los Double arrastran centavos de las divisiones; un peso de tolerancia evita que la
        // pantalla le diga al usuario que le falta plata cuando en realidad cuadra.
        assertTrue(d.cuadra)
    }
}
