package com.gestor_ventures.front.ui.auth

import com.gestor_ventures.back.model.ErrorAuth
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.AuthRepository
import com.gestor_ventures.back.repository.AutenticadorFalso
import com.gestor_ventures.db.dao.UsuarioDaoFalso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-02. El ViewModel con un `AuthRepository` real, pero Firebase y Room en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val autenticador = AutenticadorFalso()
    private val usuarioDao = UsuarioDaoFalso()
    private val authRepository = AuthRepository(autenticador, usuarioDao, Reloj { LocalDateTime.now() })
    private val viewModel = LoginViewModel(authRepository)

    private val correo = "sebastian@correo.com"
    private val contrasena = "Clave123!"

    private val estado get() = viewModel.uiState.value

    @Before
    fun fijarDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun soltarDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun arrancaVacioYSinPoderIniciarSesion() {
        assertEquals("", estado.correo)
        assertEquals("", estado.contrasena)
        assertFalse(estado.puedeIniciarSesion)
    }

    @Test
    fun conCorreoYContrasena_puedeIniciarSesion() {
        viewModel.onCorreoChange(correo)
        viewModel.onContrasenaChange(contrasena)

        assertTrue(estado.puedeIniciarSesion)
    }

    @Test
    fun onMostrarContrasenaChange_alternaLaVisibilidad() {
        viewModel.onMostrarContrasenaChange()
        assertTrue(estado.mostrarContrasena)

        viewModel.onMostrarContrasenaChange()
        assertFalse(estado.mostrarContrasena)
    }

    @Test
    fun iniciarSesion_conCredencialesCorrectas_dejaDeCargarYSinError() = runTest(dispatcher) {
        authRepository.registrar("Sebastián Orrego", correo, contrasena)
        viewModel.onCorreoChange(correo)
        viewModel.onContrasenaChange(contrasena)

        viewModel.iniciarSesion()
        advanceUntilIdle()

        assertFalse(estado.cargando)
        assertNull(estado.error)
    }

    @Test
    fun iniciarSesion_conContrasenaIncorrecta_muestraError() = runTest(dispatcher) {
        authRepository.registrar("Sebastián Orrego", correo, contrasena)
        viewModel.onCorreoChange(correo)
        viewModel.onContrasenaChange("OtraClave123!")

        viewModel.iniciarSesion()
        advanceUntilIdle()

        assertEquals(ErrorAuth.CredencialesInvalidas, estado.error)
        assertFalse(estado.cargando)
    }

    @Test
    fun onCorreoChange_borraElErrorAnterior() = runTest(dispatcher) {
        viewModel.onCorreoChange(correo)
        viewModel.onContrasenaChange("mala")
        viewModel.iniciarSesion()
        advanceUntilIdle()

        viewModel.onCorreoChange("otro@correo.com")

        assertNull(estado.error)
    }
}
