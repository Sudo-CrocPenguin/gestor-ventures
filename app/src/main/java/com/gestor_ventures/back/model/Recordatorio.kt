package com.gestor_ventures.back.model

/**
 * HU-26. Los recordatorios de la agenda: cuánto antes avisar y qué decir.
 */

import java.time.LocalDate
import java.time.LocalTime

/**
 * Un pedido no tiene hora, solo día de entrega. Para contar la anticipación se toma esta hora
 * del día de entrega: "1 día antes" avisa a las 8:00 del día anterior.
 */
val HoraDeReferenciaPedido: LocalTime = LocalTime.of(8, 0)

/**
 * Cuánto antes avisar. Son unas pocas opciones fijas y no un número libre: es más rápido de
 * elegir y cubre lo que de verdad se usa. Se guarda en minutos.
 */
enum class AnticipacionRecordatorio(val minutos: Int) {
    QuinceMinutos(15),
    MediaHora(30),
    UnaHora(60),
    UnDia(24 * 60),
    ;

    companion object {
        /** La opción que corresponde a lo guardado; null si no hay recordatorio. */
        fun deMinutos(minutos: Int?): AnticipacionRecordatorio? = entries.firstOrNull { it.minutos == minutos }
    }
}

/**
 * Lo que dice el aviso. Se arma justo cuando suena, con los datos de ese momento, por si el
 * cliente o la fecha cambiaron después de programarlo.
 */
sealed interface ContenidoRecordatorio {
    data class DeCita(val nombreCliente: String, val fecha: LocalDate, val hora: LocalTime) :
        ContenidoRecordatorio

    data class DePedido(val nombreCliente: String, val fechaEntrega: LocalDate) : ContenidoRecordatorio
}
