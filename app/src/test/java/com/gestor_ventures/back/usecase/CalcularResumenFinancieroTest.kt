package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.MetodoPago
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.back.model.TipoRegistroVenta
import com.gestor_ventures.back.repository.BaseFinancieraRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.ObligacionRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CostoDaoFalso
import com.gestor_ventures.db.dao.GastoDaoFalso
import com.gestor_ventures.db.dao.GastoFijoDaoFalso
import com.gestor_ventures.db.dao.MetaAhorroDaoFalso
import com.gestor_ventures.db.dao.NegocioDaoFalso
import com.gestor_ventures.db.dao.ObligacionDaoFalso
import com.gestor_ventures.db.dao.VentaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HU-16. El resumen del mes: lo que entró, lo que salió y cuánto de la ganancia se puede sacar.
 *
 * Acá se fija lo que HU-06, HU-08 y HU-09 pidieron y no se podía comprobar hasta ahora: que los
 * gastos fijos entren en los cálculos, que la meta muestre progreso y que la app calcule sola
 * cuánto se puede reinvertir.
 */
class CalcularResumenFinancieroTest {

    private val ahora = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val hoy: LocalDate = ahora.toLocalDate()
    private val reloj = Reloj { ahora }

    private val ventaDao = VentaDaoFalso()
    private val gastoDao = GastoDaoFalso()
    private val costoDao = CostoDaoFalso()
    private val gastoFijoDao = GastoFijoDaoFalso()
    private val metaAhorroDao = MetaAhorroDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val obligacionDao = ObligacionDaoFalso()

    private val ventaRepository = VentaRepository(ventaDao, reloj)
    private val gastoRepository = GastoRepository(gastoDao, reloj)
    private val costoRepository = CostoRepository(costoDao, reloj)
    private val negocioRepository = NegocioRepository(negocioDao, reloj)
    private val baseFinancieraRepository =
        BaseFinancieraRepository(gastoFijoDao, metaAhorroDao, negocioDao, reloj)
    private val obligacionRepository = ObligacionRepository(obligacionDao, reloj)

    private val calcularResumen = CalcularResumenFinanciero(
        ventaRepository = ventaRepository,
        gastoRepository = gastoRepository,
        costoRepository = costoRepository,
        baseFinancieraRepository = baseFinancieraRepository,
        obligacionRepository = obligacionRepository,
        negocioRepository = negocioRepository,
        calcularAhorroMensual = CalcularAhorroMensual(reloj),
        reloj = reloj,
    )

    private var negocioId = 0L

    private suspend fun crearNegocio(porcentajeReinversion: Double = 0.0) {
        negocioRepository.crearNegocio(
            usuarioId = SemillaTemporal.USUARIO_ID,
            nombre = "Dulce Antojo",
            tipoActividad = TipoActividad.PRODUCTOS,
            categoria = "Repostería",
            porcentajeReinversion = porcentajeReinversion,
        )
        negocioId = negocioRepository.negociosDeUsuario(SemillaTemporal.USUARIO_ID).first().first().id
    }

    private suspend fun vender(monto: Double, fecha: LocalDateTime = ahora) =
        ventaRepository.registrarVenta(
            negocioId = negocioId,
            tipoRegistro = TipoRegistroVenta.RAPIDO,
            monto = monto,
            fechaHora = fecha,
            productoServicio = null,
            metodoPago = MetodoPago.EFECTIVO,
        )

    private suspend fun gastar(monto: Double, fecha: LocalDate = hoy) =
        gastoRepository.registrarGasto(negocioId, "Transporte", monto, fecha)

    private suspend fun costear(monto: Double, fecha: LocalDateTime = ahora) =
        costoRepository.registrarCosto(negocioId, "Harina", monto, fecha)

    private suspend fun resumen() = calcularResumen(negocioId).first()

    @Test
    fun laGananciaEsLoQueEntroMenosTodoLoQueSalio() = runTest {
        crearNegocio()
        vender(1_000_000.0)
        gastar(150_000.0)
        costear(250_000.0)

        val resumen = resumen()

        assertEquals(1_000_000.0, resumen.ingresos, 0.001)
        assertEquals(400_000.0, resumen.egresos, 0.001)
        assertEquals(600_000.0, resumen.ganancia, 0.001)
    }

