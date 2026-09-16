package com.gestor_ventures.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * TEMPORAL — BORRAR CUANDO EXISTA HU-01 (registro e inicio de sesión).
 *
 * La tabla `negocios` exige un usuario dueño, y mientras no haya pantallas de cuenta no hay
 * forma de crearlo. Esta semilla inserta un único usuario al crear la base de datos, para que
 * HU-05 (registrar negocio) y HU-11/HU-12 (registrar venta) puedan funcionar.
 *
 * Cuando HU-01 esté lista: se elimina este archivo, se quita el `addCallback` del módulo que
 * construye la base de datos y el usuario pasa a salir de la sesión real.
 */
object SemillaTemporal {

    /** Id fijo del usuario sembrado; hasta HU-01, es "el usuario" de toda la app. */
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
