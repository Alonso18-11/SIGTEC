# Tareas 008 — Bandeja del técnico y cierre con informe confirmado por huella (HU-08)

> Salen de `spec.md` y `plan.md`. Cada tarea dura entre 20 y 30 minutos como máximo y depende solo de las
> anteriores de su PR. Van agrupadas por PR (A a F, plan sección 1) y, dentro de cada uno, de adentro hacia
> afuera: dominio/reglas puras con sus pruebas → Room/DAO → `core/sync` o repositorio → `ViewModel` → pantalla
> → verificación.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas, sin subcarpetas). ⚠️ =
> archivo compartido de `CLAUDE.md` **ya autorizado para esta HU**; 🔶 = sensible fuera de esa lista (necesita
> el visto bueno de este plan, igual que en la HU-06).
> Orden de fusión: A → B y C en cualquier orden → D (después de C) → E (después de A y B) → F (último).

## PR A — migración del informe e «Iniciar atención» (`feat/hu08-migracion-iniciar-atencion`, desde `main`)

- [ ] **T01 · Crear la rama desde `main` al día**
  `git pull origin main` y `git switch -c feat/hu08-migracion-iniciar-atencion`.
  RF: —
  Hecho cuando: `git branch --show-current` muestra la rama y `git log -1` coincide con `origin/main`.

- [x] **T02 · Corregir la frase de tareas ya resueltas en la HU-06 (docs)**
  En `specs/006-asignacion-tecnico/tasks.md`, la sección «Revisión posterior a los PR #25 y #26» decía que
  T23 a T25 y T40 a T45 seguían pendientes, pero ya están hechas (la spec 006 está «completa» desde el
  2026-10-12). Se agrega una nota de corrección con la fecha, sin borrar la frase original.
  RF: — (pedido explícito del usuario para el primer PR de esta HU)
  Hecho cuando: el archivo tiene la nota de corrección.
  > Hecho por el planificador el 2026-10-12, junto con este plan y estas tareas.

- [x] **T03 · Migración de `InformeTecnico`/`InformeTecnicoEntity` ⚠️**
  Agregar `diagnostico: String` (antes de `trabajoRealizado`) en `core/domain/model/InformeTecnico.kt` y en
  `core/database/entity/InformeTecnicoEntity.kt`, más `necesitaAtencion: Boolean = false` solo en la entidad
  (la usa el PR C). Actualizar `toDomain()`/`toEntity()`. Subir `version` a 3 en `core/database/SigtecDatabase.kt`.
  RF: RF-813, RNF persistencia local
  Hecho cuando: `./gradlew assembleDebug` compila y `SigtecDatabase.kt` dice `version = 3`.

- [x] **T04 · `MarcaCambiada` y su mensaje ⚠️**
  `data object MarcaCambiada : ErrorDominio` en `core/domain/Resultado.kt`, con su texto («Esta incidencia ya
  cambió de estado. Vuelve a abrirla para ver su situación actual.») en `core/ui/MensajesError.kt`.
  RF: RF-808, RF-812
  Hecho cuando: `./gradlew assembleDebug` compila (el `when` exhaustivo de `mensaje()` cierra).

- [x] **T05 · `IncidenciaRepository` ampliado**
  Agregar `suspend fun porId(id): Incidencia?`, `suspend fun iniciarAtencion(incidencia, movimiento, ahora): Resultado<Unit>`
  y `fun observarInforme(incidenciaId): Flow<InformeTecnico?>` a la interfaz y a `IncidenciaRepositoryImpl`
  (inyecta `InformeTecnicoDao` solo para la lectura). Ampliar `IncidenciaRepositoryFalso` con los tres métodos
  en memoria, con `fallar`.
  RF: RF-807–RF-810, RF-828
  Hecho cuando: `./gradlew assembleDebug testDebugUnitTest` compila y pasa.

