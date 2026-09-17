package com.gestor_ventures.front.ui.finanzas

import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.ObligacionRepository
import com.gestor_ventures.back.repository.SesionRepository
import com.gestor_ventures.back.repository.VentaRepository
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/** HU-16: la sección de resumen del negocio activo. */
@OptIn(ExperimentalCoroutinesApi::class)
class ResumenFinancieroViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val reloj = Reloj { ahora }

    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()
    private val gastoFijoDao = GastoFijoDaoFalso()
    private val metaAhorroDao = MetaAhorroDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val obligacionDao = ObligacionDaoFalso()

    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val negocioRepository = NegocioRepository(negocioDao, reloj)
    private val baseFinancieraRepository =
        BaseFinancieraRepository(gastoFijoDao, metaAhorroDao, negocioDao, reloj)
    private val negocioActivo = NegocioActivoRepository(negocioRepository, SesionRepository())

    private val calcularResumen = CalcularResumenFinanciero(
        ventaRepository = ventaRepository,
        gastoRepository = GastoRepository(gastoDao, reloj),
        costoRepository = CostoRepository(costoDao, reloj),
        baseFinancieraRepository = baseFinancieraRepository,
        obligacionRepository = ObligacionRepository(obligacionDao, reloj),
        negocioRepository = negocioRepository,
        calcularAhorroMensual = CalcularAhorroMensual(reloj),
        reloj = reloj,
    )

    private lateinit var viewModel: ResumenFinancieroViewModel

    private val estado get() = viewModel.uiState.value

    @Before
    fun prepararHilo() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private suspend fun crearNegocio(porcentajeReinversion: Double = 0.0): Long = negocioDao.insertar(
        NegocioEntity(
            usuarioId = SemillaTemporal.USUARIO_ID,
            nombreNegocio = "Dulce Antojo",
            tipoActividad = TipoActividad.PRODUCTOS,
            categoriaNegocio = "Repostería",
            porcentajeReinversion = porcentajeReinversion,
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

    private fun abrirPantalla() {
        viewModel = ResumenFinancieroViewModel(negocioActivo, calcularResumen)
    }

    @Test
    fun sinNegocioNoHayNadaQueResumir() = runTest(dispatcher) {
        abrirPantalla()
        advanceUntilIdle()

        assertNull(estado.resumen)
        assertTrue(estado.sinNegocio)
    }

    @Test
    fun unNegocioSinMovimientosMuestraElAvisoYNoUnTableroDeCeros() = runTest(dispatcher) {
        crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        assertFalse(estado.sinNegocio)
        assertTrue(estado.resumen?.sinMovimientos ?: false)
    }

    @Test
    fun elResumenTraeElMesDelNegocioActivo() = runTest(dispatcher) {
        val negocioId = crearNegocio(porcentajeReinversion = 20.0)
        vender(negocioId, 1_000_000.0)
        abrirPantalla()
        advanceUntilIdle()

        val resumen = estado.resumen

        assertNotNull(resumen)
        assertEquals(1_000_000.0, resumen?.ingresos ?: 0.0, 0.001)
        assertEquals(200_000.0, resumen?.reinversion ?: 0.0, 0.001)
        assertFalse(resumen?.sinMovimientos ?: true)
    }

    @Test
    fun elResumenSeActualizaSoloCuandoSeRegistraUnaVenta() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        vender(negocioId, 350_000.0)
        advanceUntilIdle()

        // No hace falta volver a entrar a la pantalla: escucha mientras está a la vista.
        assertEquals(350_000.0, estado.resumen?.ingresos ?: 0.0, 0.001)
    }

    @Test
    fun laMetaDelNegocioLlegaConSuProgreso() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        baseFinancieraRepository.definirMetaAhorro(
            negocioId,
            1_000_000.0,
            ahora.toLocalDate().plusMonths(4),
        )
        vender(negocioId, 400_000.0)
        abrirPantalla()
        advanceUntilIdle()

        val progreso = estado.resumen?.progresoMeta

        assertNotNull(progreso)
        assertEquals(0.4f, progreso?.fraccion ?: 0f, 0.001f)
    }
}
