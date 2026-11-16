# Spec 017 — Envío y avisos en segundo plano, con la app cerrada (HU-17)

> Estado: lista (2026-11-16). Decisiones D-1 a D-6 aprobadas por Nina el 2026-11-16, sin cambios; dudas respondidas.
> El cómo está en `plan.md` y las tareas en `tasks.md`.

> Implementación (2026-11-16): RF-1709 a RF-1713 en #219 (regla de aviso y guardado compartido) y RF-1701 a RF-1708 y
> RF-1714 a RF-1717 en #220 (revisión periódica, sobre #219). Revisada en el emulador con `cmendoza`: la revisión queda
> programada cada 15 minutos y se anota el inicio de sesión. Falta la prueba en un celular real con el técnico.

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-17
> (RF-1701…). Sale de la planificación (`SIGTEC_Planificación.xlsx`, Sprint 5, épica EP-08 «Trabajo automático en
> segundo plano») y de la sección 15 del documento del producto («Aviso de nuevas asignaciones», cada quince minutos).
> Criterios del curso: 7 (WorkManager) y 8 (SQLite). No tiene prototipo: no agrega pantallas.

## Contexto y objetivo
El técnico trabaja con el celular en el bolsillo y la app cerrada la mayor parte del día. Hoy la app ya hace dos cosas
en segundo plano, pero ninguna alcanza para esta HU:

- Lo registrado sin señal se sube solo cuando vuelve la conexión, aunque la app esté cerrada: lo hace
  `SubirCambiosWorker` (HU-05, RF-66), que WorkManager guarda y ejecuta incluso después de reiniciar el celular. Pero
  esa subida solo se programa cuando aparece un pendiente con la app abierta; si una pasada termina con algo pendiente
  (RF-74), nada la vuelve a programar hasta la próxima vez que se abra la app.
- El aviso «Nueva incidencia asignada» (HU-07) lo genera la escucha en tiempo real de `SincronizacionTiempoReal`, que
  solo existe mientras el proceso de la app está vivo. Con la app cerrada no llega ningún aviso, y al abrirla tampoco
  se avisa de una incidencia que se creó y se asignó mientras tanto (RF-703 de la HU-07).

Sin Firebase Cloud Messaging (exige un servidor con Cloud Functions, fuera del plan Spark), la única forma de enterarse
con la app cerrada es que el propio celular pregunte. Esta HU agrega una revisión periódica con WorkManager, cada
15 minutos (el mínimo que permite Android), que consulta al servidor las asignaciones de quien tiene la sesión, las
guarda en Room con las reglas de siempre y muestra el aviso de la HU-07 cuando hay una nueva. La misma revisión sirve de
respaldo de la subida.

## Usuarios / actores
- Técnico: recibe el aviso de sus asignaciones nuevas con la app cerrada; es el único perfil con
  `ATENDER_INCIDENCIA` (`Perfil.kt`).
- Jefe y personal de sistemas: no reciben asignaciones; su revisión solo hace el respaldo de la subida (D-3).
- `core/sync`: el único lugar que habla con Firestore; aquí vive la revisión.
- WorkManager y Android: deciden cuándo corre de verdad la revisión (reposo, ahorro de batería, fabricante).

## Historias de usuario
- HU-17: Como técnico, quiero que lo que registro sin señal se envíe solo y que la app me avise de nuevas asignaciones
  aunque esté cerrada, para no perder información ni casos.
- Descripción de la planificación: cuando el celular recupera la conexión, la app envía las incidencias e informes
  registrados sin señal y descarga los cambios de los demás. Además, cada 15 minutos revisa si hay asignaciones nuevas y
  muestra una notificación.
- Criterio de aceptación de la planificación: al volver la conexión, lo registrado sin señal aparece para el resto del
  área sin que el técnico haga nada; con la app cerrada, una nueva asignación genera una notificación en un máximo de
  15 minutos.

