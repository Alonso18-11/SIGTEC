# Tareas 014 — Clasificación, prioridad y borrador del informe con IA (HU-14)

> Salen de `plan.md` (secciones 1 y 2) y de `spec.md` (aprobada el 2026-11-15). Van agrupadas por PR, en el orden de
> la tabla del plan; las tandas de 5 están en la sección 1 del plan. Cada tarea dice cuándo está hecha. Todo PR de
> código termina con `./gradlew assembleDebug testDebugUnitTest` en verde; `lintDebug` corre una vez por tanda.
> Antes de cada tanda: ramas al día con `main`, revisar si Alonso abrió ramas o PR de lo mismo y, antes de marcar
> listo, que todos los commits sean de demianj123. Revisión pedida a Alonso18-11.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` y `app/src/test/java/com/softcorp/sigtec/`.
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista; ⏸ = paso manual de Nina en la consola.

> **Reagrupación (2026-11-16, plan, sección 1).** Ya salieron como PR propios PR-01 (#167), PR-02 (#168), PR-04
> (#169), PR-07 (#170), PR-11 (#171), PR-12 (#182), PR-13 (#183), PR-18 (#184), PR-24 (#185) y PR-25 (#186). Lo que
> falta va en un PR por bloque, con las mismas tareas: **A** = PR-05, PR-06, PR-08 a PR-10 (T10 a T14, T17 a T20);
> **B** = PR-14 a PR-17 y PR-20 a PR-23 (T27 a T34, T42 a T50); **C** = PR-19 (T38 a T41); **D** = PR-26 a PR-30
> (T54 a T66); **E** = PR-31 a PR-38 (T67 a T83); **F** = PR-39 a PR-43 (T84 a T90); **G** = PR-03 (T05 y T06);
> **cierre** = PR-44 (T91 a T95). Las tareas se marcan cuando su PR se fusiona.

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

## PR-25 — `AsignarIncidencia` guarda la clasificación (base `main`)
- [ ] **T52 · `ClasificacionElegida?` en `AsignarIncidencia`** (`null` no cambia nada). RF-1412. Hecho cuando: compila.
- [ ] **T53 · Casos nuevos de `AsignarIncidenciaTest`**: con clasificación, sin ella y con el límite alcanzado.
  RF-1412. Hecho cuando: la prueba pasa.

## PR-26 — el `ViewModel` con la clasificación elegida (base PR-25)
- [ ] **T54 · `tipoElegido`, `prioridadElegida` y sus `al…`**, iniciados una vez con los de la incidencia (plan 4.7).
  RF-1408, RF-1411. Hecho cuando: compila.
- [ ] **T55 · `alAsignar` envía la clasificación.** RF-1412. Hecho cuando: compila.
- [ ] **T56 · Casos nuevos de `AsignacionViewModelTest`.** Mismos RF. Hecho cuando: la prueba pasa.

## PR-27 — sección «Clasificación» (base PR-26)
- [ ] **T57 · `SeccionClasificacion` con dos `PestanasSegmentadas` y la opción nula** (plan 5.1, P-6). RF-1408.
  Hecho cuando: compila.
- [ ] **T58 · Vistas previas con y sin clasificación** en claro y oscuro. RF: — Hecho cuando: se ven.

## PR-28 — pedir y mostrar la sugerencia (base PR-26, trae PR-19, PR-20 y PR-23)
- [ ] **T59 · `EstadoSugerencia` y el pedido en la primera carga** (plan 4.7). RF-1401, RF-1403, RF-1404, RF-1406,
  RF-1407. Hecho cuando: compila.
- [ ] **T60 · La sugerencia a la vista sale de `ObservarSugerencia`.** RF-1405. Hecho cuando: compila.
- [ ] **T61 · Casos nuevos de `AsignacionViewModelTest`**: generando, lista, sin sugerencia, ya clasificada.
  Mismos RF. Hecho cuando: la prueba pasa.

## PR-29 — usar, descartar y decidir (base PR-28, trae PR-14 y PR-22)
- [ ] **T62 · `alUsarSugerencia` y `alDescartarSugerencia`.** RF-1409, RF-1410. Hecho cuando: compila.
- [ ] **T63 · Decidir la sugerencia después de asignar** con `clasificacionAceptada` (plan 4.7, P-8). RF-1413.
  Hecho cuando: compila.
- [ ] **T64 · Casos nuevos de `AsignacionViewModelTest`**: aceptada, corregida, descartada y sin usar. Mismos RF.
  Hecho cuando: la prueba pasa.

## PR-30 — tarjeta «Sugerencia de IA» (base PR-29, trae PR-27)
- [ ] **T65 · La tarjeta generando y lista, sin el comentario «es de la HU-14»** (plan 5.2). RF-1403, RF-1404,
  RF-1409, RF-1410. Hecho cuando: compila.
- [ ] **T66 · Vistas previas de la sugerencia** en claro y oscuro. RF: —
  Hecho cuando: en el celular, como jefe, se ve «Generando sugerencia» y luego la sugerencia; usarla, corregirla y
  descartarla dejan en Firestore la decisión esperada (spec, «Criterios de finalización»).

## PR-31 — `NOTAS` se puede dictar (base `main`)
- [ ] **T67 · `CampoDictado.NOTAS` y la rama en `InformeTecnicoViewModel` (sin efecto todavía).** RF-1414.
  Hecho cuando: compila.
- [ ] **T68 · «Dictar notas» en `tituloDictado`.** RF-1414. Hecho cuando: compila.

## PR-32 — el informe lleva «Mis notas» (base PR-31)
- [ ] **T69 · `notas`, `alCambiarNotas` y el dictado de `NOTAS`.** RF-1414. Hecho cuando: compila.
- [ ] **T70 · Casos nuevos de `InformeTecnicoViewModelTest`**: escribir y dictar notas. RF-1414.
  Hecho cuando: la prueba pasa.

## PR-33 — campo «Mis notas» (base PR-32)
- [ ] **T71 · `CampoTexto` «Mis notas» con micrófono, antes de los campos del informe.** RF-1414.
  Hecho cuando: compila y las vistas previas lo muestran.
- [ ] **T72 · `NOTAS` en `CAMPOS_INFORME`.** RF-1414. Hecho cuando: en el celular, dictar notas las escribe en el campo.

## PR-34 — pedir el borrador (base PR-32, trae PR-19, PR-21 y PR-23)
- [ ] **T73 · `iaDisponible`, `EstadoBorrador`, `puedeRedactar` y `alRedactarBorrador`** (plan 4.8). RF-1415 a
  RF-1419, RF-1423. Hecho cuando: compila.
- [ ] **T74 · El borrador sin decidir se muestra al volver.** RF-1424. Hecho cuando: compila.
- [ ] **T75 · Casos nuevos de `InformeTecnicoViewModelTest`**: redactando, listo, error, sin conexión. Mismos RF.
  Hecho cuando: la prueba pasa.

## PR-35 — usar, descartar y decidir al cerrar (base PR-34, trae PR-22)
- [ ] **T76 · `alUsarBorrador`, `alDescartarBorrador` y `textoDeIA` en el `SavedStateHandle`.** RF-1420 a RF-1422.
  Hecho cuando: compila.
- [ ] **T77 · Un borrador sin decidir queda no aceptado al cerrar.** RF-1425, RF-1426. Hecho cuando: compila.
- [ ] **T78 · Casos nuevos de `InformeTecnicoViewModelTest`**: usar, descartar, otro borrador, cierre, rotación.
  Mismos RF. Hecho cuando: la prueba pasa.

## PR-36 — botón y aviso sin conexión (base PR-34, trae PR-33)
- [ ] **T79 · «Redactar borrador con IA» habilitado según `puedeRedactar`.** RF-1414, RF-1415. Hecho cuando: compila.
- [ ] **T80 · «La IA necesita conexión a internet.» sin red.** RF-1415.
  Hecho cuando: las vistas previas muestran el botón con y sin conexión.

## PR-37 — tarjeta del borrador (base PR-36, trae PR-24 y PR-35)
- [ ] **T81 · Tarjeta redactando y lista con «Usar borrador», y el error con `error.mensaje()`** (plan 5.3).
  RF-1418 a RF-1421, RF-1423. Hecho cuando: compila.
- [ ] **T82 · Vistas previas del borrador** en claro y oscuro. RF: — Hecho cuando: se ven.

## PR-38 — aviso del texto redactado por IA (base PR-37)
- [ ] **T83 · «Texto redactado por IA: revísalo antes de confirmar el cierre.» mientras `textoDeIA`.** RF-1420.
  Hecho cuando: en el celular, como técnico, dictar notas, redactar, usar el borrador, corregir un campo y cerrar con
  huella deja el informe y la sugerencia aceptada en Firestore.

## PR-39 — ordenar por urgencia (base `main`)
- [ ] **T84 · `OrdenIncidencias` y `ordenar`** (plan 3.4). RF-1434, RF-1436. Hecho cuando: compila.
- [ ] **T85 · `OrdenIncidenciasTest`.** Mismos RF. Hecho cuando: la prueba pasa.

## PR-40 — la lista recuerda el orden (base PR-39)
- [ ] **T86 · `orden` en el `SavedStateHandle`, `alOrdenar` y el orden después de los filtros.** RF-1433, RF-1435.
  Hecho cuando: compila.
- [ ] **T87 · Casos nuevos de `ListaIncidenciasViewModelTest`**: con filtros y al limpiar. Mismos RF.
  Hecho cuando: la prueba pasa.

## PR-41 — prioridad en `FilaIncidencia` (base `main`)
- [ ] **T88 · `prioridad: String? = null` y su vista previa 🔶.** RF-1437. Hecho cuando: se ve en claro y oscuro.

## PR-42 — chip «Orden» (base PR-40)
- [ ] **T89 · `FiltroDesplegable("Orden", …)` antes de «Etapa»; «Limpiar» no lo toca** (plan 5.4). RF-1433, RF-1435.
  Hecho cuando: compila y las vistas previas lo muestran.

## PR-43 — prioridad en cada fila (base PR-42, trae PR-41)
- [ ] **T90 · Con «Más urgentes», cada pendiente muestra su prioridad o «Sin prioridad».** RF-1437.
  Hecho cuando: en el celular se cumple el punto del orden de la spec, incluida la rotación.

## PR-44 — cierre (base: la rama del último PR fusionado)
- [ ] **T91 · Comprobar que nada quedó fuera**: cada rama de la HU tiene su PR fusionado. RF: —
  Hecho cuando: en el remoto no queda ninguna rama de la HU.
- [ ] **T92 · Pruebas manuales de punta a punta** («Criterios de finalización» de la spec) en un celular real.
  RF-1401 a RF-1437. Hecho cuando: cada prueba está anotada con su resultado.
- [ ] **T93 · Spec «completa»**, con la fecha y qué se probó. RF: — Hecho cuando: la spec describe el código.
- [ ] **T94 · Tareas marcadas**, con el motivo de las que no se hicieron. RF: — Hecho cuando: no queda ninguna sin
  explicar.
- [ ] **T95 · HU-14 «completa (RF-1401 a RF-1437)» en `MEMORY.md`**, después de `git pull origin main`. RF: —
  Hecho cuando: la línea está actualizada.
