# Plan 006 — Asignación de trabajo con carga y límite de atención (HU-06)

> Basado en `spec.md` (aprobada el 2026-10-11) y `docs/constitution.md`. La HU necesita cambios en `core/sync` y `firebase/firestore.rules` además del módulo `feature/asignacion`, así que se entrega en seis PR pequeños. Cada PR tiene su rama, varios commits chicos con la HU al final del título, y la aprobación de la otra persona del equipo antes de fusionarse (`CLAUDE.md`, «Revisión cruzada»).
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/` (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (pruebas instrumentadas).

## 0. Adaptaciones de la plantilla
- **Ámbitos de los commits:** `CLAUDE.md` no tiene el ámbito `sync`. Los cambios de `core/sync` usan `core` (como el PR #17) y el de reglas usa `firebase`.
- **«`hoy`/`ahora` como parámetro»:** `AsignarIncidencia` recibe `ahora: Instant` y el `ViewModel` le pasa `Instant.now()`, igual que `RegistrarIncidencia` (spec 003).
- **Pruebas:** JUnit 4, coroutines-test y dobles propios con `./gradlew testDebugUnitTest`, sin dependencias nuevas. Lo que depende de `FirebaseFirestore` o `WorkManager` reales se verifica a mano, igual que en la spec 005.
- **Room:** ningún PR cambia una entidad, así que `SigtecDatabase.version` se queda en 2. Sí hay consultas nuevas en tres DAO (B, C y E), con su prueba instrumentada (`CLAUDE.md`, «Verificación», punto 3).

## 1. División en PR, dependencias y orden de fusión

| PR | Rama | Título del PR | Sale de | Depende de | Puede ir en paralelo con |
|---|---|---|---|---|---|
| A | `docs/hu06-asignacion-spec` | `docs(asignacion): spec, plan y tareas de la asignación (HU-06)` | `main` | — | B |
| B | `fix/sync-rechazos` | `fix(core): rechazos de sincronización sin movimientos huérfanos ni pérdidas en silencio (HU-05)` | `main` | — (su spec es la 005, que se corrige en el mismo PR) | A, C, E |
| C | `feat/sync-tecnicos` | `feat(core): bajar los técnicos desde usuarios (HU-06)` | `main` con A fusionado | A | B, E |
| D | `feat/sync-carga-tecnico` | `feat(core): mover la carga del técnico al subir una asignación (HU-06)` | rama de B (**apilada**) | A, B | C, E |
| E | `feat/asignacion-dominio` | `feat(asignacion): asignar y reasignar en Room respetando el límite (HU-06)` | `main` con A fusionado | A | B, C, D |
| F | `feat/asignacion-pantalla` | `feat(asignacion): pantalla de asignación conectada al detalle (HU-06)` | rama de E (**apilada**) | E; se fusiona **después** de C y D | — |

**Orden de fusión:** A → B, C y E en cualquier orden → D (después de B) → F (último).

**Por qué este orden:**
- B no necesita a A: corrige la HU-05 y actualiza su propia spec (005) en el mismo PR.
- D va apilada sobre B porque ambos tocan `SincronizadorIncidencias.kt` y porque D depende del rechazo corregido en B (RF-621): sin B, un límite excedido en el servidor dejaría un movimiento `ASIGNADA` huérfano.
- C y E no comparten archivos con D. C solo puede chocar con B en `SincronizacionTiempoReal.kt`; quien fusione segundo hace `git pull origin main` en su rama.
- F es el primer PR que deja asignar desde la app. Si se fusionara antes de D, las asignaciones subirían sin mover `incidenciasActivas` y la carga del servidor quedaría desfasada; antes de C, la lista de técnicos estaría vacía. Por eso F se fusiona al final aunque su rama salga de E.
- Si A sigue en revisión cuando se empiece C o E, sus ramas salen de la de A (ramas apiladas) y se rebasan sobre `main` cuando A se fusione.

## 2. Archivos y commits por PR

Leyenda: 🆕 se crea · ✏️ se modifica · ⚠️ compartido según la lista «Pregunta antes» de `CLAUDE.md` · 🔶 sensible fuera de esa lista (`core/sync`, DAO usados por otros módulos, archivos fuera del módulo de la HU).

### PR A — spec, plan y tareas (docs)
| Archivo | Cambio |
|---|---|
| 🆕 `specs/006-asignacion-tecnico/spec.md` | RF-601 a RF-625. |
| 🆕 `specs/006-asignacion-tecnico/plan.md` | Este plan. |
| 🆕 `specs/006-asignacion-tecnico/tasks.md` | Tareas por PR. |
| ✏️ `MEMORY.md` | Separa la línea «HU-06 y HU-07 (Nina)» y deja la de la HU-06 como «spec aprobada (RF-601 a RF-625); código en los PR B a F». Solo esa línea. |

- **Compartidos:** ninguno. **Room:** sin cambios.
- **Commits:**
  1. `docs(asignacion): spec de la asignación con carga y límite (HU-06)`
  2. `docs(asignacion): plan por PR con archivos compartidos y commits (HU-06)`
  3. `docs(asignacion): tareas de la asignación por PR (HU-06)`
  4. `docs(asignacion): estado de la spec de la HU-06 en MEMORY (HU-06)`

### PR B — rechazos de sincronización (problemas 1 a 5 del PR #17)
La spec 005 se corrige primero, dentro del mismo PR: cambian RF-68, RF-75 y RF-76, y se agregan RF-79 a RF-81. Esos números siguen el bloque de la HU-05 y nadie más los usa. El diseño está en la sección 4.8.

| Archivo | Responsabilidad | Problema |
|---|---|---|
| ✏️ `specs/005-sincronizacion/spec.md`, `plan.md`, `tasks.md` | Texto corregido y RF nuevos. | 1–5 |
| ✏️🔶 `core/sync/ReglasSincronizacion.kt` | Saca `FAILED_PRECONDITION` de los códigos permanentes; nueva `atencionAlBajar(local, remota)`. | 3, 4 |
| ✏️🔶 `core/sync/SincronizadorIncidencias.kt` | Sube solo los movimientos listos; ante un rechazo permanente marca la incidencia y sus movimientos y trae la versión del servidor. | 1, 3, 5 |
| ✏️🔶 `core/sync/SincronizacionTiempoReal.kt` | Al bajar, conserva la marca y marca el cambio local que pierde frente al servidor. | 3 |
| ✏️🔶 `core/sync/EstadoDatosRepositoryImpl.kt` | Cuenta las incidencias afectadas (no filas) y descarta los rechazos. | 1, 3 |
| ✏️🔶 `core/database/dao/IncidenciaDao.kt` | `marcarNecesitaAtencion(id, actualizadoEn): Int` con guarda, conteo por incidencia afectada, borrado y limpieza de rechazos. | 3, 5 |
| ✏️🔶 `core/database/dao/MovimientoDao.kt` | `listosParaSubir()`, `marcarNecesitaAtencionDe(incidenciaId)`, `borrarRechazados()`. | 1 |
| ✏️🔶 `core/domain/repository/EstadoDatosRepository.kt` | Nuevo `descartarRechazados()`. | 3 |
| 🆕🔶 `core/domain/usecase/DescartarCambiosRechazados.kt` | Caso de uso del botón «Entendido». | 3 |
| ✏️⚠️ `core/domain/usecase/EvaluarCierreSesion.kt`, `EvaluarIngresoDeOtraPersona.kt` | Cuentan también los rechazos como datos que se perderían. | 2 |
| ✏️🔶 `core/ui/components/AvisoConexion.kt` | Texto «N cambios no se pudieron enviar» y botón «Entendido». | 3 |
| ✏️ pruebas | `ReglasSincronizacionTest`, `EvaluarCierreSesionTest`, `EvaluarIngresoDeOtraPersonaTest`, `AvisoConexionTest`, 🆕 `DescartarCambiosRechazadosTest`, `EstadoDatosRepositoryFalso`; instrumentada 🆕 `MovimientoDaoTest` y casos nuevos en `IncidenciaDaoTest`. | 1–5 |

- **Compartidos ⚠️:** los casos de uso de sesión `EvaluarCierreSesion` y `EvaluarIngresoDeOtraPersona`. El resto es `core/sync` y `core`, que también exigen plan y visto bueno. La línea de la HU-05 en `MEMORY.md` es de Alonso: el cambio se le propone en el PR.
- **Room:** sin cambios de entidad (`necesitaAtencion` ya existe). `version` sigue en 2.
- **Commits:**
  1. `docs(core): corregir el rechazo permanente en la spec 005 (HU-05)`
  2. `fix(core): reintentar FAILED_PRECONDITION y no marcar un rechazo sobre una edición nueva (HU-05)`
  3. `fix(core): subir los movimientos solo después de su incidencia (HU-05)`
  4. `fix(core): ante un rechazo permanente, traer la versión del servidor y bloquear sus movimientos (HU-05)`
  5. `fix(core): marcar el cambio local que pierde frente al servidor al bajar (HU-05)`
  6. `fix(core): contar los cambios rechazados al cerrar sesión o al entrar otra persona (HU-05)`
  7. `fix(core): botón «Entendido» en el aviso para descartar los cambios rechazados (HU-05)`

### PR C — bajar los técnicos
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️🔶 `core/sync/MapeoFirestore.kt` | `tecnicoDesdeDatos(id, datos: Map<String, Any?>): TecnicoEntity?` (función pura) y `DocumentSnapshot.aTecnicoEntity()`, que la usa. | RF-619 |
| ✏️🔶 `core/sync/SincronizacionTiempoReal.kt` | `escuchar(...)` acepta una consulta; nueva escucha de `usuarios` con `perfil == "TECNICO"` y `guardarTecnicos(snap)`. | RF-619 |
| ✏️🔶 `core/database/dao/CatalogoDao.kt` | `borrarTecnicos(ids: List<String>)`. | RF-619 |
| 🆕 `core/sync/MapeoTecnicoTest.kt` (test) | Técnico válido, campos que faltan, enteros `Long` → `Int`. | RF-619 |
| ✏️ `CatalogoDaoTest.kt` (androidTest, 🆕 si no existe) | Guardar y borrar técnicos. | RF-619 |

- **Compartidos ⚠️:** ninguno. **Sensibles 🔶:** `core/sync` y `CatalogoDao`. `firestore.rules` no cambia: `usuarios` ya se lee con sesión.
- **Room:** solo una consulta nueva en `CatalogoDao`; la entidad `TecnicoEntity` no cambia y `version` sigue en 2.
- **Commits:**
  1. `feat(core): mapear un usuario técnico a la tabla tecnicos (HU-06)`
  2. `feat(core): borrar de Room los técnicos que dejan de serlo (HU-06)`
  3. `feat(core): escuchar los usuarios con perfil técnico (HU-06)`
  4. `docs(asignacion): RF-619 hecho en las tareas de la HU-06 (HU-06)`

### PR D — carga del técnico al subir y regla con `getAfter()` (apilado sobre B)
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️🔶 `core/sync/ReglasSincronizacion.kt` | `cambiosDeCarga(tecnicoServidor, tecnicoLocal): Map<String, Int>` y `cargaValida(actual, limite, delta): Boolean`. | RF-620, RF-621 |
| ✏️🔶 `core/sync/SincronizadorIncidencias.kt` | La subida de una incidencia que ya existe pasa a una transacción `actualizarConCarga(local)`; un límite excedido usa el rechazo de B. | RF-620, RF-621 |
| ✏️⚠️ `firebase/firestore.rules` | `update` de `incidencias`: cambio de técnico solo con la carga movida (`getAfter`); el técnico no cambia el técnico asignado. | RF-622, RF-623 |
| ✏️ `core/sync/ReglasSincronizacionTest.kt` (test) | Casos de `cambiosDeCarga` y `cargaValida`. | RF-620, RF-621 |

- **Compartidos ⚠️:** `firebase/firestore.rules`. La publican a mano el usuario o Alonso, después de probarla en el Rules Playground y de verificar en la consola que `incidenciasActivas` de cada técnico coincide con sus incidencias pendientes asignadas; ningún agente la publica.
- **Room:** sin cambios.
- **Commits:**
  1. `feat(core): calcular cuánto cambia la carga de cada técnico al subir (HU-06)`
  2. `feat(core): subir la incidencia y la carga del técnico en una sola transacción (HU-06)`
  3. `feat(firebase): exigir que la carga se mueva al cambiar el técnico asignado (HU-06)`
  4. `docs(asignacion): RF-620 a RF-623 hechos y reglas publicadas (HU-06)`

### PR E — dominio y datos de la asignación
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/asignacion/domain/ReglasAsignacion.kt` | Funciones puras (sección 3). | RF-602, RF-607, RF-609–RF-612, RF-614, RF-615, RF-625 |
| 🆕 `feature/asignacion/domain/AsignacionRepository.kt` | Interfaz: incidencia, técnicos, oficina del equipo, asignaciones sin subir y `asignar(...)`. | RF-601, RF-602, RF-613 |
| 🆕 `feature/asignacion/domain/AsignarIncidencia.kt` | Caso de uso (sección 4.2). | RF-608–RF-616, RF-625 |
| 🆕 `feature/asignacion/domain/ObservarOpcionesAsignacion.kt` | Resumen de la incidencia y técnicos con carga efectiva para la pantalla. | RF-601–RF-607 |
| 🆕 `feature/asignacion/data/AsignacionRepositoryImpl.kt` | Solo Room, `db.withTransaction`, `@IoDispatcher`, `catch` con `Log.w`. | RF-613, RF-616 |
| 🆕 `feature/asignacion/di/AsignacionModule.kt` | `@Binds` del repositorio. | — |
| ✏️🔶 `core/database/dao/IncidenciaDao.kt` | `observarAsignacionesSinSubir(): Flow<List<String>>` y `asignacionesSinSubir(tecnicoId): Int` (sección 4.4). | RF-602 |
| ✏️🔶 `core/database/dao/CatalogoDao.kt` | `tecnicoPorId(id)`. | RF-612 |
| ✏️⚠️ `core/domain/Resultado.kt` | `IncidenciaPorSincronizar`, `IncidenciaCerrada`, `YaAsignada(tecnicoNombre)`; `LimiteAtencionAlcanzado` ya existe. | RF-609–RF-611 |
| ✏️⚠️ `core/ui/MensajesError.kt` | Textos de esos tres errores (spec). | RF-609–RF-611 |
| 🆕 pruebas | `ReglasAsignacionTest`, `AsignarIncidenciaTest`, `ObservarOpcionesAsignacionTest`, doble `AsignacionRepositoryFalso`; caso instrumentado nuevo en `IncidenciaDaoTest`. | — |

