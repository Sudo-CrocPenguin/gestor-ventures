package com.gestor_ventures.back.notificaciones

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AjustesNotificaciones(
    val recordatorios: Boolean = true,
    val alertas: Boolean = true,
)

@Singleton
class PreferenciasNotificaciones @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val preferencias: SharedPreferences
        get() = context.getSharedPreferences(Archivo, Context.MODE_PRIVATE)

    fun actuales(): AjustesNotificaciones = AjustesNotificaciones(
        recordatorios = preferencias.getBoolean(ClaveRecordatorios, true),
        alertas = preferencias.getBoolean(ClaveAlertas, true),
    )

    fun observar(): Flow<AjustesNotificaciones> = callbackFlow {
        val escucha = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(actuales()) }
        val archivo = preferencias
        archivo.registerOnSharedPreferenceChangeListener(escucha)
        trySend(actuales())
        awaitClose { archivo.unregisterOnSharedPreferenceChangeListener(escucha) }
    }

    fun cambiarRecordatorios(activos: Boolean) {
        preferencias.edit().putBoolean(ClaveRecordatorios, activos).apply()
    }

    fun cambiarAlertas(activas: Boolean) {
        preferencias.edit().putBoolean(ClaveAlertas, activas).apply()
    }

    fun permite(canal: String): Boolean = when (canal) {
        Notificador.CanalAgenda -> actuales().recordatorios
        Notificador.CanalAlertas -> actuales().alertas
        else -> true
    }

    private companion object {
        const val Archivo = "notificaciones"
        const val ClaveRecordatorios = "recordatorios"
        const val ClaveAlertas = "alertas"
    }
}
