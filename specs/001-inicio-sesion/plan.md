# Plan 001 — Inicio de sesión y permisos por perfil (HU-01)

> Basado en `spec.md` y `docs/constitution.md`. RF-1 a RF-12 ya existen en `main`; este plan dice dónde vive cada uno y qué falta para que la HU cumpla la constitución. El trabajo nuevo es RF-13 y RF-14, y la corrección de lo que encontró la revisión QA (hallazgos citados como «QA-n»).

## 0. Adaptaciones de la plantilla
- **«Algoritmo del mapa»:** esta HU no tiene un mapa visual. Se interpreta como las dos tablas de correspondencia que decide la HU: perfil → destinos del menú, y fallo técnico → mensaje (sección 3).
- **«`node --test`»:** SIGTEC es Android y Kotlin. Las pruebas usan JUnit 4 y coroutines-test, ya declaradas en `app/build.gradle.kts`, y se corren con `./gradlew testDebugUnitTest` (principio 4: sin instalar dependencias).
- **«`hoy` como parámetro»:** ninguna regla de esta HU depende de la fecha. Ninguna función pura recibe `hoy`, porque sería un parámetro sin uso. Si una regla futura lo necesita (por ejemplo, un bloqueo propio por intentos), recibirá `ahora: Instant` y nunca llamará a `Instant.now()` por dentro.

## 1. Archivos

Leyenda: 🆕 se crea · ✏️ se modifica · ✅ ya existe y no cambia · ⚠️ archivo compartido (requiere visto bueno según `CLAUDE.md`).

### Dominio (Kotlin puro, ningún `import android.*`)
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕⚠️ `core/domain/ReglasSesion.kt` | Funciones puras de la sección 2. Sigue el patrón de `core/sync/ReglasSincronizacion.kt`. | RF-3, RF-7, RF-11, RF-13, RF-14 |
| ✏️⚠️ `core/domain/usecase/IniciarSesion.kt` | Usa `normalizarCorreo` en lugar de normalizar en línea. | RF-1, RF-3 |
| 🆕⚠️ `core/domain/usecase/EvaluarCierreSesion.kt` | Lee el primer valor de `ObservarEstadoDatos` y devuelve `evaluarBorrado(pendientes)`. Decide; no cierra nada. | RF-13, RF-14 |
| 🆕⚠️ `core/domain/usecase/EvaluarIngresoDeOtraPersona.kt` | Si hay un dueño bloqueado y el correo ingresado es de otra persona, devuelve `evaluarBorrado(pendientes)`; si no, `SinRiesgo`. | RF-11, RF-13 (QA-15) |
| ✅⚠️ `core/domain/usecase/CerrarSesion.kt` | Cierra de verdad. Solo se llama cuando ya no hace falta confirmar o el usuario confirmó. | RF-10 |
| ✅ `core/domain/usecase/VerificarPermiso.kt` | Primer paso de toda acción protegida. | RF-9 |
| ✅⚠️ `core/domain/model/Perfil.kt` | Perfil → permisos. Debe coincidir con `firestore.rules`. | RF-8, RF-9 |

### Datos
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️⚠️ `core/data/SesionRepositoryFirebase.kt` | (a) Usa `perfilDesdeTexto` y `debeBorrarDatosAlEntrar`. (b) Si algo falla después de autenticarse, hace `signOut()` antes de devolver el error. (c) Traduce `FirebaseFirestoreException` con código `UNAVAILABLE` a `SinConexion`. (d) Si el perfil deja de ser válido con la sesión abierta, ejecuta el mismo cierre que `cerrarSesion()`. | RF-5, RF-7, RF-11, nuevo RF-15 (QA-16, QA-17) |

