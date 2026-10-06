# CLAUDE.md — SIGTEC

App Android nativa para el área de Soporte y Sistemas de Soft Corporation. Permite registrar, asignar, atender y cerrar incidencias técnicas de equipos de cómputo. La usan tres perfiles (jefe, técnico y personal de sistemas), sobre todo el técnico, que trabaja con el celular fuera de su escritorio. Es el proyecto del curso Desarrollo de Sistemas Móviles (UNMSM), hecho por un equipo de 2.

La fuente de verdad es el código en `main`; este archivo resume las decisiones que no se deducen leyéndolo. Si tu conocimiento general (tutoriales, versiones habituales) contradice este archivo, sigue el archivo y explica la diferencia. Para ver qué HU ya están hechas, revisa `git log` (los commits llevan la HU al final).

## Stack y estructura

- **AGP 9.3.3 con Kotlin integrado (2.2.10).** No se aplica el plugin `org.jetbrains.kotlin.android`. No se usa kapt: Hilt y Room usan KSP 2.3.10.
- JDK 25 para compilar, bytecode Java 11. minSdk 26 (Android 8.0), targetSdk y compileSdk 37. `java.time` se usa sin librerías extra; las APIs más nuevas van protegidas por versión.
- Interfaz: Jetpack Compose (BOM 2026.02.01), Material 3 sin color dinámico y Navigation Compose 2.9 con rutas tipadas (`@Serializable`).
- Datos e infraestructura: Hilt 2.59.2, Room 2.8.4 (SQLite), DataStore, WorkManager 2.10 con `HiltWorkerFactory`.
- Firebase BoM 34.19.0: Auth (correo y contraseña), Firestore (southamerica-east1) y AI Logic (`firebase-ai`, Gemini Developer API en nivel gratuito). No se usa Analytics.
- Otras librerías: androidx.biometric 1.1.0, CameraX 1.4.2 con ML Kit Text Recognition, Vico 2.1.3, y Retrofit 3 con kotlinx.serialization.
- Todas las versiones están en `gradle/libs.versions.toml`. No las cambies (ver «Límites»).

Paquete raíz `com.softcorp.sigtec`. Los paquetes técnicos van en inglés y los de negocio en español. La línea `package` debe coincidir con la carpeta.

- `SigtecApp.kt`: aplicación Hilt y fábrica de WorkManager. `MainActivity.kt`: `AppCompatActivity` con `@AndroidEntryPoint`, necesaria para `BiometricPrompt`.
- `core/domain/`: `Resultado.kt` (con `Resultado<T>` y `ErrorDominio`), `BloqueoApp`, `model/` (Kotlin puro), `repository/` (contratos de sesión y preferencias) y `usecase/` (sesión y huella).
- `core/database/`: `SigtecDatabase` (10 tablas, 9 DAO), `Convertidores`, `entity/` (con `toDomain()`/`toEntity()`) y `dao/`.
- `core/data/`: `SesionRepositoryFirebase` y `PreferenciasSeguridadDataStore`.
- `core/sync/`: el **único** lugar que habla con Firestore. Contiene `SubirCambiosWorker`, `SincronizacionTiempoReal`, `ReglasSincronizacion.kt` (la regla de conflictos), `MapeoFirestore.kt` y `MonitorConexion`.
- `core/biometric/Biometria.kt`: `rememberVerificadorBiometrico()`, `ResultadoBiometrico` y `DisponibilidadBiometrica`.
- `core/navigation/`: `Rutas.kt` (las 18 rutas), `SigtecRaiz.kt`, `RaizViewModel.kt`, `DestinoPrincipal.kt` (menú por permisos) y `DatosDemo.kt` (temporal).
- `core/theme/`: paleta, `ColoresEstado.kt` (`SigtecTheme.estados.*`), tipografía, `Dimens.kt` (`Espaciado`, `AreaTactilMinima`) y `TemaPreview`.
- `core/ui/`: `MensajesError.kt` y `components/` (`EtiquetaEstado`, `EtiquetaIncidencia`, `AvisoConexion` y `PantallaEnConstruccion`, esta última temporal).
- `core/di/`: módulos de Dispatchers, Firebase, Database, Sesion, DataStore y Sincronizacion.
- `feature/<modulo>/{domain,data,di,presentation}`: acceso, inicio, incidencias, asignacion, atencion, captura, diccionario e indicadores. Las pantallas sin HU implementada siguen con `PantallaEnConstruccion`.
- `firebase/firestore.rules`: reglas del servidor. Se cambian aquí, se revisan en el PR y después se publican a mano en la consola.
- `app/src/test/`: pruebas JVM y dobles de prueba (`SesionRepositoryDemo`, `IncidenciaRepositoryFalso`, `MainDispatcherRule`). `app/src/androidTest/`: pruebas de DAO con Room en memoria.

