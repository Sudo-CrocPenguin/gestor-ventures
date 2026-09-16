package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.CategoriaDao
import com.gestor_ventures.db.dao.GastoDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.entity.CategoriaEntity
import com.gestor_ventures.db.entity.GastoEntity
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-14. Gastos generales contra una base de datos real en memoria.
 *
 * Lo que de verdad se prueba acá son los bordes del rango y qué pasa con un gasto cuya
 * categoría se borra: no puede desaparecer de las finanzas por perder su etiqueta.
 */
class GastoDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var categoriaDao: CategoriaDao
    private lateinit var gastoDao: GastoDao

    private val mes: LocalDate = LocalDate.of(2026, 9, 1)
    private val finDeMes: LocalDate = LocalDate.of(2026, 9, 30)

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
        gastoDao = db.gastoDao()

        negocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = LocalDateTime.of(2026, 9, 1, 8, 0),
            ),
        )
        categoriaId = categoriaDao.insertar(
            CategoriaEntity(
                negocioId = negocioId,
                nombreCategoria = "Transporte",
                tipoCategoria = TipoCategoria.GASTO,
            ),
        )
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    @Test
    fun elGastoSeGuardaConSuCategoriaYSuFecha() = runTest {
        val id = gastoDao.insertar(gasto(descripcion = "Domicilio", monto = 12_000.0, dia = 10))

        val guardado = gastoDao.obtener(id)

        assertEquals("Domicilio", guardado?.descripcion)
        assertEquals(12_000.0, guardado?.monto)
        assertEquals(LocalDate.of(2026, 9, 10), guardado?.fecha)
        assertEquals(categoriaId, guardado?.categoriaId)
    }

    @Test
    fun unGastoPuedeQuedarSinCategoria() = runTest {
        val id = gastoDao.insertar(gasto(descripcion = "Varios", monto = 5_000.0, categoria = null))

        assertNull(gastoDao.obtener(id)?.categoriaId)
    }

    @Test
    fun elPrimeroYElUltimoDiaDelMesCuentan() = runTest {
        gastoDao.insertar(gasto(descripcion = "Uno", monto = 10_000.0, dia = 1))
        gastoDao.insertar(gasto(descripcion = "Treinta", monto = 20_000.0, dia = 30))
        gastoDao.insertar(gasto(descripcion = "Mes pasado", monto = 99_000.0, fecha = mes.minusDays(1)))

        val total = gastoDao.observarTotalEntre(negocioId, mes, finDeMes).first()

        assertEquals(30_000.0, total, 0.001)
    }

    @Test
    fun losGastosSalenDelMasRecienteAlMasAntiguo() = runTest {
        gastoDao.insertar(gasto(descripcion = "Primero", monto = 10_000.0, dia = 5))
        gastoDao.insertar(gasto(descripcion = "Ultimo", monto = 20_000.0, dia = 20))
        gastoDao.insertar(gasto(descripcion = "Medio", monto = 15_000.0, dia = 12))

        val gastos = gastoDao.observarEntre(negocioId, mes, finDeMes).first()

        assertEquals(listOf("Ultimo", "Medio", "Primero"), gastos.map { it.descripcion })
    }

    @Test
    fun dosGastosDelMismoDiaNoSalenEnCualquierOrden() = runTest {
        val primero = gastoDao.insertar(gasto(descripcion = "Uno", monto = 10_000.0, dia = 10))
        val segundo = gastoDao.insertar(gasto(descripcion = "Otro", monto = 20_000.0, dia = 10))

        val gastos = gastoDao.observarEntre(negocioId, mes, finDeMes).first()

        assertEquals(listOf(segundo, primero), gastos.map { it.gastoId })
    }

    @Test
    fun unMesSinGastosTotalizaCeroYNoNulo() = runTest {
        assertEquals(0.0, gastoDao.observarTotalEntre(negocioId, mes, finDeMes).first(), 0.001)
    }

    @Test
    fun elTotalIgnoraLosGastosDeOtroNegocio() = runTest {
        val otroNegocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Bella Piel",
                tipoActividad = TipoActividad.SERVICIOS,
                categoriaNegocio = "Belleza",
                porcentajeReinversion = 0.0,
                fechaCreacion = LocalDateTime.of(2026, 9, 1, 8, 0),
            ),
        )
        gastoDao.insertar(gasto(descripcion = "Mío", monto = 10_000.0, dia = 10))
        gastoDao.insertar(
            gasto(descripcion = "Ajeno", monto = 99_000.0, dia = 10, negocio = otroNegocioId, categoria = null),
        )

        assertEquals(10_000.0, gastoDao.observarTotalEntre(negocioId, mes, finDeMes).first(), 0.001)
    }

    @Test
    fun elTotalPorCategoriaSumaCadaUnaYDejaAparteLoSinClasificar() = runTest {
        val papeleria = categoriaDao.insertar(
            CategoriaEntity(
                negocioId = negocioId,
                nombreCategoria = "Papelería",
                tipoCategoria = TipoCategoria.GASTO,
            ),
        )
        gastoDao.insertar(gasto(descripcion = "Domicilio", monto = 12_000.0, dia = 5))
        gastoDao.insertar(gasto(descripcion = "Taxi", monto = 8_000.0, dia = 6))
        gastoDao.insertar(gasto(descripcion = "Facturas", monto = 30_000.0, dia = 7, categoria = papeleria))
        gastoDao.insertar(gasto(descripcion = "Varios", monto = 5_000.0, dia = 8, categoria = null))

        val totales = gastoDao.observarTotalPorCategoriaEntre(negocioId, mes, finDeMes).first()

        // De mayor a menor: papelería 30.000, transporte 20.000, sin clasificar 5.000.
        assertEquals(listOf(papeleria, categoriaId, null), totales.map { it.categoriaId })
        assertEquals(listOf(30_000.0, 20_000.0, 5_000.0), totales.map { it.total })
    }

    @Test
    fun borrarLaCategoriaNoSeLlevaElGasto() = runTest {
        val id = gastoDao.insertar(gasto(descripcion = "Domicilio", monto = 12_000.0, dia = 10))

        categoriaDao.eliminar(categoriaDao.obtener(categoriaId)!!)

        // El gasto sigue ahí, sin etiqueta, y sigue contando en el total del mes.
        assertNull(gastoDao.obtener(id)?.categoriaId)
        assertEquals(12_000.0, gastoDao.observarTotalEntre(negocioId, mes, finDeMes).first(), 0.001)
    }

    private fun gasto(
        descripcion: String,
        monto: Double,
        dia: Int = 15,
        fecha: LocalDate = mes.withDayOfMonth(dia),
        negocio: Long = negocioId,
        categoria: Long? = categoriaId,
    ) = GastoEntity(
        negocioId = negocio,
        categoriaId = categoria,
        descripcion = descripcion,
        monto = monto,
        fecha = fecha,
    )
}
