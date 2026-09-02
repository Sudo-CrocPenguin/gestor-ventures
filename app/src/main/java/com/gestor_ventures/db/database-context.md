# Database Context — Gestor Ventures

Diagrama de las 20 tablas y el flujo de datos entre ellas, en ASCII puro (bloques de
código con fuente monoespaciada). Se ve igual abierto en VS Code, Android Studio,
terminal o GitHub — no depende de ningún plugin de Mermaid.

---

## 1. Mapa de relaciones (jerarquía por FK)

```text
USUARIOS  (Épica 1)
  │
  │ 1:N
  ▼
NEGOCIOS  (Épica 2)
  │
  ├──1:N──▶ GASTOS_FIJOS                              (Épica 2)
  ├──1:N──▶ OBLIGACIONES                               (Épica 2)
  ├──1:N──▶ METAS_AHORRO                                (Épica 2)
  │
  ├──1:N──▶ CATEGORIAS                                   (Épica 2, referenciada en 3)
  │            ├──1:N──▶ COSTOS   ◀── también FK negocio_id   (Épica 3)
  │            └──1:N──▶ GASTOS   ◀── también FK negocio_id   (Épica 3)
  │
  ├──1:N──▶ VENTAS  ◀───────────────0:N── CLIENTES (opcional)  (Épica 3)
  │
  ├──1:N──▶ CAJAS                                          (Épica 4)
  │
  ├──1:N──▶ CLIENTES                                        (Épica 6)
  │            ├──1:N──▶ CITAS                                (Épica 5)
  │            │            └──1:N──▶ RECORDATORIOS            (Épica 5)
  │            ├──1:N──▶ PEDIDOS                                (Épica 5)
  │            │            ├──1:N──▶ DETALLE_PEDIDOS            (Épica 5)
  │            │            └──1:N──▶ RECORDATORIOS (comparte)    (Épica 5)
  │            └──1:N──▶ RECORDATORIOS_CLIENTES                    (Épica 6)
  │
  ├──1:N──▶ CONSULTAS_IA                                              (Épica 7)
  ├──1:N──▶ SUGERENCIAS                                                (Épica 7)
  └──1:N──▶ ALERTAS ──1:N──▶ NOTIFICACIONES (tipo=ALERTA_IA)             (Épica 7/8)

USUARIOS ──1:N──▶ NOTIFICACIONES (tipo=RECORDATORIO_AGENDA también cae acá)  (Épica 8)
```

**Cómo leer las flechas:** `A ──1:N──▶ B` significa que `B` tiene una columna FK que
apunta a `A` (un `A` puede tener muchos `B`). Las tablas indentadas debajo de otra son
sus hijas directas por FK.

---

## 2. Detalle de cada tabla

