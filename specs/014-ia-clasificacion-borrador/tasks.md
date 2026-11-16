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

## PR-04 — modelo y entidad de la sugerencia (base `main`)
- [ ] **T07 · `SugerenciaIA` con `modelo`, `generadaEn`, `decididaPorId` y `decididaEn` ⚠️.** RF-1427.
  Hecho cuando: compila.
- [ ] **T08 · `SugerenciaIAEntity` con lo mismo y `actualizadoEn`, y sus `toDomain()`/`toEntity()` ⚠️.** RF-1427.
  Hecho cuando: compila.
- [ ] **T09 · `SigtecDatabase` en `version = 7` ⚠️.** RF: — Hecho cuando: el PR dice que se pierde lo no sincronizado.

## PR-05 — consultas del DAO (base PR-04)
- [ ] **T10 · `observarSinDecidir` (la más reciente por incidencia y tipo) y `contar` ⚠️.** RF-1405, RF-1424.
  Hecho cuando: compila.
- [ ] **T11 · `decidir(id, aceptada, decididaPorId, decididaEn, actualizadoEn)`, sin `responder` ⚠️.** RF-1413,
  RF-1420, RF-1421. Hecho cuando: compila.
- [ ] **T12 · `pendientesDeSincronizar` (solo decididas), `observarCantidadPendientes` y `marcarSincronizada` ⚠️.**
  RF-1429. Hecho cuando: compila.

## PR-06 — `SugerenciaIADaoTest` (base PR-05)
- [ ] **T13 · Sin decidir más reciente y conteo por tipo.** RF-1405, RF-1424.
  Hecho cuando: `./gradlew connectedDebugAndroidTest` pasa en un celular o emulador.
- [ ] **T14 · Decidir, solo las decididas por subir y marcar sincronizada.** RF-1427, RF-1429. Hecho cuando: igual.

## PR-07 — regla de `sugerenciasIA` (base `main`)
- [ ] **T15 · Crear solo con `decididaPorId == uid`, `aceptada is bool` y tipo válido; sin `update` ni `delete` ⚠️.**
  RF-1430. Hecho cuando: el Rules Playground permite el caso bueno y rechaza los cuatro malos (plan, sección 7).
- [ ] **T16 · ⏸ Nina publica la regla en la consola** al fusionarse. RF-1430. Hecho cuando: la regla publicada
  coincide con `firestore.rules`.

## PR-08 — mapeo a Firestore (base PR-04)
- [ ] **T17 · `SugerenciaIAEntity.aFirestore()` en `MapeoFirestore` 🔶** (fechas como `Timestamp`). RF-1429.
  Hecho cuando: compila.

## PR-09 — subir las decididas (base PR-08, trae PR-05)
- [ ] **T18 · Subir con `set` y `marcarSincronizada`; un error permanente no frena al resto 🔶** (plan 4.4). RF-1429.
  Hecho cuando: compila.
- [ ] **T19 · Las sugerencias decididas cuentan en `hayPendientes` 🔶.** RF-1429. Hecho cuando: compila.

## PR-10 — programar la subida (base PR-09)
- [ ] **T20 · Quinto flujo en `vigilarPendientes()` 🔶.** RF-1429.
  Hecho cuando: en el celular, decidir una sugerencia la sube sola (se comprueba con PR-29).
