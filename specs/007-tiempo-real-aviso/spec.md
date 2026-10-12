# Spec 007 — Tiempo real y aviso de asignación (HU-07)

> Estado: borrador

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-07 (RF-701…). Corresponde al RF-24 del producto: avisar al técnico con una notificación cuando se le asigna una incidencia. Criterio del curso: 3 (Firebase). La HU se entrega en cuatro PR pequeños; la división, los archivos y los commits están en `plan.md`.

## Contexto y objetivo
El jefe y los técnicos trabajan en celulares distintos y necesitan ver lo mismo al mismo tiempo. Cuando el jefe asigna una incidencia, el técnico debería enterarse en el momento, sin abrir la lista a ver si hay algo nuevo. Y cuando un técnico cierra un caso, el jefe debería verlo sin refrescar nada.

La primera parte ya la resuelve la sincronización de la HU-05: mientras hay sesión, la app escucha Firestore en tiempo real y todas las pantallas leen de Room. Esta HU la verifica y agrega lo que falta: que la propia app, al bajar una asignación nueva para quien tiene la sesión, muestre una notificación local, y que al tocarla se abra el detalle de esa incidencia.

Sin Firebase Cloud Messaging: enviar avisos desde un servidor exige Cloud Functions, que no existen en el plan gratuito (costo cero). La notificación la genera el celular del técnico al detectar el cambio que bajó la sincronización. El aviso con la app cerrada es de la HU-17 (WorkManager).

## Usuarios / actores
- Técnico: recibe el aviso de sus asignaciones nuevas y lo toca para abrir el detalle.
- Jefe de Soporte y Sistemas: ve en tiempo real los cambios de los técnicos (por ejemplo, un cierre). Esta HU no le envía avisos; los avisos al jefe son de la HU-18.
- Personal de sistemas: ve los cambios en tiempo real, como el jefe.
- Android: el sistema de notificaciones y, desde Android 13, el permiso de notificaciones que concede la persona.
- La sincronización de la HU-05 (`core/sync`), que es la que detecta el cambio al bajarlo de Firestore.

## Historias de usuario
- HU-07: Como jefe y técnico, queremos ver los cambios en tiempo real y recibir un aviso apenas se asigna un caso, para trabajar siempre con información actual.

## Requisitos funcionales (criterios de aceptación en EARS)

### Tiempo real (ya cubierto por la HU-05; esta HU lo verifica)
- RF-701: MIENTRAS haya sesión, CUANDO otro miembro del área cambia una incidencia en el servidor (por ejemplo, un técnico la cierra o el jefe la asigna), EL SISTEMA actualiza en segundos la lista, el detalle y la línea de tiempo abiertos en este celular, sin que la persona refresque nada. Lo cubren RF-58 (escucha en tiempo real) y RF-62/RF-63 (reglas al bajar) de la HU-05, junto con las pantallas que leen siempre de Room (HU-04); esta HU solo agrega su prueba.

### Aviso de asignación
- RF-702: CUANDO la sincronización baja el cambio de una incidencia que antes no estaba asignada a quien tiene la sesión y ahora sí, y que no está cerrada, EL SISTEMA muestra una notificación local con el título «Nueva incidencia asignada» y el texto «<código visible> · <equipo> · <descripción>». (RF-24 del producto)
- RF-703: SI una incidencia llega al celular por primera vez ya asignada a quien tiene la sesión (por ejemplo, en la primera descarga después de iniciar sesión, o porque se creó y se asignó mientras la app estaba cerrada), ENTONCES EL SISTEMA no muestra el aviso, para no llenar la pantalla de notificaciones de casos que la persona ya puede ver en su lista. Avisar con la app cerrada es de la HU-17.
- RF-704: SI la misma incidencia genera un segundo aviso (por ejemplo, la reasignan a otro y luego otra vez a la misma persona), ENTONCES EL SISTEMA reemplaza la notificación anterior de esa incidencia en vez de mostrar dos.
- RF-705: EL SISTEMA no avisa de los cambios hechos en este mismo celular que todavía no subieron, de una incidencia cerrada, ni de una incidencia que se reasigna a otra persona.
- RF-706: EL SISTEMA agrupa estos avisos en un canal de notificaciones llamado «Asignaciones», que la persona puede silenciar desde los ajustes de Android sin afectar el resto de la app.
- RF-707: MIENTRAS el celular esté bloqueado, la notificación muestra solo «Nueva incidencia asignada», sin el equipo ni la descripción, para no exponer datos del caso en la pantalla de bloqueo.

