package com.gestor_ventures.front.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Negocio
import com.gestor_ventures.back.repository.AlertaRepository
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
import com.gestor_ventures.back.model.ModoTema
import com.gestor_ventures.back.repository.AparienciaRepository
import com.gestor_ventures.back.repository.Onboarding
import com.gestor_ventures.back.repository.EquipoRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    negocioRepository: NegocioRepository,
    sesionRepository: SesionRepository,
    private val negocioActivoRepository: NegocioActivoRepository,
    private val authRepository: AuthRepository,
    alertaRepository: AlertaRepository,
    private val onboarding: Onboarding,
    private val equipoRepository: EquipoRepository,
    private val aparienciaRepository: AparienciaRepository,
) : ViewModel() {

    init {
        // Un vendedor invitado que entra con su correo acepta la invitación: pasa de "Pendiente"
        // a "Activo" en la pantalla de equipo del Líder.
        viewModelScope.launch {
            sesionRepository.usuarioActual
                .map { it?.correo }
                .distinctUntilChanged()
                .collect { correo -> correo?.let { equipoRepository.aceptarInvitaciones(it) } }
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        sesionRepository.usuarioActual.flatMapLatest { usuario ->
            usuario?.let { negocioRepository.negociosAccesibles(it.id, it.correo) } ?: flowOf(emptyList())
        },
        // El negocio elegido a mano lo guarda el repositorio, no esta pantalla: el inicio y el
        // registro de ventas necesitan saber el mismo.
        negocioActivoRepository.seleccionado,
        sesionRepository.usuarioActual,
        // HU-38. El número de la campana sale de las alertas de verdad del negocio activo. Era
        // un número fijo, así que marcaba avisos pendientes aunque no hubiera ninguno.
        negocioActivoRepository.negocioActivoId.flatMapLatest { negocioId ->
            if (negocioId == null) flowOf(emptyList()) else alertaRepository.alertas(negocioId)
        },
        aparienciaRepository.modo,
    ) { negocios, elegido, usuario, alertas, modoTema ->
        MainUiState(
            usuario = usuario?.aUsuarioUi(),
            negocios = negocios.map { aNegocioUi(it, esLider = it.usuarioId == usuario?.id) },
            negocioActivoId = elegido?.toString(),
            notificacionesSinLeer = alertas.count { !it.leida },
            modoTema = modoTema,
            cargando = false,
        )
    }.combine(onboarding.pendientes) { estado, pendientes ->
        estado.copy(onboardingPendiente = estado.usuario?.correo?.trim()?.lowercase() in pendientes)
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

    /** Claro, oscuro o como el dispositivo: se aplica al instante en toda la app. */
    fun cambiarModoTema(modo: ModoTema) {
        aparienciaRepository.cambiar(modo)
    }

    /** Cambia el negocio activo desde el menú lateral. */
    fun seleccionarNegocio(negocioId: String) {
        // Con un negocio ya creado, la cuenta deja de ser "nueva sin negocio".
        uiState.value.usuario?.correo?.let(onboarding::completar)
        negocioId.toLongOrNull()?.let(negocioActivoRepository::seleccionar)
    }
}

/** Quien creó el negocio es su Líder; quien entró por una invitación es Vendedor. */
private fun aNegocioUi(negocio: Negocio, esLider: Boolean) = NegocioUi(
    id = negocio.id.toString(),
    nombre = negocio.nombre,
    categoria = negocio.categoria,
    rol = if (esLider) RolNegocio.Lider else RolNegocio.Vendedor,
    colorMarca = negocio.colorMarca,
)

private fun Usuario.aUsuarioUi() = UsuarioUi(nombre = nombre, correo = correo, fotoPerfil = fotoPerfilUrl)
