# Tareas 005 — Correcciones de sincronización tras la revisión QA (HU-05)

> Salen de `spec.md` y `plan.md`. Cada tarea dura 20-30 min como máximo y depende solo de las anteriores. Orden de adentro hacia afuera: reglas puras con sus pruebas → Room/DAO → `core/sync` → aviso en pantalla → verificación.
> RF-58 a RF-68, RF-70 a RF-74 y RF-76/RF-77 (lo que no cambia) ya están en `main`; aquí solo aparecen cuando una tarea los toca.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/` (pruebas). ⚠️ = archivo compartido, coordinado según `plan.md` sección 11.

## Fase 0 — Preparación

- [x] **T01 · Confirmar la rama**
  Ya se trabaja en `feat/hu05-sincronizacion-sdd` (no se crea una rama nueva). Verificar que está al día con `main` antes de empezar.
  RF: —
  Hecho cuando: `git branch --show-current` muestra `feat/hu05-sincronizacion-sdd`, `git fetch && git log origin/main..HEAD` no sorprende, y `./gradlew testDebugUnitTest` pasa sin cambios todavía.

## Fase 1 — Reglas puras (`core/sync/ReglasSincronizacion.kt`)

- [x] **T02 · `siguienteNumero`**
  Agregar `internal fun siguienteNumero(ultimoActual: Long?): Long = (ultimoActual ?: 0L) + 1` y sus 3 casos en `ReglasSincronizacionTest.kt` (`null` → 1, `0L` → 1, `5L` → 6).
  RF: RF-69
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ReglasSincronizacionTest"` pasa con los 3 casos nuevos.

- [x] **T03 · `esErrorPermanente`**
  Agregar la función (plan, sección 2.2): `FirebaseFirestoreException` con `PERMISSION_DENIED`, `FAILED_PRECONDITION` o `INVALID_ARGUMENT` → `true`; cualquier otro código o excepción → `false`. Agregar 6 casos a `ReglasSincronizacionTest.kt` (los 3 códigos permanentes, `UNAVAILABLE`, `DEADLINE_EXCEEDED` y una `IOException` genérica).
  RF: RF-75
  Hecho cuando: la prueba pasa con los 6 casos, construyendo `FirebaseFirestoreException` directamente (sin mocks).
  Corrección (2026-10-11): construir `FirebaseFirestoreException.Code` en la JVM falla sin `isReturnDefaultValues` (el enum usa clases de Android). Se quitó ese cambio de `app/build.gradle.kts` (archivo compartido, no estaba en el plan) y la regla pasó a `esCodigoPermanente(nombre)`; las pruebas usan el nombre del código.

- [x] **T04 · `debeIntentarSubir`**
  Agregar `internal fun debeIntentarSubir(hayConexionValidada: Boolean): Boolean = hayConexionValidada` y sus 2 casos (`true`, `false`) en `ReglasSincronizacionTest.kt`.
  RF: RF-78
  Hecho cuando: la prueba pasa con los 2 casos.

## Fase 2 — Room y DAO ⚠️

- [x] **T05 · Campo `necesitaAtencion` en las 2 entidades ⚠️**
  Agregar `necesitaAtencion: Boolean = false` a `IncidenciaEntity.kt` y `MovimientoEntity.kt`, al final de los metadatos de sincronización. No tocar `toDomain()` (no es dato de dominio) ni los `toEntity()`/`aIncidenciaEntity()`/`aMovimientoEntity()` existentes (el valor por defecto ya resuelve D-7 del plan).
  RF: RF-75, RF-76
  Hecho cuando: compila sin tocar ningún otro archivo que use esas entidades.

- [x] **T06 · Subir `version` de Room ⚠️**
  `SigtecDatabase.kt`: `version = 1` → `version = 2`. Anotar en la descripción del PR (plantilla, sección de archivos compartidos).
  RF: —
  Hecho cuando: `./gradlew assembleDebug` compila y la app arranca en un emulador/celular sin crashear al abrir la base (destructiva, se pierde lo no sincronizado, esperado en desarrollo).

