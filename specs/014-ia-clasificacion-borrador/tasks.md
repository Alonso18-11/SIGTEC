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

## PR-11 — error de la IA (base `main`)
- [ ] **T21 · `ErrorDominio.IANoDisponible` ⚠️.** RF-1423. Hecho cuando: compila.
- [ ] **T22 · Su texto en `MensajesError.kt` ⚠️:** «No se pudo redactar el borrador. Puedes escribir el informe a
  mano.». RF-1423. Hecho cuando: el `when` exhaustivo compila.

## PR-12 — leer la clasificación (base `main`)
- [ ] **T23 · `ClasificacionSugerida`, `leerClasificacion` y `comoContenido`** en `feature/ia/domain/Clasificacion.kt`
  (plan 3.1). RF-1401, RF-1406. Hecho cuando: compila.
- [ ] **T24 · `ClasificacionTest`.** Mismos RF. Hecho cuando: la prueba pasa.

## PR-13 — leer el borrador (base `main`)
- [ ] **T25 · `BorradorInforme`, `EntradaBorrador`, `leerBorrador` y `comoContenido`** (plan 3.2). RF-1417, RF-1419,
  RF-1423. Hecho cuando: compila.
- [ ] **T26 · `BorradorTest`.** Mismos RF. Hecho cuando: la prueba pasa.

## PR-14 — regla de aceptación (base PR-12)
- [ ] **T27 · `clasificacionAceptada`** en `ReglasSugerencia.kt` (plan 3.3). RF-1413. Hecho cuando: compila.
- [ ] **T28 · `ReglasSugerenciaTest`.** RF-1413. Hecho cuando: la prueba pasa.

## PR-15 — contrato del asistente (base PR-11)
- [ ] **T29 · `AsistenteIA`** (`modelo`, `disponible`, `clasificar`, `redactarBorrador`). RF-1401, RF-1415, RF-1417.
  Hecho cuando: compila.
- [ ] **T30 · `AsistenteIAFalso`** con respuestas fijas, fallas y disponibilidad. RF: —
  Hecho cuando: compila con `testDebugUnitTest`.

## PR-16 — contrato del repositorio de sugerencias (base PR-04)
- [ ] **T31 · `SugerenciaIARepository`** (plan P-7). RF-1404, RF-1427. Hecho cuando: compila.
- [ ] **T32 · `SugerenciaIARepositoryFalso`** en memoria, con `fallar`. RF: — Hecho cuando: compila con
  `testDebugUnitTest`.

## PR-17 — repositorio sobre Room (base PR-16, trae PR-05)
- [ ] **T33 · `SugerenciaIARepositoryImpl`** con `@param:IoDispatcher` y el `catch` de la convención (P-9). RF-1427,
  RF-1429. Hecho cuando: compila.
- [ ] **T34 · `IAModule` con el `@Binds` del repositorio.** RF: — Hecho cuando: compila.

## PR-18 — instrucciones para Gemini (base `main`)
- [ ] **T35 · Instrucciones y pedido para clasificar** (plan 4.5). RF-1401. Hecho cuando: compila.
- [ ] **T36 · Instrucciones y pedido para el borrador** (plan 4.5). RF-1417. Hecho cuando: compila.
- [ ] **T37 · `InstruccionesGeminiTest`**: lleva el texto del problema y no lleva datos personales. RNF de privacidad.
  Hecho cuando: la prueba pasa.

## PR-19 — asistente con Gemini (base PR-17, trae PR-15 y PR-18)
- [ ] **T38 · Los dos `GenerativeModel` en `IAModule`** con esquema, `temperature` y límite de tiempo (P-1 a P-4).
  RF-1406, RF-1423. Hecho cuando: compila.
- [ ] **T39 · `AsistenteIAGemini`** (plan 4.6). RF-1401, RF-1406, RF-1417, RF-1423. Hecho cuando: compila.
- [ ] **T40 · `@Binds` del asistente.** RF: — Hecho cuando: compila.
- [ ] **T41 · ⏸ Nina activa Firebase AI Logic** con la exigencia de App Check aplazada (spec, «Configuración en la
  consola», pasos 1 a 3). RF: — Hecho cuando: la consola muestra AI Logic con la Gemini Developer API.

## PR-20 — `SugerirClasificacion` (base PR-16, trae PR-12 y PR-15)
- [ ] **T42 · `SugerirClasificacion`** (plan 4.1). RF-1401, RF-1402, RF-1404 a RF-1407. Hecho cuando: compila.
- [ ] **T43 · `SugerirClasificacionTest`** (plan, sección 7). Mismos RF. Hecho cuando: la prueba pasa.

## PR-21 — `RedactarBorradorInforme` (base PR-16, trae PR-13 y PR-15)
- [ ] **T44 · `RedactarBorradorInforme`** (plan 4.2). RF-1416, RF-1417, RF-1419, RF-1422, RF-1423.
  Hecho cuando: compila.
- [ ] **T45 · `RedactarBorradorInformeTest`.** Mismos RF. Hecho cuando: la prueba pasa.

## PR-22 — `DecidirSugerencia` (base PR-16)
- [ ] **T46 · `DecidirSugerencia`** (plan 4.3). RF-1413, RF-1420, RF-1421, RF-1426. Hecho cuando: compila.
- [ ] **T47 · `DecidirSugerenciaTest`.** Mismos RF. Hecho cuando: la prueba pasa.

## PR-23 — observar la sugerencia y la disponibilidad (base PR-16, trae PR-15)
- [ ] **T48 · `ObservarSugerencia`.** RF-1405, RF-1424. Hecho cuando: compila.
- [ ] **T49 · `ObservarIADisponible`.** RF-1415. Hecho cuando: compila.
- [ ] **T50 · `ObservarSugerenciaTest`.** Mismos RF. Hecho cuando: la prueba pasa.

## PR-24 — texto del botón «Usar» (base `main`)
- [ ] **T51 · `textoUsar = "Usar"` en `TarjetaSugerenciaIA` y vista previa con «Usar borrador» 🔶.** RF-1419.
  Hecho cuando: las vistas previas se ven en claro y oscuro.
