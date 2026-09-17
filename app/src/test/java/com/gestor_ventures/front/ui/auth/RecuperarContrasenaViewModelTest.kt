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

/** HU-03. El ViewModel con un `AuthRepository` real, pero Firebase y Room en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecuperarContrasenaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val autenticador = AutenticadorFalso()
    private val usuarioDao = UsuarioDaoFalso()
    private val authRepository = AuthRepository(autenticador, usuarioDao, Reloj { LocalDateTime.now() })
    private val viewModel = RecuperarContrasenaViewModel(authRepository)

    private val correo = "sebastian@correo.com"

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
    fun enviarCodigo_conCorreoRegistrado_marcaEnviado() = runTest(dispatcher) {
        authRepository.registrar("Sebastián Orrego", correo, "Clave123!")
        viewModel.onCorreoChange(correo)

        viewModel.enviarCodigo()
        advanceUntilIdle()

        assertTrue(estado.enviado)
        assertFalse(estado.enviando)
        assertNull(estado.error)
    }

    @Test
    fun enviarCodigo_conCorreoNoRegistrado_muestraError() = runTest(dispatcher) {
        viewModel.onCorreoChange("nadie@correo.com")

        viewModel.enviarCodigo()
        advanceUntilIdle()

        assertEquals(ErrorAuth.CorreoNoRegistrado, estado.error)
        assertFalse(estado.enviado)
    }

    @Test
    fun onCorreoChange_reiniciaEnviadoYError() = runTest(dispatcher) {
        authRepository.registrar("Sebastián Orrego", correo, "Clave123!")
        viewModel.onCorreoChange(correo)
        viewModel.enviarCodigo()
        advanceUntilIdle()

        viewModel.onCorreoChange("otro@correo.com")

        assertFalse(estado.enviado)
        assertNull(estado.error)
    }
}
