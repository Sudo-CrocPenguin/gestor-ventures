package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.db.dao.MetodoPagoDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetodoPagoRepositoryTest {

    private val repository = MetodoPagoRepository(MetodoPagoDaoFalso())

    @Test
    fun unNegocioNuevoOfreceTodosLosMetodos() = runTest {
        assertEquals(MetodosConfigurables, repository.activos(1).first())
    }

    @Test
    fun apagarUnMetodoLoQuitaDeLosActivos() = runTest {
        assertTrue(repository.cambiar(1, MetodoPago.TARJETA, activo = false))

        assertEquals(listOf(MetodoPago.EFECTIVO, MetodoPago.TRANSFERENCIA), repository.activos(1).first())
    }

    @Test
    fun volverAEncenderLoDevuelve() = runTest {
        repository.cambiar(1, MetodoPago.TARJETA, activo = false)
        repository.cambiar(1, MetodoPago.TARJETA, activo = true)

        assertEquals(MetodosConfigurables, repository.activos(1).first())
    }

    @Test
    fun nuncaDejaApagarElUltimoActivo() = runTest {
        repository.cambiar(1, MetodoPago.TARJETA, activo = false)
        repository.cambiar(1, MetodoPago.TRANSFERENCIA, activo = false)

        assertFalse(repository.cambiar(1, MetodoPago.EFECTIVO, activo = false))
        assertEquals(listOf(MetodoPago.EFECTIVO), repository.activos(1).first())
    }

    @Test
    fun cadaNegocioTieneLosSuyos() = runTest {
        repository.cambiar(1, MetodoPago.TARJETA, activo = false)

        assertEquals(MetodosConfigurables, repository.activos(2).first())
    }
}
