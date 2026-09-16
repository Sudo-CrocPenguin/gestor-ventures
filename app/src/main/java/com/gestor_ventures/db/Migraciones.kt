package com.gestor_ventures.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Cambios de esquema entre versiones de la base de datos.
 *
 * Cada migración se escribe una sola vez y no se vuelve a tocar: es lo que permite que a un
 * usuario que ya tiene la app instalada no se le borren los datos al actualizar.
 */

/** v1 → v2: HU-05 agrega el color de marca del negocio. */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE negocios ADD COLUMN color_marca TEXT")
    }
}

/** Todas las migraciones, en orden, para pasárselas al constructor de la base de datos. */
val MIGRACIONES = arrayOf(MIGRACION_1_2)
