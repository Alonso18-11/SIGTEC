# Plan 011 — Solicitud de repuestos a Logística (HU-11)

> Basado en `spec.md` (aprobada el 2026-11-07, con D-1 a D-10 respondidas) y `docs/constitution.md`.
> El código vive en `feature/atencion` (D-10). Toca archivos compartidos: el modelo `SolicitudRepuesto` y su entidad
> (sube `version`), `firebase/firestore.rules` (D-8), `core/sync` y `MEMORY.md`. Se entrega en PR de un solo tema,
> con la HU al final del título y la aprobación de la otra persona del equipo antes de fusionarse.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas).

## 0. Hallazgos del código real

- **El modelo existe desde el esqueleto.** `core/domain/model/SolicitudRepuesto.kt` (con `EstadoEntrega`
  `POR_DESPACHAR`, `EN_CAMINO`, `ENTREGADO`), `SolicitudRepuestoEntity` (tabla `solicitudes_repuesto`) y
  `SolicitudRepuestoDao` (`observarTodas`, `observarPorIncidencia`, `sinEntregar`, `guardar`,
  `pendientesDeSincronizar`). Faltan el número de Logística, la disponibilidad consultada, las fechas de despacho y
  entrega, quién confirmó, el rechazo y `actualizadoEn` (convención de Room). **Sube `version` de 3 a 4** (PR-01).
- **La marca y los movimientos existen.** `MarcaSeguimiento.ESPERANDO_REPUESTO`, `TipoMovimiento.REPUESTO_SOLICITADO`
  y `REPUESTO_RECIBIDO`. La reasignación ya conserva «Esperando repuesto» (RF-625) y el cierre ya se permite desde ella
  (RF-812).
- **Las rutas existen.** `Ruta.SolicitarRepuesto(incidenciaId)` y `Ruta.EstadoRepuestos`, registradas en
  `AtencionNavigation` con `PantallaEnConstruccion`. **No se toca `Rutas.kt`.**
- **El detalle ya tiene el botón.** `BarraAcciones` muestra «Solicitar repuesto» con `puedeAtender` (técnico asignado
  y pendiente), que es la condición de D-5; `IncidenciasNavigation` ya navega a `Ruta.SolicitarRepuesto(id)`.
- **«Mis tareas» no tiene «Repuestos».** Se agrega el botón del prototipo 7 en su encabezado (PR-11).
- **Retrofit está declarado y sin usar.** `retrofit` 3.0.0 y `converter-kotlinx-serialization` en
  `libs.versions.toml` y en `app/build.gradle.kts`; Retrofit trae OkHttp como dependencia. **Sin dependencias
  nuevas.** Permiso `INTERNET`: lo agrega el manifest de Firebase; se verifica en el manifest combinado (PR-02).
- **Patrón del informe técnico.** `CerrarIncidenciaConInforme` y `AtencionRepositoryImpl.cerrarConInforme` guardan
  incidencia, movimiento e informe en un `withTransaction`; `SincronizadorIncidencias.subir(informe)` lo sube a
  Firestore cuando su incidencia ya está en el servidor. La solicitud sigue el mismo camino.
- **WorkManager.** `ProgramadorSincronizacion.subirCambios()` encola la subida a Firestore; el registro en Logística
  es otra tarea, con su propio programador (PR-07).

## 1. División en PR, dependencias y orden de fusión

