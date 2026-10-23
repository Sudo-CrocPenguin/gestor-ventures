package com.gestor_ventures.back.model

import java.time.LocalDate
import java.time.LocalTime

val HoraDeReferenciaPedido: LocalTime = LocalTime.of(8, 0)

enum class AnticipacionRecordatorio(val minutos: Int) {
    QuinceMinutos(15),
    MediaHora(30),
    UnaHora(60),
    UnDia(24 * 60),
    ;

    companion object {
        fun deMinutos(minutos: Int?): AnticipacionRecordatorio? = entries.firstOrNull { it.minutos == minutos }
    }
}

sealed interface ContenidoRecordatorio {
    data class DeCita(val nombreCliente: String, val fecha: LocalDate, val hora: LocalTime) :
        ContenidoRecordatorio

    data class DePedido(val nombreCliente: String, val fechaEntrega: LocalDate) : ContenidoRecordatorio
}
