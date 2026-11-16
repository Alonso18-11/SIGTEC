# Tareas 014 — Clasificación, prioridad y borrador del informe con IA (HU-14)

> Salen de `plan.md` (secciones 1 y 2) y de `spec.md` (aprobada el 2026-11-15). Van agrupadas por PR, en el orden de
> la tabla del plan; las tandas de 5 están en la sección 1 del plan. Cada tarea dice cuándo está hecha. Todo PR de
> código termina con `./gradlew assembleDebug testDebugUnitTest` en verde; `lintDebug` corre una vez por tanda.
> Antes de cada tanda: ramas al día con `main`, revisar si Alonso abrió ramas o PR de lo mismo y, antes de marcar
> listo, que todos los commits sean de demianj123. Revisión pedida a Alonso18-11.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` y `app/src/test/java/com/softcorp/sigtec/`.
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista; ⏸ = paso manual de Nina en la consola.

## PR-01 — `feature/ia` y el ámbito `ia` en `CLAUDE.md` (base `main`)
- [ ] **T01 · `ia` en la lista de módulos de `feature/` 🔶** (D-10). RF: — Hecho cuando: `CLAUDE.md` la nombra.
- [ ] **T02 · `ia` en los ámbitos de commit 🔶.** RF: — Hecho cuando: la lista de ámbitos la incluye.

## PR-02 — quitar «Abrir asistente» (base `main`)
- [ ] **T03 · Sin el menú «Más opciones» ni `alAbrirAsistente` en `DetalleIncidenciaScreen`.** RF-1432.
  Hecho cuando: compila y las vistas previas del detalle no muestran el menú.
- [ ] **T04 · Sin la navegación a `Ruta.Asistente` en `IncidenciasNavigation`.** RF-1432.
  Hecho cuando: en el celular, el detalle no ofrece «Abrir asistente».

## PR-03 — borrar `Ruta.Asistente` (base PR-02)
- [ ] **T05 · Quitar `Asistente` de `Rutas.kt` ⚠️.** RF-1432. Hecho cuando: compila sin referencias a la ruta.
- [ ] **T06 · «las 18 rutas» en `CLAUDE.md` 🔶.** RF: — Hecho cuando: el número coincide con `Rutas.kt`.