| PR | Rama | Título | Base | Trae | RF |
|---|---|---|---|---|---|
| PR-01 | `feat/hu11-modelo-solicitud` | `feat(core): la solicitud de repuesto guarda Logística, disponibilidad y fechas (HU-11)` | `main` | — | RF-1113, RF-1117, RF-1119, RF-1122 |
| PR-02 | `feat/hu11-api-logistica` | `feat(atencion): API de Logística con Retrofit detrás de un repositorio (HU-11)` | PR-01 | — | RF-1104 a RF-1108, RF-1117, RF-1123 |
| PR-03 | `feat/hu11-reglas-repuesto` | `feat(atencion): reglas de quién solicita, la disponibilidad y los días de espera (HU-11)` | PR-01 | — | RF-1102, RF-1105, RF-1107, RF-1108, RF-1122, RF-1124 |
| PR-04 | `feat/hu11-solicitar-repuesto` | `feat(atencion): SolicitarRepuesto guarda la solicitud, el movimiento y la marca (HU-11)` | PR-03 | — | RF-1102, RF-1103, RF-1110, RF-1113, RF-1115 |
| PR-05 | `feat/hu11-solicitud-viewmodel` | `feat(atencion): SolicitudRepuestoViewModel busca la pieza y envía con huella (HU-11)` | PR-04 | PR-02 | RF-1104 a RF-1116 |
| PR-06 | `feat/hu11-solicitud-pantalla` | `feat(atencion): pantalla «Solicitar repuesto» en la navegación (HU-11)` | PR-05 | — | RF-1101, RF-1104 a RF-1112, RF-1114 |
| PR-07 | `feat/hu11-registrar-logistica` | `feat(atencion): registrar las solicitudes en Logística en segundo plano (HU-11)` | PR-04 | PR-02 | RF-1117, RF-1119 |
| PR-08 | `feat/hu11-sync-solicitudes` | `feat(core): subir y bajar las solicitudes de repuesto de Firestore (HU-11)` | PR-01 | — | RF-1118, RF-1120 |
| PR-09 | `feat/hu11-detalle-repuestos` | `feat(incidencias): sección «Repuestos» en el detalle (HU-11)` | PR-01 | — | RF-1120 |
| PR-10 | `feat/hu11-actualizar-estados` | `feat(atencion): actualizar el estado de las solicitudes y volver a «En atención» (HU-11)` | PR-04 | PR-02 | RF-1123, RF-1124 |
| PR-11 | `feat/hu11-estado-repuestos` | `feat(atencion): pantalla «Estado de repuestos» y su acceso desde «Mis tareas» (HU-11)` | PR-10 | — | RF-1121 a RF-1123 |
| PR-12 | `docs/hu11-cierre` | `docs(atencion): cierre de la HU-11 con las pruebas manuales (HU-11)` | el último fusionado | todos | RF-1101 a RF-1124 |

**Orden.** PR-01 primero (modelo). Después, en paralelo: la API (PR-02), las reglas (PR-03), la sincronización
(PR-08) y el detalle (PR-09). La solicitud sigue PR-03 → PR-04 → PR-05 → PR-06; el registro en Logística (PR-07) y
los estados (PR-10 → PR-11) salen de PR-04 y traen PR-02.

**Antes de PR-02:** el usuario crea la cuenta gratuita en MockAPI.io y el proyecto con los recursos de la sección 5.1,
y pasa la dirección base. El asistente no crea cuentas.

## 2. Archivos y commits por PR

Leyenda: 🆕 nuevo, ✏️ modificado, ⚠️ compartido de `CLAUDE.md`, 🔶 sensible fuera de esa lista.

