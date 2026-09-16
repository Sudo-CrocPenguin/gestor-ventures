package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.CategoriaDao
import com.gestor_ventures.db.dao.CostoDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.entity.CategoriaEntity
import com.gestor_ventures.db.entity.CostoEntity
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
 * HU-13. Costos contra una base de datos real en memoria.
 *
 * Los bordes del mes son lo delicado, igual que en ventas y gastos: las fechas se guardan como
 * milisegundos y un error de uno deja por fuera el primer o el último costo del periodo.
 */
class CostoDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var categoriaDao: CategoriaDao
    private lateinit var costoDao: CostoDao

    private val desde: LocalDateTime = LocalDateTime.of(2026, 9, 1, 0, 0)
    private val hasta: LocalDateTime = LocalDateTime.of(2026, 9, 30, 23, 59, 59)

    private var negocioId: Long = 0
    private var categoriaId: Long = 0

    @Before
    fun crearBaseDeDatos() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        categoriaDao = db.categoriaDao()
        costoDao = db.costoDao()

        negocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = desde,
            ),
        )
        categoriaId = categoriaDao.insertar(
            CategoriaEntity(
                negocioId = negocioId,
                nombreCategoria = "Insumos",
                tipoCategoria = TipoCategoria.COSTO,
            ),
        )
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    @Test
    fun elCostoSeGuardaConSuProductoYSuCategoria() = runTest {
        val id = costoDao.insertar(costo(producto = "Torta de chocolate", monto = 18_000.0))

        val guardado = costoDao.obtener(id)

        assertEquals("Torta de chocolate", guardado?.productoServicio)
        assertEquals(18_000.0, guardado?.montoCosto)
        assertEquals(categoriaId, guardado?.categoriaId)
    }

    @Test
    fun unCostoPuedeQuedarSinCategoria() = runTest {
        val id = costoDao.insertar(costo(producto = "Empaques", monto = 3_000.0, categoria = null))

        assertNull(costoDao.obtener(id)?.categoriaId)
    }

    @Test
    fun elPrimeroYElUltimoCostoDelMesCuentan() = runTest {
        costoDao.insertar(costo(producto = "Uno", monto = 10_000.0, fecha = desde))
        costoDao.insertar(costo(producto = "Treinta", monto = 20_000.0, fecha = hasta))
        costoDao.insertar(costo(producto = "Mes pasado", monto = 99_000.0, fecha = desde.minusDays(1)))

        assertEquals(30_000.0, costoDao.observarTotalEntre(negocioId, desde, hasta).first(), 0.001)
    }

    @Test
    fun losCostosSalenDelMasRecienteAlMasAntiguo() = runTest {
        costoDao.insertar(costo(producto = "Primero", monto = 10_000.0, dia = 5))
        costoDao.insertar(costo(producto = "Ultimo", monto = 20_000.0, dia = 20))
        costoDao.insertar(costo(producto = "Medio", monto = 15_000.0, dia = 12))

        val costos = costoDao.observarEntre(negocioId, desde, hasta).first()

        assertEquals(listOf("Ultimo", "Medio", "Primero"), costos.map { it.productoServicio })
    }

    @Test
    fun dosCostosDelMismoMomentoNoSalenEnCualquierOrden() = runTest {
        val primero = costoDao.insertar(costo(producto = "Uno", monto = 10_000.0, dia = 10))
        val segundo = costoDao.insertar(costo(producto = "Otro", monto = 20_000.0, dia = 10))

        val costos = costoDao.observarEntre(negocioId, desde, hasta).first()

        assertEquals(listOf(segundo, primero), costos.map { it.costoId })
    }

    @Test
    fun unMesSinCostosTotalizaCeroYNoNulo() = runTest {
        assertEquals(0.0, costoDao.observarTotalEntre(negocioId, desde, hasta).first(), 0.001)
    }

    @Test
    fun elTotalIgnoraLosCostosDeOtroNegocio() = runTest {
        val otroNegocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Bella Piel",
                tipoActividad = TipoActividad.SERVICIOS,
                categoriaNegocio = "Belleza",
                porcentajeReinversion = 0.0,
                fechaCreacion = desde,
            ),
        )
        costoDao.insertar(costo(producto = "Mío", monto = 10_000.0, dia = 10))
        costoDao.insertar(
            costo(producto = "Ajeno", monto = 99_000.0, dia = 10, negocio = otroNegocioId, categoria = null),
        )

        assertEquals(10_000.0, costoDao.observarTotalEntre(negocioId, desde, hasta).first(), 0.001)
    }

    @Test
    fun elTotalPorCategoriaSumaCadaUnaYDejaAparteLoSinClasificar() = runTest {
        val empaques = categoriaDao.insertar(
            CategoriaEntity(
                negocioId = negocioId,
                nombreCategoria = "Empaques",
                tipoCategoria = TipoCategoria.COSTO,
            ),
        )
        costoDao.insertar(costo(producto = "Harina", monto = 12_000.0, dia = 5))
        costoDao.insertar(costo(producto = "Huevos", monto = 8_000.0, dia = 6))
        costoDao.insertar(costo(producto = "Cajas", monto = 30_000.0, dia = 7, categoria = empaques))
        costoDao.insertar(costo(producto = "Varios", monto = 5_000.0, dia = 8, categoria = null))

        val totales = costoDao.observarTotalPorCategoriaEntre(negocioId, desde, hasta).first()

        assertEquals(listOf(empaques, categoriaId, null), totales.map { it.categoriaId })
        assertEquals(listOf(30_000.0, 20_000.0, 5_000.0), totales.map { it.total })
    }

    @Test
    fun borrarLaCategoriaNoSeLlevaElCosto() = runTest {
        val id = costoDao.insertar(costo(producto = "Harina", monto = 12_000.0, dia = 10))

        categoriaDao.eliminar(categoriaDao.obtener(categoriaId)!!)

        assertNull(costoDao.obtener(id)?.categoriaId)
        assertEquals(12_000.0, costoDao.observarTotalEntre(negocioId, desde, hasta).first(), 0.001)
    }

    private fun costo(
        producto: String,
        monto: Double,
        dia: Int = 15,
        fecha: LocalDateTime = desde.withDayOfMonth(dia),
        negocio: Long = negocioId,
        categoria: Long? = categoriaId,
    ) = CostoEntity(
        negocioId = negocio,
        categoriaId = categoria,
        productoServicio = producto,
        montoCosto = monto,
        fechaRegistro = fecha,
    )
}
