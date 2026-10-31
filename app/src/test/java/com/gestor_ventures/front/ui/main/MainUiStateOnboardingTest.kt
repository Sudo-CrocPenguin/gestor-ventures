package com.gestor_ventures.front.ui.main

import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.RolNegocio
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cuándo la app lleva sola a "Registrar negocio": solo con una cuenta nueva que todavía no tiene
 * negocio. Iniciar sesión con una cuenta que ya existía no debe imponer el formulario.
 */
class MainUiStateOnboardingTest {

    private val negocio = NegocioUi(id = "1", nombre = "Bella Piel", categoria = "Belleza", rol = RolNegocio.Lider)

    @Test
    fun cuentaNuevaSinNegocio_vaAlRegistroDeNegocio() {
        val estado = MainUiState(cargando = false, onboardingPendiente = true)

        assertTrue(estado.debeRegistrarNegocio)
    }

    @Test
    fun cuentaQueYaExistiaSinNegocios_noVaAlRegistroDeNegocio() {
        val estado = MainUiState(cargando = false, onboardingPendiente = false)

        assertTrue(estado.sinNegocios)
        assertFalse(estado.debeRegistrarNegocio)
    }

    @Test
    fun cuentaNuevaConSuNegocioYaCreado_noVuelveAlRegistro() {
        val estado = MainUiState(cargando = false, negocios = listOf(negocio), onboardingPendiente = true)

        assertFalse(estado.debeRegistrarNegocio)
    }

    @Test
    fun mientrasCarga_noVaAlRegistro() {
        val estado = MainUiState(cargando = true, onboardingPendiente = true)

        assertFalse(estado.debeRegistrarNegocio)
    }
}
