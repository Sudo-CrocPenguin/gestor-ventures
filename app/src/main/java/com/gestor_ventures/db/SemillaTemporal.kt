package com.gestor_ventures.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * HU-01 ya existe (`AuthRepository`/`SesionRepository`), así que la base de datos de producción
 * ya no usa [callback]: cada usuario se crea al registrarse de verdad.
 *
 * Este archivo se conserva solo como fixture para pruebas (unitarias y `androidTest`) que
 * necesitan *algún* usuario dueño de un negocio y no les importa la autenticación — son
 * decenas de pruebas de otras épicas (HU-05 en adelante) que darían mucho más ruido si cada
 * una tuviera que armar su propia sesión. [USUARIO_ID] es el id que ese usuario de prueba
 * siempre recibe.
 */
object SemillaTemporal {

    /** Id fijo del usuario sembrado, para pruebas que necesitan un usuario dueño cualquiera. */
    const val USUARIO_ID: Long = 1L

    private const val NOMBRE = "Usuario de prueba"
    private const val CORREO = "usuario@gestorventures.local"

    private const val INSERTAR_USUARIO = """
        INSERT INTO usuarios (usuario_id, nombre, correo, contrasena_hash, fecha_creacion)
        VALUES (?, ?, ?, ?, ?)
    """

    /**
     * Se ejecuta una sola vez, cuando el archivo de base de datos se crea. `Converters` guarda
     * las fechas como epoch millis, por eso [System.currentTimeMillis].
     */
    val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.execSQL(
                INSERTAR_USUARIO,
                arrayOf<Any>(USUARIO_ID, NOMBRE, CORREO, "", System.currentTimeMillis()),
            )
        }
    }
}
