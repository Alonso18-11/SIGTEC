# Plan 010 — Dictado por voz y linterna (HU-10)

> Basado en `spec.md` (aprobada el 2026-11-07, con D-1 a D-9 respondidas) y `docs/constitution.md`.
> Todo vive en `feature/captura`, que ya tiene la cámara y el permiso de la HU-09. La HU amplía el registro
> (`incidencias`), el detalle (`incidencias`) y el informe (`atencion`), y toca dos archivos compartidos:
> `AndroidManifest.xml` (D-1) y `Rutas.kt` (D-9). Se entrega en PR de un solo tema (sección 1), cada uno con su rama,
> commits chicos, la HU al final del título y la aprobación de la otra persona del equipo antes de fusionarse.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM).

## 0. Hallazgos del código real

- **La ruta ya existe sin argumentos.** `core/navigation/Rutas.kt` tiene `@Serializable data object DictarTexto`, y
  `feature/captura/presentation/CapturaNavigation.kt` la registra con `PantallaEnConstruccion` («Dictar», HU-10).
  Pasa a clase con argumentos (D-9, PR-07).
- **El micrófono del registro ya está dibujado.** `RegistroIncidenciaScreen` tiene el `IconButton` con
  `Icons.Outlined.Mic` («Dictar descripción») y el texto de apoyo «Puedes dictarla con el micrófono y corregirla
  antes de guardar»; `IncidenciasNavigation` lo conecta a `navigate(Ruta.DictarTexto)`. Solo falta recibir el texto.
- **El informe no tiene micrófonos.** `InformeTecnicoScreen` tiene «Diagnóstico *», «Solución aplicada *», «Piezas
  cambiadas» y «Observaciones», con `alCambiar…` en `InformeTecnicoViewModel`. Se agregan tres micrófonos (D-2).
- **El permiso de cámara es casi genérico.** `vistaPermiso(tieneCamara, concedido, yaSePidio, androidLoExplicaria)`
  (`PermisoCamara.kt`) ya es una función pura con su prueba; `ConPermisoCamara` y `ExplicacionPermisoCamara` tienen
  el permiso y los textos de la cámara fijos. D-7 los generaliza sin cambiar lo que se ve con la cámara (PR-02).
- **La cámara no expone su control.** `VistaCamara` hace `bindToLifecycle` y se queda con la `Camera`; para la
  linterna de las pantallas de cámara tiene que publicar si la cámara tiene flash y dejar encenderlo (PR-12, D-8).
- **La barra superior ya acepta acciones.** `BarraSuperior(titulo, alVolver, acciones)` tiene la ranura
  `acciones`; el detalle ya la usa para su menú. La linterna va ahí, antes del menú (prototipo 5).
- **Íconos.** `material-icons-extended` ya está en `app/build.gradle.kts`: `Icons.Outlined.FlashlightOn` /
  `FlashlightOff`, `Icons.Outlined.Mic`, `MicOff` y `Stop` están disponibles.
- **Sin dependencias nuevas.** `SpeechRecognizer` (reconocimiento de voz) y `CameraManager.setTorchMode` (flash sin
  abrir la cámara, API 23) son de Android; CameraX ya está desde la HU-09.
- **Visibilidad de paquetes.** Desde Android 11, `SpeechRecognizer.isRecognitionAvailable` solo ve el servicio de
  voz si el manifest declara `<queries>` con la acción `android.speech.RecognitionService` (PR-03).
- **El ViewModel lee los argumentos del `SavedStateHandle`**, como `CapturaEvidenciaViewModel`
  (`savedStateHandle[ARG_INCIDENCIA_ID]`), para poder probarlo en la JVM sin `toRoute`.

## 1. División en PR, dependencias y orden de fusión

Cada PR compila y pasa `./gradlew assembleDebug testDebugUnitTest lintDebug` por sí mismo. Si depende de un PR
abierto, sale de su rama («Base») y trae con `git merge` los demás que necesite («Trae»); el CI corre también en los
PR apilados (P-13 de la HU-09). Al fusionarse la base, se hace `git pull origin main` y se revisa
`git log main..<rama>`.