- [x] **T06 · `IniciarAtencion` con su prueba**
  Caso de uso de la sección 4.1 del plan (`@Inject constructor`, `operator fun invoke(incidenciaId, ahora)`),
  con `bloqueoParaIniciar` (3.2) como función pura en el mismo archivo. `IniciarAtencionTest` con los 5 casos
  de `plan.md` sección 7.
  RF: RF-806–RF-811
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*IniciarAtencionTest"` pasa.

- [x] **T07 · `ObservarDetalleIncidencia` ampliado con su prueba**
  `DetalleIncidencia` agrega `informe: InformeTecnico?`, combinando `repo.observarInforme(id)` con lo que ya
  combinaba. `ObservarDetalleIncidenciaTest` con los 3 casos de `plan.md` sección 7.
  RF: RF-827, RF-828, RF-829
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ObservarDetalleIncidenciaTest"` pasa.

- [x] **T08 · Verificación y PR A**
  Correr el comando del CI. El PR no cambia nada visible (lo visible llega en el PR E): así se indica en
  «Cómo probarlo». Hacer los 6 commits de `plan.md` (PR A, incluidos los 2 de docs de T02) y abrir el PR en
  borrador con `.github/pull_request_template.md`, marcando `InformeTecnico.kt`, `InformeTecnicoEntity.kt`,
  `SigtecDatabase.kt`, `Resultado.kt` y `MensajesError.kt` en «Archivos compartidos que toca» (ya autorizados
  por el usuario, según la spec).
  RF: —
  Hecho cuando: CI en verde y aprobación de la otra persona del equipo.

## PR B — dominio y datos de «cerrar con informe» y la bandeja (`feat/hu08-atencion-dominio`, desde `main` con A)

- [x] **T09 · Crear la rama desde `main` con A fusionado**
  Si A todavía no se fusionó, crear la rama desde la de A (apilada) y rebasar cuando A se fusione.
  RF: —
  Hecho cuando: `git log` muestra los commits de A en la rama.

- [x] **T10 · `AtencionRepository`, `ObservarBandeja` y su doble falso**
  Interfaz en `feature/atencion/domain/AtencionRepository.kt` (`incidencia(id)`, `observarBandeja(tecnicoId)`,
  `cerrarConInforme(...)`); `ObservarBandeja` combinando `ObservarUsuarioActual` con el repositorio;
  `AtencionRepositoryFalso` en `app/src/test/.../feature/atencion/data`, en memoria y con `fallar`.
  `ObservarBandejaTest` con los 3 casos de `plan.md` sección 7.
  RF: RF-801, RF-803, RF-805
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ObservarBandejaTest"` pasa.

- [x] **T11 · `CerrarIncidenciaConInforme` con su prueba**
  Caso de uso de la sección 4.2 del plan, con `bloqueoParaCerrar` (3.3) y `recortarCampos` (3.4) como
  funciones puras en el mismo archivo. `CerrarIncidenciaConInformeTest` con los casos de `plan.md` sección 7.
  RF: RF-812–RF-820
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*CerrarIncidenciaConInformeTest"` pasa.

- [x] **T12 · `AtencionRepositoryImpl` y `AtencionModule`**
  Implementación de la sección 4.3 del plan (`@IoDispatcher`, `db.withTransaction`, `catch` que relanza
  `CancellationException` y registra con `Log.w`) y `@Binds` en `feature/atencion/di/AtencionModule.kt`.
  RF: RF-819, RF-824
  Hecho cuando: `./gradlew assembleDebug` compila (Hilt resuelve el repositorio).

- [x] **T13 · Verificación y PR B**
  Correr el comando del CI. El PR no cambia nada visible (llega en E): así se indica en «Cómo probarlo».
  Hacer los 3 commits de `plan.md` (PR B).
  RF: RF-801, RF-803, RF-805, RF-812–RF-820, RF-824
  Hecho cuando: CI en verde y aprobación de la otra persona del equipo.

