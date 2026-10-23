package com.gestor_ventures

import android.app.Application
import com.gestor_ventures.back.notificaciones.AlertasWorker
import com.gestor_ventures.back.notificaciones.Notificador
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de Hilt: desde acá se construyen la base de datos y los repositorios. */
@HiltAndroidApp
class GestorVenturesApp : Application() {

    /**
     * HU-26. Los canales de notificación tienen que existir antes del primer aviso.
     * HU-38. Y la revisión diaria de alertas queda programada desde que se abre la app.
     */
    override fun onCreate() {
        super.onCreate()
        Notificador.crearCanales(this)
        AlertasWorker.programar(this)
    }
}