| PR | Rama | Título | Base | Trae | RF |
|---|---|---|---|---|---|
| PR-01 | `feat/hu10-texto-dictado` | `feat(captura): unir el texto dictado y los campos que se dictan (HU-10)` | `main` | — | RF-1001, RF-1008, RF-1011 |
| PR-02 | `refactor/hu10-permiso-generico` | `refactor(captura): la regla y la explicación del permiso sirven para cualquier recurso (HU-10)` | `main` | — | RF-1019 a RF-1023 |
| PR-03 | `feat/hu10-permiso-microfono` | `feat(captura): permiso de micrófono y consulta del servicio de voz en el manifest (HU-10)` | `main` | — | RF-1017, RF-1019 |
| PR-04 | `feat/hu10-reconocedor-voz` | `feat(captura): reconocedor de voz de Android detrás de una interfaz (HU-10)` | PR-01 | — | RF-1004 a RF-1007, RF-1013, RF-1015 a RF-1018 |
| PR-05 | `feat/hu10-dictado-viewmodel` | `feat(captura): DictadoViewModel escucha, se detiene y suma el texto (HU-10)` | PR-04 | — | RF-1004 a RF-1012, RF-1014 a RF-1018 |
| PR-06 | `feat/hu10-dictar-pantalla` | `feat(captura): pantalla «Dictar» con el permiso de micrófono (HU-10)` | PR-05 | PR-02 | RF-1003 a RF-1005, RF-1007, RF-1009, RF-1015 a RF-1022 |
| PR-07 | `feat/hu10-dictar-navegacion` | `feat(captura): «Dictar» en la navegación con el campo y la devolución del texto (HU-10)` | PR-06 | PR-03 | RF-1003, RF-1010, RF-1012 |
| PR-08 | `feat/hu10-registro-dictado` | `feat(incidencias): la descripción recibe el texto dictado (HU-10)` | PR-07 | — | RF-1001, RF-1002, RF-1011 |
| PR-09 | `feat/hu10-informe-dictado` | `feat(atencion): dictar diagnóstico, solución y observaciones del informe (HU-10)` | PR-07 | — | RF-1001 a RF-1003, RF-1011 |
| PR-10 | `feat/hu10-linterna` | `feat(captura): linterna con el flash de la cámara trasera (HU-10)` | `main` | — | RF-1025, RF-1028, RF-1029 |
| PR-11 | `feat/hu10-detalle-linterna` | `feat(incidencias): linterna en el detalle para el técnico asignado (HU-10)` | PR-10 | — | RF-1024 a RF-1029 |
| PR-12 | `feat/hu10-camara-linterna` | `feat(captura): linterna en «Escanear etiqueta» y «Evidencia fotográfica» (HU-10)` | PR-10 | — | RF-1030 a RF-1032 |
| PR-13 | `docs/hu10-cierre` | `docs(captura): cierre de la HU-10 con las pruebas manuales (HU-10)` | el último fusionado | todos | RF-1001 a RF-1032 |

**Orden de fusión.** Las tres ramas que salen de `main` (PR-01, PR-02, PR-03) y PR-10 pueden revisarse en paralelo.
El dictado sigue la cadena PR-04 → PR-05 → PR-06 → PR-07 → PR-08 y PR-09; la linterna, PR-10 → PR-11 y PR-12. Las
dos cadenas no se tocan: se pueden repartir entre las dos personas del equipo.

## 2. Archivos y commits por PR

Leyenda: 🆕 nuevo, ✏️ modificado, ⚠️ compartido de `CLAUDE.md`, 🔶 sensible fuera de esa lista.