- **Compartidos ⚠️:** `core/domain/Resultado.kt` y `core/ui/MensajesError.kt`. **Sensibles 🔶:** `IncidenciaDao` y `CatalogoDao`.
- **Room:** dos consultas nuevas, sin cambio de entidad; `version` sigue en 2.
- **Commits:**
  1. `feat(core): errores de asignación y sus mensajes (HU-06)`
  2. `feat(asignacion): reglas puras de carga, bloqueo, marca y movimiento (HU-06)`
  3. `feat(asignacion): caso de uso AsignarIncidencia con permiso, número y límite (HU-06)`
  4. `feat(core): consultar en Room las asignaciones que aún no subieron (HU-06)`
  5. `feat(asignacion): repositorio en Room con transacción y su @Binds (HU-06)`
  6. `feat(asignacion): opciones de técnicos con carga efectiva para la pantalla (HU-06)`

### PR F — pantalla y conexión con el detalle (apilado sobre E)
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/asignacion/presentation/AsignacionViewModel.kt` | `AsignacionUiState`, elección, guardado y evento de un solo uso `asignadaA`. | RF-605, RF-608–RF-617 |
| 🆕 `feature/asignacion/presentation/AsignacionScreen.kt` | `AsignacionRoute` + `AsignacionScreen` según el prototipo 06, con previews claro y oscuro. | RF-601–RF-607 |
| 🆕 `feature/asignacion/presentation/TextosAsignacion.kt` | `iniciales(nombre)`, texto del botón y de la línea de bandeja. | RF-601, RF-605 |
| ✏️ `feature/asignacion/presentation/AsignacionNavigation.kt` | Reemplaza `PantallaEnConstruccion`; al asignar, deja el aviso al detalle y vuelve. | RF-617 |
| ✏️🔶 `feature/incidencias/presentation/IncidenciasNavigation.kt` | `CLAVE_AVISO` pasa de `private` a `internal`, para que la asignación deje su aviso. | RF-617 |
| ✏️🔶 `feature/incidencias/presentation/DetalleIncidenciaScreen.kt` | Botón «Reasignar técnico» cuando ya hay uno. | RF-605 |
| 🆕 pruebas | `AsignacionViewModelTest`, `TextosAsignacionTest`. | — |

- **Compartidos ⚠️:** ninguno de la lista. `Rutas.kt` y `SigtecRaiz.kt` no cambian, porque la ruta y `asignacionGraph` ya existen. **Sensibles 🔶:** dos archivos del módulo `incidencias`, fuera del módulo de la HU.
- **Room:** sin cambios.
- **Commits:**
  1. `feat(asignacion): ViewModel con elección del técnico y guardado (HU-06)`
  2. `feat(asignacion): pantalla «Asignar incidencia» según el prototipo (HU-06)`
  3. `feat(asignacion): reemplazar la pantalla en construcción y volver al detalle con aviso (HU-06)`
  4. `feat(incidencias): «Reasignar técnico» en el detalle cuando ya hay uno (HU-06)`
  5. `docs(asignacion): spec de la HU-06 completa y MEMORY (HU-06)`

## 3. Funciones puras de lógica

| # | Firma (dónde) | Regla | RF |
|---|---|---|---|
| 3.1 | `cargaEfectiva(tecnico: Tecnico, sinSubir: Int): Tecnico` (`ReglasAsignacion`) | `tecnico.copy(incidenciasActivas = tecnico.incidenciasActivas + sinSubir)`. Así se reutilizan `disponible` y `cargaRelativa` del modelo sin tocarlo. | RF-602 |
| 3.2 | `bloqueoParaAsignar(incidencia: Incidencia): ErrorDominio?` | Cerrada → `IncidenciaCerrada`; sin número → `IncidenciaPorSincronizar`; si no, `null`. La usan el caso de uso y la pantalla: una sola regla. | RF-607, RF-609, RF-610 |
| 3.3 | `tipoDeAsignacion(tecnicoAnteriorId: String?): TipoMovimiento` | `null` → `ASIGNADA`; si no, `REASIGNADA`. | RF-613 |
| 3.4 | `detalleDeAsignacion(anterior: String?, nuevo: String, jefe: String): String` | «A Luis Ramírez, por Carlos Mendoza» / «De Ana Torres a Luis Ramírez, por Carlos Mendoza». | RF-615 |
| 3.5 | `armarOpciones(incidencia, tecnicos: List<Tecnico>, sinSubir: Map<String, Int>): List<OpcionTecnico>` | Ordena por nombre, aplica 3.1 y marca `disponible` y `esActual` (`tecnico.id == incidencia.tecnicoAsignadoId`). | RF-601–RF-604 |
| 3.6 | `cambiosDeCarga(tecnicoServidor: String?, tecnicoLocal: String?): Map<String, Int>` (`ReglasSincronizacion`) | Iguales → vacío; si no, `tecnicoServidor → -1` (si no es `null`) y `tecnicoLocal → +1` (si no es `null`). | RF-620 |
| 3.7 | `cargaValida(actual: Long, limite: Long, delta: Int): Boolean` | `actual + delta in 0..limite`. | RF-621 |
| 3.8 | `tecnicoDesdeDatos(id: String, datos: Map<String, Any?>): TecnicoEntity?` (`MapeoFirestore`) | `nombre` (String), `limiteAtencion` y `incidenciasActivas` (Long → Int; si falta `incidenciasActivas`, 0). Si falta otro campo, `null`. | RF-619 |
| 3.9 | `atencionAlBajar(local: IncidenciaEntity?, remota: IncidenciaEntity): Boolean` (PR B) | `true` si el local ya tenía la marca, o si tenía un cambio sin subir y la remota lo reemplaza. | RF-79 (005), RF-624 |
| 3.10 | `iniciales(nombre: String): String` (`TextosAsignacion`) | Primeras letras de las dos primeras palabras, en mayúsculas («Luis Ramírez» → «LR»). | RF-601 |
| 3.11 | `marcaAlAsignar(actual: MarcaSeguimiento): MarcaSeguimiento` (`ReglasAsignacion`) | `ESPERANDO_REPUESTO` → `ESPERANDO_REPUESTO` (la pieza sigue en camino); `SIN_ASIGNAR`, `ASIGNADA` o `EN_ATENCION` → `ASIGNADA`. `CERRADA` no llega aquí: la frena 3.2. | RF-614, RF-625 |

`ReglasAsignacion.kt` solo importa `core/domain`: es Kotlin puro (principio 3).

## 4. Algoritmos en pseudocódigo

### 4.1 Carga efectiva (RF-602)
```
sinSubir = repo.observarAsignacionesSinSubir()          // lista de tecnicoAsignadoId (4.4)
             .map { ids -> ids.groupingBy { it }.eachCount() }
