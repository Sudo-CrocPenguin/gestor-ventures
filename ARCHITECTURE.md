# Arquitectura — Gestor Ventures

Documento de referencia sobre la arquitectura del proyecto: qué se eligió, por qué, y
cómo se organiza el código. Aplica a partir de este punto del desarrollo; se actualiza
si la arquitectura cambia.

---

## 1. Contexto

- **App móvil Android** (Kotlin + Jetpack Compose), proyecto en parejas, 4to semestre.
- **41 historias de usuario** en 8 épicas (auth, config. de negocio, gestor financiero,
  caja, agenda, clientes, asistente de IA, reportes/notificaciones).
- **Persistencia local**: Room/SQLite, 20 tablas ya modeladas (1FN/2FN).
- **Un componente remoto real**: el asistente de IA (llamada a un LLM vía API). El resto
  de la app funciona con datos 100% locales.
- **Equipo de 2 personas**, trabajando en paralelo.

Estos cinco hechos son los que determinan la arquitectura elegida más abajo.

---

## 2. Arquitectura elegida

**MVVM + Repository, con el código organizado en tres carpetas principales por capa
técnica: `front/`, `back/`, `db/`.**

```
front  →  back  →  db
              ↘  network
```

Una capa solo puede llamar hacia la capa inmediatamente inferior. `front/` nunca importa
nada de `db/` directamente, ni `back/` expone detalles de Room hacia arriba.

No es Clean Architecture en su forma estricta (no hay una capa `domain/` con un caso de
uso por cada acción), y no es arquitectura multi-módulo. Es MVVM clásico con Repository
como frontera entre datos locales y remotos, organizado por capa en vez de por feature.

---

## 3. Por qué esta arquitectura (y no otra)

### 3.1 Por qué MVVM + Repository y no Clean Architecture estricta

Clean Architecture (con `domain/` conteniendo un `UseCase` por cada acción: crear
cliente, registrar venta, editar cita, etc.) tiene sentido cuando hay lógica de negocio
compleja en cada operación o cuando el proyecto va a vivir años con equipos rotando.

Acá, la mayoría de las 41 HU son CRUD directo sobre una tabla de Room: crear un cliente,
registrar un gasto, agendar una cita. Envolver cada una en un `UseCase` que solo le pasa
los datos al `Repository` es ceremonia sin beneficio — más archivos, más tiempo de
desarrollo, sin ganar testabilidad real porque no hay nada que testear ahí aparte del
paso de datos.

**Decisión intermedia**: se usa una capa de casos de uso *solo* donde hay cálculo o
lógica real (ver sección 5.2). Todo lo demás es `ViewModel → Repository → DAO` directo.

### 3.2 Por qué separar por capa (`front/back/db`) y no por feature

La alternativa —organizar por feature (`auth/`, `finanzas/`, `agenda/`, cada una con su
propia UI + ViewModel + Repository + DAO)— es más común en apps grandes porque aísla
mejor el trabajo en equipos de muchas personas y facilita modularizar más adelante.

Con 2 personas y un proyecto de semestre, separar por capa técnica es más simple de
razonar y de calificar: cada carpeta principal tiene una responsabilidad clara y se
puede repartir el trabajo por capa si conviene (uno avanza pantallas mientras el otro
cierra Repository/DAO de la misma HU), sin la sobrecarga de mantener límites entre
módulos Gradle. Es la organización correcta para este tamaño de proyecto; si crece más
allá del curso, migrar a estructura por feature (o a módulos Gradle) es un refactor
mecánico de mover archivos, no un cambio de arquitectura.

### 3.3 Por qué Repository (y no que la UI hable directo con Room/Retrofit)

El asistente de IA (HU-34/35/37) es el único punto de la app que necesita red; todo lo
demás es local. El `Repository` es lo que le esconde esa diferencia al `ViewModel`: la
UI pide "el resumen financiero" o "la respuesta del asistente" sin saber si el dato
viene de SQLite o de una llamada HTTP. Esto también es lo que permite, más adelante,
agregar sincronización con un backend propio sin tocar una sola pantalla.