### Permiso de notificaciones (Android 13 o superior)
- RF-708: CUANDO un técnico tiene la sesión activa, el celular usa Android 13 o superior y la app no tiene permiso para mostrar notificaciones, EL SISTEMA lo pide con el diálogo del sistema. Lo hace una vez por cada vez que se abre la app y deja de pedirlo cuando Android ya no muestra el diálogo (tras dos rechazos).
- RF-709: SI la persona niega el permiso, ENTONCES la app funciona igual: no muestra avisos, pero la lista y el detalle siguen actualizándose en tiempo real (RF-701).
- RF-710: SI el celular usa Android 8 a 12, ENTONCES EL SISTEMA no pide ningún permiso, porque Android lo concede al instalar. Al jefe y al personal de sistemas no se les pide, porque no reciben asignaciones.

### Abrir el detalle desde el aviso
- RF-711: CUANDO la persona toca la notificación, EL SISTEMA abre la app (o la trae al frente si ya estaba abierta) en el detalle de esa incidencia, y quita la notificación. «Volver» desde el detalle lleva a la pantalla en la que estaba, o a Inicio si la app estaba cerrada.
- RF-712: SI la sesión está bloqueada cuando se toca la notificación, ENTONCES EL SISTEMA muestra primero el desbloqueo (HU-02) y, al desbloquear, abre el detalle.
- RF-713: SI cuando se toca la notificación no hay sesión, ENTONCES EL SISTEMA abre la app en el inicio de sesión y guarda el aviso. Si después entra la misma persona que lo recibió, abre el detalle; si entra otra, lo descarta. SI la sesión ya abierta es de otra persona, ENTONCES abre la app normalmente, sin mostrar el detalle. (Guardar el aviso también cubre el instante en que la app arranca y todavía no sabe si hay sesión.)

## Requisitos no funcionales
- Costo cero (plan Spark): sin Firebase Cloud Messaging ni Cloud Functions; solo notificaciones locales generadas por la app.
- Arquitectura: la detección del cambio vive en `core/sync`, el único lugar que habla con Firestore; las reglas (cuándo avisar, qué texto, adónde llevar el toque, cuándo pedir permiso) son funciones puras con prueba.
- Stack cerrado: sin dependencias nuevas; las notificaciones usan `androidx.core` y el permiso, `activity-compose`, que ya están en el proyecto.
- Privacidad: la notificación no muestra datos del caso en la pantalla de bloqueo (RF-707).
- Plataforma: Android 8.0 (minSdk 26) o superior; el permiso de notificaciones solo existe desde Android 13 (API 33).
- Idioma: textos del aviso y del canal en español.

