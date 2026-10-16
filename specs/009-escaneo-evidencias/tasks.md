# Tareas 009 — Código del equipo por cámara y evidencia fotográfica (HU-09)

> Salen de `spec.md` y `plan.md`. Cada tarea dura entre 20 y 30 minutos como máximo, termina en **un commit con
> su push inmediato** (antes del push se revisa el estado del PR) y depende solo de las anteriores de su PR. Van
> agrupadas por PR (plan, sección 1) y, dentro de cada uno, de adentro hacia afuera: reglas puras y dominio con sus
> pruebas → datos → `ViewModel` → pantalla → navegación → verificación.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM). ⚠️ = compartido de `CLAUDE.md` (se aprueba junto con el plan); 🔶 = sensible fuera de esa lista.
> **M, N, O y Q solo se hacen si se aprueban D-5, D-6, D-7 y D-8 (opción a).**
> Cada «Abrir el PR» es: `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas,
> PR en borrador con la plantilla llena (HU-09, criterio 10 u 11, archivos compartidos), revisión pedida a
> Alonso18-11 y, al terminar, «Ready for review».
> Orden de fusión: A → B → D; C antes de E; E → F; G y H → I → J → K y L; L → M y N; G → O → Q; P al final.

## PR A — extraer el código del texto (`feat/hu09-lectura-codigo`, desde `main`)

- [ ] **T01 · Crear la rama desde `main` al día**
  `git switch main`, `git pull origin main` y `git switch -c feat/hu09-lectura-codigo`.
  RF: —
  Hecho cuando: `git branch --show-current` muestra la rama y `git log -1` coincide con `origin/main`.

- [ ] **T02 · `normalizarTexto`**
  En `feature/captura/domain/CodigoEquipo.kt`: mayúsculas, guiones unificados y sin espacios alrededor (plan 3.1).
  RF: RF-903
  Hecho cuando: `./gradlew assembleDebug` compila.

- [ ] **T03 · `candidatosCodigo`**
  En el mismo archivo: la expresión de 3.2 y la corrección de cada parte según su posición.
  RF: RF-903
  Hecho cuando: compila.

- [ ] **T04 · `LecturaCodigo` y `elegirCodigo`**
  `data class LecturaCodigo(codigo, enCatalogo)` y la regla de 3.3.
  RF: RF-904, RF-907
  Hecho cuando: compila.

- [ ] **T05 · `CodigoEquipoTest`**
  En `feature/captura/domain/`, los casos de la sección 7 del plan (incluido el texto del prototipo 13, una fecha y
  un anexo que no deben salir).
  RF: RF-903, RF-904, RF-907
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*CodigoEquipoTest"` pasa.

- [ ] **T06 · `RachaLectura`**
  `feature/captura/domain/RachaLectura.kt` con `siguiente`, `estable` y `LECTURAS_IGUALES = 3` (plan 3.4).
  RF: RF-905, RF-908
  Hecho cuando: compila.

- [ ] **T07 · `RachaLecturaTest`**
  Los cuatro casos de la sección 7.
  RF: RF-905, RF-908
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RachaLecturaTest"` pasa.

- [ ] **T08 · Abrir el PR A**
  Compartidos: ninguno. «Cómo probarlo»: solo JUnit.
  RF: —
  Hecho cuando: el PR está abierto con el check «compilar-y-probar» en verde.

## PR B — leer el código con el catálogo (`feat/hu09-leer-codigo-catalogo`, apilada sobre A)

- [ ] **T09 · Crear la rama**
  Desde la rama de A (o desde `main`, si A ya se fusionó).
  RF: —
  Hecho cuando: `git log --oneline -3` muestra los commits de A debajo.

- [ ] **T10 · `CatalogoEquiposRepository`**
  Interfaz en `feature/captura/domain/` con `suspend fun existe(codigo: String): Boolean`.
  RF: RF-904, RF-907
  Hecho cuando: compila.

- [ ] **T11 · `LeerCodigoEquipo`**
  Caso de uso con `@Inject constructor` y `suspend operator fun invoke(texto)` (plan 4.1), sin `VerificarPermiso`
  (P-1).
  RF: RF-902 a RF-904, RF-907
  Hecho cuando: compila.