### 3.4 Por qué no multi-módulo Gradle

Separar en módulos (`:app`, `:core-db`, `:feature-finanzas`, etc.) trae beneficios reales
de tiempos de build y aislamiento en apps grandes, pero para 41 HU y 2 personas el costo
de configurar y mantener los `build.gradle` de cada módulo supera el beneficio. Se queda
en un solo módulo con paquetes bien separados; la estructura de carpetas ya deja el
camino trazado para modularizar después si hace falta.

---

## 4. Estructura de carpetas

```
com.gestorventures/
│
├── front/                          ← Presentación (Compose + ViewModels)
│   ├── ui/
│   │   ├── auth/                     HU-01 a HU-04
│   │   ├── negocio/                  HU-05 a HU-10
│   │   ├── finanzas/                 HU-11 a HU-18
│   │   ├── caja/                     HU-19 a HU-23
│   │   ├── agenda/                   HU-24 a HU-28
│   │   ├── clientes/                 HU-29 a HU-33
│   │   ├── asistente/                HU-34 a HU-39
│   │   └── notificaciones/           HU-40, HU-41
│   ├── navigation/                 NavGraph, rutas
│   └── theme/                      Color.kt, Type.kt, Theme.kt
│
├── back/                           ← Lógica de negocio y orquestación
│   ├── repository/                 Un repository por dominio
│   ├── usecase/                    Solo donde hay cálculo/lógica real (§5.2)
│   ├── network/                    Retrofit: ApiService + DTOs (asistente de IA)
│   └── di/                         Módulos Hilt
│
├── db/                             ← Persistencia local (Room)
│   ├── entity/                     20 entidades (ya definidas)
│   ├── enums/                      Enums de dominio (ya definidos)
│   ├── dao/                        Un DAO por entidad (pendiente)
│   ├── Converters.kt
│   └── GestorVenturesDatabase.kt
│
└── MainActivity.kt
```

Cada subcarpeta de `front/ui/` trae su(s) pantalla(s) Compose y su `XxxViewModel.kt`.
Cada dominio en `back/repository/` corresponde a un grupo de tablas relacionadas
(`NegocioRepository`, `VentaRepository`, `CajaRepository`, `ClienteRepository`,
`AsistenteRepository`, etc.).

---

## 5. Responsabilidades por capa

### 5.1 `front/` — Presentación

- Composables: solo describen la UI a partir de un `UiState`, sin lógica de negocio.
- `ViewModel`: dueño del estado de pantalla (`StateFlow<UiState>`), expone eventos que
  la UI dispara (`onClick`, `onSubmit`) y llama a `back/repository/`.
- Nunca importa nada de `db/` ni de Retrofit directamente — solo conoce `back/`.
- Flujo unidireccional: evento de UI → ViewModel → Repository → nuevo estado → UI se
  recompone.

### 5.2 `back/` — Lógica de negocio y orquestación

- `repository/`: única fuente de verdad de cada dominio. Decide si el dato sale de
  `db/` (DAO), de `network/` (Retrofit), o de ambos combinados. El `ViewModel` nunca lo
  sabe.
- `usecase/`: **solo** para lógica real, no para envolver CRUD. Ejemplos concretos de
  este proyecto:
  - `CalcularResumenFinanciero` (HU-16): ingresos − gastos − costos, incluye gastos
    fijos (HU-06) y obligaciones (HU-07) en el cálculo de dinero disponible.
  - `CalcularDiferenciaCaja` (HU-22): monto esperado (derivado de ventas/gastos/costos
    de la jornada) vs. monto real de cierre.
  - `CalcularMontoReinversion` (HU-09 / HU-36): porcentaje configurado × ganancia
    estimada.
  - `GenerarAlertas` (HU-38): evalúa las tres condiciones — caída de ventas, gasto
    elevado, meta de ahorro en riesgo — y crea las alertas correspondientes.
  - `ConsultarAsistente` (HU-34 / HU-35 / HU-37): arma el contexto financiero del
    negocio y lo envía a `network/` para obtener la respuesta del asistente.

  Todo lo que es alta/consulta simple (crear cliente, registrar venta, agendar cita,
  marcar una notificación como leída) va `ViewModel → Repository → DAO`, sin usecase de
  por medio.
