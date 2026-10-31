package com.gestor_ventures.back.model

import java.time.LocalDateTime

/**
 * HU-05. Negocio tal como lo entiende la app, sin nada de Room.
 *
 * `front/` trabaja con este modelo y nunca con [com.gestor_ventures.db.entity.NegocioEntity]:
 * el repositorio es el único que traduce entre los dos.
 */
data class Negocio(
    val id: Long,
    /** Quién lo creó: es su Líder. Cualquier otra persona con acceso es Vendedor. */
    val usuarioId: Long,
    val nombre: String,
    val tipoActividad: TipoActividad,
    val categoria: String,
    val porcentajeReinversion: Double,
    /** HU-05. Color de marca en hexadecimal; null = el color por defecto de la app. */
    val colorMarca: String?,
    val fechaCreacion: LocalDateTime,
    /** Celular de WhatsApp de la empresa, sin indicativo; null si todavía no se vinculó. */
    val whatsappEmpresa: String? = null,
)

/** HU-05. Qué vende el negocio; define qué módulos de agenda tienen sentido. */
enum class TipoActividad { SERVICIOS, PRODUCTOS, MIXTO }