## PR C — restar la carga al cerrar y sincronizar el informe (`feat/sync-cierre-informe`, desde `main` con A)

- [x] **T14 · Crear la rama desde `main` con A fusionado**
  Puede avanzar en paralelo con B: no comparten archivos.
  RF: —
  Hecho cuando: `git log` muestra los commits de A en la rama.

- [x] **T15 · `cambiosDeCarga` ampliada 🔶**
  En `core/sync/ReglasSincronizacion.kt`, agregar los parámetros `cerradaServidor`/`cerradaLocal` (plan 3.1).
  Casos nuevos en `ReglasSincronizacionTest`: cerrar sin cambiar de técnico, reabrir, sin cambios, y los casos
  de RF-620 que ya existían (deben seguir pasando con la firma nueva).
  RF: RF-822
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ReglasSincronizacionTest"` pasa.

- [x] **T16 · `actualizarConCarga` pasa la marca de antes y de después 🔶**
  En `core/sync/SincronizadorIncidencias.kt`, `actualizarConCarga` lee `marca` del documento remoto y de
  `local.marca` y los pasa a `cambiosDeCarga` (plan 4.4).
  RF: RF-822
  Hecho cuando: compila y una incidencia que no cambia de técnico ni se cierra se sube igual que antes (sin
  escrituras en `usuarios`).

- [x] **T17 · Consultas de rechazo en `InformeTecnicoDao` 🔶**
  `listosParaSubir()`, `marcarSincronizado(id)`, `marcarNecesitaAtencion(id)`, `marcarNecesitaAtencionDe(incidenciaId)`
  y `borrarRechazados()` (plan sección 4.5, sin la rama de «incidencia sin número»). Nueva
  `app/src/androidTest/.../InformeTecnicoDaoTest.kt`: `listosParaSubir` con padre pendiente (no sale),
  rechazado (no sale) y sincronizado (sale); `marcarNecesitaAtencionDe`; `borrarRechazados`.
  RF: RF-825
  Hecho cuando: `connectedDebugAndroidTest` pasa en un celular.
  > Implementado el 2026-10-12: sin celular/emulador conectado (`adb devices` vacío), no se pudo correr
  > `connectedDebugAndroidTest`. Se verificó que compila con `./gradlew compileDebugAndroidTestSources`
  > (BUILD SUCCESSFUL) y que `./gradlew assembleDebug testDebugUnitTest` sigue en verde (213/213). Pendiente
  > correr `connectedDebugAndroidTest` en un celular real antes de cerrar el PR C (T21).

- [x] **T18 · El informe espera a su incidencia al subir 🔶**
  En `SincronizadorIncidencias.kt`: nuevo `subir(informe: InformeTecnicoEntity)` (documento
  `informes/{informe.incidenciaId}`, no `informe.id`); `subirPendientes` recorre
  `informeTecnicoDao.listosParaSubir()` después de los movimientos; `rechazar(local)` también llama a
  `informeTecnicoDao.marcarNecesitaAtencionDe(local.id)`; `hayPendientes()` cuenta también los informes.
  RF: RF-824, RF-825
  Hecho cuando: `./gradlew assembleDebug` compila; con una incidencia que falla al subir, su informe sigue
  pendiente (prueba manual completa en el PR F).
  > Implementado el 2026-10-12: para que `subir(informe)` compilara hubo que adelantar también
  > `InformeTecnicoEntity.aFirestore()` (T19) en `MapeoFirestore.kt`, con `diagnostico` incluido —no hay
  > mapeo mínimo "temporal" distinto del definitivo, es la misma función; T19 solo queda por confirmarse
  > y marcarse como hecha. `213/213` pruebas JVM siguen en verde (ninguna prueba nueva: T18 no pedía una,
  > y `aFirestore()` tampoco tiene prueba propia en el plan, igual que el resto de funciones de mapeo).

