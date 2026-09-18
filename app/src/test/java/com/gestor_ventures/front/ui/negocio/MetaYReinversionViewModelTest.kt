package com.gestor_ventures.front.ui.negocio

import com.gestor_ventures.back.model.Reloj
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

/** HU-08 y HU-09: definir y corregir la meta de ahorro y la reinversión fuera del onboarding. */
@OptIn(ExperimentalCoroutinesApi::class)
class MetaYReinversionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy: LocalDate = ahora.toLocalDate()
    private val reloj = Reloj { ahora }

    private val gastoFijoDao = GastoFijoDaoFalso()
    private val metaAhorroDao = MetaAhorroDaoFalso()
    private val negocioDao = NegocioDaoFalso()

    private val negocioRepository = NegocioRepository(negocioDao, reloj)
    private val repository =
        BaseFinancieraRepository(gastoFijoDao, metaAhorroDao, negocioDao, reloj)
    private val negocioActivo = NegocioActivoRepository(negocioRepository, sesionRepositoryDePrueba())

    private val calcularResumen = CalcularResumenFinanciero(
        ventaRepository = VentaRepository(VentaDaoFalso(), reloj),
        gastoRepository = GastoRepository(GastoDaoFalso(), reloj),
        costoRepository = CostoRepository(CostoDaoFalso(), reloj),
        baseFinancieraRepository = repository,
        obligacionRepository = ObligacionRepository(ObligacionDaoFalso(), reloj),
        negocioRepository = negocioRepository,
        calcularAhorroMensual = CalcularAhorroMensual(reloj),
        reloj = reloj,
    )

    private lateinit var viewModel: MetaYReinversionViewModel

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

    private fun abrirPantalla() {
        viewModel = MetaYReinversionViewModel(
            repository = repository,
            negocioActivoRepository = negocioActivo,
            calcularAhorroMensual = CalcularAhorroMensual(reloj),
            calcularResumenFinanciero = calcularResumen,
        )
    }

    @Test
    fun laPantallaAbreConLoQueElNegocioYaTeniaGuardado() = runTest(dispatcher) {
        val negocioId = crearNegocio(porcentajeReinversion = 20.0)
        repository.definirMetaAhorro(negocioId, 2_000_000.0, hoy.plusMonths(4))

        abrirPantalla()
        advanceUntilIdle()

        assertEquals("2000000", estado.metaMonto)
        assertEquals(hoy.plusMonths(4), estado.fechaLimite)
        assertEquals(20, estado.porcentajeReinversion)
        assertFalse(estado.cargando)
    }

    @Test
    fun unNegocioSinMetaAbreConLosCamposVacios() = runTest(dispatcher) {
        crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        assertEquals("", estado.metaMonto)
        assertNull(estado.fechaLimite)
        assertEquals(0, estado.porcentajeReinversion)
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun unaMetaAMediasNoSePuedeGuardar() = runTest(dispatcher) {
        crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onMetaMontoChange("2000000")

        // Un monto sin fecha no deja calcular cuánto apartar al mes.
        assertTrue(estado.metaAMedias)
        assertFalse(estado.puedeGuardar)

        viewModel.onFechaLimiteChange(hoy.plusMonths(4))
        assertFalse(estado.metaAMedias)
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun laPantallaDiceCuantoTocaApartarCadaMes() = runTest(dispatcher) {
        crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onMetaMontoChange("2000000")
        viewModel.onFechaLimiteChange(hoy.plusMonths(4))

        // Faltan unos cuatro meses, así que la cuota ronda la cuarta parte de la meta.
        assertEquals(500_000.0, estado.ahorroMensual ?: 0.0, 10_000.0)
    }

    @Test
    fun guardarDejaLaMetaYElPorcentajeEnElNegocio() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onMetaMontoChange("2000000")
        viewModel.onFechaLimiteChange(hoy.plusMonths(4))
        viewModel.onPorcentajeReinversionChange(25)
        viewModel.guardar()
        advanceUntilIdle()

        val meta = repository.metaActiva(negocioId).first()
        assertEquals(2_000_000.0, meta?.montoObjetivo ?: 0.0, 0.001)
        assertEquals(hoy.plusMonths(4), meta?.fechaLimite)
        assertEquals(
            25.0,
            negocioRepository.observarNegocio(negocioId).first()?.porcentajeReinversion ?: 0.0,
            0.001,
        )
    }

    @Test
    fun corregirLaMetaNoDejaUnaSegundaEnElNegocio() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        repository.definirMetaAhorro(negocioId, 2_000_000.0, hoy.plusMonths(4))
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onMetaMontoChange("3000000")
        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(1, metaAhorroDao.metas.value.size)
        assertEquals(
            3_000_000.0,
            repository.metaActiva(negocioId).first()?.montoObjetivo ?: 0.0,
            0.001,
        )
    }

    @Test
    fun sinMetaSoloSeGuardaElPorcentaje() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        viewModel.onPorcentajeReinversionChange(30)
        viewModel.guardar()
        advanceUntilIdle()

        // Dejar la meta vacía significa "todavía no tengo meta", no "créame una en cero".
        assertNull(repository.metaActiva(negocioId).first())
        assertEquals(
            30.0,
            negocioRepository.observarNegocio(negocioId).first()?.porcentajeReinversion ?: 0.0,
            0.001,
        )
    }

    @Test
    fun laPantallaMuestraComoVaLaMetaQueYaExiste() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        repository.definirMetaAhorro(negocioId, 1_000_000.0, hoy.plusMonths(4))

        abrirPantalla()
        advanceUntilIdle()

        // Sin ventas el progreso es cero, pero la barra existe: hay meta que medir.
        assertEquals(0f, estado.progreso?.fraccion ?: -1f, 0.001f)
        assertEquals(1_000_000.0, estado.progreso?.montoObjetivo ?: 0.0, 0.001)
    }
}