opciones = armarOpciones(incidencia, repo.observarTecnicos(), sinSubir)   // 3.5
```

### 4.2 `AsignarIncidencia(incidenciaId, tecnicoId, ahora)` (RF-608 a RF-616, RF-625)
```
jefe = verificarPermiso(ASIGNAR_INCIDENCIA)            // SinPermiso si falla (RF-608)
incidencia = repo.incidencia(incidenciaId) ?: Desconocido("incidencia no encontrada")
bloqueoParaAsignar(incidencia)?.let { return Error(it) }          // RF-609, RF-610
tecnico = repo.tecnico(tecnicoId) ?: Desconocido("técnico no encontrado")
si incidencia.tecnicoAsignadoId == tecnico.id: Error(YaAsignada(tecnico.nombre))   // RF-611
efectivo = cargaEfectiva(tecnico, repo.asignacionesSinSubir(tecnico.id))
si !efectivo.disponible: Error(LimiteAtencionAlcanzado(tecnico.nombre))            // RF-612
anterior = incidencia.tecnicoAsignadoNombre
actualizada = incidencia.copy(marca = marcaAlAsignar(incidencia.marca),             // 3.11: RF-614, RF-625
                              tecnicoAsignadoId = tecnico.id, tecnicoAsignadoNombre = tecnico.nombre)
