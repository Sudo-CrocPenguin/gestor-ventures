package com.gestor_ventures.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.ZoneId

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

/** v2 → v3: HU-11/HU-12 agrega la nota de la venta. */
val MIGRACION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ventas ADD COLUMN nota TEXT")
    }
}

/**
 * v3 → v4: HU-20 necesita que el gasto diga a qué hora fue.
 *
 * `gastos.fecha` guardaba solo el día (epoch day) y pasa a ser `gastos.fecha_hora` con fecha y
 * hora (epoch millis), como ya lo hacían ventas y costos. Sin la hora, un gasto no se puede
 * ubicar dentro de una jornada de caja: con dos turnos en el mismo día caía en los dos.
 *
 * SQLite no sabe convertir un día en un instante —eso depende de la zona horaria—, así que la
 * conversión se hace fila por fila desde Kotlin, con las mismas reglas de [Converters]: el día
 * se interpreta como su medianoche local. Un gasto viejo queda entonces a las 00:00 de su día,
 * que es toda la precisión que de verdad existía.
 */
val MIGRACION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gastos_nuevo` (
                `gasto_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `negocio_id` INTEGER NOT NULL,
                `categoria_id` INTEGER,
                `descripcion` TEXT NOT NULL,
                `monto` REAL NOT NULL,
                `fecha_hora` INTEGER NOT NULL,
                FOREIGN KEY(`negocio_id`) REFERENCES `negocios`(`negocio_id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`categoria_id`) REFERENCES `categorias`(`categoria_id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )

        // Se leen todas las filas antes de tocar nada: escribir en la misma tabla que se está
        // recorriendo deja el cursor en un estado que depende de la implementación.
        val dias = mutableListOf<Pair<Long, Long>>()
        db.query("SELECT `gasto_id`, `fecha` FROM `gastos`").use { fila ->
            while (fila.moveToNext()) {
                dias += fila.getLong(0) to fila.getLong(1)
            }
        }
        dias.forEach { (gastoId, epochDay) ->
            db.execSQL(
                "UPDATE `gastos` SET `fecha` = ? WHERE `gasto_id` = ?",
                arrayOf<Any>(aMedianocheLocal(epochDay), gastoId),
            )
        }

        db.execSQL(
            """
            INSERT INTO `gastos_nuevo`
                (`gasto_id`, `negocio_id`, `categoria_id`, `descripcion`, `monto`, `fecha_hora`)
            SELECT `gasto_id`, `negocio_id`, `categoria_id`, `descripcion`, `monto`, `fecha`
            FROM `gastos`
            """.trimIndent(),
        )

        db.execSQL("DROP TABLE `gastos`")
        db.execSQL("ALTER TABLE `gastos_nuevo` RENAME TO `gastos`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gastos_negocio_id` ON `gastos` (`negocio_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gastos_categoria_id` ON `gastos` (`categoria_id`)")
    }
}

/** El día suelto que guardaba la versión 3, leído como su medianoche en la zona del equipo. */
private fun aMedianocheLocal(epochDay: Long): Long =
    LocalDate.ofEpochDay(epochDay)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

/**
 * v4 → v5: HU-18 guarda cuándo se corrigió cada registro.
 *
 * Columna nueva y nada más, así que basta un ALTER por tabla: no hay que reconstruirlas como
 * en [MIGRACION_3_4]. Lo que ya estaba queda con la fecha en nulo, que es la verdad — esos
 * registros nunca se editaron, o se editaron antes de que la app lo anotara.
 */
val MIGRACION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("ventas", "gastos", "costos").forEach { tabla ->
            db.execSQL("ALTER TABLE `$tabla` ADD COLUMN `fecha_modificacion` INTEGER")
        }
    }
}

/**
 * v5 → v6: HU-11 deja que cada negocio apague los métodos de pago que no usa.
 *
 * Tabla nueva y vacía: sin filas todos los métodos están activos, que es como funcionaba la app.
 */
val MIGRACION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `metodos_pago_inactivos` (
                `metodo_pago_inactivo_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `negocio_id` INTEGER NOT NULL,
                `metodo_pago` TEXT NOT NULL,
                FOREIGN KEY(`negocio_id`) REFERENCES `negocios`(`negocio_id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_metodos_pago_inactivos_negocio_id_metodo_pago` " +
                "ON `metodos_pago_inactivos` (`negocio_id`, `metodo_pago`)",
        )
    }
}

/**
 * v6 → v7: el negocio guarda el celular de WhatsApp con el que escribe a sus clientes, y nace la
 * tabla del equipo de vendedores. Una columna y una tabla nuevas, ambas vacías: nada de lo que ya
 * existía cambia.
 */
val MIGRACION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `negocios` ADD COLUMN `whatsapp_empresa` TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `miembros_equipo` (
                `miembro_equipo_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `negocio_id` INTEGER NOT NULL,
                `correo` TEXT NOT NULL,
                `estado_miembro` TEXT NOT NULL,
                `fecha_invitacion` INTEGER NOT NULL,
                FOREIGN KEY(`negocio_id`) REFERENCES `negocios`(`negocio_id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_miembros_equipo_negocio_id_correo` " +
                "ON `miembros_equipo` (`negocio_id`, `correo`)",
        )
    }
}

/** Todas las migraciones, en orden, para pasárselas al constructor de la base de datos. */
val MIGRACIONES = arrayOf(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5, MIGRACION_5_6, MIGRACION_6_7)