## Comandos

```bash
./gradlew assembleDebug                 # compilar
./gradlew testDebugUnitTest             # pruebas unitarias (JVM)
./gradlew lintDebug                     # lint de Android
./gradlew connectedDebugAndroidTest     # instrumentadas: requieren celular o emulador (el CI no las corre)

# Lo mismo que el CI (check «compilar-y-probar»); debe pasar antes de abrir el PR
./gradlew assembleDebug testDebugUnitTest lintDebug

# Una sola clase de prueba
./gradlew testDebugUnitTest --tests "com.softcorp.sigtec.RegistrarIncidenciaTest"
```

`app/google-services.json` no está en el repo: se comparte por privado, y en el CI llega desde el secreto `GOOGLE_SERVICES_JSON`. Si falta, el build falla con «File google-services.json is missing». En ese caso avisa y no lo generes.

## Reglas

- Lee `docs/constitution.md` y la spec activa (`specs/NNN-*/`) antes de tocar código.

## Convenciones

- **Idioma:** el dominio, las clases, funciones, variables, comentarios y textos de UI van en español (`RegistrarIncidencia`, `alCambiarCodigo`). Los sufijos que impone Android quedan en inglés (`ViewModel`, `Screen`, `Dao`, `Entity`, `Worker`). Los textos de UI se escriben en Kotlin, no en `strings.xml`.
- **Comentarios:** deben ser cortos, explicar el *porqué* y citar la HU o el RF cuando de ahí sale la regla (`// HU-03`, `RF-23`).
- **Casos de uso:** una clase por acción, con `@Inject constructor` y `operator fun invoke`, que devuelve `Resultado<T>`. El primer paso de toda acción protegida es `VerificarPermiso(Permiso.X)`, que además devuelve el usuario. Si `repo` devuelve un error, se propaga; no lo ignores. Referencia: `feature/incidencias/domain/RegistrarIncidencia.kt`.
- **Repositorios:** la interfaz va en `feature/x/domain`, la `*RepositoryImpl` en `feature/x/data` y el `@Binds` en `feature/x/di`. Reciben `@IoDispatcher` y nunca escriben `Dispatchers.IO`. El `catch` relanza `CancellationException`, registra con `Log.w(TAG, "...", e)` y devuelve `ErrorDominio.Desconocido`. Referencia: `IncidenciaRepositoryImpl.kt`.
- **Room:** las lecturas devuelven `Flow` y las escrituras son `suspend` con `@Upsert`. No hay llaves foráneas, porque la sincronización puede traer hijos antes que su padre. Los enums se guardan como texto. Toda entidad tiene `pendienteSincronizar` y `actualizadoEn`.
- **ViewModel:** se anota con `@HiltViewModel` y tiene un `data class XUiState` con valores por defecto, expuesto como `StateFlow` (`stateIn(viewModelScope, WhileSubscribed(5_000), …)` cuando combina flujos). Los eventos de un solo uso son un campo nullable que la pantalla consume y limpia, como `registradaId`. Los manejadores de eventos llevan el prefijo `al…`. Referencia: `RegistroIncidenciaViewModel.kt`.
- **Pantallas:** se usa el par `XRoute` + `XScreen`. `XRoute` usa `hiltViewModel()` y `collectAsStateWithLifecycle()`. `XScreen` solo dibuja, recibe estado y lambdas, y tiene `@Preview` en modo claro y oscuro dentro de `SigtecTheme`. Referencias: `LoginScreen.kt` y `RegistroIncidenciaScreen.kt`.
- **Barra superior:** las pantallas con `TopAppBar` usan `Scaffold(contentWindowInsets = WindowInsets(0))` y `TopAppBar(windowInsets = WindowInsets(0))`, porque la raíz ya aplica el margen de la barra de estado. Ejemplo: `AjustesSeguridadScreen.kt`.
- **Diseño:** solo se usan roles de `MaterialTheme.colorScheme` y estilos de `MaterialTheme.typography`. Nunca se escriben hexadecimales ni `fontSize` fuera de `core/theme/`. Los estados de incidencia usan `SigtecTheme.estados.*` y siempre van con texto o ícono; «Asignada» usa `surfaceVariant`. El espaciado se toma de `Espaciado.*` (4/8/16/24/32 dp). Las tarjetas de incidencia usan `EtiquetaIncidencia(marca, critica, diasPendiente)` en lugar de etiquetas propias.
- **Navegación:** las rutas se definen en `Rutas.kt` y se registran en el `XNavigation.kt` del módulo (`NavGraphBuilder.xGraph(navController)`); `SigtecRaiz` solo los llama. Las pantallas reciben lambdas, nunca el `NavController`. Los argumentos se leen con `entrada.toRoute<…>()`. Las pantallas de escaneo y dictado devuelven su resultado con `previousBackStackEntry?.savedStateHandle`.
- **Errores:** para un error nuevo, agrega el caso a `ErrorDominio` y su texto en `MensajesError.kt`; el `when` exhaustivo obliga a hacerlo. En pantalla se muestra siempre con `error.mensaje()`.
- **Pruebas:** los dobles de prueba viven en `app/src/test` con el mismo paquete que la clase real. Si una librería se usa en `test` y en `androidTest`, se declara dos veces.
- **Ramas:** se nombran `tipo/descripcion-corta`, con `tipo` ∈ feat, fix, chore, test, docs, refactor. Ejemplo: `feat/asignacion-tecnico`.
- **Commits y títulos de PR:** Conventional Commits en español, con la HU al final. Ámbitos: `core, acceso, inicio, incidencias, asignacion, atencion, captura, diccionario, indicadores, ci, firebase`. Ejemplo: `feat(incidencias): registrar con fecha automática (HU-03)`.