- [x] **T07 · `marcarNecesitaAtencion` y `observarNecesitanAtencion` en los 2 DAO**
  `IncidenciaDao.kt` y `MovimientoDao.kt`: agregar `suspend fun marcarNecesitaAtencion(id: String)` (`UPDATE ... SET pendienteSincronizar = 0, necesitaAtencion = 1 WHERE id = :id`) y `fun observarNecesitanAtencion(): Flow<Int>` (`SELECT COUNT(*) ... WHERE necesitaAtencion = 1`).
  RF: RF-75, RF-76
  Hecho cuando: `./gradlew assembleDebug` compila (las consultas de Room se validan en tiempo de compilación).

## Fase 3 — `core/sync`

- [x] **T08 · `Reloj` y `RelojSistema`**
  Crear `core/sync/Reloj.kt`: `interface Reloj { fun ahora(): Instant }` y `class RelojSistema @Inject constructor() : Reloj { override fun ahora() = Instant.now() }`.
  RF: RNF «Reloj inyectado»
  Hecho cuando: compila; `RelojSistema` es el único lugar de `core/sync` que llama a `Instant.now()` (se confirma en T12).

- [x] **T09 · Enlazar `Reloj` en Hilt ⚠️**
  `core/di/SincronizacionModule.kt`: `@Binds @Singleton abstract fun bindReloj(impl: RelojSistema): Reloj`.
  RF: RNF «Reloj inyectado»
  Hecho cuando: `./gradlew assembleDebug` compila (Hilt resuelve la inyección).

- [x] **T10 · `MonitorConexion.hayConexionAhora()`**
  Agregar `fun hayConexionAhora(): Boolean = conexionActual()` (método público, reutiliza la función privada existente).
  RF: RF-78
  Hecho cuando: compila y no cambia el comportamiento de `hayConexion` (el `Flow` existente sigue igual).

- [x] **T11 · `SincronizadorIncidencias`: contador, error permanente y reloj por parámetro**
  (a) `crearConNumero`: usar `siguienteNumero` sobre el valor leído del contador y escribir con `tx.set(contador, mapOf("ultimo" to numero))` en vez de `tx.update(...)` (una sola escritura, plan sección 3.1).
  (b) `subir(local: IncidenciaEntity)` y `subir(movimiento: MovimientoEntity)`: en el `catch`, si `esErrorPermanente(e)`, llamar a `marcarNecesitaAtencion(id)` y devolver `true`; si no, mantener el comportamiento actual (`false`).
  (c) `subirPendientes()` → `subirPendientes(ahora: Instant)`, usar `ahora` en `SincronizacionEntity(ejecutadaEn = ahora, ...)`, quitar el `Instant.now()` interno.
  RF: RF-69, RF-75, reloj
  Hecho cuando: `./gradlew assembleDebug` compila, y `SincronizadorIncidencias.kt` no contiene `Instant.now()`.

- [x] **T12 · `SubirCambiosWorker`: red validada y reloj**
  Agregar `private val monitor: MonitorConexion` al constructor `@AssistedInject`. En `doWork()`, antes del `repeat`: `if (!debeIntentarSubir(monitor.hayConexionAhora())) return Result.retry()`. Cambiar la llamada a `sincronizador.subirPendientes(Instant.now())`.
  RF: RF-78, reloj
  Hecho cuando: `./gradlew assembleDebug` compila.

- [x] **T13 · `SincronizacionTiempoReal`: reloj inyectado**
  Agregar `private val reloj: Reloj` al constructor. `registrarDescarga()` usa `reloj.ahora()` en vez de `Instant.now()`.
  RF: reloj
  Hecho cuando: `./gradlew assembleDebug` compila, y `SincronizacionTiempoReal.kt` no contiene `Instant.now()`.

- [x] **T14 · Doble `RelojFalso`**
  Crear `app/src/test/.../core/sync/RelojFalso.kt`: `class RelojFalso(private val instante: Instant) : Reloj { override fun ahora() = instante }`. No se usa todavía en ninguna prueba (queda listo para cuando exista una prueba completa de estas clases, ver `plan.md` sección 8); se deja constancia en `MEMORY.md` en T18.
  RF: reloj (preparación)
  Hecho cuando: compila con `./gradlew testDebugUnitTest` y vive en el mismo paquete que `Reloj`.

## Fase 4 — `EstadoDatos` y su repositorio ⚠️