- `network/`: cliente Retrofit, interfaces de servicio y DTOs para la comunicación con
  el proveedor del asistente de IA (y con un backend propio si se agrega más adelante).
- `di/`: módulos Hilt (`DatabaseModule`, `NetworkModule`, `RepositoryModule`) que
  proveen las instancias de `db/` y `network/` a `back/`, y de `back/` a `front/`.

### 5.3 `db/` — Persistencia

- `entity/`, `enums/`, `Converters.kt`, `GestorVenturesDatabase.kt`: ya definidos (20
  tablas, 1FN/2FN, ver entrega anterior).
- `dao/`: pendiente — un DAO por entidad con las queries que cada `Repository`
  necesita (`@Insert`, `@Update`, `@Delete`, `@Query` con `Flow<T>` para observar
  cambios reactivamente desde el `ViewModel`).
- No importa nada de `back/` ni de `front/`. Solo expone entidades y DAOs hacia arriba.

---

## 6. Manejo de datos locales vs. remotos

La app es **local-first**: casi todo el dominio (negocio, ventas, gastos, caja, agenda,
clientes) vive y se consulta desde Room sin depender de red. El único flujo que
necesita conectividad es el asistente de IA.

```
ViewModel (asistente)
      │
      ▼
AsistenteRepository
      │
      ├── back/network/  → arma el contexto del negocio (vía CalcularResumenFinanciero,
      │                     datos de ventas/gastos recientes) y llama al LLM
      │
      └── db/dao/ConsultaIaDao → guarda pregunta + respuesta en `consultas_ia`
                                  (agrupadas por sesion_id, ver HU-39)
```

Si el proyecto llegara a tener un backend propio (por ejemplo para sincronizar entre
dispositivos), el patrón se extiende igual: el `Repository` sigue siendo el único que
decide entre caché local y red, sin que `front/` se entere del cambio.

---

## 7. Inyección de dependencias (Hilt)

- `db/`: expone `GestorVenturesDatabase` y cada DAO vía `@Provides` en
  `back/di/DatabaseModule.kt`.
- `network/`: expone `Retrofit` y los `ApiService` vía `back/di/NetworkModule.kt`.
- `repository/`: cada `Repository` se anota `@Inject constructor(...)` recibiendo sus
  DAO y/o `ApiService` necesarios; Hilt los resuelve automáticamente.
- `ViewModel`: `@HiltViewModel` + `@Inject constructor(...)` recibiendo el/los
  `Repository` que necesita.

---

## 8. Convenciones

- **Idioma del código**: nombres de clase/paquete en inglés técnico estándar de Android
  (`Entity`, `Repository`, `ViewModel`, `UseCase`); nombres de dominio (propiedades,
  tablas, columnas, variables de negocio) en español, como ya se definió en `db/`.
- **Commits**: Conventional Commits en español, siguiendo GitFlow (ya establecido en el
  repo).
- **Estado de UI**: `StateFlow` + `data class UiState`, flujo unidireccional (UDF), sin
  `LiveData`.
- **Queries reactivas**: los DAO devuelven `Flow<T>` para listas/consultas que la UI
  debe observar en tiempo real (ej. lista de ventas del día, saldo de caja).

---

## 9. Próximos pasos

1. `db/dao/` — un DAO por entidad.
2. `back/repository/` — un Repository por dominio, envolviendo los DAO.
3. `back/network/` — integración con el proveedor del asistente de IA.
4. `back/usecase/` — los 5 casos de uso listados en §5.2.
5. `front/` — pantallas Compose + ViewModels, épica por épica, siguiendo el orden de
   prioridad MoSCoW ya definido en el backlog.
