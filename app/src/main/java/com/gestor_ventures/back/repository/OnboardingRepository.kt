package com.gestor_ventures.back.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Qué cuentas acaban de crearse y todavía no registraron su primer negocio.
 *
 * Solo a esas cuentas la app las lleva sola a "Registrar negocio". Quien simplemente inicia
 * sesión con una cuenta que ya tenía no pasa por ahí, aunque este dispositivo no tenga sus
 * negocios (los negocios se guardan en cada dispositivo, no en la cuenta): para eso está el
 * botón "Agregar negocio" del menú.
 *
 * Se identifica a la cuenta por su correo porque la marca se pone antes de crearla, cuando todavía
 * no existe un id.
 */
interface Onboarding {
    val pendientes: StateFlow<Set<String>>
    fun marcarPendiente(correo: String)
    fun completar(correo: String)
}

/** Guarda las cuentas pendientes en `SharedPreferences`, para que sobrevivan a cerrar la app. */
@Singleton
class OnboardingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : Onboarding {

    private val preferencias: SharedPreferences
        get() = context.getSharedPreferences(Archivo, Context.MODE_PRIVATE)

    private val _pendientes = MutableStateFlow(preferencias.getStringSet(Clave, emptySet()).orEmpty().toSet())
    override val pendientes: StateFlow<Set<String>> = _pendientes.asStateFlow()

    override fun marcarPendiente(correo: String) = cambiar { it + correo.trim().lowercase() }

    override fun completar(correo: String) = cambiar { it - correo.trim().lowercase() }

    private fun cambiar(transformar: (Set<String>) -> Set<String>) {
        val nuevo = transformar(_pendientes.value)
        preferencias.edit().putStringSet(Clave, nuevo).apply()
        _pendientes.value = nuevo
    }

    private companion object {
        const val Archivo = "gestor_ventures_onboarding"
        const val Clave = "cuentas_sin_negocio"
    }
}
