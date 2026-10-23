package com.gestor_ventures.back.model

import java.time.LocalDateTime

data class Alerta(
    val id: Long,
    val tipo: TipoAlerta,
    val descripcion: String,
    val leida: Boolean,
    val fechaGeneracion: LocalDateTime,
)

enum class TipoAlerta { CaidaVentas, GastoElevado, MetaEnRiesgo }

data class AlertaNueva(val tipo: TipoAlerta, val descripcion: String)
