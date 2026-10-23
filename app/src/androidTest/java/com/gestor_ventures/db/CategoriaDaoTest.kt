package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.CategoriaDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.entity.CategoriaEntity
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.enums.TipoActividad
import com.gestor_ventures.db.enums.TipoCategoria
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-15. Categorías contra una base de datos real en memoria.
 *
 * Lo delicado acá no es guardar un nombre: es que cada negocio vea solo las suyas, que el
 * selector de gastos no ofrezca categorías de costos, y que borrar una etiqueta no se lleve por
 * delante lo que estaba clasificado con ella.
 */
class CategoriaDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var categoriaDao: CategoriaDao

    private val ahora: LocalDateTime = LocalDateTime.of(2026, 9, 16, 10, 0)
    private var negocioId: Long = 0

    @Before
    fun crearBaseDeDatos() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        categoriaDao = db.categoriaDao()

        negocioId = negocioDao.insertar(negocio("Dulce Antojo"))
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    @Test
    fun laCategoriaSeGuardaConSuNombreYSuTipo() = runTest {
        val id = categoriaDao.insertar(categoria("Insumos", TipoCategoria.COSTO))

        val guardada = categoriaDao.obtener(id)

        assertEquals("Insumos", guardada?.nombreCategoria)
        assertEquals(TipoCategoria.COSTO, guardada?.tipoCategoria)
        assertEquals(negocioId, guardada?.negocioId)
    }

    @Test
    fun lasCategoriasSalenEnOrdenAlfabeticoSinImportarMayusculas() = runTest {
        categoriaDao.insertar(categoria("transporte", TipoCategoria.GASTO))
        categoriaDao.insertar(categoria("Arriendo", TipoCategoria.GASTO))
        categoriaDao.insertar(categoria("insumos", TipoCategoria.COSTO))

        val nombres = categoriaDao.observarDeNegocio(negocioId).first().map { it.nombreCategoria }

        assertEquals(listOf("Arriendo", "insumos", "transporte"), nombres)
    }

    @Test
    fun elSelectorDeGastosNoOfreceCategoriasDeCostos() = runTest {
        categoriaDao.insertar(categoria("Transporte", TipoCategoria.GASTO))
        categoriaDao.insertar(categoria("Insumos", TipoCategoria.COSTO))

        val deGasto = categoriaDao
            .observarDeNegocioPorTipo(negocioId, TipoCategoria.GASTO)
            .first()

        assertEquals(listOf("Transporte"), deGasto.map { it.nombreCategoria })
    }

    @Test
    fun cadaNegocioVeSoloSusCategorias() = runTest {
        val otroNegocioId = negocioDao.insertar(negocio("Bella Piel"))
        categoriaDao.insertar(categoria("Insumos", TipoCategoria.COSTO))
        categoriaDao.insertar(categoria("Insumos", TipoCategoria.COSTO, otroNegocioId))

        val delPrimero = categoriaDao.observarDeNegocio(negocioId).first()

        assertEquals(1, delPrimero.size)
        assertEquals(negocioId, delPrimero.single().negocioId)
    }

    @Test
    fun existeConNombre_delataElNombreRepetidoAunqueCambienLasMayusculas() = runTest {
        categoriaDao.insertar(categoria("Insumos", TipoCategoria.COSTO))

        assertEquals(1, categoriaDao.existeConNombre(negocioId, TipoCategoria.COSTO, "insumos"))
        assertEquals(1, categoriaDao.existeConNombre(negocioId, TipoCategoria.COSTO, "INSUMOS"))
        // El mismo nombre en el otro tipo es otra categoría: una cosa es gastar y otra costear.
        assertEquals(0, categoriaDao.existeConNombre(negocioId, TipoCategoria.GASTO, "Insumos"))
        assertEquals(0, categoriaDao.existeConNombre(negocioId, TipoCategoria.COSTO, "Transporte"))
    }

    @Test
    fun existeConNombre_noSeDelataASiMismaAlEditar() = runTest {
        val id = categoriaDao.insertar(categoria("Insumos", TipoCategoria.COSTO))

        // Corregirle una tilde a la misma categoría no puede contar como nombre repetido.
        val repetidas = categoriaDao.existeConNombre(
            negocioId = negocioId,
            tipo = TipoCategoria.COSTO,
            nombre = "Insumos",
            exceptoId = id,
        )

        assertEquals(0, repetidas)
    }

    @Test
    fun editarCambiaElNombreSinCrearOtra() = runTest {
        val id = categoriaDao.insertar(categoria("Insumos", TipoCategoria.COSTO))

        categoriaDao.actualizar(categoriaDao.obtener(id)!!.copy(nombreCategoria = "Materia prima"))

        val categorias = categoriaDao.observarDeNegocio(negocioId).first()
        assertEquals(1, categorias.size)
        assertEquals("Materia prima", categorias.single().nombreCategoria)
    }

    @Test
    fun eliminarLaSacaDeLaLista() = runTest {
        val id = categoriaDao.insertar(categoria("Insumos", TipoCategoria.COSTO))

        categoriaDao.eliminar(categoriaDao.obtener(id)!!)

        assertNull(categoriaDao.obtener(id))
        assertEquals(emptyList<Any>(), categoriaDao.observarDeNegocio(negocioId).first())
    }

    private fun categoria(
        nombre: String,
        tipo: TipoCategoria,
        negocio: Long = negocioId,
    ) = CategoriaEntity(
        negocioId = negocio,
        nombreCategoria = nombre,
        tipoCategoria = tipo,
    )

    private fun negocio(nombre: String) = NegocioEntity(
        usuarioId = SemillaTemporal.USUARIO_ID,
        nombreNegocio = nombre,
        tipoActividad = TipoActividad.PRODUCTOS,
        categoriaNegocio = "Repostería",
        porcentajeReinversion = 0.0,
        fechaCreacion = ahora,
    )
}
