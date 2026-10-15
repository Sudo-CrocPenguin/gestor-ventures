package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorCategoria
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.db.dao.CategoriaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** HU-15: las reglas de una categoría antes de que llegue a la base de datos. */
class CategoriaRepositoryTest {

    private val dao = CategoriaDaoFalso()
    private val repository = CategoriaRepository(dao)

    private val negocioId = 1L

    private suspend fun crear(nombre: String, tipo: TipoCategoria = TipoCategoria.GASTO) =
        repository.crearCategoria(negocioId, nombre, tipo)

    @Test
    fun crearCategoria_laGuardaConSuTipo() = runTest {
        assertNull(crear("Transporte"))

        val categoria = repository.categoriasDeNegocio(negocioId).first().single()
        assertEquals("Transporte", categoria.nombre)
        assertEquals(TipoCategoria.GASTO, categoria.tipo)
    }

    @Test
    fun elNombreSeGuardaSinEspaciosDeSobra() = runTest {
        crear("  Transporte  ")

        assertEquals("Transporte", repository.categoriasDeNegocio(negocioId).first().single().nombre)
    }

    @Test
    fun elNombreEsObligatorio() = runTest {
        assertEquals(ErrorCategoria.NombreVacio, crear("   "))
        assertEquals(emptyList<Any>(), repository.categoriasDeNegocio(negocioId).first())
    }

    @Test
    fun elNombreNoPasaDeCincuentaCaracteres() = runTest {
        assertNull(crear("a".repeat(50)))
        assertEquals(ErrorCategoria.NombreMuyLargo, crear("b".repeat(51)))

        assertEquals(1, repository.categoriasDeNegocio(negocioId).first().size)
    }

    @Test
    fun noSePuedeRepetirElNombreEnElMismoTipo() = runTest {
        crear("Insumos", TipoCategoria.COSTO)

        assertEquals(ErrorCategoria.NombreRepetido, crear("Insumos", TipoCategoria.COSTO))
        // Ni escribiéndolo distinto: dos "Insumos" parten el resumen en dos filas.
        assertEquals(ErrorCategoria.NombreRepetido, crear("  insumos  ", TipoCategoria.COSTO))

        assertEquals(1, repository.categoriasDeNegocio(negocioId).first().size)
    }

    @Test
    fun elMismoNombreEnElOtroTipoSiSePuede() = runTest {
        crear("Transporte", TipoCategoria.GASTO)

        assertNull(crear("Transporte", TipoCategoria.COSTO))

        assertEquals(2, repository.categoriasDeNegocio(negocioId).first().size)
    }

    @Test
    fun otroNegocioPuedeLlamarleIgualASuCategoria() = runTest {
        crear("Insumos", TipoCategoria.COSTO)

        assertNull(repository.crearCategoria(2L, "Insumos", TipoCategoria.COSTO))
    }

    @Test
    fun elSelectorSoloTraeLasDelTipoQueSePide() = runTest {
        crear("Transporte", TipoCategoria.GASTO)
        crear("Insumos", TipoCategoria.COSTO)

        val deCosto = repository.categoriasDeNegocio(negocioId, TipoCategoria.COSTO).first()

        assertEquals(listOf("Insumos"), deCosto.map { it.nombre })
    }

    @Test
    fun renombrar_cambiaElNombreSinCrearOtra() = runTest {
        crear("Insumos", TipoCategoria.COSTO)
        val categoria = repository.categoriasDeNegocio(negocioId).first().single()

        assertNull(repository.renombrarCategoria(categoria.id, "Materia prima"))

        val renombrada = repository.categoriasDeNegocio(negocioId).first().single()
        assertEquals(categoria.id, renombrada.id)
        assertEquals("Materia prima", renombrada.nombre)
    }

    @Test
    fun renombrar_noSeRechazaASiMisma() = runTest {
        crear("Insumos", TipoCategoria.COSTO)
        val categoria = repository.categoriasDeNegocio(negocioId).first().single()

        // Abrir la categoría y guardarla con el mismo nombre no puede contar como duplicado.
        assertNull(repository.renombrarCategoria(categoria.id, "Insumos"))
    }

    @Test
    fun renombrar_siExigeLoMismoQueCrear() = runTest {
        crear("Insumos", TipoCategoria.COSTO)
        crear("Empaques", TipoCategoria.COSTO)
        val insumos = repository.categoriasDeNegocio(negocioId).first().first { it.nombre == "Insumos" }

        assertEquals(ErrorCategoria.NombreVacio, repository.renombrarCategoria(insumos.id, " "))
        assertEquals(
            ErrorCategoria.NombreMuyLargo,
            repository.renombrarCategoria(insumos.id, "a".repeat(51)),
        )
        assertEquals(
            ErrorCategoria.NombreRepetido,
            repository.renombrarCategoria(insumos.id, "Empaques"),
        )
        // Ningún intento inválido alcanzó a dañar el nombre que ya estaba bien.
        assertEquals("Insumos", repository.categoriasDeNegocio(negocioId).first().first { it.id == insumos.id }.nombre)
    }

    @Test
    fun renombrarUnaCategoriaQueYaNoExisteNoRompeNada() = runTest {
        assertNull(repository.renombrarCategoria(99L, "Insumos"))
    }

    @Test
    fun eliminar_laSacaDeLaLista() = runTest {
        crear("Insumos", TipoCategoria.COSTO)
        val categoria = repository.categoriasDeNegocio(negocioId).first().single()

        repository.eliminarCategoria(categoria.id)

        assertEquals(emptyList<Any>(), repository.categoriasDeNegocio(negocioId).first())
    }
}
