package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorRecordatorio
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRecordatorio
import com.gestor_ventures.db.dao.RecordatorioClienteDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-32. Dejar constancia de los recordatorios que se le mandan a un cliente.
 *
 * Lo que se guarda es el mensaje que el usuario decidió mandar, no la plantilla de la que
 * salió: puede corregirlo antes de enviarlo y después lo que importa es qué dijo de verdad.
 */
class RecordatorioClienteRepositoryTest {

    private var momento = LocalDateTime.of(2026, 10, 16, 15, 0)
    private val reloj = Reloj { momento }

    private val dao = RecordatorioClienteDaoFalso()
    private val repository = RecordatorioClienteRepository(dao, reloj)

    private val clienteId = 1L
    private val otroCliente = 2L

    private suspend fun recordatoriosDe(cliente: Long = clienteId) =
        repository.recordatoriosDeCliente(cliente).first()

    @Test
    fun registrarDejaConstanciaDelMensajeYDelTipo() = runTest {
        val error = repository.registrarEnvio(
            clienteId = clienteId,
            tipo = TipoRecordatorio.PagoPendiente,
            mensaje = "Hola Ana, te recuerdo tu pago pendiente.",
        )

        assertNull(error)
        val guardado = recordatoriosDe().single()
        assertEquals("Hola Ana, te recuerdo tu pago pendiente.", guardado.mensaje)
        assertEquals(TipoRecordatorio.PagoPendiente, guardado.tipo)
    }

    @Test
    fun laFechaLaPoneElRelojYNoElUsuario() = runTest {
        momento = momento.withHour(18).withMinute(45)

        repository.registrarEnvio(clienteId, TipoRecordatorio.Cita, "Te espero mañana")

        assertEquals(momento, recordatoriosDe().single().fechaEnvio)
    }

    @Test
    fun seGuardaElMensajeCorregidoYNoLaPlantilla() = runTest {
        repository.registrarEnvio(clienteId, TipoRecordatorio.Cita, "Anita, nos vemos el sábado")

        assertEquals("Anita, nos vemos el sábado", recordatoriosDe().single().mensaje)
    }

    @Test
    fun unMensajeEnBlancoNoEsUnRecordatorio() = runTest {
        val error = repository.registrarEnvio(clienteId, TipoRecordatorio.Cita, "   ")

        assertEquals(ErrorRecordatorio.MensajeVacio, error)
        assertTrue(recordatoriosDe().isEmpty())
    }

    @Test
    fun elMensajeSeGuardaSinEspaciosDeSobra() = runTest {
        repository.registrarEnvio(clienteId, TipoRecordatorio.Cita, "  Te espero mañana  ")

        assertEquals("Te espero mañana", recordatoriosDe().single().mensaje)
    }

    @Test
    fun losDosTiposSeGuardanYSeLeenIgual() = runTest {
        repository.registrarEnvio(clienteId, TipoRecordatorio.Cita, "De cita")
        repository.registrarEnvio(clienteId, TipoRecordatorio.PagoPendiente, "De pago")

        val tipos = recordatoriosDe().map { it.tipo }
        assertTrue(tipos.containsAll(TipoRecordatorio.entries))
    }

    @Test
    fun losDeUnClienteNoTraenLosDeOtro() = runTest {
        repository.registrarEnvio(clienteId, TipoRecordatorio.Cita, "Para Ana")
        repository.registrarEnvio(otroCliente, TipoRecordatorio.Cita, "Para Carlos")

        assertEquals("Para Ana", recordatoriosDe().single().mensaje)
    }

    @Test
    fun elHistorialVaDelMasRecienteAlMasAntiguo() = runTest {
        momento = momento.minusDays(3)
        repository.registrarEnvio(clienteId, TipoRecordatorio.Cita, "El viejo")
        momento = momento.plusDays(3)
        repository.registrarEnvio(clienteId, TipoRecordatorio.Cita, "El nuevo")

        assertEquals(listOf("El nuevo", "El viejo"), recordatoriosDe().map { it.mensaje })
    }
}