    @Test
    fun losGastosFijosEntranEnLosCalculosAunqueNadieLosRegistreCadaMes() = runTest {
        crearNegocio()
        vender(1_000_000.0)
        baseFinancieraRepository.agregarGastoFijo(
            negocioId,
            "Arriendo",
            300_000.0,
            Frecuencia.MENSUAL,
        )

        val resumen = resumen()

        // HU-06: el gasto fijo no es un movimiento registrado y aun así pesa en la ganancia.
        assertEquals(300_000.0, resumen.gastosFijos, 0.001)
        assertEquals(700_000.0, resumen.ganancia, 0.001)
    }

    @Test
    fun cadaGastoFijoSeLlevaASuEquivalenteMensualAntesDeSumarlo() = runTest {
        crearNegocio()
        baseFinancieraRepository.agregarGastoFijo(
            negocioId,
            "Empaques",
            50_000.0,
            Frecuencia.SEMANAL,
        )
        baseFinancieraRepository.agregarGastoFijo(
            negocioId,
            "Seguro",
            1_200_000.0,
            Frecuencia.ANUAL,
        )

        // Sumar 50.000 + 1.200.000 daría un número que no es de ningún periodo.
        val esperado = 50_000.0 * 52 / 12 + 1_200_000.0 / 12
        assertEquals(esperado, resumen().gastosFijos, 0.001)
    }

    @Test
    fun laAppCalculaSolaCuantoSePuedeReinvertir() = runTest {
        crearNegocio(porcentajeReinversion = 20.0)
        vender(1_000_000.0)
        costear(400_000.0)

        // HU-09: el 20 % de una ganancia de 600.000.
        assertEquals(120_000.0, resumen().reinversion, 0.001)
    }

    @Test
    fun unMesEnPerdidaNoDejaNadaParaReinvertir() = runTest {
        crearNegocio(porcentajeReinversion = 20.0)
        vender(100_000.0)
        costear(400_000.0)

        val resumen = resumen()

        assertTrue(resumen.ganancia < 0)
        // El 20 % de una pérdida sería reinvertir plata que no existe.
        assertEquals(0.0, resumen.reinversion, 0.001)
    }

    @Test
    fun loDisponibleDescuentaObligacionesMetaYReinversion() = runTest {
        crearNegocio(porcentajeReinversion = 10.0)
        vender(2_000_000.0)
        costear(1_000_000.0)
        obligacionRepository.registrarObligacion(negocioId, "Cuota", 200_000.0, hoy.plusDays(10))
        baseFinancieraRepository.definirMetaAhorro(negocioId, 600_000.0, hoy.plusMonths(3))

        val resumen = resumen()

        assertEquals(1_000_000.0, resumen.ganancia, 0.001)
        assertEquals(200_000.0, resumen.obligacionesPendientes, 0.001)
        // Tres meses justos para 600.000: algo más de 200.000 al mes.
        assertEquals(200_700.0, resumen.apartadoParaMeta, 500.0)
        assertEquals(100_000.0, resumen.reinversion, 0.001)
        assertEquals(499_300.0, resumen.disponible, 500.0)
    }

    @Test
    fun unaObligacionPagadaYaNoLeQuitaNadaALoDisponible() = runTest {
        crearNegocio()
        vender(1_000_000.0)
        obligacionRepository.registrarObligacion(negocioId, "Cuota", 200_000.0, hoy.plusDays(10))
        val obligacion = obligacionRepository.obligacionesDeNegocio(negocioId).first().single()
        obligacionRepository.marcarPagada(obligacion.id, pagada = true)

        assertEquals(0.0, resumen().obligacionesPendientes, 0.001)
        assertEquals(1_000_000.0, resumen().disponible, 0.001)
    }

    @Test
    fun sinMetaNoHayProgresoQueMostrar() = runTest {
        crearNegocio()
        vender(1_000_000.0)

        assertNull(resumen().progresoMeta)
        assertEquals(0.0, resumen().apartadoParaMeta, 0.001)
    }

    @Test
    fun laMetaAvanzaConLoQueElNegocioDejaLibre() = runTest {
        crearNegocio()
        baseFinancieraRepository.definirMetaAhorro(negocioId, 1_000_000.0, hoy.plusMonths(6))
        vender(700_000.0)
        costear(200_000.0)

        val progreso = resumen().progresoMeta

        assertNotNull(progreso)
        assertEquals(500_000.0, progreso?.acumulado ?: 0.0, 0.001)
        assertEquals(0.5f, progreso?.fraccion ?: 0f, 0.001f)
        assertEquals(500_000.0, progreso?.falta ?: 0.0, 0.001)
        assertFalse(progreso?.cumplida ?: true)
    }

