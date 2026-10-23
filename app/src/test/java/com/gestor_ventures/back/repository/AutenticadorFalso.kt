package com.gestor_ventures.back.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [Autenticador] de mentiras para las pruebas de JVM: guarda las cuentas creadas en memoria,
 * simula quién tiene la sesión abierta con [sesion], y permite forzar una respuesta puntual
 * (p. ej. [ResultadoAutenticador.ErrorDeRed]) para probar cómo reacciona [AuthRepository]
 * cuando Firebase falla.
 */
class AutenticadorFalso : Autenticador {

    private val cuentas = mutableMapOf<String, String>()

    /** El correo de la sesión simulada. Mutable a propósito: las pruebas la fuerzan directo. */
    val sesion = MutableStateFlow<String?>(null)

    var siguienteCrearCuenta: ResultadoAutenticador? = null
    var siguienteIniciarSesion: ResultadoAutenticador? = null
    var siguienteEnviarCorreo: ResultadoAutenticador? = null

    override suspend fun crearCuenta(correo: String, contrasena: String): ResultadoAutenticador {
        siguienteCrearCuenta?.let { return it }
        if (cuentas.containsKey(correo)) return ResultadoAutenticador.CorreoYaRegistrado
        cuentas[correo] = contrasena
        sesion.value = correo
        return ResultadoAutenticador.Exito
    }

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAutenticador {
        siguienteIniciarSesion?.let { return it }
        val registrada = cuentas[correo] ?: return ResultadoAutenticador.CorreoNoRegistrado
        if (registrada != contrasena) return ResultadoAutenticador.CredencialesInvalidas
        sesion.value = correo
        return ResultadoAutenticador.Exito
    }

    override suspend fun enviarCorreoDeRecuperacion(correo: String): ResultadoAutenticador {
        siguienteEnviarCorreo?.let { return it }
        return if (cuentas.containsKey(correo)) {
            ResultadoAutenticador.Exito
        } else {
            ResultadoAutenticador.CorreoNoRegistrado
        }
    }

    override fun observarCorreoDeSesion(): Flow<String?> = sesion

    override fun cerrarSesion() {
        sesion.value = null
    }
}
