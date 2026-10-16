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

## PR G — carpeta interna de las fotos (`feat/hu09-carpeta-fotos`, desde `main`)

- [ ] **T43 · Crear la rama desde `main` al día**
  RF: —
  Hecho cuando: la rama coincide con `origin/main`.

- [ ] **T44 · `AlmacenFotos` 🔶**
  `core/data/AlmacenFotos.kt`: `@Singleton`, el constructor con `@ApplicationContext`, el constructor `internal`
  con la carpeta raíz, `archivoNuevo`, `borrar` y `borrarTodo` (plan, PR G y P-4).
  RF: RF-925, RF-928, RF-940, RF-942
  Hecho cuando: `./gradlew assembleDebug` compila.

- [ ] **T45 · `AlmacenFotosTest`**
  En `core/data/` de `app/src/test`, con `TemporaryFolder` y los casos de la sección 7.
  RF: RF-928, RF-940, RF-942
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*AlmacenFotosTest"` pasa.

- [ ] **T46 · Fotos fuera de la copia en Android 11 o anterior 🔶**
  `res/xml/backup_rules.xml`: `<exclude domain="file" path="evidencias/" />`.
  RF: RF-941
  Hecho cuando: `./gradlew lintDebug` no marca el archivo.

- [ ] **T47 · Fotos fuera de la copia y de la transferencia en Android 12 o superior 🔶**
  `res/xml/data_extraction_rules.xml`: la misma exclusión en `cloud-backup` y en `device-transfer`.
  RF: RF-941
  Hecho cuando: `./gradlew lintDebug` no marca el archivo.

- [ ] **T48 · Abrir el PR G**
  Sensibles: `core/data` nuevo y `res/xml` (D-7, D-9).
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR H — reglas y casos de uso de las fotos (`feat/hu09-evidencias-dominio`, desde `main`)

- [ ] **T49 · Crear la rama desde `main` al día**
  RF: —
  Hecho cuando: la rama coincide con `origin/main`.

- [ ] **T50 · `LimiteFotosAlcanzado` y su mensaje ⚠️**
  `data object LimiteFotosAlcanzado : ErrorDominio` en `core/domain/Resultado.kt` y «Llegaste al máximo de 10 fotos
  por incidencia.» en `core/ui/MensajesError.kt` (D-2).
  RF: RF-929
  Hecho cuando: compila (el `when` exhaustivo de `mensaje()` cierra).

- [ ] **T51 · `EvidenciaRepository`**
  Interfaz en `feature/captura/domain/` con los métodos del plan (PR H).
  RF: RF-924 a RF-929
  Hecho cuando: compila.

- [ ] **T52 · `ReglasEvidencia`**
  `MAX_FOTOS = 10` y `bloqueoParaFotos` (plan 3.6).
  RF: RF-923, RF-926, RF-929, RF-931, RF-937
  Hecho cuando: compila.

- [ ] **T53 · `ReglasEvidenciaTest`**
  Los ocho casos de la sección 7.
  RF: RF-923, RF-926, RF-929, RF-931, RF-937
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ReglasEvidenciaTest"` pasa.

- [ ] **T54 · `EvidenciaRepositoryFalso`**
  En `feature/captura/data/` de `app/src/test`: incidencias y fotos en memoria (con `MutableStateFlow` para poder
  reasignar en las pruebas), `fallar` y la lista de rutas descartadas.
  RF: —
  Hecho cuando: compila con `testDebugUnitTest`.

- [ ] **T55 · `PrepararFoto`**
  `FotoNueva(id, ruta)` y el caso de uso (plan 4.3).
  RF: RF-925
  Hecho cuando: compila.

- [ ] **T56 · `GuardarEvidencia`**
  Caso de uso de 4.4, con `ahora: Instant` recibido como parámetro.
  RF: RF-925, RF-926, RF-928, RF-929
  Hecho cuando: compila.