## Reglas de dominio / trampas conocidas

- **Estado vs. marca.** El caso define solo dos estados, Pendiente y Solucionado. Lo que se guarda es `MarcaSeguimiento` (`SIN_ASIGNAR`, `ASIGNADA`, `EN_ATENCION`, `ESPERANDO_REPUESTO`, `CERRADA`), y `EstadoIncidencia` se **deriva** de ella. No persistas el estado ni inventes estados nuevos.
- **Numeración.** `id` es un UUID generado en el celular. `numero` (el `INC-0124` visible, vía `codigoVisible`) es `null` hasta que `core/sync` lo asigna con el contador `contadores/incidencias`. Mientras tanto, la UI muestra «Por sincronizar».
- **Room es la fuente de verdad.** Las pantallas leen siempre de Room. Los repositorios de `feature/` escriben **solo en Room**, con `pendienteSincronizar = true`; subir y bajar de Firestore es trabajo exclusivo de `core/sync/`.
- **Conflictos.** Al bajar de Firestore no se pisa un cambio local sin subir, salvo que el remoto sea más reciente (`debeReemplazarLocal`). Los movimientos solo se agregan; nunca se editan.
- **Transacciones.** Todo cambio de marca guarda su `Movimiento` (REGISTRADA, ASIGNADA, ATENCION_INICIADA, REPUESTO_SOLICITADO, CERRADA…) en la misma transacción `db.withTransaction`.
- **Permisos.** Se consultan con `perfil.puede(Permiso.X)`; nunca con `if (perfil == JEFE)`. Se validan en el dominio, no solo ocultando opciones del menú. `Perfil.kt` y `firestore.rules` deben coincidir. El jefe no tiene acceso al diccionario, y el personal de sistemas lo consulta pero no registra fallas.
- **Huella = dueño del celular, no identidad.** (Esto reemplaza lo que dice la especificación original del producto.) Android no sabe de quién es el dedo. La huella solo desbloquea una sesión de Firebase ya iniciada, y pertenece a una sola persona por celular (`PreferenciasHuella.propietarioId`). Si inicia sesión otra persona, se borran los datos locales de la anterior. «Cerrar sesión» desvincula la huella; «Bloquear» la conserva. No toques este modelo (`feature/acceso`, casos de uso de sesión) sin coordinar.
- **Navegación por acceso.** La navegación sigue a `EstadoAcceso` (`SinSesion`, `Bloqueada`, `Activa`). Ninguna pantalla navega por iniciar o cerrar sesión.
- **Confirmación biométrica (RF-23).** Cerrar una incidencia con informe y enviar una solicitud de repuesto se confirman con `rememberVerificadorBiometrico()`, que acepta huella o PIN/patrón. El `instante` de `ResultadoBiometrico.Exito` se guarda como `confirmadoEn`. Ese campo es obligatorio en `InformeTecnico` y `SolicitudRepuesto`, y Firestore también lo exige.
- **Límite de atención (RF-06).** No se asigna una incidencia a un técnico con `incidenciasActivas >= limiteAtencion`; el error es `LimiteAtencionAlcanzado`. Firestore lo valida también. No relajes la regla del servidor para que pase la app.
- **Datos de prueba.** Firestore tiene 6 usuarios de prueba (`cmendoza` jefe; `lramirez`, `atorres`, `jsalas` y `rvilca` técnicos; `pcardenas` personal de sistemas, todos `@softcorp.pe`) y 4 equipos (`PC-CONT-014`, `PC-LOG-009`, `PC-SIS-003`, `IMP-RRHH-002`). Los enteros en Firestore son `int64`.
- **IA sugiere, nunca decide.** Toda salida de Gemini se marca como generada y requiere aceptarla, corregirla o descartarla; la decisión se guarda en `SugerenciaIA.aceptada`. Si el servicio falla o se agota la cuota, la pantalla funciona igual sin la sugerencia. Usa un modelo estable, porque los modelos en versión preliminar pueden exigir facturación.
- **Costo cero (plan Spark).** No se usan Cloud Functions, FCM con servidor, Cloud Storage ni Analytics. Las notificaciones son locales. Las fotos se guardan en almacenamiento interno, solo en el celular que las tomó.
- **Claves.** La clave de Gemini se usa solo vía Firebase AI Logic, nunca en el código ni con Retrofit. Retrofit es solo para las API de Logística y del directorio, hoy contra un servidor de prueba con la misma estructura.
- **WorkManager.** El inicializador automático se quitó del manifest a propósito; no lo restaures. Los workers usan `@HiltWorker`. El intervalo periódico mínimo es de 15 minutos.
- **Migraciones de Room.** La base usa `fallbackToDestructiveMigration(dropAllTables = true)`, solo para desarrollo. Quien cambia una entidad sube `version` en `SigtecDatabase` y lo dice en el PR. Ten presente que eso borra lo que no se ha sincronizado.
- **Pendiente conocido.** Cerrar sesión ejecuta `clearAllTables()` y pierde lo registrado sin señal. Antes de dar por terminada la sincronización, debe sincronizar o avisar «tienes N registros sin enviar».
- **Temporal.** `DatosDemo.kt` y cada `PantallaEnConstruccion` se borran cuando su HU real existe.

