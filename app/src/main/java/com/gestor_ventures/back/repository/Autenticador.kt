package com.gestor_ventures.back.repository

/**
 * Puerta hacia el proveedor de identidad (Firebase Authentication).
 *
 * `AuthRepository` solo conoce esta interfaz, nunca el SDK de Firebase directamente: así las
 * pruebas la reemplazan por una versión en memoria ([FirebaseAutenticador] es la única
 * implementación real) sin depender de Google Play Services en la JVM.
 */
interface Autenticador {

    /** HU-01. Crea la cuenta en el proveedor de identidad. */
    suspend fun crearCuenta(correo: String, contrasena: String): ResultadoAutenticador

    /** HU-02. Valida las credenciales contra el proveedor de identidad. */
    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAutenticador

    /** HU-03. Pide al proveedor que mande el enlace de recuperación al correo indicado. */
    suspend fun enviarCorreoDeRecuperacion(correo: String): ResultadoAutenticador

    /** HU-02. Cierra la sesión: cierre manual o expiración por inactividad. */
    fun cerrarSesion()
}

/** Resultado de una operación contra el proveedor de identidad, sin exponer sus excepciones. */
sealed interface ResultadoAutenticador {
    data object Exito : ResultadoAutenticador
    data object CorreoYaRegistrado : ResultadoAutenticador
    data object CredencialesInvalidas : ResultadoAutenticador
    data object CorreoNoRegistrado : ResultadoAutenticador
    data object ErrorDeRed : ResultadoAutenticador
}