movimiento = Movimiento(UUID, incidencia.id, tipoDeAsignacion(incidencia.tecnicoAsignadoId),
                        autorNombre = jefe.nombre, fecha = ahora,
                        detalle = detalleDeAsignacion(anterior, tecnico.nombre, jefe.nombre))
repo.asignar(actualizada, movimiento, ahora)  -> Exito(actualizada) | su Error (RF-616)
```
Orden de validaciones: permiso → incidencia → bloqueo → técnico → mismo técnico → límite → guardar.

### 4.3 `AsignacionRepositoryImpl.asignar` (RF-613)
```
withContext(io) {
  try {
    db.withTransaction {
      incidenciaDao.guardar(actualizada.toEntity(pendienteSincronizar = true, actualizadoEn = ahora))
      movimientoDao.guardar(movimiento.toEntity(pendienteSincronizar = true))
    }
    Exito(Unit)
  } catch (CancellationException) { relanzar }
    catch (e) { Log.w(TAG, "Error al asignar", e); Error(Desconocido(e.message)) }
}
```
`actualizadoEn` se pasa explícito (no el `Instant.now()` por defecto de `toEntity`). `toEntity` deja `necesitaAtencion` en `false`, así que una asignación nueva reemplaza un rechazo anterior (D-7 de la spec 005).

### 4.4 Asignaciones que aún no subieron (RF-602)
```sql
SELECT i.tecnicoAsignadoId FROM incidencias i
WHERE i.tecnicoAsignadoId IS NOT NULL AND i.marca != 'CERRADA' AND i.pendienteSincronizar = 1
  AND EXISTS (SELECT 1 FROM movimientos m
              WHERE m.incidenciaId = i.id AND m.pendienteSincronizar = 1
                AND m.tipo IN ('ASIGNADA', 'REASIGNADA'))
