package com.gestor_ventures.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.gestor_ventures.db.dao.CajaDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.entity.CajaEntity
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.enums.EstadoCaja
import com.gestor_ventures.db.enums.TipoActividad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-19, HU-21 y HU-22. Abrir y cerrar caja, contra una base de datos real en memoria.
 *
 * Lo que se prueba acá no es guardar un monto: es que la consulta que responde "¿hay una
 * jornada en curso?" diga la verdad. De esa respuesta depende la única regla de la historia
 * —no abrir una caja nueva si hay otra sin cerrar—, y se equivoca de dos formas distintas:
 * contando una caja ya cerrada, o contando la de otro negocio del mismo dueño.
 */
class CajaDaoTest {

    private lateinit var db: GestorVenturesDatabase
    private lateinit var negocioDao: NegocioDao
    private lateinit var cajaDao: CajaDao

    private val ahora: LocalDateTime = LocalDateTime.of(2026, 10, 7, 8, 0)
    private var negocioId: Long = 0

    @Before
    fun crearBaseDeDatos() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GestorVenturesDatabase::class.java)
            .addCallback(SemillaTemporal.callback)
            .build()
        negocioDao = db.negocioDao()
        cajaDao = db.cajaDao()

        negocioId = negocioDao.insertar(negocio("Dulce Antojo"))
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    @Test
    fun laCajaSeGuardaConSuMontoYLaHoraEnQueSeAbrio() = runTest {
        cajaDao.insertar(caja(montoInicial = 50_000.0, apertura = ahora))

        val abierta = cajaDao.obtenerAbierta(negocioId)

        assertEquals(50_000.0, abierta?.montoInicial ?: 0.0, 0.001)
        assertEquals(ahora, abierta?.fechaHoraApertura)
        assertEquals(EstadoCaja.ABIERTA, abierta?.estadoCaja)
    }

    @Test
    fun unaCajaRecienAbiertaNoTieneDatosDeCierre() = runTest {
        cajaDao.insertar(caja(montoInicial = 50_000.0, apertura = ahora))

        val abierta = cajaDao.obtenerAbierta(negocioId)

        assertNull(abierta?.montoRealCierre)
        assertNull(abierta?.fechaHoraCierre)
        assertNull(abierta?.notaDiferencia)
    }

    @Test
    fun sinNingunaCajaNoHayJornadaEnCurso() = runTest {
        assertNull(cajaDao.obtenerAbierta(negocioId))
    }

    @Test
    fun unaCajaYaCerradaNoCuentaComoJornadaEnCurso() = runTest {
        cajaDao.insertar(
            caja(montoInicial = 50_000.0, apertura = ahora).copy(
                estadoCaja = EstadoCaja.CERRADA,
                fechaHoraCierre = ahora.plusHours(6),
                montoRealCierre = 141_000.0,
            ),
        )

        assertNull(cajaDao.obtenerAbierta(negocioId))
    }

    @Test
    fun laCajaDeOtroNegocioNoEstorbaLaDeEste() = runTest {
        val otroNegocioId = negocioDao.insertar(negocio("Café del Parque"))
        cajaDao.insertar(caja(montoInicial = 30_000.0, apertura = ahora, negocio = otroNegocioId))

        // El dueño es el mismo, pero cada negocio lleva su propia jornada.
        assertNull(cajaDao.obtenerAbierta(negocioId))
        assertEquals(30_000.0, cajaDao.obtenerAbierta(otroNegocioId)?.montoInicial ?: 0.0, 0.001)
    }

    @Test
    fun siAlgunaVezHubieraDosAbiertasGanaLaMasReciente() = runTest {
        cajaDao.insertar(caja(montoInicial = 50_000.0, apertura = ahora))
        val segunda = cajaDao.insertar(caja(montoInicial = 20_000.0, apertura = ahora.plusHours(7)))

        // El repositorio no deja llegar a esto, pero si la tabla quedara así, la consulta tiene
        // que devolver la jornada en la que el usuario está parado, no la vieja.
        assertEquals(segunda, cajaDao.obtenerAbierta(negocioId)?.cajaId)
    }

    @Test
    fun laPantallaSeEnteraSolaDeQueSeAbrioLaCaja() = runTest {
        assertNull(cajaDao.observarAbierta(negocioId).first())

        cajaDao.insertar(caja(montoInicial = 50_000.0, apertura = ahora))

        assertEquals(50_000.0, cajaDao.observarAbierta(negocioId).first()?.montoInicial ?: 0.0, 0.001)
    }

    // ---------- HU-21 y HU-22: cerrar la jornada ----------

    @Test
    fun cerrarLaJornadaLeGuardaElMontoContadoLaHoraYLaNota() = runTest {
        val id = cajaDao.insertar(caja(montoInicial = 50_000.0, apertura = ahora))
        val cierre = ahora.plusHours(6).plusMinutes(30)

        cajaDao.actualizar(
            cajaDao.obtener(id)!!.copy(
                montoRealCierre = 141_000.0,
                fechaHoraCierre = cierre,
                estadoCaja = EstadoCaja.CERRADA,
                notaDiferencia = "Se dio vuelto de más en una venta",
            ),
        )

        val cerrada = cajaDao.obtener(id)
        assertEquals(141_000.0, cerrada?.montoRealCierre ?: 0.0, 0.001)
        assertEquals(cierre, cerrada?.fechaHoraCierre)
        assertEquals(EstadoCaja.CERRADA, cerrada?.estadoCaja)
        assertEquals("Se dio vuelto de más en una venta", cerrada?.notaDiferencia)
    }

    @Test
    fun cerrarLaJornadaNoLePisaConQueArranco() = runTest {
        val id = cajaDao.insertar(caja(montoInicial = 50_000.0, apertura = ahora))

        cajaDao.actualizar(
            cajaDao.obtener(id)!!.copy(
                montoRealCierre = 141_000.0,
                fechaHoraCierre = ahora.plusHours(6),
                estadoCaja = EstadoCaja.CERRADA,
            ),
        )

        // El monto y la hora de apertura son el punto de partida de la cuenta: si el cierre los
        // tocara, la diferencia que se le muestra al usuario saldría de datos cambiados.
        val cerrada = cajaDao.obtener(id)
        assertEquals(50_000.0, cerrada?.montoInicial ?: 0.0, 0.001)
        assertEquals(ahora, cerrada?.fechaHoraApertura)
    }

    @Test
    fun despuesDeCerrarSePuedeAbrirOtraElMismoDia() = runTest {
        val primera = cajaDao.insertar(caja(montoInicial = 50_000.0, apertura = ahora))
        cajaDao.actualizar(
            cajaDao.obtener(primera)!!.copy(
                montoRealCierre = 141_000.0,
                fechaHoraCierre = ahora.plusHours(6),
                estadoCaja = EstadoCaja.CERRADA,
            ),
        )

        val segunda = cajaDao.insertar(caja(montoInicial = 20_000.0, apertura = ahora.plusHours(7)))

        // El mockup dice que puede haber varias aperturas en un mismo día; lo que no puede
        // haber es dos abiertas a la vez.
        assertEquals(segunda, cajaDao.obtenerAbierta(negocioId)?.cajaId)
    }

    @Test
    fun unaJornadaCerradaSinNotaEsValida() = runTest {
        val id = cajaDao.insertar(caja(montoInicial = 50_000.0, apertura = ahora))

        cajaDao.actualizar(
            cajaDao.obtener(id)!!.copy(
                montoRealCierre = 50_000.0,
                fechaHoraCierre = ahora.plusHours(6),
                estadoCaja = EstadoCaja.CERRADA,
            ),
        )

        // Cuando la caja cuadra no hay nada que explicar: la nota admite nulos a propósito.
        assertNull(cajaDao.obtener(id)?.notaDiferencia)
    }

    private fun caja(
        montoInicial: Double,
        apertura: LocalDateTime,
        negocio: Long = negocioId,
    ) = CajaEntity(
        negocioId = negocio,
        montoInicial = montoInicial,
        fechaHoraApertura = apertura,
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