| PR | Archivos | Commits |
|---|---|---|
| PR-01 | ✏️⚠️ `core/domain/model/SolicitudRepuesto.kt` (3.1); ✏️⚠️ `SolicitudRepuestoEntity.kt`; ✏️⚠️ `SolicitudRepuestoDao.kt` (`observarDeTecnico`, `sinNumeroDeLogistica`, `sinEntregarDe`, `marcarRegistrada`, `marcarRechazada`); ✏️⚠️ `SigtecDatabase.kt` (`version = 4`); 🆕 (androidTest) `SolicitudRepuestoDaoTest.kt` | 1. `feat(core): campos de Logística y de entrega en la solicitud de repuesto` · 2. `feat(core): consultas de la solicitud de repuesto` · 3. `test(core): SolicitudRepuestoDao` |
| PR-02 | 🆕 `feature/atencion/data/LogisticaApi.kt` (interfaz Retrofit y DTOs, 5.1); 🆕 `feature/atencion/data/LogisticaRepositoryImpl.kt` (4.4); 🆕 `feature/atencion/domain/LogisticaRepository.kt`; 🆕 `feature/atencion/di/LogisticaModule.kt` (Retrofit, `OkHttpClient` con 10 s, P-2); 🆕 `feature/atencion/data/MapeoLogistica.kt` (3.3); 🆕 (test) `MapeoLogisticaTest.kt`, `feature/atencion/data/LogisticaRepositoryFalso.kt` | 1. `feat(atencion): contrato de Logística` · 2. `feat(atencion): API de Logística con Retrofit` · 3. `feat(atencion): traducir las respuestas y errores de Logística` · 4. `test(atencion): mapeo de Logística` · 5. `test(atencion): Logística falsa` |
| PR-03 | 🆕 `feature/atencion/domain/ReglasRepuesto.kt` (3.2); 🆕 (test) `ReglasRepuestoTest.kt` | 1. `feat(atencion): reglas de la solicitud de repuesto` · 2. `test(atencion): reglas de la solicitud de repuesto` |
| PR-04 | 🆕 `feature/atencion/domain/SolicitarRepuesto.kt` (4.1); ✏️ `AtencionRepository.kt` y ✏️ `AtencionRepositoryImpl.kt` (`solicitarRepuesto`, en `withTransaction`); ✏️ (test) `AtencionRepositoryFalso.kt`; 🆕 (test) `SolicitarRepuestoTest.kt` | 1. `feat(atencion): el repositorio guarda la solicitud con su movimiento y la marca` · 2. `feat(atencion): caso de uso SolicitarRepuesto` · 3. `test(atencion): SolicitarRepuesto` |
| PR-05 | 🆕 `feature/atencion/presentation/SolicitudRepuestoViewModel.kt` (4.2); 🆕 (test) `SolicitudRepuestoViewModelTest.kt` | 1. `feat(atencion): buscar la pieza mientras se escribe` · 2. `feat(atencion): enviar la solicitud con la confirmación` · 3. `test(atencion): SolicitudRepuestoViewModel` |
| PR-06 | 🆕 `feature/atencion/presentation/SolicitudRepuestoScreen.kt` (5.2, vistas previas); ✏️ `AtencionNavigation.kt`; ✏️ `IncidenciasNavigation.kt` (aviso al volver al detalle) | 1. `feat(atencion): pantalla «Solicitar repuesto»` · 2. `feat(atencion): «Solicitar repuesto» en la navegación` |
| PR-07 | 🆕 `feature/atencion/domain/RegistrarSolicitudesEnLogistica.kt` (4.3); 🆕 `feature/atencion/data/RegistrarEnLogisticaWorker.kt` y `ProgramadorLogistica.kt` (P-3); ✏️ `AtencionRepositoryImpl.kt` (encola al guardar); 🆕 (test) `RegistrarSolicitudesEnLogisticaTest.kt` | 1. `feat(atencion): registrar en Logística las solicitudes sin número` · 2. `test(atencion): registro con éxito, error de red y rechazo` · 3. `feat(atencion): worker que registra en Logística con conexión` |
| PR-08 | ✏️🔶 `core/sync/SincronizadorIncidencias.kt` (`subir(solicitud)`); ✏️🔶 `core/sync/MapeoFirestore.kt`; ✏️🔶 `core/sync/SincronizacionTiempoReal.kt` (escucha `solicitudesRepuesto`); ✏️⚠️ `firebase/firestore.rules` (D-8); 🆕 (test) casos en `ReglasSincronizacion` si aplica | 1. `feat(core): subir las solicitudes de repuesto a Firestore` · 2. `feat(core): bajar las solicitudes de repuesto` · 3. `feat(firebase): solo el técnico asignado crea la solicitud de repuesto` |
| PR-09 | ✏️ `feature/incidencias/domain/ObservarDetalleIncidencia.kt` (`solicitudes`); ✏️ `IncidenciaRepositoryImpl.kt`; ✏️ `DetalleIncidenciaScreen.kt` (sección «Repuestos», vistas previas); ✏️ (test) `IncidenciaRepositoryFalso.kt`, `DetalleIncidenciaViewModelTest.kt` | 1. `feat(incidencias): el detalle observa las solicitudes de repuesto` · 2. `feat(incidencias): sección «Repuestos» en el detalle` · 3. `test(incidencias): solicitudes en el detalle` |
| PR-10 | 🆕 `feature/atencion/domain/ActualizarEstadoRepuestos.kt` (4.5); ✏️ `AtencionRepository*.kt` (`aplicarEstado`, en `withTransaction`); 🆕 (test) `ActualizarEstadoRepuestosTest.kt` | 1. `feat(atencion): aplicar el estado que informa Logística` · 2. `feat(atencion): volver a «En atención» con el último repuesto entregado` · 3. `test(atencion): ActualizarEstadoRepuestos` |
| PR-11 | 🆕 `feature/atencion/presentation/EstadoRepuestosViewModel.kt` y `EstadoRepuestosScreen.kt` (5.3); ✏️ `AtencionNavigation.kt`; ✏️ `MisTareasScreen.kt` (botón «Repuestos»); 🆕 (test) `EstadoRepuestosViewModelTest.kt` | 1. `feat(atencion): EstadoRepuestosViewModel con filtros y consulta` · 2. `test(atencion): EstadoRepuestosViewModel` · 3. `feat(atencion): pantalla «Estado de repuestos»` · 4. `feat(atencion): «Repuestos» en «Mis tareas»` |
| PR-12 | ✏️ `spec.md` («completa»), ✏️ `tasks.md`, ✏️ `MEMORY.md` (línea de la HU-11) | 1. `docs(atencion): HU-11 completa` |