## Lo que ya existe y se reutiliza
| Pieza | De | Qué cubre | Qué le falta para la HU-17 |
|---|---|---|---|
| `SubirCambiosWorker` + `ProgramadorSincronizacion` | HU-05 | Sube lo pendiente al volver la red, con la app cerrada y tras reiniciar (primera mitad del criterio). | Un respaldo periódico para el límite de RF-74. |
| `NotificadorAsignaciones`, `contenidoAvisoAsignacion`, `idNotificacion` | HU-07 | Canal «Asignaciones», texto, privacidad en la pantalla de bloqueo, id estable por incidencia. | Que alguien lo llame con la app cerrada. |
| `AvisoTocado` y la navegación por `EstadoAcceso` | HU-07 | Tocar el aviso abre el detalle, pasando antes por el desbloqueo. | Nada. |
| `PedirPermisoNotificaciones` | HU-07 | Pide el permiso al técnico en Android 13+. | Nada. |
| `debeReemplazarLocal`, `atencionAlBajar` | HU-05 | Reglas de conflicto al bajar una incidencia. | Usarlas también desde la revisión. |
| `esAsignacionNuevaPara` | HU-07 | Cuándo avisar. | El caso de la incidencia que llega por primera vez ya asignada (D-1). |
| `RegistrarEnLogisticaWorker` + `ProgramadorLogistica` | HU-11 | Modelo de worker con Hilt y red obligatoria. | No aplica: es de Logística. |
| Aviso «tienes N registros sin enviar» al cerrar sesión | HU-01 (RF-13, RF-14), HU-09 (RF-943) | El pendiente de `CLAUDE.md` sobre `clearAllTables()` ya está resuelto. | Nada. |

## Requisitos funcionales (criterios de aceptación en EARS)

### La revisión periódica
- RF-1701: MIENTRAS haya una sesión de Firebase en el celular, activa o bloqueada con huella, EL SISTEMA mantiene
  programada una revisión en segundo plano cada 15 minutos que solo corre con conexión. Sigue programada aunque la
  persona cierre la app o reinicie el celular, y programarla de nuevo no crea una segunda (D-3).
- RF-1702: CUANDO la persona cierra sesión, EL SISTEMA cancela la revisión. Bloquear la sesión no la cancela.
- RF-1703: SI la revisión corre y no hay sesión (por ejemplo, se cerró justo antes), ENTONCES termina sin consultar
  nada ni avisar.
- RF-1704: SI la red no tiene salida validada a internet (como en RF-78), ENTONCES la revisión termina sin consultar y
  espera a la siguiente, sin reintentos propios.
- RF-1705: CUANDO otra persona inicia sesión en el celular, la revisión trabaja a nombre de quien tenga la sesión en el
  momento en que corre, nunca de la anterior.

### Respaldo de la subida
- RF-1706: CUANDO corre la revisión y en Room hay registros pendientes de subir (los mismos que hoy programan la subida:
  incidencias, movimientos, solicitudes de repuesto, fallas y sugerencias de IA), EL SISTEMA programa la subida de la
  HU-05 (`ProgramadorSincronizacion.subirCambios()`), que se encola detrás de la que esté en curso (RF-67). Así, lo que
  quedó pendiente tras las 3 pasadas de RF-74 sube como máximo en la siguiente revisión, aunque nadie abra la app.
- RF-1707: El respaldo corre para cualquier perfil con sesión (D-3).

### Asignaciones con la app cerrada
- RF-1708: CUANDO corre la revisión y quien tiene la sesión puede atender incidencias (`ATENDER_INCIDENCIA`), EL SISTEMA
  consulta al servidor, no a la caché de Firestore, las incidencias asignadas a esa persona que no están cerradas (D-2).
- RF-1709: EL SISTEMA guarda en Room cada incidencia que llega con las mismas reglas que la escucha en tiempo real:
  descarta el documento mal formado sin frenar al resto (RF-60), no pisa un cambio local sin subir salvo que el remoto
  sea más reciente (RF-62) y, si lo pisa, lo marca como rechazado (RF-79).
- RF-1710: CUANDO una incidencia que llega está asignada a esa persona, no está cerrada y en el celular no estaba, o
  estaba sin asignar o asignada a otra persona, EL SISTEMA muestra el aviso de la HU-07: el mismo título y texto
  (RF-702), canal (RF-706), privacidad con el celular bloqueado (RF-707) y comportamiento al tocarlo (RF-711 a RF-713).
