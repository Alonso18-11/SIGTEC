# Plan 005 — Correcciones de sincronización tras la revisión QA (HU-05)

> Basado en `spec.md` (ya aprobada) y `docs/constitution.md`. RF-58 a RF-78 ya existen en `main` (PR #10); este plan **no** los re-implementa. Solo cubre las cinco correcciones que la revisión QA del 2026-10-11 dejó pendientes y que la spec ya describe como comportamiento esperado: RF-69 (el contador se crea si falta), RF-75 (distinguir error permanente de transitorio), RF-76/77 (tercer estado del aviso), RF-78 (red validada antes de subir) y el reloj inyectado en `SincronizadorIncidencias`/`SincronizacionTiempoReal` (RNF). Todo lo demás de `core/sync` queda igual.

## 0. Alcance de este plan
- **Sí:** las 5 correcciones de arriba, con su prueba unitaria cada una, y los archivos compartidos que exigen.
- **No:** no se toca `ReglasSincronizacionTest` existente (RF-62, RF-63 ya probados), no se agrega sincronización periódica (fuera de alcance en la spec), no se cambia la regla de conflictos (`debeReemplazarLocal`/`debeGuardarMovimiento`), no se agregan dependencias nuevas.
- **«`hoy`/`ahora` como parámetro»:** dos soluciones distintas, según si la clase tiene un punto de llamada externo claro (sección 5, D-5):
  - `SincronizadorIncidencias.subirPendientes(ahora: Instant)`: parámetro simple, como `RegistroIncidenciaViewModel.guardar()` le pasa `Instant.now()` al caso de uso. Su único llamador (`SubirCambiosWorker`) se lo da.
  - `SincronizacionTiempoReal`: recibe un `Reloj` inyectado por Hilt (interfaz con `fun ahora(): Instant`), porque necesita la hora muchas veces a lo largo de un flujo reactivo de larga duración, sin un punto de llamada externo único que se la pueda pasar cada vez.
- **Pruebas:** siguen con JUnit 4, coroutines-test y dobles propios, `./gradlew testDebugUnitTest`, sin dependencias nuevas (principios 1 y 4). Donde la lógica depende de `FirebaseFirestoreException`, se construye la excepción real (tiene constructor público `(String, Code)`); no se necesita Mockito.

## 1. Archivos

Leyenda: 🆕 se crea · ✏️ se modifica · ✅ ya existe y no cambia · ⚠️ archivo compartido (requiere visto bueno según `CLAUDE.md`).

### Dominio / reglas puras (`core/sync`, Kotlin con el mínimo de Android posible)
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️ `core/sync/ReglasSincronizacion.kt` | Agrega 3 funciones puras: `siguienteNumero`, `esErrorPermanente`, `debeIntentarSubir` (sección 2). | RF-69, RF-75, RF-78 |
| 🆕 `core/sync/Reloj.kt` | Interfaz `Reloj` (`fun ahora(): Instant`) y su implementación real `RelojSistema` (llama a `Instant.now()`, el único lugar que lo hace). | RNF «Reloj inyectado» |

### Datos (`core/sync`, `core/database`, `core/domain/model`)
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️⚠️ `core/database/entity/IncidenciaEntity.kt` | Nuevo campo `necesitaAtencion: Boolean = false`. No cambia `toDomain()` (es metadato de sincronización, igual que `pendienteSincronizar`). | RF-75, RF-76 |
| ✏️⚠️ `core/database/entity/MovimientoEntity.kt` | Mismo campo `necesitaAtencion: Boolean = false`. | RF-75, RF-76 |
| ✏️⚠️ `core/database/SigtecDatabase.kt` | `version = 1` → `2` (campo nuevo en 2 entidades). Se declara en el PR; `fallbackToDestructiveMigration` ya cubre el desarrollo. | — |
| ✏️ `core/database/dao/IncidenciaDao.kt` | `marcarNecesitaAtencion(id)` (pone `pendienteSincronizar = 0` y `necesitaAtencion = 1`) y `observarNecesitanAtencion(): Flow<Int>`. | RF-75, RF-76 |
| ✏️ `core/database/dao/MovimientoDao.kt` | Mismas dos funciones que `IncidenciaDao`. | RF-75, RF-76 |
| ✏️⚠️ `core/domain/model/EstadoDatos.kt` | Nuevo campo `necesitanAtencion: Int`. | RF-76, RF-77 |
| ✏️ `core/sync/EstadoDatosRepositoryImpl.kt` | Agrega `MovimientoDao` al constructor y combina las dos nuevas cuentas en `necesitanAtencion`. | RF-76 |
| ✏️ `core/sync/SincronizadorIncidencias.kt` | (a) `crearConNumero` usa `siguienteNumero` y escribe con `tx.set` en vez de `tx.update` (RF-69). (b) `subir(...)` usa `esErrorPermanente` para marcar «necesita atención» en vez de reintentar sin límite (RF-75). (c) `subirPendientes(ahora: Instant)` ya no llama a `Instant.now()`. | RF-69, RF-75, reloj |
| ✏️ `core/sync/SubirCambiosWorker.kt` | Recibe `MonitorConexion` por constructor; antes de cada pasada comprueba `debeIntentarSubir(monitor.hayConexionAhora())` y, si no, `Result.retry()` sin intentar nada (RF-78). Pasa `Instant.now()` a `subirPendientes`. | RF-78, reloj |
| ✏️ `core/sync/MonitorConexion.kt` | Expone `fun hayConexionAhora(): Boolean` (reutiliza `conexionActual()`, ya privada) para la comprobación puntual del Worker. | RF-78 |
| ✏️ `core/sync/SincronizacionTiempoReal.kt` | Recibe `Reloj` por constructor; `registrarDescarga()` usa `reloj.ahora()` en vez de `Instant.now()`. | reloj |
| ✏️⚠️ `core/di/SincronizacionModule.kt` | `@Binds fun bindReloj(impl: RelojSistema): Reloj`. | reloj |
| ✏️⚠️ `firebase/firestore.rules` | `contadores/incidencias` agrega `allow create` (sección 6). Se publica a mano en la consola después de aprobarse; ningún agente lo hace. | RF-69 |

### Presentación
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️ `core/ui/components/AvisoConexion.kt` | Nueva función pura `textoAviso(estado): String?` con los 3 estados y la condición de ocultarse (sección 4); `AvisoConexionContenido` solo la llama y elige el ícono. Nuevo `@Preview` «Necesita atención». | RF-76, RF-77 |

### Pruebas (`app/src/test`)
| Archivo | RF |
|---|---|
| ✏️ `core/sync/ReglasSincronizacionTest.kt` | `siguienteNumero`, `esErrorPermanente`, `debeIntentarSubir` (RF-69, RF-75, RF-78) |
| ✏️ `core/sync/EstadoDatosRepositoryFalso.kt` | Agrega `fijarNecesitanAtencion(n)`, igual que ya hace `fijarPendientes(n)`. |
| 🆕 `core/ui/components/AvisoConexionTest.kt` | `textoAviso` en sus 3 estados y la condición de no mostrar nada (RF-76, RF-77). |
| 🆕 `core/sync/RelojFalso.kt` (doble) | `Reloj` fijo para pruebas futuras de `SincronizacionTiempoReal`/`SincronizadorIncidencias` (hoy no hay dobles de `FirebaseFirestore`/DAOs reales para probar esas clases completas; ver sección 8). |

### Documentación
| Archivo | Cambio |
|---|---|
| ✏️ `MEMORY.md` | Estado de la HU-05 tras las correcciones, y recordatorio de publicar `firestore.rules` a mano. |

## 2. Funciones puras de lógica

| # | Firma | Regla | RF |
|---|---|---|---|
| 2.1 | `siguienteNumero(ultimoActual: Long?): Long` | `(ultimoActual ?: 0L) + 1`. Documenta que un contador ausente se trata como si valiera 0, en vez de fallar. | RF-69 |
| 2.2 | `esErrorPermanente(error: Throwable): Boolean` | `true` solo si `error` es `FirebaseFirestoreException` con código `PERMISSION_DENIED`, `FAILED_PRECONDITION` o `INVALID_ARGUMENT`. Cualquier otro código (`UNAVAILABLE`, `DEADLINE_EXCEEDED`, `ABORTED`, `UNKNOWN`, `RESOURCE_EXHAUSTED`, `INTERNAL`…) o cualquier excepción que no sea de Firestore (sin conexión, timeout de red) se trata como transitorio: `false`. | RF-75 |
| 2.3 | `debeIntentarSubir(hayConexionValidada: Boolean): Boolean` | Hoy es la identidad (`= hayConexionValidada`); existe como regla nombrada, no como `if` suelto en el `Worker`, para que RF-78 tenga una prueba unitaria propia y un único lugar si la condición se complica más adelante. | RF-78 |
| 2.4 | `textoAviso(estado: EstadoDatos): String?` (presentación, `core/ui/components/AvisoConexion.kt`) | Mueve el `when` que hoy vive dentro de `AvisoConexionContenido` a una función no-`@Composable`, para poder probarla con JUnit sin Compose. Ver sección 4 para las 4 ramas (3 estados + nada). | RF-76, RF-77 |

`siguienteNumero` y `debeIntentarSubir` son pequeñas a propósito: no hay más regla de negocio detrás, pero nombrarlas evita un `if` suelto sin prueba y deja un lugar único si la regla crece (ej. una futura lista de códigos reintentables configurable).

## 3. Algoritmos en pseudocódigo

### 3.1 RF-69 — El contador se crea si falta
```
crearConNumero(local, ahora):
    transacción:
        documento = incidencias/{local.id}
        existente = leer(documento)
        si existente.existe: devolver existente.numero          # RF-70, sin cambios
        ultimoActual = leer(contador).campo("ultimo")            # null si el documento no existe
        numero = siguienteNumero(ultimoActual)                   # 2.1: trata null como 0
        escribir(contador, {"ultimo": numero})  # tx.set, UNA sola escritura: crea o actualiza según exista
        escribir(documento, local.aFirestore(numero))
        devolver numero
```
Importante para quien implemente: Firestore **no permite dos escrituras a la misma referencia dentro de una transacción** (ni siquiera «crear con 0» y luego «actualizar a 1» como dos pasos). Por eso la corrección no es «crear el documento en 0 y después incrementarlo»: es leer con el mismo valor por defecto que ya usaba el código (`?: 0L`) y escribir **una vez** con `tx.set(...)` en vez de `tx.update(...)`. `set` sobre un documento que no existe cuenta como `create` para las reglas de Firestore; sobre uno que ya existe, cuenta como `update`. Esa es la razón del cambio de regla en la sección 6.

### 3.2 RF-75 — Error permanente vs. transitorio al subir
```
subir(local):
    intentar:
        ... (sin cambios: crearConNumero o set con merge, luego asignarNumero y marcarSincronizada)
        devolver true
    si falla con CancellationException: relanzar
    si falla con e:
        registrar en Log.w (sin cambios)
        si esErrorPermanente(e):
            incidenciaDao.marcarNecesitaAtencion(local.id)   # pendienteSincronizar=0, necesitaAtencion=1
            devolver true     # ya no es "falta reintentar": se dejó de intentar a propósito
        si no:
            devolver false    # transitorio: sigue pendiente, el Worker lo reintenta (sin cambios)
```
Lo mismo para `subir(movimiento)`, con `movimientoDao.marcarNecesitaAtencion(movimiento.id)`.

### 3.3 RF-78 — Red validada antes de intentar subir
```
SubirCambiosWorker.doWork():
    si !debeIntentarSubir(monitor.hayConexionAhora()):
        devolver Result.retry()           # ni siquiera llama a subirPendientes()
    repetir hasta 3 veces:
        si !sincronizador.subirPendientes(Instant.now()): devolver Result.retry()
        si !sincronizador.hayPendientes(): devolver Result.success()
    devolver Result.success()
```
`MonitorConexion.hayConexionAhora()` es una lectura puntual de `NetworkCapabilities.NET_CAPABILITY_VALIDATED` (ya existe como `conexionActual()`, privada; solo se expone). No existe un `NetworkType` de WorkManager para «validada»: por eso la comprobación vive en el propio `Worker`, no en `Constraints` (alternativa descartada, ver D-4).

### 3.4 Reloj inyectado
```
// SincronizadorIncidencias: parámetro simple (un solo llamador, una vez por pasada)
subirPendientes(ahora: Instant):
    ... (igual que hoy, pero SincronizacionEntity(ejecutadaEn = ahora, ...))

// SincronizacionTiempoReal: Reloj inyectado (muchos eventos por el flujo, sin llamador único)
registrarDescarga():
    sincronizacionDao.registrar(SincronizacionEntity(ejecutadaEn = reloj.ahora(), ...))
```

## 4. Cómo se pinta en Compose (RF-76, RF-77)

`textoAviso(estado)` (2.4), en orden de prioridad:
1. `!estado.enLinea` → «Sin conexión» (+ «· mostrando lo descargado a las HH:mm» si hubo descarga + «· N cambios por enviar» si hay pendientes). **Sin cambios.**
2. `estado.necesitanAtencion > 0` → singular/plural con verbo concordado, igual que `textoSePerdera` en `TextosSalida.kt` (HU-01, D-4): `"1 cambio necesita tu atención"` / `"N cambios necesitan tu atención"`. **Nuevo.**
3. `estado.pendientesPorEnviar > 0` → «Enviando N cambios…». **Sin cambios.**
4. Si no, `null` → `AvisoConexionContenido` no pinta nada ni reserva espacio (RF-77). **Sin cambios.**

`AvisoConexionContenido`:
```
texto = textoAviso(estado) ?: return
icono = cuando:
    !estado.enLinea -> Icons.Outlined.WifiOff                 # sin cambios
    estado.necesitanAtencion > 0 -> Icons.Outlined.ErrorOutline  # nuevo (ya en material-icons-extended)
    si no -> Icons.Outlined.CloudUpload                        # sin cambios
Surface(...) { Row { Icon(icono); Text(texto) } }
```
Reglas de diseño sin cambios: solo `MaterialTheme.colorScheme`/`typography`, nunca hex ni `fontSize`; el ícono siempre va con texto (nunca solo color). Se agrega un tercer `@Preview` «Necesita atención», claro y oscuro, dentro de `SigtecTheme`.

## 5. Decisiones técnicas
| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D-1 | `tx.set(contador, mapOf("ultimo" to numero))` en vez de `tx.update(...)`, sin escribir el documento dos veces. | Firestore no permite dos escrituras a la misma referencia en una transacción; `set` resuelve `create` y `update` con una sola escritura y las reglas pueden distinguir cuál fue (sección 6). | Hacer `tx.set(contador, 0)` si no existe y luego `tx.update(contador, "ultimo", numero)`: falla con «more than one write for the same document» en el SDK de Firestore. |
| D-2 | `esErrorPermanente` solo mira `FirebaseFirestoreException` con 3 códigos concretos; todo lo demás es transitorio. | Es la lista mínima de errores que las reglas del servidor nunca van a dejar pasar reintentando (permiso, precondición, dato inválido). Ser conservador evita marcar «necesita atención» algo que en realidad era un corte de red. | Tratar cualquier `Exception` no reconocida como permanente: un error nuevo de la librería de Firestore (por ejemplo, un código agregado en una versión futura) dejaría de reintentarse sin motivo. |
| D-3 | Marcar «necesita atención» también apaga `pendienteSincronizar`. | Es justo lo que pide RF-75: «deja de reintentarlo automáticamente». Si se dejara `pendienteSincronizar = 1`, el próximo `Worker` lo volvería a intentar y a fallar igual, sin límite, que es el problema original. | Dejar `pendienteSincronizar` en 1 y que el `Worker` filtre los que «necesitan atención» antes de reintentar: duplica la regla de filtrado en dos lugares (el `Worker` y el DAO) sin ninguna ventaja. |
| D-4 | La comprobación de red validada vive en `SubirCambiosWorker.doWork()`, no en `Constraints`. | `androidx.work.NetworkType` no tiene un valor «validada»: solo `CONNECTED`, `UNMETERED`, `NOT_ROAMING`, `METERED`, `NOT_REQUIRED`. `Constraints.setRequiredNetworkType(CONNECTED)` se queda (ya filtra el caso obvio de «sin red»); la comprobación fina se hace al entrar a `doWork()`, con la misma fuente que ya usa el aviso de conexión (`MonitorConexion`). | Agregar `work-testing` o una librería de conectividad nueva para una `Constraint` personalizada: una dependencia nueva solo para esto, prohibido sin PR propio (principio 1). |
| D-5 | `SincronizadorIncidencias.subirPendientes` recibe `ahora: Instant` como parámetro simple; `SincronizacionTiempoReal` recibe un `Reloj` inyectado por Hilt. | El primero tiene un único llamador (`SubirCambiosWorker`, una vez por pasada): es el mismo patrón que usa `RegistroIncidenciaViewModel.guardar()` al llamarle `Instant.now()` al caso de uso. El segundo necesita la hora en un flujo reactivo que vive mientras haya sesión, sin un punto de llamada externo que la renueve en cada evento; un `Reloj` inyectado aísla la única llamada real a `Instant.now()` en `RelojSistema` y permite una prueba futura con un `Reloj` falso. | Usar `Reloj` inyectado también en `SincronizadorIncidencias` por uniformidad: agregaría una dependencia de Hilt donde un parámetro ya resuelve el caso y es más simple de leer y de probar. |
| D-6 | `necesitanAtencion` suma incidencias y movimientos rechazados por separado (no se deduplica como sí hace `pendientesPorEnviar`, HU-01 D-3). | Son rechazos independientes de Firestore: una incidencia y su propio movimiento pueden fallar por motivos distintos, y no hay garantía de que compartan causa. Cruzar ambas tablas para evitar un doble conteo en un aviso informativo no se justifica. | Deduplicar por `incidenciaId` como hace `pendientesPorEnviar`: añade una consulta más compleja para un caso raro (ambas rechazadas a la vez) que ya es, en sí mismo, una situación que requiere revisión manual. |
| D-7 | Editar de nuevo una incidencia u movimiento marcado «necesita atención» lo vuelve a dejar pendiente (porque `toEntity()`/`aMovimientoEntity()` no pasan `necesitaAtencion`, y su valor por defecto es `false`). | Es el comportamiento correcto: si la persona corrige el dato que el servidor rechazó, debe poder reintentarse con los datos nuevos, no quedar atascado para siempre. No se escribe código nuevo para lograrlo: es consecuencia del valor por defecto del campo. | Preservar `necesitaAtencion` explícitamente en cada `toEntity()`: obligaría a decidir también cuándo se limpia, repitiendo la regla en varios sitios de `feature/`. |

## 6. Cambio en `firebase/firestore.rules` (RF-69)

```diff
     match /contadores/incidencias {
       allow read: if autenticado();
+      // La primera incidencia de todo el proyecto crea el contador; después solo se incrementa de 1 en 1 (RF-69)
+      allow create: if autenticado() && request.resource.data.ultimo == 1;
       allow update: if autenticado()
         && request.resource.data.ultimo == resource.data.ultimo + 1;
     }
```
Por qué `== 1` y no `== 0`: con la corrección de la sección 3.1, la única escritura de creación ya incluye el incremento (se lee el contador ausente como 0 y se escribe `0 + 1`); nunca se guarda un `0` suelto. Limitar `create` a exactamente `1` evita que alguien cree el contador con un valor arbitrario (por ejemplo, muy alto, para robarse números, o negativo). Esto **no relaja** la regla existente de `update` (principio «no relajar la regla del servidor para que pase la app»).

**Pendiente, a cargo del usuario, no de un agente:** probar en el Rules Playground de la consola de Firebase tanto esta condición de `create` como la de `update` ya existente, y publicar el archivo corregido después. Ningún agente publica reglas.

## 7. Migración de Room

`SigtecDatabase.kt`: `version = 1` → `version = 2` (campo `necesitaAtencion` nuevo en `IncidenciaEntity` y `MovimientoEntity`). Se declara en el PR, como exige `CLAUDE.md`. `fallbackToDestructiveMigration(dropAllTables = true)` sigue cubriendo el desarrollo (borra lo no sincronizado; aceptado, ya documentado como «Pendiente conocido»/limitación de desarrollo).

## 8. Estrategia de pruebas

`./gradlew testDebugUnitTest`, JUnit 4 + coroutines-test + dobles propios, sin dependencias nuevas. `FirebaseFirestoreException` se instancia directamente en la prueba (constructor público `(mensaje, code)`); no hace falta mock.

| Prueba | Casos | RF |
|---|---|---|
| `ReglasSincronizacionTest` (existente, se agregan casos) | `siguienteNumero`: `null` → 1, `0L` → 1, `5L` → 6. `esErrorPermanente`: `PERMISSION_DENIED`, `FAILED_PRECONDITION`, `INVALID_ARGUMENT` → `true`; `UNAVAILABLE`, `DEADLINE_EXCEEDED`, `UNKNOWN` → `false`; una `IOException` genérica → `false`. `debeIntentarSubir`: `true`→`true`, `false`→`false`. Más los 5 casos ya existentes de RF-62/RF-63 (sin cambios). | RF-69, RF-75, RF-78 |
| `AvisoConexionTest` (nuevo) | Sin conexión sin descarga previa → «Sin conexión» sin la parte «mostrando…». Sin conexión con descarga y pendientes → ambas partes. Necesita atención con 1 → singular «necesita»; con 2 → plural «necesitan», con conexión y sin pendientes. Enviando (en línea, pendientes > 0, necesitanAtencion = 0) → «Enviando N cambios…». Nada que mostrar (en línea, pendientes = 0, necesitanAtencion = 0) → `null`. | RF-76, RF-77 |

**Sin prueba automática nueva, con su motivo (ya aceptado en la spec, sección «Criterios de finalización»):**
- El resto de `SincronizadorIncidencias` (transacción del contador real, `subir(...)` contra Firestore real) y de `SincronizacionTiempoReal` (listeners reales): dependen de `FirebaseFirestore` real, igual que antes de esta revisión. El cambio de reloj y de clasificación de errores se verifica por revisión de código (`grep Instant.now` no debe aparecer en ninguno de los dos archivos, salvo dentro de `RelojSistema`) y por prueba manual en el celular.
- `SubirCambiosWorker.doWork()`: requeriría `androidx.work:work-testing` (dependencia nueva, prohibida sin su propio PR). Se verifica a mano con modo avión y Wi-Fi sin salida real, si se puede simular.
- `RelojFalso.kt` queda como doble disponible para cuando exista una prueba completa de `SincronizacionTiempoReal`/`SincronizadorIncidencias` (no es parte del criterio de cierre de esta HU; ver `MEMORY.md`).

**Cierre:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde antes de abrir el PR.

## 9. Cobertura de RF (solo lo que cambia en esta revisión; el resto de RF-58 a RF-78 sigue igual que en `main`, sin prueba nueva)

| RF | Dónde vive | Prueba |
|---|---|---|
| RF-69 | `siguienteNumero`, `SincronizadorIncidencias.crearConNumero`, `firestore.rules` (`allow create`) | `ReglasSincronizacionTest` (la aritmética) + manual (la transacción real) + Rules Playground (a cargo del usuario) |
| RF-75 | `esErrorPermanente`, `SincronizadorIncidencias.subir(...)`, `IncidenciaDao`/`MovimientoDao.marcarNecesitaAtencion` | `ReglasSincronizacionTest` (la clasificación) + manual (el flujo completo contra Firestore) |
| RF-76 | `textoAviso`, `EstadoDatos.necesitanAtencion`, `EstadoDatosRepositoryImpl` | `AvisoConexionTest` |
| RF-77 | `textoAviso` (rama `null`) | `AvisoConexionTest` |
| RF-78 | `debeIntentarSubir`, `MonitorConexion.hayConexionAhora`, `SubirCambiosWorker.doWork` | `ReglasSincronizacionTest` (la regla) + manual (Wi-Fi sin salida real, difícil de simular en emulador) |
| Reloj (RNF) | `Reloj`/`RelojSistema`, `core/di/SincronizacionModule.kt` | Revisión de código (sin `Instant.now()` fuera de `RelojSistema`) |

## 10. Respeto a la constitución
1. **Stack cerrado:** sin dependencias nuevas; `libs.versions.toml` no cambia.
2. **Spec antes que código:** `spec.md` ya está aprobada con estas 5 correcciones descritas; este plan no le agrega RF nuevos, solo dice dónde viven.
3. **Lógica fuera de la pantalla:** `textoAviso` sale de `AvisoConexionContenido` a una función pura probable con JUnit; las 3 reglas nuevas (`siguienteNumero`, `esErrorPermanente`, `debeIntentarSubir`) son funciones puras en `ReglasSincronizacion.kt`, no `if` sueltos en clases de infraestructura.
4. **Toda regla tiene prueba:** las 3 funciones puras y `textoAviso` tienen prueba unitaria (sección 8); lo que depende de Firestore/WorkManager/ConnectivityManager reales se verifica a mano, igual que ya aceptaba la spec para el resto de RF-58 a RF-78.
5. **Datos protegidos:** sin cambios; esta revisión no toca sesión, huella ni biometría.
6. **Español:** nombres de funciones, campos y pruebas en español; solo sufijos de Android (`Dao`, `Worker`, `Entity`) en inglés.

## 11. Archivos compartidos que toca (requieren visto bueno)
- `firebase/firestore.rules` — agrega `allow create` sobre `contadores/incidencias` (sección 6). Se publica a mano en la consola después de aprobarse; no lo hace ningún agente.
- `core/database/entity/IncidenciaEntity.kt` y `core/database/entity/MovimientoEntity.kt` — campo nuevo `necesitaAtencion: Boolean = false`.
- `core/database/SigtecDatabase.kt` — `version` de 1 a 2.
- `core/domain/model/EstadoDatos.kt` — campo nuevo `necesitanAtencion: Int`.
- `core/di/SincronizacionModule.kt` — nuevo `@Binds` para `Reloj`.

No forman parte de la lista oficial de `CLAUDE.md` pero los usan otros módulos, por transparencia: `core/database/dao/IncidenciaDao.kt` y `core/database/dao/MovimientoDao.kt` (se agregan 2 funciones a cada uno; no se quita ni cambia ninguna existente, así que no debería romper a quien ya los usa).