- [x] **T15 · Campo `necesitanAtencion` en `EstadoDatos` ⚠️**
  `core/domain/model/EstadoDatos.kt`: agregar `necesitanAtencion: Int`. Actualizar `EstadoDatosRepositoryFalso.kt` (el constructor del `MutableStateFlow` inicial) y los 2 `@Preview` de `AvisoConexion.kt` que construyen `EstadoDatos` a mano, para que sigan compilando. Agregar `fun fijarNecesitanAtencion(n: Int)` a `EstadoDatosRepositoryFalso.kt`, igual que ya existe `fijarPendientes`.
  RF: RF-76, RF-77
  Hecho cuando: `./gradlew assembleDebug testDebugUnitTest` compila.

- [x] **T16 · `EstadoDatosRepositoryImpl` combina las 2 cuentas nuevas**
  Agregar `MovimientoDao` al constructor. Combinar `incidenciaDao.observarNecesitanAtencion()` y `movimientoDao.observarNecesitanAtencion()` (suma simple, D-6 del plan) dentro del `combine` existente, y pasar el resultado a `EstadoDatos.necesitanAtencion`.
  RF: RF-76
  Hecho cuando: `./gradlew assembleDebug` compila (Hilt sigue resolviendo `EstadoDatosRepositoryImpl` sin tocar su `@Binds`).

## Fase 5 — Aviso de conexión

- [x] **T17 · `textoAviso` y su prueba**
  En `AvisoConexion.kt`, extraer el `when` de `AvisoConexionContenido` a `internal fun textoAviso(estado: EstadoDatos): String?` (plan, sección 4), con la rama nueva de `necesitanAtencion` (singular «necesita tu atención» / plural «necesitan tu atención», concordancia de verbo como en `TextosSalida.textoSePerdera`). `AvisoConexionContenido` solo llama a `textoAviso` y elige el ícono (`Icons.Outlined.ErrorOutline` para «necesita atención», ya disponible en `material-icons-extended`). Agregar `@Preview` «Necesita atención» en claro y oscuro. Crear `AvisoConexionTest.kt` en `app/src/test/.../core/ui/components/` con los 5 casos del plan (sección 8).
  RF: RF-76, RF-77
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*AvisoConexionTest"` pasa con los 5 casos, y las previews se ven en Android Studio en claro y oscuro.

## Fase 6 — `firestore.rules` ⚠️

- [x] **T18 · Agregar `allow create` sobre `contadores/incidencias` ⚠️**
  Aplicar el diff de `plan.md` sección 6 a `firebase/firestore.rules`. No se publica desde aquí (eso es la tarea informativa de más abajo, a cargo del usuario).
  RF: RF-69
  Hecho cuando: el archivo tiene el bloque `allow create: if autenticado() && request.resource.data.ultimo == 1;` antes de `allow update`, y el `allow update` existente no cambió.

## Fase 7 — Verificación y cierre

- [x] **T19 · Comando del CI**
  Correr `./gradlew assembleDebug testDebugUnitTest lintDebug`.
  RF: todos los de esta revisión
  Hecho cuando: termina en verde y sin warnings de lint nuevos respecto a `main`.

- [x] **T20 · Revisión de código del reloj inyectado**
  Buscar `Instant.now()` en `app/src/main/java/com/softcorp/sigtec/core/sync/`: solo debe aparecer dentro de `RelojSistema.ahora()` y, si quedó así, dentro de `SubirCambiosWorker` (que es quien le pasa `ahora` a `subirPendientes`, plan D-5).
  RF: RNF «Reloj inyectado»
  Hecho cuando: ningún otro archivo de `core/sync` contiene `Instant.now()`.

- [x] **T21 · Actualizar `MEMORY.md`**
  Agregar el estado de las 5 correcciones de esta revisión (hechas), el recordatorio de publicar `firestore.rules` a mano antes de que RF-69 funcione en producción, y la nota del doble `RelojFalso` sin usar todavía.
  RF: todos los de esta revisión
  Hecho cuando: `MEMORY.md` sigue en unas 50 líneas como máximo.

- [x] **T22 · Commit y PR**
  Commit tipo `fix(core): corregir contador, errores permanentes, red validada y reloj en sincronización (HU-05)`, sin línea de coautoría. PR en borrador con `.github/pull_request_template.md`, incluida la sección «Archivos compartidos que toca» (lista de `plan.md` sección 11) y un recordatorio explícito de que `firestore.rules` no se publicó todavía.
  RF: todos los de esta revisión
  Hecho cuando: el PR existe en borrador, el CI «compilar-y-probar» está en verde y la descripción enlaza `specs/005-sincronizacion/`.