## Forma de trabajar

- Una HU por rama y por PR. Abre el PR como borrador y márcalo «Ready for review» al terminar. Llena `.github/pull_request_template.md`, incluida la sección «Archivos compartidos que toca».
- `main` está protegida: requiere 1 aprobación, el CI en verde, la rama al día y conversaciones resueltas, y solo admite squash. Si `main` avanzó, haz `git pull origin main` en tu rama. Si el PR anterior sigue en revisión, crea la siguiente rama desde la tuya (ramas apiladas).
- Planifica y espera el visto bueno antes de tocar código si el cambio afecta más de un módulo, una entidad de Room, `core/sync`, `firestore.rules`, el modelo de sesión o huella, o algún archivo compartido.
- Construye de adentro hacia afuera: dominio con sus pruebas, luego repositorio y `@Binds`, después ViewModel y Route + Screen según el prototipo, y al final el reemplazo de `PantallaEnConstruccion` en el `Navigation.kt` del módulo.
- Haz cambios pequeños y centrados en la HU. No refactorices ni actualices nada «de paso».
- Antes del código, indica qué HU atiendes, qué archivos creas o modificas y cuáles son compartidos. Entrega archivos completos con `package` e imports.
- Al terminar, explica qué criterios de aceptación cubre el cambio, cómo probarlo en el celular y qué quedó pendiente.
- Si algo marcado como PENDIENTE, TEMPORAL o «por confirmar» afecta la tarea, pregunta en lugar de suponer.

## Memoria

- Al empezar, lee `MEMORY.md` para conocer el estado del proyecto, los pendientes y los errores ya vistos.
- Al terminar una tarea, actualízalo: estado actual, decisiones nuevas (con su porqué) y errores a evitar.
- Mantenlo breve (máximo ~50 líneas): resume o elimina lo que ya no aporta. Lo que ya se ve en `git log` no se repite.
- Si algo se convierte en una regla permanente, propón moverlo a este `CLAUDE.md` en lugar de dejarlo en la memoria.
- Nunca guardes datos sensibles: contraseñas de los usuarios de prueba, claves, tokens ni el contenido de `google-services.json`.
- Edita solo las líneas que cambian, sin reordenar el archivo, para evitar conflictos de merge entre ramas.

## Límites

- ✅ **Siempre:**
  - Respetar las tres capas y la frontera Room ↔ `core/sync`.
  - Usar `Resultado`/`ErrorDominio`, `VerificarPermiso` y `error.mensaje()`.
  - Escribir pruebas para cada caso de uso y ViewModel nuevo.
  - Correr el comando del CI antes de dar algo por terminado.
  - Actualizar `MEMORY.md` al terminar cada tarea.
