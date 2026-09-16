package com.gestor_ventures.front.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Negocio
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.repository.SesionRepository
import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.RolNegocio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    negocioRepository: NegocioRepository,
    sesionRepository: SesionRepository,
) : ViewModel() {

    /** Negocio elegido a mano en el menú; si es nulo manda el primero de la lista. */
    private val seleccionado = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        negocioRepository.negociosDeUsuario(sesionRepository.usuarioId()),
        seleccionado,
    ) { negocios, elegido ->
        MainUiState(
            usuario = MainPreviewData.usuario,
            negocios = negocios.map(::aNegocioUi),
            negocioActivoId = elegido,
            // Las notificaciones (HU-40/HU-41) todavía no tienen datos reales.
            notificacionesSinLeer = MainPreviewData.NOTIFICACIONES_SIN_LEER,
            cargando = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(usuario = MainPreviewData.usuario),
    )

    /** Cambia el negocio activo desde el menú lateral. */
    fun seleccionarNegocio(negocioId: String) {
        seleccionado.value = negocioId
    }
}

/**
 * El rol todavía no existe en la base de datos: quien crea el negocio es su Líder. Cuando se
 * modele el equipo (tabla pendiente), saldrá de ahí.
 */
private fun aNegocioUi(negocio: Negocio) = NegocioUi(
    id = negocio.id.toString(),
    nombre = negocio.nombre,
    categoria = negocio.categoria,
    rol = RolNegocio.Lider,
)
