package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorCaja
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.CajaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-19. Reglas de abrir caja antes de que la jornada llegue a la base de datos.
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
}