- [ ] **T57 · `GuardarEvidenciaTest`**
  Los seis casos de la sección 7, con `SesionRepositoryDemo`.
  RF: RF-925, RF-926, RF-928, RF-929
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*GuardarEvidenciaTest"` pasa.

- [ ] **T58 · `DescartarFoto`**
  Caso de uso de 4.5.
  RF: RF-928
  Hecho cuando: compila.

- [ ] **T59 · `ObservarCapturaEvidencia`**
  `CapturaEvidencia(incidencia, evidencias, bloqueo)` y el caso de uso de 4.6.
  RF: RF-924, RF-929, RF-931
  Hecho cuando: compila.

- [ ] **T60 · `ObservarCapturaEvidenciaTest`**
  Los cinco casos de la sección 7.
  RF: RF-924, RF-929, RF-931
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ObservarCapturaEvidenciaTest"` pasa.

- [ ] **T61 · Abrir el PR H**
  Compartidos: `Resultado.kt` y `MensajesError.kt` (D-2).
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR I — repositorio de evidencias (`feat/hu09-evidencias-datos`, desde `main` con G y H)

- [ ] **T62 · Crear la rama desde `main` con G y H fusionados**
  RF: —
  Hecho cuando: `git log --oneline` muestra los commits de G y de H en `main`.

- [ ] **T63 · `EvidenciaRepositoryImpl`**
  En `feature/captura/data/`: `EvidenciaDao`, las lecturas de `IncidenciaDao`, `AlmacenFotos` y `@IoDispatcher`;
  `guardar` borra el archivo si Room falla (plan 4.7).
  RF: RF-925, RF-928, RF-939, RF-940
  Hecho cuando: compila.

- [ ] **T64 · `@Binds` del repositorio de evidencias**
  En `CapturaModule`.
  RF: —
  Hecho cuando: `./gradlew assembleDebug` compila.

- [ ] **T65 · Abrir el PR I**
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR J — «Evidencia fotográfica» sin cámara (`feat/hu09-evidencia-pantalla`, apilada sobre I)

- [ ] **T66 · Crear la rama**
  RF: —
  Hecho cuando: la rama tiene debajo los commits de I.

- [ ] **T67 · `CapturaEvidenciaViewModel`**
  Estado, tipo en el `SavedStateHandle`, `alCambiarTipo`, `alDisparar`, `alFotoTomada` y `alFalloCaptura`
  (plan 4.8).
  RF: RF-924 a RF-931
  Hecho cuando: compila.

- [ ] **T68 · `CapturaEvidenciaViewModelTest`**
  Los siete casos de la sección 7, con `MainDispatcherRule`.
  RF: RF-924 a RF-931
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*CapturaEvidenciaViewModelTest"` pasa.

- [ ] **T69 · `MiniaturaEvidencia`**
  Decodificación reducida con la orientación del EXIF, fuera del hilo principal, con «Foto no disponible» si no
  puede (plan 5.6, P-6).
  RF: RF-925, RF-932, RF-935
  Hecho cuando: compila y su vista previa muestra el recuadro de carga y el de «Foto no disponible».

- [ ] **T70 · `EvidenciaFotograficaScreen`**
  Route + Screen con la ranura de la cámara (plan 5.4); vistas previas con 0 y 2 fotos, guardando y al máximo, en
  claro y oscuro.
  RF: RF-924, RF-927, RF-929 a RF-931
  Hecho cuando: las vistas previas coinciden con el prototipo 14, salvo la barra (D-10) y la linterna (HU-10).

- [ ] **T71 · Abrir el PR J**
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR K — tomar la foto con la cámara (`feat/hu09-evidencia-camara`, apilada sobre J, con E en `main`)

- [ ] **T72 · Crear la rama**
  Desde la rama de J; `git pull origin main` para traer E (`VistaCamara`, `ConPermisoCamara`).
  RF: —
  Hecho cuando: la rama tiene los commits de J y de E.