| PR | Archivos | Commits |
|---|---|---|
| PR-01 | 🆕 `feature/captura/domain/TextoDictado.kt` (`CampoDictado`, `unirTexto`, 3.1); 🆕 (test) `TextoDictadoTest.kt` | 1. `feat(captura): campos que se dictan y unir el texto dictado` · 2. `test(captura): unir texto vacío, con texto y con espacios` |
| PR-02 | ✏️🔶 `PermisoCamara.kt` → 🆕 `PermisoRecurso.kt` (`VistaPermiso` con nombres neutros, 3.2); ✏️🔶 `ExplicacionPermisoCamara.kt` → 🆕 `ExplicacionPermiso.kt` (recibe los textos); ✏️🔶 `ConPermisoCamara.kt` → 🆕 `ConPermiso.kt` + `ConPermisoCamara` como envoltorio con los textos de siempre; ✏️ (test) `PermisoCamaraTest.kt` → `PermisoRecursoTest.kt` | 1. `refactor(captura): regla del permiso con nombres neutros` · 2. `refactor(captura): explicación del permiso con sus textos como parámetro` · 3. `refactor(captura): ConPermiso genérico y ConPermisoCamara sobre él` |
| PR-03 | ✏️⚠️ `AndroidManifest.xml`: `RECORD_AUDIO`, `uses-feature android.hardware.microphone` con `required="false"` y `<queries>` del servicio de voz (D-1) | 1. `feat(captura): permiso de micrófono y consulta del servicio de voz` |
| PR-04 | 🆕 `feature/captura/domain/ReconocedorVoz.kt` (interfaz, `EventoDictado`, `ErrorDictado`, 4.1); 🆕 `feature/captura/data/ReconocedorVozAndroid.kt` (4.2, P-1 a P-3); 🆕 `feature/captura/data/ErroresReconocedor.kt` (`errorDictadoDe(codigo)`, 3.3); ✏️ `feature/captura/di/CapturaModule.kt` (`@Binds`); 🆕 (test) `feature/captura/data/ReconocedorVozFalso.kt`, `ErroresReconocedorTest.kt` | 1. `feat(captura): contrato del reconocedor de voz` · 2. `feat(captura): traducir los errores del reconocedor de Android` · 3. `test(captura): errores del reconocedor` · 4. `feat(captura): reconocedor de voz sobre SpeechRecognizer` · 5. `test(captura): reconocedor de voz falso` |
| PR-05 | 🆕 `feature/captura/presentation/DictadoViewModel.kt` (4.3); 🆕 (test) `DictadoViewModelTest.kt` | 1. `feat(captura): DictadoViewModel escucha y muestra lo reconocido` · 2. `feat(captura): detener, seguir dictando y usar el texto` · 3. `test(captura): DictadoViewModel con el reconocedor falso` |
| PR-06 | 🆕 `feature/captura/presentation/DictarScreen.kt` (Route + Screen, 5.1; vistas previas de cada estado); 🆕 `feature/captura/presentation/MensajesDictado.kt` (`mensajeDictado(error)`); 🆕 (test) `MensajesDictadoTest.kt` | 1. `feat(captura): mensajes de los errores al dictar` · 2. `test(captura): mensajes de los errores al dictar` · 3. `feat(captura): pantalla «Dictar» con el texto transcrito` · 4. `feat(captura): «Dictar» pide el permiso de micrófono` |
| PR-07 | ✏️⚠️ `core/navigation/Rutas.kt` (`DictarTexto(campo, subtitulo)`, P-4); ✏️ `CapturaNavigation.kt` (reemplaza la `PantallaEnConstruccion`, `claveDictado(campo)`, 5.2); ✏️ `IncidenciasNavigation.kt` (la llamada de siempre con los argumentos nuevos) | 1. `feat(captura): ruta «Dictar» con el campo y el subtítulo` · 2. `feat(captura): «Dictar» en la navegación y la clave de cada campo` |
| PR-08 | ✏️ `RegistroIncidenciaViewModel.kt` (`alRecibirDictado`); ✏️ `RegistroIncidenciaScreen.kt` (la Route arma el subtítulo y recibe el texto); ✏️ `IncidenciasNavigation.kt` (5.2); ✏️ (test) `RegistroIncidenciaViewModelTest.kt` | 1. `feat(incidencias): la descripción recibe el texto dictado` · 2. `test(incidencias): dictado en la descripción vacía, con texto y guardando` · 3. `feat(incidencias): el registro abre «Dictar» y recibe el texto` |
| PR-09 | ✏️🔶 `InformeTecnicoViewModel.kt` (`alRecibirDictado(campo, texto)`); ✏️🔶 `InformeTecnicoScreen.kt` (tres micrófonos, Route); ✏️🔶 `AtencionNavigation.kt` (5.2); ✏️ (test) `InformeTecnicoViewModelTest.kt` | 1. `feat(atencion): el informe recibe el texto dictado en cada campo` · 2. `test(atencion): dictado en el informe editable y confirmado` · 3. `feat(atencion): micrófonos en diagnóstico, solución y observaciones` · 4. `feat(atencion): el informe abre «Dictar» y recibe el texto` |
| PR-10 | 🆕 `feature/captura/domain/Linterna.kt` (interfaz, 4.4); 🆕 `feature/captura/data/LinternaAndroid.kt` (P-5); ✏️ `CapturaModule.kt`; 🆕 `feature/captura/presentation/BotonLinterna.kt` (5.3, vistas previas); 🆕 (test) `feature/captura/data/LinternaFalsa.kt` | 1. `feat(captura): contrato de la linterna` · 2. `feat(captura): linterna con CameraManager y su estado real` · 3. `feat(captura): botón de linterna para la barra superior` · 4. `test(captura): linterna falsa` |
| PR-11 | ✏️ `DetalleIncidenciaViewModel.kt` (4.5); ✏️ `DetalleIncidenciaScreen.kt` (botón en `BarraSuperior`, apagar al dejar de ver, vistas previas); ✏️ (test) `DetalleIncidenciaViewModelTest.kt` | 1. `feat(incidencias): el detalle sabe si muestra la linterna y si está encendida` · 2. `test(incidencias): linterna en el detalle por perfil, estado y flash` · 3. `feat(incidencias): botón de linterna en el detalle` · 4. `feat(incidencias): apagar la linterna al salir del detalle` |
| PR-12 | ✏️🔶 `VistaCamara.kt` (`LinternaCamara`, 5.4, P-6); ✏️ `EscanearEtiquetaScreen.kt` y `EvidenciaFotograficaScreen.kt` (botón en la barra, vistas previas) | 1. `feat(captura): la vista de cámara publica su flash` · 2. `feat(captura): linterna en «Escanear etiqueta»` · 3. `feat(captura): linterna en «Evidencia fotográfica»` |
| PR-13 | ✏️ `specs/010-dictado-linterna/spec.md` («completa»), ✏️ `tasks.md`, ✏️ `MEMORY.md` (línea de la HU-10) | 1. `docs(captura): pruebas manuales de la HU-10` · 2. `docs(captura): HU-10 completa` |

