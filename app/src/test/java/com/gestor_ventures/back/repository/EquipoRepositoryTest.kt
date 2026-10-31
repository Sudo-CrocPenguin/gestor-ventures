package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorEquipo
import com.gestor_ventures.back.model.EstadoMiembro
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.EquipoDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class EquipoRepositoryTest {

    private val repository = EquipoRepository(
        EquipoDaoFalso(),
        Reloj { LocalDateTime.of(2026, 10, 30, 14, 0) },
    )
    private val lider = "mariana@gmail.com"

    @Test
    fun invitarDejaAlVendedorPendiente() = runTest {
        assertNull(repository.invitar(1, "valentina@correo.com", lider))

        val equipo = repository.miembros(1).first()
        assertEquals(1, equipo.size)
        assertEquals("valentina@correo.com", equipo.first().correo)
        assertEquals(EstadoMiembro.PENDIENTE, equipo.first().estado)
    }

    @Test
    fun elCorreoSeGuardaSinMayusculasNiEspacios() = runTest {
        repository.invitar(1, "  Valentina@Correo.COM ", lider)

        assertEquals("valentina@correo.com", repository.miembros(1).first().first().correo)
    }

    @Test
    fun uncorreoSinFormatoSeRechaza() = runTest {
        assertEquals(ErrorEquipo.CorreoInvalido, repository.invitar(1, "valentina", lider))
        assertEquals(emptyList<Any>(), repository.miembros(1).first())
    }

    @Test
    fun noSePuedeInvitarAlPropioLider() = runTest {
        assertEquals(ErrorEquipo.EsElLider, repository.invitar(1, "Mariana@Gmail.com", lider))
    }

    @Test
    fun noSePuedeInvitarDosVecesAlMismoCorreo() = runTest {
        repository.invitar(1, "ana@correo.com", lider)

        assertEquals(ErrorEquipo.YaEstaEnElEquipo, repository.invitar(1, "ANA@correo.com", lider))
        assertEquals(1, repository.miembros(1).first().size)
    }

    @Test
    fun elMismoCorreoSirveEnOtroNegocio() = runTest {
        repository.invitar(1, "ana@correo.com", lider)

        assertNull(repository.invitar(2, "ana@correo.com", lider))
    }

    @Test
    fun quitarBorraAlVendedor() = runTest {
        repository.invitar(1, "ana@correo.com", lider)
        val id = repository.miembros(1).first().first().id

        repository.quitar(id)

        assertEquals(emptyList<Any>(), repository.miembros(1).first())
    }
}