- [ ] **T73 · `TomarFoto`**
  `ImageCapture` con la resolución cercana a 1600 × 1200 y `setJpegQuality(85)`, y una función `suspend` que
  escribe en la ruta de `FotoNueva` (P-5).
  RF: RF-925, RNF almacenamiento
  Hecho cuando: compila.

- [ ] **T74 · La Route arma el permiso y la cámara**
  `ConPermisoCamara { VistaCamara(...) }` con `Preview` e `ImageCapture`; al disparar, captura y llama a
  `alFotoTomada` o `alFalloCaptura` (plan 5.4).
  RF: RF-924, RF-925, RF-928
  Hecho cuando: compila.

- [ ] **T75 · Conectar «Evidencia fotográfica» en la navegación**
  En `CapturaNavigation.kt`, reemplazar la `PantallaEnConstruccion` de `CapturarEvidencia`; «Listo» y «Volver»
  regresan.
  RF: RF-924, RF-930
  Hecho cuando: «Agregar foto» del detalle abre la pantalla.

- [ ] **T76 · Verificación manual en el celular**
  Los puntos de K de la sección 7, y el tamaño de cada foto con `adb shell run-as com.softcorp.sigtec ls -l
  files/evidencias/<incidencia>` (unos 300 a 500 KB).
  RF: RF-924 a RF-931, RNF almacenamiento
  Hecho cuando: el resultado queda escrito en el PR.

- [ ] **T77 · Abrir el PR K**
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR L — fotos en el detalle (`feat/hu09-evidencias-detalle`, apilada sobre J)

- [ ] **T78 · Crear la rama**
  Desde la rama de J (o desde `main`, si J ya se fusionó).
  RF: —
  Hecho cuando: la rama tiene debajo los commits de J.

- [ ] **T79 · El repositorio de incidencias lee las fotos**
  `observarEvidencias(incidenciaId)` en `IncidenciaRepository` y en `IncidenciaRepositoryImpl`, que inyecta
  `EvidenciaDao` solo para leer.
  RF: RF-932, RF-936
  Hecho cuando: compila.

- [ ] **T80 · Fotos en `IncidenciaRepositoryFalso`**
  Lista de fotos en memoria por incidencia.
  RF: —
  Hecho cuando: las pruebas existentes siguen pasando.

- [ ] **T81 · El detalle observa las fotos**
  `DetalleIncidencia.evidencias` (por defecto vacía) y el `combine` de `ObservarDetalleIncidencia`.
  RF: RF-932, RF-936
  Hecho cuando: compila.

- [ ] **T82 · Pruebas del detalle con fotos**
  En `ObservarDetalleIncidenciaTest`: con fotos, sin fotos y en orden.
  RF: RF-932, RF-936
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ObservarDetalleIncidenciaTest"` pasa.

- [ ] **T83 · `SeccionEvidencias`**
  Composable sin estado de `feature/captura/presentation` (plan 5.5), con vistas previas con y sin fotos, con y
  sin «Agregar foto», en claro y oscuro.
  RF: RF-923, RF-932, RF-933
  Hecho cuando: las vistas previas coinciden con la sección «Evidencias» del prototipo 5.

- [ ] **T84 · `FotoAmpliada`**
  `Dialog` a pantalla completa con la foto, su tipo y su fecha y hora (plan 5.5).
  RF: RF-934, RF-935
  Hecho cuando: compila y su vista previa se ve en claro y oscuro.

- [ ] **T85 · Fotos en `DetalleIncidenciaScreen`**
  Cambiar la sección actual por `SeccionEvidencias(puedeAgregar = estado.puedeAtender)` y abrir `FotoAmpliada` al
  tocar una miniatura; vistas previas con fotos.
  RF: RF-923, RF-932 a RF-936
  Hecho cuando: «Agregar foto» ya no se ve como jefe ni como personal de sistemas en las vistas previas.

- [ ] **T86 · Pruebas del `ViewModel` del detalle**
  En `DetalleIncidenciaViewModelTest`: las fotos llegan al estado y `puedeAtender` sigue la regla de RF-923.
  RF: RF-923, RF-932
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*DetalleIncidenciaViewModelTest"` pasa.

