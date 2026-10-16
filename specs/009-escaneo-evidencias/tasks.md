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

## PR-10 — permiso en el manifest (base `main`)
- [ ] **T20 · `CAMERA` y la cámara no obligatoria ⚠️** en `AndroidManifest.xml` (D-11). RF-916, RF-920.
  Hecho cuando: `./gradlew lintDebug` no muestra `PermissionImpliesUnsupportedHardware`.

## PR-11 — regla del permiso (base `main`)
- [ ] **T21 · `VistaPermiso` y `vistaPermiso`** en `feature/captura/presentation/PermisoCamara.kt` (3.5).
  RF-916 a RF-920, RF-922. Hecho cuando: compila.
- [ ] **T22 · `PermisoCamaraTest`** con la tabla de la sección 7. RF-916 a RF-920, RF-922.
  Hecho cuando: la prueba pasa.

## PR-12 — explicación del permiso (base PR-11)
- [ ] **T23 · `ExplicacionPermisoCamara`** sin estado, con el texto y el botón de cada vista (5.1).
  RF-916, RF-918 a RF-920. Hecho cuando: compila.
- [ ] **T24 · Vistas previas** de `EXPLICAR`, `NEGADO`, `IR_A_AJUSTES` y `SIN_CAMARA` en claro y oscuro. RF: —
  Hecho cuando: se ven sin colores ni tamaños fijos.

## PR-13 — pedir el permiso (base PR-12)
- [ ] **T25 · `ConPermisoCamara`**: consulta, solicitud y «Abrir ajustes» (5.1). RF-916 a RF-921.
  Hecho cuando: compila.
- [ ] **T26 · Volver a consultar en cada `ON_RESUME`.** RF-919, RF-922.
  Hecho cuando: compila; se prueba en el celular con PR-16.

## PR-14 — vista de cámara (base `main`)
- [ ] **T27 · `VistaCamara`** con `ProcessCameraProvider.awaitInstance` y los casos de uso recibidos (P-2).
  RF-901, RF-915. Hecho cuando: compila.
- [ ] **T28 · Aviso cuando la cámara no abre**, con «Reintentar». RF-921.
  Hecho cuando: compila.

## PR-15 — lector de texto (base `main`)
- [ ] **T29 · `LectorTextoEtiqueta`** con ML Kit; cierra cada imagen y el reconocedor (P-3). RF-902, RF-914.
  Hecho cuando: compila.

## PR-16 — escaneo con la cámara (base PR-09, trae PR-13, PR-14 y PR-15)
- [ ] **T30 · La Route arma `ConPermisoCamara { VistaCamara(...) }`** con `Preview` e `ImageAnalysis` y solo pasa
  textos sin un código reconocido. RF-901, RF-906, RF-916.
  Hecho cuando: compila; la prueba en el celular se hace con PR-17, cuando la pantalla ya está en la navegación.

## PR-17 — escaneo en la navegación (base PR-16)
- [ ] **T31 · `CLAVE_CODIGO_ESCANEADO`** en `CapturaNavigation.kt` (5.3). RF-913.
  Hecho cuando: compila.
- [ ] **T32 · Reemplazar la `PantallaEnConstruccion` de `EscanearEtiqueta`** y devolver el código. RF-909, RF-912.
  Hecho cuando: el botón de cámara de «Nueva incidencia» abre la pantalla y, en el celular, se cumplen los puntos de
  PR-16 y PR-17 de la sección 7 del plan.

## PR-18 — el registro recuerda el código escaneado (base `main`)
- [ ] **T33 · `codigoLeidoConCamara` y `alUsarCodigoEscaneado`** en `RegistroIncidenciaViewModel`. RF-910, RF-911.
  Hecho cuando: compila.
- [ ] **T34 · Casos nuevos de `RegistroIncidenciaViewModelTest`.** RF-910, RF-911.
  Hecho cuando: la prueba pasa.

## PR-19 — indicación bajo el código (base PR-18)
- [ ] **T35 · `supportingText` según el estado**, sin el comentario «Neutral hasta la HU-09», y vista previa. RF-910.
  Hecho cuando: la vista previa coincide con el prototipo 4.

