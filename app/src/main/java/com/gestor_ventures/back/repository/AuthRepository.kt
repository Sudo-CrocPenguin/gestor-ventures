package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorAuth
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoAuth
import com.gestor_ventures.db.dao.UsuarioDao
import com.gestor_ventures.db.entity.UsuarioEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Tope de caracteres del nombre y formato de correo/contraseña, tal como quedó en HU-01. */
private const val MaxCaracteresNombre = 50
private val FormatoCorreo = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** 8-25 caracteres, con al menos una mayúscula, una minúscula y un símbolo. */
private val FormatoContrasena = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).{8,25}$")

/**
 * HU-01, HU-02 y HU-03. Única puerta de entrada a la autenticación.
 *
 * La contraseña la valida y guarda [Autenticador] (Firebase Authentication); acá solo se
 * valida el formato antes de mandarla y se mantiene el perfil local (tabla `usuarios`) que el
 * resto de la app usa para relacionar negocios, ventas, etc. con el usuario dueño.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val autenticador: Autenticador,
    private val usuarioDao: UsuarioDao,
    private val reloj: Reloj,
) {

    /** HU-01. Crea la cuenta y el perfil local si los datos son válidos. */
    suspend fun registrar(nombre: String, correo: String, contrasena: String): ResultadoAuth {
        val nombreLimpio = nombre.trim()
        val correoLimpio = correo.trim().lowercase()
        validarRegistro(nombreLimpio, correoLimpio, contrasena)?.let {
            return ResultadoAuth.Invalido(it)
        }
        if (usuarioDao.obtenerPorCorreo(correoLimpio) != null) {
            return ResultadoAuth.Invalido(ErrorAuth.CorreoYaRegistrado)
        }

        return when (autenticador.crearCuenta(correoLimpio, contrasena)) {
            ResultadoAutenticador.Exito -> {
                val usuarioId = usuarioDao.insertar(
                    UsuarioEntity(
                        nombre = nombreLimpio,
                        correo = correoLimpio,
                        // La contraseña la hashea y guarda Firebase Authentication, no Room.
                        contrasenaHash = "",
                        fechaCreacion = reloj.ahora(),
                    ),
                )
                ResultadoAuth.Exito(usuarioId)
            }
            ResultadoAutenticador.CorreoYaRegistrado -> ResultadoAuth.Invalido(ErrorAuth.CorreoYaRegistrado)
            else -> ResultadoAuth.Invalido(ErrorAuth.ErrorDeRed)
        }
    }

    /** HU-02. Inicia sesión y registra la fecha de último acceso. */
    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAuth {
        val correoLimpio = correo.trim().lowercase()
        if (correoLimpio.isEmpty() || contrasena.isEmpty()) {
            return ResultadoAuth.Invalido(ErrorAuth.CredencialesInvalidas)
        }

        return when (autenticador.iniciarSesion(correoLimpio, contrasena)) {
            ResultadoAutenticador.Exito -> {
                val usuario = usuarioDao.obtenerPorCorreo(correoLimpio)
                    ?: return ResultadoAuth.Invalido(ErrorAuth.CredencialesInvalidas)
                usuarioDao.actualizar(usuario.copy(fechaUltimoAcceso = reloj.ahora()))
                ResultadoAuth.Exito(usuario.usuarioId)
            }
            ResultadoAutenticador.CredencialesInvalidas,
            ResultadoAutenticador.CorreoNoRegistrado,
            -> ResultadoAuth.Invalido(ErrorAuth.CredencialesInvalidas)
            else -> ResultadoAuth.Invalido(ErrorAuth.ErrorDeRed)
        }
    }

    /** HU-03. Manda el enlace de recuperación al correo indicado, si está registrado. */
    suspend fun recuperarContrasena(correo: String): ResultadoAuth {
        val correoLimpio = correo.trim().lowercase()
        if (!FormatoCorreo.matches(correoLimpio)) {
            return ResultadoAuth.Invalido(ErrorAuth.CorreoInvalido)
        }

        return when (autenticador.enviarCorreoDeRecuperacion(correoLimpio)) {
            ResultadoAutenticador.Exito -> ResultadoAuth.Exito()
            ResultadoAutenticador.CorreoNoRegistrado -> ResultadoAuth.Invalido(ErrorAuth.CorreoNoRegistrado)
            else -> ResultadoAuth.Invalido(ErrorAuth.ErrorDeRed)
        }
    }

    private fun validarRegistro(nombre: String, correo: String, contrasena: String): ErrorAuth? = when {
        nombre.isEmpty() -> ErrorAuth.NombreVacio
        nombre.length > MaxCaracteresNombre -> ErrorAuth.NombreMuyLargo
        !FormatoCorreo.matches(correo) -> ErrorAuth.CorreoInvalido
        !FormatoContrasena.matches(contrasena) -> ErrorAuth.ContrasenaInvalida
        else -> null
    }
}