- [ ] **T87 · Verificación manual en el celular**
  Con K fusionado: los puntos de L de la sección 7 (también otro técnico y el jefe en otro celular).
  RF: RF-923, RF-932 a RF-936, RF-939
  Hecho cuando: el resultado queda escrito en el PR.

- [ ] **T88 · Abrir el PR L**
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR M — eliminar una foto (`feat/hu09-eliminar-foto`, apilada sobre L) — solo con D-5

- [ ] **T89 · Crear la rama**
  RF: —
  Hecho cuando: la rama tiene debajo los commits de L.

- [ ] **T90 · El repositorio elimina la foto y su archivo**
  `eliminar(evidencia)` en `EvidenciaRepository`, `EvidenciaRepositoryImpl` (primero la fila, después el archivo)
  y `EvidenciaRepositoryFalso`.
  RF: RF-937
  Hecho cuando: compila.

- [ ] **T91 · `EliminarEvidencia`**
  Caso de uso de 4.9.
  RF: RF-937
  Hecho cuando: compila.

- [ ] **T92 · `EliminarEvidenciaTest`**
  Sin permiso, otro técnico, incidencia cerrada, correcto y fallo del repositorio.
  RF: RF-937
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*EliminarEvidenciaTest"` pasa.

- [ ] **T93 · «Eliminar foto» en `FotoAmpliada`**
  `BotonPeligro` y su `AlertDialog` («¿Eliminar esta foto? No se puede deshacer.»), solo con `puedeEliminar`.
  RF: RF-937
  Hecho cuando: la vista previa con `puedeEliminar` muestra el botón y la otra no.

- [ ] **T94 · Eliminar desde el detalle**
  `alEliminarFoto(evidencia)` y su error en `DetalleIncidenciaViewModel` (inyecta `EliminarEvidencia`, P-7);
  `DetalleIncidenciaScreen` pasa `puedeEliminar = estado.puedeAtender`.
  RF: RF-937
  Hecho cuando: compila.

- [ ] **T95 · Pruebas de eliminar desde el detalle**
  En `DetalleIncidenciaViewModelTest`: con éxito y con error.
  RF: RF-937
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*DetalleIncidenciaViewModelTest"` pasa.

- [ ] **T96 · Verificación manual y abrir el PR M**
  Eliminar una foto en el celular y comprobar que su archivo ya no está.
  RF: RF-937
  Hecho cuando: el PR está abierto con el CI en verde y la prueba escrita.

## PR N — «Evidencias» en el informe técnico (`feat/hu09-evidencias-informe`, apilada sobre L) — solo con D-6

- [ ] **T97 · Crear la rama**
  RF: —
  Hecho cuando: la rama tiene debajo los commits de L.

- [ ] **T98 · El informe observa las fotos 🔶**
  `InformeTecnicoViewModel` combina `ObservarCapturaEvidencia(incidenciaId)` en su estado.
  RF: RF-938
  Hecho cuando: compila.

- [ ] **T99 · Pruebas del informe con fotos 🔶**
  En `InformeTecnicoViewModelTest`: las fotos llegan al estado y lo escrito se conserva.
  RF: RF-938
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*InformeTecnicoViewModelTest"` pasa.

- [ ] **T100 · Sección «Evidencias» en el informe 🔶**
  `SeccionEvidencias` antes de «Confirmar cierre con huella» en `InformeTecnicoScreen`; vistas previas.
  RF: RF-938
  Hecho cuando: la vista previa coincide con la sección «Evidencias» del prototipo 9.

- [ ] **T101 · «Agregar foto» desde el informe 🔶**
  La Route recibe `alAgregarFoto` y `AtencionNavigation` navega a `Ruta.CapturarEvidencia(id)`.
  RF: RF-938
  Hecho cuando: compila.

