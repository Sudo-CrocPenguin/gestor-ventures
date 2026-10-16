package com.gestor_ventures.front.ui.clientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Venta
import com.gestor_ventures.back.repository.ClienteRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.front.navigation.ArgumentoClienteId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-30. El perfil de un cliente y su historial de compras.
 *
 * El cliente y sus ventas se siguen por separado y se juntan acá: así, corregirle el nombre
 * desde la lista se ve en el perfil, y registrarle una venta aparece en el historial, sin que
 * ninguna de las dos pantallas tenga que avisarle a la otra.
 */
@HiltViewModel
class ClienteDetalleViewModel @Inject constructor(
    clienteRepository: ClienteRepository,
    ventaRepository: VentaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val clienteId: Long =
        savedStateHandle.get<String>(ArgumentoClienteId)?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ClienteDetalleUiState())
    val uiState: StateFlow<ClienteDetalleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                clienteRepository.observarCliente(clienteId),
                ventaRepository.ventasDeCliente(clienteId),
            ) { cliente, ventas ->
                ClienteDetalleUiState(
                    cliente = cliente,
                    compras = ventas.map(::aCompraUi),
                    cargando = false,
                )
            }.collect { nuevo -> _uiState.update { nuevo } }
        }
    }

    /**
     * Una venta rápida no dice qué se vendió —es el total del día—, así que la fila se queda
     * con el monto y la fecha en vez de inventarle un nombre.
     */
    private fun aCompraUi(venta: Venta) = CompraUi(
        id = venta.id,
        titulo = venta.productoServicio.orEmpty(),
        monto = venta.monto,
        fechaHora = venta.fechaHora,
    )
}