- ⚠️ **Pregunta antes:**
  - Agregar o actualizar dependencias o versiones; cada actualización va en su propio PR, aunque Android Studio la subraye.
  - Tocar archivos compartidos: `libs.versions.toml`, `app/build.gradle.kts`, `SigtecDatabase.kt` y las entidades, `Rutas.kt`, `SigtecRaiz.kt`, `Resultado.kt`, `core/domain/model/*`, `MensajesError.kt`, `core/theme/*`, `core/di/*`, `AndroidManifest.xml`, `firestore.rules`, `feature/acceso/*` y los casos de uso de sesión.
  - Crear archivos fuera del módulo de la HU.
  - Agregar permisos al manifest.
  - Cambiar la regla de conflictos.
- 🚫 **Nunca:**
  - Usar `kapt` o el plugin `kotlin-android`.
  - Hacer push o `--force` a `main`, desactivar el ruleset o dejar una bypass permanente.
  - Commitear `google-services.json`, `*.jks`, `keystore.properties` o `local.properties`.
  - Escribir en Firestore desde `feature/`.
  - Persistir `EstadoIncidencia`.
  - Quitar la confirmación biométrica de cierres y repuestos.
  - Guardar contraseñas o datos de huella.
  - Poner claves en el código.
  - Dejar que la IA cambie datos sin aceptación humana.
  - Reactivar el color dinámico.
  - Usar `getInstance()` o construir dependencias a mano.

## Verificación

1. `./gradlew assembleDebug testDebugUnitTest lintDebug` pasa sin errores y sin warnings de lint nuevos.
2. Cada regla nueva tiene prueba unitaria con dobles de prueba (`SesionRepositoryDemo`, repositorios falsos) y `MainDispatcherRule` para los ViewModels. Cubre como mínimo: sin sesión o sin permiso, datos inválidos y el camino feliz.
3. Si cambió un DAO o una entidad, la prueba instrumentada correspondiente (como `IncidenciaDaoTest`) pasa en un celular, y `version` subió.
4. Las pantallas nuevas tienen `@Preview` en modo claro y oscuro, no usan colores ni tamaños fijos, y su barra superior no tiene doble margen.
5. Los criterios de aceptación de la HU se verificaron en un celular real. Para los flujos de huella, sin conexión o con sincronización, describe en el PR la prueba manual: huella simulada en los controles extendidos del emulador, modo avión, y dos cuentas en el mismo celular.
6. Si hay un emulador o celular conectado (`adb devices`), después de cada cambio de UI verifícalo con `adb`: `./gradlew installDebug`, abre la app (`adb shell monkey -p com.softcorp.sigtec 1`), navega hasta la pantalla (`adb shell input tap x y`), toma una captura (`adb exec-out screencap -p > captura.png`, en el scratchpad, nunca en el repo) y revísala en claro y oscuro (`adb shell cmd uimode night yes|no`). Si algo falla, revisa `adb logcat *:W`. Para sin conexión: `adb shell cmd connectivity airplane-mode enable|disable`. Para huella en el emulador: `adb -e emu finger touch 1`. Esto no reemplaza la prueba en un celular real del punto 5. Si `adb` no está en el PATH, está en `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.
7. Ante un error conocido, aplica primero su solución:
   - «Connection reset» o «Sin conexión» al iniciar sesión: es la red, no el código (DNS privado, VPN, restricción de datos de la app).
   - «Ocurrió un error inesperado»: busca en Logcat el `TAG` del repositorio; el `catch` registra la excepción completa.
   - `File google-services.json is missing`: falta el archivo en `app/`.
   - «You need to use a Theme.AppCompat theme»: el `parent` de `themes.xml` debe ser `Theme.AppCompat.DayNight.NoActionBar`.
   - «Hilt Activity must be attached to an @HiltAndroidApp»: falta `android:name=".SigtecApp"` en el manifest.
   - `Unresolved reference` a un componente existente: la línea `package` no coincide con la carpeta.
   - Push rechazado con `GH013`: es el ruleset; a `main` solo se llega por PR.
   - Colores distintos en otro celular: alguien reactivó el color dinámico en `SigtecTheme`.
   - Barra superior con doble margen: falta `WindowInsets(0)` en `Scaffold` y `TopAppBar`.
   - `INSTALL_FAILED_USER_RESTRICTED` (Xiaomi/Redmi/POCO): activar «Instalar vía USB» en Opciones de desarrollador.
