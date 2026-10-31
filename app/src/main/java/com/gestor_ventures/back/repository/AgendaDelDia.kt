package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Cita
import com.gestor_ventures.back.model.Pedido
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Lo que la pantalla de inicio necesita saber de la agenda: las citas y los pedidos de un rango
 * de días. Es una parte de [AgendaRepository], separada para que el inicio no dependa de todo lo
 * demás que ese repositorio hace (validar, agendar, recordatorios) y se pueda probar sin armarlo.
 */
interface AgendaDelDia {
    fun citasEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Cita>>
    fun pedidosEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Pedido>>
}