## 3. Funciones puras de lógica

Kotlin puro, sin `import android.*` en `domain` (principio 3), probadas en la JVM.

### 3.1 `TextoDictado.kt` (PR-01)
```text
enum CampoDictado { DESCRIPCION, DIAGNOSTICO, SOLUCION, OBSERVACIONES }      // RF-1001, D-2

unirTexto(actual, nuevo):                                                     // RF-1008, RF-1011
    agregado = nuevo.trim()
    si agregado vacío: devolver actual
    si actual está en blanco: devolver agregado
    si actual termina en espacio o salto de línea: devolver actual + agregado
    devolver actual + " " + agregado
```
Sirve para las dos uniones: el texto transcrito que suma cada escucha (RF-1008) y el campo que recibe el dictado
(RF-1011). No toca lo que la persona ya escribió, ni siquiera sus espacios.

### 3.2 `PermisoRecurso.kt` (PR-02, D-7)
```text
enum VistaPermiso { SIN_RECURSO, EXPLICAR, NEGADO, IR_A_AJUSTES, CONCEDIDO }
vistaPermiso(tieneRecurso, concedido, yaSePidio, androidLoExplicaria)       // misma tabla de la HU-09
```
Solo cambian los nombres (`SIN_CAMARA` → `SIN_RECURSO`, `CAMARA` → `CONCEDIDO`, `tieneCamara` → `tieneRecurso`). Los
casos de `PermisoCamaraTest` se conservan uno a uno en `PermisoRecursoTest`.

### 3.3 `errorDictadoDe(codigo)` (PR-04)
```text
ERROR_NO_MATCH, ERROR_SPEECH_TIMEOUT                         -> SIN_ENTENDER   // RF-1015
ERROR_NETWORK, ERROR_NETWORK_TIMEOUT, ERROR_SERVER,
ERROR_SERVER_DISCONNECTED, ERROR_LANGUAGE_UNAVAILABLE        -> SIN_CONEXION   // RF-1016
ERROR_INSUFFICIENT_PERMISSIONS                               -> SIN_PERMISO    // vuelve a la explicación (RF-1021)
cualquier otro (ERROR_AUDIO, ERROR_CLIENT, ERROR_RECOGNIZER_BUSY…) -> MICROFONO // RF-1018
```
Vive en `data` porque usa las constantes de `SpeechRecognizer`; son constantes de compilación, así que la prueba
corre en la JVM sin celular.

## 4. Algoritmos en pseudocódigo

