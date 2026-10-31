package com.gestor_ventures.back.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** [Onboarding] en memoria para las pruebas de JVM, que no tienen `SharedPreferences`. */
class OnboardingFalso : Onboarding {

    private val _pendientes = MutableStateFlow<Set<String>>(emptySet())
    override val pendientes: StateFlow<Set<String>> = _pendientes

    override fun marcarPendiente(correo: String) {
        _pendientes.value = _pendientes.value + correo.trim().lowercase()
    }

    override fun completar(correo: String) {
        _pendientes.value = _pendientes.value - correo.trim().lowercase()
    }
}
