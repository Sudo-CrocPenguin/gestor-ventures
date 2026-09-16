package com.gestor_ventures.back.repository

import com.gestor_ventures.db.SemillaTemporal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quién está usando la app. Hoy devuelve siempre el usuario sembrado, porque HU-01 (registro
 * e inicio de sesión) todavía no existe.
 *
 * Está acá para que `front/` nunca dependa de ese detalle: cuando llegue el login real, cambia
 * esta clase y ninguna pantalla se entera.
 */
@Singleton
class SesionRepository @Inject constructor() {

    /** TEMPORAL — cuando exista HU-01, sale del usuario que inició sesión. */
    fun usuarioId(): Long = SemillaTemporal.USUARIO_ID
}
