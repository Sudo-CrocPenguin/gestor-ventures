package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.EstadoMiembro
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.db.dao.EquipoDaoFalso
import com.gestor_ventures.db.dao.NegocioDaoFalso
import com.gestor_ventures.front.navigation.TopLevelDestination
import com.gestor_ventures.front.navigation.destinosDe
import com.gestor_ventures.front.ui.tour.EscenaTour
import com.gestor_ventures.front.ui.tour.pasosDelTour
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * El rol Vendedor: a qué negocios llega alguien invitado, qué pestañas ve y qué le muestra el tour.
 * El Líder de un negocio es quien lo creó; cualquier otro con acceso es Vendedor.
 */
class RolVendedorTest {

    private val reloj = Reloj { LocalDateTime.of(2026, 10, 30, 14, 0) }
    private val negocioDao = NegocioDaoFalso()
    private val negocios = NegocioRepository(negocioDao, reloj)
    private val equipoDao = EquipoDaoFalso()
    private val equipo = EquipoRepository(equipoDao, reloj)

    private val lider = 1L
    private val vendedor = 2L
    private val correoVendedor = "ana@correo.com"

    private suspend fun crearNegocio(dueno: Long, nombre: String): Long =
        (negocios.crearNegocio(dueno, nombre, TipoActividad.SERVICIOS, "Belleza", 0.0)
            as com.gestor_ventures.back.model.ResultadoNegocio.Exito).negocioId

    @Test
    fun elLiderVeSusNegocios() = runTest {
        crearNegocio(lider, "Bella Piel")

        assertEquals(listOf("Bella Piel"), negocios.negociosAccesibles(lider, "lider@correo.com").first().map { it.nombre })
    }

    @Test
    fun unVendedorInvitadoVeElNegocioDeQuienLoInvito() = runTest {
        val id = crearNegocio(lider, "Bella Piel")
        negocioDao.invitados += id to correoVendedor

        val vistos = negocios.negociosAccesibles(vendedor, correoVendedor).first()

        assertEquals(listOf("Bella Piel"), vistos.map { it.nombre })
        assertEquals(lider, vistos.first().usuarioId)
    }

    @Test
    fun alguienSinInvitacionNoVeNegociosAjenos() = runTest {
        crearNegocio(lider, "Bella Piel")

        assertTrue(negocios.negociosAccesibles(vendedor, "otro@correo.com").first().isEmpty())
    }

    @Test
    fun elCorreoSeCompararSinMayusculas() = runTest {
        val id = crearNegocio(lider, "Bella Piel")
        negocioDao.invitados += id to correoVendedor

        assertEquals(1, negocios.negociosAccesibles(vendedor, "  ANA@Correo.com ").first().size)
    }

    @Test
    fun alEntrarConSuCorreoLaInvitacionPasaAActiva() = runTest {
        equipo.invitar(1, correoVendedor, "lider@correo.com")
        assertEquals(EstadoMiembro.PENDIENTE, equipo.miembros(1).first().first().estado)

        equipo.aceptarInvitaciones("Ana@correo.com")

        assertEquals(EstadoMiembro.ACTIVO, equipo.miembros(1).first().first().estado)
    }

    @Test
    fun sabeSiUnCorreoTieneInvitacion() = runTest {
        equipo.invitar(1, correoVendedor, "lider@correo.com")

        assertTrue(equipo.tieneInvitacion("ANA@correo.com"))
        assertFalse(equipo.tieneInvitacion("otro@correo.com"))
    }

    @Test
    fun elVendedorSoloVeInicioAgendaYClientes() {
        assertEquals(
            listOf(TopLevelDestination.Inicio, TopLevelDestination.Agenda, TopLevelDestination.Clientes),
            destinosDe(esLider = false),
        )
        assertEquals(5, destinosDe(esLider = true).size)
    }

    @Test
    fun elTourDelVendedorNoSenalaLoQueNoVe() {
        val pasos = pasosDelTour(esLider = false)

        assertTrue(pasos.none { it.escena == EscenaTour.Finanzas || it.escena == EscenaTour.Asistente })
        assertTrue(pasos.none { it.objetivo in setOf("inicio_resumen", "clientes_agregar", "menu_config") })
        assertTrue(pasos.size < pasosDelTour(esLider = true).size)
    }
}