### 4.1 Contrato del reconocedor (PR-04)
```text
interface ReconocedorVoz {
    val disponible: Boolean                          // RF-1017: hay servicio de voz en el celular
    fun escuchar(): Flow<EventoDictado>              // frío; termina con Final o Error; cancelar = soltar el micrófono
    fun detener()                                    // pide terminar: llega el Final con lo reconocido (RF-1007)
}
sealed EventoDictado { Escuchando; Parcial(texto); Final(texto); Error(ErrorDictado) }
enum ErrorDictado { SIN_ENTENDER, SIN_CONEXION, SIN_SERVICIO, SIN_PERMISO, MICROFONO }
```

### 4.2 `ReconocedorVozAndroid` (PR-04)
```text
escuchar() = callbackFlow {
    si !disponible: enviar Error(SIN_SERVICIO); cerrar
    reconocedor = SpeechRecognizer.createSpeechRecognizer(contexto)      // en el hilo principal (P-1)
    reconocedor.setRecognitionListener(
        onReadyForSpeech -> Escuchando
        onPartialResults -> Parcial(primer resultado)
        onResults        -> Final(primer resultado o ""); cerrar
        onError(codigo)  -> Error(errorDictadoDe(codigo)); cerrar
    )
    reconocedor.startListening(intent con LANGUAGE_MODEL_FREE_FORM, EXTRA_LANGUAGE = "es-PE" (D-5),
                               EXTRA_PARTIAL_RESULTS = true, EXTRA_PREFER_OFFLINE = true (D-6))
    awaitClose { reconocedor.cancel(); reconocedor.destroy() }           // RF-1012, RF-1014
}.flowOn(Dispatchers.Main.immediate)
```
`detener()` llama a `stopListening()` del reconocedor en curso. Nunca guarda el audio (RF-1013).

### 4.3 `DictadoViewModel` (PR-05)
```text
DictadoUiState(campo, subtitulo, escuchando = false, texto = "", parcial = "", error: ErrorDictado? = null,
               textoListo: String? = null)            // evento de un solo uso, como registradaId
    textoVisible = unirTexto(texto, parcial)
    puedeUsar = textoVisible.isNotBlank()             // RF-1009

campo y subtitulo salen del SavedStateHandle (ARG_CAMPO, ARG_SUBTITULO)
empezoUnaVez = false

alPermisoConcedido():                                 // RF-1004, RF-1020: solo la primera vez
    si !empezoUnaVez: empezoUnaVez = true; escuchar()
alSeguirDictando() = escuchar()                       // RF-1008
escuchar():
    si escuchando: nada
    error = null
    trabajo = viewModelScope.launch {
        reconocedor.escuchar().collect { evento ->
            Escuchando -> escuchando = true
            Parcial(t) -> parcial = t                                         // RF-1005
            Final(t)   -> texto = unirTexto(texto, t); parcial = ""; escuchando = false   // RF-1007, RF-1008
            Error(e)   -> texto = unirTexto(texto, parcial); parcial = ""; escuchando = false; error = e
        }                                                                      // RF-1015 a RF-1018: el texto se conserva
    }
alDetener() = reconocedor.detener()                   // RF-1007
alDejarDeVer():                                       // RF-1014 (ON_STOP); también al rotar
    trabajo.cancel(); texto = unirTexto(texto, parcial); parcial = ""; escuchando = false
alUsarTexto():                                        // RF-1010
    alDejarDeVer(); textoListo = textoVisible.trim()
alTextoEntregado() = textoListo = null
```
`SIN_SERVICIO` deja `sinServicio = true` en el estado y la pantalla quita el micrófono (RF-1017). `SIN_PERMISO`
(el permiso se quitó mientras escuchaba) lo resuelve la pantalla volviendo a la explicación.

### 4.4 Contrato de la linterna (PR-10)
```text
interface Linterna {
    val tieneFlash: Boolean
    val encendida: StateFlow<Boolean>                // refleja el estado real, también si se apaga desde fuera (RF-1029)
    fun encender(): Boolean                          // false si no se pudo (RF-1028)
    fun apagar()
}
```
`LinternaAndroid`: busca una vez el id de la cámara trasera con `FLASH_INFO_AVAILABLE`; `setTorchMode(id, …)` dentro
de `try/catch` (`CameraAccessException`, `IllegalArgumentException`) con `Log.w`; `registerTorchCallback` actualiza
`encendida`. Es `@Singleton` para que haya un solo dueño del flash.

