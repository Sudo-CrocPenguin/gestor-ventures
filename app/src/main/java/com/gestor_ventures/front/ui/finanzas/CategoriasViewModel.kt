package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-15. Categorías del negocio activo.
 *
 * La lista se vuelve a pedir cuando cambia la pestaña o el negocio: son dos listas distintas,
 * no una filtrada en memoria, porque la consulta ya sabe separarlas por tipo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val repository: CategoriaRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriasUiState())
    val uiState: StateFlow<CategoriasUiState> = _uiState.asStateFlow()

    private val tipo = MutableStateFlow(TipoCategoria.GASTO)

    init {
        viewModelScope.launch {
            combine(negocioActivoRepository.negocioActivoId, tipo) { negocioId, tipo ->
                negocioId to tipo
            }.flatMapLatest { (negocioId, tipo) ->
                if (negocioId == null) {
                    flowOf(emptyList())
                } else {
                    repository.categoriasDeNegocio(negocioId, tipo)
                }
            }.collect { categorias ->
                _uiState.update { it.copy(categorias = categorias, cargando = false) }
            }
        }
    }

    fun onTipoChange(nuevoTipo: TipoCategoria) {
        tipo.value = nuevoTipo
        _uiState.update { it.copy(tipo = nuevoTipo, cargando = true, error = null) }
    }

    // ---------- Que hacer con una categoria ----------

    /** Tocarla no la edita de una: primero se pregunta que se quiere hacer con ella. */
    fun abrirAcciones(categoria: Categoria) {
        _uiState.update { it.copy(acciones = categoria) }
    }

    fun cerrarAcciones() {
        _uiState.update { it.copy(acciones = null) }
    }

    fun editarLaElegida() {
        val categoria = _uiState.value.acciones ?: return
        cerrarAcciones()
        abrirFormularioDe(categoria)
    }

    /** Borrar sigue pidiendo confirmacion: hay que advertir que pasa con lo ya clasificado. */
    fun eliminarLaElegida() {
        val categoria = _uiState.value.acciones ?: return
        cerrarAcciones()
        pedirEliminar(categoria)
    }

    // ---------- El formulario ----------

    fun abrirFormularioNuevo() {
        _uiState.update { it.copy(formulario = FormularioCategoria(), error = null) }
    }

    fun abrirFormularioDe(categoria: Categoria) {
        _uiState.update {
            it.copy(
                formulario = FormularioCategoria(categoria.id, categoria.nombre),
                error = null,
            )
        }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formulario = null, error = null) }
    }

    fun onNombreChange(texto: String) {
        _uiState.update {
            it.copy(formulario = it.formulario?.copy(nombre = texto), error = null)
        }
    }

    /** Guarda el formulario: crea una nueva o le corrige el nombre a la que se abrió. */
    fun guardarFormulario() {
        val formulario = _uiState.value.formulario ?: return
        if (!formulario.puedeGuardar) return

        viewModelScope.launch {
            val error = if (formulario.categoriaId == null) {
                val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
                repository.crearCategoria(negocioId, formulario.nombre, _uiState.value.tipo)
            } else {
                repository.renombrarCategoria(formulario.categoriaId, formulario.nombre)
            }

            // Con error la hoja se queda abierta: si se cerrara, el usuario perdería lo escrito.
            _uiState.update {
                it.copy(formulario = if (error == null) null else it.formulario, error = error)
            }
        }
    }

    // ---------- Eliminar ----------

    /**
     * Borrar no es inmediato: lo clasificado con esa categoría queda sin etiqueta, y eso hay
     * que advertirlo antes y no después.
     */
    fun pedirEliminar(categoria: Categoria) {
        _uiState.update { it.copy(porEliminar = categoria) }
    }

    fun cancelarEliminar() {
        _uiState.update { it.copy(porEliminar = null) }
    }

    fun confirmarEliminar() {
        val categoria = _uiState.value.porEliminar ?: return
        viewModelScope.launch {
            repository.eliminarCategoria(categoria.id)
            _uiState.update { it.copy(porEliminar = null) }
        }
    }
}
