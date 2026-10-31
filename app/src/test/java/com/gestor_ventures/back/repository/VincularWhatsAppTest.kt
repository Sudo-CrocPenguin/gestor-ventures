package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorWhatsApp
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.back.model.celularColombiano
import com.gestor_ventures.back.model.celularConFormato
import com.gestor_ventures.db.dao.NegocioDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/** El WhatsApp de la empresa: qué números acepta y que se guarde por negocio. */
class VincularWhatsAppTest {

    private val dao = NegocioDaoFalso()
    private val repository = NegocioRepository(dao, Reloj { LocalDateTime.of(2026, 10, 30, 14, 0) })

    private suspend fun crearNegocio(): Long =
        (repository.crearNegocio(1, "Bella Piel", TipoActividad.SERVICIOS, "Belleza", 0.0)
            as com.gestor_ventures.back.model.ResultadoNegocio.Exito).negocioId

    @Test
    fun aceptaElNumeroComoLoEscribeLaGente() {
        assertEquals("3001234567", celularColombiano("300 123 4567"))
        assertEquals("3001234567", celularColombiano("(300) 123-4567"))
        assertEquals("3001234567", celularColombiano("+57 300 123 4567"))
    }

    @Test
    fun rechazaLoQueNoEsUnCelular() {
        assertNull(celularColombiano(""))
        assertNull(celularColombiano("123"))
        assertNull(celularColombiano("2001234567"))
        assertNull(celularColombiano("30012345678"))
    }

    @Test
    fun daFormatoLegible() {
        assertEquals("+57 300 123 4567", celularConFormato("3001234567"))
    }

    @Test
    fun vincularGuardaSoloLosDigitos() = runTest {
        val id = crearNegocio()

        assertNull(repository.vincularWhatsApp(id, "+57 300 123 4567"))

        assertEquals("3001234567", repository.observarNegocio(id).first()?.whatsappEmpresa)
    }

    @Test
    fun unNumeroInvalidoNoSeGuarda() = runTest {
        val id = crearNegocio()

        assertEquals(ErrorWhatsApp.NumeroInvalido, repository.vincularWhatsApp(id, "12345"))

        assertNull(repository.observarNegocio(id).first()?.whatsappEmpresa)
    }

    @Test
    fun desvincularBorraElNumero() = runTest {
        val id = crearNegocio()
        repository.vincularWhatsApp(id, "3001234567")

        assertNull(repository.vincularWhatsApp(id, null))

        assertNull(repository.observarNegocio(id).first()?.whatsappEmpresa)
    }

    @Test
    fun unNegocioQueNoExisteSeRechaza() = runTest {
        assertEquals(ErrorWhatsApp.NegocioNoExiste, repository.vincularWhatsApp(99, "3001234567"))
    }
}