    @Test
    fun elProgresoDeLaMetaDescuentaObligacionesYReinversion() = runTest {
        crearNegocio(porcentajeReinversion = 10.0)
        baseFinancieraRepository.definirMetaAhorro(negocioId, 1_000_000.0, hoy.plusMonths(3))
        vender(1_000_000.0)
        obligacionRepository.registrarObligacion(negocioId, "Cuota", 200_000.0, hoy.plusDays(10))

        // Lo que se debe y lo que vuelve al negocio no se puede ahorrar: 1.000.000 - 200.000 - 100.000.
        assertEquals(700_000.0, resumen().progresoMeta?.acumulado ?: 0.0, 0.001)
    }

    @Test
    fun elProgresoNoSeDescuentaASiMismo() = runTest {
        crearNegocio()
        baseFinancieraRepository.definirMetaAhorro(negocioId, 1_000_000.0, hoy.plusMonths(3))
        vender(1_000_000.0)

        val resumen = resumen()

        // El dinero disponible ya le restó lo apartado para la meta. Medir la meta con ese
        // número descontaría dos veces el mismo ahorro y el progreso nunca llegaría.
        assertTrue(resumen.apartadoParaMeta > 0.0)
        assertEquals(1_000_000.0, resumen.progresoMeta?.acumulado ?: 0.0, 0.001)
        assertEquals(resumen.disponible + resumen.apartadoParaMeta, resumen.libreParaAhorrar, 0.001)
    }

    @Test
    fun laMetaSeDaPorCumplidaAlLlegarAlObjetivo() = runTest {
        crearNegocio()
        baseFinancieraRepository.definirMetaAhorro(negocioId, 500_000.0, hoy.plusMonths(6))
        vender(500_000.0)

        val progreso = resumen().progresoMeta

        assertTrue(progreso?.cumplida ?: false)
        assertEquals(1f, progreso?.fraccion ?: 0f, 0.001f)
        // Pasarse de la meta no es deber plata: lo que falta es cero, no negativo.
        assertEquals(0.0, progreso?.falta ?: -1.0, 0.001)
    }

    @Test
    fun unMesEnPerdidaDejaLaMetaEnCeroYNoEnNegativo() = runTest {
        crearNegocio()
        baseFinancieraRepository.definirMetaAhorro(negocioId, 500_000.0, hoy.plusMonths(6))
        costear(300_000.0)

        val progreso = resumen().progresoMeta

        assertEquals(0.0, progreso?.acumulado ?: -1.0, 0.001)
        assertEquals(0f, progreso?.fraccion ?: -1f, 0.001f)
    }

    @Test
    fun loDeAntesDeLaMetaNoCuentaParaLaMeta() = runTest {
        crearNegocio()
        vender(800_000.0, ahora.minusDays(10))
        baseFinancieraRepository.definirMetaAhorro(negocioId, 1_000_000.0, hoy.plusMonths(6))
        vender(300_000.0)

        // La meta se definió hoy: solo cuenta lo de hoy en adelante.
        assertEquals(300_000.0, resumen().progresoMeta?.acumulado ?: 0.0, 0.001)
        // El resumen del mes sí ve las dos ventas: habla del mes completo.
        assertEquals(1_100_000.0, resumen().ingresos, 0.001)
    }

    @Test
    fun unNegocioSinMovimientosLoDiceEnVezDeMostrarUnTableroDeCeros() = runTest {
        crearNegocio()

        val resumen = resumen()

        assertTrue(resumen.sinMovimientos)
        assertNull(resumen.margen)
    }

    @Test
    fun elMargenEsLaProporcionDeLoVendidoQueQuedoComoGanancia() = runTest {
        crearNegocio()
        vender(1_000_000.0)
        costear(400_000.0)

        assertEquals(0.6f, resumen().margen ?: 0f, 0.001f)
    }

    @Test
    fun elResumenEsDelMesPedidoYNoDeTodaLaHistoria() = runTest {
        crearNegocio()
        vender(500_000.0, ahora.minusMonths(1))
        vender(300_000.0)

        assertEquals(300_000.0, resumen().ingresos, 0.001)
    }
}
