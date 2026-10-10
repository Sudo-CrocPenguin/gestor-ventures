package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.db.dao.CajaDaoFalso
import com.gestor_ventures.db.dao.CostoDaoFalso
import com.gestor_ventures.db.dao.GastoDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-20. El saldo que la caja debería tener ahora mismo.
 *
 * Lo delicado no es la resta: es qué entra en la jornada y qué no. Un movimiento de antes de
 * abrir, o de otro negocio, inflaría un saldo que al cerrar va a tener que cuadrar contra
 * billetes de verdad.
 */
class CalcularSaldoEsperadoTest {

    // Miércoles 7 de octubre de 2026, 2 de la tarde.
    private var momento = LocalDateTime.of(2026, 10, 7, 14, 0)
    private val reloj = Reloj { momento }

    private val cajaDao = CajaDaoFalso()
    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()

    private val cajaRepository = CajaRepository(cajaDao, reloj)
    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)

    private val calcularSaldoEsperado = CalcularSaldoEsperado(
        cajaRepository = cajaRepository,
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = costoRepository,
    )

    private val negocioId = 1L
    private val otroNegocio = 2L

    private suspend fun saldo() = calcularSaldoEsperado(negocioId).first()

    private suspend fun abrirCajaCon(monto: Double, aLas: LocalDateTime) {
        momento = aLas
        cajaRepository.abrirCaja(negocioId, monto)
    }

    private suspend fun vender(
        monto: Double,
        aLas: LocalDateTime = momento,
        negocio: Long = negocioId,
    ) = ventaRepository.registrarVenta(
        negocioId = negocio,
        tipoRegistro = TipoRegistroVenta.DETALLADO,
        monto = monto,
        fechaHora = aLas,
        productoServicio = "Torta",
        metodoPago = MetodoPago.EFECTIVO,
    )

    private suspend fun gastar(monto: Double, aLas: LocalDateTime = momento) =
        gastoRepository.registrarGasto(negocioId, "Domicilio", monto, aLas)

    private suspend fun costear(monto: Double, aLas: LocalDateTime = momento) =
        costoRepository.registrarCosto(negocioId, "Harina", monto, aLas)

    @Test
    fun sinCajaAbiertaNoHaySaldoQueCalcular() = runTest {
        assertNull(saldo())
    }

    @Test
    fun recienAbiertaLaCajaTieneSoloLoQueSeConto() = runTest {
        abrirCajaCon(50_000.0, aLas = momento.withHour(8))

        val saldo = saldo()

        assertEquals(50_000.0, saldo?.saldoEsperado ?: 0.0, 0.001)
        assertEquals(0.0, saldo?.entradas ?: -1.0, 0.001)
        assertEquals(0.0, saldo?.salidas ?: -1.0, 0.001)
    }

    @Test
    fun lasVentasEntranYLosGastosYCostosSalen() = runTest {
        abrirCajaCon(50_000.0, aLas = momento.withHour(8))
        momento = momento.withHour(11)
        vender(92_500.0)
        gastar(12_000.0)
        costear(8_000.0)

        val saldo = saldo()

        // 50.000 + 92.500 − 12.000 − 8.000
        assertEquals(122_500.0, saldo?.saldoEsperado ?: 0.0, 0.001)
        assertEquals(92_500.0, saldo?.entradas ?: -1.0, 0.001)
        assertEquals(20_000.0, saldo?.salidas ?: -1.0, 0.001)
    }

    @Test
    fun elDesgloseSeparaGastosDeCostos() = runTest {
        abrirCajaCon(0.0, aLas = momento.withHour(8))
        momento = momento.withHour(11)
        gastar(12_000.0)
        costear(8_000.0)

        val saldo = saldo()

        // La pantalla los muestra aparte: un saldo raro no dice nada sin el desglose.
        assertEquals(12_000.0, saldo?.gastos ?: -1.0, 0.001)
        assertEquals(8_000.0, saldo?.costos ?: -1.0, 0.001)
    }

    @Test
    fun loDeAntesDeAbrirLaCajaNoCuenta() = runTest {
        // Una venta de la mañana, antes de que empezara este turno.
        momento = momento.withHour(7)
        vender(99_000.0)

        abrirCajaCon(50_000.0, aLas = momento.withHour(8))
        momento = momento.withHour(11)
        vender(10_000.0)

        // Si contara la venta de las 7, al cerrar faltarían $99.000 que nunca entraron al turno.
        assertEquals(60_000.0, saldo()?.saldoEsperado ?: 0.0, 0.001)
    }

    @Test
    fun loDeOtroNegocioNoCuenta() = runTest {
        abrirCajaCon(50_000.0, aLas = momento.withHour(8))
        momento = momento.withHour(11)
        vender(10_000.0, negocio = otroNegocio)

        assertEquals(50_000.0, saldo()?.saldoEsperado ?: 0.0, 0.001)
    }

    @Test
    fun elSaldoSeMueveSoloAlRegistrarAlgoNuevo() = runTest {
        abrirCajaCon(50_000.0, aLas = momento.withHour(8))
        momento = momento.withHour(11)
        assertEquals(50_000.0, saldo()?.saldoEsperado ?: 0.0, 0.001)

        // HU-20: nadie le avisa a la caja; el saldo se entera solo.
        vender(25_000.0)

        assertEquals(75_000.0, saldo()?.saldoEsperado ?: 0.0, 0.001)
    }

    @Test
    fun unaVentaRegistradaDespuesDeConsultarElSaldoTambienCuenta() = runTest {
        abrirCajaCon(50_000.0, aLas = momento.withHour(8))

        // El rango de la jornada abierta no tiene final: si lo tuviera fijo en el momento de
        // armar la consulta, lo que se registre un minuto después quedaría fuera.
        momento = momento.withHour(18)
        vender(30_000.0)

        assertEquals(80_000.0, saldo()?.saldoEsperado ?: 0.0, 0.001)
    }

    @Test
    fun laCajaPuedeQuedarEnRojoSiSeGastoMasDeLoQueEntro() = runTest {
        abrirCajaCon(10_000.0, aLas = momento.withHour(8))
        momento = momento.withHour(11)
        gastar(25_000.0)

        // No se tapa con un cero: si la cuenta da negativo, hay algo que revisar.
        assertEquals(-15_000.0, saldo()?.saldoEsperado ?: 0.0, 0.001)
    }
}
