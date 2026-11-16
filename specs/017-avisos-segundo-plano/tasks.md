# Tareas 017 — Envío y avisos en segundo plano, con la app cerrada (HU-17)

> Salen de `spec.md` y `plan.md`. Cada tarea es un commit chico con su push inmediato. Van agrupadas por PR (plan,
> sección 2) y en el orden de la tabla.
>
> **Al empezar cada PR:** revisar que Alonso no tenga ramas ni PR de la HU-17, crear la rama desde su «Base» al día y
> abrir el PR con la plantilla llena (HU-17, criterios 7 y 8, archivos compartidos).
>
> **Al terminar cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas, el
> CI en verde, todos los commits de demianj123, «Cómo probarlo» escrito y «Ready for review».
>
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

> **Estado (2026-11-16).** PR-A en #219, PR-B en #220 (sobre #219) y PR-C en este PR (sobre #218). T09 revisada en el
> emulador con `cmendoza`: la base de WorkManager tiene `revisar-asignaciones` en cola cada 15 minutos; la cancelación
> al cerrar sesión no se probó para no dejar el emulador sin sesión. Falta T12.

## PR-A: regla de aviso y guardado compartido (base `main`)
- [x] **T01 · `esAsignacionNuevaPara` con `inicioSesion` en `ReglasSincronizacion` 🔶**, con los casos de D-1 en
  `ReglasSincronizacionTest`. RF-1710, RF-1711.
  - Hecho cuando: pasan los casos de la HU-07 y los nuevos.
- [x] **T02 · `InicioSesionLocal`.** D-1.
  - Hecho cuando: compila.
- [x] **T03 · `GuardadoIncidencias` con el `Mutex`.** RF-1709, RF-1712.
  - Hecho cuando: compila.
- [x] **T04 · `SincronizacionTiempoReal` anota el inicio y guarda con `GuardadoIncidencias` 🔶.** RF-1713.
  - Hecho cuando: compila y pasan las pruebas.
- [x] **T05 · RF-703 de la spec 007 apunta a RF-1713.** D-1.
  - Hecho cuando: la spec 007 ya no dice que una incidencia que llega asignada nunca se avisa.

## PR-B: revisión periódica (base PR-A)
- [x] **T06 · `debeConsultarAsignaciones`, `hayPendientesDeSubir`, `MARCAS_ACTIVAS_ASIGNADAS` y `TAREA_REVISAR` 🔶**,
  con sus pruebas; `vigilarPendientes` usa `hayPendientesDeSubir`. RF-1706, RF-1708, RF-1716.
  - Hecho cuando: pasan las pruebas.
- [x] **T07 · `RevisarAsignacionesWorker`.** RF-1703, RF-1704, RF-1706 a RF-1717.
  - Hecho cuando: compila.
- [x] **T08 · `programarRevision()` y `cancelarRevision()` en `ProgramadorSincronizacion` 🔶.** RF-1701, RF-1702.
  - Hecho cuando: compila.
- [x] **T09 · `SincronizacionTiempoReal` programa y cancela según la sesión 🔶.** RF-1701, RF-1702, RF-1705.
  - Hecho cuando: compila, pasan las pruebas y, en el emulador, la base de WorkManager tiene el trabajo periódico con
    sesión y no lo tiene después de cerrar sesión.

## PR-C: cierre (base: la spec, #218)
- [x] **T10 · `CLAUDE.md` sin el pendiente de cerrar sesión ⚠️.**
  - Hecho cuando: la sección «Reglas de dominio» ya no lo lista.
- [x] **T11 · `MEMORY.md`, spec y tareas con el estado de la HU-17.**
  - Hecho cuando: la línea de la HU-17 dice qué falta probar.
- [ ] **T12 · Prueba manual en un celular real** (spec, «Criterios de finalización»).
  - Hecho cuando: todos los casos pasan; la línea de `MEMORY.md` dice «completa».
