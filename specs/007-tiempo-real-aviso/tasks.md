# Tareas 007 — Tiempo real y aviso de asignación (HU-07)

> Salen de `spec.md` y `plan.md`. Cada tarea dura entre 20 y 30 minutos como máximo y depende solo de las anteriores de su PR. Van agrupadas por PR (A7 a D7, plan sección 1) y, dentro de cada uno, de adentro hacia afuera: reglas puras con sus pruebas → infraestructura de Android → raíz → verificación.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/main/res/` (recursos) y `app/src/test/java/com/softcorp/sigtec/` (pruebas). ⚠️ = archivo compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.
> Orden de fusión: A7 → B7 → C7 y D7, en cualquier orden.

## PR A7 — spec, plan y tareas (`docs/hu07-tiempo-real-spec`, desde `main`)

- [x] **T01 · Rama desde `main` al día**
  RF: —
  Hecho cuando: `git branch --show-current` muestra `docs/hu07-tiempo-real-spec` sobre el `main` que ya incluye la HU-06 (#26).

- [x] **T02 · Visto bueno de la spec, el plan y las tareas**
  El usuario revisa la división en PR, las decisiones D-1 a D-10 del plan y las 4 «Dudas abiertas» de la spec; cada cambio va primero a `spec.md`.
  RF: todos
  Hecho cuando: el usuario aprueba o los cambios pedidos ya están escritos, y la spec pasa a «Estado: aprobada».
  > Aprobado por el usuario el 2026-10-11 (D-1 a D-9 y las 4 dudas resueltas en la spec).

- [ ] **T03 · Commits, `MEMORY.md` y PR A7**
  Solo la línea de la HU-07 en `MEMORY.md` (después de `git pull origin main`). Hacer los 4 commits de `plan.md` (PR A7) sin coautoría, con push tras cada uno, y el PR en borrador con revisión de Alonso, marcado «no fusionar hasta T02».
  RF: —
  Hecho cuando: el PR existe, el CI está en verde y, tras T02, Alonso lo aprueba.

## PR B7 — notificación local y permiso (`feat/aviso-notificacion`, desde `main` con A7)

- [ ] **T04 · `contenidoAvisoAsignacion` e `idNotificacion` con su prueba 🔶**
  En `core/notificaciones/ContenidoAviso.kt` (Kotlin puro, plan 3.2 y 3.3), con `ContenidoAvisoTest`: título, texto, versión pública e id estable.
  RF: RF-702, RF-704, RF-707
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ContenidoAvisoTest"` pasa y el archivo no importa `android.*`.

- [ ] **T05 · Ícono de la notificación**
  `res/drawable/ic_aviso_asignacion.xml`: vector monocromo de 24 dp.
  RF: RF-702
  Hecho cuando: compila y lint no da avisos nuevos sobre el recurso.

- [ ] **T06 · `NotificadorAsignaciones` 🔶**
  `@Singleton` con `@ApplicationContext`: canal «Asignaciones» perezoso, comprobación de permiso, `PendingIntent` a `MainActivity` con extras y flags (plan 4.1), visibilidad privada con versión pública.
  RF: RF-702, RF-704, RF-706, RF-707
  Hecho cuando: `./gradlew assembleDebug lintDebug` sin avisos nuevos (en particular, sin `MissingPermission`).

- [ ] **T07 · Permiso en el manifest ⚠️**
  `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` en `AndroidManifest.xml`.
  RF: RF-708
  Hecho cuando: compila y el diff del manifest es solo esa línea.

- [ ] **T08 · `debePedirPermisoNotificaciones` y su prueba 🔶**
  En `core/notificaciones/PermisoNotificaciones.kt` (plan 3.4), con `PermisoNotificacionesTest` (5 casos de la sección 7 del plan).
  RF: RF-708, RF-710
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*PermisoNotificacionesTest"` pasa.

- [ ] **T09 · `PedirPermisoNotificaciones` en la raíz ⚠️**
  Composable de plan 4.4 y una llamada en `SigtecRaiz.kt` con `habilitado = activa && !mostrarOferta`.
  RF: RF-708, RF-709
  Hecho cuando: compila; `SigtecRaiz.kt` solo suma la llamada.

- [ ] **T10 · Verificación y PR B7**
  Comando del CI y los 5 commits de `plan.md` (PR B7).
  RF: RF-702 a RF-710
  Hecho cuando: CI en verde y aprobación de Alonso.

