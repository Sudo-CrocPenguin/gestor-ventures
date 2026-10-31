package com.gestor_ventures.back.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recuerda quién ya vio el tour de bienvenida.
 *
 * Es un dato de este teléfono y de cada cuenta (dos personas pueden compartir el aparato), no del
 * negocio, así que va en `SharedPreferences` y no en la base de datos: igual que
 * `PreferenciasNotificaciones`.
 */
@Singleton
class TourRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val preferencias: SharedPreferences
        get() = context.getSharedPreferences(Archivo, Context.MODE_PRIVATE)

    fun yaLoVio(usuarioId: Long): Boolean = preferencias.getBoolean(clave(usuarioId), false)

    fun marcarVisto(usuarioId: Long) {
        preferencias.edit().putBoolean(clave(usuarioId), true).apply()
    }

    private fun clave(usuarioId: Long) = "visto_$usuarioId"

    private companion object {
        const val Archivo = "gestor_ventures_tour"
    }
}
