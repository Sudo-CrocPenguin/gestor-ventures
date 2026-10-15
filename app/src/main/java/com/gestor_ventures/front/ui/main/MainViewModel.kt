package com.gestor_ventures.front.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Negocio
import com.gestor_ventures.back.repository.AuthRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.NegocioRepository
import com.gestor_ventures.back.model.Usuario
import com.gestor_ventures.back.repository.SesionRepository
import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.RolNegocio
import com.gestor_ventures.front.model.UsuarioUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    negocioRepository: NegocioRepository,
    sesionRepository: SesionRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        sesionRepository.observarUsuarioId().flatMapLatest { usuarioId ->
            usuarioId?.let(negocioRepository::negociosDeUsuario) ?: flowOf(emptyList())
        },
        // El negocio elegido a mano lo guarda el repositorio, no esta pantalla: el inicio y el
        // registro de ventas necesitan saber el mismo.
        negocioActivoRepository.seleccionado,
        sesionRepository.usuarioActual,
    ) { negocios, elegido, usuario ->
        MainUiState(
            usuario = usuario?.aUsuarioUi(),
            negocios = negocios.map(::aNegocioUi),
            negocioActivoId = elegido?.toString(),
            // Las notificaciones (HU-40/HU-41) todavía no tienen datos reales.
            notificacionesSinLeer = MainPreviewData.NOTIFICACIONES_SIN_LEER,
            cargando = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    /**
     * HU-02. Cierra la sesión desde el menú lateral.
     *
     * No hay que navegar a ninguna parte: `AppRoot` sigue al usuario de la sesión y en cuanto
     * queda en nulo muestra el login solo.
     */
    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }

    /** Cambia el negocio activo desde el menú lateral. */
    fun seleccionarNegocio(negocioId: String) {
        negocioId.toLongOrNull()?.let(negocioActivoRepository::seleccionar)
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
    colorMarca = negocio.colorMarca,
)

private fun Usuario.aUsuarioUi() = UsuarioUi(nombre = nombre, correo = correo)