## 3. Funciones puras y modelo

### 3.1 `SolicitudRepuesto` (PR-01)
```text
SolicitudRepuesto(
    id, incidenciaId, pieza, motivo, fechaSolicitud, confirmadoEn, estadoEntrega,
    solicitadoPorId, solicitadoPorNombre,               // RF-1120: quién la confirmó con huella
    numeroLogistica: String? = null,                    // RF-1117: null = «Por enviar a Logística»
    stockConsultado: Int? = null, diasEnvio: Int? = null,   // RF-1113: la disponibilidad que vio el técnico
    despachadaEn: Instant? = null, entregadaEn: Instant? = null, llegadaEstimada: Instant? = null,   // RF-1122
    rechazadaPorLogistica: Boolean = false              // RF-1119
)
```
`EstadoEntrega` no cambia. La entidad agrega esos campos, `actualizadoEn` y `pendienteSincronizar` (ya está).

### 3.2 `ReglasRepuesto.kt` (PR-03)
```text
puedeSolicitar(usuario, incidencia): ErrorDominio?            // RF-1102, D-5
    sin permiso ATENDER_INCIDENCIA o no es el técnico asignado -> SinPermiso
    marca CERRADA                                             -> IncidenciaCerrada
    si no                                                     -> null

sealed Disponibilidad { Consultando; ConStock(unidades, diasEnvio); SinStock; NoEnCatalogo; SinRespuesta }  // RF-1105 a RF-1108
diasDeEspera(solicitud, ahora): Long = días entre fechaSolicitud y (entregadaEn ?: ahora), mínimo 0   // RF-1122, D-9
marcaAlEntregar(incidencia, quedanSinEntregar): MarcaSeguimiento?   // RF-1124, D-7
    si incidencia.marca == ESPERANDO_REPUESTO y quedanSinEntregar == 0 -> EN_ATENCION
    si no                                                               -> null (no cambia)
estadoVisible(solicitud): EstadoSolicitudVisible               // RF-1120
    rechazadaPorLogistica -> RECHAZADA; numeroLogistica == null -> POR_ENVIAR; si no, según estadoEntrega
```

