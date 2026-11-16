# Plan 017 — Envío y avisos en segundo plano, con la app cerrada (HU-17)

> Basado en `spec.md` (lista el 2026-11-16, con D-1 a D-6 aprobadas) y en `docs/constitution.md`.
> Todo el código vive en `core/sync` (D-5), con ámbito de commit `core`. Toca estos archivos compartidos o sensibles:
> - `core/sync/SincronizacionTiempoReal.kt`, `ReglasSincronizacion.kt` y `ProgramadorSincronizacion.kt` (D-1, D-5);
> - `specs/007-tiempo-real-aviso/spec.md` (RF-703, D-1);
> - `CLAUDE.md` (quitar el pendiente ya resuelto) y `MEMORY.md`, en el PR de cierre.
>
> No toca entidades, DAO, `SigtecDatabase` (no sube `version`), `firestore.rules`, el manifiesto, `core/di`,
> `SigtecApp`, `feature/acceso`, los casos de uso de sesión ni las dependencias. Cada PR trata un bloque, con varios
> commits chicos y push después de cada uno, lleva la HU al final del título y necesita la aprobación de Alonso.
> Las rutas son relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM).

## 0. Hallazgos del código real
- **La subida ya funciona con la app cerrada.** `ProgramadorSincronizacion.subirCambios()` encola `SubirCambiosWorker`
  con red obligatoria y `APPEND_OR_REPLACE`; `vigilarPendientes()` lo llama cuando la suma de pendientes de cinco DAO
  pasa a ser mayor que 0. Todos esos DAO ya tienen un `Flow<Int>` de pendientes.
- **El aviso vive dentro de la escucha.** `SincronizacionTiempoReal.guardarIncidencias` hace, por documento: saltar
  `REMOVED` y las escrituras propias sin confirmar, `debeReemplazarLocal`, guardar con `atencionAlBajar` y marcar los
  movimientos (RF-79) en una transacción, y avisar con `esAsignacionNuevaPara`. El worker necesita lo mismo desde la
  línea `debeReemplazarLocal` en adelante.
- **El proceso arranca con el worker.** Cuando WorkManager despierta la app, `SigtecApp.onCreate()` corre
  `SincronizacionTiempoReal.iniciar()` y la escucha también empieza a bajar. La escucha y el worker pueden procesar la
  misma incidencia a la vez: hay que guardarla y avisar bajo un candado (`Mutex`) para no avisar dos veces (RF-1712).
- **La sesión tarda en llegar.** `usuarioActual` es un `StateFlow` que empieza en `null` y se llena cuando Firestore
  devuelve el perfil (`stateIn(Eagerly)`). El worker espera el primer usuario no nulo con un límite de tiempo.
- **La sesión se sigue en un solo lugar.** `iniciar()` mapea `usuarioActual` a su `id` con `distinctUntilChanged`: el
  primer valor al arrancar es `null` aunque haya sesión. Cancelar la revisión solo al pasar de un `id` a `null`.
- **Cerrar sesión borra Room.** `SesionRepositoryFirebase.cerrarSesion()` y la entrada de otra persona (D-12 de la
  HU-05) llaman a `clearAllTables()`. Lo que se guarde en Room dura exactamente lo que dura la sesión de su dueño.
- **Registro de ejecuciones.** La tabla `sincronizaciones` (`tipoTarea`, `ejecutadaEn`, `exitosa`) ya tiene
  `registrar`, `recortar(tipo)` y `observarUltimaExitosa(tipo)`. Nadie la lista entera: un tipo nuevo no cambia ninguna
  pantalla.
- **Firestore.** `marca` se guarda con `marca.name` y `tecnicoAsignadoId` es texto. `SincronizadorIncidencias` ya usa
  `get(Source.SERVER).await()` (`kotlinx.coroutines.tasks`).
- **Pruebas.** `ReglasSincronizacionTest` ya cubre `esAsignacionNuevaPara`; `RelojFalso` existe sin usar.

## 1. Diseño
- **Inicio de sesión en este celular (D-1).** `InicioSesionLocal` anota en `sincronizaciones` un registro de tipo
  `INICIO_SESION` cuando la escucha arranca con una sesión y no hay ninguno (después del borrado de D-12). Como cerrar
  sesión y la entrada de otra persona borran Room, el registro vive lo mismo que la sesión. En un celular que ya tenía
  sesión al instalar esta versión, el primer arranque lo anota con esa hora: nada anterior se avisa.
- **Regla de aviso (D-1).** `esAsignacionNuevaPara(usuarioId, local, remota, inicioSesion)`: asignada a la persona y sin
  cerrar, y además `local` existe y no era suya, o `local` no existe y `remota.actualizadoEn` es posterior a
  `inicioSesion`. Sin `inicioSesion` conocido, solo la primera parte (como la HU-07).
- **Guardado compartido.** `GuardadoIncidencias` (`@Singleton`) con `suspend fun guardar(remota, usuarioId,
  inicioSesion)`: el bloque de `guardarIncidencias` desde `debeReemplazarLocal`, bajo un `Mutex`. La escucha y el worker
  lo usan.
- **Worker (D-3, D-6).** `RevisarAsignacionesWorker` (`@HiltWorker`):
  1. espera el usuario hasta 10 s; sin usuario, `success()` sin hacer nada (RF-1703);
  2. si hay pendientes, `programador.subirCambios()` (RF-1706, RF-1707);
  3. si `!debeConsultarAsignaciones(perfil)`, termina (RF-1716);
  4. si `!debeIntentarSubir(monitor.hayConexionAhora())` (red sin validar), termina (RF-1704);
  5. consulta `incidencias` con `tecnicoAsignadoId == id` y `marca in [ASIGNADA, EN_ATENCION, ESPERANDO_REPUESTO]`, con
     `Source.SERVER` (RF-1708); si falla, `Log.w` y `success()` (RF-1715);
  6. guarda cada documento bien formado con `GuardadoIncidencias` (RF-1709 a RF-1714);
  7. anota `REVISAR_ASIGNACIONES` y recorta a 20 (RF-1717).
  Siempre devuelve `success()`: el período ya es el reintento.
