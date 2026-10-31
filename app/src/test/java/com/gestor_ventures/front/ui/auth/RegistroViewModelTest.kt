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
import com.gestor_ventures.back.repository.OnboardingFalso
import com.gestor_ventures.back.repository.EquipoRepository

/** HU-01. El ViewModel con un `AuthRepository` real, pero Firebase y Room en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val autenticador = AutenticadorFalso()
    private val usuarioDao = UsuarioDaoFalso()
    private val authRepository = AuthRepository(autenticador, usuarioDao, Reloj { LocalDateTime.now() })
    private val onboarding = OnboardingFalso()
    private val equipoDao = com.gestor_ventures.db.dao.EquipoDaoFalso()
    private val equipoRepository = EquipoRepository(equipoDao, Reloj { LocalDateTime.now() })
    private val viewModel = RegistroViewModel(authRepository, onboarding, equipoRepository)

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

    /** Solo una cuenta recién creada es llevada sola a registrar su primer negocio. */
    @Test
    fun crearCuenta_conExito_marcaLaCuentaComoNuevaSinNegocio() = runTest(dispatcher) {
        llenarFormulario()

        viewModel.crearCuenta()
        advanceUntilIdle()

        assertEquals(setOf(correo), onboarding.pendientes.value)
    }

    /** Un vendedor invitado que crea su cuenta entra al negocio de quien lo invitó: no crea uno. */
    @Test
    fun crearCuenta_deUnVendedorInvitado_noLaMarcaComoNuevaSinNegocio() = runTest(dispatcher) {
        equipoRepository.invitar(1, correo, "lider@correo.com")
        llenarFormulario()

        viewModel.crearCuenta()
        advanceUntilIdle()

        assertEquals(emptySet<String>(), onboarding.pendientes.value)
    }

    /** Una cuenta que ya existía no puede quedar marcada como nueva por intentar registrarse. */
    @Test
    fun crearCuenta_conCorreoYaRegistrado_noDejaLaCuentaMarcadaComoNueva() = runTest(dispatcher) {
        authRepository.registrar(nombre, correo, contrasena)
        llenarFormulario()

        viewModel.crearCuenta()
        advanceUntilIdle()

        assertEquals(emptySet<String>(), onboarding.pendientes.value)
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
