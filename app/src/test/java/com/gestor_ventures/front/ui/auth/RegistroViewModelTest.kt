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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-01. El ViewModel con un `AuthRepository` real, pero Firebase y Room en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val autenticador = AutenticadorFalso()
    private val usuarioDao = UsuarioDaoFalso()
    private val authRepository = AuthRepository(autenticador, usuarioDao, Reloj { LocalDateTime.now() })
    private val viewModel = RegistroViewModel(authRepository)

    private val nombre = "Sebastián Orrego"
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

    private fun llenarFormulario(
        nombre: String = this.nombre,
        correo: String = this.correo,
        contrasena: String = this.contrasena,
        confirmar: String = this.contrasena,
        aceptaTerminos: Boolean = true,
    ) {
        viewModel.onNombreChange(nombre)
        viewModel.onCorreoChange(correo)
        viewModel.onContrasenaChange(contrasena)
        viewModel.onConfirmarContrasenaChange(confirmar)
        viewModel.onAceptaTerminosChange(aceptaTerminos)
    }

    @Test
    fun arrancaVacioYSinPoderCrearCuenta() {
        assertFalse(estado.puedeCrearCuenta)
    }

    @Test
    fun conFormularioCompleto_puedeCrearCuenta() {
        llenarFormulario()

        assertTrue(estado.puedeCrearCuenta)
    }

    @Test
    fun sinAceptarTerminos_noPuedeCrearCuenta() {
        llenarFormulario(aceptaTerminos = false)

        assertFalse(estado.puedeCrearCuenta)
    }

    @Test
    fun conContrasenasDistintas_marcaQueNoCoincidenYNoPuedeCrear() {
        llenarFormulario(confirmar = "OtraClave123!")

        assertTrue(estado.contrasenasNoCoinciden)
        assertFalse(estado.puedeCrearCuenta)
    }

    @Test
    fun crearCuenta_conDatosValidos_dejaDeCargarYSinError() = runTest(dispatcher) {
        llenarFormulario()

        viewModel.crearCuenta()
        advanceUntilIdle()

        assertFalse(estado.cargando)
        assertNull(estado.error)
        assertNotNull(usuarioDao.obtenerPorCorreo(correo))
    }

    @Test
    fun crearCuenta_conCorreoYaRegistrado_muestraError() = runTest(dispatcher) {
        authRepository.registrar(nombre, correo, contrasena)
        llenarFormulario()

        viewModel.crearCuenta()
        advanceUntilIdle()

        assertEquals(ErrorAuth.CorreoYaRegistrado, estado.error)
        assertFalse(estado.cargando)
    }
}
