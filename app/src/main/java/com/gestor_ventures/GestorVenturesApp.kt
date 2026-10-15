package com.gestor_ventures

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de Hilt: desde acá se construyen la base de datos y los repositorios. */
@HiltAndroidApp
class GestorVenturesApp : Application()
