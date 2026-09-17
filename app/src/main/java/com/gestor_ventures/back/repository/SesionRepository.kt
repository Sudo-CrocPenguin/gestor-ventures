package com.gestor_ventures.back.repository

import com.gestor_ventures.back.di.ApplicationScope
import com.gestor_ventures.db.dao.UsuarioDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quién está usando la app: el `usuarioId` local de quien tiene la sesión abierta en Firebase
 * Authentication, o `null` si nadie inició sesión (o si expiró por inactividad, HU-02).
 *
 * Vive fuera de las pantallas porque no es de ninguna: el menú lateral lo cambia, la barra
 * superior lo muestra, el inicio resume sus ventas y el registro de ventas lo necesita para
 * saber a quién apuntarle la venta. Si cada pantalla guardara el suyo, tarde o temprano dos
 * dirían cosas distintas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SesionRepository @Inject constructor(
    autenticador: Autenticador,
    usuarioDao: UsuarioDao,
    authRepository: AuthRepository,
    @ApplicationScope scope: CoroutineScope,
) {

    /**
     * Sigue el perfil local por `Flow` (no una consulta puntual): justo después de registrarse
     * o iniciar sesión, Firebase ya avisó pero Room puede no tener la fila todavía — con una
     * consulta puntual esa carrera se resuelve como "sin sesión" y ahí se queda. Con un `Flow`,
     * en cuanto `AuthRepository` guarda la fila, esto se entera solo y se corrige.
     */
    private val usuarioIdActual: StateFlow<Long?> = autenticador.observarCorreoDeSesion()
        .flatMapLatest { correo -> correo?.let(usuarioDao::observarPorCorreo) ?: flowOf(null) }
        .map { usuario -> usuario?.usuarioId?.takeIf { authRepository.sesionActiva(it) } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** El id de quien tiene la sesión abierta ahora mismo, o `null` si nadie. */
    fun usuarioId(): Long? = usuarioIdActual.value

    /** Para quien necesita reaccionar a que alguien inicie o cierre sesión. */
    fun observarUsuarioId(): StateFlow<Long?> = usuarioIdActual
}
