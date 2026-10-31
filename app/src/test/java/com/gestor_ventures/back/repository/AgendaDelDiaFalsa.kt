package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Cita
import com.gestor_ventures.back.model.Pedido
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/** [AgendaDelDia] en memoria para las pruebas: lo que se ponga en [citas] y [pedidos] es lo que sale. */
class AgendaDelDiaFalsa : AgendaDelDia {

    val citas = MutableStateFlow<List<Cita>>(emptyList())
    val pedidos = MutableStateFlow<List<Pedido>>(emptyList())

    override fun citasEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Cita>> = citas

    override fun pedidosEntre(negocioId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<Pedido>> = pedidos
}