### 4.5 Linterna en `DetalleIncidenciaViewModel` (PR-11)
```text
estado agrega: linternaVisible = puedeAtender && linterna.tieneFlash      // RF-1024
               linternaEncendida = linterna.encendida
               errorLinterna: Boolean                                     // RF-1028, se limpia al volver a tocar

alAlternarLinterna():
    si encendida: linterna.apagar()
    si no: errorLinterna = !linterna.encender()
alDejarDeVer() = linterna.apagar()                                        // RF-1026 (ON_STOP de la entrada)
init: cuando linternaVisible pasa a false y está encendida -> linterna.apagar()   // RF-1027
onCleared() = linterna.apagar()
```

## 5. Cómo se pinta en Compose

Todas las pantallas usan `Scaffold(contentWindowInsets = WindowInsets(0))` con `BarraSuperior`, solo roles de
`MaterialTheme.colorScheme` y estilos de `MaterialTheme.typography`, `Espaciado.*` y los botones de `Botones.kt`.
Las `Screen` reciben estado y lambdas y tienen vistas previas en claro y oscuro.

### 5.1 «Dictar» (PR-06)
- `DictarRoute`: `hiltViewModel()`, `collectAsStateWithLifecycle()`, `LifecycleEventEffect(ON_STOP) { vm.alDejarDeVer() }`.
  Envuelve el contenido en `ConPermiso(RECORD_AUDIO, tieneRecurso = FEATURE_MICROPHONE && vm.disponible, textos del
  micrófono)`; cuando el contenido se compone (permiso concedido), `LaunchedEffect(Unit) { vm.alPermisoConcedido() }`.
- `DictarScreen`: `BarraSuperior(titulo del campo, alVolver)`, con el subtítulo debajo en `bodySmall` y
  `onSurfaceVariant` (la barra no tiene subtítulo, como en «Evidencia fotográfica»).
- Centro: el micrófono en un círculo `primary` con dos anillos `primaryContainer` que laten mientras `escuchando`
  (animación infinita de escala); detenido, el círculo es un `IconButton` «Seguir dictando». Debajo, «Escuchando…»
  (`primary`), «Toca el micrófono para seguir dictando» o el mensaje de error (`error`).
- Recuadro «Texto transcrito»: `Surface` con borde `outlineVariant` y esquinas de 8 dp, `labelMedium` arriba y el
  `textoVisible` en `bodyLarge`; lo parcial va con `onSurfaceVariant` hasta que se confirma.
- Pie: `BotonSecundario` rojo «Detener» con `Icons.Outlined.Stop` mientras escucha, y `BotonPrincipal` «Usar este
  texto» habilitado con `puedeUsar`. Sin servicio (RF-1017): solo el mensaje y «Volver».

### 5.2 Devolver el texto (PR-07 a PR-09, P-4)
```kotlin
// CapturaNavigation.kt
fun claveDictado(campo: CampoDictado) = "textoDictado_${campo.name}"
composable<Ruta.DictarTexto> {
    DictarRoute(
        alVolver = { navController.popBackStack() },                        // RF-1012
        alUsarTexto = { campo, texto ->                                       // RF-1010
            navController.previousBackStackEntry?.savedStateHandle?.set(claveDictado(campo), texto)
            navController.popBackStack()
        }
    )
}
```
La pantalla que recibe lee la clave como flujo (`getStateFlow<String?>(clave, null)`), entrega el texto a su
`ViewModel` y la borra, igual que el código escaneado de la HU-09 (`CLAVE_CODIGO_ESCANEADO`). El informe lee las tres
claves de sus campos.

### 5.3 `BotonLinterna(encendida, alAlternar)` (PR-10)
`IconButton` de 48 dp con `FlashlightOn` (relleno, `primary`) o `FlashlightOff` (`onSurfaceVariant`) y la descripción
«Apagar linterna» / «Encender linterna»; el estado no depende solo del color (cambia el ícono). Lo usan el detalle y
las pantallas de cámara.

### 5.4 `LinternaCamara` (PR-12, P-6)
`rememberLinternaCamara()` devuelve un objeto con `tieneFlash` y `encendida` (estado de Compose) y `alternar()`.
`VistaCamara(casos, linterna)` le entrega la `Camera` al hacer `bindToLifecycle` (`cameraInfo.hasFlashUnit()`,
`cameraInfo.torchState`) y la quita en el `finally`, así `tieneFlash` es falso mientras se ve la explicación del
permiso o un error (RF-1032) y la linterna se apaga con la cámara (RF-1031). El Route la crea y le pasa a la `Screen`
`linternaVisible`, `linternaEncendida` y `alAlternarLinterna`.

