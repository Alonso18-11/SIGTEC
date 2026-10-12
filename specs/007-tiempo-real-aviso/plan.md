# Plan 007 — Tiempo real y aviso de asignación (HU-07)

> Basado en `spec.md` (borrador) y `docs/constitution.md`. La HU toca `core/sync`, el `AndroidManifest.xml` y la navegación raíz, así que se entrega en cuatro PR pequeños. Cada PR tiene su rama, commits chicos con la HU al final del título y la aprobación de la otra persona del equipo.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/main/res/` (recursos) y `app/src/test/java/com/softcorp/sigtec/` (pruebas JVM).

## 0. Qué ya existe y no se repite
- **Tiempo real (RF-701):** la HU-05 ya escucha `incidencias`, `movimientos` y `equipos` mientras hay sesión (RF-58) y la HU-06 agregó los técnicos (RF-619). La lista, el detalle y la línea de tiempo leen de Room con `Flow`, así que se actualizan solos. Esta HU solo agrega la prueba de RF-701; no cambia esa parte.
- **Detección:** `SincronizacionTiempoReal.guardarIncidencias` ya compara la versión local con la remota antes de guardar (`debeReemplazarLocal`, `atencionAlBajar`). El aviso se engancha ahí, sin otra escucha.
- **Dependencias:** `NotificationCompat` y `NotificationManagerCompat` (`androidx.core`) y el lanzador de permisos (`activity-compose`) ya están en el proyecto. No se agrega ninguna librería.
- **Room y reglas de Firestore:** sin cambios. `SigtecDatabase.version` sigue en 2.
- **Ámbitos de commit:** `core` para casi todo (no hay módulo de feature para esta HU) e `incidencias` para la prueba de la lista.

## 1. División en PR, dependencias y orden de fusión

| PR | Rama | Título del PR | Sale de | Depende de | En paralelo con |
|---|---|---|---|---|---|
| A7 | `docs/hu07-tiempo-real-spec` | `docs(core): spec, plan y tareas del tiempo real y el aviso de asignación (HU-07)` | `main` | — | — |
| B7 | `feat/aviso-notificacion` | `feat(core): notificación local de asignación y su permiso (HU-07)` | `main` con A7 | A7 | — |
| C7 | `feat/aviso-deteccion` | `feat(core): avisar al técnico cuando baja una asignación nueva (HU-07)` | `main` con B7 | B7 | D7 |
| D7 | `feat/aviso-abrir-detalle` | `feat(core): abrir el detalle al tocar el aviso (HU-07)` | `main` con B7 | B7 | C7 |

**Orden de fusión:** A7 → B7 → C7 y D7, en cualquier orden.

**Por qué:**
- B7 crea lo que los otros dos usan: el paquete `core/notificaciones` (texto, canal, notificación, `PendingIntent`) y el permiso.
- C7 hace que el aviso salga: engancha la detección en `core/sync`.
- D7 hace que tocarlo abra el detalle: lee el `PendingIntent` en `MainActivity` y navega desde la raíz.
- C7 y D7 no comparten archivos, así que pueden ir en paralelo.
- Con solo B7 fusionado no pasa nada visible: el notificador existe, pero nadie lo llama.
- Con B7 y C7, tocar el aviso abre la app en Inicio; con D7, abre el detalle.
- Si B7 sigue en revisión, C7 y D7 salen de su rama (ramas apiladas) y se actualizan con `git pull origin main` cuando B7 se fusione.

## 2. Archivos y commits por PR

Leyenda: 🆕 se crea · ✏️ se modifica · ⚠️ compartido según la lista «Pregunta antes» de `CLAUDE.md` · 🔶 sensible fuera de esa lista (`core/sync`, `core/navigation`, archivos nuevos fuera de un módulo de feature).

### PR A7 — spec, plan y tareas
| Archivo | Cambio |
|---|---|
| 🆕 `specs/007-tiempo-real-aviso/spec.md`, `plan.md`, `tasks.md` | RF-701 a RF-713. |
| ✏️ `MEMORY.md` | Solo la línea de la HU-07: «spec en borrador (RF-701 a RF-713)». |

- **Compartidos:** ninguno. **Room:** sin cambios.
- **Commits:**
  1. `docs(core): spec del tiempo real y el aviso de asignación (HU-07)`
  2. `docs(core): plan por PR del aviso de asignación (HU-07)`
  3. `docs(core): tareas del aviso de asignación por PR (HU-07)`
  4. `docs(core): estado de la spec de la HU-07 en MEMORY (HU-07)`

### PR B7 — notificación local y permiso
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕🔶 `core/notificaciones/ContenidoAviso.kt` | Funciones puras: `contenidoAvisoAsignacion(incidencia)` (título, texto y versión pública) e `idNotificacion(incidenciaId)` (sección 3). | RF-702, RF-704, RF-707 |
| 🆕🔶 `core/notificaciones/NotificadorAsignaciones.kt` | `@Singleton`. Crea el canal «Asignaciones» si falta, comprueba el permiso y publica la notificación con `PendingIntent` a `MainActivity`. | RF-702, RF-704, RF-706, RF-707 |
| 🆕🔶 `core/notificaciones/PermisoNotificaciones.kt` | Función pura `debePedirPermisoNotificaciones(sdk, concedido, perfil)` y composable `PedirPermisoNotificaciones(...)` con `rememberLauncherForActivityResult`. | RF-708 a RF-710 |
| 🆕 `res/drawable/ic_aviso_asignacion.xml` | Ícono monocromo de la notificación (vector). | RF-702 |
| ✏️⚠️ `AndroidManifest.xml` | `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`. | RF-708 |
| ✏️⚠️ `core/navigation/SigtecRaiz.kt` | Una llamada a `PedirPermisoNotificaciones(...)` con la sesión activa y sin la oferta de huella en pantalla. | RF-708 |
| 🆕 pruebas | `ContenidoAvisoTest`, `PermisoNotificacionesTest`. | — |

- **Compartidos ⚠️:** `AndroidManifest.xml` (un permiso nuevo; `CLAUDE.md` pide preguntar antes de agregar permisos) y `SigtecRaiz.kt`. **Room:** sin cambios.
- **Commits:**
  1. `feat(core): texto e identificador del aviso de asignación (HU-07)`
  2. `feat(core): notificación local de asignación en su canal (HU-07)`
  3. `feat(core): permiso de notificaciones de Android 13 en el manifest (HU-07)`
  4. `feat(core): pedir el permiso de notificaciones al técnico (HU-07)`
  5. `docs(core): tareas del PR B7 hechas (HU-07)`

### PR C7 — detectar la asignación nueva y avisar
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️🔶 `core/sync/ReglasSincronizacion.kt` | Función pura `esAsignacionNuevaPara(usuarioId, local, remota)` (sección 3). | RF-702, RF-703, RF-705 |
| ✏️🔶 `core/sync/SincronizacionTiempoReal.kt` | Recibe `NotificadorAsignaciones`. `guardarIncidencias` recibe el `uid` de la sesión y, después de guardar la remota, avisa si corresponde (4.2). | RF-702 |
| ✏️ pruebas | `ReglasSincronizacionTest` (casos de `esAsignacionNuevaPara`) y `ListaIncidenciasViewModelTest` (RF-701: un cambio en Room actualiza la lista sin acción). | RF-701 a RF-705 |

- **Compartidos ⚠️:** ninguno. **Sensibles 🔶:** `core/sync`. **Room:** sin cambios.
- **Commits:**
  1. `feat(core): reconocer al bajar una asignación nueva para quien tiene la sesión (HU-07)`
  2. `feat(core): avisar al técnico cuando la sincronización baja su asignación (HU-07)`
  3. `test(incidencias): la lista se actualiza sola cuando cambia Room (HU-07)`
  4. `docs(core): tareas del PR C7 hechas (HU-07)`

### PR D7 — abrir el detalle al tocar el aviso
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕🔶 `core/navigation/AvisoTocado.kt` | `data class AvisoTocado(incidenciaId, usuarioId)` y función pura `destinoDelAviso(estado, aviso)` → `Abrir` / `Esperar` / `Descartar` (sección 3). | RF-711 a RF-713 |
| ✏️🔶 `MainActivity.kt` | Lee los extras del aviso en `onCreate` (solo si no se está restaurando) y en `onNewIntent`, y se los pasa a `RaizViewModel`. | RF-711 |
| ✏️🔶 `core/navigation/RaizViewModel.kt` | `avisoPendiente: StateFlow<AvisoTocado?>`, `alTocarAviso(...)` y `alAtenderAviso()`. | RF-711 a RF-713 |
| ✏️⚠️ `core/navigation/SigtecRaiz.kt` | Con la sesión activa y fuera del login, aplica `destinoDelAviso`: navega a `Ruta.DetalleIncidencia(id)` o descarta (4.3). | RF-711 a RF-713 |
| 🆕 pruebas | `AvisoTocadoTest`. | RF-711 a RF-713 |

- **Compartidos ⚠️:** `SigtecRaiz.kt`. **Sensibles 🔶:** `MainActivity`, `RaizViewModel`. `Rutas.kt` no cambia (`DetalleIncidencia` ya existe). **Room:** sin cambios.
- **Commits:**
  1. `feat(core): decidir adónde lleva tocar el aviso según la sesión (HU-07)`
  2. `feat(core): recibir en la app el aviso tocado (HU-07)`
  3. `feat(core): abrir el detalle de la incidencia del aviso (HU-07)`
  4. `docs(core): tareas del PR D7 hechas y pruebas manuales pendientes (HU-07)`

## 3. Funciones puras de lógica

| # | Firma (dónde) | Regla | RF |
|---|---|---|---|
| 3.1 | `esAsignacionNuevaPara(usuarioId: String, local: IncidenciaEntity?, remota: IncidenciaEntity): Boolean` (`core/sync/ReglasSincronizacion.kt`) | `local != null && local.tecnicoAsignadoId != usuarioId && remota.tecnicoAsignadoId == usuarioId && remota.marca != CERRADA`. Si es la primera vez que llega (`local == null`), no avisa. | RF-702, RF-703, RF-705 |
| 3.2 | `contenidoAvisoAsignacion(incidencia: Incidencia): ContenidoAviso` (`core/notificaciones`) | Título «Nueva incidencia asignada»; texto «INC-0127 · IMP-RRHH-002 · La impresora no aparece en la red»; versión pública solo con el título. Una incidencia asignada siempre tiene número (RF-610), pero si faltara se usa «Incidencia». | RF-702, RF-707 |
| 3.3 | `idNotificacion(incidenciaId: String): Int` | `incidenciaId.hashCode()`: estable por incidencia, así un segundo aviso reemplaza al primero. La HU-17 usará el mismo. | RF-704 |
| 3.4 | `debePedirPermisoNotificaciones(sdk: Int, concedido: Boolean, perfil: Perfil?): Boolean` (`core/notificaciones`) | `sdk >= 33 && !concedido && perfil?.puede(Permiso.ATENDER_INCIDENCIA) == true`. | RF-708, RF-710 |
| 3.5 | `destinoDelAviso(estado: EstadoAcceso, aviso: AvisoTocado): DestinoDelAviso` (`core/navigation`) | `Activa` con el mismo usuario → `Abrir(incidenciaId)`; `Activa` con otro usuario → `Descartar`; `Bloqueada` o `SinSesion` → `Esperar` (se decide cuando la sesión quede activa). | RF-711 a RF-713 |

Las cinco son Kotlin puro y se prueban en la JVM sin clases de Android ni de Firebase. `idNotificacion` y `contenidoAvisoAsignacion` viven en `core/notificaciones` junto al notificador, pero sin importar Android.

## 4. Algoritmos en pseudocódigo

### 4.1 Notificador (PR B7)
```
avisar(incidencia, usuarioId):
    si sdk >= 33 y no hay permiso POST_NOTIFICATIONS: salir            // RF-709: sin permiso, sin aviso
    crear el canal "asignaciones" («Asignaciones», importancia alta) si no existe   // idempotente (RF-706)
    contenido = contenidoAvisoAsignacion(incidencia)
    intent = Intent(contexto, MainActivity) con extras {incidenciaId, usuarioId}
             y flags NEW_TASK | CLEAR_TOP | SINGLE_TOP                  // si la app está abierta, llega a onNewIntent
    pendiente = PendingIntent.getActivity(contexto, idNotificacion(id), intent, UPDATE_CURRENT | IMMUTABLE)
    notificación = NotificationCompat.Builder(contexto, "asignaciones")
        .ícono(ic_aviso_asignacion).título(contenido.titulo).texto(contenido.texto)
        .visibilidad(PRIVATE).versiónPública(solo contenido.textoPublico)            // RF-707
        .prioridad(HIGH).autoCancel(true).contentIntent(pendiente)                    // RF-711: se quita al tocar
    NotificationManagerCompat.notify(idNotificacion(id), notificación)              // RF-704: reemplaza
