package com.gestor_ventures.front.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Categoria
import com.gestor_ventures.back.model.FiltroMovimientos
import com.gestor_ventures.back.model.Movimiento
import com.gestor_ventures.back.model.PeriodoPredefinido
import com.gestor_ventures.back.model.rango
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.repository.CategoriaRepository
import com.gestor_ventures.back.model.ResultadoVenta
import com.gestor_ventures.back.usecase.ListarMovimientos
import com.gestor_ventures.front.model.MetodoPagoUi
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.model.aDominio
import com.gestor_ventures.front.model.aUi
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
 * HU-17. El historial del negocio activo.
 *
 * Al caso de uso siempre se le pide [FiltroMovimientos.Todos] a propósito, y el filtro se
 * aplica sobre la lista ya cargada: los totales del periodo tienen que contar también lo que el
 * filtro esconde, y cambiar de filtro no debería costar una consulta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val listarMovimientos: ListarMovimientos,
    private val ventaRepository: VentaRepository,
    private val gastoRepository: GastoRepository,
    private val costoRepository: CostoRepository,
    categoriaRepository: CategoriaRepository,
    negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    private val periodo = MutableStateFlow(PeriodoPredefinido.Hoy)

    init {
        viewModelScope.launch {
            combine(negocioActivoRepository.negocioActivoId, periodo) { negocioId, periodo ->
                negocioId to periodo
            }.flatMapLatest { (negocioId, periodo) ->
                if (negocioId == null) {
                    flowOf(emptyList<Movimiento>() to emptyList<Categoria>())
                } else {
                    combine(
                        listarMovimientos(negocioId, periodo.rango(listarMovimientos.hoy())),
                        categoriaRepository.categoriasDeNegocio(negocioId),
                    ) { movimientos, categorias -> movimientos to categorias }
                }
            }.collect { (movimientos, categorias) ->
                _uiState.update {
                    it.copy(todos = movimientos, categorias = categorias, cargando = false)
                }
            }
        }
    }

    fun onPeriodoChange(nuevo: PeriodoPredefinido) {
        periodo.value = nuevo
        _uiState.update { it.copy(periodo = nuevo, cargando = true) }
    }

    fun onFiltroChange(nuevo: FiltroMovimientos) {
        _uiState.update { it.copy(filtro = nuevo) }
    }

    /** Tocar un movimiento no lo borra de una: primero se pregunta qué se quiere hacer con él. */
    fun abrirAcciones(movimiento: Movimiento) {
        _uiState.update { it.copy(acciones = movimiento) }
    }

    fun cerrarAcciones() {
        _uiState.update { it.copy(acciones = null) }
    }

    /** Borrar no tiene vuelta atrás, así que del menú se pasa a una confirmación. */
    fun pedirConfirmacionDeEliminar() {
        val movimiento = _uiState.value.acciones ?: return
        _uiState.update { it.copy(acciones = null, porEliminar = movimiento) }
    }

    fun cancelarEliminar() {
        _uiState.update { it.copy(porEliminar = null) }
    }

    // ---------- Corregir (HU-17) ----------

    /** Abre el formulario del movimiento elegido, con lo que ya tiene guardado. */
    fun editarElMovimientoElegido() {
        val movimiento = _uiState.value.acciones ?: return
        _uiState.update { it.copy(acciones = null, formulario = formularioDe(movimiento)) }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formulario = null) }
    }

    /** El monto se escribe igual en los tres: solo dígitos, y la pantalla los formatea. */
    fun onMontoChange(texto: String) {
        val digitos = texto.filter(Char::isDigit).take(MaxDigitosMonto)
        actualizarFormulario(
            venta = { it.copy(campos = it.campos.copy(monto = digitos, error = null)) },
            gasto = { it.copy(campos = it.campos.copy(monto = digitos), error = null) },
            costo = { it.copy(campos = it.campos.copy(monto = digitos), error = null) },
        )
    }

    /** Qué se vendió, en qué se gastó, qué se costeó: el mismo campo con tres nombres. */
    fun onTituloChange(texto: String) {
        actualizarFormulario(
            venta = { it.copy(campos = it.campos.copy(productoServicio = texto, error = null)) },
            gasto = { it.copy(campos = it.campos.copy(descripcion = texto), error = null) },
            costo = { it.copy(campos = it.campos.copy(productoServicio = texto), error = null) },
        )
    }

    /** Solo cambia el día: la hora original se conserva. */
    fun onFechaChange(fecha: LocalDate) {
        actualizarFormulario(
            venta = {
                val hora = it.campos.fechaHora.toLocalTime()
                it.copy(campos = it.campos.copy(fechaHora = fecha.atTime(hora), error = null))
            },
            gasto = { it.copy(campos = it.campos.copy(fecha = fecha), error = null) },
            costo = {
                val hora = it.campos.fecha.toLocalTime()
                it.copy(campos = it.campos.copy(fecha = fecha.atTime(hora)), error = null)
            },
        )
    }

    fun onCategoriaChange(categoriaId: Long?) {
        actualizarFormulario(
            gasto = { it.copy(campos = it.campos.copy(categoriaId = categoriaId)) },
            costo = { it.copy(campos = it.campos.copy(categoriaId = categoriaId)) },
        )
    }

    fun onTipoRegistroChange(tipo: TipoRegistroVentaUi) {
        actualizarFormulario(
            venta = { it.copy(campos = it.campos.copy(tipoRegistro = tipo, error = null)) },
        )
    }

    fun onMetodoPagoChange(metodo: MetodoPagoUi) {
        actualizarFormulario(venta = { it.copy(campos = it.campos.copy(metodoPago = metodo)) })
    }

    fun onNotaChange(texto: String) {
        actualizarFormulario(venta = { it.copy(campos = it.campos.copy(nota = texto)) })
    }

    /**
     * Guarda la corrección. Si el repositorio la rechaza, la hoja se queda abierta con el
     * aviso: lo que el usuario escribió no se pierde por una regla que puede arreglar.
     */
    fun guardarFormulario() {
        val formulario = _uiState.value.formulario ?: return

        viewModelScope.launch {
            when (formulario) {
                is FormularioMovimiento.DeVenta -> guardarVenta(formulario)
                is FormularioMovimiento.DeGasto -> guardarGasto(formulario)
                is FormularioMovimiento.DeCosto -> guardarCosto(formulario)
            }
        }
    }

    private suspend fun guardarVenta(formulario: FormularioMovimiento.DeVenta) {
        val campos = formulario.campos
        if (!campos.puedeGuardar) return

        val resultado = ventaRepository.editarVenta(
            ventaId = formulario.ventaId,
            tipoRegistro = campos.tipoRegistro.aDominio(),
            monto = campos.montoValor.toDouble(),
            fechaHora = campos.fechaHora,
            productoServicio = campos.productoServicio,
            metodoPago = campos.metodoPago.aDominio(),
            nota = campos.nota,
        )
        when (resultado) {
            is ResultadoVenta.Exito -> cerrarFormulario()
            is ResultadoVenta.Invalido -> _uiState.update {
                it.copy(
                    formulario = formulario.copy(
                        campos = campos.copy(error = ErrorVentaUi.Regla(resultado.error)),
                    ),
                )
            }
        }
    }

    private suspend fun guardarGasto(formulario: FormularioMovimiento.DeGasto) {
        val campos = formulario.campos
        if (!campos.puedeGuardar || campos.gastoId == null) return

        val error = gastoRepository.editarGasto(
            gastoId = campos.gastoId,
            descripcion = campos.descripcion,
            monto = campos.montoValor.toDouble(),
            fecha = campos.fecha,
            categoriaId = campos.categoriaId,
        )
        if (error == null) cerrarFormulario() else _uiState.update {
            it.copy(formulario = formulario.copy(error = error))
        }
    }

    private suspend fun guardarCosto(formulario: FormularioMovimiento.DeCosto) {
        val campos = formulario.campos
        if (!campos.puedeGuardar || campos.costoId == null) return

        val error = costoRepository.editarCosto(
            costoId = campos.costoId,
            productoServicio = campos.productoServicio,
            monto = campos.montoValor.toDouble(),
            fecha = campos.fecha,
            categoriaId = campos.categoriaId,
        )
        if (error == null) cerrarFormulario() else _uiState.update {
            it.copy(formulario = formulario.copy(error = error))
        }
    }

    private fun formularioDe(movimiento: Movimiento): FormularioMovimiento = when (movimiento) {
        is Movimiento.DeVenta -> FormularioMovimiento.DeVenta(
            ventaId = movimiento.venta.id,
            campos = RegistrarVentaUiState(
                tipoRegistro = movimiento.venta.tipoRegistro.aUi(),
                monto = movimiento.venta.monto.toLong().toString(),
                productoServicio = movimiento.venta.productoServicio.orEmpty(),
                metodoPago = movimiento.venta.metodoPago?.aUi() ?: MetodoPagoUi.Efectivo,
                nota = movimiento.venta.nota.orEmpty(),
                fechaHora = movimiento.venta.fechaHora,
            ),
        )

        is Movimiento.DeGasto -> FormularioMovimiento.DeGasto(
            FormularioGasto(
                gastoId = movimiento.gasto.id,
                descripcion = movimiento.gasto.descripcion,
                monto = movimiento.gasto.monto.toLong().toString(),
                fecha = movimiento.gasto.fecha,
                categoriaId = movimiento.gasto.categoriaId,
            ),
        )

        is Movimiento.DeCosto -> FormularioMovimiento.DeCosto(
            FormularioCosto(
                costoId = movimiento.costo.id,
                productoServicio = movimiento.costo.productoServicio,
                monto = movimiento.costo.monto.toLong().toString(),
                fecha = movimiento.costo.fecha,
                categoriaId = movimiento.costo.categoriaId,
            ),
        )
    }

    /** Aplica el cambio a la variante que esté abierta y deja las otras como están. */
    private fun actualizarFormulario(
        venta: (FormularioMovimiento.DeVenta) -> FormularioMovimiento = { it },
        gasto: (FormularioMovimiento.DeGasto) -> FormularioMovimiento = { it },
        costo: (FormularioMovimiento.DeCosto) -> FormularioMovimiento = { it },
    ) {
        _uiState.update { estado ->
            estado.copy(
                formulario = when (val formulario = estado.formulario) {
                    is FormularioMovimiento.DeVenta -> venta(formulario)
                    is FormularioMovimiento.DeGasto -> gasto(formulario)
                    is FormularioMovimiento.DeCosto -> costo(formulario)
                    null -> null
                },
            )
        }
    }

    fun confirmarEliminar() {
        val movimiento = _uiState.value.porEliminar ?: return
        _uiState.update { it.copy(porEliminar = null) }

        viewModelScope.launch {
            when (movimiento) {
                is Movimiento.DeVenta -> ventaRepository.eliminarVenta(movimiento.id)
                is Movimiento.DeGasto -> gastoRepository.eliminarGasto(movimiento.id)
                is Movimiento.DeCosto -> costoRepository.eliminarCosto(movimiento.id)
            }
        }
    }
}