### 3.3 `MapeoLogistica.kt` (PR-02)
```text
PiezaDto(id, nombre, stock, diasEnvio) -> PiezaLogistica          // stock < 0 o nombre vacío: se descarta
SolicitudLogisticaDto(id, idSigtec, incidencia, equipo, pieza, motivo, confirmadoEn, estado,
                      despachadaEn?, entregadaEn?, llegadaEstimada?) <-> SolicitudRepuesto
estado: "POR_DESPACHAR" | "EN_CAMINO" | "ENTREGADO"; otro texto -> formato inválido
errorDeLogistica(excepción):                                       // RF-1108, RF-1119
    IOException, SocketTimeoutException, HTTP 5xx, HTTP 429    -> ErrorLogistica.SinRespuesta (se reintenta)
    HTTP 400, 404, 422                                          -> ErrorLogistica.Rechazada (no se reintenta)
    SerializationException                                      -> ErrorLogistica.SinRespuesta (y Log.w)
```

## 4. Algoritmos

### 4.1 `SolicitarRepuesto(incidenciaId, pieza, motivo, disponibilidad, confirmadoEn, ahora)` (PR-04)
```text
usuario = VerificarPermiso(ATENDER_INCIDENCIA)        // error -> se propaga
incidencia = repo.incidencia(id) ?: Desconocido("incidencia no encontrada")   // como CerrarIncidenciaConInforme
puedeSolicitar(usuario, incidencia)?.let { return Error(it) }
pieza.trim() o motivo.trim() vacíos -> CampoObligatorio
solicitud = SolicitudRepuesto(UUID, …, estado POR_DESPACHAR, solicitadoPor = usuario)
movimiento = Movimiento(REPUESTO_SOLICITADO, detalle = pieza, ahora)
marca = si incidencia.marca == ESPERANDO_REPUESTO: igual; si no: ESPERANDO_REPUESTO   // RF-1113
repo.solicitarRepuesto(incidencia.copy(marca), movimiento, solicitud, ahora)   // una transacción (RF-1115)
```

### 4.2 `SolicitudRepuestoViewModel` (PR-05)
```text
estado: incidencia, pieza, motivo, coincidencias, elegida, disponibilidad, enviando, error, registrada (evento)
alCambiarPieza(t): pieza = t; elegida = null; disponibilidad = null; busqueda.value = t      // RF-1109
busqueda.debounce(600).filter { length >= 3 }.collectLatest { buscar(it) }               // RF-1104, RF-1106
buscar(t): disponibilidad = Consultando; repo.buscarPiezas(t) ->
    Exito(lista vacía) -> NoEnCatalogo; Exito(lista) -> coincidencias = lista.take(5); Error -> SinRespuesta
alElegir(p): pieza = p.nombre; elegida = p; disponibilidad = si p.stock > 0 ConStock(...) si no SinStock
alConfirmar(confirmadoEn): SolicitarRepuesto(...) -> Exito: registrada = id; Error: error
```
`collectLatest` cancela la consulta anterior cuando llega texto nuevo (RF-1106).

### 4.3 `RegistrarSolicitudesEnLogistica()` (PR-07)
```text
para cada solicitud con numeroLogistica == null y sin rechazo:
    incidencia = repo.incidencia(...)                  // para el código del equipo y el visible
    logistica.registrar(solicitud, incidencia) ->
        Exito(numero) -> dao.marcarRegistrada(id, numero)           // pendienteSincronizar = true
        Rechazada     -> dao.marcarRechazada(id)                    // RF-1119
        SinRespuesta  -> quedan pendientes (el worker devuelve retry)
```
El worker corre con la restricción de red conectada, con `ExistingWorkPolicy.KEEP` y reintento exponencial.

### 4.4 `LogisticaRepositoryImpl` (PR-02)
`buscarPiezas(texto)`, `registrar(solicitud, incidencia)` y `consultar(numero)` llaman a `LogisticaApi` en
`@IoDispatcher`; el `catch` relanza `CancellationException` y traduce el resto con `errorDeLogistica` (3.3), con
`Log.w(TAG, …, e)`.

