package com.gestor_ventures.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val NombreBaseDePrueba = "migraciones-test.db"

/**
 * Las migraciones contra bases de datos reales de la versión anterior.
 *
 * Esto es lo que separa "actualicé la app" de "perdí todas mis ventas": si una migración está
 * mal escrita, a quien ya tiene la app instalada se le borran los datos. El archivo de prueba
 * se crea con el esquema viejo exportado en `app/schemas`, se le meten datos, y se verifica que
 * sigan ahí después de migrar.
 */
class MigracionesTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GestorVenturesDatabase::class.java,
    )

    @Test
    fun migracion_1_2_agregaElColorDeMarcaSinPerderElNegocio() {
        helper.createDatabase(NombreBaseDePrueba, 1).use { db ->
            db.execSQL(InsertarUsuario)
            db.execSQL(
                """
                INSERT INTO negocios
                    (negocio_id, usuario_id, nombre_negocio, tipo_actividad, categoria_negocio,
                     porcentaje_reinversion, fecha_creacion)
                VALUES (1, 1, 'Dulce Antojo', 'PRODUCTOS', 'Repostería', 0.0, 0)
                """,
            )
        }

        val db = helper.runMigrationsAndValidate(NombreBaseDePrueba, 2, true, MIGRACION_1_2)

        db.query("SELECT nombre_negocio, color_marca FROM negocios").use { fila ->
            assertTrue(fila.moveToFirst())
            assertEquals("Dulce Antojo", fila.getString(0))
            // El negocio que ya existía no tenía color: se queda con el azul de la app.
            assertTrue(fila.isNull(1))
        }
    }

    @Test
    fun migracion_2_3_agregaLaNotaSinPerderLasVentas() {
        helper.createDatabase(NombreBaseDePrueba, 2).use { db ->
            db.execSQL(InsertarUsuario)
            db.execSQL(
                """
                INSERT INTO negocios
                    (negocio_id, usuario_id, nombre_negocio, tipo_actividad, categoria_negocio,
                     porcentaje_reinversion, color_marca, fecha_creacion)
                VALUES (1, 1, 'Dulce Antojo', 'PRODUCTOS', 'Repostería', 0.0, '#B8E0D2', 0)
                """,
            )
            db.execSQL(
                """
                INSERT INTO ventas
                    (venta_id, negocio_id, tipo_registro, producto_servicio, monto, metodo_pago,
                     fecha_hora)
                VALUES (1, 1, 'DETALLADO', 'Torta de chocolate', 25000.0, 'EFECTIVO', 0)
                """,
            )
        }

        val db = helper.runMigrationsAndValidate(NombreBaseDePrueba, 3, true, MIGRACION_2_3)

        db.query("SELECT producto_servicio, monto, nota FROM ventas").use { fila ->
            assertTrue(fila.moveToFirst())
            assertEquals("Torta de chocolate", fila.getString(0))
            assertEquals(25_000.0, fila.getDouble(1), 0.001)
            // Las ventas viejas no tienen nota, y eso está bien: la columna admite nulos.
            assertTrue(fila.isNull(2))
        }
    }

    @Test
    fun laCadenaCompletaDeMigracionesLlegaHastaLaUltimaVersion() {
        helper.createDatabase(NombreBaseDePrueba, 1).use { db ->
            db.execSQL(InsertarUsuario)
        }

        // Quien instaló la app cuando salió y la actualiza hoy pasa por todas de una vez.
        val db = helper.runMigrationsAndValidate(NombreBaseDePrueba, 3, true, *MIGRACIONES)

        db.query("SELECT nombre FROM usuarios").use { fila ->
            assertTrue(fila.moveToFirst())
            assertEquals("Usuario de prueba", fila.getString(0))
        }
        db.query("PRAGMA foreign_key_check").use { huerfanas ->
            assertEquals("Quedaron filas apuntando a nada", 0, huerfanas.count)
        }
    }
}

private const val InsertarUsuario = """
    INSERT INTO usuarios (usuario_id, nombre, correo, contrasena_hash, fecha_creacion)
    VALUES (1, 'Usuario de prueba', 'usuario@gestorventures.local', '', 0)
"""
