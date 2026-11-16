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