### 4.5 `ActualizarEstadoRepuestos(tecnicoId, ahora)` (PR-10)
```text
para cada solicitud del técnico registrada y sin entregar:
    remota = logistica.consultar(numero) -> si falla: se cuenta y se sigue con las demás
    si cambió estado o fechas: repo.aplicarEstado(solicitud, remota, ahora)
        en una transacción: guarda la solicitud; si quedó ENTREGADO y marcaAlEntregar(...) != null,
        cambia la marca a EN_ATENCION y guarda el movimiento REPUESTO_RECIBIDO           // RF-1124
devuelve cuántas se actualizaron y si alguna consulta falló; guarda la hora de la última consulta correcta
```

## 5. Servidor y pantallas

### 5.1 MockAPI.io (D-1)
El usuario crea el proyecto y estos dos recursos (la dirección base queda en una sola constante, P-1):

| Recurso | Campos | Uso |
|---|---|---|
| `piezas` | `nombre` (texto), `stock` (número), `diasEnvio` (número) | `GET piezas?search=<texto>` (RF-1104) |
| `solicitudes` | `idSigtec`, `incidencia`, `equipo`, `pieza`, `motivo`, `confirmadoEn` (ISO-8601), `estado`, `despachadaEn`, `entregadaEn`, `llegadaEstimada` | `POST solicitudes` (RF-1117), `GET solicitudes/{id}` (RF-1123) |

Se cargan unas 10 piezas de ejemplo (fuente de poder ATX 500 W, memoria RAM DDR4 8 GB, disco SSD 480 GB, cable HDMI,
teclado USB…), algunas con `stock = 0`. Para simular a Logística, los estados se cambian a mano en el panel de MockAPI.

### 5.2 «Solicitar repuesto» (PR-06, prototipo 8)
`BarraSuperior("Solicitar repuesto", alVolver)`; recuadro «Para la incidencia» en `surfaceVariant`; campo de pieza
con la lista de coincidencias debajo (filas de 48 dp); tarjeta de disponibilidad con ícono y color de estado (verde
con stock, amarillo sin stock, gris sin respuesta con «Reintentar»); campo de motivo; aviso con ícono de información;
pie con `BotonPrincipal` «Enviar con huella» y el ícono de huella. El `Route` usa `rememberVerificadorBiometrico()` con
el título «Confirma la solicitud de repuesto».

### 5.3 «Estado de repuestos» (PR-11, prototipo 17)
`BarraSuperior("Estado de repuestos")` con «Consultado a Logística hoy a las HH:mm» debajo; chips «Todos»,
«Pendientes», «Entregados»; tarjetas con pieza, incidencia y equipo, `EtiquetaEstado` y la línea de tres pasos; pie
con los días de espera. `PullToRefreshBox` de Material 3 para deslizar y consultar (RF-1123).

## 6. Decisiones técnicas

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| P-1 | La dirección base de MockAPI en una constante de `LogisticaModule`. | No es secreta y el producto pide declarar las direcciones en un solo lugar; cambiar al servicio real es cambiar esa línea. | `buildConfigField` desde `local.properties`: toca `app/build.gradle.kts` sin necesidad. |
| P-2 | `OkHttpClient` con 10 s de conexión y lectura, y `Json { ignoreUnknownKeys = true }`. | RF-1108 pide cortar a los 10 s; MockAPI agrega campos propios (`createdAt`). | Los tiempos por defecto (10 s de conexión, sin límite total claro). |
| P-3 | El registro en Logística es un worker de `feature/atencion`, no de `core/sync`. | `core/sync` es el único que habla con **Firestore**; Logística es otra API, de esta feature. | Meterlo en `SubirCambiosWorker`: mezclaría dos servicios con reglas de reintento distintas. |
| P-4 | La solicitud se guarda en Room antes de cualquier llamada a Logística. | Funciona sin conexión (D-2) y respeta «Room es la fuente de verdad». | Registrar primero en Logística y guardar después: sin red no se podría pedir. |
| P-5 | `SolicitarRepuesto` recibe `confirmadoEn` y `ahora`; ningún caso de uso llama a `now()`. | Pruebas deterministas, igual que `CerrarIncidenciaConInforme`. | — |

## 7. Pruebas