- **Programación (D-3, D-5).** `ProgramadorSincronizacion.programarRevision()` usa
  `enqueueUniquePeriodicWork("revisar-asignaciones", KEEP, …)` con 15 minutos y red obligatoria; `cancelarRevision()`
  la quita. `iniciar()` llama a `programarRevision()` con cada `id` no nulo y a `cancelarRevision()` al pasar de un `id`
  a `null` (RF-1701, RF-1702, RF-1705).
- **Índice de Firestore.** La consulta combina dos igualdades (`==` e `in`) sin desigualdad ni orden, que Firestore
  resuelve con los índices de un solo campo. Si el log mostrara «requires an index», es un paso en la consola (gratis
  en Spark) y una decisión nueva: se detiene ahí.

## 2. División en PR, dependencias y orden de fusión
| PR | Rama | Título | Base | RF |
|---|---|---|---|---|
| Spec | `docs/spec-017-avisos-segundo-plano` | `docs(core): spec del envío y los avisos con la app cerrada (HU-17)` | `main` | — |
| PR-A | `feat/hu17-regla-aviso` | `feat(core): avisar la asignación que llega por primera vez después del ingreso (HU-17)` | `main` | RF-1709 a RF-1713 |
| PR-B | `feat/hu17-revision-periodica` | `feat(core): revisión periódica de asignaciones con la app cerrada (HU-17)` | PR-A | RF-1701 a RF-1708, RF-1714 a RF-1717 |
| PR-C | `docs/hu17-cierre` | `docs(core): cierre de la HU-17 y pendiente de CLAUDE.md resuelto (HU-17)` | `main` | todos |

PR-B se apila sobre PR-A. PR-C solo toca documentación y sale de `main`.

## 3. Archivos por PR
Leyenda: 🆕 nuevo, ✏️ modificado, ⚠️ compartido de `CLAUDE.md`, 🔶 sensible fuera de esa lista.

### PR-A — regla de aviso y guardado compartido (D-1)
- ✏️🔶 `core/sync/ReglasSincronizacion.kt`: `TAREA_INICIO_SESION`, y `esAsignacionNuevaPara` con `inicioSesion`.
- ✏️ `test/.../core/sync/ReglasSincronizacionTest.kt`: los casos de la HU-07 con el parámetro nuevo y los de D-1.
- 🆕 `core/sync/InicioSesionLocal.kt`: `anotarSiFalta(ahora)` y `leer(): Instant?` sobre `SincronizacionDao`.
- 🆕 `core/sync/GuardadoIncidencias.kt`: guardar y avisar bajo un `Mutex`.
- ✏️🔶 `core/sync/SincronizacionTiempoReal.kt`: anota el inicio después del borrado de D-12 y `guardarIncidencias`
  usa `GuardadoIncidencias`.
- ✏️ `specs/007-tiempo-real-aviso/spec.md`: RF-703 reemplazado por RF-1713 de la HU-17.

### PR-B — revisión periódica (D-2, D-3, D-6)
- ✏️🔶 `core/sync/ReglasSincronizacion.kt`: `TAREA_REVISAR`, `MARCAS_ACTIVAS_ASIGNADAS`,
  `debeConsultarAsignaciones(perfil)` y `hayPendientesDeSubir(vararg cantidades)`; `vigilarPendientes` usa esta última.
- ✏️ `test/.../core/sync/ReglasSincronizacionTest.kt`: casos de las tres reglas.
- 🆕 `core/sync/RevisarAsignacionesWorker.kt`.
- ✏️🔶 `core/sync/ProgramadorSincronizacion.kt`: `programarRevision()` y `cancelarRevision()`.
- ✏️🔶 `core/sync/SincronizacionTiempoReal.kt`: programar y cancelar según la sesión.

### PR-C — cierre
- ✏️⚠️ `CLAUDE.md`: quitar el «Pendiente conocido» de cerrar sesión, ya resuelto por RF-13, RF-14 y RF-943.
- ✏️ `MEMORY.md`: línea de la HU-17 y estado actual.
- ✏️ `specs/017-avisos-segundo-plano/spec.md` y `tasks.md`: estado de la implementación.

## 4. Pruebas
- JVM (`ReglasSincronizacionTest`): aviso con local sin asignar, asignada a otro, ya suya, sin local antes y después del
  inicio, sin inicio conocido, cerrada; consulta para técnico, jefe y personal de sistemas; pendientes en 0 y mayores
  que 0; las marcas activas no incluyen `SIN_ASIGNAR` ni `CERRADA`.
- Manual (spec, «Criterios de finalización»): con dos cuentas, la app del técnico cerrada, bloqueada y tras reiniciar;
  primer ingreso sin avisos; modo avión; el jefe sin avisos. Forzar el trabajo con `adb shell cmd jobscheduler run`.
- El worker, `GuardadoIncidencias` y la programación dependen de WorkManager, Firestore y Room reales, igual que
  `SubirCambiosWorker`: se prueban a mano, como en la HU-05.

## 5. Riesgos
- Doze y fabricantes retrasan la revisión (D-4): se documenta, no se combate.
- La escucha y el worker en el mismo proceso: el `Mutex` de `GuardadoIncidencias` evita el aviso doble.
- Hora de los celulares (D-1, RF-62): un celular con la hora muy atrasada puede dejar sin aviso una asignación que
  llega por primera vez; aparece igual en la lista.