- [ ] **T12 · `CatalogoEquiposRepositoryFalso`**
  En `feature/captura/data/` de `app/src/test`, con los 4 equipos de prueba (`PC-CONT-014`, `PC-LOG-009`,
  `PC-SIS-003`, `IMP-RRHH-002`).
  RF: —
  Hecho cuando: compila con `testDebugUnitTest`.

- [ ] **T13 · `LeerCodigoEquipoTest`**
  Los cuatro casos de la sección 7.
  RF: RF-904, RF-907
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*LeerCodigoEquipoTest"` pasa.

- [ ] **T14 · `CatalogoEquiposRepositoryImpl`**
  En `feature/captura/data/`: `CatalogoDao.equipoPorCodigo`, `@IoDispatcher` y el `catch` de la convención
  (relanza `CancellationException`, `Log.w` y `false`).
  RF: RF-904
  Hecho cuando: compila.

- [ ] **T15 · `CapturaModule`**
  `feature/captura/di/CapturaModule.kt` con el `@Binds` del catálogo.
  RF: —
  Hecho cuando: `./gradlew assembleDebug` compila (Hilt resuelve el grafo).

- [ ] **T16 · Abrir el PR B**
  Compartidos: ninguno.
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR C — permiso de cámara (`feat/hu09-permiso-camara`, desde `main`)

- [ ] **T17 · Crear la rama desde `main` al día**
  RF: —
  Hecho cuando: la rama coincide con `origin/main`.

- [ ] **T18 · Permiso de cámara en el manifest ⚠️**
  En `AndroidManifest.xml`: `uses-permission` `CAMERA` y `uses-feature` `android.hardware.camera` con
  `required="false"` (D-11).
  RF: RF-916, RF-920
  Hecho cuando: `./gradlew lintDebug` no muestra `PermissionImpliesUnsupportedHardware`.

- [ ] **T19 · `VistaPermiso` y `vistaPermiso`**
  En `feature/captura/presentation/PermisoCamara.kt`, la función pura de 3.5.
  RF: RF-916 a RF-920, RF-922
  Hecho cuando: compila.

- [ ] **T20 · `PermisoCamaraTest`**
  La tabla de la sección 7.
  RF: RF-916 a RF-920, RF-922
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*PermisoCamaraTest"` pasa.

- [ ] **T21 · `ExplicacionPermisoCamara`**
  Composable sin estado con el texto y el botón de cada vista (plan 5.1); vistas previas de `EXPLICAR`, `NEGADO`,
  `IR_A_AJUSTES` y `SIN_CAMARA` en claro y oscuro.
  RF: RF-916, RF-918 a RF-920
  Hecho cuando: las vistas previas se ven en Android Studio, sin colores ni tamaños fijos.

- [ ] **T22 · `ConPermisoCamara`**
  Composable con estado (plan 5.1): consulta, solicitud, «Abrir ajustes» y nueva consulta en cada `ON_RESUME`.
  RF: RF-916 a RF-922
  Hecho cuando: compila; se prueba en el celular con E.

- [ ] **T23 · Abrir el PR C**
  Compartidos: `AndroidManifest.xml` (D-11).
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR D — «Escanear etiqueta» sin cámara (`feat/hu09-escaneo-pantalla`, apilada sobre B)

- [ ] **T24 · Crear la rama**
  Desde la rama de B (o desde `main`, si B ya se fusionó).
  RF: —
  Hecho cuando: la rama tiene debajo los commits de B.

- [ ] **T25 · `EscaneoViewModel`**
  `EscaneoUiState(racha, reconocido)`, el flujo de textos de capacidad 1 y `alLeerTexto` y `alVolverAEscanear`
  (plan 4.2, P-9).
  RF: RF-902, RF-905 a RF-908
  Hecho cuando: compila.

- [ ] **T26 · `EscaneoViewModelTest`**
  Los cinco casos de la sección 7, con `MainDispatcherRule` y `CatalogoEquiposRepositoryFalso`.
  RF: RF-905 a RF-908
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*EscaneoViewModelTest"` pasa.

