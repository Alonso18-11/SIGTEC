# Tareas 004 — Lista de incidencias con búsqueda y filtros, y detalle (HU-04)

> Salen de `spec.md` y `plan.md`. RF-42 a RF-57 ya están en `main` desde el PR #8 (`1940fcb`) y el PR #9 (`6420f40`); ninguna tarea los reimplementa. Solo hay tres ajustes reales: mover la búsqueda/filtros a `domain/` (RF-43, RF-44), el temporizador en ambos `ViewModel` (caso límite) y el renombre del chip «Estado» → «Marca» (RF-44).
> Cada tarea dura 20-30 min como máximo. Orden de adentro hacia afuera: primero el dominio con su prueba, luego el `ViewModel` de lista y sus imports, luego el chip, luego el temporizador de cada `ViewModel` con su prueba, y al final la verificación y el cierre.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/` (pruebas). Ningún archivo de esta spec es «archivo compartido» según `CLAUDE.md`; no se necesita visto bueno adicional.

## Fase 0 — Preparación

- [x] **T01 · Crear o retomar la rama de trabajo**
  Esta spec se escribió en `feat/hu04-lista-detalle-sdd`; si esa rama ya se usó para abrir un PR (documentación), crear la siguiente rama apilada para estos ajustes, por ejemplo `feat/hu04-ajustes-sdd`, desde `main` al día.
  RF: —
  Hecho cuando: `git branch --show-current` muestra la rama elegida y `./gradlew testDebugUnitTest` pasa sin cambios respecto a `main`.

## Fase 1 — Mover la búsqueda y los filtros a `domain/` (RF-43, RF-44)

- [x] **T02 · Crear `feature/incidencias/domain/FiltrarIncidencias.kt`**
  Archivo nuevo con `POR_SINCRONIZAR`, `Incidencia.codigoParaMostrar()`, `SIN_TECNICO`, `FiltrosIncidencias` y `List<Incidencia>.filtrar(f: FiltrosIncidencias)` con sus helpers privados (`coincideCon`, `normalizado`, `MARCAS_DIACRITICAS`), copiando el código actual de `ListaIncidenciasViewModel.kt` y `TextosIncidencia.kt` sin cambiar su lógica (ver plan.md, sección 1.2). Todavía no se usa desde ningún otro archivo ni se borra el original.
  RF: RF-43, RF-44
  Hecho cuando: `./gradlew assembleDebug` compila (el archivo nuevo convive, sin usarse, con las declaraciones originales que aún no se han quitado).

- [x] **T03 · Crear `FiltrarIncidenciasTest.kt` con los casos movidos**
  Archivo nuevo en `app/src/test/java/com/softcorp/sigtec/`. Mover y adaptar de `ListaIncidenciasViewModelTest.kt` los casos de búsqueda por código INC (con y sin prefijo), por equipo sin mayúsculas, por descripción sin tildes, filtro por cada marca, filtro por técnico y por «Sin técnico», filtro por equipo, y la combinación de búsqueda con filtros — probando `lista.filtrar(FiltrosIncidencias(...))` directamente (sin `ViewModel`, `StateFlow` ni `MainDispatcherRule`, porque la función ya no necesita coroutines).
  RF: RF-43, RF-44
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*FiltrarIncidenciasTest"` pasa con todos los casos movidos.

- [x] **T04 · Usar la función de dominio desde `ListaIncidenciasViewModel.kt`**
  Quitar de `ListaIncidenciasViewModel.kt` la `data class FiltrosIncidencias`, `filtrar()` y sus helpers privados; importarlos desde `feature.incidencias.domain`. `estado` sigue llamando `todas.filtrar(f)` igual que antes.
  RF: RF-44
  Hecho cuando: `./gradlew assembleDebug` compila y `./gradlew testDebugUnitTest --tests "*ListaIncidenciasViewModelTest"` sigue pasando (con los casos que todavía no se han quitado, ver T05).

