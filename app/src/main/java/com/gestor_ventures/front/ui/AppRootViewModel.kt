package com.gestor_ventures.front.ui

import androidx.lifecycle.ViewModel
import com.gestor_ventures.back.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Con o sin sesión abierta: de eso depende si [AppRoot] muestra el login o la app. */
@HiltViewModel
class AppRootViewModel @Inject constructor(
    sesionRepository: SesionRepository,
) : ViewModel() {

    val usuarioId: StateFlow<Long?> = sesionRepository.observarUsuarioId()
}