```text
┌────────────────────────────────────────────┐
│                  USUARIOS                  │
│         (Épica 1 — Autenticación)          │
├────────────────────────────────────────────┤
│ usuario_id        PK                       │
│ nombre, correo, contrasena_hash            │
│ foto_perfil_url, telefono                  │
│ fecha_creacion, fecha_ultimo_acceso        │
│ token_sesion, codigo_recuperacion          │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                  NEGOCIOS                  │
│        (Épica 2 — Config. negocio)         │
├────────────────────────────────────────────┤
│ negocio_id         PK                      │
│ usuario_id         FK -> USUARIOS          │
│ nombre_negocio, tipo_actividad             │
│ categoria_negocio, porcentaje_reinversion  │
│ fecha_creacion, fecha_actualizacion        │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                GASTOS_FIJOS                │
│                 (Épica 2)                  │
├────────────────────────────────────────────┤
│ gasto_fijo_id      PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ nombre_gasto, monto, frecuencia            │
│ fecha_registro                             │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                OBLIGACIONES                │
│                 (Épica 2)                  │
├────────────────────────────────────────────┤
│ obligacion_id      PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ nombre_obligacion, monto                   │
│ fecha_vencimiento, estado_pago             │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                METAS_AHORRO                │
│                 (Épica 2)                  │
├────────────────────────────────────────────┤
│ meta_ahorro_id     PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ monto_objetivo, fecha_limite               │
│ estado_meta                                │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                 CATEGORIAS                 │
│           (Épica 2, usada en 3)            │
├────────────────────────────────────────────┤
│ categoria_id       PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ nombre_categoria, tipo_categoria           │
│ (GASTO | COSTO)                            │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                   VENTAS                   │
│           (Épica 3 — Financiero)           │
├────────────────────────────────────────────┤
│ venta_id           PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ cliente_id         FK -> CLIENTES (opc.)   │
│ tipo_registro, producto_servicio           │
│ monto, metodo_pago, fecha_hora             │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                   COSTOS                   │
│                 (Épica 3)                  │
├────────────────────────────────────────────┤
│ costo_id           PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ categoria_id       FK -> CATEGORIAS (opc.) │
│ producto_servicio, monto_costo             │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                   GASTOS                   │
│                 (Épica 3)                  │
├────────────────────────────────────────────┤
│ gasto_id           PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ categoria_id       FK -> CATEGORIAS (opc.) │
│ descripcion, monto, fecha                  │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                   CAJAS                    │
│              (Épica 4 — Caja)              │
├────────────────────────────────────────────┤
│ caja_id            PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ monto_inicial, monto_real_cierre           │
│ fecha_hora_apertura, fecha_hora_cierre     │
│ estado_caja, nota_diferencia               │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                   CITAS                    │
│             (Épica 5 — Agenda)             │
├────────────────────────────────────────────┤
│ cita_id            PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ cliente_id         FK -> CLIENTES          │
│ fecha, hora, descripcion, estado_cita      │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                  PEDIDOS                   │
│                 (Épica 5)                  │
├────────────────────────────────────────────┤
│ pedido_id          PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ cliente_id         FK -> CLIENTES          │
│ fecha_entrega, estado_pedido               │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│              DETALLE_PEDIDOS               │
│               (Épica 5, 1FN)               │
├────────────────────────────────────────────┤
│ detalle_pedido_id  PK                      │
│ pedido_id          FK -> PEDIDOS           │
│ producto_servicio, cantidad                │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│               RECORDATORIOS                │
│                 (Épica 5)                  │
├────────────────────────────────────────────┤
│ recordatorio_id    PK                      │
│ cita_id            FK -> CITAS   (opc.)    │
│ pedido_id          FK -> PEDIDOS (opc.)    │
│ tiempo_anticipacion, enviado               │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                  CLIENTES                  │
│            (Épica 6 — Clientes)            │
├────────────────────────────────────────────┤
│ cliente_id         PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ nombre, telefono, correo, notas            │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│           RECORDATORIOS_CLIENTES           │
│                 (Épica 6)                  │
├────────────────────────────────────────────┤
│ recordatorio_cliente_id PK                 │
│ cliente_id         FK -> CLIENTES          │
│ tipo_recordatorio, mensaje, fecha_envio    │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                CONSULTAS_IA                │
│          (Épica 7 — Asistente IA)          │
├────────────────────────────────────────────┤
│ consulta_id        PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ sesion_id, pregunta, respuesta             │
│ fecha_hora                                 │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                SUGERENCIAS                 │
│                 (Épica 7)                  │
├────────────────────────────────────────────┤
│ sugerencia_id      PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ tipo_sugerencia, contenido                 │
│ estado_sugerencia, fecha_generacion        │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                  ALERTAS                   │
│                 (Épica 7)                  │
├────────────────────────────────────────────┤
│ alerta_id          PK                      │
│ negocio_id         FK -> NEGOCIOS          │
│ tipo_alerta, descripcion                   │
│ leida, fecha_generacion                    │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│               NOTIFICACIONES               │
│            (Épica 8 — Reportes)            │
├────────────────────────────────────────────┤
│ notificacion_id    PK                      │
│ usuario_id         FK -> USUARIOS          │
│ tipo_notificacion, referencia_id           │
│ contenido, leida, fecha_hora_envio         │
└────────────────────────────────────────────┘
```

---

## 3. Flujos de datos por operación

### 3.1 Registrar una venta (HU-11/12)