## 6. Decisiones técnicas

Las de producto (D-1 a D-9) están en `spec.md`. Estas son las del cómo:

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| P-1 | `SpeechRecognizer` dentro de un `callbackFlow` en `Dispatchers.Main.immediate`. | El reconocedor exige el hilo principal; el flujo frío ata su vida a quien lo recolecta y `awaitClose` lo destruye siempre. | Manejarlo desde la pantalla: la lógica quedaría en la UI (principio 3). |
| P-2 | Una escucha por toque; cada pausa termina la escucha y el micrófono la reanuda sumando. | Es como funciona `SpeechRecognizer` (no hay escucha continua estable) y deja al usuario el control. | Reiniciar solo al terminar cada frase: escucharía sin que la persona lo sepa y gasta batería. |
| P-3 | Se toma el primer resultado de `RESULTS_RECOGNITION`. | Android lo ordena por confianza; ofrecer alternativas complica la pantalla sin pedido de la HU. | Mostrar las alternativas para elegir. |
| P-4 | `DictarTexto(campo: String, subtitulo: String)`, con `campo` como nombre de `CampoDictado`. | `core/navigation` no depende de `feature/`; el `ViewModel` convierte el texto con `CampoDictado.valueOf`. | Poner `CampoDictado` en `core/domain/model`: otro archivo compartido sin necesidad. |
| P-5 | Linterna del detalle con `CameraManager.setTorchMode`. | Enciende el flash sin abrir la cámara ni pedir su permiso; `registerTorchCallback` da el estado real. | Abrir CameraX en el detalle: pediría el permiso de cámara solo para alumbrar. |
| P-6 | En las pantallas de cámara, el flash se maneja con `CameraControl.enableTorch` de la cámara abierta. | Con la cámara abierta, `setTorchMode` falla; una sola vía por pantalla evita estados cruzados. | Usar `Linterna` también ahí. |
| P-7 | Al dictar, el `ViewModel` no llama a `VerificarPermiso`. | Dictar no cambia datos: el texto solo llega a un campo, y guardarlo pasa por los casos de uso de siempre con su permiso (RF-1013). | Exigir `REGISTRAR_INCIDENCIA` o `ATENDER_INCIDENCIA`: ataría «Dictar» a cada campo. |

## 7. Estrategia de pruebas

JUnit 4, `kotlinx-coroutines-test`, `MainDispatcherRule` y dobles propios, sin librerías nuevas. Los dobles nuevos
viven en `app/src/test` con el paquete de la clase real: `ReconocedorVozFalso` (emite los eventos que la prueba
manda, cuenta `detener()` y si se canceló) y `LinternaFalsa` (`tieneFlash`, falla a pedido, estado en un
`MutableStateFlow`).

| Prueba (PR) | Casos | RF |
|---|---|---|
| `TextoDictadoTest` (PR-01) | Campo vacío; con texto; con espacio o salto al final; dictado vacío o solo espacios; espacios en los extremos del dictado. | RF-1008, RF-1011 |
| `PermisoRecursoTest` (PR-02) | Los mismos casos de `PermisoCamaraTest`, con los nombres nuevos. | RF-1019 a RF-1023 |
| `ErroresReconocedorTest` (PR-04) | Cada código de 3.3 y uno desconocido. | RF-1015 a RF-1018 |
| `DictadoViewModelTest` (PR-05) | Empieza solo la primera vez; parciales visibles; final suma al texto; detener llama al reconocedor; seguir dictando suma con espacio; cada error conserva el texto; dejar de ver cancela y conserva lo parcial; usar entrega el texto recortado y cancela; «Usar» deshabilitado sin texto; sin servicio. | RF-1004 a RF-1018 |
| `MensajesDictadoTest` (PR-06) | Un texto por cada `ErrorDictado`, iguales a la spec. | RF-1015 a RF-1018 |
| `RegistroIncidenciaViewModelTest` (PR-08) | Dictado en descripción vacía; con texto; durante el guardado no cambia nada. | RF-1002, RF-1011 |
| `InformeTecnicoViewModelTest` (PR-09) | Cada campo recibe su texto; con el informe confirmado o guardando no cambia. | RF-1002, RF-1011 |
| `DetalleIncidenciaViewModelTest` (PR-11) | Visible para el técnico asignado y pendiente con flash; oculta para otro técnico, el jefe, cerrada y sin flash; encender y apagar; error al encender; apagar al dejar de ver; apagar si se cierra o se reasigna. | RF-1024 a RF-1029 |