- [x] **T19 · `InformeTecnicoEntity.aFirestore()` 🔶**
  Función de mapeo en `core/sync/MapeoFirestore.kt`, con `diagnostico` incluido.
  RF: RF-825, RNF trazabilidad
  Hecho cuando: `./gradlew assembleDebug` compila.
  > Verificado el 2026-10-12: la función que adelantó T18 ya cumplía exactamente lo pedido — los 6 campos
  > del RNF de trazabilidad (`diagnostico`, `trabajoRealizado`, `piezasCambiadas`, `observaciones`,
  > `fechaCierre`, `confirmadoEn`) con los nombres exactos, mismo estilo `mapOf(...)` e `Instant.aTimestamp()`
  > que `IncidenciaEntity.aFirestore()`/`MovimientoEntity.aFirestore()`. No se tocó código.
  > `./gradlew assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL, 213/213 pruebas JVM en verde.

- [x] **T20 · `descartarRechazados()` también borra informes 🔶**
  En `core/sync/EstadoDatosRepositoryImpl.kt`, la transacción de `descartarRechazados()` agrega
  `informeTecnicoDao.borrarRechazados()`.
  RF: RF-825
  Hecho cuando: `./gradlew assembleDebug testDebugUnitTest` compila y pasa.
  > Implementado el 2026-10-12: se inyectó `InformeTecnicoDao` en el constructor (mismo patrón que
  > `movimientoDao`/`incidenciaDao`, provisto por Hilt vía `DatabaseModule`) y se agregó la llamada
  > justo después de `movimientoDao.borrarRechazados()`, antes de las dos llamadas de `incidenciaDao`,
  > igual que el bloque del plan 4.5. No existía ningún `EstadoDatosRepositoryImplTest.kt` que probara
  > `descartarRechazados()` (solo hay un comentario de referencia en `MovimientoDaoTest.kt`, que no
  > construye el repositorio), así que no se creó una prueba nueva, como ya indicaba la tarea.
  > `./gradlew assembleDebug testDebugUnitTest lintDebug`: BUILD SUCCESSFUL, 213/213 pruebas JVM en
  > verde (sin pruebas nuevas), sin warnings de lint nuevos en el archivo tocado.

- [x] **T21 · Verificación y PR C**
  Correr el comando del CI. La transacción real de la carga al cerrar y la subida real del informe no se
  pueden probar sin la pantalla de E: se prueban en el PR F, y así se indica en «Cómo probarlo». Hacer los 4
  commits de `plan.md` (PR C).
  RF: RF-822, RF-824, RF-825
  Hecho cuando: CI en verde y aprobación de la otra persona del equipo.
  > Implementado el 2026-10-12: `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde (213/213
  > pruebas JVM) antes de cada commit. Los 4 commits de `plan.md` se hicieron en local, en orden; sin
  > `git push` ni PR en GitHub (decisión explícita de esta sesión). `SincronizadorIncidencias.kt` se dividió
  > entre los commits 2 y 3 reconstruyendo manualmente la versión intermedia (solo el cambio de T16, marca
  > de antes/después en `actualizarConCarga`), porque el hunk de imports mezclaba una línea de T16 con dos
  > de T18 demasiado cerca para que `git add -p` las separara solo; no fue necesario meter todo el archivo
  > en un único commit. La aprobación de la otra persona del equipo y el PR en GitHub quedan pendientes:
  > esta sesión no abre PR.

## PR D — reglas de Firestore (`feat/reglas-cierre-informe`, apilada sobre C)

- [ ] **T22 · Crear la rama apilada sobre C**
  RF: —
  Hecho cuando: `git log` muestra los commits de A y C en la rama.

