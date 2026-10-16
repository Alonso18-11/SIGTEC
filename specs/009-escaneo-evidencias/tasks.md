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

## PR-32 — `CapturaEvidenciaViewModel` (base PR-31, trae PR-27, PR-29 y PR-30)
- [ ] **T57 · `CapturaEvidenciaViewModel`** con el tipo en el `SavedStateHandle` (4.8). RF-924 a RF-931.
  Hecho cuando: compila con Hilt.
- [ ] **T58 · `CapturaEvidenciaViewModelTest`** (siete casos). Mismos RF. Hecho cuando: la prueba pasa.

## PR-33 — miniatura (base `main`)
- [ ] **T59 · Decodificar reducida y con la orientación del EXIF** (5.6, P-6). RF-932, RF-935.
  Hecho cuando: compila.
- [ ] **T60 · `MiniaturaEvidencia`** con carga, imagen y «Foto no disponible». RF-932, RF-935.
  Hecho cuando: sus vistas previas muestran la carga y «Foto no disponible».

## PR-34 — «Evidencia fotográfica» sin cámara (base PR-32, trae PR-33)
- [ ] **T61 · Pantalla con el título, el subtítulo y el selector** (5.4). RF-924.
  Hecho cuando: compila.
- [ ] **T62 · Miniaturas, disparador, «N fotos», «Listo» y mensajes**, con vistas previas (0 y 2 fotos, guardando,
  al máximo). RF-927, RF-929 a RF-931.
  Hecho cuando: las vistas previas coinciden con el prototipo 14, salvo la barra (D-10) y la linterna (HU-10).

## PR-35 — tomar la foto (base `main`)
- [ ] **T63 · `TomarFoto`**: `ImageCapture` cercano a 1600 × 1200 con `setJpegQuality(85)` y una función `suspend`
  que escribe en la ruta (P-5). RF-925. Hecho cuando: compila.

## PR-36 — captura con la cámara (base PR-34, trae PR-13, PR-14 y PR-35)
- [ ] **T64 · La Route arma el permiso y la cámara** y avisa `alFotoTomada` o `alFalloCaptura`. RF-924, RF-925, RF-928.
  Hecho cuando: compila; la prueba en el celular va con PR-37.

## PR-37 — captura en la navegación (base PR-36, trae PR-17)
- [ ] **T65 · Reemplazar la `PantallaEnConstruccion` de `CapturarEvidencia`.** RF-924, RF-930.
  Hecho cuando: en el celular se cumplen los puntos de PR-36 y PR-37 de la sección 7 (tamaño de las fotos incluido).

## PR-38 — el repositorio de incidencias lee las fotos (base `main`)
- [ ] **T66 · `observarEvidencias`** en `IncidenciaRepository` e `IncidenciaRepositoryImpl`. RF-932, RF-936.
  Hecho cuando: compila.
- [ ] **T67 · Fotos en `IncidenciaRepositoryFalso`.** RF: — Hecho cuando: las pruebas existentes pasan.

## PR-39 — el detalle observa las fotos (base PR-38)
- [ ] **T68 · `DetalleIncidencia.evidencias`** en `ObservarDetalleIncidencia`. RF-932, RF-936. Hecho cuando: compila.
- [ ] **T69 · Casos nuevos de `ObservarDetalleIncidenciaTest`.** Mismos RF. Hecho cuando: la prueba pasa.

## PR-40 — sección «Evidencias» (base PR-33)
- [ ] **T70 · `SeccionEvidencias`** con vistas previas (5.5). RF-923, RF-932, RF-933.
  Hecho cuando: las vistas previas coinciden con la sección del prototipo 5.

## PR-41 — foto ampliada (base PR-33)
- [ ] **T71 · `FotoAmpliada`** (5.5). RF-934, RF-935. Hecho cuando: su vista previa se ve en claro y oscuro.

## PR-42 — fotos en el detalle (base PR-39, trae PR-40 y PR-41)
- [ ] **T72 · `SeccionEvidencias(puedeAgregar = estado.puedeAtender)`** en `DetalleIncidenciaScreen`. RF-923, RF-932,
  RF-933, RF-936. Hecho cuando: las vistas previas del jefe no muestran «Agregar foto».
- [ ] **T73 · Ampliar una foto desde el detalle.** RF-934, RF-935. Hecho cuando: compila.
- [ ] **T74 · Casos nuevos de `DetalleIncidenciaViewModelTest`.** RF-923, RF-932. Hecho cuando: la prueba pasa.

## PR-43 — el repositorio elimina (base PR-27)
- [ ] **T75 · `eliminar`** en `EvidenciaRepository` y `EvidenciaRepositoryImpl` (fila y luego archivo). RF-937.
  Hecho cuando: compila.
- [ ] **T76 · `eliminar` en `EvidenciaRepositoryFalso`.** RF: — Hecho cuando: compila con `testDebugUnitTest`.

## PR-44 — `EliminarEvidencia` (base PR-43, trae PR-25)
- [ ] **T77 · `EliminarEvidencia`** (4.9). RF-937. Hecho cuando: compila.
- [ ] **T78 · `EliminarEvidenciaTest`.** RF-937. Hecho cuando: la prueba pasa.

## PR-45 — botón «Eliminar foto» (base PR-41)
- [ ] **T79 · `BotonPeligro` y su confirmación** en `FotoAmpliada`. RF-937.
  Hecho cuando: la vista previa con `puedeEliminar` muestra el botón y la otra no.

## PR-46 — eliminar desde el detalle (base PR-42, trae PR-44 y PR-45)
- [ ] **T80 · `alEliminarFoto`** en `DetalleIncidenciaViewModel` y `puedeEliminar` en la pantalla. RF-937.
  Hecho cuando: compila.