Lo que necesita el hardware (reconocer la voz, encender el flash, la cámara) se prueba a mano en un celular real
(spec, «Criterios de finalización») y queda escrito en cada PR.

## 8. Cobertura de RF

| RF | Dónde vive | Prueba |
|---|---|---|
| RF-1001, RF-1002 | `RegistroIncidenciaScreen`, `InformeTecnicoScreen` | Vistas previas + `RegistroIncidenciaViewModelTest`, `InformeTecnicoViewModelTest` |
| RF-1003 | `DictarScreen`, `Rutas.DictarTexto`, Routes del registro y del informe | Vistas previas + manual |
| RF-1004 a RF-1009 | `DictadoViewModel`, `ReconocedorVozAndroid`, `DictarScreen` | `DictadoViewModelTest` + manual |
| RF-1010, RF-1012 | `DictadoViewModel`, `CapturaNavigation` | `DictadoViewModelTest` + manual |
| RF-1011 | `unirTexto`, VMs del registro y del informe | `TextoDictadoTest` y pruebas de esos VMs |
| RF-1013 | `ReconocedorVozAndroid` (no graba) | Revisión de código |
| RF-1014 | `DictarRoute` (`ON_STOP`), `DictadoViewModel.alDejarDeVer` | `DictadoViewModelTest` + manual |
| RF-1015 a RF-1018 | `errorDictadoDe`, `DictadoViewModel`, `mensajeDictado` | `ErroresReconocedorTest`, `DictadoViewModelTest`, `MensajesDictadoTest` |
| RF-1019 a RF-1023 | `vistaPermiso`, `ConPermiso`, `ExplicacionPermiso` | `PermisoRecursoTest` + manual |
| RF-1024 a RF-1029 | `DetalleIncidenciaViewModel`, `LinternaAndroid`, `BotonLinterna` | `DetalleIncidenciaViewModelTest` + manual |
| RF-1030 a RF-1032 | `LinternaCamara`, `VistaCamara`, pantallas de cámara | Vistas previas + manual |

## 9. Respeto a la constitución

1. **Stack cerrado:** sin dependencias nuevas; reconocimiento de voz y flash son de Android, CameraX ya estaba.
2. **Spec antes que código:** la spec se aprobó el 2026-11-07, antes del primer PR de código.
3. **Lógica fuera de la pantalla:** `unirTexto` y `CampoDictado` en `domain`; los contratos `ReconocedorVoz` y
   `Linterna` en `domain`, y sus implementaciones con Android en `data`. Las `Screen` solo dibujan.
4. **Toda regla tiene prueba:** sección 7.
5. **Datos del usuario protegidos:** el audio no se guarda (RF-1013); nada cambia sin que la persona guarde el
   formulario; no se toca la confirmación biométrica de cierres ni de repuestos.
6. **Español en el negocio:** `ReconocedorVoz`, `Linterna`, `DictadoViewModel`, `unirTexto`; solo los sufijos de
   Android quedan en inglés.

## 10. Resumen: archivos compartidos y Room por PR

| PR | Compartidos ⚠️ (lista de `CLAUDE.md`) | Sensibles 🔶 | Room |
|---|---|---|---|
| PR-02 | — | Permiso de cámara de la HU-09 (`PermisoCamara`, `ExplicacionPermisoCamara`, `ConPermisoCamara`), sin cambiar lo que hace (D-7) | — |
| PR-03 | `AndroidManifest.xml` (D-1) | — | — |
| PR-07 | `core/navigation/Rutas.kt` (D-9) | — | — |
| PR-09 | — | `feature/atencion/presentation/*` de la HU-08 | — |
| PR-12 | — | `VistaCamara.kt` de la HU-09 | — |
| PR-13 | `MEMORY.md` (solo la línea de la HU-10) | — | — |

Ningún PR toca `libs.versions.toml`, `app/build.gradle.kts`, `SigtecDatabase.kt`, las entidades, `SigtecRaiz.kt`,
`Resultado.kt`, `MensajesError.kt`, `core/theme/*`, `core/di/*`, `feature/acceso/*` ni `firebase/firestore.rules`.
No hay migración de Room.