### Presentación
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️ `feature/acceso/presentation/AjustesSeguridadViewModel.kt` | `alPulsarCerrarSesion()` llama a `EvaluarCierreSesion`: con `SinRiesgo` cierra, y con `Confirmar(n)` guarda `confirmarSalida = n` en el `UiState`. Agrega `alConfirmarSalida()` y `alCancelarSalida()`. | RF-13, RF-14 |
| ✏️ `feature/acceso/presentation/AjustesSeguridadScreen.kt` | Diálogo de confirmación (sección 4) y `@Preview` claro y oscuro del diálogo. | RF-13 |
| ✏️ `feature/acceso/presentation/LoginViewModel.kt` | Antes de `iniciarSesion`, consulta `EvaluarIngresoDeOtraPersona`. Con `Confirmar(n)` guarda `confirmarIngreso = n` y espera a que el usuario decida. | RF-11, RF-13 (QA-15) |
| ✏️ `feature/acceso/presentation/LoginScreen.kt` | Diálogo de confirmación del ingreso de otra persona y su `@Preview`. | RF-11 |
| 🆕 `feature/acceso/presentation/TextosSalida.kt` | `textoCambiosSinEnviar(n)` (sección 2.6). Es texto de UI, por eso va en presentación. | RF-13 |
| ✅⚠️ `core/navigation/DestinoPrincipal.kt` | Menú según permisos. | RF-1, RF-8 |
| ✅⚠️ `core/ui/MensajesError.kt` | Error → texto. No cambia: no hay errores nuevos. | RF-4 a RF-7 |

### Pruebas (`app/src/test`)
| Archivo | RF |
|---|---|
| 🆕 `ReglasSesionTest.kt` | RF-3, RF-7, RF-11, RF-13, RF-14 |
| 🆕 `DestinoPrincipalTest.kt` | RF-8 (QA-18) |
| 🆕 `EvaluarCierreSesionTest.kt` | RF-13, RF-14 |
| 🆕 `EvaluarIngresoDeOtraPersonaTest.kt` | RF-11, RF-13 |
| 🆕 `TextosSalidaTest.kt` | RF-13 (QA-10) |
| ✏️ `LoginViewModelTest.kt` | Agrega el diálogo de otra persona. |
| 🆕 `core/sync/EstadoDatosRepositoryFalso.kt` (doble) | Pendientes controlables en las pruebas. |
| ✅ `SesionYPermisosTest.kt`, `EstadoAccesoTest.kt` | RF-4, RF-9, RF-12 |

### Documentación
| Archivo | Cambio |
|---|---|
| ✏️ `spec.md` | Aplicar la sección 6. |
| ✏️ `MEMORY.md` | Estado de la HU-01 y el pendiente de fotos (D-7). |

## 2. Funciones puras de lógica (`core/domain/ReglasSesion.kt`)
Ninguna recibe `hoy`, porque ninguna depende del tiempo (ver sección 0).

| # | Firma | Regla | RF |
|---|---|---|---|
| 2.1 | `normalizarCorreo(correo: String): String` | `trim()` y luego `lowercase()`. Solo quita espacios de los extremos (QA-3). | RF-3 |
| 2.2 | `perfilDesdeTexto(texto: String?): Perfil?` | `null` si el texto es nulo o no coincide exactamente con un valor de `Perfil`. | RF-7 |
| 2.3 | `debeBorrarDatosAlEntrar(uidAnterior: String?, uidNuevo: String): Boolean` | `true` solo si `uidAnterior != null` y es distinto de `uidNuevo`. | RF-11 |
| 2.4 | `esOtraPersona(correoBloqueado: String?, correoIngresado: String): Boolean` | `true` si hay un dueño bloqueado y su correo es distinto del ingresado ya normalizado. | RF-11 (QA-15) |
| 2.5 | `evaluarBorrado(pendientes: Int): DecisionBorrado` | `pendientes > 0` → `Confirmar(pendientes)`; si no, `SinRiesgo`. Lo usan la salida y el ingreso de otra persona. | RF-13, RF-14 |
| 2.6 | `textoCambiosSinEnviar(n: Int): String` (presentación) | `1` → «1 cambio sin enviar»; `n` → «n cambios sin enviar». Usa la misma palabra que `AvisoConexion` (D-4). | RF-13 (QA-10) |
| ✅ | `LoginUiState.puedeIngresar` | Correo y contraseña sin quedar en blanco (`isNotBlank`), y sin un ingreso en curso. | RF-2 (QA-2) |
| ✅ | `DestinoPrincipal.para(perfil)` | Filtra los destinos por permiso. | RF-8 |
| ✅ | `Perfil.puede(permiso)` | Pertenencia al conjunto de permisos. | RF-9 |