## Casos límite
- La persona inicia sesión en un celular nuevo y ya tiene 3 incidencias asignadas: llegan por primera vez y no se avisa de ninguna (RF-703); las ve en su lista.
- La app estuvo cerrada mientras le asignaban una incidencia que ya estaba en su celular sin asignar: al abrir la app, la sincronización baja el cambio y se muestra el aviso (RF-702). Cuando exista la HU-17, el aviso hecho con la app cerrada usará el mismo identificador, así que no se duplica (RF-704).
- El jefe reasigna una incidencia de Ana a Luis: Luis recibe el aviso; Ana no recibe ninguno (RF-705).
- La incidencia se asigna y se cierra antes de que el celular del técnico la baje: llega cerrada y no se avisa (RF-705).
- El técnico negó el permiso de notificaciones: no hay avisos, pero la incidencia aparece sola en su lista (RF-709).
- El técnico toca un aviso viejo de una incidencia que ya no es suya: se abre su detalle con la información actual (el detalle muestra quién la tiene ahora).
- Toca el aviso después de que otra persona inició sesión en ese celular: se abre la app sin el detalle (RF-713).
- Toca el aviso después de haber cerrado sesión: se abre el inicio de sesión; si vuelve a entrar la misma persona, llega al detalle (RF-713).
- La app estaba cerrada del todo: arranca, confirma la sesión, pasa por Inicio y abre el detalle; «Volver» lleva a Inicio (RF-711).
- Es el primer ingreso y aparece la oferta de huella (HU-02): el permiso de notificaciones se pide después de responderla, para no mostrar dos diálogos juntos (RF-708).
- El celular está bloqueado con huella: primero el desbloqueo y luego el detalle (RF-712).

## Fuera de alcance
- Avisar con la app cerrada (revisión periódica cada 15 minutos con WorkManager): es la HU-17.
- Avisar al jefe de casos demorados u otros eventos: es la HU-18.
- Avisar al técnico cuando le quitan una incidencia, o a cualquiera cuando cambia la marca de un caso.
- Firebase Cloud Messaging o cualquier aviso enviado desde un servidor.
- Cerrar una incidencia (HU-08): RF-701 se verifica con cualquier cambio que haga otro celular; el cierre real se prueba cuando exista la HU-08.
- Una pantalla propia de ajustes de notificaciones dentro de la app: se usan los de Android (RF-706).

## Criterios de finalización
- RF-701 a RF-713 implementados en los PR B a D, según `plan.md`.
- Pruebas unitarias (JVM): cuándo una bajada es una asignación nueva (asignada a mí antes y ahora, llegada por primera vez, cerrada, reasignada a otro, sin técnico); el contenido del aviso y su versión pública; el identificador estable por incidencia; cuándo pedir el permiso (versión de Android, perfil, ya concedido); adónde lleva el toque (sesión activa, bloqueada, sin sesión, otra persona); y que la lista se actualiza sola cuando cambia Room (RF-701).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde en cada PR.
- Prueba manual en dos dispositivos (jefe y técnico), cuando el equipo la programe: asignar desde el celular del jefe y ver el aviso en el del técnico; tocarlo y llegar al detalle; con la app bloqueada, desbloquear y llegar al detalle; negar el permiso en Android 13+ y ver que la lista igual se actualiza; cambiar una incidencia desde otro celular y ver que la lista del jefe se actualiza sola.

## Dudas abiertas
- [NECESITA ACLARACIÓN] ¿Avisar también al reabrir la app si la asignación ocurrió mientras estaba cerrada (caso límite 2)? La propuesta es sí, porque el cambio se detecta igual al bajar; la alternativa es avisar solo si la app ya estaba abierta en el momento de la asignación, lo que exige distinguir la primera descarga de las siguientes.
- [NECESITA ACLARACIÓN] ¿Pedir el permiso de notificaciones solo a los técnicos (RF-710)? Cuando la HU-18 avise al jefe de casos demorados, habrá que pedírselo también a él.
- [NECESITA ACLARACIÓN] La HU-07 figura a nombre de Alonso en el documento de contexto y de Nina en `MEMORY.md`. Como el PR C toca `core/sync`, conviene acordar quién lo implementa y quién lo revisa.
- [NECESITA ACLARACIÓN] RF-701 con un cierre real necesita la HU-08. Propuesta: verificarlo ahora con cualquier cambio hecho desde otro celular (por ejemplo, una asignación) y repetirlo con el cierre cuando exista la HU-08.