```
El permiso se comprueba justo antes de `notify` (lint `MissingPermission`), y una `SecurityException` se registra con `Log.w` sin romper la sincronización.

### 4.2 Detección al bajar (PR C7)
```
iniciar(): ... escuchar("incidencias", { snap -> guardarIncidencias(snap, uid) }) ...

guardarIncidencias(snap, uid):
    para cada cambio no REMOVED y sin escrituras pendientes propias:      // RF-705: lo de este celular no avisa
        remota = mapear(cambio); local = incidenciaDao.porId(remota.id)
        si debeReemplazarLocal(local, remota):
            transacción de Room (sin cambios, RF-79)
            si esAsignacionNuevaPara(uid, local, remota):                  // 3.1
                notificador.avisar(remota.toDomain(), uid)                  // después de guardar
```
Avisar después de guardar hace que, al tocar el aviso, el detalle ya tenga la versión nueva en Room.

### 4.3 Abrir el detalle (PR D7)
```
MainActivity.onCreate(saved): si saved == null: leerAviso(intent)
MainActivity.onNewIntent(intent): leerAviso(intent)
leerAviso(intent): si trae incidenciaId y usuarioId: raiz.alTocarAviso(AvisoTocado(incidenciaId, usuarioId))

SigtecRaiz: LaunchedEffect(estado, avisoPendiente, destinoActual):
    aviso = avisoPendiente ?: salir
    si destinoActual es Login: salir         // primero la navegación por acceso lleva a Inicio
    según destinoDelAviso(estado, aviso):
        Abrir(id)  -> navController.navigate(Ruta.DetalleIncidencia(id)); raiz.alAtenderAviso()
        Descartar  -> raiz.alAtenderAviso()
        Esperar    -> nada (se reevalúa cuando cambie el estado)
