package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.GastoFijoDao
import com.gestor_ventures.db.dao.MetaAhorroDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.entity.GastoFijoEntity
import com.gestor_ventures.db.entity.MetaAhorroEntity
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.enums.EstadoMeta
import com.gestor_ventures.db.enums.Frecuencia
import com.gestor_ventures.db.enums.TipoActividad
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
 * HU-06 y HU-08. Gastos fijos y meta de ahorro contra una base de datos real en memoria.
 */
class BaseFinancieraDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var gastoFijoDao: GastoFijoDao
    private lateinit var metaAhorroDao: MetaAhorroDao

    private val ahora: LocalDateTime = LocalDateTime.of(2026, 9, 16, 10, 0)
    private var negocioId: Long = 0

    @Before
    fun crearBaseDeDatos() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        gastoFijoDao = db.gastoFijoDao()
        metaAhorroDao = db.metaAhorroDao()

        negocioId = negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = ahora,
            ),
        )
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    private fun gastoFijo(nombre: String, monto: Double) = GastoFijoEntity(
        negocioId = negocioId,
        nombreGasto = nombre,
        monto = monto,
        frecuencia = Frecuencia.MENSUAL,
        fechaRegistro = ahora,
    )

    private fun meta(monto: Double, limite: LocalDate) = MetaAhorroEntity(
        negocioId = negocioId,
        montoObjetivo = monto,
        fechaLimite = limite,
        fechaCreacion = ahora,
    )

    @Test
    fun gastosFijos_seGuardanYSeListanDelNegocio() = runTest {
        gastoFijoDao.insertar(gastoFijo("Arriendo local", 300_000.0))
        gastoFijoDao.insertar(gastoFijo("Servicios", 120_000.0))

        val gastos = gastoFijoDao.observarDeNegocio(negocioId).first()

        assertEquals(2, gastos.size)
        assertEquals(setOf("Arriendo local", "Servicios"), gastos.map { it.nombreGasto }.toSet())
        assertEquals(Frecuencia.MENSUAL, gastos.first().frecuencia)
    }

    @Test
    fun gastosFijos_sumaElTotalDelNegocio() = runTest {
        assertEquals(0.0, gastoFijoDao.observarTotalDeNegocio(negocioId).first(), 0.001)

        gastoFijoDao.insertar(gastoFijo("Arriendo local", 300_000.0))
        gastoFijoDao.insertar(gastoFijo("Servicios", 120_000.0))

        assertEquals(420_000.0, gastoFijoDao.observarTotalDeNegocio(negocioId).first(), 0.001)
    }

    @Test
    fun gastosFijos_sePuedenEliminar() = runTest {
        val id = gastoFijoDao.insertar(gastoFijo("Arriendo local", 300_000.0))
        val guardado = gastoFijoDao.obtener(id)!!

        gastoFijoDao.eliminar(guardado)

        assertEquals(emptyList<GastoFijoEntity>(), gastoFijoDao.observarDeNegocio(negocioId).first())
    }

    @Test
    fun metaDeAhorro_devuelveLaActivaMasReciente() = runTest {
        metaAhorroDao.insertar(meta(1_000_000.0, LocalDate.of(2026, 6, 30)))
        metaAhorroDao.insertar(meta(2_000_000.0, LocalDate.of(2026, 12, 31)))

        val activa = metaAhorroDao.observarActivaDeNegocio(negocioId).first()

        assertEquals(2_000_000.0, activa?.montoObjetivo ?: 0.0, 0.001)
        assertEquals(LocalDate.of(2026, 12, 31), activa?.fechaLimite)
    }

    @Test
    fun metaDeAhorro_lasCumplidasNoCuentanComoActivas() = runTest {
        val id = metaAhorroDao.insertar(meta(2_000_000.0, LocalDate.of(2026, 12, 31)))
        val guardada = metaAhorroDao.observarDeNegocio(negocioId).first().first { it.metaAhorroId == id }

        metaAhorroDao.actualizar(guardada.copy(estadoMeta = EstadoMeta.CUMPLIDA))

        assertNull(metaAhorroDao.obtenerActivaDeNegocio(negocioId))
    }

    @Test
    fun borrarElNegocio_borraSuBaseFinanciera() = runTest {
        gastoFijoDao.insertar(gastoFijo("Arriendo local", 300_000.0))
        metaAhorroDao.insertar(meta(2_000_000.0, LocalDate.of(2026, 12, 31)))

        db.openHelper.writableDatabase.execSQL(
            "DELETE FROM negocios WHERE negocio_id = ?",
            arrayOf<Any>(negocioId),
        )

        assertEquals(emptyList<GastoFijoEntity>(), gastoFijoDao.observarDeNegocio(negocioId).first())
        assertNull(metaAhorroDao.obtenerActivaDeNegocio(negocioId))
    }
}
