package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.UsuarioDaoFalso
import com.gestor_ventures.db.entity.UsuarioEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-01. El perfil de quien tiene la sesión abierta.
 *
 * Existe porque el saludo del inicio y la cabecera del menú mostraban un nombre de ejemplo
 * quemado en el código, y nadie se daba cuenta si el de verdad no llegaba.
 */
class SesionUsuarioActualTest {

    private val ahora = LocalDateTime.of(2026, 9, 19, 10, 0)
    private val reloj = Reloj { ahora }
    private val usuarioDao = UsuarioDaoFalso()
    private val autenticador = AutenticadorFalso()
    private val authRepository = AuthRepository(autenticador, usuarioDao, reloj)

    private val repository = SesionRepository(
        autenticador = autenticador,
        usuarioDao = usuarioDao,
        authRepository = authRepository,
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    private suspend fun registrar(nombre: String, correo: String) {
        usuarioDao.insertar(
            UsuarioEntity(
                nombre = nombre,
                correo = correo,
                contrasenaHash = "",
                fechaCreacion = ahora,
                fechaUltimoAcceso = ahora,
            ),
        )
        autenticador.sesion.value = correo
    }

    @Test
    fun elPerfilEsElDeQuienIniciaSesion() = runTest {
        registrar("Mariana Restrepo", "mariana@correo.com")

        val usuario = repository.usuarioActual.first()

        assertEquals("Mariana Restrepo", usuario?.nombre)
        assertEquals("mariana@correo.com", usuario?.correo)
    }

    @Test
    fun sinSesionNoHayPerfil() = runTest {
        assertNull(repository.usuarioActual.first())
    }

    @Test
    fun elSaludoUsaSoloElPrimerNombre() = runTest {
        registrar("Mariana Restrepo", "mariana@correo.com")

        // Saludar con nombre y apellido suena a carta del banco.
        assertEquals("Mariana", repository.usuarioActual.first()?.primerNombre)
    }

    @Test
    fun unNombreDeUnaSolaPalabraSeSaludaEntero() = runTest {
        registrar("Mariana", "mariana@correo.com")

        assertEquals("Mariana", repository.usuarioActual.first()?.primerNombre)
    }

    @Test
    fun cambiarElNombreDelPerfilSeVeSinVolverAEntrar() = runTest {
        registrar("Mariana Restrepo", "mariana@correo.com")
        val guardado = usuarioDao.obtener(1L)!!

        usuarioDao.actualizar(guardado.copy(nombre = "Mariana R."))

        // Se sigue por Flow justamente para esto: HU-04 va a editar el perfil.
        assertEquals("Mariana R.", repository.usuarioActual.first()?.nombre)
    }
}