- [ ] **T27 · `EscanearEtiquetaScreen`**
  Route + Screen con la ranura `vistaCamara`, el marco, la indicación y el panel del código (plan 5.2); vistas
  previas buscando, reconocido y fuera del catálogo, en claro y oscuro.
  RF: RF-901, RF-906, RF-907, RF-912
  Hecho cuando: las vistas previas coinciden con el prototipo 13, salvo la barra (D-10) y la linterna (HU-10).

- [ ] **T28 · Abrir el PR D**
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR E — cámara y ML Kit en el escaneo (`feat/hu09-escaneo-camara`, apilada sobre D, con C en `main`)

- [ ] **T29 · Crear la rama**
  Desde la rama de D; `git pull origin main` para traer C.
  RF: —
  Hecho cuando: la rama tiene los commits de D y de C.

- [ ] **T30 · `VistaCamara`**
  `PreviewView` en `AndroidView`, `ProcessCameraProvider.awaitInstance`, casos de uso recibidos y enlazados al ciclo
  de vida, cámara trasera o la que haya, y `alFallar` (P-2).
  RF: RF-901, RF-915, RF-921
  Hecho cuando: compila.

- [ ] **T31 · `LectorTextoEtiqueta`**
  `ImageAnalysis.Analyzer` con ML Kit que entrega el texto y cierra cada imagen (P-3); el reconocedor se cierra al
  salir de la pantalla.
  RF: RF-902, RF-914
  Hecho cuando: compila.

- [ ] **T32 · La Route arma el permiso y la cámara**
  `ConPermisoCamara { VistaCamara(...) }` con `Preview` e `ImageAnalysis` (`KEEP_ONLY_LATEST`); solo pasa textos
  mientras no haya un código reconocido.
  RF: RF-901, RF-906, RF-916
  Hecho cuando: compila.

- [ ] **T33 · Conectar «Escanear etiqueta» en la navegación**
  En `CapturaNavigation.kt`, reemplazar la `PantallaEnConstruccion`, declarar `CLAVE_CODIGO_ESCANEADO` y devolver el
  código por `previousBackStackEntry?.savedStateHandle` (plan 5.3).
  RF: RF-909, RF-912, RF-913
  Hecho cuando: compila y el botón de cámara de «Nueva incidencia» abre la pantalla.

- [ ] **T34 · Verificación manual en el celular**
  Los puntos de E de la sección 7 del plan, con capturas en claro y oscuro (en el scratchpad, nunca en el repo).
  RF: RF-901, RF-902, RF-905, RF-906, RF-915 a RF-921
  Hecho cuando: el resultado queda escrito en «Cómo probarlo» del PR.

- [ ] **T35 · Abrir el PR E**
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR F — «Nueva incidencia» recibe el código (`feat/hu09-escaneo-registro`, apilada sobre E)

- [ ] **T36 · Crear la rama**
  RF: —
  Hecho cuando: la rama tiene debajo los commits de E.

- [ ] **T37 · El registro recuerda si el código vino de la cámara**
  En `RegistroIncidenciaViewModel`: `codigoLeidoConCamara` en el estado, `alUsarCodigoEscaneado(codigo)` y
  `alCambiarCodigo` que lo apaga.
  RF: RF-910, RF-911
  Hecho cuando: compila.

- [ ] **T38 · Pruebas del registro con código escaneado**
  Los tres casos nuevos de la sección 7 en `RegistroIncidenciaViewModelTest`.
  RF: RF-910, RF-911
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RegistroIncidenciaViewModelTest"` pasa.

- [ ] **T39 · «Leído desde la etiqueta con la cámara»**
  En `RegistroIncidenciaScreen`, el `supportingText` según el estado (y fuera el comentario «Neutral hasta la
  HU-09»); vista previa con el código escaneado.
  RF: RF-910
  Hecho cuando: la vista previa coincide con el prototipo 4.

- [ ] **T40 · El registro recibe el código al volver**
  `IncidenciasNavigation` lee `CLAVE_CODIGO_ESCANEADO` de su entrada y la Route lo aplica y lo borra (plan 5.3).
  RF: RF-909, RF-910
  Hecho cuando: compila.

- [ ] **T41 · Verificación manual en el celular**
  Los puntos de F de la sección 7, incluida una rotación después de usar el código (no debe repetirse).
  RF: RF-909 a RF-912
  Hecho cuando: el resultado queda escrito en el PR.

- [ ] **T42 · Abrir el PR F**
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.