- RF-1711: SI esa incidencia ya estaba asignada a la persona antes de que iniciara sesión en este celular, ENTONCES no
  se avisa, para no llenar la pantalla en la primera descarga (D-1).
- RF-1712: EL SISTEMA avisa de cada asignación una sola vez entre la revisión y la escucha en tiempo real: la que llega
  primero la guarda en Room como asignada, y la otra ya no la ve como nueva. Si la misma incidencia vuelve a avisarse
  (se reasigna y vuelve), reemplaza la notificación anterior (RF-704).
- RF-1713: La escucha en tiempo real usa la misma regla de RF-1710 y RF-1711. Con eso, al abrir la app se avisa de una
  incidencia que se creó y se asignó mientras estaba cerrada, si la revisión no lo hizo antes. Esto reemplaza RF-703 de
  la HU-07; la spec 007 se actualiza en el mismo PR (D-1).
- RF-1714: SI la persona no concedió el permiso de notificaciones o silenció el canal «Asignaciones», ENTONCES la
  revisión igual guarda las incidencias en Room, sin avisar (RF-709).
- RF-1715: SI la consulta falla (la red se cae a mitad, se agota el tiempo o el servidor la rechaza), ENTONCES EL
  SISTEMA registra el error en el log y termina sin cambiar Room; la siguiente revisión vuelve a intentarlo.
- RF-1716: El jefe y el personal de sistemas no hacen la consulta de RF-1708, porque no reciben asignaciones.

### Registro de la revisión
- RF-1717: CUANDO una revisión consulta al servidor sin error, EL SISTEMA anota la fecha y hora en el registro de
  sincronización de Room, con un tipo propio y las 20 más recientes, como RF-65; sirve para comprobar en la prueba
  manual que la revisión corrió con la app cerrada (D-6).

## Requisitos no funcionales
- Costo cero (plan Spark): sin FCM ni Cloud Functions. La consulta de RF-1708 lee como máximo las incidencias activas de
  un técnico (hasta su `limiteAtencion`), y una consulta vacía cuenta como 1 lectura: con 96 revisiones al día son unas
  500 lecturas por técnico, menos del 5 % de las 50 000 diarias gratuitas con los 4 técnicos de prueba.
- Tiempo: con el celular en uso o cargando, el aviso llega a más tardar 15 minutos después de la asignación. En reposo
  (Doze), con ahorro de batería o con las restricciones de algunos fabricantes (Xiaomi, Huawei), Android puede retrasar
  la revisión; la app no lo evita ni pide quitar la optimización de batería (D-4).
- Batería: la revisión solo exige conexión; no mantiene el celular despierto ni abre escuchas que queden vivas. Termina
  en segundos.
- Arquitectura: la revisión vive en `core/sync`, que sigue siendo el único lugar que habla con Firestore; escribe solo en
  Room y el aviso pasa por `core/notificaciones`. Las reglas (cuándo consultar, cuándo avisar, cuándo es la primera
  descarga) son funciones puras en `ReglasSincronizacion.kt`, con prueba JVM.
