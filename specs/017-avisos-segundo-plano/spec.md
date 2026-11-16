# Spec 017 — Envío y avisos en segundo plano, con la app cerrada (HU-17)

> Estado: borrador (2026-11-16). Decisiones D-1 a D-6 propuestas, pendientes de aprobación de Nina. El plan y las
> tareas se escriben después de aprobarla.

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
