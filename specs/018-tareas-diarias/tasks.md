# Tareas 018 — Tareas diarias: repuestos, resumen del día y casos demorados (HU-18)

> Salen de `spec.md` y `plan.md`. Cada tarea es un commit chico con su push inmediato. Van agrupadas por PR (plan,
> sección 2) y en el orden de la tabla.
>
> **Al empezar cada PR:** revisar que Alonso no tenga ramas ni PR de la HU-18, crear la rama desde su «Base» al día y
> abrir el PR con la plantilla llena (HU-18, criterios 6 y 7, archivos compartidos).
>
> **Al terminar cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas, el
> CI en verde, todos los commits de demianj123, «Cómo probarlo» escrito y «Ready for review».
>
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

> **Estado (2026-11-16).** PR-A en #223 (sobre #226, que reemplaza a #220 de la HU-17), PR-B en #224 y PR-C en #225
> (los dos sobre #223), y PR-D en este PR (sobre #222). Falta T16.

## PR-A: núcleo (base #226, antes #220)
- [x] **T01 · `TipoTareaDiaria` y `TareaDiaria` 🔶.** D-2, D-10.
  - Hecho cuando: compila.
- [x] **T02 · `tocaTareaDiaria` en `ReglasSincronizacion` 🔶** con sus casos. RF-1801, RF-1803.
  - Hecho cuando: pasan las pruebas.
- [x] **T03 · `DescargaDatosAlDia` 🔶.** RF-1810.
  - Hecho cuando: compila.
- [x] **T04 · `RevisarAsignacionesWorker` corre las tareas diarias 🔶.** RF-1801 a RF-1805, RF-1811.
  - Hecho cuando: compila y pasan las pruebas.
- [x] **T05 · `NotificadorAvisosArea`.** RF-1819 a RF-1821.
  - Hecho cuando: compila.
- [x] **T06 · Permiso de notificaciones también para el jefe**, con su prueba. RF-1822, RF-1823.
  - Hecho cuando: pasa `PermisoNotificacionesTest`.

## PR-B: repuestos (base PR-A)
- [x] **T07 · `TareaRepuestos` con su prueba.** RF-1806 a RF-1809.
  - Hecho cuando: pasa (técnico sí, jefe no; éxito solo sin solicitudes sin respuesta).
- [x] **T08 · `@IntoSet` en `AtencionModule` ⚠️.**
  - Hecho cuando: compila.
- [x] **T09 · Comentarios de «la HU-18 llamará…».**
  - Hecho cuando: ya no hay comentarios que hablen de la HU-18 en futuro en `feature/atencion`.

## PR-C: avisos del jefe (base PR-A)
- [x] **T10 · `AvisosJefe` con su prueba.** RF-1813, RF-1817, RF-1820.
  - Hecho cuando: pasa con 0, 1 y varios.
- [x] **T11 · `registrarTarea` y `ultimaTareaExitosa` en `IndicadoresRepository`**, su `Impl` y el doble. RF-1803.
  - Hecho cuando: compila.
- [x] **T12 · `CalcularIndicadoresDeHoy` con su prueba.** RF-1812, RF-1816.
  - Hecho cuando: pasa (sin sesión, sin permiso, reparados de hoy).
- [x] **T13 · `TareaCasosDemorados`, `TareaResumenDia` y su `@IntoSet` ⚠️.** RF-1812 a RF-1818.
  - Hecho cuando: compila y pasan las pruebas.
- [x] **T14 · Comentarios de `critica` (D-3) ⚠️.**
  - Hecho cuando: ningún comentario dice que la HU-18 escribe `critica`.

## PR-D: cierre (base: la spec)
- [x] **T15 · Nota de RF-1508 en la spec 015**, `MEMORY.md`, spec y tareas con el estado.
  - Hecho cuando: la línea de la HU-18 dice qué falta probar.
- [ ] **T16 · Prueba manual en un celular real** (spec, «Criterios de finalización»).
  - Hecho cuando: todos los casos pasan; la línea de `MEMORY.md` dice «completa».
