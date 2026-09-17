package com.gestor_ventures.back.repository

/**
 * [Autenticador] de mentiras para las pruebas de JVM: guarda las cuentas creadas en memoria y
 * permite forzar una respuesta puntual (p. ej. [ResultadoAutenticador.ErrorDeRed]) para probar
 * cómo reacciona [AuthRepository] cuando Firebase falla.
 */
class AutenticadorFalso : Autenticador {

    private val cuentas = mutableMapOf<String, String>()
    var siguienteCrearCuenta: ResultadoAutenticador? = null
    var siguienteIniciarSesion: ResultadoAutenticador? = null
    var siguienteEnviarCorreo: ResultadoAutenticador? = null
    var sesionCerrada = false
        private set

    override suspend fun crearCuenta(correo: String, contrasena: String): ResultadoAutenticador {
        siguienteCrearCuenta?.let { return it }
        if (cuentas.containsKey(correo)) return ResultadoAutenticador.CorreoYaRegistrado
        cuentas[correo] = contrasena
        return ResultadoAutenticador.Exito
    }

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAutenticador {
        siguienteIniciarSesion?.let { return it }
        val registrada = cuentas[correo] ?: return ResultadoAutenticador.CorreoNoRegistrado
        return if (registrada == contrasena) {
            ResultadoAutenticador.Exito
        } else {
            ResultadoAutenticador.CredencialesInvalidas
        }
    }

    override suspend fun enviarCorreoDeRecuperacion(correo: String): ResultadoAutenticador {
        siguienteEnviarCorreo?.let { return it }
        return if (cuentas.containsKey(correo)) {
            ResultadoAutenticador.Exito
        } else {
            ResultadoAutenticador.CorreoNoRegistrado
        }
    }

    override fun cerrarSesion() {
        sesionCerrada = true
    }
}
