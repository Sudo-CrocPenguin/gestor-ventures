package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Negocio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Con qué negocio está trabajando el usuario ahora mismo.
 *
 * Vive fuera de las pantallas porque no es de ninguna: el menú lateral lo cambia, la barra
 * superior lo muestra, el inicio resume sus ventas y el registro de ventas lo necesita para
 * saber a quién apuntarle la venta. Si cada pantalla guardara el suyo, tarde o temprano dos
 * dirían cosas distintas.
 */
@Singleton
class NegocioActivoRepository @Inject constructor(
    private val negocioRepository: NegocioRepository,
    private val sesionRepository: SesionRepository,
) {

    private val elegido = MutableStateFlow<Long?>(null)

    /** Lo que el usuario escogió a mano, sin resolver. El menú lo usa para marcar cuál está. */
    val seleccionado: StateFlow<Long?> = elegido.asStateFlow()

    /**
     * El negocio activo: el elegido a mano y, si no hay ninguno, el primero de la lista. Así la
     * app funciona desde el primer momento sin obligar a elegir.
     */
    val negocioActivo: Flow<Negocio?> = combine(
        negocioRepository.negociosDeUsuario(sesionRepository.usuarioId()),
        elegido,
    ) { negocios, elegidoId ->
        negocios.firstOrNull { it.id == elegidoId } ?: negocios.firstOrNull()
    }

    /** El id del negocio activo, para quien solo necesita saber a quién apuntarle los datos. */
    val negocioActivoId: Flow<Long?> = negocioActivo.map { it?.id }

    fun seleccionar(negocioId: Long) {
        elegido.value = negocioId
    }
}