- Stack cerrado: sin dependencias nuevas; usa WorkManager (2.12 desde #205) con `@HiltWorker`, como
  `SubirCambiosWorker`.
- Sin cambios en `firestore.rules` (cualquier usuario autenticado ya lee `/incidencias`), en el manifiesto (ningún
  permiso nuevo) ni en las entidades de Room (no sube `version`).
- Privacidad: el aviso no muestra datos del caso con el celular bloqueado (RF-707); la revisión no guarda nada fuera de
  Room.
- Plataforma: Android 8.0 (minSdk 26) o superior.

## Casos límite
- El jefe registra y asigna una incidencia nueva mientras la app del técnico está cerrada: la siguiente revisión la baja
  y avisa (RF-1710), aunque el celular nunca la haya visto.
- El técnico inicia sesión en un celular nuevo y ya tiene 3 incidencias asignadas: no se avisa de ninguna (RF-1711);
  las ve en su lista.
- Una incidencia se asigna y se cierra entre dos revisiones: no llega en la consulta (solo trae las no cerradas) y no
  se avisa, como RF-705.
- El jefe reasigna una incidencia de Ana a Luis con las dos apps cerradas: Luis recibe el aviso en su revisión; la de
  Ana ya no la trae, así que en su celular sigue figurando como suya hasta que abra la app y baje el cambio (D-2).
- La revisión avisa y luego la persona abre la app: la escucha baja la misma incidencia, ya asignada en Room, y no
  repite el aviso (RF-1712).
- La sesión está bloqueada con huella: la revisión corre igual y el aviso se ve sin detalles en la pantalla de bloqueo;
  al tocarlo pide desbloquear (RF-712).
- El celular se reinicia: WorkManager vuelve a programar la revisión sin abrir la app.
- La persona usa «Forzar detención» en los ajustes de Android: Android cancela los trabajos programados hasta que se
  vuelve a abrir la app; al abrirla, la revisión se programa de nuevo (RF-1701).
- Modo avión durante horas: no corre ninguna revisión; al volver la red, WorkManager ejecuta la que estaba atrasada.
- Dos asignaciones en la misma revisión: dos avisos, uno por incidencia (cada una con su id).
- La persona negó el permiso de notificaciones: las asignaciones aparecen en su lista al abrir la app, sin aviso.
- Cerrar sesión con registros sin enviar: lo cubre el aviso de la HU-01 (RF-13); la revisión se cancela al cerrar.

## Fuera de alcance
- Bajar con la app cerrada los cambios de los demás que no son asignaciones propias (cierres, otras incidencias,
  movimientos, solicitudes, fallas): siguen llegando por la escucha en tiempo real al abrir la app (D-2).
- Consulta diaria del estado de los repuestos, resumen del día y alerta de casos demorados al jefe: son la HU-18, que
  podrá reutilizar la forma de programar y el canal de avisos de esta HU.
- Avisar al jefe o al personal de sistemas de cualquier evento, y pedirles el permiso de notificaciones: HU-18.
- Pedir quitar la optimización de batería o guiar a la persona por los ajustes del fabricante (D-4).
- Una pantalla o ajuste para activar, desactivar o cambiar la frecuencia de la revisión.
- Notificaciones push con FCM, que exigen un servidor (plan Spark).

## Criterios de finalización
- RF-1701 a RF-1717 implementados, y RF-703 de la spec 007 reemplazado por la referencia a RF-1713.
- Pruebas JVM: la regla de aviso (no estaba, estaba sin asignar, asignada a otro, ya era suya, cerrada, asignada antes
  del inicio de sesión), la regla de quién consulta (técnico, jefe, personal de sistemas, sin sesión), la de cuándo
  programar el respaldo (con y sin pendientes) y la de la red sin validar. Lo que depende de WorkManager, Firestore o
  `NotificationManager` reales se prueba a mano, como en la HU-05 y la HU-07.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- Prueba manual en un celular real con el técnico (`lramirez`) y el jefe (`cmendoza`) en otro celular o emulador:
  - Con la app del técnico cerrada (quitada de recientes), el jefe registra y asigna una incidencia: el aviso llega en
    15 minutos o menos con la pantalla encendida; tocarlo abre el detalle.
  - Lo mismo con la sesión bloqueada: el aviso se ve sin detalles y pide la huella.
  - Reiniciar el celular sin abrir la app y repetir la asignación: el aviso llega.
  - Iniciar sesión en un celular limpio con incidencias ya asignadas: no hay avisos.
  - Registrar una incidencia en modo avión, cerrar la app y quitar el modo avión: el jefe la ve sin que el técnico abra
    la app.
  - Como jefe, comprobar que nunca recibe avisos de asignación.
  - `adb shell cmd jobscheduler run -f` no adelanta la revisión: WorkManager pospone un trabajo periódico hasta que
    pasan sus 15 minutos. Hay que esperar con la pantalla encendida; la última revisión se ve en la tabla
    `sincronizaciones` (tipo `REVISAR_ASIGNACIONES`).
- La línea de la HU-17 en `MEMORY.md` dice «completa».

## Decisiones
Aprobadas por Nina el 2026-11-16, sin cambios.
- D-1 · Aviso de una incidencia que llega por primera vez ya asignada (RF-1710, RF-1711, RF-1713). **Aprobada:** se
  avisa si no estaba en el celular, salvo que su última modificación (`actualizadoEn`) sea anterior al inicio de sesión
  de esa persona en este celular. La regla vale para la revisión y para la escucha en tiempo real, y reemplaza RF-703
  de la HU-07. Sin esto, el caso más común (el jefe registra y asigna con la app del técnico cerrada) nunca avisaría,
  porque el celular no conocía la incidencia, y el criterio de la HU no se cumpliría. Comparar horas de dos celulares
  tiene la misma limitación aceptada en RF-62. Alternativas descartadas: mantener RF-703 (no cumple el criterio) y
  avisar de todo lo que llegue asignado (una avalancha de avisos al iniciar sesión).
- D-2 · Qué baja la revisión (RF-1708). **Aprobada:** solo las incidencias no cerradas asignadas a quien tiene la
  sesión, consultadas al servidor. Es lo que pide el criterio, cuesta pocas lecturas y no depende de la hora de otros
  celulares. Lo demás («descarga los cambios de los demás») llega al abrir la app, como hoy. Alternativa: bajar todas
  las incidencias y movimientos modificados desde la última descarga (`actualizadoEn` mayor que la última hora), que
  deja Room al día también para el jefe, pero puede perder cambios de un celular con la hora atrasada, lee más y
  duplica buena parte de la escucha en tiempo real.
- D-3 · Una sola revisión para todos los perfiles (RF-1701, RF-1706, RF-1707, RF-1716). **Aprobada:** un único
  trabajo periódico por celular (`enqueueUniquePeriodicWork` con `KEEP`) mientras haya sesión; el respaldo de la
  subida corre para todos y la consulta de asignaciones solo para quien puede atender. Así el jefe y el personal de
  sistemas también cierran el hueco de RF-74. Alternativa: programarla solo para técnicos, que deja ese hueco abierto
  para los otros dos perfiles.
- D-4 · Qué significa «en un máximo de 15 minutos» (RNF de tiempo). **Aprobada:** se cumple con el celular en uso o
  cargando; en reposo, Android decide cuándo corre la revisión y la spec lo deja escrito como limitación. Alternativas
  descartadas: alarmas exactas (`SCHEDULE_EXACT_ALARM`, un permiso restringido por Google Play), un servicio en primer
  plano con una notificación fija todo el día (gasta batería y molesta al técnico) y pedir que se quite la optimización
  de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, también restringido por Play).
