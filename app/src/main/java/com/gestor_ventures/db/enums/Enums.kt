package com.gestor_ventures.db.enums

/** HU-05. Adapta los módulos de agenda según el tipo de negocio. */
enum class TipoActividad { SERVICIOS, PRODUCTOS, MIXTO }

/** HU-06, HU-07. Periodicidad de gastos fijos y obligaciones. */
enum class Frecuencia { SEMANAL, QUINCENAL, MENSUAL, ANUAL }

/** HU-07. */
enum class EstadoPago { PENDIENTE, PAGADA }

/** HU-08. */
enum class EstadoMeta { ACTIVA, CUMPLIDA }

/** HU-15. Discriminador de a qué tipo de movimiento aplica una categoría. */
enum class TipoCategoria { GASTO, COSTO }

/** HU-11/HU-12. */
enum class TipoRegistroVenta { DETALLADO, RAPIDO }

/** HU-11. */
enum class MetodoPago { EFECTIVO, TARJETA, TRANSFERENCIA, OTRO }

/** HU-19 a HU-22. */
enum class EstadoCaja { ABIERTA, CERRADA }

/** HU-24, HU-28. */
enum class EstadoCita { PROGRAMADA, COMPLETADA, CANCELADA }

/** HU-25, HU-28. */
enum class EstadoPedido { PENDIENTE, EN_PROCESO, ENTREGADO, CANCELADO }

/** HU-32. */
enum class TipoRecordatorioCliente { CITA, PAGO_PENDIENTE }

/** HU-35. */
enum class EstadoSugerencia { PENDIENTE, GUARDADA, DESCARTADA }

/** HU-38. Disparadores explícitos definidos en la especificación. */
enum class TipoAlerta { CAIDA_VENTAS, GASTO_ELEVADO, META_EN_RIESGO }

/** HU-41. Origen de la notificación push. */
enum class TipoNotificacion { ALERTA_IA, RECORDATORIO_AGENDA }