## PR C7 — detectar y avisar (`feat/aviso-deteccion`, desde `main` con B7)

- [ ] **T11 · `esAsignacionNuevaPara` y sus pruebas 🔶**
  En `core/sync/ReglasSincronizacion.kt` (plan 3.1), con los 5 casos de la sección 7 del plan en `ReglasSincronizacionTest`.
  RF: RF-702, RF-703, RF-705
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ReglasSincronizacionTest"` pasa.

- [ ] **T12 · Avisar desde `SincronizacionTiempoReal` 🔶**
  Inyectar `NotificadorAsignaciones`; `guardarIncidencias(snap, uid)` avisa después de guardar si `esAsignacionNuevaPara` (plan 4.2). La regla de conflictos no cambia.
  RF: RF-702
  Hecho cuando: compila y `git diff` no toca `debeReemplazarLocal` ni `atencionAlBajar`.

- [ ] **T13 · Prueba de RF-701 en la lista**
  Caso nuevo en `ListaIncidenciasViewModelTest`: con la lista abierta, el repositorio falso cambia una incidencia a Cerrada y el estado de la lista lo refleja sin llamar a nada.
  RF: RF-701
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ListaIncidenciasViewModelTest"` pasa.

- [ ] **T14 · Verificación y PR C7**
  Comando del CI y los 4 commits de `plan.md` (PR C7).
  RF: RF-701 a RF-705
  Hecho cuando: CI en verde y aprobación de Alonso.

## PR D7 — abrir el detalle (`feat/aviso-abrir-detalle`, desde `main` con B7)

- [ ] **T15 · `destinoDelAviso` y su prueba 🔶**
  `core/navigation/AvisoTocado.kt` (plan 3.5) con `AvisoTocadoTest` (4 casos).
  RF: RF-711 a RF-713
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*AvisoTocadoTest"` pasa.

- [ ] **T16 · Recibir el aviso en `MainActivity` y `RaizViewModel` 🔶**
  `avisoPendiente`, `alTocarAviso` y `alAtenderAviso` en `RaizViewModel`. `MainActivity` lee los extras en `onCreate` (si no se restaura) y en `onNewIntent` (plan 4.3).
  RF: RF-711
  Hecho cuando: compila; `MainActivity` usa el mismo `RaizViewModel` que la raíz.

- [ ] **T17 · Abrir el detalle desde la raíz ⚠️**
  `LaunchedEffect` en `SigtecRaiz.kt` que aplica `destinoDelAviso` cuando la sesión está activa y ya no está en el login (plan 4.3).
  RF: RF-711 a RF-713
  Hecho cuando: compila y la navegación por acceso existente no cambió.

- [ ] **T18 · Verificación y PR D7**
  Comando del CI y los 4 commits de `plan.md` (PR D7).
  RF: RF-711 a RF-713
  Hecho cuando: CI en verde y aprobación de Alonso.

## Pruebas manuales (pendientes hasta que el equipo las programe)

- [ ] **T19 · Aviso en otro dispositivo**
  Con el técnico en un emulador o celular y el jefe en otro: asignar desde el jefe y ver «Nueva incidencia asignada» en el técnico en segundos; con el celular bloqueado, ver solo el título (RF-707); reasignar a otro y comprobar que el primero no recibe aviso (RF-705).
  RF: RF-702 a RF-707

- [ ] **T20 · Permiso en Android 13+**
  Instalación limpia en Android 13+ con un técnico: aparece el diálogo tras la oferta de huella; al negarlo no hay avisos, pero la lista se actualiza sola (RF-709); con el jefe no aparece (RF-710).
  RF: RF-708 a RF-710

- [ ] **T21 · Tocar el aviso**
  Con la app abierta en otra pantalla, cerrada y bloqueada: llega al detalle (tras desbloquear si estaba bloqueada) y «Volver» se comporta como dice RF-711; tras cerrar sesión y entrar otra persona, no abre el detalle (RF-713).
  RF: RF-711 a RF-713

- [ ] **T22 · Tiempo real en la lista del jefe**
  Con la lista abierta en el celular del jefe, cambiar una incidencia desde otro celular (asignarla ahora; cerrarla cuando exista la HU-08) y ver que se actualiza sola.
  RF: RF-701
