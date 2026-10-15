package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.UsuarioDaoFalso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-01, HU-02 y HU-03. `SesionRepository` no habla con Firebase ni con Room directamente: se
 * arma con [AutenticadorFalso] y [UsuarioDaoFalso] para probar que reacciona a que alguien
 * inicie o cierre sesión, o a que la sesión expire por inactividad (HU-02).
 *
 * [Dispatchers.Unconfined] hace que el `StateFlow` interno se resuelva en el mismo hilo, sin
 * saltos de scheduler: así se puede leer `usuarioId()` justo después de cada acción.
 */
class SesionRepositoryTest {

    private var ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val autenticador = AutenticadorFalso()
    private val usuarioDao = UsuarioDaoFalso()
    private val authRepository = AuthRepository(autenticador, usuarioDao, Reloj { ahora })
    private val sesionRepository = SesionRepository(
        autenticador,
        usuarioDao,
        authRepository,
        CoroutineScope(Dispatchers.Unconfined),
    )

    private val correo = "sebastian@correo.com"
    private val contrasena = "Clave123!"

    @Test
    fun usuarioId_sinNadieConSesion_esNulo() {
        assertNull(sesionRepository.usuarioId())
    }

    @Test
    fun usuarioId_trasRegistrarse_esElDelPerfilLocal() = runTest {
        authRepository.registrar("Sebastián Orrego", correo, contrasena)

        assertEquals(1L, sesionRepository.usuarioId())
    }

    @Test
    fun usuarioId_trasIniciarSesion_esElDelPerfilLocal() = runTest {
        authRepository.registrar("Sebastián Orrego", correo, contrasena)
        autenticador.sesion.value = null // como si hubiera cerrado sesión antes de este login

        authRepository.iniciarSesion(correo, contrasena)

        assertEquals(1L, sesionRepository.usuarioId())
    }

    @Test
    fun usuarioId_trasCerrarSesion_vuelveANulo() = runTest {
        authRepository.registrar("Sebastián Orrego", correo, contrasena)

        autenticador.cerrarSesion()

        assertNull(sesionRepository.usuarioId())
    }

    @Test
    fun usuarioId_conSesionExpiradaPorInactividad_alReiniciarLaAppCierraLaSesion() = runTest {
        authRepository.registrar("Sebastián Orrego", correo, contrasena)
        authRepository.iniciarSesion(correo, contrasena)
        val momentoLogin = ahora
        ahora = momentoLogin.plusDays(7).plusMinutes(1)

        // Simula reabrir la app: nace un SesionRepository nuevo (como al reiniciar el proceso),
        // que al suscribirse recibe la sesión persistida y ahí sí evalúa la inactividad.
        val sesionRepositoryNueva = SesionRepository(
            autenticador,
            usuarioDao,
            authRepository,
            CoroutineScope(Dispatchers.Unconfined),
        )

        assertNull(sesionRepositoryNueva.usuarioId())
        assertNull(autenticador.sesion.value)
    }
}
