package com.gestor_ventures.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.dao.UsuarioDao
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.entity.UsuarioEntity
import com.gestor_ventures.db.enums.TipoActividad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-05. Prueba el DAO de negocios contra una base de datos real de Room, en memoria.
 * Corre en el emulador porque Room necesita SQLite de Android.
 */
class NegocioDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var usuarioDao: UsuarioDao

    private val ahora: LocalDateTime = LocalDateTime.of(2026, 9, 16, 10, 0)

    @Before
    fun crearBaseDeDatos() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        usuarioDao = db.usuarioDao()
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    private fun negocio(
        nombre: String,
        usuarioId: Long = SemillaTemporal.USUARIO_ID,
        colorMarca: String? = null,
    ) = NegocioEntity(
        usuarioId = usuarioId,
        nombreNegocio = nombre,
        tipoActividad = TipoActividad.PRODUCTOS,
        categoriaNegocio = "Repostería",
        porcentajeReinversion = 30.0,
        colorMarca = colorMarca,
        fechaCreacion = ahora,
    )

    @Test
    fun colorDeMarca_seGuardaYPuedeQuedarVacio() = runTest {
        val conColor = negocioDao.insertar(negocio("Bella Piel", colorMarca = "#B8E0D2"))
        val sinColor = negocioDao.insertar(negocio("Dulce Antojo"))

        assertEquals("#B8E0D2", negocioDao.obtener(conColor)?.colorMarca)
        assertNull(negocioDao.obtener(sinColor)?.colorMarca)
    }

    @Test
    fun laSemillaCreaElUsuarioTemporal() = runTest {
        val usuario = usuarioDao.obtener(SemillaTemporal.USUARIO_ID)

        assertEquals("Usuario de prueba", usuario?.nombre)
    }

    @Test
    fun insertarNegocio_devuelveSuIdYSePuedeLeer() = runTest {
        val id = negocioDao.insertar(negocio("Dulce Antojo"))

        val guardado = negocioDao.obtener(id)
        assertEquals("Dulce Antojo", guardado?.nombreNegocio)
        assertEquals(TipoActividad.PRODUCTOS, guardado?.tipoActividad)
        assertEquals(30.0, guardado?.porcentajeReinversion ?: 0.0, 0.001)
        assertEquals(ahora, guardado?.fechaCreacion)
    }

    @Test
    fun observarDeUsuario_devuelveLosNegociosOrdenadosPorNombre() = runTest {
        negocioDao.insertar(negocio("Dulce Antojo"))
        negocioDao.insertar(negocio("Bella Piel"))

        val negocios = negocioDao.observarDeUsuario(SemillaTemporal.USUARIO_ID).first()

        assertEquals(listOf("Bella Piel", "Dulce Antojo"), negocios.map { it.nombreNegocio })
    }

    @Test
    fun contarDeUsuario_sirveParaSaberSiHayQueCrearElPrimerNegocio() = runTest {
        assertEquals(0, negocioDao.contarDeUsuario(SemillaTemporal.USUARIO_ID))

        negocioDao.insertar(negocio("Dulce Antojo"))

        assertEquals(1, negocioDao.contarDeUsuario(SemillaTemporal.USUARIO_ID))
    }

    @Test
    fun actualizar_cambiaLosDatosDelNegocio() = runTest {
        val id = negocioDao.insertar(negocio("Dulce Antojo"))
        val guardado = negocioDao.obtener(id)!!

        negocioDao.actualizar(
            guardado.copy(nombreNegocio = "Dulce Antojo SAS", porcentajeReinversion = 45.0),
        )

        val actualizado = negocioDao.obtener(id)
        assertEquals("Dulce Antojo SAS", actualizado?.nombreNegocio)
        assertEquals(45.0, actualizado?.porcentajeReinversion ?: 0.0, 0.001)
    }

    @Test
    fun noSePuedeCrearUnNegocioDeUnUsuarioQueNoExiste() = runTest {
        var fallo = false
        try {
            negocioDao.insertar(negocio("Sin dueño", usuarioId = 999L))
        } catch (e: SQLiteConstraintException) {
            fallo = true
        }

        assertTrue("La llave foránea debe impedir negocios sin usuario", fallo)
        assertNull(negocioDao.obtener(999L))
    }

    @Test
    fun borrarElUsuario_borraSusNegocios() = runTest {
        negocioDao.insertar(negocio("Dulce Antojo"))
        val usuario: UsuarioEntity = usuarioDao.obtener(SemillaTemporal.USUARIO_ID)!!

        db.openHelper.writableDatabase.execSQL(
            "DELETE FROM usuarios WHERE usuario_id = ?",
            arrayOf(usuario.usuarioId),
        )

        assertEquals(0, negocioDao.contarDeUsuario(SemillaTemporal.USUARIO_ID))
    }
}