`DecisionBorrado` es una `sealed interface` en `ReglasSesion.kt` con dos casos: `SinRiesgo` y `Confirmar(val pendientes: Int)`.

## 3. Algoritmos en pseudocódigo

### 3.1 Tabla perfil → menú (RF-1, RF-8)
```
para(perfil):
    resultado = []
    para cada destino en DestinoPrincipal (en orden de declaración):
        si destino.permiso es nulo o perfil.puede(destino.permiso):
            agregar destino a resultado
    devolver resultado
```
Resultado esperado:

| Perfil | Destinos |
|---|---|
| JEFE | Inicio, Incidencias, Indicadores |
| TECNICO | Inicio, Incidencias, Mis tareas, Diccionario |
| PERSONAL_SISTEMAS | Inicio, Incidencias, Diccionario |

### 3.2 Tabla fallo técnico → error de dominio (RF-4 a RF-7)
```
iniciarSesion(correo, contrasena):
    uidAnterior = uid de la sesión actual de Firebase (o nulo)
    intentar:
        uid = autenticar(correo, contrasena)                    # puede fallar
        si debeBorrarDatosAlEntrar(uidAnterior, uid): borrar Room      # RF-11
        perfil = perfilDesdeTexto(leer usuarios/{uid}.perfil)   # puede fallar
        si perfil es nulo: signOut(); devolver PerfilNoConfigurado     # RF-7
        devolver Exito(usuario)
    si falla con:
        credenciales inválidas o usuario inválido  -> CredencialesInvalidas   # RF-4
        red (Auth) o Firestore UNAVAILABLE         -> SinConexion             # RF-5 (QA-5)
        demasiadas solicitudes                     -> DemasiadosIntentos      # RF-6
        cancelación                                -> relanzar
        cualquier otro                             -> Log.w(TAG, mensaje sin correo, e); Desconocido
    en todo fallo posterior a autenticar: signOut() antes de devolver   # QA-17
```

### 3.3 Cerrar sesión (RF-10, RF-13, RF-14)
```
alPulsarCerrarSesion():
    decision = EvaluarCierreSesion()           # primer valor de pendientes (Room siempre emite)
    si decision es SinRiesgo:     CerrarSesion()                          # RF-14
    si decision es Confirmar(n):  estado.confirmarSalida = n              # RF-13
alConfirmarSalida(): estado.confirmarSalida = nulo; CerrarSesion()
alCancelarSalida():  estado.confirmarSalida = nulo                        # se queda en Ajustes (QA-11)
```

### 3.4 Otra persona entra con un dueño bloqueado (RF-11 + RF-13, QA-15)
```
ingresar():
    si no puedeIngresar: terminar                                         # RF-2, QA-20
    decision = EvaluarIngresoDeOtraPersona(correoIngresado)
    si decision es Confirmar(n): estado.confirmarIngreso = n; terminar
    continuar con IniciarSesion
alConfirmarIngreso(): estado.confirmarIngreso = nulo; continuar con IniciarSesion
alCancelarIngreso():  estado.confirmarIngreso = nulo
```

### 3.5 Perfil revocado con la sesión abierta (nuevo RF-15, QA-16)
```
al recibir una nueva versión de usuarios/{uid}:
    si uid no es nulo y perfilDesdeTexto(perfil) es nulo:
        ejecutar el mismo cierre que cerrarSesion()   # borra Room y hace signOut
```

## 4. Cómo se pinta en la interfaz
- **Diálogo de salida** (`AjustesSeguridadScreen`, RF-13): `AlertDialog` visible cuando `estado.confirmarSalida != null`.
  - Ícono `Icons.Outlined.CloudOff`.
  - Título «¿Cerrar sesión?».
  - Texto «Tienes {textoCambiosSinEnviar(n)}. Si cierras sesión ahora, se perderán.».
  - Botón de confirmar «Cerrar de todos modos», como `TextButton` con `colorScheme.error`.
  - Botón de cancelar «Cancelar».
  - Tocar fuera del diálogo equivale a cancelar.
- **Diálogo de ingreso** (`LoginScreen`, RF-11): misma estructura.
  - Título «¿Ingresar con otra cuenta?».
  - Texto «{Nombre} tiene {textoCambiosSinEnviar(n)} en este celular. Si ingresas, se perderán.».
  - Botones «Ingresar de todos modos» y «Cancelar».
