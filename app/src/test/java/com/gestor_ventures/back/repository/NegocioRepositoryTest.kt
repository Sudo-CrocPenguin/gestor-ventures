package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorNegocio
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoNegocio
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.db.dao.NegocioDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * HU-05. Prueba las reglas del repositorio sin base de datos: el DAO se reemplaza por uno
 * falso que guarda en memoria. Lo que se verifica acá son las validaciones y la traducción
 * entre el modelo de la app y la entidad de Room.
 */
class NegocioRepositoryTest {

    private val momento = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val dao = NegocioDaoFalso()
    private val repository = NegocioRepository(dao, Reloj { momento })

    private val usuarioId = 1L

    private suspend fun crear(
        nombre: String = "Dulce Antojo",
        categoria: String = "Repostería",
        porcentaje: Double = 30.0,
        colorMarca: String? = null,
    ) = repository.crearNegocio(
        usuarioId = usuarioId,
        nombre = nombre,
        tipoActividad = TipoActividad.PRODUCTOS,
        categoria = categoria,
        porcentajeReinversion = porcentaje,
        colorMarca = colorMarca,
    )

    @Test
    fun crearNegocio_guardaElColorDeMarcaYAdmiteNinguno() = runTest {
        crear(colorMarca = "#B8E0D2")

        assertEquals("#B8E0D2", repository.observarNegocio(1L).first()?.colorMarca)

        crear(nombre = "Bella Piel")

        assertNull(repository.observarNegocio(2L).first()?.colorMarca)
    }

    @Test
    fun crearNegocio_guardaYDevuelveElId() = runTest {
        val resultado = crear()

        assertEquals(ResultadoNegocio.Exito(1L), resultado)
        val negocios = repository.negociosDeUsuario(usuarioId).first()
        assertEquals(1, negocios.size)
        assertEquals("Dulce Antojo", negocios.first().nombre)
        assertEquals(TipoActividad.PRODUCTOS, negocios.first().tipoActividad)
    }

    @Test
    fun crearNegocio_guardaLaFechaDelReloj() = runTest {
        crear()

        assertEquals(momento, repository.negociosDeUsuario(usuarioId).first().first().fechaCreacion)
    }

    @Test
    fun crearNegocio_recortaLosEspaciosSobrantes() = runTest {
        crear(nombre = "  Dulce Antojo  ", categoria = "  Repostería ")

        val negocio = repository.negociosDeUsuario(usuarioId).first().first()
        assertEquals("Dulce Antojo", negocio.nombre)
        assertEquals("Repostería", negocio.categoria)
    }

    @Test
    fun elNombreEsObligatorio() = runTest {
        assertEquals(ResultadoNegocio.Invalido(ErrorNegocio.NombreVacio), crear(nombre = "   "))
        assertTrue(dao.negocios.value.isEmpty())
    }

    @Test
    fun elNombreNoPuedePasarDeCienCaracteres() = runTest {
        val resultado = crear(nombre = "a".repeat(101))

        assertEquals(ResultadoNegocio.Invalido(ErrorNegocio.NombreMuyLargo), resultado)
    }

    @Test
    fun laCategoriaEsObligatoria() = runTest {
        assertEquals(ResultadoNegocio.Invalido(ErrorNegocio.CategoriaVacia), crear(categoria = ""))
    }

    @Test
    fun elPorcentajeDeReinversionVaDeCeroACien() = runTest {
        assertEquals(
            ResultadoNegocio.Invalido(ErrorNegocio.PorcentajeFueraDeRango),
            crear(porcentaje = -1.0),
        )
        assertEquals(
            ResultadoNegocio.Invalido(ErrorNegocio.PorcentajeFueraDeRango),
            crear(porcentaje = 101.0),
        )
        assertEquals(ResultadoNegocio.Exito(1L), crear(porcentaje = 0.0))
    }

    @Test
    fun tieneNegocios_diceSiHayQuePedirElPrimero() = runTest {
        assertEquals(false, repository.tieneNegocios(usuarioId))

        crear()

        assertEquals(true, repository.tieneNegocios(usuarioId))
    }

    @Test
    fun actualizarNegocio_cambiaLosDatos() = runTest {
        crear()

        val resultado = repository.actualizarNegocio(
            negocioId = 1L,
            nombre = "Dulce Antojo SAS",
            tipoActividad = TipoActividad.MIXTO,
            categoria = "Repostería y eventos",
            porcentajeReinversion = 45.0,
        )

        assertEquals(ResultadoNegocio.Exito(1L), resultado)
        val negocio = repository.observarNegocio(1L).first()!!
        assertEquals("Dulce Antojo SAS", negocio.nombre)
        assertEquals(TipoActividad.MIXTO, negocio.tipoActividad)
        assertEquals(45.0, negocio.porcentajeReinversion, 0.001)
    }

    @Test
    fun actualizarNegocio_avisaSiNoExiste() = runTest {
        val resultado = repository.actualizarNegocio(
            negocioId = 99L,
            nombre = "Fantasma",
            tipoActividad = TipoActividad.SERVICIOS,
            categoria = "Otro",
            porcentajeReinversion = 10.0,
        )

        assertEquals(ResultadoNegocio.Invalido(ErrorNegocio.NegocioNoExiste), resultado)
    }
}

