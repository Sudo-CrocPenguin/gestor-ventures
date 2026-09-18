package com.gestor_ventures.front.ui.inicio

import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.ObligacionRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.back.usecase.CalcularAhorroMensual
import com.gestor_ventures.back.usecase.CalcularResumenFinanciero
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CostoDaoFalso
import com.gestor_ventures.db.dao.GastoDaoFalso
import com.gestor_ventures.db.dao.GastoFijoDaoFalso
import com.gestor_ventures.db.dao.MetaAhorroDaoFalso
import com.gestor_ventures.db.dao.NegocioDaoFalso
import com.gestor_ventures.db.dao.ObligacionDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.enums.TipoActividad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-16 y HU-08: el resumen del día y el progreso de la meta en el inicio.
 *
 * El onboarding le promete al usuario que verá su progreso acá ("Verás tu progreso en el
 * inicio"), así que esa tarjeta no puede depender de que entre a Finanzas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy: LocalDate = ahora.toLocalDate()
    private val reloj = Reloj { ahora }

    private val ventaDao = VentaDaoFalso()
    private val metaAhorroDao = MetaAhorroDaoFalso()
    private val negocioDao = NegocioDaoFalso()

    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val negocioRepository = NegocioRepository(negocioDao, reloj)
    private val baseFinancieraRepository =
        BaseFinancieraRepository(GastoFijoDaoFalso(), metaAhorroDao, negocioDao, reloj)
    private val negocioActivo = NegocioActivoRepository(negocioRepository, sesionRepositoryDePrueba())

    private val calcularResumen = CalcularResumenFinanciero(
        ventaRepository = ventaRepository,
        gastoRepository = GastoRepository(GastoDaoFalso(), reloj),
        costoRepository = CostoRepository(CostoDaoFalso(), reloj),
        baseFinancieraRepository = baseFinancieraRepository,
        obligacionRepository = ObligacionRepository(ObligacionDaoFalso(), reloj),
        negocioRepository = negocioRepository,
        calcularAhorroMensual = CalcularAhorroMensual(reloj),
        reloj = reloj,
    )

    private lateinit var viewModel: InicioViewModel

    private val estado get() = viewModel.uiState.value

    @Before
    fun prepararHilo() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private suspend fun crearNegocio(): Long = negocioDao.insertar(
        NegocioEntity(
            usuarioId = SemillaTemporal.USUARIO_ID,
            nombreNegocio = "Dulce Antojo",
            tipoActividad = TipoActividad.PRODUCTOS,
            categoriaNegocio = "Repostería",
            porcentajeReinversion = 0.0,
            fechaCreacion = ahora,
        ),
    )

    private suspend fun vender(negocioId: Long, monto: Double) = ventaRepository.registrarVenta(
        negocioId = negocioId,
        tipoRegistro = TipoRegistroVenta.RAPIDO,
        monto = monto,
        fechaHora = ahora,
        productoServicio = null,
        metodoPago = MetodoPago.EFECTIVO,
    )

    /**
     * El estado del inicio solo se calcula mientras alguien lo está mirando, así que la prueba
     * tiene que mirarlo igual que la pantalla.
     */
    private fun TestScope.abrirPantalla() {
        viewModel = InicioViewModel(negocioActivo, ventaRepository, calcularResumen, reloj)
        backgroundScope.launch { viewModel.uiState.collect { } }
    }

    @Test
    fun sinMetaLaTarjetaDeAhorroNoAparece() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        vender(negocioId, 300_000.0)
        abrirPantalla()
        advanceUntilIdle()

        assertNull(estado.resumenHoy.progresoMetaAhorro)
        assertFalse(estado.metaCumplida)
    }

    @Test
    fun elInicioMuestraElProgresoDeLaMeta() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        baseFinancieraRepository.definirMetaAhorro(negocioId, 1_000_000.0, hoy.plusMonths(4))
        vender(negocioId, 600_000.0)
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(0.6f, estado.resumenHoy.progresoMetaAhorro ?: 0f, 0.001f)
        assertFalse(estado.metaCumplida)
    }

    @Test
    fun alLlegarAlObjetivoElInicioLoAvisa() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        baseFinancieraRepository.definirMetaAhorro(negocioId, 500_000.0, hoy.plusMonths(4))
        vender(negocioId, 500_000.0)
        abrirPantalla()
        advanceUntilIdle()

        // HU-08: el aviso es lo más cerca de una notificación mientras no exista la Épica 8.
        assertTrue(estado.metaCumplida)
        assertEquals(1f, estado.resumenHoy.progresoMetaAhorro ?: 0f, 0.001f)
    }

    @Test
    fun elAvisoDesapareceCuandoSeDefineLaMetaSiguiente() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        baseFinancieraRepository.definirMetaAhorro(negocioId, 500_000.0, hoy.plusMonths(4))
        vender(negocioId, 500_000.0)
        abrirPantalla()
        advanceUntilIdle()
        assertTrue(estado.metaCumplida)

        baseFinancieraRepository.definirMetaAhorro(negocioId, 2_000_000.0, hoy.plusMonths(8))
        advanceUntilIdle()

        // No hace falta recordar si ya se mostró: se resuelve solo al hacer lo que pide.
        assertFalse(estado.metaCumplida)
    }

    @Test
    fun elResumenDelDiaTraeLasVentasDeHoy() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        vender(negocioId, 300_000.0)
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(300_000.0, estado.resumenHoy.ventas, 0.001)
    }
}
