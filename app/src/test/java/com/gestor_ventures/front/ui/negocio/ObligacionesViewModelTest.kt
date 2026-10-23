package com.gestor_ventures.front.ui.negocio

import com.gestor_ventures.back.model.ErrorObligacion
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.ObligacionRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.NegocioDaoFalso
import com.gestor_ventures.db.dao.ObligacionDaoFalso
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
import java.time.LocalDate
import java.time.LocalDateTime

/** HU-07: las obligaciones del negocio activo. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObligacionesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy: LocalDate = ahora.toLocalDate()
    private val reloj = Reloj { ahora }

    private val obligacionDao = ObligacionDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val repository = ObligacionRepository(obligacionDao, reloj)
    private val negocioActivo = NegocioActivoRepository(
        NegocioRepository(negocioDao, reloj),
        sesionRepositoryDePrueba(),
    )

    private lateinit var viewModel: ObligacionesViewModel

    private val estado get() = viewModel.uiState.value

    @Before
    fun prepararPantalla() = runTest {
        Dispatchers.setMain(dispatcher)
        negocioDao.insertar(
            NegocioEntity(
                usuarioId = SemillaTemporal.USUARIO_ID,
                nombreNegocio = "Dulce Antojo",
                tipoActividad = TipoActividad.PRODUCTOS,
                categoriaNegocio = "Repostería",
                porcentajeReinversion = 0.0,
                fechaCreacion = ahora,
            ),
        )
        viewModel = ObligacionesViewModel(repository, negocioActivo)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private suspend fun registrar(nombre: String, monto: Double, vencimiento: LocalDate) =
        repository.registrarObligacion(1L, nombre, monto, vencimiento)

    @Test
    fun sinObligacionesLoDiceEnVezDeMostrarUnaListaEnBlanco() = runTest(dispatcher) {
        advanceUntilIdle()

        assertTrue(estado.vacio)
        assertEquals(0.0, estado.totalPendiente, 0.001)
    }

    @Test
    fun registrarUnaObligacionLaDejaEnLaListaYSumaAlPendiente() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Cuota del horno")
        viewModel.onMontoChange("250000")
        viewModel.onFechaChange(hoy.plusDays(10))
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals("Cuota del horno", estado.obligaciones.single().nombre)
        assertEquals(250_000.0, estado.totalPendiente, 0.001)
        assertNull(estado.formulario)
    }

    @Test
    fun sinFechaNoSePuedeGuardar() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Cuota")
        viewModel.onMontoChange("250000")

        // Una obligación sin vencimiento no se puede vigilar.
        assertFalse(estado.formulario?.puedeGuardar ?: true)

        viewModel.guardarFormulario()
        advanceUntilIdle()
        assertTrue(estado.obligaciones.isEmpty())
    }

    @Test
    fun lasProximasSalenAparteDeLaListaCompleta() = runTest(dispatcher) {
        registrar("Vencida", 30_000.0, hoy.minusDays(3))
        registrar("Esta semana", 20_000.0, hoy.plusDays(4))
        registrar("El otro mes", 50_000.0, hoy.plusMonths(1))
        advanceUntilIdle()

        assertEquals(listOf("Vencida", "Esta semana"), estado.proximas.map { it.nombre })
        assertEquals(3, estado.obligaciones.size)
    }

    @Test
    fun siTodoVenceProntoNoSeRepiteLaListaCompleta() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        advanceUntilIdle()

        // La única obligación ya está arriba: mostrarla otra vez sería ruido.
        assertEquals(1, estado.proximas.size)
        assertFalse(estado.mostrarTodas)
    }

    @Test
    fun laListaCompletaApareceCuandoHayAlgoQueNoVencePronto() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        registrar("El otro mes", 50_000.0, hoy.plusMonths(1))
        advanceUntilIdle()

        assertTrue(estado.mostrarTodas)
    }

    @Test
    fun marcarComoPagadaLaSacaDelPendienteYDeLasProximas() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.obligaciones.single())
        viewModel.alternarPagadaDeLaElegida()
        advanceUntilIdle()

        assertTrue(estado.obligaciones.single().pagada)
        assertEquals(0.0, estado.totalPendiente, 0.001)
        assertTrue(estado.proximas.isEmpty())
        // No se borra: sigue en la lista como historia del negocio.
        assertEquals(1, estado.obligaciones.size)
    }

    @Test
    fun marcarComoPagadaSePuedeDeshacer() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.obligaciones.single())
        viewModel.alternarPagadaDeLaElegida()
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.obligaciones.single())
        viewModel.alternarPagadaDeLaElegida()
        advanceUntilIdle()

        assertFalse(estado.obligaciones.single().pagada)
        assertEquals(250_000.0, estado.totalPendiente, 0.001)
    }

    @Test
    fun tocarUnaObligacionAbreSusOpcionesYNoLaEditaDeUna() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.obligaciones.single())

        assertNotNull(estado.acciones)
        assertNull(estado.formulario)
    }

    @Test
    fun elFormularioDeEdicionLlegaConTodo() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.obligaciones.single())
        viewModel.editarLaElegida()

        val formulario = estado.formulario
        assertNull(estado.acciones)
        assertEquals("Cuota", formulario?.nombre)
        assertEquals("250000", formulario?.monto)
        assertEquals(hoy.plusDays(3), formulario?.fechaVencimiento)
    }

    @Test
    fun editarCorrigeLaObligacionEnVezDeCrearOtra() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.obligaciones.single())
        viewModel.editarLaElegida()
        viewModel.onMontoChange("260000")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        assertEquals(1, estado.obligaciones.size)
        assertEquals(260_000.0, estado.totalPendiente, 0.001)
    }

    @Test
    fun unMontoInvalidoDejaLaHojaAbiertaConElAviso() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.obligaciones.single())
        viewModel.editarLaElegida()
        viewModel.onNombreChange("   ")
        viewModel.guardarFormulario()
        advanceUntilIdle()

        // El nombre en blanco lo bloquea la pantalla; el repositorio es la red de atrás.
        assertNotNull(estado.formulario)
        assertEquals(250_000.0, estado.obligaciones.single().monto, 0.001)
    }

    @Test
    fun eliminarLaSacaDeLaLista() = runTest(dispatcher) {
        registrar("Cuota", 250_000.0, hoy.plusDays(3))
        advanceUntilIdle()

        viewModel.abrirAcciones(estado.obligaciones.single())
        viewModel.eliminarLaElegida()
        advanceUntilIdle()

        assertTrue(estado.obligaciones.isEmpty())
        assertEquals(0.0, estado.totalPendiente, 0.001)
    }

    @Test
    fun cerrarElFormularioNoDejaRastro() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.abrirFormularioNuevo()
        viewModel.onNombreChange("Cuota")
        viewModel.cerrarFormulario()

        assertNull(estado.formulario)
        viewModel.abrirFormularioNuevo()
        assertEquals("", estado.formulario?.nombre)
    }

    @Test
    fun elErrorDelRepositorioSeMuestraYNoBorraLoEscrito() = runTest(dispatcher) {
        advanceUntilIdle()

        // Monto en cero: la pantalla no deja guardar, pero si llegara, el repositorio avisa.
        assertEquals(
            ErrorObligacion.MontoNoPositivo,
            repository.registrarObligacion(1L, "Cuota", 0.0, hoy),
        )
    }
}