- [x] **T05 · Limpiar `TextosIncidencia.kt` y ajustar los imports que dependían de la visibilidad de paquete**
  Quitar `POR_SINCRONIZAR`, `codigoParaMostrar()` y `SIN_TECNICO` de `TextosIncidencia.kt` (ya viven en `domain`). Agregar el `import` correspondiente en `ListaIncidenciasScreen.kt` (`FiltrosIncidencias`, `SIN_TECNICO`, `codigoParaMostrar`), `DetalleIncidenciaScreen.kt` (`codigoParaMostrar`, `SIN_TECNICO`), `IncidenciasNavigation.kt` (`POR_SINCRONIZAR`) y `TextosIncidenciaTest.kt` (`codigoParaMostrar`).
  RF: RF-44
  Hecho cuando: `./gradlew assembleDebug testDebugUnitTest lintDebug` compila y pasa sin ningún `Unresolved reference`.

- [x] **T06 · Quitar de `ListaIncidenciasViewModelTest.kt` los casos ya cubiertos por `FiltrarIncidenciasTest`**
  Quitar los casos de búsqueda y filtro movidos en T03; dejar solo: orden de la más reciente a la más antigua (RF-42), opciones de técnico y equipo desde los datos cargados (RF-45) y limpiar quita todo de un toque (RF-46). Actualizar el `import` de `SIN_TECNICO` a `domain` si el archivo todavía lo usa.
  RF: RF-42, RF-45, RF-46
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ListaIncidenciasViewModelTest"` pasa con los casos restantes, sin duplicar cobertura con `FiltrarIncidenciasTest`.

## Fase 2 — Renombrar el chip «Estado» → «Marca» (RF-44)

- [x] **T07 · Cambiar el texto del chip en `ListaIncidenciasScreen.kt`**
  En la función privada `Filtros(...)`, cambiar `FiltroDesplegable("Estado", f.marca, ...)` a `FiltroDesplegable("Marca", f.marca, ...)`.
  RF: RF-44
  Hecho cuando: `./gradlew assembleDebug` compila y `grep -rn "\"Estado\"" app/src/main/java/com/softcorp/sigtec/feature/incidencias` no encuentra ninguna coincidencia.

## Fase 3 — Temporizador en `ListaIncidenciasViewModel` (caso límite)

- [x] **T08 · Agregar el `reloj` y combinarlo en `ListaIncidenciasViewModel.kt`**
  Agregar el `flow { while (true) { emit(Instant.now()); delay(RELOJ_MS) } }` (con `RELOJ_MS = 60_000L`, igual que `RegistroIncidenciaViewModel`), y cambiar el `combine(observarIncidencias(), filtros) { ... }` por `combine(observarIncidencias(), filtros, reloj) { todas, f, ahora -> ... }`, usando `ahora` en vez de `Instant.now()` inline.
  RF: caso límite (`ahora` sin temporizador)
  Hecho cuando: `./gradlew assembleDebug` compila y `./gradlew testDebugUnitTest --tests "*ListaIncidenciasViewModelTest"` sigue pasando.

- [x] **T09 · Agregar el caso del temporizador a `ListaIncidenciasViewModelTest.kt`**
  Con tiempo virtual (`main.dispatcher.scheduler.advanceTimeBy(60_000L)` + `runCurrent()`, igual que `RegistroIncidenciaViewModelTest."la fecha mostrada avanza cada minuto..."`), comprobar que `estado.value.ahora` cambia sin que cambien las incidencias ni los filtros.
  RF: caso límite
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ListaIncidenciasViewModelTest"` pasa con el caso nuevo.

## Fase 4 — Temporizador en `DetalleIncidenciaViewModel` (caso límite)