- [ ] **T23 · Reglas de `incidencias` e `informes` ⚠️**
  Aplicar el diff de `plan.md` sección 4.6 a `firebase/firestore.rules`: funciones `marcaAntes()`,
  `marcaDespues()`, `seCierra()`, `cargaRestadaAlCerrar()`; la rama del técnico en `incidencias.update` exige
  la resta cuando `seCierra()`; `informes.create` exige `diagnostico is string && diagnostico.size() > 0`.
  RF: RF-823, RNF trazabilidad
  Hecho cuando: `git diff` solo muestra esas funciones y esas dos condiciones nuevas.

- [ ] **T24 · Rules Playground — a cargo del usuario, no de un agente**
  Probar con usuarios reales: el técnico asignado cierra y resta su propia carga en la misma escritura
  (puede salir «error desconocido» si el Playground no simula `getAfter()`, igual que en la HU-06: se prueba
  desde la app en T39); el técnico cierra sin restar la carga (denegado); crear un informe con `diagnostico`
  vacío o ausente (denegado); crear un informe completo del técnico asignado (permitido).
  RF: RF-823
  Hecho cuando: los 4 casos dan el resultado esperado (o, si el Playground no puede simularlo, se anota para
  probarse desde la app).

- [ ] **T25 · Verificar la carga en la consola — a cargo del usuario**
  Antes de publicar, comprobar para cada técnico que `usuarios/{uid}.incidenciasActivas` sigue coincidiendo
  con sus incidencias pendientes asignadas (igual que T24 de la HU-06).
  RF: RF-823
  Hecho cuando: la carga de cada técnico coincide con lo que muestra el filtro por técnico de la lista.

- [ ] **T26 · Publicar las reglas — a cargo del usuario o de la otra persona del equipo**
  Pegar `firebase/firestore.rules` en la consola después de T24 y T25.
  RF: RF-823
  Hecho cuando: la consola muestra la versión publicada con la fecha, anotada en `MEMORY.md`.

- [ ] **T27 · Verificación y PR D**
  Correr el comando del CI (no toca código de la app, solo reglas). Hacer los 2 commits de `plan.md` (PR D).
  RF: RF-823
  Hecho cuando: CI en verde y aprobación de la otra persona del equipo.

## PR E — pantallas (`feat/hu08-pantallas`, apilada sobre B; necesita A y B fusionados)

- [x] **T28 · Crear la rama apilada sobre B**
  RF: —
  Hecho cuando: `git log` muestra los commits de A y B en la rama.

- [x] **T29 · `MisTareasViewModel` con su prueba**
  `MisTareasUiState` con reloj de un minuto (mismo patrón que `AsignacionViewModel`/`ListaIncidenciasViewModel`,
  `MEMORY.md`), combinando `ObservarBandeja()`. `MisTareasViewModelTest` con los casos de `plan.md` sección 7.
  RF: RF-801, RF-803, RF-805
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*MisTareasViewModelTest"` pasa.

- [x] **T30 · `MisTareasScreen` según `plan.md` sección 5**
  `MisTareasRoute` (`hiltViewModel()`, `collectAsStateWithLifecycle()`) y `MisTareasScreen` (solo dibuja), con
  tarjetas que reutilizan `EtiquetaIncidencia`, mensaje de bandeja vacía (RF-803) y previews claro/oscuro.
  RF: RF-801–RF-804
  Hecho cuando: compila, las previews se ven en claro y oscuro, y no hay hexadecimales ni `fontSize` en el archivo.

- [x] **T31 · `InformeTecnicoViewModel` con su prueba**
  `InformeTecnicoUiState` (cuatro campos, `guardando`, `error`, `disponibilidadBiometrica`, evento `cerrada`),
  `alCambiarDiagnostico`/`alCambiarTrabajo`/`alCambiarPiezas`/`alCambiarObservaciones` y
  `alConfirmarCierre(confirmadoEn)`. `InformeTecnicoViewModelTest` con los casos de `plan.md` sección 7
  (`MainDispatcherRule`, `SesionRepositoryDemo`, `AtencionRepositoryFalso`).
  RF: RF-813–RF-820
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*InformeTecnicoViewModelTest"` pasa.

