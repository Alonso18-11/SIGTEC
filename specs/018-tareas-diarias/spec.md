# Spec 018 — Tareas diarias: repuestos, resumen del día y casos demorados (HU-18)

> Estado: borrador (2026-11-16). Decisiones D-1 a D-10 propuestas, pendientes de aprobación de Nina. El plan y las
> tareas se escriben después de aprobarla.

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-18
> (RF-1801…). Sale de la planificación (`SIGTEC_Planificación.xlsx`, Sprint 5, épica EP-08 «Trabajo automático en
> segundo plano») y de la sección 15 del documento del producto («Consulta del estado de repuestos», «Resumen diario
> del área» y «Alerta de casos demorados»). Criterios del curso: 7 (WorkManager) y 6 (Retrofit). No tiene prototipo:
> no agrega pantallas. Depende de la HU-17 (#219 y #220, sin fusionar): reutiliza su revisión periódica.

## Contexto y objetivo
El jefe necesita saber cómo va el área sin revisar caso por caso, y el técnico que espera un repuesto no debería tener
que preguntar a Logística todos los días. El documento del producto pide tres tareas automáticas: consultar una vez
al día el estado de los repuestos, dejar listo el resumen del día y alertar cada mañana de los casos que llevan más de
cinco días pendientes.

Buena parte ya existe:
- La consulta a Logística está hecha y probada (HU-11, `ActualizarEstadoRepuestos`): baja el estado de las solicitudes
  no entregadas del técnico, guarda los cambios y, con el último repuesto entregado, devuelve la incidencia a
  «En atención» con su movimiento `REPUESTO_RECIBIDO`. Hoy solo corre al abrir «Estado de repuestos» o al deslizar.
- El tablero (HU-15) calcula en el celular, al abrirlo, los reparados de cada día (con la columna de hoy resaltada),
  los pendientes y los críticos.
- «Crítico» ya es una regla única (`esCritica`, HU-15): pendiente hace más de 5 días, o con `critica = true`. La usan
  Inicio, la lista de incidencias y el tablero. Nadie escribe `critica` todavía.
- La HU-17 dejó una revisión cada 15 minutos con la app cerrada, que se programa con la sesión y se cancela al
  cerrarla, y el aviso local de `core/notificaciones`.

Lo que falta es quién dispare cada tarea una vez al día con la app cerrada, y los dos avisos al jefe. Esta HU hace
que la revisión de la HU-17 corra, además, tres tareas diarias a partir de una hora fija, y agrega los avisos «Casos
demorados» y «Resumen del día».

## Usuarios / actores
- Jefe (`VER_INDICADORES`): recibe cada mañana el aviso de casos demorados y al cierre el resumen del día.
- Técnico (`ATENDER_INCIDENCIA`): su celular consulta una vez al día el estado de sus repuestos.
- Personal de sistemas: no corre ninguna tarea diaria ni recibe avisos.
- API de Logística (servidor de prueba en MockAPI, HU-11), por Retrofit.
- `core/sync` y WorkManager: la revisión de la HU-17 es la que corre las tareas.

## Historias de usuario
- HU-18: Como jefe, quiero que la app actualice sola el estado de los repuestos, prepare el resumen del día y me
  alerte de los casos demorados, para tener el control sin revisar caso por caso.
- Criterio de aceptación de la planificación: un repuesto despachado se refleja en su incidencia sin intervención; el
  tablero muestra el resumen del día al abrirlo; las incidencias con más de cinco días pendientes aparecen como caso
  crítico y el jefe recibe una notificación.

## Lo que ya existe y se reutiliza
| Pieza | De | Qué cubre | Qué le falta para la HU-18 |
|---|---|---|---|
| `ActualizarEstadoRepuestos` y el registro `CONSULTAR_REPUESTOS` | HU-11 | Toda la consulta a Logística y sus efectos (RF-1123, RF-1124). | Que corra sola una vez al día. |
| Tablero de indicadores | HU-15 | El resumen del día al abrirlo: reparados por día con hoy resaltado, pendientes y críticos. | Nada; el aviso del resumen lo lleva al jefe. |
| `esCritica` | HU-15 | «Caso crítico» en Inicio, la lista y el tablero. | Nada (D-3). |
| `RevisarAsignacionesWorker` y su programación | HU-17 | Corre cada 15 minutos con la app cerrada mientras haya sesión. | Correr las tareas diarias (D-1). |
| `GuardadoIncidencias` | HU-17 | Guardar en Room lo que baja, con las reglas de conflicto. | Usarlo para refrescar los datos del jefe (D-6). |
| `NotificadorAsignaciones`, `PedirPermisoNotificaciones` | HU-07 | Aviso local con privacidad en la pantalla de bloqueo; permiso solo para el técnico. | Avisos del jefe y su permiso (D-8, D-9). |

## Requisitos funcionales (criterios de aceptación en EARS)

### Cuándo corren las tareas (D-1, D-2)
- RF-1801: CUANDO corre la revisión periódica de la HU-17, EL SISTEMA revisa además si toca alguna tarea diaria. Cada
  tarea tiene una hora desde la que puede correr (D-2) y corre como máximo una vez con éxito por día, según la fecha
  del celular.
- RF-1802: Cada tarea corre solo para el perfil que la necesita: la de repuestos para quien tiene
  `ATENDER_INCIDENCIA`; la de casos demorados y la del resumen, para quien tiene `VER_INDICADORES`. El personal de
  sistemas no corre ninguna. El permiso se consulta con `perfil.puede(...)`.
- RF-1803: CUANDO una tarea termina con éxito, EL SISTEMA lo anota en la tabla de sincronizaciones con su tipo y su
  hora, conservando las 20 más recientes. MIENTRAS no haya una anotación exitosa de ese día, la tarea se vuelve a
  intentar en la siguiente revisión. Una tarea que no se logró en el día no se recupera al día siguiente: la de ese
  día la reemplaza.
- RF-1804: SI no hay sesión o la red no tiene salida validada (RF-1703 y RF-1704 de la HU-17), ENTONCES no corre
  ninguna tarea diaria.
- RF-1805: Una tarea que falla no impide que corran las demás ni la revisión de asignaciones de la HU-17.

### Estado de los repuestos (técnico)
- RF-1806: CUANDO toca la tarea de repuestos, EL SISTEMA ejecuta la misma consulta de «Estado de repuestos» (HU-11,
  RF-1123 y RF-1124): pregunta a Logística por cada solicitud no entregada del técnico, guarda lo que cambió y, al
  entregarse el último repuesto de una incidencia, la deja en «En atención» con su movimiento `REPUESTO_RECIBIDO`. Todo
  queda en Room pendiente de subir, y la subida de la HU-05 lo lleva al servidor, donde lo ve el jefe.
- RF-1807: SI ese día ya hubo una consulta exitosa a Logística (hecha a mano en «Estado de repuestos» o por esta
  tarea), ENTONCES no consulta de nuevo. La consulta automática usa el mismo registro (`CONSULTAR_REPUESTOS`), así
  que «Consultado a Logística hoy a las HH:mm» refleja también la automática.
- RF-1808: SI alguna solicitud no se pudo consultar (Logística no respondió), ENTONCES lo que sí respondió queda
  guardado y la tarea se reintenta en la siguiente revisión (RF-1803).
- RF-1809: La tarea de repuestos no muestra avisos (D-7).

### Datos al día para el jefe (D-6)
- RF-1810: ANTES de contar los casos demorados o armar el resumen, EL SISTEMA baja del servidor todas las incidencias
  y los movimientos registrados desde el inicio del día, y los guarda en Room con las reglas de siempre (RF-60, RF-62 y
  RF-79). Así no cuenta como pendiente una incidencia que otro celular ya cerró mientras la app del jefe estaba
  cerrada.
- RF-1811: SI esa descarga falla, ENTONCES EL SISTEMA no avisa con datos viejos: la tarea queda sin anotar y se
  reintenta en la siguiente revisión.

### Casos demorados (jefe)
- RF-1812: CUANDO toca la tarea de casos demorados, EL SISTEMA cuenta las incidencias pendientes que son críticas con
  la regla de la HU-15 (`esCritica`: más de 5 días pendiente, o `critica = true`), el mismo número que muestran Inicio
  y el tablero.
- RF-1813: SI hay una o más, ENTONCES EL SISTEMA muestra la notificación «Casos demorados» con el texto «N incidencias
  llevan más de 5 días pendientes.» («1 incidencia lleva…» si es una). Reemplaza la del día anterior en vez de
  acumularse.
- RF-1814: SI no hay ninguna, ENTONCES no avisa y quita el aviso del día anterior si sigue a la vista.
- RF-1815: Las incidencias demoradas aparecen como caso crítico donde ya aparecían (Inicio, lista y tablero). La tarea
  no escribe `critica` ni cambia ninguna incidencia (D-3).

### Resumen del día (jefe)
- RF-1816: CUANDO toca la tarea del resumen, EL SISTEMA calcula con Room, ya al día por RF-1810: los equipos reparados
  hoy (incidencias con su movimiento CERRADA en el día local, la misma regla del tablero, RF-1511), las pendientes y
  las críticas.
- RF-1817: EL SISTEMA muestra la notificación «Resumen del día» con el texto «Hoy se repararon N equipos. Quedan P
  pendientes, C críticos.», con singular cuando el número es 1 («Hoy se reparó 1 equipo.»). Se envía aunque no haya
  reparados.
- RF-1818: El resumen no se guarda aparte: el tablero ya lo calcula al abrirlo, con la columna de hoy resaltada
  (RF-1509 y RF-1510 de la HU-15) (D-5).

### Avisos al jefe (D-8, D-9)
- RF-1819: Los dos avisos del jefe van en un canal propio, «Avisos del área», que se puede silenciar en los ajustes de
  Android sin afectar las asignaciones. Usan el mismo mecanismo de `core/notificaciones`.
- RF-1820: MIENTRAS el celular esté bloqueado, los avisos muestran solo su título, sin números.
- RF-1821: CUANDO el jefe toca uno de los avisos, EL SISTEMA abre la app (o la trae al frente) en Inicio, donde ve las
  críticas, y pasa antes por el desbloqueo si la sesión está bloqueada. El aviso se quita al tocarlo.
- RF-1822: EL SISTEMA pide el permiso de notificaciones (Android 13 o superior) también al jefe, con las mismas
  reglas que al técnico (RF-708 a RF-710 de la HU-07). Al personal de sistemas no se le pide.
- RF-1823: SI el jefe no concedió el permiso o silenció el canal, ENTONCES las tareas corren igual (la descarga de
  RF-1810 deja Room al día), sin avisar.

## Requisitos no funcionales
- Costo cero (plan Spark): sin FCM, Cloud Functions ni servidor propio. La descarga del jefe (RF-1810) lee todas las
  incidencias y los movimientos del día, como máximo dos veces al día: con unas 200 incidencias son unas 500 lecturas
  diarias, el 1 % de las 50 000 gratuitas. La consulta a Logística hace una llamada por solicitud no entregada y por
  técnico al día.
- Hora: las tareas corren dentro de la revisión de la HU-17, así que empiezan como mucho 15 minutos después de su hora
  con el celular en uso. En reposo o con restricciones del fabricante pueden correr más tarde ese mismo día (D-4 de la
  HU-17).
- Arquitectura: la lógica de cada tarea vive en el dominio de su módulo (repuestos en `feature/atencion`, demorados y
  resumen en `feature/indicadores`), en Kotlin puro y con prueba; `core/sync` solo las dispara y es el único que habla
  con Firestore (D-10). Los repositorios de `feature/` siguen escribiendo solo en Room.
- Stack cerrado: sin dependencias nuevas.
- Sin cambios en `firestore.rules` (cualquier usuario autenticado ya lee `/incidencias` y `/movimientos`), ni en las
  entidades de Room (no sube `version`), ni permisos nuevos en el manifiesto (`POST_NOTIFICATIONS` ya está).
- Privacidad: los avisos del jefe no muestran números con el celular bloqueado (RF-1820) ni datos de ningún caso.
- Idioma: textos de los avisos y del canal en español, sin «·».
- Pruebas: las reglas (si toca una tarea según la hora, el perfil y la última anotación; los textos con singular y
  plural; el conteo de reparados hoy) son funciones puras con prueba JVM; cada caso de uso nuevo, con dobles de prueba.

## Casos límite
- El técnico abre «Estado de repuestos» a las 7:30 y consulta: a las 8:00 la tarea no vuelve a consultar ese día
  (RF-1807).
- Logística no responde a las 8:00: la tarea se reintenta cada 15 minutos hasta que responda ese día (RF-1808).
- El técnico no tiene solicitudes sin entregar: la tarea termina sin llamar a Logística y se anota como hecha.
- El último repuesto de una incidencia llega: la incidencia vuelve a «En atención» en el celular del técnico, sube con
  la subida de la HU-05 y el jefe la ve al abrir la app o en su siguiente descarga (RF-1806).
- El celular del jefe estuvo apagado hasta las 11:00: el aviso de demorados llega en la primera revisión después de
  encenderlo, ese mismo día.
- El celular del jefe estuvo apagado todo el día: no hay aviso de ese día; al día siguiente llega el de ese día.
- Un técnico cerró ayer una incidencia demorada y el jefe no abrió la app: la descarga de RF-1810 la trae cerrada y no
  se cuenta.
- Cero demorados: no hay aviso, y se quita el de ayer si sigue a la vista (RF-1814).
- El jefe cambia la hora del celular o pasa la medianoche con la app abierta: la fecha que cuenta es la del celular
  al correr la revisión.
- Otra persona entra en el celular: Room se borra con sus anotaciones (D-12 de la HU-05), así que las tareas del día
  corren de nuevo para la nueva sesión, según su perfil.
- El jefe negó el permiso de notificaciones: las tareas corren y Room queda al día, sin avisos (RF-1823).

## Fuera de alcance
- Escribir `critica` en Room o en Firestore (D-3).
- Guardar un resumen diario aparte o mostrar una tarjeta nueva en el tablero (D-5).
- Avisar al técnico cuando cambia el estado de un repuesto (D-7).
- Avisar al personal de sistemas de cualquier cosa.
- Elegir las horas o desactivar las tareas desde la app.
- Abrir desde el aviso una lista filtrada de críticas: abre Inicio (RF-1821), sin tocar la navegación.

## Criterios de finalización
- RF-1801 a RF-1823 implementados.
- Pruebas JVM: cuándo toca una tarea (antes y después de su hora, ya hecha hoy, hecha ayer, falló hoy, cada perfil);
  textos de los dos avisos con 0, 1 y varios; reparados hoy con cierres de hoy, de ayer y sin movimiento; los casos de
  uso de demorados y resumen (sin sesión, sin permiso, camino feliz) y el de repuestos ya existente llamado por la
  tarea. Lo que depende de WorkManager, Firestore o las notificaciones reales se prueba a mano, como en la HU-17.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- Prueba manual en un celular real (con la hora del celular adelantada para no esperar a las 8:00 o a las 18:00):
  - Como técnico con una solicitud registrada, cambiar su estado a «ENTREGADO» en MockAPI y, con la app cerrada,
    comprobar que tras la hora de la tarea la incidencia vuelve a «En atención» y el jefe lo ve.
  - Como jefe, con incidencias de más de 5 días, recibir «Casos demorados» con el número correcto y sin números con el
    celular bloqueado; tocarlo y llegar a Inicio.
  - Como jefe, recibir «Resumen del día» con los reparados de hoy, después de cerrar una incidencia desde otro celular
    con la app del jefe cerrada.
  - Como personal de sistemas, comprobar que no recibe avisos ni se le pide el permiso.
- La línea de la HU-18 en `MEMORY.md` dice «completa».
