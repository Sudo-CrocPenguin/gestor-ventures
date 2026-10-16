package com.gestor_ventures.front.ui.clientes

import androidx.lifecycle.SavedStateHandle
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.ClienteRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.db.dao.ClienteDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
import com.gestor_ventures.front.navigation.ArgumentoClienteId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-30. El perfil de un cliente y lo que le ha comprado al negocio.
 *
 * Lo que de verdad se fija acá es que cada compra sea de ese cliente y de nadie más: si se
 * colaran las de otro, el total acumulado —que es lo que responde "cuánto vale este cliente"—
 * diría cualquier cosa.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClienteDetalleViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2026, 10, 16, 11, 0)
    private val reloj = Reloj { ahora }

    private val clienteDao = ClienteDaoFalso()
    private val ventaDao = VentaDaoFalso()

    private val clienteRepository = ClienteRepository(clienteDao)
    private val ventaRepository = VentaRepository(ventaDao, reloj)

    private val negocioId = 1L

    private lateinit var viewModel: ClienteDetalleViewModel

    private val estado get() = viewModel.uiState.value

    @Before
    fun prepararHiloPrincipal() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun soltarElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private suspend fun crearCliente(nombre: String, telefono: String? = null): Long {
        clienteRepository.crearCliente(negocioId, nombre, telefono = telefono)
        return clienteRepository.clientesDeNegocio(negocioId).first()
            .first { it.nombre == nombre }
            .id
    }

    private suspend fun venderA(clienteId: Long?, monto: Double, producto: String? = "Torta") =
        ventaRepository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = if (producto == null) {
                TipoRegistroVenta.RAPIDO
            } else {
                TipoRegistroVenta.DETALLADO
            },
            monto = monto,
            fechaHora = ahora,
            productoServicio = producto,
            metodoPago = MetodoPago.EFECTIVO,
            clienteId = clienteId,
        )

    private fun abrirPerfil(clienteId: Long) {
        viewModel = ClienteDetalleViewModel(
            clienteRepository = clienteRepository,
            ventaRepository = ventaRepository,
            savedStateHandle = SavedStateHandle(
                mapOf(ArgumentoClienteId to clienteId.toString()),
            ),
        )
    }

    @Test
    fun elPerfilTraeLosDatosDelCliente() = runTest(dispatcher) {
        val id = crearCliente("Laura Gómez", telefono = "3001112233")
        abrirPerfil(id)
        advanceUntilIdle()

        assertEquals("Laura Gómez", estado.cliente?.nombre)
        assertEquals("3001112233", estado.cliente?.telefono)
    }

    @Test
    fun unClienteSinComprasLoDiceEnVezDeMostrarUnaListaEnBlanco() = runTest(dispatcher) {
        val id = crearCliente("Camila Torres")
        abrirPerfil(id)
        advanceUntilIdle()

        assertTrue(estado.sinCompras)
        assertEquals(0.0, estado.totalAcumulado, 0.001)
    }

    @Test
    fun elHistorialTraeLasComprasDeEseCliente() = runTest(dispatcher) {
        val id = crearCliente("Laura Gómez")
        venderA(id, 85_000.0, "Torta personalizada")
        venderA(id, 7_500.0, "Cupcakes x6")
        abrirPerfil(id)
        advanceUntilIdle()

        assertEquals(2, estado.compras.size)
    }

    @Test
    fun elTotalAcumuladoSumaTodasSusCompras() = runTest(dispatcher) {
        val id = crearCliente("Laura Gómez")
        venderA(id, 85_000.0)
        venderA(id, 7_500.0)
        abrirPerfil(id)
        advanceUntilIdle()

        assertEquals(92_500.0, estado.totalAcumulado, 0.001)
    }

    @Test
    fun lasComprasDeOtroClienteNoSeCuelan() = runTest(dispatcher) {
        val laura = crearCliente("Laura Gómez")
        val andres = crearCliente("Andrés Ruiz")
        venderA(laura, 85_000.0)
        venderA(andres, 999_000.0)
        abrirPerfil(laura)
        advanceUntilIdle()

        // Si se colaran, el total diría que Laura vale un millón y no lo vale.
        assertEquals(85_000.0, estado.totalAcumulado, 0.001)
        assertEquals(1, estado.compras.size)
    }

    @Test
    fun lasVentasSinClienteNoLeSumanANadie() = runTest(dispatcher) {
        val id = crearCliente("Laura Gómez")
        venderA(id, 85_000.0)
        venderA(null, 50_000.0)
        abrirPerfil(id)
        advanceUntilIdle()

        assertEquals(85_000.0, estado.totalAcumulado, 0.001)
    }

    @Test
    fun unaVentaRapidaSeMuestraSinNombreDeProducto() = runTest(dispatcher) {
        val id = crearCliente("Laura Gómez")
        // Una venta rápida es el total del día: el repositorio le descarta el cliente.
        venderA(id, 50_000.0, producto = null)
        abrirPerfil(id)
        advanceUntilIdle()

        assertTrue(estado.sinCompras)
    }

    @Test
    fun registrarleUnaVentaApareceEnElPerfilSinRecargarlo() = runTest(dispatcher) {
        val id = crearCliente("Laura Gómez")
        abrirPerfil(id)
        advanceUntilIdle()
        assertEquals(0.0, estado.totalAcumulado, 0.001)

        venderA(id, 30_000.0)
        advanceUntilIdle()

        assertEquals(30_000.0, estado.totalAcumulado, 0.001)
    }

    @Test
    fun corregirleElNombreSeVeEnElPerfil() = runTest(dispatcher) {
        val id = crearCliente("Laura")
        abrirPerfil(id)
        advanceUntilIdle()

        clienteRepository.editarCliente(clienteId = id, nombre = "Laura Gómez")
        advanceUntilIdle()

        assertEquals("Laura Gómez", estado.cliente?.nombre)
    }

    @Test
    fun elPerfilDeAlguienQueYaNoExisteNoRompeNada() = runTest(dispatcher) {
        abrirPerfil(clienteId = 99L)
        advanceUntilIdle()

        assertNull(estado.cliente)
        assertEquals(0.0, estado.totalAcumulado, 0.001)
    }

    @Test
    fun elEnlaceDeWhatsAppSaleDelTelefonoDelCliente() = runTest(dispatcher) {
        val id = crearCliente("Laura Gómez", telefono = "3001112233")
        abrirPerfil(id)
        advanceUntilIdle()

        // HU-31 pedía el botón "desde su perfil": acá es.
        assertEquals("https://wa.me/573001112233", estado.whatsapp)
    }

    @Test
    fun sinTelefonoElPerfilNoOfreceWhatsApp() = runTest(dispatcher) {
        val id = crearCliente("Camila Torres")
        abrirPerfil(id)
        advanceUntilIdle()

        assertNull(estado.whatsapp)
    }
}