- [x] **T32 · `InformeTecnicoScreen` según `plan.md` sección 5**
  `InformeTecnicoRoute` (pide `rememberVerificadorBiometrico()` y solo llama a `vm.alConfirmarCierre` en
  `ResultadoBiometrico.Exito`) + `InformeTecnicoScreen` (solo dibuja), con el botón deshabilitado por campos
  vacíos o por `SIN_SEGURIDAD`, y previews claro/oscuro.
  RF: RF-813–RF-817
  Hecho cuando: compila y las previews se ven en claro y oscuro.
  > Hecho el 2026-10-12: SIN resumen de la incidencia arriba (decisión del usuario, `InformeTecnicoUiState`
  > de T31 no expone ningún dato de la incidencia, solo los 4 campos del formulario). `InformeTecnicoScreen`
  > solo tiene los 4 `OutlinedTextField` + botón «Confirmar cierre»; sin bloque de resumen.

- [x] **T33 · Reemplazar las dos pantallas en construcción 🔶**
  En `feature/atencion/presentation/AtencionNavigation.kt`, `Ruta.MisTareas` usa `MisTareasRoute` y
  `Ruta.RegistrarInforme` usa `InformeTecnicoRoute`; `Ruta.SolicitarRepuesto` sigue con `PantallaEnConstruccion`
  (HU-11, fuera de alcance).
  RF: RF-801, RF-813
  Hecho cuando: compila y el archivo ya no importa `PantallaEnConstruccion` para esas dos rutas.
  > Hecho el 2026-10-12: `MisTareas` navega con `alAbrir = { id -> navController.navigate(Ruta.DetalleIncidencia(id)) }`
  > (mismo patrón que `ListaIncidenciasScreen`); `RegistrarInforme` usa `InformeTecnicoRoute(alVolver = ..., alCerrar = ...)`,
  > ambas lambdas hacen `popBackStack()` (volver manual y cierre exitoso terminan en el detalle). `SolicitarRepuesto`
  > y `EstadoRepuestos` sin cambios. 236 pruebas en verde, `assembleDebug` y `testDebugUnitTest` sin errores.

