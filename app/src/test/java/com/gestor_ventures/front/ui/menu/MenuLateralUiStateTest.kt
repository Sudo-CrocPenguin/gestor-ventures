package com.gestor_ventures.front.ui.menu

import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.RolNegocio
import com.gestor_ventures.front.model.UsuarioUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuLateralUiStateTest {

    private val usuario = UsuarioUi(nombre = "Sebastián Orrego", correo = "sebastian@correo.com")

    private val negocios = listOf(
        NegocioUi("dulce", "Dulce Antojo", "Repostería", RolNegocio.Lider),
        NegocioUi("bella", "Bella Piel", "Estética", RolNegocio.Vendedor),
    )

    private fun estadoCon(negocioActivoId: String) =
        MenuLateralUiState(usuario = usuario, negocios = negocios, negocioActivoId = negocioActivoId)

    @Test
    fun negocioActivo_esElDelIdSeleccionado() {
        assertEquals("Bella Piel", estadoCon("bella").negocioActivo?.nombre)
    }

    @Test
    fun negocioActivo_esNuloSiElIdNoExiste() {
        assertNull(estadoCon("otro").negocioActivo)
    }

    @Test
    fun lider_veLaConfiguracionCompletaDelNegocio() {
        assertEquals(
            listOf(
                OpcionMenu.InformacionNegocio,
                OpcionMenu.GastosFijos,
                OpcionMenu.Categorias,
                OpcionMenu.MetodosPago,
                OpcionMenu.Equipo,
            ),
            estadoCon("dulce").opcionesConfiguracion,
        )
    }

    @Test
    fun vendedor_noVeLaConfiguracionDelNegocio() {
        assertTrue(estadoCon("bella").opcionesConfiguracion.isEmpty())
    }

    @Test
    fun sinNegocioActivo_noHayConfiguracion() {
        assertTrue(estadoCon("otro").opcionesConfiguracion.isEmpty())
    }

    @Test
    fun opcionesGenerales_lasVeCualquierRol() {
        assertEquals(listOf(OpcionMenu.MiPerfil, OpcionMenu.Notificaciones), opcionesGenerales)
    }
}