- **Sin pendientes** (RF-14): no se muestra ningún diálogo. El botón «Cerrar sesión» actúa como hoy, y `SigtecRaiz` lleva a la pantalla de acceso porque `EstadoAcceso` pasa a `SinSesion` (RF-10).
- **Reglas de diseño:** solo se usan roles de `MaterialTheme.colorScheme` y estilos de `typography`, sin hexadecimales ni `fontSize`. Cada diálogo tiene `@Preview` claro y oscuro dentro de `SigtecTheme`. Las pantallas reciben estado y lambdas; nunca el `NavController` (principio 3).
- **Sin cambios:** login (RF-1 a RF-7), con el error en `supportingText` del campo contraseña vía `error.mensaje()`, y el menú inferior (RF-8).

## 5. Decisiones técnicas
| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D-1 | La regla «¿hay que confirmar?» vive en `ReglasSesion` y en los casos de uso `Evaluar…`; el ViewModel solo la consulta. | Principio 3: reglas en `domain/`. Así se prueba sin Android. | Decidir en el ViewModel con `if (pendientes > 0)`. Mezcla regla y UI, y obliga a probarla con un ViewModel que hoy no se puede construir en la JVM (D-8). |
| D-2 | Avisar y pedir confirmación, no sincronizar antes de cerrar. | Funciona sin conexión, es simple de entender y no deja al usuario esperando una subida que puede fallar. | Forzar la sincronización y bloquear la salida hasta que termine: falla en modo avión y añade estados de carga y error. |
| D-3 | `pendientes` = incidencias + movimientos, lo que ya calcula `EstadoDatosRepositoryImpl`. | Un solo origen de verdad; no se toca `core/sync`. | Contar en Room desde `feature/acceso`: duplicaría la consulta y rompería la frontera con `core/sync`. |
| D-4 | El texto dice «cambios», no «registros». | Coincide con `AvisoConexion` («3 cambios por enviar»): el usuario ve la misma palabra en toda la app. | «Registros», como dice hoy la spec: dos palabras distintas para lo mismo. |
| D-5 | Cualquier fallo después de autenticarse hace `signOut()`. | Evita la sesión a medias (QA-17), igual que RF-7. | Dejar la sesión abierta y reintentar el perfil: la app quedaría autenticada sin perfil ni permisos. |
| D-6 | Un perfil revocado en vivo cierra la sesión y borra Room sin pedir confirmación (nuevo RF-15). | Quien ya no tiene perfil no debe conservar datos del área (principio 5). Lo decidió un administrador, no el usuario. | Conservar los datos hasta que entre otra persona: tras el `signOut`, `uidAnterior` sería nulo y RF-11 nunca los borraría. Quedarían expuestos. |
| D-7 | RF-10 borra Room y, si quien sale es el dueño, las preferencias de huella. Las fotos quedan fuera hasta que exista la HU de captura. | Hoy no se guardan fotos (captura sigue en `PantallaEnConstruccion`). Se anota en `MEMORY.md` para que esa HU amplíe RF-10 (QA-7, QA-29). | Especificar ya el borrado de fotos: describiría código que no existe. |
| D-8 | No se crea `AjustesSeguridadViewModelTest`; la regla se prueba en los casos de uso y en las funciones puras. | `EvaluadorBiometrico` es una clase concreta que recibe un `Context`: construir el ViewModel en la JVM obligaría a cambiar `core/biometric`, que es compartido. El ViewModel no es nuevo (principio 4: «ViewModel nuevo»). | Extraer una interfaz de `EvaluadorBiometrico`: es un cambio en un archivo compartido, fuera del alcance de la HU. |
| D-9 | La traducción de excepciones de Firebase a `ErrorDominio` sigue en `SesionRepositoryFirebase`. | Es traducción técnica, no regla de negocio. El dominio no puede importar Firebase (principio 3). | Mover la traducción a `domain/`: obligaría a importar clases de Firebase en Kotlin puro. |
| D-10 | El dominio `@softcorp.pe` no se valida en la app (QA-4). | Las cuentas solo las crea el jefe en la consola de Firebase; una validación local repetiría esa regla en dos sitios. | Validar el dominio en la app: añade un mensaje y una regla más sin mejorar la seguridad. |
| D-11 | Un campo con solo espacios cuenta como vacío (QA-2). | Es lo que hace `isNotBlank` y evita enviar algo que Firebase rechazará igual. | Permitir contraseñas de solo espacios: no se crean así en la consola. |

