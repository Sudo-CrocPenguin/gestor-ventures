package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.UsuarioDaoFalso
import com.gestor_ventures.db.entity.UsuarioEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

private const val Correo = "usuario@gestorventures.local"

/**
 * [SesionRepository] con una sesión ya abierta para [SemillaTemporal.USUARIO_ID], para pruebas
 * de pantallas que solo necesitan un usuario fijo y no les importa cómo se resolvió.
 *
 * [UsuarioDaoFalso.insertar] siempre autoasigna el id empezando en 1 (igual que
 * [SemillaTemporal.USUARIO_ID]), así que basta con insertar una sola vez.
 *
 * Usa [Dispatchers.Unconfined] a propósito: así el `usuarioId` queda resuelto de forma
 * síncrona, sin tener que envolver la construcción en `runTest`.
 */
fun sesionRepositoryDePrueba(): SesionRepository {
    val usuarioDao = UsuarioDaoFalso()
    val ahora = LocalDateTime.now()
    runBlocking {
        usuarioDao.insertar(
            UsuarioEntity(
                nombre = "Usuario de prueba",
                correo = Correo,
                contrasenaHash = "",
                fechaCreacion = ahora,
                fechaUltimoAcceso = ahora,
            ),
        )
    }
    val autenticador = AutenticadorFalso().apply { sesion.value = Correo }
    val authRepository = AuthRepository(autenticador, usuarioDao, Reloj { ahora })
    return SesionRepository(autenticador, usuarioDao, authRepository, CoroutineScope(Dispatchers.Unconfined))
}