JUnit 4, `kotlinx-coroutines-test`, `MainDispatcherRule` y dobles propios, sin librerías nuevas. Doble nuevo:
`LogisticaRepositoryFalso` (catálogo en memoria, falla a pedido con cada `ErrorLogistica`, cuenta registros).

| Prueba (PR) | Casos | RF |
|---|---|---|
| `SolicitudRepuestoDaoTest` (PR-01, instrumentada) | Sin número, por técnico, sin entregar, marcar registrada y rechazada. | RF-1117, RF-1119, RF-1121 |
| `MapeoLogisticaTest` (PR-02) | DTO a dominio y vuelta; estado desconocido; cada tipo de error. | RF-1105 a RF-1108, RF-1119 |
| `ReglasRepuestoTest` (PR-03) | Técnico asignado, otro técnico, jefe, cerrada; días de espera; marca al entregar con 0 y con más pendientes; estado visible. | RF-1102, RF-1120, RF-1122, RF-1124 |
| `SolicitarRepuestoTest` (PR-04) | Sin sesión, sin permiso, otro técnico, cerrada, campos vacíos, camino feliz, ya esperando otro repuesto, falla de Room. | RF-1102, RF-1103, RF-1110, RF-1113, RF-1115 |
| `SolicitudRepuestoViewModelTest` (PR-05) | Menos de 3 letras no busca; espera 600 ms; la última búsqueda gana; elegir con y sin stock; sin coincidencias; sin respuesta y reintentar; editar quita la disponibilidad; enviar deshabilitado; doble toque. | RF-1104 a RF-1116 |
| `RegistrarSolicitudesEnLogisticaTest` (PR-07) | Registra y guarda el número; sin red quedan pendientes; rechazo permanente. | RF-1117, RF-1119 |
| `DetalleIncidenciaViewModelTest` (PR-09) | Las solicitudes llegan al estado para cualquier perfil. | RF-1120 |
| `ActualizarEstadoRepuestosTest` (PR-10) | Cambia a «En camino»; entrega con otra pendiente no cambia la marca; entrega de la última vuelve a «En atención» con el movimiento; incidencia cerrada solo actualiza; una consulta que falla no frena las demás. | RF-1123, RF-1124 |
| `EstadoRepuestosViewModelTest` (PR-11) | Filtros; consulta al abrir; hora de la última consulta; sin conexión muestra lo guardado. | RF-1121 a RF-1123 |

## 8. Respeto a la constitución
1. **Stack cerrado:** Retrofit, OkHttp y kotlinx-serialization ya estaban; no se agregan dependencias.
2. **Spec antes que código:** aprobada el 2026-11-07, antes del primer PR de código.
3. **Lógica fuera de la pantalla:** reglas y casos de uso en `feature/atencion/domain`, en Kotlin puro; Retrofit solo en `data`.
4. **Toda regla tiene prueba:** sección 7.
5. **Datos protegidos:** la confirmación biométrica es obligatoria y `confirmadoEn` también lo exige Firestore; la
   dirección de MockAPI no es una clave.
6. **Español en el negocio:** `SolicitarRepuesto`, `LogisticaRepository`, `ReglasRepuesto`; solo los sufijos de Android en inglés.

## 9. Resumen: archivos compartidos y Room por PR

| PR | Compartidos ⚠️ | Sensibles 🔶 | Room |
|---|---|---|---|
| PR-01 | `core/domain/model/SolicitudRepuesto.kt`, `SolicitudRepuestoEntity.kt`, `SigtecDatabase.kt` | `SolicitudRepuestoDao.kt` | Entidad cambia; `version` 3 → 4 (borra lo no sincronizado en desarrollo) |
| PR-08 | `firebase/firestore.rules` (D-8, publicar a mano) | `core/sync/*` | — |
| PR-12 | `MEMORY.md` (línea de la HU-11) | — | — |

Ningún PR toca `libs.versions.toml`, `app/build.gradle.kts`, `Rutas.kt`, `SigtecRaiz.kt`, `Resultado.kt`,
`MensajesError.kt`, `core/theme/*`, `core/di/*`, `AndroidManifest.xml` ni `feature/acceso/*`.