## 6. Cambios que este plan exige en `spec.md` (antes de programar, principio 2)

> ✅ Puntos 1 a 7 aplicados el 2026-10-05 (RF-5b y RF-15 agregados a la spec; el punto 7 se aplicó en `docs/constitution.md`).
1. **RF-10:** «CUANDO el usuario confirma el cierre de sesión, o no hay cambios sin enviar…». Resuelve QA-14.
2. **RF-11:** agregar que, si hay un dueño bloqueado con cambios sin enviar, se pide confirmación antes (sección 3.4). Resuelve QA-15.
3. **RF-13:** cambiar «registros» por «cambios», definir el singular y el plural y que cancelar se queda en Ajustes. Resuelve QA-9, QA-10 y QA-11.
4. **Nuevo RF-15:** perfil revocado con la sesión abierta → cierre completo (D-6). Resuelve QA-16.
5. **RF-5:** incluir la falla al leer el perfil en Firestore. Resuelve QA-5 y QA-17.
6. **Casos límite:** cuenta deshabilitada → «Correo o contraseña incorrectos» (QA-19); doble pulsación (QA-20); abrir sin conexión y sin perfil en caché → pantalla de acceso (QA-21).
7. **Constitución (QA-28):** el principio 5 prohíbe guardar tokens, pero RF-12 depende del token que guarda Firebase Auth. Se aclaró en el principio 5 que la sesión que guarda Firebase Auth es la única excepción.

## 7. Estrategia de pruebas
Se usa `./gradlew testDebugUnitTest`, con JUnit 4, `kotlinx-coroutines-test` y dobles propios (`SesionRepositoryDemo`, `PreferenciasSeguridadFalsas` y el nuevo `EstadoDatosRepositoryFalso`). No se agregan librerías (principios 1 y 4). Cada RF tiene al menos un caso de sin sesión o sin permiso, uno de datos inválidos y uno del camino feliz, cuando aplica.

| Prueba | Casos | RF |
|---|---|---|
| `ReglasSesionTest` | `normalizarCorreo`: espacios en los extremos, mayúsculas, espacios internos intactos. `perfilDesdeTexto`: los 3 valores válidos, nulo, texto mal escrito y minúsculas. `debeBorrarDatosAlEntrar`: anterior nulo, mismo uid, uid distinto. `esOtraPersona`: sin bloqueado, mismo correo con otro formato, otro correo. `evaluarBorrado`: 0, 1, 5. | RF-3, RF-7, RF-11, RF-13, RF-14 |
| `DestinoPrincipalTest` | La tabla de la sección 3.1 para los 3 perfiles; el jefe no ve el diccionario. | RF-8 |
| `EvaluarCierreSesionTest` | 0 pendientes → `SinRiesgo`; 3 → `Confirmar(3)`. | RF-13, RF-14 |
| `EvaluarIngresoDeOtraPersonaTest` | Sin dueño bloqueado → `SinRiesgo`; el mismo dueño → `SinRiesgo`; otra persona con 0 → `SinRiesgo`; otra persona con 2 → `Confirmar(2)`. | RF-11, RF-13 |
| `TextosSalidaTest` | 1 → singular, 2 → plural. | RF-13 |
| `LoginViewModelTest` (+3) | Otra persona con pendientes abre el diálogo y no inicia sesión; confirmar inicia sesión; cancelar no la inicia. | RF-11 |
| Existentes | `SesionYPermisosTest`, `EstadoAccesoTest`, `LoginViewModelTest`. | RF-1 a RF-4, RF-9, RF-12 |

