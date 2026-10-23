package com.gestor_ventures.back.model

import java.time.LocalDateTime

/** HU-38. Un aviso sobre algo del negocio que conviene atender pronto. */
data class Alerta(
    val id: Long,
    val tipo: TipoAlerta,
    val descripcion: String,
    val leida: Boolean,
    val fechaGeneracion: LocalDateTime,
)

/** Las tres situaciones que pide HU-38. */
enum class TipoAlerta { CaidaVentas, GastoElevado, MetaEnRiesgo }

/** Una alerta recién calculada, antes de guardarla. */
data class AlertaNueva(val tipo: TipoAlerta, val descripcion: String)
