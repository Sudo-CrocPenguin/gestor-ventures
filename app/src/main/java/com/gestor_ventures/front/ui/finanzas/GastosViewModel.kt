package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.Gasto
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.GastoRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Máximo de dígitos de un monto, igual que en el resto de formularios. */
private const val MaxDigitosMonto = 12

/**
 * HU-14. Gastos generales del negocio activo, del mes en curso.
 *
 * Trae también las categorías de tipo gasto, porque el formulario las necesita para clasificar
 * y la lista para mostrar el nombre en vez del id.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GastosViewModel @Inject constructor(
    private val gastoRepository: GastoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GastosUiState(mes = gastoRepository.mesActual()))
    val uiState: StateFlow<GastosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.flatMapLatest { negocioId ->
                if (negocioId == null) {
                    flowOf(Triple(emptyList<Gasto>(), 0.0, emptyList<Categoria>()))
                } else {
                    val mes = gastoRepository.mesActual()
                    combine(
                        gastoRepository.gastosDelMes(negocioId, mes),
                        gastoRepository.totalDelMes(negocioId, mes),
                        categoriaRepository.categoriasDeNegocio(negocioId, TipoCategoria.GASTO),
                    ) { gastos, total, categorias -> Triple(gastos, total, categorias) }
                }
            }.collect { (gastos, total, categorias) ->
                _uiState.update { estado ->
                    estado.copy(
                        gastos = gastos.map { it.conNombreDeCategoria(categorias) },
                        total = total,
                        categorias = categorias,
                        cargando = false,
                    )
                }
            }
        }
    }

    // ---------- Qué hacer con un gasto ----------

    /** Tocar un gasto no lo edita de una: primero se pregunta qué se quiere hacer con él. */
    fun abrirAcciones(gasto: GastoUi) {
        _uiState.update { it.copy(acciones = gasto) }
    }

    fun cerrarAcciones() {
        _uiState.update { it.copy(acciones = null) }
    }

    /** El menú se cierra al elegir: no tiene sentido dejarlo encima del formulario. */
    fun editarElGastoElegido() {
        val gasto = _uiState.value.acciones?.gasto ?: return
        cerrarAcciones()
        abrirFormularioDe(gasto)
    }

    fun eliminarElGastoElegido() {
        val gasto = _uiState.value.acciones?.gasto ?: return
        cerrarAcciones()
        eliminar(gasto.id)
    }

    // ---------- El formulario ----------

    fun abrirFormularioNuevo() {
        _uiState.update {
            it.copy(formulario = FormularioGasto(fecha = gastoRepository.hoy()), error = null)
        }
    }

    fun abrirFormularioDe(gasto: Gasto) {
        _uiState.update {
            it.copy(
                formulario = FormularioGasto(
                    gastoId = gasto.id,
                    descripcion = gasto.descripcion,
                    monto = gasto.monto.toLong().toString(),
                    fecha = gasto.fecha,
                    categoriaId = gasto.categoriaId,
                ),
                error = null,
            )
        }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formulario = null, error = null) }
    }

    fun onDescripcionChange(texto: String) {
        _uiState.update {
            it.copy(formulario = it.formulario?.copy(descripcion = texto), error = null)
        }
    }

    fun onMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update {
            it.copy(formulario = it.formulario?.copy(monto = digitos), error = null)
        }
    }

    fun onFechaChange(fecha: LocalDate) {
        _uiState.update { it.copy(formulario = it.formulario?.copy(fecha = fecha), error = null) }
    }

    /** Volver a tocar la categoría ya elegida la quita: es la forma de dejar el gasto sin clasificar. */
    fun onCategoriaChange(categoriaId: Long?) {
        _uiState.update { estado ->
            val actual = estado.formulario?.categoriaId
            estado.copy(
                formulario = estado.formulario?.copy(
                    categoriaId = if (categoriaId == actual) null else categoriaId,
                ),
            )
        }
    }

    /** Guarda el formulario: registra uno nuevo o corrige el que se abrió, según traiga id. */
    fun guardarFormulario() {
        val formulario = _uiState.value.formulario ?: return
        if (!formulario.puedeGuardar) return

        viewModelScope.launch {
            val error = if (formulario.gastoId == null) {
                val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
                gastoRepository.registrarGasto(
                    negocioId = negocioId,
                    descripcion = formulario.descripcion,
                    monto = formulario.montoValor.toDouble(),
                    fecha = formulario.fecha,
                    categoriaId = formulario.categoriaId,
                )
            } else {
                gastoRepository.editarGasto(
                    gastoId = formulario.gastoId,
                    descripcion = formulario.descripcion,
                    monto = formulario.montoValor.toDouble(),
                    fecha = formulario.fecha,
                    categoriaId = formulario.categoriaId,
                )
            }

            // Con error la hoja se queda abierta: si se cerrara, el usuario perdería lo escrito.
            _uiState.update {
                it.copy(formulario = if (error == null) null else it.formulario, error = error)
            }
        }
    }

    fun eliminar(gastoId: Long) {
        viewModelScope.launch { gastoRepository.eliminarGasto(gastoId) }
    }
}

/**
 * La fila muestra el nombre de la categoría, no su id. Si la categoría se borró mientras tanto,
 * el gasto se muestra sin etiqueta en vez de desaparecer.
 */
private fun Gasto.conNombreDeCategoria(categorias: List<Categoria>) = GastoUi(
    gasto = this,
    categoria = categorias.firstOrNull { it.id == categoriaId }?.nombre,
)
