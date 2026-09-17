package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorAuth
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoAuth
import com.gestor_ventures.db.dao.UsuarioDaoFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-01, HU-02 y HU-03. Prueba las reglas de `AuthRepository` sin Firebase ni Room: el
 * proveedor de identidad y el DAO se reemplazan por versiones en memoria.
 */
class AuthRepositoryTest {

    private val momentoRegistro = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val momentoLogin = LocalDateTime.of(2026, 9, 17, 8, 30)
    private var ahora = momentoRegistro

    private val autenticador = AutenticadorFalso()
    private val usuarioDao = UsuarioDaoFalso()
    private val repository = AuthRepository(autenticador, usuarioDao, Reloj { ahora })

    private val nombre = "Sebastián Orrego"
    private val correo = "sebastian@correo.com"
    private val contrasenaValida = "Clave123!"

    private suspend fun registrar(
        nombre: String = this.nombre,
        correo: String = this.correo,
        contrasena: String = contrasenaValida,
    ) = repository.registrar(nombre, correo, contrasena)

    // --- HU-01: registro ---

    @Test
    fun registrar_conDatosValidos_creaLaCuentaYElPerfilLocal() = runTest {
        val resultado = registrar()

        assertEquals(ResultadoAuth.Exito(1L), resultado)
        val usuario = usuarioDao.obtenerPorCorreo(correo)
        assertNotNull(usuario)
        assertEquals(nombre, usuario?.nombre)
        assertEquals(momentoRegistro, usuario?.fechaCreacion)
        assertNull(usuario?.fechaUltimoAcceso)
    }

    @Test
    fun registrar_normalizaElCorreoAMinusculasYSinEspacios() = runTest {
        registrar(correo = "  Sebastian@Correo.com  ")

        assertNotNull(usuarioDao.obtenerPorCorreo("sebastian@correo.com"))
    }

    @Test
    fun registrar_conCorreoYaRegistrado_devuelveInvalido() = runTest {
        registrar()

        val resultado = registrar(nombre = "Otro Usuario")

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.CorreoYaRegistrado), resultado)
    }

    @Test
    fun registrar_conNombreVacio_devuelveInvalido() = runTest {
        val resultado = registrar(nombre = "   ")

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.NombreVacio), resultado)
    }

    @Test
    fun registrar_conNombreDeMasDe50Caracteres_devuelveInvalido() = runTest {
        val resultado = registrar(nombre = "a".repeat(51))

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.NombreMuyLargo), resultado)
    }

    @Test
    fun registrar_conCorreoSinFormatoValido_devuelveInvalido() = runTest {
        val resultado = registrar(correo = "no-es-un-correo")

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.CorreoInvalido), resultado)
    }

    @Test
    fun registrar_conContrasenaSinSimbolo_devuelveInvalido() = runTest {
        val resultado = registrar(contrasena = "Clave1234")

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.ContrasenaInvalida), resultado)
    }

    @Test
    fun registrar_conContrasenaMuyCorta_devuelveInvalido() = runTest {
        val resultado = registrar(contrasena = "Cl1!")

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.ContrasenaInvalida), resultado)
    }

    @Test
    fun registrar_conContrasenaMuyLarga_devuelveInvalido() = runTest {
        val resultado = registrar(contrasena = "Aa1!" + "a".repeat(22))

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.ContrasenaInvalida), resultado)
    }

    @Test
    fun registrar_siFirebaseRechazaElCorreo_noCreaPerfilLocal() = runTest {
        autenticador.siguienteCrearCuenta = ResultadoAutenticador.CorreoYaRegistrado

        val resultado = registrar()

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.CorreoYaRegistrado), resultado)
        assertNull(usuarioDao.obtenerPorCorreo(correo))
    }

    @Test
    fun registrar_siFirebaseFalla_devuelveErrorDeRed() = runTest {
        autenticador.siguienteCrearCuenta = ResultadoAutenticador.ErrorDeRed

        val resultado = registrar()

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.ErrorDeRed), resultado)
    }

    // --- HU-02: inicio de sesión ---

    @Test
    fun iniciarSesion_conCredencialesCorrectas_devuelveElUsuarioYActualizaUltimoAcceso() = runTest {
        registrar()
        ahora = momentoLogin

        val resultado = repository.iniciarSesion(correo, contrasenaValida)

        assertEquals(ResultadoAuth.Exito(1L), resultado)
        assertEquals(momentoLogin, usuarioDao.obtenerPorCorreo(correo)?.fechaUltimoAcceso)
    }

    @Test
    fun iniciarSesion_conContrasenaIncorrecta_devuelveCredencialesInvalidas() = runTest {
        registrar()

        val resultado = repository.iniciarSesion(correo, "OtraClave123!")

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.CredencialesInvalidas), resultado)
    }

    @Test
    fun iniciarSesion_conCorreoNoRegistrado_devuelveCredencialesInvalidas() = runTest {
        val resultado = repository.iniciarSesion("nadie@correo.com", contrasenaValida)

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.CredencialesInvalidas), resultado)
    }

    @Test
    fun iniciarSesion_siFirebaseFalla_devuelveErrorDeRed() = runTest {
        registrar()
        autenticador.siguienteIniciarSesion = ResultadoAutenticador.ErrorDeRed

        val resultado = repository.iniciarSesion(correo, contrasenaValida)

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.ErrorDeRed), resultado)
    }

    // --- HU-03: recuperar contraseña ---

    @Test
    fun recuperarContrasena_conCorreoRegistrado_devuelveExito() = runTest {
        registrar()

        val resultado = repository.recuperarContrasena(correo)

        assertEquals(ResultadoAuth.Exito(), resultado)
    }

    @Test
    fun recuperarContrasena_conCorreoNoRegistrado_devuelveInvalido() = runTest {
        val resultado = repository.recuperarContrasena("nadie@correo.com")

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.CorreoNoRegistrado), resultado)
    }

    @Test
    fun recuperarContrasena_conFormatoDeCorreoInvalido_noConsultaAlProveedor() = runTest {
        val resultado = repository.recuperarContrasena("no-es-un-correo")

        assertEquals(ResultadoAuth.Invalido(ErrorAuth.CorreoInvalido), resultado)
    }
}
