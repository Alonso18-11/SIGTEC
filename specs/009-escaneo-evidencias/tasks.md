# Tareas 009 — Código del equipo por cámara y evidencia fotográfica (HU-09)

> Salen de `spec.md` y `plan.md`. Una tarea es un commit (20 a 30 minutos como máximo) con su push inmediato, después
> de revisar el estado del PR. Van agrupadas por PR (plan, sección 1) y en el orden de la tabla.
> **Al empezar cada PR:** rama desde su «Base» al día (`git pull origin main` o la rama base), `git merge` de las
> ramas de «Trae» que sigan abiertas, primer commit y push, y PR en borrador con la plantilla llena (HU-09, criterio,
> archivos compartidos, qué trae de otros PR) y revisión pedida a Alonso18-11.
> **Al terminar cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas,
> el CI en verde, «Cómo probarlo» escrito y «Ready for review».
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` y `app/src/test/java/com/softcorp/sigtec/`.
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

## PR-01 — CI en los PR apilados (base `main`)
- [ ] **T01 · Quitar el filtro de rama base del CI 🔶**
  En `.github/workflows/ci.yml`, `pull_request:` sin `branches: [main]`; `push` sigue solo en `main` (P-13).
  RF: —
  Hecho cuando: un PR con base en otra rama muestra el check `compilar-y-probar`.

## PR-02 — normalizar el texto (base `main`)
- [ ] **T02 · `normalizarTexto`** en `feature/captura/domain/CodigoEquipo.kt` (plan 3.1). RF-903.
  Hecho cuando: compila.
- [ ] **T03 · Pruebas de `normalizarTexto`** en `CodigoEquipoTest`: mayúsculas, guiones raros, espacios. RF-903.
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*CodigoEquipoTest"` pasa.

## PR-03 — candidatos (base PR-02)
- [ ] **T04 · `candidatosCodigo`** (3.2). RF-903.
  Hecho cuando: compila.
- [ ] **T05 · Pruebas de candidatos**: `PC-C0NT-O14`, texto del prototipo 13, fecha, anexo, dos códigos. RF-903.
  Hecho cuando: `CodigoEquipoTest` pasa.

## PR-04 — elegir el código (base PR-03)
- [ ] **T06 · `LecturaCodigo` y `elegirCodigo`** (3.3). RF-904, RF-907.
  Hecho cuando: compila.
- [ ] **T07 · Pruebas de `elegirCodigo`**: con catálogo, sin catálogo, sin candidatos. RF-904, RF-907.
  Hecho cuando: `CodigoEquipoTest` pasa.

## PR-05 — racha de lecturas (base `main`)
- [ ] **T08 · `RachaLectura`** en `feature/captura/domain/RachaLectura.kt` (3.4). RF-905, RF-908.
  Hecho cuando: compila.
- [ ] **T09 · `RachaLecturaTest`**: 3 iguales, alternancia, sin código, racha nueva. RF-905, RF-908.
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RachaLecturaTest"` pasa.

## PR-06 — catálogo de equipos (base `main`)
- [ ] **T10 · `CatalogoEquiposRepository`** (interfaz, `existe(codigo)`). RF-904.
  Hecho cuando: compila.
- [ ] **T11 · `CatalogoEquiposRepositoryImpl`** con `CatalogoDao.equipoPorCodigo`, `@IoDispatcher` y el `catch` de la
  convención. RF-904.
  Hecho cuando: compila.
- [ ] **T12 · `CapturaModule`** con el `@Binds`. RF: —
  Hecho cuando: `./gradlew assembleDebug` compila con Hilt.
- [ ] **T13 · `CatalogoEquiposRepositoryFalso`** con los 4 equipos de prueba. RF: —
  Hecho cuando: compila con `testDebugUnitTest`.

## PR-07 — `LeerCodigoEquipo` (base PR-04, trae PR-06)
- [ ] **T14 · `LeerCodigoEquipo`** (4.1, sin `VerificarPermiso`, P-1). RF-902 a RF-904, RF-907.
  Hecho cuando: compila.
- [ ] **T15 · `LeerCodigoEquipoTest`**: del catálogo, fuera, dos códigos, sin código. RF-904, RF-907.
  Hecho cuando: la prueba pasa.

## PR-08 — `EscaneoViewModel` (base PR-07, trae PR-05)
- [ ] **T16 · `EscaneoViewModel`** con el flujo de capacidad 1 (4.2, P-9). RF-902, RF-905 a RF-908.
  Hecho cuando: compila.
- [ ] **T17 · `EscaneoViewModelTest`** con `MainDispatcherRule` (plan, sección 7). RF-905 a RF-908.
  Hecho cuando: la prueba pasa.

## PR-09 — «Escanear etiqueta» sin cámara (base PR-08)
- [ ] **T18 · Pantalla con el marco y la indicación** (Route + Screen con ranura `vistaCamara`, 5.2). RF-901, RF-912.
  Hecho cuando: la vista previa «buscando» se ve en claro y oscuro.
- [ ] **T19 · Panel del código reconocido y aviso fuera del catálogo** (5.2). RF-906, RF-907.
  Hecho cuando: las vistas previas «reconocido» y «fuera del catálogo» coinciden con el prototipo 13, salvo la
  barra (D-10) y la linterna (HU-10).
