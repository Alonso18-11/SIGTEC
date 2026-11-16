# Plan 018 — Tareas diarias: repuestos, resumen del día y casos demorados (HU-18)

> Basado en `spec.md` (lista el 2026-11-16, con D-1 a D-10 aprobadas) y en `docs/constitution.md`.
> El disparo y la descarga viven en `core/sync`, y el canal y el permiso en `core/notificaciones`, con ámbito `core`;
> la tarea de repuestos, en `feature/atencion` (ámbito `atencion`), y las del jefe, en `feature/indicadores` (ámbito
> `indicadores`) (D-10). Toca estos archivos compartidos o sensibles:
> - `core/sync/RevisarAsignacionesWorker.kt` y `ReglasSincronizacion.kt` (D-1, D-6);
> - `core/notificaciones/PermisoNotificaciones.kt` (D-9);
> - `core/domain/model/Incidencia.kt` (solo el comentario de `critica`, D-3);
> - `feature/atencion/di/AtencionModule.kt` y `feature/indicadores/di/IndicadoresModule.kt` (`@IntoSet`);
> - `specs/015-tablero-indicadores/spec.md` (nota de RF-1508, D-3) y `MEMORY.md`.
>
> No toca entidades, DAO, `SigtecDatabase` (no sube `version`), `firestore.rules`, el manifiesto, `core/di`,
> `Rutas.kt`, la navegación ni las dependencias. Cada PR trata un bloque, con varios commits chicos y push después de
> cada uno, y necesita la aprobación de Alonso. Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` y
> `app/src/test/java/com/softcorp/sigtec/`.

## 0. Hallazgos del código real
- **La HU-17 no está en `main`.** `RevisarAsignacionesWorker`, `GuardadoIncidencias` y `programarRevision()` están en
  #220 (sobre #219). El código de esta HU se apila sobre #220.
- **Repuestos.** `ActualizarEstadoRepuestos(ahora)` exige `ATENDER_INCIDENCIA`, devuelve
  `ResultadoActualizacion(actualizadas, sinRespuesta)` y anota `CONSULTAR_REPUESTOS` con `exitosa = sinRespuesta == 0`.
  `AtencionRepository.observarUltimaConsultaLogistica()` ya da la última consulta exitosa (manual o no).
- **Indicadores.** `calcularIndicadores(...)` ya da `pendientes`, `criticos` (con `esCritica`) y `reparadosPorDia`, cuyo
  último elemento es hoy (`esHoy`). `IndicadoresRepository.observarDatos()` da todo lo que necesita. Basta con tomar el
  primer valor del `Flow`.
- **Permiso.** `debePedirPermisoNotificaciones(sdk, concedido, perfil)` solo acepta `ATENDER_INCIDENCIA`; lo usa
  `SigtecRaiz` y tiene `PermisoNotificacionesTest`.
- **Toque del aviso.** `MainActivity` solo navega si el intent trae `EXTRA_INCIDENCIA_ID`: un intent sin extras abre la
  app normalmente (RF-1821) sin tocar la navegación.
- **Firestore.** Los movimientos guardan `fecha` como `Timestamp`; filtrar por `fecha >= inicio del día` usa el índice de
  un solo campo, sin índice compuesto. `/incidencias` y `/movimientos` se leen con estar autenticado.
- **Registro.** `sincronizaciones` guarda `tipoTarea` como texto: las tareas del jefe usan el nombre de su tipo.

## 1. Diseño
- **Las horas en una sola constante (D-2).** `enum class TipoTareaDiaria(val desde: LocalTime)` en
  `core/sync/TareaDiaria.kt`: `REPUESTOS(8:00)`, `CASOS_DEMORADOS(8:00)`, `RESUMEN_DIA(18:00)`.
- **La interfaz (D-10).** `interface TareaDiaria { val tipo; val necesitaDatosAlDia: Boolean; fun correPara(perfil):
  Boolean; suspend fun ultimaExitosa(): Instant?; suspend fun ejecutar(ahora: Instant, zona: ZoneId): Boolean }`. Cada
  tarea anota su propio éxito (la de repuestos ya lo hace con `CONSULTAR_REPUESTOS`, RF-1807).
- **Cuándo toca (RF-1801, RF-1803).** `tocaTareaDiaria(desde, ahora, zona, ultimaExitosa)`: la hora local ya pasó de
  `desde` y la última exitosa no es de hoy.
- **El worker (RF-1804, RF-1805).** `doWork` queda: sesión → respaldo de la subida → red validada → asignaciones (solo
  técnico) → tareas diarias. Para las tareas: filtra las que corren para el perfil y tocan; si alguna necesita datos al
  día, baja una vez (`DescargaDatosAlDia`) y, si falla, salta esas (RF-1811); corre cada una en `try`/`catch`
  (relanzando `CancellationException`) para que una falla no frene a las demás.
- **Descarga del jefe (D-6, RF-1810).** `DescargaDatosAlDia.bajar(usuarioId, ahora, zona): Boolean` en `core/sync`:
  todas las incidencias con `Source.SERVER`, guardadas con `GuardadoIncidencias` (como el jefe no es técnico, nunca
  avisa de asignaciones), y los movimientos con `fecha >= inicio del día`, guardados con `debeGuardarMovimiento`.
- **Avisos (D-8, RF-1819 a RF-1821).** `NotificadorAvisosArea` en `core/notificaciones`: canal `avisos_area`
  («Avisos del área», importancia por defecto), `avisar(id, contenido)` y `quitar(id)` con la etiqueta `avisos_area`
  (no choca con los ids de las asignaciones), versión pública con solo el título y un intent a `MainActivity` sin
  extras.
- **Tarea de repuestos (RF-1806 a RF-1809).** `TareaRepuestos` en `feature/atencion/data`: corre para
  `ATENDER_INCIDENCIA`, `ultimaExitosa` es la última consulta exitosa a Logística y `ejecutar` es
  `ActualizarEstadoRepuestos(ahora)`, con éxito solo si `sinRespuesta == 0`.
- **Tareas del jefe (RF-1812 a RF-1818).** En `feature/indicadores`:
  - `CalcularIndicadoresDeHoy(ahora, zona)` (dominio): `VerificarPermiso(VER_INDICADORES)` y `calcularIndicadores` con el
    primer valor de `observarDatos()`.
  - `AvisosJefe.kt` (dominio): `contenidoCasosDemorados(n): ContenidoAviso?` (`null` con 0) y
    `contenidoResumenDia(reparados, pendientes, criticos): ContenidoAviso`, con singular y plural.
  - `TareaCasosDemorados` y `TareaResumenDia` (datos): `necesitaDatosAlDia = true`; calculan, avisan (o quitan el aviso
    con 0 demorados) y anotan su tipo con `IndicadoresRepository.registrarTarea`.
- **Permiso al jefe (D-9).** `debePedirPermisoNotificaciones` acepta `ATENDER_INCIDENCIA` o `VER_INDICADORES`.
- **Comentarios de `critica` (D-3).** Los que dicen que la HU-18 la marcaría pasan a decir que nadie la escribe hoy y
  que «crítico» se calcula con `esCritica`.

## 2. División en PR, dependencias y orden de fusión
| PR | Rama | Título | Base | RF |
|---|---|---|---|---|
| Spec | `docs/spec-018-tareas-diarias` | `docs(core): spec, plan y tareas de las tareas diarias (HU-18)` | `main` | — |
| PR-A | `feat/hu18-tareas-diarias` | `feat(core): tareas diarias en la revisión periódica y avisos del área (HU-18)` | #220 | RF-1801 a RF-1805, RF-1810, RF-1811, RF-1819 a RF-1823 |
| PR-B | `feat/hu18-tarea-repuestos` | `feat(atencion): consulta diaria del estado de los repuestos (HU-18)` | PR-A | RF-1806 a RF-1809 |
| PR-C | `feat/hu18-avisos-jefe` | `feat(indicadores): avisos de casos demorados y del resumen del día (HU-18)` | PR-A | RF-1812 a RF-1818 |
| PR-D | `docs/hu18-cierre` | `docs(core): cierre de la HU-18 (HU-18)` | Spec | todos |

PR-A sale de #220; PR-B y PR-C se apilan sobre PR-A y no dependen entre sí. Si #220 se fusiona antes, PR-A pasa a
salir de `main`.

## 3. Archivos por PR
Leyenda: 🆕 nuevo, ✏️ modificado, ⚠️ compartido de `CLAUDE.md`, 🔶 sensible fuera de esa lista.

### PR-A — núcleo
- 🆕🔶 `core/sync/TareaDiaria.kt`: `TipoTareaDiaria` y `TareaDiaria`.
- ✏️🔶 `core/sync/ReglasSincronizacion.kt`: `tocaTareaDiaria`; ✏️ `ReglasSincronizacionTest`.
- 🆕🔶 `core/sync/DescargaDatosAlDia.kt`.
- ✏️🔶 `core/sync/RevisarAsignacionesWorker.kt`: nuevo orden de `doWork` y `correrTareasDiarias`.
- 🆕 `core/notificaciones/NotificadorAvisosArea.kt` y ✏️ `res/drawable` (si hace falta un ícono propio; si no, se
  reutiliza `ic_aviso_asignacion`).
- ✏️ `core/notificaciones/PermisoNotificaciones.kt` y ✏️ `PermisoNotificacionesTest`.

### PR-B — repuestos
- 🆕 `feature/atencion/data/TareaRepuestos.kt` y ✏️⚠️ `feature/atencion/di/AtencionModule.kt` (`@IntoSet`).
- 🆕 `test/.../feature/atencion/data/TareaRepuestosTest.kt` con los dobles existentes.
- ✏️ Comentarios que dicen «la HU-18 llamará…» en `ActualizarEstadoRepuestos`, `EstadoRepuestosViewModel`,
  `AtencionNavigation`, `AtencionRepositoryImpl` y `SolicitudRepuestoDao`.

### PR-C — avisos del jefe
- 🆕 `feature/indicadores/domain/AvisosJefe.kt` y `AvisosJefeTest`.
- 🆕 `feature/indicadores/domain/CalcularIndicadoresDeHoy.kt` y su prueba.
- ✏️ `IndicadoresRepository` (`ultimaTareaExitosa`, `registrarTarea`), su `Impl` y `IndicadoresRepositoryFalso`.
- 🆕 `feature/indicadores/data/TareaCasosDemorados.kt`, `TareaResumenDia.kt` y ✏️⚠️ `IndicadoresModule` (`@IntoSet`).
- ✏️ Comentarios de `critica` (D-3): ⚠️ `core/domain/model/Incidencia.kt`, `ReglaCritica.kt`, `Indicadores.kt` y
  `CalcularIndicadoresTest`.

### PR-D — cierre
- ✏️ `specs/015-tablero-indicadores/spec.md`: nota de RF-1508 (D-3).
- ✏️ `MEMORY.md`, spec y tareas de la HU-18.

## 4. Pruebas
- JVM: `tocaTareaDiaria` (antes de la hora, en la hora, ya hecha hoy, hecha ayer, sin anotación, medianoche);
  `debePedirPermisoNotificaciones` con el jefe; `contenidoCasosDemorados` (0, 1, varios) y `contenidoResumenDia` (0,
  1, varios); `CalcularIndicadoresDeHoy` (sin sesión, técnico sin permiso, jefe con reparados hoy y ayer);
  `TareaRepuestos` (corre solo para el técnico; éxito solo si todas respondieron).
- Manual: la lista de la spec, con la hora del celular adelantada.

## 5. Riesgos
- La descarga del jefe (RF-1810) lee toda la colección dos veces al día; con muchas más incidencias habría que pasar a
  una descarga por fecha.
- Si la escucha en tiempo real y la descarga corren a la vez, `GuardadoIncidencias` ya serializa las escrituras.
- Cambiar la hora del celular para la prueba manual afecta los registros de `sincronizaciones` de ese día.