---

## Tareas informativas (no las ejecuta el implementador — a cargo del coordinador/usuario)

- [x] **M01 · Publicar `firestore.rules` en la consola de Firebase**
  Antes de publicar: probar en el Rules Playground de la consola tanto la nueva condición de `create` sobre `contadores/incidencias` (`request.resource.data.ultimo == 1`) como la condición aritmética de `update` ya existente (`ultimo == resource.data.ultimo + 1`), con y sin el documento del contador creado. Recién después, pegar el archivo corregido en la consola. Sin esto, RF-69 sigue fallando en producción aunque el código ya esté corregido (el emulador local de Firestore, si se usa, no cuenta: tiene que ser la consola real).

- [x] **M02 · Prueba manual en celular**
  Con la app instalada (`./gradlew installDebug`) en un celular real o un emulador con Google Play Services:
  1. Registrar una incidencia en modo avión, comprobar que queda «Por sincronizar» y que el aviso muestra «Sin conexión»; desactivar el modo avión y comprobar que en pocos segundos obtiene su número y el aviso desaparece (sin cambios de esta revisión, pero sirve de referencia base).
  2. Si el proyecto de Firebase nunca tuvo el contador (o se borró a propósito para la prueba), repetir el paso 1 y comprobar que la incidencia igual obtiene el número `1` en vez de quedarse «Por sincronizar» para siempre (RF-69).
  3. Forzar un error permanente controlado (por ejemplo, registrar una incidencia y luego, desde la consola, revocar el perfil del usuario antes de que el `Worker` la suba, de modo que `incidencias.update` lo rechace con `PERMISSION_DENIED`) y comprobar que el aviso cambia a «N cambios necesitan tu atención» en vez de reintentar indefinidamente (RF-75, RF-76).
  4. Conectarse a un Wi-Fi sin salida real a internet (o simular con `adb shell cmd connectivity airplane-mode enable` seguido de una red falsa, según lo que permita el dispositivo) y comprobar que el aviso muestra «Sin conexión» y que el trabajo en segundo plano no llega a intentar subir (RF-78). Si el dispositivo de prueba no permite simular esto de forma confiable, documentarlo en el PR como limitación de la prueba manual, igual que ya reconoce `spec.md`.
  Capturas en claro y oscuro de los 3 estados del aviso, en el scratchpad (nunca en el repo), según el punto 6 de «Verificación» de `CLAUDE.md`.
  Resultado (2026-10-11): pasos 1 y 3 verificados. El paso 2 no se hizo en el proyecto real (borrar el contador duplicaría INC-0001); RF-69 quedó cubierto por el Rules Playground (M01) y `siguienteNumero`. El error permanente del paso 3 se provocó con `allow create: if false` temporal en `incidencias`, porque revocar el perfil dispara RF-15 de la HU-01 y borra el pendiente antes de subirlo. El paso 4 (Wi-Fi sin internet) quedó opcional y no se ejecutó.

---

## Fase 8 — Segunda revisión (PR B de la HU-06)

Las tareas detalladas son T05 a T15 de `specs/006-asignacion-tecnico/tasks.md`. Rama `fix/sync-rechazos`, desde `main`.

- [x] **T23 · Spec, plan y tareas de la segunda revisión** (T05 de la spec 006). RF-68, RF-75 y RF-76 corregidos; RF-79 a RF-81 nuevos; `plan.md` §12.
- [ ] **T24 · `FAILED_PRECONDITION` transitorio y guarda de `actualizadoEn`** (T06, T07). RF-75.
- [ ] **T25 · Movimientos solo después de su incidencia** (T07, T08). RF-68.
- [ ] **T26 · Rechazo permanente: marca, movimientos y versión del servidor** (T09). RF-75.
- [ ] **T27 · Cambio local que pierde al bajar** (T10). RF-79.
- [ ] **T28 · Contar rechazos por incidencia y al cerrar sesión** (T11, T12). RF-76, RF-81.
- [ ] **T29 · «Entendido» en el aviso** (T13, T14). RF-76, RF-80.
- [ ] **T30 · Verificación** (T15): `./gradlew assembleDebug testDebugUnitTest lintDebug`, instrumentadas en un celular y prueba manual descrita en el PR.