```
Una incidencia pendiente solo cuenta si su asignación a ese técnico tampoco subió, es decir, si tiene un movimiento de asignación pendiente. Así no se cuenta dos veces una incidencia ya asignada en el servidor que esté pendiente por otro motivo. Que el movimiento siga pendiente hasta que su incidencia suba lo garantiza el PR B (4.8, punto 1).

### 4.5 Bajar los técnicos (RF-619, PR C)
```
escuchar("usuarios", firestore.collection("usuarios").whereEqualTo("perfil", "TECNICO"), ::guardarTecnicos)

guardarTecnicos(snap):
    borrar = snap.documentChanges.filter { REMOVED }.map { it.document.id }   // dejó de ser técnico o se borró
    nuevos = snap.documentChanges.filter { != REMOVED }
                 .mapNotNull { tecnicoDesdeDatos(it.document.id, it.document.data) }   // 3.8
    si nuevos no está vacío: catalogoDao.guardarTecnicos(nuevos)
    si borrar no está vacío: catalogoDao.borrarTecnicos(borrar)
```
`escuchar` pasa a recibir una `Query` (una `CollectionReference` también lo es) y un nombre para el log. Las tres llamadas existentes pasan su colección sin cambiar de comportamiento.

### 4.6 Transacción de la carga (RF-620, RF-621, PR D)
```
subir(local):
    numero = si local.numero == null: crearConNumero(local)          // sin cambios: nace SIN_ASIGNAR
             si no: si actualizarConCarga(local) == CARGA_RECHAZADA: rechazar(local) (4.8); devolver true
    ... (asignarNumero y marcarSincronizada, sin cambios)

actualizarConCarga(local): ResultadoSubida = firestore.runTransaction { tx ->
    remota = tx.get(incidencias/{local.id})
    cambios = cambiosDeCarga(remota.getString("tecnicoAsignadoId"), local.tecnicoAsignadoId)   // 3.6
    perfiles = cambios.keys.associateWith { tx.get(usuarios/it) }    // todas las lecturas antes de escribir
    si algún (id, delta) no cumple cargaValida(perfiles[id].incidenciasActivas, perfiles[id].limiteAtencion, delta):
        devolver CARGA_RECHAZADA                                     // sin escrituras: no se aplica nada
    para cada (id, delta): tx.update(usuarios/id, "incidenciasActivas", actual + delta)
    tx.set(incidencias/{local.id}, local.aFirestore(local.numero), SetOptions.merge())
    devolver SUBIDA
}.await()
```
Si la incidencia no cambia de técnico, `cambios` queda vacío y la transacción solo guarda la incidencia, como hacía `set(merge)`. Las actualizaciones del técnico de la HU-08 pasan por el mismo camino sin tocar la carga.

**Dependencia para Alonso (HU-08):** al cerrar, esta misma transacción debe restar 1 a la carga del técnico cuando la marca pasa a `CERRADA`. Basta con ampliar `cambiosDeCarga` para que reciba también si la incidencia estaba activa antes y después. La regla de 4.7 debe exigir esa resta en la misma escritura. No se crea una operación aparte (spec, «Fuera de alcance»).

### 4.7 Reglas de Firestore (RF-622, RF-623, PR D)
```diff
     match /incidencias/{id} {
       allow read: if autenticado();
       allow create: if autenticado()
         && request.resource.data.registradoPorId == request.auth.uid
         && request.resource.data.marca == 'SIN_ASIGNAR';
-      // La modifica el jefe (asignar, clasificar) o el técnico asignado (atender, cerrar)
-      allow update: if esJefe()
-        || (esTecnico() && resource.data.tecnicoAsignadoId == request.auth.uid);
+      // La modifica el jefe (asignar, clasificar) o el técnico asignado (atender, cerrar).
+      // Cambiar de técnico mueve la carga de los dos en la misma escritura: RF-06 en el servidor (RF-622).
+      // El técnico no puede cambiar el técnico asignado (RF-623).
+      allow update: if (esJefe() && (!cambiaTecnico() || (resource.data.marca != 'CERRADA' && cargaMovida())))
+        || (esTecnico() && tecnicoAntes() == request.auth.uid && !cambiaTecnico());
       allow delete: if false;
     }
