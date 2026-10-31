package com.gestor_ventures.back.repository

import android.content.Context
import android.content.SharedPreferences
import com.gestor_ventures.back.model.ModoTema
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Si la app se ve clara, oscura o como el dispositivo.
 *
 * Es una preferencia de este teléfono, no del negocio ni de la cuenta, así que va en
 * `SharedPreferences` como las de notificaciones. El valor se sostiene en un `StateFlow` para
 * que el cambio se vea al instante en toda la app, también en el login.
 */
@Singleton
class AparienciaRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val preferencias: SharedPreferences
        get() = context.getSharedPreferences(Archivo, Context.MODE_PRIVATE)

    private val _modo = MutableStateFlow(leer())
    val modo: StateFlow<ModoTema> = _modo.asStateFlow()

    fun cambiar(modo: ModoTema) {
        preferencias.edit().putString(Clave, modo.name).apply()
        _modo.value = modo
    }

    /** Lo que no se reconoce —o nunca se eligió— es seguir al dispositivo, como hasta ahora. */
    private fun leer(): ModoTema =
        ModoTema.entries.firstOrNull { it.name == preferencias.getString(Clave, null) } ?: ModoTema.SISTEMA

    private companion object {
        const val Archivo = "gestor_ventures_apariencia"
        const val Clave = "modo_tema"
    }
}
