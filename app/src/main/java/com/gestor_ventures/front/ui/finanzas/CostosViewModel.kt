package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.Costo
import com.gestor_ventures.back.model.MargenProducto
import com.gestor_ventures.back.model.TipoCategoria
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.usecase.CalcularMargen
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
import java.time.LocalDateTime
import javax.inject.Inject

/** Máximo de dígitos de un monto, igual que en el resto de formularios. */
private const val MaxDigitosMonto = 12

/** Lo que la pantalla necesita de la base de datos, junto, para no armar el estado a medias. */
private data class DatosDeCostos(
    val costos: List<Costo> = emptyList(),
    val total: Double = 0.0,
    val categorias: List<Categoria> = emptyList(),
    val margenes: List<MargenProducto> = emptyList(),
)

/**
 * HU-13. Costos del negocio activo en el mes en curso, y el margen que dejan sus productos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CostosViewModel @Inject constructor(
    private val costoRepository: CostoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    private val calcularMargen: CalcularMargen,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CostosUiState(mes = costoRepository.mesActual()))
    val uiState: StateFlow<CostosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.flatMapLatest { negocioId ->
                if (negocioId == null) {
                    flowOf(DatosDeCostos())
                } else {
                    val mes = costoRepository.mesActual()
                    combine(
                        costoRepository.costosDelMes(negocioId, mes),
                        costoRepository.totalDelMes(negocioId, mes),
                        categoriaRepository.categoriasDeNegocio(negocioId, TipoCategoria.COSTO),
                        calcularMargen(negocioId, mes),
                    ) { costos, total, categorias, margenes ->
                        DatosDeCostos(costos, total, categorias, margenes)
                    }
                }
            }.collect { datos ->
                _uiState.update { estado ->
                    estado.copy(
                        costos = datos.costos.map { it.conNombreDeCategoria(datos.categorias) },
                        total = datos.total,
                        categorias = datos.categorias,
                        margenes = datos.margenes,
                        cargando = false,
                    )
                }
            }
        }
    }

    // ---------- Qué hacer con un costo ----------

    fun abrirAcciones(costo: CostoUi) {
        _uiState.update { it.copy(acciones = costo) }
    }

    fun cerrarAcciones() {
        _uiState.update { it.copy(acciones = null) }
    }

    fun editarElCostoElegido() {
        val costo = _uiState.value.acciones?.costo ?: return
        cerrarAcciones()
        abrirFormularioDe(costo)
    }

    fun eliminarElCostoElegido() {
        val costo = _uiState.value.acciones?.costo ?: return
        cerrarAcciones()
        eliminar(costo.id)
    }

    // ---------- El formulario ----------

    fun abrirFormularioNuevo() {
        _uiState.update {
            it.copy(formulario = FormularioCosto(fecha = costoRepository.ahora()), error = null)
        }
    }

    private fun abrirFormularioDe(costo: Costo) {
        _uiState.update {
            it.copy(
                formulario = FormularioCosto(
                    costoId = costo.id,
                    productoServicio = costo.productoServicio,
                    monto = costo.monto.toLong().toString(),
                    fecha = costo.fecha,
                    categoriaId = costo.categoriaId,
                ),
                error = null,
            )
        }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formulario = null, error = null) }
    }

    fun onProductoChange(texto: String) {
        _uiState.update {
            it.copy(formulario = it.formulario?.copy(productoServicio = texto), error = null)
        }
    }

    fun onMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        _uiState.update {
            it.copy(formulario = it.formulario?.copy(monto = digitos), error = null)
        }
    }

    fun onFechaChange(fecha: LocalDateTime) {
        _uiState.update { it.copy(formulario = it.formulario?.copy(fecha = fecha), error = null) }
    }

    /** Volver a tocar la categoría elegida la quita: así se deja el costo sin clasificar. */
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

    fun guardarFormulario() {
        val formulario = _uiState.value.formulario ?: return
        if (!formulario.puedeGuardar) return

        viewModelScope.launch {
            val error = if (formulario.costoId == null) {
                val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch
                costoRepository.registrarCosto(
                    negocioId = negocioId,
                    productoServicio = formulario.productoServicio,
                    monto = formulario.montoValor.toDouble(),
                    fecha = formulario.fecha,
                    categoriaId = formulario.categoriaId,
                )
            } else {
                costoRepository.editarCosto(
                    costoId = formulario.costoId,
                    productoServicio = formulario.productoServicio,
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

    fun eliminar(costoId: Long) {
        viewModelScope.launch { costoRepository.eliminarCosto(costoId) }
    }
}

/**
 * La fila muestra el nombre de la categoría, no su id. Si la categoría se borró mientras tanto,
 * el costo se muestra sin etiqueta en vez de desaparecer.
 */
private fun Costo.conNombreDeCategoria(categorias: List<Categoria>) = CostoUi(
    costo = this,
    categoria = categorias.firstOrNull { it.id == categoriaId }?.nombre,
)