- [ ] **T102 · Verificación manual y abrir el PR N**
  En el celular: escribir parte del informe, agregar una foto, volver y comprobar que lo escrito sigue ahí.
  RF: RF-938
  Hecho cuando: el PR está abierto con el CI en verde y la prueba escrita.

## PR O — borrar las fotos con los datos locales (`feat/hu09-borrar-fotos-sesion`, desde `main` con G) — solo con D-7

- [ ] **T103 · Crear la rama desde `main` con G fusionado**
  RF: —
  Hecho cuando: `main` tiene `AlmacenFotos`.

- [ ] **T104 · Borrar las fotos al cerrar sesión 🔶**
  `SesionRepositoryFirebase` inyecta `AlmacenFotos` y llama a `borrarTodo()` después de `clearAllTables()` en
  `cerrarSesion()` (plan 4.10).
  RF: RF-942
  Hecho cuando: compila.

- [ ] **T105 · Borrar las fotos al entrar otra persona 🔶**
  Lo mismo en `SincronizacionTiempoReal`, después del `clearAllTables()` de `debeBorrarDatosAlEntrar`.
  RF: RF-942
  Hecho cuando: compila.

- [ ] **T106 · RF-10 y RF-11 de la spec 001 incluyen las fotos**
  En `specs/001-inicio-sesion/spec.md`, quitar «Las fotos se agregarán cuando exista la HU de captura» y nombrar las
  fotos entre los datos locales.
  RF: RF-942
  Hecho cuando: el texto de la spec 001 coincide con el código.

- [ ] **T107 · Pendiente de las fotos resuelto en `MEMORY.md`**
  `git pull origin main` y quitar la línea «Fotos: cuando exista la HU de captura… (D-7)».
  RF: —
  Hecho cuando: la línea ya no está.

- [ ] **T108 · Verificación manual en el celular**
  Los puntos de O de la sección 7 (cerrar sesión, y entrar otra persona sin cerrar).
  RF: RF-942
  Hecho cuando: el resultado queda escrito en el PR.

- [ ] **T109 · Abrir el PR O**
  Pedir a Alonso que revise con foco el modelo de sesión y `core/sync`.
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.

## PR Q — aviso de las fotos al cerrar sesión (`feat/hu09-aviso-fotos-cierre`, apilada sobre O) — solo con D-8 (a)

- [ ] **T110 · Escribir las tareas de Q**
  Cuando se responda D-8, agregar aquí sus tareas (RF nuevo en la spec 009 y ajuste de RF-13 en la spec 001).
  RF: —
  Hecho cuando: las tareas están escritas y aprobadas.

## PR P — pruebas manuales y cierre (`docs/hu09-cierre`, desde la rama del último PR fusionado)

- [ ] **T111 · Crear la rama y comprobar que no quedó nada fuera**
  Para cada rama apilada, `git log main..<rama>` no debe mostrar commits sin fusionar (lo que pasó en la HU-08).
  RF: —
  Hecho cuando: ninguna rama de la HU tiene commits fuera de `main`.

- [ ] **T112 · Pruebas manuales de punta a punta**
  Las de «Criterios de finalización» de la spec, en un celular real, con capturas en claro y oscuro.
  RF: RF-901 a RF-942
  Hecho cuando: cada prueba queda anotada con su resultado en el PR.

- [ ] **T113 · Spec completa**
  «Estado: borrador» → «Estado: completa», con la fecha y qué se probó; quitar de la spec los RF de las decisiones
  que no se aprobaron.
  RF: —
  Hecho cuando: la spec describe lo que hace el código.

- [ ] **T114 · Tareas marcadas**
  Marcar en este archivo las tareas hechas y anotar las que no se hicieron y por qué.
  RF: —
  Hecho cuando: no queda ninguna casilla sin explicar.

- [ ] **T115 · HU-09 completa en `MEMORY.md`**
  `git pull origin main` y dejar la línea de la HU-09 como «completa (RF-901 a RF-942)».
  RF: —
  Hecho cuando: la línea está actualizada.

- [ ] **T116 · Abrir el PR P**
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.
