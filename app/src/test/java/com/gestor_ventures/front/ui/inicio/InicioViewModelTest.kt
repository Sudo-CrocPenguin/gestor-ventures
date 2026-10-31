package com.gestor_ventures.front.ui.inicio

import com.gestor_ventures.back.model.AlertaNueva
import com.gestor_ventures.back.model.Cita
import com.gestor_ventures.back.model.EstadoCita
import com.gestor_ventures.back.model.EstadoPedido
import com.gestor_ventures.back.model.Pedido
import com.gestor_ventures.back.model.ProductoPedido
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.TipoAlerta
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.AgendaDelDiaFalsa
import com.gestor_ventures.back.repository.AlertaRepository
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.ObligacionRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.repository.sesionRepositoryDePrueba
import com.gestor_ventures.back.usecase.CalcularAhorroMensual
import com.gestor_ventures.back.usecase.CalcularResumenFinanciero
import com.gestor_ventures.back.usecase.CalcularSaldoEsperado
import com.gestor_ventures.back.usecase.CompararGastosPorCategoria
import com.gestor_ventures.back.usecase.GenerarAlertas
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.AlertaDaoFalso
import com.gestor_ventures.db.dao.CajaDaoFalso
import com.gestor_ventures.db.dao.CategoriaDaoFalso
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
import java.time.LocalTime
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
    private val cajaDao = CajaDaoFalso()

    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val negocioRepository = NegocioRepository(negocioDao, reloj)
    private val baseFinancieraRepository =
        BaseFinancieraRepository(GastoFijoDaoFalso(), metaAhorroDao, negocioDao, reloj)
    private val sesionRepository = sesionRepositoryDePrueba()
    private val negocioActivo = NegocioActivoRepository(negocioRepository, sesionRepository)

    private val cajaRepository = CajaRepository(cajaDao, reloj)

    private val gastoDao = GastoDaoFalso()
    private val gastoRepository = GastoRepository(gastoDao, reloj)

    private val calcularSaldoEsperado = CalcularSaldoEsperado(
        cajaRepository = cajaRepository,
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = CostoRepository(CostoDaoFalso(), reloj),
    )

    private val calcularResumen = CalcularResumenFinanciero(
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = CostoRepository(CostoDaoFalso(), reloj),
        baseFinancieraRepository = baseFinancieraRepository,
        obligacionRepository = ObligacionRepository(ObligacionDaoFalso(), reloj),
        negocioRepository = negocioRepository,
        calcularAhorroMensual = CalcularAhorroMensual(reloj),
        reloj = reloj,
    )

    private val alertaDao = AlertaDaoFalso()
    private val alertaRepository = AlertaRepository(alertaDao, reloj)

    private val agendaRepository = AgendaDelDiaFalsa()

    private val generarAlertas = GenerarAlertas(
        alertaRepository = alertaRepository,
        ventaRepository = ventaRepository,
        baseFinancieraRepository = baseFinancieraRepository,
        calcularResumenFinanciero = calcularResumen,
        compararGastosPorCategoria = CompararGastosPorCategoria(
            gastoRepository = gastoRepository,
            costoRepository = CostoRepository(CostoDaoFalso(), reloj),
            categoriaRepository = CategoriaRepository(CategoriaDaoFalso()),
        ),
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
        viewModel = InicioViewModel(
            negocioActivoRepository = negocioActivo,
            sesionRepository = sesionRepository,
            ventaRepository = ventaRepository,
            gastoRepository = gastoRepository,
            agendaRepository = agendaRepository,
            alertaRepository = alertaRepository,
            calcularResumenFinanciero = calcularResumen,
            calcularSaldoEsperado = calcularSaldoEsperado,
            generarAlertas = generarAlertas,
            reloj = reloj,
        )
        backgroundScope.launch { viewModel.uiState.collect { } }
    }

    @Test
    fun elSaludoUsaElNombreDeLaCuentaYNoUnoDeEjemplo() = runTest(dispatcher) {
        crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        // "Usuario de prueba" es quien tiene la sesión abierta en sesionRepositoryDePrueba().
        assertEquals("Usuario", estado.nombreUsuario)
    }

    @Test
    fun sinNegocioTodaviaHayAQuienSaludar() = runTest(dispatcher) {
        abrirPantalla()
        advanceUntilIdle()

        // La cuenta existe antes que el negocio: el saludo no tiene por qué esperar.
        assertEquals("Usuario", estado.nombreUsuario)
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

    @Test
    fun elResumenDelDiaTraeLosGastosDeHoy() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        gastoRepository.registrarGasto(negocioId, "Domicilio", 15_000.0)
        gastoRepository.registrarGasto(negocioId, "Papelería", 28_000.0)
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(43_000.0, estado.resumenHoy.gastos, 0.001)
        assertEquals(2, estado.resumenHoy.cantidadGastos)
    }

    // ---------- Pendiente para hoy: lo que falta por hacer en la agenda ----------

    private fun cita(id: Long, hora: LocalTime, estado: EstadoCita, cliente: String = "Camila") = Cita(
        id = id,
        clienteId = 1,
        nombreCliente = cliente,
        fecha = hoy,
        hora = hora,
        descripcion = "Manicure",
        estado = estado,
    )

    private fun pedido(id: Long, estado: EstadoPedido) = Pedido(
        id = id,
        clienteId = 1,
        nombreCliente = "Sandra",
        fechaEntrega = hoy,
        productos = listOf(ProductoPedido("Torta", 2)),
        estado = estado,
    )

    @Test
    fun lasCitasPendientesDeHoyVanEnOrdenDeHora() = runTest(dispatcher) {
        crearNegocio()
        agendaRepository.citas.value = listOf(
            cita(1, LocalTime.of(16, 0), EstadoCita.PROGRAMADA, "Tarde"),
            cita(2, LocalTime.of(9, 30), EstadoCita.PROGRAMADA, "Mañana"),
        )
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(listOf("Mañana", "Tarde"), estado.pendientesHoy.map { it.cliente })
    }

    @Test
    fun loQueYaSeAtendioOSeCanceloNoEsUnPendiente() = runTest(dispatcher) {
        crearNegocio()
        agendaRepository.citas.value = listOf(
            cita(1, LocalTime.of(9, 0), EstadoCita.COMPLETADA),
            cita(2, LocalTime.of(10, 0), EstadoCita.CANCELADA),
            cita(3, LocalTime.of(11, 0), EstadoCita.PROGRAMADA),
        )
        agendaRepository.pedidos.value = listOf(
            pedido(1, EstadoPedido.ENTREGADO),
            pedido(2, EstadoPedido.CANCELADO),
            pedido(3, EstadoPedido.EN_PROCESO),
        )
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(2, estado.pendientesHoy.size)
        assertTrue(estado.pendientesHoy[0].esCita)
        assertFalse(estado.pendientesHoy[1].esCita)
    }

    @Test
    fun sinCitasNiPedidosNoHayPendientes() = runTest(dispatcher) {
        crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        assertTrue(estado.pendientesHoy.isEmpty())
    }

    // ---------- HU-38: las alertas del negocio ----------

    @Test
    fun elSaludoNoAnunciaAlertasQueNoExisten() = runTest(dispatcher) {
        crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        // Antes decía "2 alertas nuevas" siempre, fuera verdad o no: era un número fijo.
        assertEquals(0, estado.alertasNuevas)
        assertTrue(estado.alertas.isEmpty())
    }

    @Test
    fun lasAlertasDelNegocioLleganAlInicio() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        alertaRepository.registrar(
            negocioId,
            AlertaNueva(TipoAlerta.CaidaVentas, "Vendiste menos que la semana pasada."),
        )
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(1, estado.alertas.size)
        assertEquals(1, estado.alertasNuevas)
    }

    @Test
    fun unaAlertaLeidaDejaDeContarComoNueva() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        alertaRepository.registrar(
            negocioId,
            AlertaNueva(TipoAlerta.GastoElevado, "Tus gastos van por encima de lo normal."),
        )
        abrirPantalla()
        advanceUntilIdle()

        alertaRepository.marcarLeida(estado.alertas.single().id)
        advanceUntilIdle()

        // Sigue en la lista —pasó de verdad— pero ya no se anuncia como nueva.
        assertEquals(1, estado.alertas.size)
        assertEquals(0, estado.alertasNuevas)
    }

    // ---------- Las últimas ventas del día ----------

    @Test
    fun lasUltimasVentasVanDeLaMasRecienteALaMasVieja() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        vender(negocioId, 10_000.0)
        vender(negocioId, 20_000.0)
        abrirPantalla()
        advanceUntilIdle()

        assertEquals(listOf(20_000.0, 10_000.0), estado.ultimasVentas.map { it.monto })
    }

    @Test
    fun elInicioNoSeLlenaConTodasLasVentasDelDia() = runTest(dispatcher) {
        val negocioId = crearNegocio()
        repeat(8) { vender(negocioId, 1_000.0) }
        abrirPantalla()
        advanceUntilIdle()

        // Un día bueno trae decenas de ventas; el inicio es un resumen, no el historial.
        assertEquals(5, estado.ultimasVentas.size)
    }

    @Test
    fun sinVentasNoHayNadaQueListar() = runTest(dispatcher) {
        crearNegocio()
        abrirPantalla()
        advanceUntilIdle()

        assertTrue(estado.ultimasVentas.isEmpty())
    }
}