- D-5 · Dónde vive el código (para el plan). **Aprobada:** en `core/sync`, con ámbito `core`: un worker nuevo
  (`RevisarAsignacionesWorker`, `@HiltWorker`), su programación junto a la subida en `ProgramadorSincronizacion`, la
  regla de D-1 en `ReglasSincronizacion.kt`, y la lógica de guardar una incidencia bajada sacada de
  `SincronizacionTiempoReal` para que la usen la escucha y el worker. Se programa y se cancela desde
  `SincronizacionTiempoReal.iniciar()`, que ya sigue a la sesión, así que no toca `feature/acceso`, los casos de uso de
  sesión ni `SigtecApp`. Archivos compartidos que toca: `SincronizacionTiempoReal.kt`, `ReglasSincronizacion.kt`,
  `ProgramadorSincronizacion.kt` y la spec 007. Alternativa: programarla desde el caso de uso de iniciar sesión y
  cancelarla desde `CerrarSesion`, que toca el modelo de sesión.
- D-6 · Anotar cada revisión (RF-1717). **Aprobada:** en la tabla de sincronización que ya existe, con un tipo nuevo
  (`REVISAR_ASIGNACIONES`), sin cambiar la entidad. Permite comprobar en la prueba manual y en la exposición que la
  revisión corrió con la app cerrada. Alternativa: solo el log, que no se ve sin `adb`.

## Dudas abiertas
Ninguna. Respondidas por Nina el 2026-11-16:
- D-1 a D-6 aprobadas, incluido el cambio de RF-703 de la HU-07.
- D-4 se queda como está: sin alarmas exactas, servicio en primer plano ni permisos restringidos por Google Play.
- El pendiente de `CLAUDE.md` sobre cerrar sesión con registros sin enviar se quita en el PR de cierre de esta HU.
