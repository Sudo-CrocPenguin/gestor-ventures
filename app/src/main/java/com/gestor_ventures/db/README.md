# Base de datos local — Gestor Ventures

Room/SQLite, 20 tablas cubriendo las Épicas 1–8 (HU-01 a HU-41). Nombres de tabla y
columna en español (snake_case); propiedades Kotlin en camelCase español.

## Aplicación de 1FN y 2FN

**1FN (valores atómicos, sin grupos repetitivos).** El único punto del backlog que la
viola tal como estaba especificado es `pedidos.productos` (HU-25): un pedido puede tener
varios productos, así que ese atributo multivaluado se separó en una tabla propia,
`detalle_pedidos` (`pedido_id` FK, `producto_servicio`, `cantidad`). El resto de columnas
del modelo ya son atómicas.

**2FN (sin dependencias parciales de una clave compuesta).** Las 20 tablas usan una
clave primaria sustituta de una sola columna (`*_id` autogenerado). Con una PK simple no
puede existir dependencia parcial —esa solo se da cuando la clave es compuesta—, así que
2FN se cumple por construcción en todo el esquema; no hay tablas con clave compuesta que
revisar.

## Decisiones para evitar redundancia

- **No existe tabla `movimientos_caja`.** El saldo esperado (HU-20) y la diferencia al
  cierre (HU-22) se calculan en consulta a partir de `ventas`, `costos` y `gastos` del
  mismo negocio dentro de la ventana de la caja abierta, en vez de duplicar cada
  transacción como un "movimiento" aparte.
- **No existe tabla `conversaciones_ia`.** `consultas_ia` tiene una columna `sesion_id`
  que agrupa los turnos de una misma conversación; el historial de HU-39 es un
  `GROUP BY sesion_id` sobre `consultas_ia`, no una entidad nueva.
- **Campos/variables por tabla no se repiten entre historias relacionadas** (ver
  comentarios `HU-XX` en cada entidad Kotlin para la trazabilidad).

## Tablas por épica

| Épica | Tablas |
|---|---|
| 1 — Autenticación y perfil | `usuarios` |
| 2 — Configuración del negocio | `negocios`, `gastos_fijos`, `obligaciones`, `metas_ahorro`, `categorias` |
| 3 — Gestor financiero | `ventas`, `costos`, `gastos` |
| 4 — Apertura y cierre de caja | `cajas` |
| 5 — Agenda | `citas`, `pedidos`, `detalle_pedidos`, `recordatorios` |
| 6 — Clientes | `clientes`, `recordatorios_clientes` |
| 7 — Asistente de IA | `consultas_ia`, `sugerencias`, `alertas` |
| 8 — Reportes y notificaciones | `notificaciones` |

## Relaciones (FK → tabla padre, acción ON DELETE)

- `negocios.usuario_id` → `usuarios` — CASCADE
- `gastos_fijos.negocio_id`, `obligaciones.negocio_id`, `metas_ahorro.negocio_id`,
  `categorias.negocio_id`, `ventas.negocio_id`, `costos.negocio_id`, `gastos.negocio_id`,
  `cajas.negocio_id`, `citas.negocio_id`, `pedidos.negocio_id`, `clientes.negocio_id`,
  `consultas_ia.negocio_id`, `sugerencias.negocio_id`, `alertas.negocio_id` → `negocios`
  — CASCADE
- `ventas.cliente_id` → `clientes` — SET_NULL (cliente opcional en venta; se conserva el
  histórico de ventas aunque se borre el cliente)
- `costos.categoria_id`, `gastos.categoria_id` → `categorias` — SET_NULL
- `citas.cliente_id`, `pedidos.cliente_id` → `clientes` — CASCADE (cliente obligatorio)
- `detalle_pedidos.pedido_id` → `pedidos` — CASCADE
- `recordatorios.cita_id` → `citas`, `recordatorios.pedido_id` → `pedidos` — CASCADE
  (exactamente uno de los dos debe ser no nulo; regla validada en el repositorio)
- `recordatorios_clientes.cliente_id` → `clientes` — CASCADE
- `notificaciones.usuario_id` → `usuarios` — CASCADE

## Notas técnicas

- **Fechas y enums**: SQLite no tiene tipos nativos para ninguno de los dos, así que
  `Converters.kt` los serializa como epoch millis/epoch day (Long) y como nombre del
  enum (String) respectivamente.
- **java.time**: requiere `coreLibraryDesugaringEnabled = true` en el `build.gradle` del
  módulo si el `minSdk` es menor a 26.
- **Restricciones numéricas** (montos positivos, porcentaje 0–100, longitudes máximas de
  texto) no se aplican a nivel de columna porque Room no soporta `CHECK` vía anotaciones;
  se validan en la capa de repositorio/casos de uso, como ya lo definieron los issues de
  GitHub en "Especificaciones Técnicas".
- **Seguridad de `usuarios`**: incluye `contrasena_hash`, `token_sesion` y
  `codigo_recuperacion` porque así quedaron especificados en HU-01/02/03. En producción
  es preferible delegar la autenticación a un backend y guardar solo el token en
  almacenamiento cifrado (DataStore/EncryptedSharedPreferences), no en Room.
- **DAOs**: no incluidos en esta entrega — son el siguiente paso natural sobre estas
  entidades.

## Estructura de archivos

```
com/gestorventures/data/local/
├── enums/Enums.kt
├── entity/
│   ├── UsuarioEntity.kt
│   ├── NegocioEntity.kt        (Negocio, GastoFijo, Obligacion, MetaAhorro, Categoria)
│   ├── FinanzasEntity.kt       (Venta, Costo, Gasto)
│   ├── CajaEntity.kt
│   ├── AgendaEntity.kt         (Cita, Pedido, DetallePedido, Recordatorio)
│   ├── ClienteEntity.kt        (Cliente, RecordatorioCliente)
│   ├── AsistenteEntity.kt      (ConsultaIA, Sugerencia, Alerta)
│   └── NotificacionEntity.kt
├── Converters.kt
└── GestorVenturesDatabase.kt
```

Ajusta el paquete `com.gestorventures` al `applicationId` real del proyecto antes de
copiar los archivos a `src/main/java/`.


hello word