```
`MainActivity` obtiene el mismo `RaizViewModel` que `SigtecRaiz` (`by viewModels()`, ámbito de la actividad). Como el aviso vive en el `ViewModel`, sobrevive a la rotación (aprendizaje de la HU-02 en `MEMORY.md`).

### 4.4 Pedir el permiso (PR B7)
```
@Composable PedirPermisoNotificaciones(perfil, habilitado):
    si sdk < 33: salir                                  // RF-710
    var pedido by rememberSaveable { false }            // una vez por apertura de la app
    lanzador = rememberLauncherForActivityResult(RequestPermission()) { }   // si lo niega, no se hace nada más (RF-709)
    LaunchedEffect(perfil, habilitado):
        concedido = checkSelfPermission(POST_NOTIFICATIONS) == GRANTED
        si habilitado y !pedido y debePedirPermisoNotificaciones(sdk, concedido, perfil):
            pedido = true; lanzador.launch(POST_NOTIFICATIONS)

SigtecRaiz: PedirPermisoNotificaciones(usuario?.perfil, habilitado = activa && !mostrarOferta)
```
Android deja de mostrar el diálogo tras dos rechazos y entonces `launch` responde «negado» sin mostrar nada, así que no hace falta guardar nada en DataStore.

## 5. Cómo se pinta
No hay pantallas nuevas. La notificación usa el ícono `ic_aviso_asignacion` (vector monocromo de 24 dp, tintado por el sistema) y los textos de 3.2. El permiso usa el diálogo del sistema.

## 6. Decisiones técnicas
| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D-1 | La notificación la genera la app cuando la sincronización baja el cambio. | Costo cero: no hace falta un servidor. Es lo que dice el documento del producto («generadas por la propia aplicación cuando detecta cambios en Cloud Firestore»). | Firebase Cloud Messaging: necesita Cloud Functions para enviar, que no existen en el plan Spark. |
| D-2 | Solo se avisa si el cambio se observó: la incidencia ya estaba en el celular y no era de la persona (RF-703). | Evita una avalancha de avisos en la primera descarga tras iniciar sesión o tras borrar los datos de otra persona. | Avisar de toda incidencia asignada que llega por primera vez: un técnico con 5 casos recibiría 5 avisos al entrar. |
| D-3 | Un identificador de notificación estable por incidencia (RF-704). | Un segundo aviso reemplaza al primero, y la HU-17 puede reutilizarlo sin duplicar. | Un número creciente por aviso: duplicaría avisos de la misma incidencia. |
| D-4 | Paquete nuevo `core/notificaciones` con una clase concreta inyectada, sin interfaz ni módulo de DI. | Lo usarán la HU-07, la HU-17 y la HU-18, así que no pertenece a una feature. Sin interfaz no se toca `core/di` (compartido), y la llamada vive en `core/sync`, que no tiene pruebas JVM que necesiten un doble. Las reglas puras sí se prueban. | Interfaz en `core/domain` con `@Binds` en `core/di`: un archivo compartido más sin una prueba que lo justifique. |
| D-5 | El canal se crea de forma perezosa en el notificador. | `createNotificationChannel` es idempotente y no hace falta tocar `SigtecApp`. | Crearlo en `SigtecApp.onCreate`: otro archivo de arranque modificado. |
| D-6 | El permiso se pide desde la raíz al técnico con sesión activa, una vez por apertura y después de la oferta de huella. | Es el momento en que tiene sentido (ya sabemos que es técnico), no se cruza con el diálogo de la HU-02, y Android ya limita a dos rechazos. | Guardar «ya se pidió» en DataStore: toca preferencias compartidas. Pedirlo en Ajustes de seguridad: `feature/acceso` y el modelo de sesión no se tocan sin coordinar. Pedirlo al llegar la primera asignación: puede llegar con la app en segundo plano, sin pantalla para el diálogo. |
| D-7 | Al tocar, el aviso queda pendiente en `RaizViewModel` y la raíz lo abre cuando la sesión está activa y ya salió del login (RF-711 a RF-713). | Respeta que la navegación sigue a `EstadoAcceso` (`CLAUDE.md`) y que nadie entra al detalle sin pasar por la huella. Cubre el arranque en frío, que empieza sin sesión. | Un deep link de Navigation Compose: abriría el detalle por encima del login o del desbloqueo. |
| D-8 | RF-701 no lleva código nuevo, solo una prueba. | La HU-05 ya lo cumple; reescribirlo sería refactorizar «de paso». | Un botón o gesto para refrescar: innecesario con datos en tiempo real. |
| D-9 | Visibilidad privada con versión pública solo con el título (RF-707). | No expone el equipo ni la descripción en la pantalla de bloqueo. | Visibilidad pública: cualquiera que vea el celular lee el caso. |
| D-10 | No se avisa de reasignaciones a otro, de incidencias cerradas ni de cambios propios (RF-705). | El aviso es para trabajo nuevo; lo demás se ve en la lista en tiempo real. | Avisar de todo cambio: ruido. |

## 7. Estrategia de pruebas
`./gradlew testDebugUnitTest`, JUnit 4, coroutines-test y dobles propios. Sin dependencias nuevas.

| Prueba | Casos | RF |
|---|---|---|
| `ReglasSincronizacionTest` (C7) | Asignada a otro (o sin técnico) y ahora a mí → avisa; llega por primera vez ya mía → no; ya era mía → no; asignada a mí pero cerrada → no; reasignada de mí a otro → no. | RF-702, RF-703, RF-705 |
| `ListaIncidenciasViewModelTest` (C7) | Con la lista abierta, cambiar en el repositorio falso la marca de una incidencia a Cerrada (como lo haría la sincronización): el estado de la lista cambia sin llamar a nada. | RF-701 |
| `ContenidoAvisoTest` (B7) | Título, texto con código, equipo y descripción, versión pública sin datos, y el mismo id para la misma incidencia (distinto para otra). | RF-702, RF-704, RF-707 |
| `PermisoNotificacionesTest` (B7) | Android 13 + técnico sin permiso → pedir; Android 12 → no; ya concedido → no; jefe o personal de sistemas → no; sin perfil → no. | RF-708, RF-710 |
| `AvisoTocadoTest` (D7) | Activa con el mismo usuario → abrir; activa con otro → descartar; bloqueada → esperar; sin sesión → esperar. | RF-711 a RF-713 |

**Verificado a mano, pendiente de que el equipo lo programe:** que la notificación aparece en otro dispositivo al asignar (RF-702), su versión en la pantalla de bloqueo (RF-707), el diálogo de permiso en Android 13+ y su rechazo (RF-708, RF-709), y tocar el aviso con la app abierta, cerrada y bloqueada (RF-711 a RF-713). Para probar sin dos celulares: emulador con el técnico y el jefe asignando desde otro emulador o celular. Asignar editando el documento en la consola de Firestore se salta la transacción de la carga (HU-06), así que, si se hace, hay que dejar la carga como estaba.

**Cierre de cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde.

## 8. Cobertura de RF
| RF | Dónde vive | Prueba |
|---|---|---|
| RF-701 | HU-05 (RF-58) + pantallas que leen Room | `ListaIncidenciasViewModelTest` + manual |
| RF-702 | `esAsignacionNuevaPara`, `SincronizacionTiempoReal`, `NotificadorAsignaciones`, `contenidoAvisoAsignacion` | `ReglasSincronizacionTest`, `ContenidoAvisoTest` + manual |
| RF-703, RF-705 | `esAsignacionNuevaPara` | `ReglasSincronizacionTest` |
| RF-704 | `idNotificacion` | `ContenidoAvisoTest` |
| RF-706 | `NotificadorAsignaciones` (canal) | manual (ajustes de Android) |
| RF-707 | `contenidoAvisoAsignacion`, `NotificadorAsignaciones` (visibilidad) | `ContenidoAvisoTest` + manual |
| RF-708 a RF-710 | `debePedirPermisoNotificaciones`, `PedirPermisoNotificaciones`, manifest | `PermisoNotificacionesTest` + manual |
| RF-711 a RF-713 | `destinoDelAviso`, `MainActivity`, `RaizViewModel`, `SigtecRaiz` | `AvisoTocadoTest` + manual |

## 9. Respeto a la constitución
1. **Stack cerrado:** sin dependencias nuevas.
2. **Spec antes que código:** esta spec se aprueba en el PR A7, antes de B7 a D7.
3. **Lógica fuera de la pantalla:** cuándo avisar, qué texto, cuándo pedir el permiso y adónde llevar el toque son funciones puras en Kotlin; la raíz y la actividad solo las aplican.
4. **Toda regla tiene prueba:** sección 7. Lo que depende de Android o Firestore real (mostrar la notificación, el diálogo de permiso, la escucha) se verifica a mano.
5. **Datos protegidos:** la notificación no muestra datos del caso con el celular bloqueado, y tocarla nunca se salta el login ni la huella (D-7).
6. **Español:** textos, nombres y pruebas en español.

## 10. Resumen: archivos compartidos y Room por PR (requieren visto bueno)
| PR | Compartidos ⚠️ («Pregunta antes» de `CLAUDE.md`) | Sensibles 🔶 | Room |
|---|---|---|---|
| A7 | ninguno | — | sin cambios |
| B7 | `AndroidManifest.xml` (permiso nuevo `POST_NOTIFICATIONS`), `SigtecRaiz.kt` | paquete nuevo `core/notificaciones`, ícono en `res/drawable` | sin cambios |
| C7 | ninguno | `core/sync/ReglasSincronizacion.kt`, `core/sync/SincronizacionTiempoReal.kt` | sin cambios |
| D7 | `SigtecRaiz.kt` | `MainActivity.kt`, `core/navigation/RaizViewModel.kt`, `core/navigation/AvisoTocado.kt` (nuevo) | sin cambios |

Ningún PR toca `libs.versions.toml`, `app/build.gradle.kts`, `SigtecDatabase.kt`, las entidades, `Rutas.kt`, `Resultado.kt`, `core/domain/model/*`, `MensajesError.kt`, `core/theme/*`, `core/di/*`, `firestore.rules` ni `feature/acceso/*`.