- [x] **T10 · Agregar el `reloj` y combinarlo en `DetalleIncidenciaViewModel.kt`**
  Mismo patrón que T08: agregar el `reloj` propio y cambiar `combine(observarDetalle(incidenciaId), observarUsuarioActual()) { ... }` por `combine(observarDetalle(incidenciaId), observarUsuarioActual(), reloj) { detalle, usuario, ahora -> ... }`, usando `ahora` en vez de `Instant.now()` inline.
  RF: caso límite
  Hecho cuando: `./gradlew assembleDebug` compila y `./gradlew testDebugUnitTest --tests "*DetalleIncidenciaViewModelTest"` sigue pasando.

- [x] **T11 · Agregar el caso del temporizador a `DetalleIncidenciaViewModelTest.kt`**
  Mismo patrón que T09: avanzar el tiempo virtual y comprobar que `estado.value.ahora` cambia sin que cambien el detalle, los movimientos ni el usuario.
  RF: caso límite
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*DetalleIncidenciaViewModelTest"` pasa con el caso nuevo.

## Fase 5 — Verificación

- [x] **T12 · Comando del CI**
  Correr `./gradlew assembleDebug testDebugUnitTest lintDebug`.
  RF: todos
  Hecho cuando: termina en verde y sin warnings de lint nuevos respecto a `main`.

- [x] **T13 · Prueba manual — NO automática: chip renombrado y temporizador en pantalla**
  **Detenerse aquí si no hay celular o emulador disponible; esta tarea no se puede completar solo con el comando del CI.**
  Abrir la lista de incidencias y comprobar que el chip ahora dice «Marca» (en claro y oscuro, `adb shell cmd uimode night yes|no`) y que, al elegir una marca, sigue mostrando su etiqueta (p. ej. «Asignada»). Dejar abierta la lista y el detalle de una incidencia más de un minuto sin tocar nada ni cambiar de pantalla, y confirmar que no hay demora, ANR ni cierre inesperado (`adb logcat *:W` sin excepciones nuevas) — el temporizador corre en segundo plano sin bloquear la interfaz.
  RF: RF-44, caso límite
  Hecho cuando: el chip se lee «Marca» en ambos temas, y ninguna de las dos pantallas falla ni se congela tras el minuto de espera.

## Fase 6 — Cierre

- [x] **T14 · Actualizar `spec.md`**
  Repasar que las notas «Decisión: ... se construye en el plan de esta spec» de RF-44, «Casos límite» y «Dudas abiertas» queden coherentes con lo ya implementado (sin cambiar el fondo de lo decidido, solo quitar el tiempo futuro si corresponde, p. ej. «se construyó» en vez de «se construye», al estilo de como HU-03 cerró su nota de RF-41).
  RF: todos
  Hecho cuando: ninguna nota de `spec.md` describe como pendiente algo que ya está en el código.

- [x] **T15 · Actualizar `MEMORY.md`**
  Si corresponde, anotar que la búsqueda y los filtros de la lista de incidencias viven ahora en `feature/incidencias/domain/FiltrarIncidencias.kt` (no en el `ViewModel`), y que `ListaIncidenciasViewModel`/`DetalleIncidenciaViewModel` ya tienen el mismo patrón de temporizador que `RegistroIncidenciaViewModel` (RF-32).
  RF: —
  Hecho cuando: `MEMORY.md` sigue en ~50 líneas como máximo.

- [x] **T16 · Commit y PR**
  Commits en Conventional Commits en español con la HU al final, por ejemplo `refactor(incidencias): mover la busqueda y los filtros a domain (HU-04)`, `feat(incidencias): temporizador propio en lista y detalle (HU-04)` y `fix(incidencias): renombrar el chip de marca de seguimiento (HU-04)`, sin línea de coautoría. PR con `.github/pull_request_template.md`; la sección «Archivos compartidos que toca» queda vacía o dice «Ninguno», citando la sección 9 de `plan.md`.
  RF: todos
  Hecho cuando: el PR existe, el CI «compilar-y-probar» está en verde, y la descripción enlaza `specs/004-lista-detalle-incidencias/`.