- [x] **T34 · `DetalleIncidenciaViewModel` ampliado con su prueba 🔶**
  `puedeIniciarAtencion` (marca == Asignada, técnico asignado, permiso), `iniciandoAtencion`, `errorAtencion`,
  `alIniciarAtencion()` (deshabilita mientras guarda, RF-811). Primer `DetalleIncidenciaViewModelTest.kt` del
  proyecto, solo con los casos nuevos de esta HU (ver `plan.md`, sección 0).
  RF: RF-806, RF-807, RF-811
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*DetalleIncidenciaViewModelTest"` pasa.
  > Hecho el 2026-10-12: `plan.md` (sección 0) asumía que no existía `DetalleIncidenciaViewModelTest.kt`, pero ya
  > existía en `app/src/test/java/com/softcorp/sigtec/` (paquete raíz, de la HU-04/#15), no en el paquete de la
  > clase real. Los otros tres `ViewModelTest` del proyecto (`ListaIncidenciasViewModelTest`, `LoginViewModelTest`,
  > `RegistroIncidenciaViewModelTest`) siguen ese mismo precedente de paquete raíz, así que los 5 casos nuevos
  > (RF-806, RF-807, RF-811) se agregaron como métodos `@Test` nuevos dentro del archivo ya existente, en vez de
  > crear uno en `feature/incidencias/presentation/` (que habría sido una convención nueva e inconsistente).
  > 241 pruebas en verde (6 casos viejos + 5 nuevos de `DetalleIncidenciaViewModelTest`, 11 en total), `assembleDebug`
  > y `testDebugUnitTest` sin errores.

- [x] **T35 · Botón «Iniciar atención» y sección del informe en el detalle 🔶**
  En `DetalleIncidenciaScreen.kt`: botón en `BarraAcciones` cuando `estado.puedeIniciarAtencion`,
  deshabilitado mientras `estado.iniciandoAtencion`, con `errorAtencion` mostrado si lo hay; nueva sección
  «Informe técnico» en `Contenido(...)` cuando la incidencia está Solucionada y `detalle.informe != null`.
  Dos previews nuevas (claro y oscuro).
  RF: RF-806, RF-827
  Hecho cuando: compila, las previews se ven en ambos temas y no hay doble margen en la barra superior.
  > Hecho el 2026-10-12: `AccionesDetalle.alIniciarAtencion` se agregó como excepción documentada (no navega,
  > `DetalleIncidenciaRoute` la conecta directo a `vm::alIniciarAtencion` con `.copy(...)`, sin tocar
  > `IncidenciasNavigation.kt`). El botón queda en la misma `Row` que «Asignar técnico»/«Solicitar
  > repuesto»/«Cerrar con informe» (los tres solo pueden coexistir en marca Asignada); el ícono es
  > `Icons.Outlined.PlayArrow`. La sección «Informe técnico» va después de `TarjetaDatos` y antes de
  > «Evidencias», con el nombre del técnico asignado (`tecnicoAsignadoNombre ?: SIN_TECNICO`) y
  > `fechaCompleta(informe.confirmadoEn)`. Dos previews nuevas: «Técnico, puede iniciar atención · claro» y
  > «Cerrada con informe · oscuro» (una por tema, según pidió el orquestador). 241 pruebas en verde,
  > `assembleDebug` y `testDebugUnitTest` sin errores; sin hexadecimales ni `fontSize` nuevos.

- [ ] **T36 · Verificación con `adb` en el emulador o celular**
  `./gradlew installDebug`, entrar como un técnico de prueba (`lramirez`, `atorres`, `jsalas` o `rvilca`),
  abrir «Mis tareas» y una incidencia Asignada. Capturas en el scratchpad (nunca en el repo) en claro y
  oscuro; revisar `adb logcat *:W`.
  RF: RF-801–RF-804, RF-806
  Hecho cuando: las pantallas coinciden con `plan.md` sección 5 en ambos temas, sin doble margen en la barra
  superior y sin errores en Logcat.
  > Estado parcial (intento 2026-10-12): `./gradlew installDebug` OK en `emulator-5554`. El login mostró la
  > sesión ya vinculada de un técnico («Hola de nuevo, Luis», con huella disponible), así que se usó
  > `adb -e emu finger touch 1` (mecanismo legítimo del emulador, no una contraseña inventada). `dumpsys
  > fingerprint` confirmó dos autenticaciones exitosas y `dumpsys SurfaceFlinger --list` confirmó que
  > `com.softcorp.sigtec/.MainActivity` quedó activo en primer plano. Pero la pantalla del emulador quedó
  > atascada en negro para toda captura (`screencap`/`exec-out` y por archivo), incluso tras despertar,
  > ciclar `KEYCODE_POWER`, fijar brillo y desactivar animaciones; `dumpsys display` mostró el estado
  > `committedState` pasando a `ON` sin que el framebuffer cambiara — un cuelgue de render del emulador, no
  > un fallo de la app (no hubo ningún `*:W` con `softcorp`/`sigtec`, ni ANR ni `FATAL EXCEPTION`). No se
  > pudieron tomar las capturas en claro/oscuro. Pendiente: reiniciar el AVD (o probar en celular real) y
  > repetir los pasos 4 a 7 del procedimiento.

- [x] **T37 · Verificación y PR E**
  Correr el comando del CI. Hacer los 4 commits de `plan.md` (PR E), con las capturas de T36.
  RF: RF-801–RF-820, RF-827
  Hecho cuando: CI en verde y aprobación de la otra persona del equipo.
  > Hecho el 2026-10-12: `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde (241 pruebas, 0
  > fallos). Sin capturas de T36 (quedó bloqueada por un cuelgue de render del emulador, no de la app; el
  > usuario decidió seguir y revisar visualmente más adelante, en el emulador reiniciado o en su celular,
  > durante las pruebas manuales de T38). Hechos los 4 commits de `plan.md` en orden; no se hizo `git push`
  > ni se abrió PR en GitHub (decisión del usuario para esta sesión). T36 queda sin marcar, con su nota de
  > estado parcial.

## PR F — verificación de punta a punta y cierre (`docs/hu08-cierre`, apilada sobre E)

- [ ] **T38 · Prueba manual — NO automática: iniciar atención y cerrar con informe**
  **Detenerse aquí si no hay celular o emulador.**
  Como el técnico asignado: abrir «Mis tareas», tocar una incidencia Asignada, «Iniciar atención» (pasa a En
  atención y desaparece el botón), «Cerrar con informe» con los cuatro campos, confirmar con huella o PIN/patrón
  (`adb -e emu finger touch 1` en el emulador). Comprobar que sale de la bandeja, que el detalle muestra
  Solucionado y la sección del informe con el nombre y la fecha de `confirmadoEn`. Cancelar la huella una vez
  y comprobar que el formulario sigue completo y la incidencia no cambió. Repetir con un celular sin ningún
  bloqueo de pantalla configurado (o simulando `SIN_SEGURIDAD`) y comprobar que no deja confirmar.
  RF: RF-801–RF-821, RF-827
  Hecho cuando: todo coincide con la spec.

- [ ] **T39 · Prueba manual — NO automática: carga del técnico en la consola al cerrar**
  **Detenerse aquí si no hay celular o emulador.**
  Antes de cerrar, anotar `incidenciasActivas` del técnico en la consola. Cerrar una incidencia (T38) y
  comprobar que bajó 1 en la consola tras sincronizar. Repetir sin conexión (modo avión): cerrar, ver el aviso
  de «1 cambio por enviar», desactivar el modo avión y comprobar que sube y la carga baja igual.
  RF: RF-822, RF-823, RF-824
  Hecho cuando: la carga de la consola coincide con las incidencias pendientes asignadas en ambos casos.

- [ ] **T40 · Prueba manual — NO automática: conflicto completo de RF-624 (HU-06) y RF-825**
  **Detenerse aquí si no hay dos dispositivos y un jefe disponible.**
  Con el técnico anterior sin conexión en un celular: iniciar atención y cerrar con informe (queda pendiente
  de sincronizar). Mientras tanto, el jefe reasigna la misma incidencia a otro técnico desde otro dispositivo
  (con conexión). Al devolver la conexión al primer celular: comprobar que el cierre se rechaza, que el
  celular termina mostrando la versión reasignada con el aviso de «no se pudo enviar», que ni el movimiento
  `CERRADA` ni el informe quedan en la consola, y que el técnico anterior ya no ve «Iniciar atención»,
  «Solicitar repuesto» ni «Cerrar con informe» en el detalle. Esta es la prueba que la HU-06 (T44) dejó
  pendiente para esta HU.
  RF: RF-624 (HU-06), RF-825, RF-826
  Hecho cuando: ningún movimiento ni informe queda huérfano en la consola y el celular muestra la verdad del
  servidor.

- [ ] **T41 · Cierre: spec, `MEMORY.md` y PR F**
  En `spec.md`, cambiar «Estado: borrador» a «Estado: completa», con la fecha y qué se probó (T38 a T40). En
  `MEMORY.md` (después de `git pull origin main`), actualizar solo la línea de la HU-08: «completa (RF-801 a
  RF-829)». Hacer el commit de `plan.md` (PR F) y abrir el PR con capturas y las pruebas manuales T38 a T40.
  RF: todos
  Hecho cuando: CI en verde, aprobación de la otra persona del equipo, y el PR se fusiona al final.