- [ ] **T81 · Casos de eliminar en `DetalleIncidenciaViewModelTest`.** RF-937.
  Hecho cuando: la prueba pasa y, en el celular, el archivo de la foto eliminada ya no está.

## PR-47 — el informe observa las fotos (base PR-31, trae PR-27)
- [ ] **T82 · `InformeTecnicoViewModel` combina `ObservarCapturaEvidencia` 🔶.** RF-938. Hecho cuando: compila.
- [ ] **T83 · Casos nuevos de `InformeTecnicoViewModelTest` 🔶.** RF-938. Hecho cuando: la prueba pasa.

## PR-48 — sección en el informe (base PR-47, trae PR-40)
- [ ] **T84 · `SeccionEvidencias` antes de «Confirmar cierre con huella» 🔶.** RF-938.
  Hecho cuando: la vista previa coincide con el prototipo 9.

## PR-49 — «Agregar foto» desde el informe (base PR-48)
- [ ] **T85 · `alAgregarFoto` en la Route y en `AtencionNavigation` 🔶.** RF-938.
  Hecho cuando: en el celular, lo escrito en el informe sigue ahí al volver de tomar una foto.

## PR-50 — borrar al cerrar sesión (base PR-21)
- [ ] **T86 · `borrarTodo()` después de `clearAllTables()` en `SesionRepositoryFirebase` 🔶** (4.10). RF-942.
  Hecho cuando: compila.
- [ ] **T87 · RF-10 de la spec 001 incluye las fotos.** RF-942.
  Hecho cuando: en el celular, la carpeta queda vacía después de cerrar sesión.

## PR-51 — borrar al entrar otra persona (base PR-21)
- [ ] **T88 · Lo mismo en `SincronizacionTiempoReal` 🔶.** RF-942. Hecho cuando: compila.
- [ ] **T89 · RF-11 de la spec 001 incluye las fotos.** RF-942. Hecho cuando: el texto coincide con el código.
- [ ] **T90 · Quitar de `MEMORY.md` el pendiente de las fotos**, después de `git pull origin main`. RF: —
  Hecho cuando: en el celular, la carpeta queda vacía al entrar otra persona.

## PR-52 — contar las fotos (base `main`)
- [ ] **T91 · `EvidenciaDao.contar(): Flow<Int>` 🔶.** RF-943. Hecho cuando: compila.
- [ ] **T92 · `EvidenciaDaoTest` (instrumentada).** RF-932, RF-943.
  Hecho cuando: `./gradlew connectedDebugAndroidTest` pasa en un celular o emulador.

## PR-53 — el estado de los datos cuenta las fotos (base PR-52)
- [ ] **T93 · `EstadoDatos.fotosEnCelular = 0` ⚠️.** RF-943. Hecho cuando: compila.
- [ ] **T94 · `EstadoDatosRepositoryImpl` combina `EvidenciaDao.contar()` 🔶.** RF-943. Hecho cuando: compila.

## PR-54 — la regla de borrado cuenta las fotos (base PR-53)
- [ ] **T95 · `evaluarBorrado(pendientes, fotos = 0)` y `DecisionBorrado.Confirmar` con fotos 🔶** (4.11). RF-943.
  Hecho cuando: compila.
- [ ] **T96 · Casos nuevos de `ReglasSesionTest`.** RF-943. Hecho cuando: la prueba pasa.
- [ ] **T97 · `EvaluarCierreSesion` pasa las fotos 🔶.** RF-943. Hecho cuando: compila.
- [ ] **T98 · Casos nuevos de `EvaluarCierreSesionTest`.** RF-943. Hecho cuando: la prueba pasa.

## PR-55 — textos de las fotos (base `main`)
- [ ] **T99 · `textoFotosEvidencia` 🔶.** RF-943. Hecho cuando: compila.
- [ ] **T100 · `textoAvisoSalida`** con cambios, con fotos y con ambos 🔶 (4.11). RF-943. Hecho cuando: compila.
- [ ] **T101 · Casos nuevos de `TextosSalidaTest`.** RF-943. Hecho cuando: la prueba pasa.

## PR-56 — el aviso de cierre cuenta las fotos (base PR-54, trae PR-55)
- [ ] **T102 · El diálogo de `AjustesSeguridadScreen` usa `textoAvisoSalida` 🔶.** RF-943.
  Hecho cuando: sus vistas previas muestran los tres casos en claro y oscuro.
- [ ] **T103 · RF-13 de la spec 001 cuenta las fotos.** RF-943.
  Hecho cuando: en el celular se ve el aviso con fotos y sin cambios, y con los dos.

## PR-57 — cierre (base: la rama del último PR fusionado)
- [ ] **T104 · Comprobar que nada quedó fuera**: `git log main..<rama>` vacío en cada rama de la HU. RF: —
  Hecho cuando: ninguna rama tiene commits sin fusionar.
- [ ] **T105 · Pruebas manuales de punta a punta** («Criterios de finalización» de la spec) en un celular real.
  RF-901 a RF-943. Hecho cuando: cada prueba está anotada con su resultado.
- [ ] **T106 · Spec «completa»**, con la fecha y qué se probó. RF: — Hecho cuando: la spec describe el código.
- [ ] **T107 · Tareas marcadas**, con el motivo de las que no se hicieron. RF: — Hecho cuando: no queda ninguna sin explicar.
- [ ] **T108 · HU-09 «completa (RF-901 a RF-943)» en `MEMORY.md`**, después de `git pull origin main`. RF: —
  Hecho cuando: la línea está actualizada.
