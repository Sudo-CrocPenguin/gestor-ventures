package com.gestor_ventures.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

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
    fun migracion_3_4_leDaHoraAlGastoSinPerderNada() {
        val diaDelGasto = LocalDate.of(2026, 9, 16)

        helper.createDatabase(NombreBaseDePrueba, 3).use { db ->
            db.execSQL(InsertarUsuario)
            db.execSQL(InsertarNegocio)
            db.execSQL(
                """
                INSERT INTO categorias (categoria_id, negocio_id, nombre_categoria, tipo_categoria)
                VALUES (1, 1, 'Transporte', 'GASTO')
                """,
            )
            db.execSQL(
                """
                INSERT INTO gastos
                    (gasto_id, negocio_id, categoria_id, descripcion, monto, fecha)
                VALUES (1, 1, 1, 'Domicilio de insumos', 12000.0, ?)
                """,
                arrayOf<Any>(diaDelGasto.toEpochDay()),
            )
            db.execSQL(
                """
                INSERT INTO gastos
                    (gasto_id, negocio_id, categoria_id, descripcion, monto, fecha)
                VALUES (2, 1, NULL, 'Varios', 5000.0, ?)
                """,
                arrayOf<Any>(diaDelGasto.toEpochDay()),
            )
        }

        val db = helper.runMigrationsAndValidate(NombreBaseDePrueba, 4, true, MIGRACION_3_4)

        db.query("SELECT gasto_id, categoria_id, descripcion, monto, fecha_hora FROM gastos ORDER BY gasto_id").use { fila ->
            assertTrue(fila.moveToFirst())
            assertEquals(1L, fila.getLong(0))
            assertEquals(1L, fila.getLong(1))
            assertEquals("Domicilio de insumos", fila.getString(2))
            assertEquals(12_000.0, fila.getDouble(3), 0.001)
            // El día suelto se vuelve su medianoche local: es toda la precisión que existía.
            assertEquals(enMillis(diaDelGasto), fila.getLong(4))

            assertTrue(fila.moveToNext())
            assertEquals(5_000.0, fila.getDouble(3), 0.001)
            // El gasto sin clasificar sigue sin clasificar: la columna admite nulos.
            assertTrue(fila.isNull(1))
        }

        // La tabla se reconstruye: si los índices o las llaves no volvieran, esto lo delata.
        db.query("PRAGMA foreign_key_check").use { huerfanas ->
            assertEquals("Quedaron filas apuntando a nada", 0, huerfanas.count)
        }
        db.query("PRAGMA index_list(`gastos`)").use { indices ->
            assertEquals(2, indices.count)
        }
    }

    @Test
    fun laCadenaCompletaDeMigracionesLlegaHastaLaUltimaVersion() {
        helper.createDatabase(NombreBaseDePrueba, 1).use { db ->
            db.execSQL(InsertarUsuario)
        }

        // Quien instaló la app cuando salió y la actualiza hoy pasa por todas de una vez.
        val db = helper.runMigrationsAndValidate(NombreBaseDePrueba, 4, true, *MIGRACIONES)

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

private const val InsertarNegocio = """
    INSERT INTO negocios
        (negocio_id, usuario_id, nombre_negocio, tipo_actividad, categoria_negocio,
         porcentaje_reinversion, color_marca, fecha_creacion)
    VALUES (1, 1, 'Dulce Antojo', 'PRODUCTOS', 'Repostería', 0.0, '#B8E0D2', 0)
"""

/** Lo mismo que hace `Converters`: el día, a su medianoche en la zona del equipo. */
private fun enMillis(dia: LocalDate): Long =
    dia.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
