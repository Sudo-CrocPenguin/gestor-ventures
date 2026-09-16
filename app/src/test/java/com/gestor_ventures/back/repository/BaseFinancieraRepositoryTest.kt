package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorBaseFinanciera
import com.gestor_ventures.back.model.Frecuencia
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.db.dao.GastoFijoDaoFalso
import com.gestor_ventures.db.dao.MetaAhorroDaoFalso
import com.gestor_ventures.db.dao.NegocioDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** HU-06, HU-08 y HU-09: las reglas de la base financiera del negocio. */
class BaseFinancieraRepositoryTest {

    private val hoy = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val gastoFijoDao = GastoFijoDaoFalso()
    private val metaAhorroDao = MetaAhorroDaoFalso()
    private val negocioDao = NegocioDaoFalso()
    private val reloj = Reloj { hoy }

    private val repository = BaseFinancieraRepository(gastoFijoDao, metaAhorroDao, negocioDao, reloj)
    private val negocioRepository = NegocioRepository(negocioDao, reloj)

    private val negocioId = 1L

    private suspend fun crearNegocio() = negocioRepository.crearNegocio(
        usuarioId = 1L,
        nombre = "Dulce Antojo",
        tipoActividad = TipoActividad.PRODUCTOS,
        categoria = "Repostería",
        porcentajeReinversion = 0.0,
    )

    // ---------- Gastos fijos ----------

    @Test
    fun agregarGastoFijo_loGuardaYSuma() = runTest {
        repository.agregarGastoFijo(negocioId, "Arriendo local", 300_000.0, Frecuencia.MENSUAL)
        repository.agregarGastoFijo(negocioId, "Servicios", 120_000.0, Frecuencia.MENSUAL)

        val gastos = repository.gastosFijosDeNegocio(negocioId).first()
        assertEquals(2, gastos.size)
        assertEquals(420_000.0, repository.totalGastosFijos(negocioId).first(), 0.001)
    }

    @Test
    fun gastoFijo_exigeNombreYMontoPositivo() = runTest {
        assertEquals(
            ErrorBaseFinanciera.NombreGastoVacio,
            repository.agregarGastoFijo(negocioId, "   ", 300_000.0, Frecuencia.MENSUAL),
        )
        assertEquals(
            ErrorBaseFinanciera.MontoNoPositivo,
            repository.agregarGastoFijo(negocioId, "Arriendo", 0.0, Frecuencia.MENSUAL),
        )
        assertEquals(emptyList<Any>(), repository.gastosFijosDeNegocio(negocioId).first())
    }

    @Test
    fun editarGastoFijo_cambiaLoQueSeCorrigeYNoCreaOtro() = runTest {
        repository.agregarGastoFijo(negocioId, "Arriendo", 300_000.0, Frecuencia.MENSUAL)
        val gasto = repository.gastosFijosDeNegocio(negocioId).first().first()

        repository.editarGastoFijo(gasto.id, "  Arriendo local  ", 350_000.0, Frecuencia.QUINCENAL)

        val editado = repository.gastosFijosDeNegocio(negocioId).first().single()
        assertEquals(gasto.id, editado.id)
        assertEquals("Arriendo local", editado.nombre)
        assertEquals(350_000.0, editado.monto, 0.001)
        assertEquals(Frecuencia.QUINCENAL, editado.frecuencia)
    }

    @Test
    fun editarGastoFijo_exigeLoMismoQueCrearlo() = runTest {
        repository.agregarGastoFijo(negocioId, "Arriendo", 300_000.0, Frecuencia.MENSUAL)
        val gasto = repository.gastosFijosDeNegocio(negocioId).first().first()

        assertEquals(
            ErrorBaseFinanciera.NombreGastoVacio,
            repository.editarGastoFijo(gasto.id, "  ", 350_000.0, Frecuencia.MENSUAL),
        )
        assertEquals(
            ErrorBaseFinanciera.MontoNoPositivo,
            repository.editarGastoFijo(gasto.id, "Arriendo", 0.0, Frecuencia.MENSUAL),
        )
        // El gasto original queda intacto: un intento inválido no daña lo que ya estaba bien.
        assertEquals(300_000.0, repository.gastosFijosDeNegocio(negocioId).first().single().monto, 0.001)
    }

    @Test
    fun editarUnGastoQueYaNoExisteNoRompeNada() = runTest {
        assertNull(repository.editarGastoFijo(99L, "Arriendo", 300_000.0, Frecuencia.MENSUAL))
        assertEquals(emptyList<Any>(), repository.gastosFijosDeNegocio(negocioId).first())
    }

    @Test
    fun eliminarGastoFijo_loSacaDeLaLista() = runTest {
        repository.agregarGastoFijo(negocioId, "Arriendo local", 300_000.0, Frecuencia.MENSUAL)
        val gasto = repository.gastosFijosDeNegocio(negocioId).first().first()

        repository.eliminarGastoFijo(gasto.id)

        assertEquals(emptyList<Any>(), repository.gastosFijosDeNegocio(negocioId).first())
    }

    // ---------- Meta de ahorro ----------

    @Test
    fun definirMeta_laDejaComoActiva() = runTest {
        val error = repository.definirMetaAhorro(negocioId, 2_000_000.0, LocalDate.of(2026, 12, 31))

        assertNull(error)
        val meta = repository.metaActiva(negocioId).first()
        assertEquals(2_000_000.0, meta?.montoObjetivo ?: 0.0, 0.001)
        assertEquals(LocalDate.of(2026, 12, 31), meta?.fechaLimite)
    }

    @Test
    fun laMetaExigeMontoYFechaPosterior() = runTest {
        assertEquals(
            ErrorBaseFinanciera.MetaSinMonto,
            repository.definirMetaAhorro(negocioId, 0.0, LocalDate.of(2026, 12, 31)),
        )
        assertEquals(
            ErrorBaseFinanciera.FechaLimiteNoPosterior,
            repository.definirMetaAhorro(negocioId, 1_000_000.0, LocalDate.of(2026, 9, 16)),
        )
        assertNull(repository.metaActiva(negocioId).first())
    }

    // ---------- Reinversión ----------

    @Test
    fun definirPorcentaje_loGuardaEnElNegocio() = runTest {
        crearNegocio()

        val error = repository.definirPorcentajeReinversion(negocioId, 20.0)

        assertNull(error)
        val negocio = negocioRepository.observarNegocio(negocioId).first()
        assertEquals(20.0, negocio?.porcentajeReinversion ?: 0.0, 0.001)
    }

    @Test
    fun elPorcentajeVaDeCeroACien() = runTest {
        crearNegocio()

        assertEquals(
            ErrorBaseFinanciera.PorcentajeFueraDeRango,
            repository.definirPorcentajeReinversion(negocioId, 120.0),
        )
        assertEquals(
            ErrorBaseFinanciera.PorcentajeFueraDeRango,
            repository.definirPorcentajeReinversion(negocioId, -1.0),
        )
    }
}