## PR-20 — el registro recibe el código (base PR-19, trae PR-17)
- [ ] **T36 · `IncidenciasNavigation` lee la clave y la Route la aplica y la borra** (5.3). RF-909, RF-910.
  Hecho cuando: en el celular se cumplen los puntos de PR-20 de la sección 7, incluida una rotación.

## PR-21 — carpeta de las fotos (base `main`)
- [ ] **T37 · `AlmacenFotos` 🔶** en `core/data/` (P-4). RF-925, RF-928, RF-940, RF-942.
  Hecho cuando: compila.
- [ ] **T38 · `AlmacenFotosTest`** con `TemporaryFolder`. RF-928, RF-940, RF-942.
  Hecho cuando: la prueba pasa.

## PR-22 — copia en Android 11 o anterior (base `main`)
- [ ] **T39 · `<exclude domain="file" path="evidencias/" />`** en `res/xml/backup_rules.xml` 🔶. RF-941.
  Hecho cuando: `lintDebug` no lo marca.

## PR-23 — copia y transferencia en Android 12 o superior (base `main`)
- [ ] **T40 · La misma exclusión en `cloud-backup` y `device-transfer`** de `res/xml/data_extraction_rules.xml` 🔶.
  RF-941. Hecho cuando: `lintDebug` no lo marca.

## PR-24 — error del máximo de fotos (base `main`)
- [ ] **T41 · `LimiteFotosAlcanzado` ⚠️** en `core/domain/Resultado.kt`. RF-929.
  Hecho cuando: compila hasta el `when` de `mensaje()`, que obliga a T42.
- [ ] **T42 · Su mensaje ⚠️** en `core/ui/MensajesError.kt`. RF-929.
  Hecho cuando: compila.

## PR-25 — regla de las fotos (base PR-24)
- [ ] **T43 · `ReglasEvidencia`** con `MAX_FOTOS` y `bloqueoParaFotos` (3.6). RF-923, RF-926, RF-929, RF-931, RF-937.
  Hecho cuando: compila.
- [ ] **T44 · `ReglasEvidenciaTest`** (ocho casos). Mismos RF.
  Hecho cuando: la prueba pasa.

## PR-26 — contrato de evidencias (base `main`)
- [ ] **T45 · `EvidenciaRepository`** (interfaz). RF-924 a RF-929.
  Hecho cuando: compila.
- [ ] **T46 · `EvidenciaRepositoryFalso`** con flujos en memoria, `fallar` y rutas descartadas. RF: —
  Hecho cuando: compila con `testDebugUnitTest`.

## PR-27 — repositorio de evidencias (base PR-26, trae PR-06 y PR-21)
- [ ] **T47 · `EvidenciaRepositoryImpl`** (4.7). RF-925, RF-928, RF-939, RF-940.
  Hecho cuando: compila.
- [ ] **T48 · `@Binds` en `CapturaModule`.** RF: —
  Hecho cuando: `assembleDebug` compila con Hilt.

## PR-28 — `PrepararFoto` (base PR-26)
- [ ] **T49 · `PrepararFoto` y `FotoNueva`** (4.3). RF-925. Hecho cuando: compila.
- [ ] **T50 · `PrepararFotoTest`.** RF-925. Hecho cuando: la prueba pasa.

## PR-29 — `GuardarEvidencia` (base PR-28, trae PR-25)
- [ ] **T51 · `GuardarEvidencia`** con `ahora` como parámetro (4.4). RF-925, RF-926, RF-928, RF-929.
  Hecho cuando: compila.
- [ ] **T52 · `GuardarEvidenciaTest`** (seis casos). Mismos RF. Hecho cuando: la prueba pasa.

## PR-30 — `DescartarFoto` (base PR-26)
- [ ] **T53 · `DescartarFoto`** (4.5). RF-928. Hecho cuando: compila.
- [ ] **T54 · `DescartarFotoTest`.** RF-928. Hecho cuando: la prueba pasa.

## PR-31 — `ObservarCapturaEvidencia` (base PR-25, trae PR-26)
- [ ] **T55 · `CapturaEvidencia` y `ObservarCapturaEvidencia`** (4.6). RF-924, RF-929, RF-931.
  Hecho cuando: compila.
- [ ] **T56 · `ObservarCapturaEvidenciaTest`** (cinco casos). Mismos RF. Hecho cuando: la prueba pasa.