```text
                ┌─────────────────────────┐
                │  Usuario registra venta   │
                └────────────┬──────────────┘
                              │
                  ┌───────────┴────────────┐
                  ▼                        ▼
            DETALLADO                   RÁPIDO
       producto, monto,              monto total
       cliente, método pago            del día
                  │                        │
                  └───────────┬────────────┘
                               ▼
                        ┌────────────┐
                        │  [ventas]   │
                        └──────┬──────┘
                               │
              ┌────────────────┼────────────────────┐
              ▼                ▼                     ▼
     Resumen financiero  Saldo esperado      Historial del cliente
          (HU-16)         de caja (HU-20)         (HU-30, si aplica)
```

### 3.2 Apertura y cierre de caja (HU-19 a HU-22)

```text
        ┌──────────────────────────┐
        │  Abrir caja (monto_inicial) │
        └────────────┬────────────────┘
                      ▼
           [cajas] estado = ABIERTA
                      │
                      │   durante la jornada:
                      ├──▶ [ventas]
                      ├──▶ [costos]
                      └──▶ [gastos]
                      │
                      ▼
     Saldo esperado = monto_inicial
       + Σ ventas − Σ costos − Σ gastos
     (se calcula en consulta, NO se guarda)
                      │
                      ▼
       Cerrar caja (monto_real_cierre)
                      │
                      ▼
    diferencia = monto_real_cierre − saldo_esperado
                      │
                      ▼
     [cajas] estado = CERRADA, nota_diferencia
```

### 3.3 Resumen financiero (HU-16) y derivados

```text
  [ventas] ──┐
  [costos] ──┼──▶ Resumen financiero (HU-16)
  [gastos] ──┤            │
[gastos_fijos]┤           ├──▶ ingresos / gastos / costos totales
[obligaciones]┘           ├──▶ ganancia_estimada
                           └──▶ dinero_disponible
                                    │
                     ┌──────────────┴───────────────┐
                     ▼                               ▼
          Monto a reinvertir                Progreso de meta de ahorro
             (HU-09/36)                             (HU-08)
     = % reinversión × ganancia_estimada     compara con [metas_ahorro]
```

### 3.4 Consulta al asistente de IA (HU-34/35/37/39)

```text
              Usuario pregunta
                     │
                     ▼
  Repository arma contexto financiero
  (resumen del negocio + ventas recientes
   + gastos por categoría)
                     │
                     ▼
        network/  ──▶  API del LLM
                     │
                     ▼
             Respuesta generada
                     │
                     ▼
   [consultas_ia] (pregunta, respuesta, sesion_id)
                     │
                     ▼
    Historial de conversación (HU-39)
         GROUP BY sesion_id
```

### 3.5 Historial de cliente (HU-30)

```text
       [clientes] ──┐
[ventas.cliente_id] ─┤
 [citas.cliente_id] ─┼──▶ Historial de cliente (HU-30)
[pedidos.cliente_id] ┘            │
                                   ▼
                     listado ordenado por fecha
                       + total acumulado de compras
```

### 3.6 Generación de alertas (HU-38) y notificaciones (HU-41)

```text
      [ventas] ──┐
[gastos + costos]─┼──▶ Evaluación periódica (GenerarAlertas)
 [metas_ahorro] ──┘             │
              ┌──────────────────┼───────────────────┐
              ▼                  ▼                     ▼
      caída de ventas     gasto elevado          meta en riesgo
              │                  │                     │
              └──────────────────┴─────────────────────┘
                                  ▼
                     [alertas] (tipo correspondiente)
                                  │
                                  ▼
                [notificaciones] tipo = ALERTA_IA

recordatorios de agenda (HU-26) ──▶ [notificaciones] tipo = RECORDATORIO_AGENDA
```

---

## 4. Notas de lectura

- `[tabla]` = lectura/escritura sobre esa tabla de Room.
- Los recuadros sin corchetes son cálculos hechos en `back/usecase/`, no tablas.
- El saldo esperado de caja (3.2) y el resumen financiero (3.3) **no se persisten**:
  se recalculan en cada consulta a partir de `ventas`, `costos` y `gastos` — evita una
  tabla derivada redundante (ver `README.md` de `db/`).
- El único flujo que sale del dispositivo es 3.4 (asistente de IA); el resto es
  enteramente local.