```
Con estas funciones de apoyo (en la sección «Funciones de apoyo»):
```
function tecnicoAntes()   { return resource.data.get('tecnicoAsignadoId', null); }
function tecnicoDespues() { return request.resource.data.get('tecnicoAsignadoId', null); }
function cambiaTecnico()  { return tecnicoDespues() != tecnicoAntes(); }
function cargaDe(uid)        { return get(/databases/$(database)/documents/usuarios/$(uid)).data.incidenciasActivas; }
function cargaDespuesDe(uid) { return getAfter(/databases/$(database)/documents/usuarios/$(uid)).data.incidenciasActivas; }
function cargaMovida() {
  return (tecnicoDespues() == null || cargaDespuesDe(tecnicoDespues()) == cargaDe(tecnicoDespues()) + 1)
      && (tecnicoAntes() == null || cargaDespuesDe(tecnicoAntes()) == cargaDe(tecnicoAntes()) - 1);
}
```
La regla de `usuarios` no cambia: ya exige que solo cambie `incidenciasActivas` y que quede entre 0 y `limiteAtencion`. La suma de ambas reglas es RF-06 en el servidor. En el peor caso son 5 lecturas (`perfil()` y 4 de carga), por debajo del límite de 10 de las reglas.

### 4.8 Rechazos corregidos (PR B; lo que esta HU necesita de la spec 005)
1. **Movimientos después de su incidencia (problema 1).** `MovimientoDao.listosParaSubir()` devuelve los movimientos pendientes cuya incidencia local ya no está pendiente ni marcada, o que no existe en el celular:
   ```sql
   SELECT m.* FROM movimientos m LEFT JOIN incidencias i ON i.id = m.incidenciaId
   WHERE m.pendienteSincronizar = 1
     AND (i.id IS NULL OR (i.pendienteSincronizar = 0 AND i.necesitaAtencion = 0))
   ```
   Si la incidencia falla de forma transitoria, su movimiento espera a la siguiente pasada.
2. **Rechazo permanente (problemas 3 y 5).** En una transacción de Room, `marcarNecesitaAtencion(id, local.actualizadoEn)` (que no hace nada si la incidencia se editó mientras subía) y, si marcó, `marcarNecesitaAtencionDe(id)` para sus movimientos pendientes. Después se lee `incidencias/{id}` del servidor y, si existe, se guarda esa versión con `necesitaAtencion = true`: el servidor gana y el celular lo muestra.
3. **Cambio local que pierde al bajar (problema 3, RF-624).** En `guardarIncidencias`, cuando `debeReemplazarLocal` decide reemplazar, se guarda `remota.copy(necesitaAtencion = atencionAlBajar(local, remota))` (3.9). Si el local tenía un cambio sin subir, sus movimientos pendientes también se marcan. La regla de conflictos (`debeReemplazarLocal`) no cambia.
4. **Contar los rechazos (problema 2).** `necesitanAtencion` pasa a contar incidencias afectadas (unión de los ids de incidencias marcadas y de los `incidenciaId` de movimientos marcados), igual que «cambios sin enviar» cuenta incidencias (HU-01, D-3). `EvaluarCierreSesion` y `EvaluarIngresoDeOtraPersona` usan `pendientesPorEnviar + necesitanAtencion`.
5. **«Entendido» (problema 3).** El aviso muestra «1 cambio no se pudo enviar» / «N cambios no se pudieron enviar» y un botón «Entendido». Al pulsarlo, `DescartarCambiosRechazados` borra los movimientos marcados (nunca llegaron al servidor), borra las incidencias marcadas sin número (nunca existieron en el servidor) y quita la marca al resto (ya tienen la versión del servidor).
6. **`FAILED_PRECONDITION` (problema 4)** sale de `CODIGOS_PERMANENTES`: dentro de una transacción indica contención y se reintenta.

## 5. Cómo se pinta en Compose (PR F, prototipo 06)
- `Scaffold(contentWindowInsets = WindowInsets(0))` con `TopAppBar(title = "Asignar incidencia", windowInsets = WindowInsets(0))` y flecha de volver.
- **Resumen** (`OutlinedCard`): primera línea «INC-0127 · IMP-RRHH-002 · Recursos Humanos» (`labelMedium`, `onSurfaceVariant`; la oficina se omite si el equipo no está en el catálogo), la descripción (`titleMedium`), `EtiquetaIncidencia(marca, critica, diasPendiente)` y, si existen, el tipo y la prioridad con las `etiqueta()` de `TextosIncidencia`.
- **Encabezado:** «Elige un técnico» (`titleSmall`) a la izquierda y «Incidencias activas / límite» (`labelSmall`, `onSurfaceVariant`) a la derecha.
- **Tarjeta de técnico** (`Surface` con `Modifier.selectable(role = Role.RadioButton)` y alto mínimo `AreaTactilMinima`):
  - avatar circular con `iniciales()` sobre `secondaryContainer`;
  - nombre (`bodyLarge`) y «3 / 5» (`labelLarge`);
  - `LinearProgressIndicator(progress = cargaRelativa)` en `primary`, o en `error` si no está disponible;
  - línea de estado: «Disponible» en `primary`, o candado + «Alcanzó su límite de atención» en `error`, o «Asignado ahora» en `onSurfaceVariant`;
  - `RadioButton` al final.
  - Una tarjeta elegida lleva borde `primary` sobre `primaryContainer`. Una no disponible o la actual va sobre `surfaceVariant` con el contenido en `onSurfaceVariant` y no responde al toque.
- **Lista vacía (RF-606)** y **bloqueo (RF-607):** un texto centrado en `bodyLarge`, `onSurfaceVariant`, en lugar de la lista o encima de ella.
- **Barra inferior:** `HorizontalDivider` y `Button` de ancho completo con «Asignar a <nombre>» o «Reasignar a <nombre>»; sin elección dice «Asignar». Debajo, «La incidencia aparecerá en la bandeja de <nombre>» (`bodySmall`). El error, si lo hay, va debajo con `error.mensaje()` en `error`.
- Solo roles de `MaterialTheme.colorScheme`, estilos de `typography` y `Espaciado.*`; nunca hexadecimales ni `fontSize`.
- **Previews (claro y oscuro):** lista con un técnico al límite y otro elegido; reasignación con el técnico actual; sin técnicos descargados; incidencia por sincronizar.
- **ViewModel:**
  ```
  data class AsignacionUiState(
      val cargando: Boolean = true,
      val opciones: OpcionesAsignacion? = null,
      val elegidoId: String? = null,
      val guardando: Boolean = false,
      val error: ErrorDominio? = null,
      val asignadaA: String? = null          // evento de un solo uso para el aviso del detalle
  ) { val elegido; val puedeAsignar; val esReasignacion }   // derivados
  ```
  Combina `ObservarOpcionesAsignacion(incidenciaId)` (id leído de `SavedStateHandle`) con el estado local, usando `stateIn(viewModelScope, WhileSubscribed(5_000), …)`. Los manejadores son `alElegir(id)`, `alAsignar()` (pasa `Instant.now()`) y `alNavegarAlDetalle()`. Si el técnico elegido desaparece o deja de estar disponible, `elegido` vuelve a `null`.
- **Navegación:** `asignadaA != null` → `previousBackStackEntry?.savedStateHandle?.set(CLAVE_AVISO, "Asignada a $nombre")` y `popBackStack()`. El detalle ya lee y retira ese aviso al volver a componerse.

## 6. Decisiones técnicas
| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D-1 | Seis PR, con F fusionado al final. | PR chicos y revisables; nunca hay en `main` una forma de asignar que desfase la carga del servidor. | Un solo PR: demasiado grande para la revisión cruzada. F antes que D: la carga del servidor se desfasaría con cada asignación. |
| D-2 | Carga efectiva = la del servidor + asignaciones con movimiento de asignación pendiente (4.4). | Se compara con el mismo número que valida la regla del servidor, sin cambiar ninguna entidad. | Contar en Room todas las incidencias activas del técnico: ignora el contador que valida el servidor. Agregar a `IncidenciaEntity` el técnico confirmado por el servidor: cambia una entidad, sube `version` y borra lo no sincronizado. |
| D-3 | No dejar asignar una incidencia sin número (RF-610). | El servidor solo acepta crear incidencias sin asignar; es la solución más simple y no toca `core/sync`. | Que `core/sync` cree la incidencia sin asignar y luego la actualice: dos escrituras, más estados intermedios y otro camino de error. |
| D-4 | El técnico y el jefe van en el `detalle` del movimiento (RF-615). | El detalle ya pinta `detalle ?: autorNombre` bajo el título; no cambia el modelo ni el mapeo de Firestore. | Campo nuevo `tecnicoNombre` en `Movimiento`: toca `core/domain/model`, la entidad, `MapeoFirestore` y `version`. |
| D-5 | Incidencia o técnico inexistentes en Room → `Desconocido`. | Solo pasa si los datos cambian entre la pantalla y el toque; no justifica errores nuevos en un archivo compartido. | Un `ErrorDominio` por caso: más texto que mantener para casos que la UI no produce. |
| D-6 | Transacción del cliente + regla con `getAfter()` (RF-620, RF-622). | Costo cero (sin Cloud Functions) y atómico: o se mueven la incidencia y las dos cargas, o nada. | Cloud Function que recalcule la carga: no existe en el plan Spark. `FieldValue.increment` fuera de la transacción: no valida el límite ni es atómico con la incidencia. |
| D-7 | El límite excedido se detecta dentro de la transacción y se devuelve como resultado, sin lanzar una excepción. | La tarea de Firestore puede envolver una excepción propia y su clasificación como permanente sería frágil; devolver un valor sin escrituras no aplica nada. | Dejar que la regla rechace (`PERMISSION_DENIED`): también funciona, pero gasta una escritura fallida y no distingue el motivo en el log. |
| D-8 | Bajar los técnicos con la consulta `perfil == TECNICO` y borrar los `REMOVED`. | Quien deja de ser técnico llega como `REMOVED` y desaparece; no se bajan datos de jefes ni de personal de sistemas. | Bajar todos los usuarios y filtrar en el celular. Solo guardar, sin borrar: dejaría técnicos fantasma elegibles. |
| D-9 | Mapeo de técnicos con una función pura desde el `Map` del documento. | Se prueba en la JVM sin clases de Firebase (`MEMORY.md`). | Mapear solo con `DocumentSnapshot`, como los equipos: quedaría sin prueba. |
| D-10 | Ante un rechazo, el servidor gana y queda una marca hasta «Entendido» (PR B). | Nada se pierde en silencio, el celular muestra la verdad del servidor y la persona tiene una salida. | Conservar el cambio local hasta que la persona elija: exige una pantalla de resolución de conflictos, fuera de alcance de la spec 005. Borrar en silencio: es el problema 3. |
| D-11 | El aviso al volver al detalle reutiliza `CLAVE_AVISO` (pasa a `internal`). | El detalle ya lee y retira ese aviso; no hace falta otro mecanismo. | Copiar la cadena `"aviso"` en otro módulo: se rompería en silencio si cambia. |
| D-12 | Al reasignar, Asignada o En atención vuelven a Asignada, y Esperando repuesto se conserva (RF-614, RF-625; aprobado al revisar la spec). | El técnico nuevo todavía no inicia la atención, pero si la incidencia espera una pieza, esa pieza sigue en camino aunque cambie el técnico. | Volver siempre a Asignada: ocultaría que hay un repuesto en camino. Conservar siempre la marca anterior: un técnico que no ha empezado figuraría «En atención». |

## 7. Estrategia de pruebas
`./gradlew testDebugUnitTest`, JUnit 4, coroutines-test, `MainDispatcherRule` y dobles propios (`SesionRepositoryDemo`, el nuevo `AsignacionRepositoryFalso`, `EstadoDatosRepositoryFalso`). Sin dependencias nuevas.

| Prueba | Casos | RF |
|---|---|---|
| `ReglasAsignacionTest` (E) | Carga efectiva (3+1 de 5 disponible; 4+1 de 5 no); bloqueo de cerrada, sin número y ninguno; `marcaAlAsignar` desde Sin asignar, Asignada y En atención → Asignada, y desde Esperando repuesto → Esperando repuesto; tipo `ASIGNADA`/`REASIGNADA`; texto del detalle con y sin anterior; `armarOpciones` ordena, marca la actual y la no disponible. | RF-602–RF-604, RF-607, RF-609, RF-610, RF-613–RF-615, RF-625 |
| `AsignarIncidenciaTest` (E) | Sin sesión y técnico (sin permiso) → `SinPermiso`; cerrada; sin número; mismo técnico; límite alcanzado contando una asignación sin subir; camino feliz (`ASIGNADA`, marca, técnico, `ahora` fijo en el movimiento); reasignación desde En atención (`REASIGNADA`, queda Asignada); reasignación desde Esperando repuesto (`REASIGNADA`, conserva Esperando repuesto); Room falla → error y nada guardado. | RF-608–RF-616, RF-625 |
| `ObservarOpcionesAsignacionTest` (E) | La carga cambia al agregar una asignación sin subir; la incidencia cerrada emite su bloqueo; sin técnicos → lista vacía. | RF-601, RF-602, RF-606, RF-607 |
| `AsignacionViewModelTest` (F) | Botón deshabilitado sin elección, con técnico no disponible, con bloqueo o guardando; elegir y asignar emite `asignadaA` y se limpia con `alNavegarAlDetalle()`; error de límite queda en `error` y se puede reintentar; si el elegido deja de estar disponible, la elección se borra. | RF-605, RF-607, RF-612, RF-616, RF-617 |
| `TextosAsignacionTest` (F) | Iniciales de uno, dos y tres nombres; texto del botón para asignar y reasignar. | RF-601, RF-605 |
| `MapeoTecnicoTest` (C) | Datos completos; falta `limiteAtencion` → `null`; falta `incidenciasActivas` → 0. | RF-619 |
| `ReglasSincronizacionTest` (D, B) | `cambiosDeCarga`: sin cambio, asignar, reasignar, quitar. `cargaValida`: dentro, en el límite, sobre el límite, bajo 0. B: `atencionAlBajar` y `FAILED_PRECONDITION` transitorio. | RF-620, RF-621, RF-624 |
| Instrumentadas en celular (`connectedDebugAndroidTest`) | `IncidenciaDaoTest`: asignaciones sin subir (cuenta solo con movimiento de asignación pendiente). `CatalogoDaoTest`: guardar y borrar técnicos. B: `MovimientoDaoTest` (listos para subir, marcar y borrar). | RF-602, RF-619 |

**Verificado a mano, no automatizado (dependen de Firestore o WorkManager reales):** la escucha de `usuarios` (RF-619); la transacción real de la carga (RF-620, RF-621), revisando la consola; las reglas en el Rules Playground (RF-622, RF-623); la llegada al técnico (RF-618); la parte de RF-624 que se puede probar sin «Iniciar atención» (Rules Playground y el rechazo de RF-621; el resto en la HU-08, según la spec); y la pantalla en el celular con `adb` en claro y oscuro (`CLAUDE.md`, «Verificación», punto 6).

**Cierre de cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde.

## 8. Cobertura de RF
| RF | Dónde vive | Prueba |
|---|---|---|
| RF-601 | `ObservarOpcionesAsignacion`, `AsignacionScreen` | `ObservarOpcionesAsignacionTest`, `TextosAsignacionTest`, preview + manual |
| RF-602 | `cargaEfectiva`, `IncidenciaDao.observarAsignacionesSinSubir` | `ReglasAsignacionTest`, `ObservarOpcionesAsignacionTest`, `IncidenciaDaoTest` |
| RF-603, RF-604 | `armarOpciones`, `AsignacionScreen` | `ReglasAsignacionTest`, preview |
| RF-605 | `AsignacionUiState.puedeAsignar`, `TextosAsignacion` | `AsignacionViewModelTest`, `TextosAsignacionTest` |
| RF-606 | `AsignacionScreen` | `ObservarOpcionesAsignacionTest`, preview |
| RF-607 | `bloqueoParaAsignar` | `ReglasAsignacionTest`, `AsignacionViewModelTest` |
| RF-608–RF-612 | `AsignarIncidencia` | `AsignarIncidenciaTest` |
| RF-613 | `AsignarIncidencia`, `AsignacionRepositoryImpl` | `AsignarIncidenciaTest` + manual |
| RF-614, RF-625 | `marcaAlAsignar`, `AsignarIncidencia` | `ReglasAsignacionTest`, `AsignarIncidenciaTest` |
| RF-615 | `tipoDeAsignacion`, `detalleDeAsignacion`, `AsignarIncidencia` | `ReglasAsignacionTest`, `AsignarIncidenciaTest` |
| RF-616 | `AsignacionRepositoryImpl` (catch) | `AsignarIncidenciaTest` (repositorio falso que falla) |
| RF-617 | `AsignacionNavigation`, `asignadaA` | `AsignacionViewModelTest` + manual |
| RF-618 | `IncidenciaDao.observarBandeja` (ya existe) + sincronización | manual (filtro por técnico y detalle en el celular del técnico) |
| RF-619 | `tecnicoDesdeDatos`, `SincronizacionTiempoReal`, `CatalogoDao` | `MapeoTecnicoTest`, `CatalogoDaoTest` + manual |
| RF-620, RF-621 | `cambiosDeCarga`, `cargaValida`, `SincronizadorIncidencias` | `ReglasSincronizacionTest` + manual (consola) |
| RF-622, RF-623 | `firebase/firestore.rules` | Rules Playground (usuario) |
| RF-624 | PR B (4.8) + RF-622/RF-623 | `ReglasSincronizacionTest` (`atencionAlBajar`) + Rules Playground + rechazo de RF-621 en el celular; completo en la HU-08 |

## 9. Respeto a la constitución
1. **Stack cerrado:** sin dependencias nuevas; `libs.versions.toml` no cambia.
2. **Spec antes que código:** esta spec se aprueba en el PR A, antes de C a F. El PR B corrige primero la spec 005 y después su código, en el mismo PR.
3. **Lógica fuera de la pantalla:** permiso, bloqueos, carga efectiva, marca al asignar y tipo de movimiento viven en `feature/asignacion/domain` (Kotlin puro); la pantalla solo dibuja y llama lambdas.
4. **Toda regla tiene prueba:** cada función pura, el caso de uso, el ViewModel y las consultas nuevas tienen prueba (sección 7). Lo que depende de Firestore real se verifica a mano, igual que en la spec 005.
5. **Datos protegidos:** no toca sesión, huella, contraseñas ni claves. La regla de `incidencias` se endurece (RF-623) y no se relaja ninguna.
6. **Español:** clases, funciones, textos y pruebas en español; solo los sufijos de Android (`ViewModel`, `Screen`, `Dao`, `Entity`) en inglés.

## 10. Resumen: archivos compartidos y Room por PR (requieren visto bueno)
| PR | Compartidos ⚠️ («Pregunta antes» de `CLAUDE.md`) | Sensibles 🔶 | Room |
|---|---|---|---|
| A | ninguno | — | sin cambios |
| B | `EvaluarCierreSesion`, `EvaluarIngresoDeOtraPersona` (casos de uso de sesión) | `core/sync/*`, `IncidenciaDao`, `MovimientoDao`, `EstadoDatosRepository`, `AvisoConexion`, `DescartarCambiosRechazados` (nuevo, en `core/domain/usecase`), spec 005 | solo DAO; `version` 2 |
| C | ninguno | `MapeoFirestore`, `SincronizacionTiempoReal`, `CatalogoDao` | solo DAO; `version` 2 |
| D | `firebase/firestore.rules` | `ReglasSincronizacion`, `SincronizadorIncidencias` | sin cambios |
| E | `core/domain/Resultado.kt`, `core/ui/MensajesError.kt` | `IncidenciaDao`, `CatalogoDao` | solo DAO; `version` 2 |
| F | ninguno | `IncidenciasNavigation`, `DetalleIncidenciaScreen` (módulo `incidencias`) | sin cambios |

Ningún PR toca `libs.versions.toml`, `app/build.gradle.kts`, `SigtecDatabase.kt`, las entidades, `Rutas.kt`, `SigtecRaiz.kt`, `core/domain/model/*`, `core/theme/*`, `core/di/*`, `AndroidManifest.xml` ni `feature/acceso/*`.
