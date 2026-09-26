package com.gestor_ventures.front.ui.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.repository.ClienteRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU-29. Registrar y corregir los clientes del negocio activo.
 *
 * La lista viene del repositorio y las hojas abiertas viven acá: así, al guardar, la fila
 * cambia sola sin que la pantalla tenga que pedir nada de vuelta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClientesViewModel @Inject constructor(
    private val repository: ClienteRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    init {
        // La lista la manda el repositorio; lo que el usuario está haciendo vive acá y responde
        // al instante, sin pasar por la base de datos para mostrar una letra escrita.
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId
                .flatMapLatest { negocioId ->
                    if (negocioId == null) {
                        flowOf(emptyList())
                    } else {
                        repository.clientesDeNegocio(negocioId)
                    }
                }
                .collect { clientes ->
                    _uiState.update { it.copy(clientes = clientes, cargando = false) }
                }
        }
    }

    // ---------- Formulario ----------

    fun abrirFormularioNuevo() {
        _uiState.update { it.copy(formulario = FormularioCliente(), error = null) }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(formulario = null, error = null) }
    }

    fun onNombreChange(texto: String) {
        actualizarFormulario { it.copy(nombre = texto) }
    }

    /** Se deja escribir con espacios y guiones; el repositorio lo guarda en puros dígitos. */
    fun onTelefonoChange(texto: String) {
        actualizarFormulario { it.copy(telefono = texto) }
    }

    fun onCorreoChange(texto: String) {
        actualizarFormulario { it.copy(correo = texto) }
    }

    fun onNotasChange(texto: String) {
        actualizarFormulario { it.copy(notas = texto) }
    }

    private fun actualizarFormulario(cambio: (FormularioCliente) -> FormularioCliente) {
        _uiState.update { estado ->
            estado.copy(formulario = estado.formulario?.let(cambio), error = null)
        }
    }

    fun guardarFormulario() {
        val formulario = _uiState.value.formulario ?: return
        if (!formulario.puedeGuardar) return

        viewModelScope.launch {
            val negocioId = negocioActivoRepository.negocioActivoId.first() ?: return@launch

            val error = if (formulario.clienteId == null) {
                repository.crearCliente(
                    negocioId = negocioId,
                    nombre = formulario.nombre,
                    telefono = formulario.telefono,
                    correo = formulario.correo,
                    notas = formulario.notas,
                )
            } else {
                repository.editarCliente(
                    clienteId = formulario.clienteId,
                    nombre = formulario.nombre,
                    telefono = formulario.telefono,
                    correo = formulario.correo,
                    notas = formulario.notas,
                )
            }

            // Con error la hoja se queda abierta: lo escrito no se pierde y se puede corregir.
            _uiState.update {
                it.copy(error = error, formulario = if (error == null) null else formulario)
            }
        }
    }

    // ---------- Acciones sobre un cliente ----------

    /** Tocar un cliente no lo edita de una: primero se pregunta qué se quiere hacer con él. */
    fun abrirAcciones(cliente: Cliente) {
        _uiState.update { it.copy(acciones = cliente) }
    }

    fun cerrarAcciones() {
        _uiState.update { it.copy(acciones = null) }
    }

    fun editarElElegido() {
        val cliente = _uiState.value.acciones ?: return
        _uiState.update {
            it.copy(
                acciones = null,
                error = null,
                formulario = FormularioCliente(
                    clienteId = cliente.id,
                    nombre = cliente.nombre,
                    telefono = cliente.telefono.orEmpty(),
                    correo = cliente.correo.orEmpty(),
                    notas = cliente.notas.orEmpty(),
                ),
            )
        }
    }

    fun eliminarElElegido() {
        val cliente = _uiState.value.acciones ?: return
        cerrarAcciones()
        viewModelScope.launch { repository.eliminarCliente(cliente.id) }
    }
}