**Sin prueba automática, con su motivo y su verificación manual en el celular:**
- RF-5, RF-6, RF-7 y RF-15: dependen de `SesionRepositoryFirebase`, que usa clases de Firebase que no se construyen en la JVM. Se prueban con la cuenta real: modo avión al ingresar (RF-5), contraseña incorrecta repetida (RF-6), una cuenta sin documento en `usuarios` (RF-7), y cambiar el perfil a un valor inválido desde la consola con la app abierta (RF-15).
- RF-10 y RF-13: registrar una incidencia en modo avión, pulsar «Cerrar sesión», ver el diálogo, cancelar, confirmar, y comprobar que otra cuenta no ve esa incidencia.
- Con un emulador conectado, el flujo se verifica con `adb` según el punto 6 de «Verificación» de `CLAUDE.md`: captura en claro y oscuro de cada diálogo.

**Cierre:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde antes de abrir el PR.

## 8. Cobertura de RF
| RF | Dónde vive | Prueba |
|---|---|---|
| RF-1 | `IniciarSesion`, `LoginViewModel`, `DestinoPrincipal` | `LoginViewModelTest`, `DestinoPrincipalTest` |
| RF-2 | `LoginUiState.puedeIngresar` | `LoginViewModelTest` |
| RF-3 | `normalizarCorreo` | `ReglasSesionTest`, `LoginViewModelTest` |
| RF-4 | `SesionRepositoryFirebase`, `MensajesError` | `SesionYPermisosTest`, `LoginViewModelTest` |
| RF-5 y RF-5b | `SesionRepositoryFirebase` (D-5, D-9) | manual |
| RF-6 | `SesionRepositoryFirebase` | manual |
| RF-7 | `perfilDesdeTexto`, `SesionRepositoryFirebase` | `ReglasSesionTest` + manual |
| RF-8 | `DestinoPrincipal.para` | `DestinoPrincipalTest` |
| RF-9 | `VerificarPermiso`, `Perfil.puede` | `SesionYPermisosTest` |
| RF-10 | `CerrarSesion`, `SesionRepositoryFirebase.cerrarSesion` | `EstadoAccesoTest` (huella) + manual |
| RF-11 | `debeBorrarDatosAlEntrar`, `esOtraPersona`, `EvaluarIngresoDeOtraPersona` | `ReglasSesionTest`, `EvaluarIngresoDeOtraPersonaTest`, `LoginViewModelTest` |
| RF-12 | `ObservarEstadoAcceso` | `EstadoAccesoTest` |
| RF-13 | `evaluarBorrado`, `EvaluarCierreSesion`, diálogo de salida | `ReglasSesionTest`, `EvaluarCierreSesionTest`, `TextosSalidaTest` + manual |
| RF-14 | `evaluarBorrado`, `EvaluarCierreSesion` | `ReglasSesionTest`, `EvaluarCierreSesionTest` |
| RF-15 (nuevo) | `perfilDesdeTexto`, `SesionRepositoryFirebase` | `ReglasSesionTest` + manual |

## 9. Respeto a la constitución
1. **Stack cerrado:** sin dependencias nuevas; `libs.versions.toml` no cambia.
2. **Spec antes que código:** la sección 6 se aplica a `spec.md` antes de programar. La HU-01 es retroactiva: RF-1 a RF-12 se especificaron después del PR #5, y eso se declara en la cabecera de la spec.
3. **Lógica fuera de la pantalla:** reglas en `ReglasSesion` y en los casos de uso; las `Screen` solo pintan los diálogos según el estado.
4. **Toda regla tiene prueba:** cada función pura y cada caso de uso nuevo tienen prueba. Las excepciones manuales están justificadas en la sección 7.
5. **Datos protegidos:** D-6 y RF-11 evitan que datos de una persona queden para otra. Las contraseñas no se guardan. El token que guarda Firebase Auth es la excepción declarada en el principio 5.
6. **Español:** funciones, textos y pruebas en español; solo los sufijos de Android en inglés.

## 10. Archivos compartidos que toca (requieren visto bueno)
`core/domain/ReglasSesion.kt` (nuevo), `core/domain/usecase/IniciarSesion.kt`, los dos casos de uso `Evaluar…` nuevos y `core/data/SesionRepositoryFirebase.kt`. Todos son parte del modelo de sesión, que `CLAUDE.md` pide coordinar antes de tocar. ✅ Coordinado con el equipo (2026-10-05).
