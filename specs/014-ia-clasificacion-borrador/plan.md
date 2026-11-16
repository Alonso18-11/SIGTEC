# Plan 014 — Clasificación, prioridad y borrador del informe con IA (HU-14)

> Basado en `spec.md` (aprobada el 2026-11-15, con D-1 a D-11) y `docs/constitution.md`.
> La HU hace nacer el módulo `feature/ia` (`domain`, `data`, `di`), amplía `asignacion` (clasificación), `atencion`
> (borrador del informe), `incidencias` (orden por urgencia y el menú del detalle) y `captura` (un campo dictable más),
> y toca `core` (modelo y tabla de sugerencias, subida, dos componentes) y `firestore.rules`. Como `main` solo admite
> squash, se entrega en muchos PR de un solo cambio (sección 1): cada uno tiene su rama, commits chicos con push
> inmediato y la HU al final del título, y la aprobación de la otra persona del equipo antes de fusionarse.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas).

## 0. Hallazgos del código real

- **La incidencia ya sabe clasificarse.** `Incidencia.tipoFalla` y `Incidencia.prioridad` existen, `IncidenciaEntity`
  los guarda, `MapeoFirestore` los sube y los baja, y la regla de `/incidencias` deja que el jefe los cambie. **No
  cambia la incidencia, su entidad ni su regla.** `AsignacionScreen` ya muestra la clasificación si existe, con el
  comentario «Sin la sugerencia de IA del prototipo: es de la HU-14».
- **La tabla de sugerencias existe sin uso.** `SugerenciaIAEntity` (`id`, `incidenciaId`, `tipo`, `contenido`,
  `aceptada`, `pendienteSincronizar`) no tiene `actualizadoEn` ni fechas; `SugerenciaIADao` (`porIncidencia`,
  `responder`, `guardar`) no lo usa nadie. `SigtecDatabase` está en `version = 6`. RF-1427 cambia la entidad, así que
  sube a 7 (PR-04).
- **`TipoSugerencia.SOLUCIONES_PARECIDAS` no se usa** (la HU-13 no guarda sus sugerencias, D-1 de la spec 013). La HU
  no lo toca; quitarlo va en la lista de mantenimiento.
- **Regla actual de `sugerenciasIA`:** crear con sesión y actualizar solo `aceptada`. Pasa a «solo crear, decidida y a
  nombre propio» (D-7, PR-07).
- **Subida.** `SincronizadorIncidencias` sube todas las colecciones y decide si quedan pendientes;
  `SincronizacionTiempoReal.vigilarPendientes()` combina cuatro flujos de pendientes. Se suma uno más: `combine` con
  tipos admite hasta cinco (PR-09 y PR-10).
- **`TarjetaSugerenciaIA`** tiene el botón «Usar» con texto fijo; RF-1419 pide «Usar borrador» (PR-24).
- **Informe técnico.** Cuatro campos (`diagnostico`, `trabajoRealizado`, `piezasCambiadas`, `observaciones`), dictado
  con `CampoDictado` y `CAMPOS_INFORME` en `AtencionNavigation`. Hay dos `when` exhaustivos sobre `CampoDictado`
  (`tituloDictado` en captura e `InformeTecnicoViewModel.alRecibirDictado`); `RegistrarFallaViewModel` usa `else`.
  Agregar `NOTAS` (PR-31) obliga a tocar los dos primeros.
- **Detalle.** El menú «Más opciones» tiene un solo ítem, «Abrir asistente», que navega a `Ruta.Asistente`, sin
  destino desde #144. Al quitar el ítem (PR-02) se quita el botón del menú entero. `Ruta.Asistente` se borra después
  (PR-03, ⚠️ `Rutas.kt`).
- **Lista.** El orden lo da el repositorio (más reciente primero, HU-04); los filtros son `FiltroDesplegable`
  privados de `ListaIncidenciasScreen`; `FilaIncidencia` no muestra la prioridad.
- **Conexión.** `core/sync/MonitorConexion` ofrece `hayConexion: Flow<Boolean>` y `hayConexionAhora()`.
- **Sin dependencias nuevas.** `firebase-ai` ya está en `app/build.gradle.kts` (BoM 34.19.0) y
  `kotlinx-serialization-json` también, para leer el JSON de Gemini. App Check se aplaza en la consola (D-9).
- **Errores.** No hay un caso para la IA: se agrega `ErrorDominio.IANoDisponible` (PR-11).
- **Warnings de Kotlin.** En `main`, cada calificador de Hilt sobre una propiedad del constructor
  (`@IoDispatcher private val io`) da el aviso «This annotation is currently applied to the value parameter only».
  Las clases nuevas de esta HU usan `@param:` desde el principio (P-9) para no sumar avisos.

## 1. División en PR, dependencias y orden de fusión

Cada PR es **un solo cambio** (una regla pura con sus pruebas, un contrato, un repositorio, un caso de uso, un paso de
un `ViewModel`, una pieza de pantalla, un ajuste de navegación o de `core`) y compila y pasa
`./gradlew assembleDebug testDebugUnitTest` por sí mismo; `lintDebug` corre una vez por tanda. Son 3 PR de
documentación (la spec en #154, este plan y las tareas) y 44 de código y cierre (PR-01 a PR-44).

**Bases.** Como en la HU-09: si no depende de nada abierto, sale de `main`; si depende de un PR abierto, sale de su
rama y en GitHub su base es esa rama («Base»); si además necesita otros PR abiertos («Trae»), se hace `git merge` de
sus ramas y la descripción lo dice. Cuando una base se fusiona, el PR apilado pasa a `main`, se hace
`git pull origin main` y se revisa `git log main..<rama>`. Antes de cada tanda: ramas al día con `main`, revisar si
Alonso abrió ramas o PR de lo mismo, y antes de marcar listo, que todos los commits sean de demianj123.

| PR | Rama | Título | Base | Trae | RF |
|---|---|---|---|---|---|
| PR-01 | `docs/hu14-modulo-ia` | `docs(ci): módulo feature/ia y ámbito ia en CLAUDE.md (HU-14)` | `main` | — | — (D-10) |
| PR-02 | `fix/hu14-quitar-abrir-asistente` | `fix(incidencias): quitar «Abrir asistente», que llevaba a una ruta sin pantalla (HU-14)` | `main` | — | RF-1432 |
| PR-03 | `refactor/hu14-borrar-ruta-asistente` | `refactor(core): borrar Ruta.Asistente, que ya no se usa (HU-14)` | PR-02 | — | RF-1432 |
| PR-04 | `feat/hu14-modelo-sugerencia` | `feat(core): la sugerencia de IA guarda el modelo, las fechas y quién decide (HU-14)` | `main` | — | RF-1427 |
| PR-05 | `feat/hu14-dao-sugerencias` | `feat(core): consultas de las sugerencias sin decidir y por subir (HU-14)` | PR-04 | — | RF-1405, RF-1424, RF-1429 |
| PR-06 | `test/hu14-dao-sugerencias` | `test(core): SugerenciaIADaoTest con Room en memoria (HU-14)` | PR-05 | — | RF-1427, RF-1429 |
| PR-07 | `feat/hu14-regla-sugerencias` | `feat(firebase): las sugerencias de IA solo se crean, decididas y a nombre propio (HU-14)` | `main` | — | RF-1430 |
| PR-08 | `feat/hu14-mapeo-sugerencia` | `feat(core): mapeo de la sugerencia de IA a Firestore (HU-14)` | PR-04 | — | RF-1429 |
| PR-09 | `feat/hu14-subir-sugerencias` | `feat(core): subir las sugerencias decididas (HU-14)` | PR-08 | PR-05 | RF-1429 |
| PR-10 | `feat/hu14-vigilar-sugerencias` | `feat(core): una sugerencia decidida programa la subida (HU-14)` | PR-09 | — | RF-1429 |
| PR-11 | `feat/hu14-error-ia` | `feat(core): error IANoDisponible y su mensaje (HU-14)` | `main` | — | RF-1423 |
| PR-12 | `feat/hu14-leer-clasificacion` | `feat(ia): leer y validar la clasificación que propone Gemini (HU-14)` | `main` | — | RF-1401, RF-1406 |
| PR-13 | `feat/hu14-leer-borrador` | `feat(ia): leer y validar el borrador del informe (HU-14)` | `main` | — | RF-1417, RF-1419, RF-1423 |
| PR-14 | `feat/hu14-regla-aceptacion` | `feat(ia): cuándo una clasificación cuenta como aceptada (HU-14)` | PR-12 | — | RF-1413 |
| PR-15 | `feat/hu14-contrato-asistente` | `feat(ia): contrato del asistente de IA y su doble (HU-14)` | PR-11 | — | RF-1401, RF-1415, RF-1417 |
| PR-16 | `feat/hu14-contrato-sugerencias` | `feat(ia): contrato del repositorio de sugerencias y su doble (HU-14)` | PR-04 | — | RF-1404, RF-1427 |
| PR-17 | `feat/hu14-repositorio-sugerencias` | `feat(ia): repositorio de sugerencias sobre Room (HU-14)` | PR-16 | PR-05 | RF-1427, RF-1429 |
| PR-18 | `feat/hu14-instrucciones-gemini` | `feat(ia): instrucciones para Gemini sin datos personales (HU-14)` | `main` | — | RF-1401, RF-1417 |
| PR-19 | `feat/hu14-asistente-gemini` | `feat(ia): asistente con Gemini por Firebase AI Logic (HU-14)` | PR-17 | PR-15, PR-18 | RF-1401, RF-1406, RF-1417, RF-1423 |
| PR-20 | `feat/hu14-sugerir-clasificacion` | `feat(ia): SugerirClasificacion, una vez por incidencia (HU-14)` | PR-16 | PR-12, PR-15 | RF-1401, RF-1402, RF-1404 a RF-1407 |
| PR-21 | `feat/hu14-redactar-borrador` | `feat(ia): RedactarBorradorInforme para el técnico asignado (HU-14)` | PR-16 | PR-13, PR-15 | RF-1416, RF-1417, RF-1419, RF-1422, RF-1423 |
| PR-22 | `feat/hu14-decidir-sugerencia` | `feat(ia): decidir una sugerencia con quién y cuándo (HU-14)` | PR-16 | — | RF-1413, RF-1420, RF-1421, RF-1426 |
| PR-23 | `feat/hu14-observar-sugerencia` | `feat(ia): observar la sugerencia sin decidir y si la IA está disponible (HU-14)` | PR-16 | PR-15 | RF-1405, RF-1415, RF-1424 |
| PR-24 | `feat/hu14-tarjeta-texto-usar` | `feat(core): TarjetaSugerenciaIA con el texto del botón «Usar» (HU-14)` | `main` | — | RF-1419 |
| PR-25 | `feat/hu14-asignar-clasificacion` | `feat(asignacion): AsignarIncidencia guarda la clasificación elegida (HU-14)` | `main` | — | RF-1412 |
| PR-26 | `feat/hu14-asignacion-vm-clasificacion` | `feat(asignacion): AsignacionViewModel con el tipo y la prioridad elegidos (HU-14)` | PR-25 | — | RF-1408, RF-1411, RF-1412 |
| PR-27 | `feat/hu14-asignacion-clasificacion` | `feat(asignacion): sección «Clasificación» en Asignar (HU-14)` | PR-26 | — | RF-1408 |
| PR-28 | `feat/hu14-asignacion-vm-sugerencia` | `feat(asignacion): AsignacionViewModel pide y muestra la sugerencia (HU-14)` | PR-26 | PR-19, PR-20, PR-23 | RF-1401, RF-1403 a RF-1407 |
| PR-29 | `feat/hu14-asignacion-vm-decision` | `feat(asignacion): usar, descartar y decidir la sugerencia al asignar (HU-14)` | PR-28 | PR-14, PR-22 | RF-1409, RF-1410, RF-1413 |
| PR-30 | `feat/hu14-asignacion-tarjeta` | `feat(asignacion): tarjeta «Sugerencia de IA» en Asignar (HU-14)` | PR-29 | PR-27 | RF-1403, RF-1404, RF-1409, RF-1410 |
| PR-31 | `feat/hu14-dictar-notas` | `feat(captura): «Mis notas» se puede dictar (HU-14)` | `main` | — | RF-1414 |
| PR-32 | `feat/hu14-informe-vm-notas` | `feat(atencion): InformeTecnicoViewModel con «Mis notas» (HU-14)` | PR-31 | — | RF-1414 |
| PR-33 | `feat/hu14-informe-notas` | `feat(atencion): campo «Mis notas» con micrófono en el informe (HU-14)` | PR-32 | — | RF-1414 |
| PR-34 | `feat/hu14-informe-vm-redactar` | `feat(atencion): InformeTecnicoViewModel pide el borrador (HU-14)` | PR-32 | PR-19, PR-21, PR-23 | RF-1415 a RF-1419, RF-1423, RF-1424 |
| PR-35 | `feat/hu14-informe-vm-decision` | `feat(atencion): usar o descartar el borrador, y decidirlo al cerrar (HU-14)` | PR-34 | PR-22 | RF-1420 a RF-1422, RF-1425, RF-1426 |
| PR-36 | `feat/hu14-informe-boton-borrador` | `feat(atencion): botón «Redactar borrador con IA» y aviso sin conexión (HU-14)` | PR-34 | PR-33 | RF-1414, RF-1415 |
| PR-37 | `feat/hu14-informe-tarjeta-borrador` | `feat(atencion): tarjeta «Borrador generado automáticamente» (HU-14)` | PR-36 | PR-24, PR-35 | RF-1418 a RF-1421, RF-1423 |
| PR-38 | `feat/hu14-informe-aviso-ia` | `feat(atencion): aviso «Texto redactado por IA» hasta cerrar (HU-14)` | PR-37 | — | RF-1420 |
| PR-39 | `feat/hu14-ordenar-urgencia` | `feat(incidencias): ordenar las incidencias por urgencia (HU-14)` | `main` | — | RF-1434, RF-1436 |
| PR-40 | `feat/hu14-lista-vm-orden` | `feat(incidencias): ListaIncidenciasViewModel con el orden elegido (HU-14)` | PR-39 | — | RF-1433, RF-1435 |
| PR-41 | `feat/hu14-fila-prioridad` | `feat(core): FilaIncidencia puede mostrar la prioridad (HU-14)` | `main` | — | RF-1437 |
| PR-42 | `feat/hu14-lista-chip-orden` | `feat(incidencias): chip «Orden» en Incidencias (HU-14)` | PR-40 | — | RF-1433, RF-1435 |
| PR-43 | `feat/hu14-lista-prioridad` | `feat(incidencias): la prioridad en cada fila al ordenar por urgencia (HU-14)` | PR-42 | PR-41 | RF-1437 |
| PR-44 | `docs/hu14-cierre` | `docs(ia): cierre de la HU-14, completa y probada (HU-14)` | el último fusionado | — | — |

**Orden por tandas de 5** (alternando con las tandas de mantenimiento). Se eligen primero los que salen de `main`;
si la base de un PR ya se fusionó, sale de `main`.

| Tanda | PR |
|---|---|
| 1 (documentación) | spec (#154), este plan, las tareas |
| 2 | PR-01, PR-02, PR-04, PR-07, PR-11 |
| 3 | PR-12, PR-13, PR-18, PR-24, PR-25 |
| 4 | PR-03, PR-05, PR-08, PR-16, PR-31 |
| 5 | PR-06, PR-09, PR-10, PR-14, PR-15 |
| 6 | PR-17, PR-19, PR-20, PR-21, PR-22 |
| 7 | PR-23, PR-26, PR-27, PR-28, PR-29 |
| 8 | PR-30, PR-32, PR-33, PR-34, PR-35 |
| 9 | PR-36, PR-37, PR-38, PR-39, PR-40 |
| 10 | PR-41, PR-42, PR-43, PR-44 |

**Pasos manuales de Nina** (la spec, «Configuración en la consola»): publicar la regla de `sugerenciasIA` cuando se
fusione PR-07, y activar Firebase AI Logic con la exigencia de App Check aplazada antes de probar PR-19 en el celular.

## 2. Archivos y commits por PR

Leyenda: 🆕 se crea · ✏️ se modifica · ⚠️ compartido de la lista de `CLAUDE.md` (aprobado con la spec) · 🔶 sensible
fuera de esa lista · sin marca: módulo de la HU. Cada commit lleva `(HU-14)` al final y va seguido de su push; las
pruebas van en commits `test(...)` propios.

### Proceso y asistente (PR-01 a PR-03)
| PR | Archivos | Commits |
|---|---|---|
| PR-01 | ✏️🔶 `CLAUDE.md`: `ia` en la lista de módulos de `feature/` y en los ámbitos de commit (D-10) | 1. `docs(ci): módulo feature/ia en la estructura` · 2. `docs(ci): ámbito ia en los commits` |
| PR-02 | ✏️ `feature/incidencias/presentation/DetalleIncidenciaScreen.kt` (sin el menú «Más opciones» ni `alAbrirAsistente`); ✏️ `IncidenciasNavigation.kt` (sin la navegación a `Ruta.Asistente`) | 1. `fix(incidencias): quitar «Abrir asistente» del detalle` · 2. `fix(incidencias): sin navegar a Ruta.Asistente` |
| PR-03 | ✏️⚠️ `core/navigation/Rutas.kt` (sin `Asistente`); ✏️🔶 `CLAUDE.md` («las 18 rutas») | 1. `refactor(core): borrar Ruta.Asistente` · 2. `docs(ci): CLAUDE.md con 18 rutas` |

### Room y Firestore (PR-04 a PR-10)
| PR | Archivos | Commits |
|---|---|---|
| PR-04 | ✏️⚠️ `core/domain/model/TipoSugerencia.kt` (`SugerenciaIA` con `modelo`, `generadaEn`, `decididaPorId`, `decididaEn`); ✏️⚠️ `core/database/entity/SugerenciaIAEntity.kt` (lo mismo y `actualizadoEn`); ✏️⚠️ `SigtecDatabase.kt` (`version = 7`) | 1. `feat(core): SugerenciaIA con modelo, fechas y quién decide` · 2. `feat(core): SugerenciaIAEntity con los campos nuevos y actualizadoEn` · 3. `feat(core): base de datos en la versión 7` |
| PR-05 | ✏️⚠️ `core/database/dao/SugerenciaIADao.kt` (`observarSinDecidir`, `contar`, `decidir`, `pendientesDeSincronizar`, `observarCantidadPendientes`, `marcarSincronizada`; sin `responder`) | 1. `feat(core): sugerencia sin decidir por incidencia y tipo` · 2. `feat(core): decidir una sugerencia` · 3. `feat(core): sugerencias decididas por subir` |
| PR-06 | 🆕 (androidTest) `SugerenciaIADaoTest.kt` | 1. `test(core): la última sugerencia sin decidir y el conteo por tipo` · 2. `test(core): solo las decididas quedan por subir` |
| PR-07 | ✏️⚠️ `firebase/firestore.rules` (`sugerenciasIA`: crear con `decididaPorId == uid`, `aceptada is bool` y tipo válido; sin `update` ni `delete`) | 1. `feat(firebase): sugerencias de IA solo se crean, decididas y a nombre propio` |
| PR-08 | ✏️🔶 `core/sync/MapeoFirestore.kt` (`SugerenciaIAEntity.aFirestore()`) | 1. `feat(core): sugerencia de IA a Firestore` |
| PR-09 | ✏️🔶 `core/sync/SincronizadorIncidencias.kt` (subir con `set` y `marcarSincronizada`; contarlas en `hayPendientes`) | 1. `feat(core): subir las sugerencias decididas` · 2. `feat(core): las sugerencias cuentan como pendientes de subir` |
| PR-10 | ✏️🔶 `core/sync/SincronizacionTiempoReal.kt` (quinto flujo en `vigilarPendientes`) | 1. `feat(core): una sugerencia decidida programa la subida` |

### Dominio y datos de la IA (PR-11 a PR-23)
| PR | Archivos | Commits |
|---|---|---|
| PR-11 | ✏️⚠️ `core/domain/Resultado.kt` (`IANoDisponible`); ✏️⚠️ `core/ui/MensajesError.kt` | 1. `feat(core): error IANoDisponible` · 2. `feat(core): mensaje de IANoDisponible` |
| PR-12 | 🆕 `feature/ia/domain/Clasificacion.kt` (`ClasificacionSugerida`, `leerClasificacion`, `comoContenido`, 3.1); 🆕 (test) `ClasificacionTest.kt` | 1. `feat(ia): leer la clasificación que propone Gemini` · 2. `test(ia): clasificación válida, valores fuera de los permitidos y texto suelto` |
| PR-13 | 🆕 `feature/ia/domain/Borrador.kt` (`BorradorInforme`, `EntradaBorrador`, `leerBorrador`, `comoContenido`, 3.2); 🆕 (test) `BorradorTest.kt` | 1. `feat(ia): leer el borrador del informe` · 2. `test(ia): borrador completo, sin diagnóstico, recortado y texto suelto` |
| PR-14 | 🆕 `feature/ia/domain/ReglasSugerencia.kt` (`clasificacionAceptada`, 3.3); 🆕 (test) `ReglasSugerenciaTest.kt` | 1. `feat(ia): cuándo una clasificación cuenta como aceptada` · 2. `test(ia): usada sin cambios, corregida, descartada y sin usar` |
| PR-15 | 🆕 `feature/ia/domain/AsistenteIA.kt` (interfaz: `modelo`, `disponible`, `clasificar`, `redactarBorrador`); 🆕 (test) `feature/ia/data/AsistenteIAFalso.kt` | 1. `feat(ia): contrato del asistente de IA` · 2. `test(ia): asistente falso con respuestas y fallas` |
| PR-16 | 🆕 `feature/ia/domain/SugerenciaIARepository.kt`; 🆕 (test) `feature/ia/data/SugerenciaIARepositoryFalso.kt` | 1. `feat(ia): contrato del repositorio de sugerencias` · 2. `test(ia): repositorio de sugerencias en memoria` |
| PR-17 | 🆕 `feature/ia/data/SugerenciaIARepositoryImpl.kt` (`@param:IoDispatcher`, `catch` de la convención); 🆕 `feature/ia/di/IAModule.kt` (`@Binds`) | 1. `feat(ia): repositorio de sugerencias sobre Room` · 2. `feat(ia): módulo de Hilt de la IA` |
| PR-18 | 🆕 `feature/ia/data/InstruccionesGemini.kt` (instrucciones de sistema y armado del pedido, 4.5); 🆕 (test) `InstruccionesGeminiTest.kt` | 1. `feat(ia): instrucciones para clasificar` · 2. `feat(ia): instrucciones para redactar el borrador` · 3. `test(ia): el pedido lleva el texto del problema y nada más` |
| PR-19 | 🆕 `feature/ia/data/AsistenteIAGemini.kt` (4.6); ✏️ `IAModule.kt` (`@Binds` y los dos modelos con su esquema, P-2) | 1. `feat(ia): modelos de Gemini con su esquema y su límite de tiempo` · 2. `feat(ia): asistente con Gemini` · 3. `feat(ia): el asistente en el módulo de Hilt` |
| PR-20 | 🆕 `feature/ia/domain/SugerirClasificacion.kt` (4.1); 🆕 (test) `SugerirClasificacionTest.kt` | 1. `feat(ia): SugerirClasificacion` · 2. `test(ia): sin permiso, ya clasificada, ya sugerida, falla y camino feliz` |
| PR-21 | 🆕 `feature/ia/domain/RedactarBorradorInforme.kt` (4.2); 🆕 (test) `RedactarBorradorInformeTest.kt` | 1. `feat(ia): RedactarBorradorInforme` · 2. `test(ia): otro técnico, cerrada, sin notas, falla, anterior sin decidir y camino feliz` |
| PR-22 | 🆕 `feature/ia/domain/DecidirSugerencia.kt` (4.3); 🆕 (test) `DecidirSugerenciaTest.kt` | 1. `feat(ia): DecidirSugerencia` · 2. `test(ia): decide con quién y cuándo, y respeta el permiso de cada tipo` |
| PR-23 | 🆕 `feature/ia/domain/ObservarSugerencia.kt`; 🆕 `feature/ia/domain/ObservarIADisponible.kt`; 🆕 (test) `ObservarSugerenciaTest.kt` | 1. `feat(ia): observar la sugerencia sin decidir` · 2. `feat(ia): observar si la IA está disponible` · 3. `test(ia): observar sin decidir y disponibilidad` |

### Componentes (PR-24)
| PR | Archivos | Commits |
|---|---|---|
| PR-24 | ✏️🔶 `core/ui/components/TarjetaSugerenciaIA.kt` (`textoUsar = "Usar"` y vista previa con «Usar borrador») | 1. `feat(core): TarjetaSugerenciaIA con el texto del botón Usar` |

### Asignar (PR-25 a PR-30)
| PR | Archivos | Commits |
|---|---|---|
| PR-25 | ✏️ `feature/asignacion/domain/AsignarIncidencia.kt` (`ClasificacionElegida?`, null = no cambia); ✏️ (test) `AsignarIncidenciaTest.kt` | 1. `feat(asignacion): AsignarIncidencia guarda la clasificación elegida` · 2. `test(asignacion): con clasificación, sin ella y si el límite lo impide` |
| PR-26 | ✏️ `AsignacionViewModel.kt` (`tipoElegido`, `prioridadElegida`, `alElegirTipo`, `alElegirPrioridad`); ✏️ (test) `AsignacionViewModelTest.kt` | 1. `feat(asignacion): el ViewModel lleva la clasificación elegida` · 2. `test(asignacion): empieza con la de la incidencia y se envía al asignar` |
| PR-27 | ✏️ `AsignacionScreen.kt` (`SeccionClasificacion` con dos `PestanasSegmentadas`, 5.1; vistas previas) | 1. `feat(asignacion): sección Clasificación` · 2. `feat(asignacion): vistas previas con y sin clasificación` |
| PR-28 | ✏️ `AsignacionViewModel.kt` (`EstadoSugerencia`, pedirla una vez, 4.7); ✏️ (test) | 1. `feat(asignacion): el ViewModel pide la sugerencia al abrir` · 2. `test(asignacion): generando, lista, sin sugerencia y ya clasificada` |
| PR-29 | ✏️ `AsignacionViewModel.kt` (`alUsarSugerencia`, `alDescartarSugerencia`, decisión al asignar); ✏️ (test) | 1. `feat(asignacion): usar y descartar la sugerencia` · 2. `feat(asignacion): decidir la sugerencia al asignar` · 3. `test(asignacion): aceptada, corregida, descartada y sin usar` |
| PR-30 | ✏️ `AsignacionScreen.kt` (tarjeta generando y lista, 5.2; sin el comentario «es de la HU-14»; vistas previas) | 1. `feat(asignacion): tarjeta Sugerencia de IA` · 2. `feat(asignacion): vistas previas de la sugerencia` |

### Informe técnico (PR-31 a PR-38)
| PR | Archivos | Commits |
|---|---|---|
| PR-31 | ✏️ `feature/captura/domain/TextoDictado.kt` (`CampoDictado.NOTAS`); ✏️ `MensajesDictado.kt` («Dictar notas»); ✏️ `InformeTecnicoViewModel.kt` (rama `NOTAS` sin efecto todavía) | 1. `feat(captura): campo dictable NOTAS` · 2. `feat(captura): título Dictar notas` |
| PR-32 | ✏️ `InformeTecnicoViewModel.kt` (`notas`, `alCambiarNotas`, dictado `NOTAS`); ✏️ (test) `InformeTecnicoViewModelTest.kt` | 1. `feat(atencion): el informe lleva Mis notas` · 2. `test(atencion): escribir y dictar notas` |
| PR-33 | ✏️ `InformeTecnicoScreen.kt` (`CampoTexto` «Mis notas» con micrófono); ✏️ `AtencionNavigation.kt` (`NOTAS` en `CAMPOS_INFORME`) | 1. `feat(atencion): campo Mis notas` · 2. `feat(atencion): dictar Mis notas` |
| PR-34 | ✏️ `InformeTecnicoViewModel.kt` (`iaDisponible`, `EstadoBorrador`, `alRedactarBorrador`, 4.8); ✏️ (test) | 1. `feat(atencion): pedir el borrador` · 2. `feat(atencion): mostrar el borrador sin decidir al volver` · 3. `test(atencion): redactando, listo, falla y sin conexión` |
| PR-35 | ✏️ `InformeTecnicoViewModel.kt` (`alUsarBorrador`, `alDescartarBorrador`, `textoDeIA`, decisión al cerrar); ✏️ (test) | 1. `feat(atencion): usar y descartar el borrador` · 2. `feat(atencion): un borrador sin decidir queda no aceptado al cerrar` · 3. `test(atencion): usar, descartar, otro borrador y cierre` |
| PR-36 | ✏️ `InformeTecnicoScreen.kt` (botón «Redactar borrador con IA» y «La IA necesita conexión a internet.») | 1. `feat(atencion): botón Redactar borrador con IA` · 2. `feat(atencion): aviso de la IA sin conexión` |
| PR-37 | ✏️ `InformeTecnicoScreen.kt` (tarjeta redactando y lista, error, 5.3; vistas previas) | 1. `feat(atencion): tarjeta del borrador` · 2. `feat(atencion): vistas previas del borrador` |
| PR-38 | ✏️ `InformeTecnicoScreen.kt` (aviso «Texto redactado por IA…») | 1. `feat(atencion): aviso del texto redactado por IA` |

### Orden por urgencia (PR-39 a PR-43)
| PR | Archivos | Commits |
|---|---|---|
| PR-39 | 🆕 `feature/incidencias/domain/OrdenIncidencias.kt` (`OrdenIncidencias`, `ordenar`, 3.4); 🆕 (test) `OrdenIncidenciasTest.kt` | 1. `feat(incidencias): ordenar por urgencia` · 2. `test(incidencias): prioridades, sin prioridad, empates y solucionadas` |
| PR-40 | ✏️ `ListaIncidenciasViewModel.kt` (`orden` en `SavedStateHandle`, `alOrdenar`); ✏️ (test) | 1. `feat(incidencias): la lista recuerda el orden elegido` · 2. `test(incidencias): orden con filtros y al limpiar` |
| PR-41 | ✏️🔶 `core/ui/components/FilaIncidencia.kt` (`prioridad: String? = null`; vista previa) | 1. `feat(core): FilaIncidencia con la prioridad` |
| PR-42 | ✏️ `ListaIncidenciasScreen.kt` (chip «Orden», 5.4) | 1. `feat(incidencias): chip Orden en Incidencias` |
| PR-43 | ✏️ `ListaIncidenciasScreen.kt` (prioridad de cada pendiente con «Más urgentes») | 1. `feat(incidencias): la prioridad en la fila al ordenar por urgencia` |

### Cierre (PR-44)
| PR | Archivos | Commits |
|---|---|---|
| PR-44 | ✏️ `spec.md` (estado «completa» y lo que se probó); ✏️ `tasks.md`; ✏️ `MEMORY.md` (línea de la HU-14) | 1. `docs(ia): spec completa` · 2. `docs(ia): tareas marcadas` · 3. `docs(ia): HU-14 completa en MEMORY` |

## 3. Funciones puras de lógica

Kotlin puro, sin `android.*` ni Firebase, con pruebas JVM.

- **3.1 `leerClasificacion(json: String): ClasificacionSugerida?`** (PR-12). Lee `{"tipo": …, "prioridad": …}` con
  `kotlinx.serialization` (`Json { ignoreUnknownKeys = true }`). Devuelve `null` si no es JSON, si falta un campo o si
  el valor no es uno de `TipoFalla` o `Prioridad` (sin importar mayúsculas). `comoContenido()` escribe el JSON
  canónico que se guarda en `SugerenciaIA.contenido`, y `leerClasificacion` también lo lee de vuelta.
- **3.2 `leerBorrador(json: String): BorradorInforme?`** (PR-13). Lee los cuatro campos; recorta espacios y deja cada
  campo en `MAX_CARACTERES_CAMPO = 1000`. Devuelve `null` si no es JSON o si el diagnóstico o el trabajo realizado
  quedan vacíos (RF-1419, RF-1423); las piezas y las observaciones pueden venir vacías.
- **3.3 `clasificacionAceptada(sugerida, usada, descartada, tipoAsignado, prioridadAsignada): Boolean`** (PR-14).
  `usada && !descartada && tipoAsignado == sugerida.tipo && prioridadAsignada == sugerida.prioridad` (D-4, RF-1413).
- **3.4 `List<Incidencia>.ordenar(orden: OrdenIncidencias): List<Incidencia>`** (PR-39). `RECIENTES` devuelve la lista
  como llega (el orden de la HU-04). `URGENTES`: primero las pendientes, por `prioridad` (ALTA, MEDIA, BAJA, `null`) y
  luego `fechaRegistro` ascendente; después las solucionadas, por `fechaRegistro` descendente (RF-1434). Solo mira
  `Incidencia.prioridad`, nunca una sugerencia (RF-1436).

## 4. Algoritmos en pseudocódigo

### 4.1 `SugerirClasificacion(incidencia, ahora): Resultado<SugerenciaIA?>` (RF-1401, RF-1402, RF-1404 a RF-1407)
```
VerificarPermiso(ASIGNAR_INCIDENCIA) → Error se propaga (RF-1402)
si incidencia cerrada, o tipoFalla != null o prioridad != null → Exito(null)   (RF-1407)
repo.contar(incidencia.id, CLASIFICACION) > 0 → Exito(null)                       (RF-1405: una consulta por celular)
asistente.clasificar(incidencia.descripcion) → Error(e) → Error(e)               (el ViewModel lo calla, RF-1406)
leerClasificacion(texto) == null → Error(IANoDisponible)                          (RF-1406)
sugerencia = SugerenciaIA(UUID, incidencia.id, CLASIFICACION, clasificacion.comoContenido(), asistente.modelo, ahora)
repo.guardar(sugerencia) → Error se propaga; Exito(sugerencia)                    (RF-1404)
```

### 4.2 `RedactarBorradorInforme(incidencia, entrada, ahora): Resultado<SugerenciaIA>` (RF-1416, RF-1417, RF-1419, RF-1422, RF-1423)
```
usuario = VerificarPermiso(ATENDER_INCIDENCIA) → Error se propaga
si incidencia.tecnicoAsignadoId != usuario.id o está cerrada → Error(SinPermiso)  (RF-1416)
si entrada.notas está en blanco → Error(CampoObligatorio)                         (RF-1415, en el dominio también)
anterior = repo.sinDecidir(incidencia.id, BORRADOR_INFORME)
asistente.redactarBorrador(entrada) → Error(e) → Error(e)                         (RF-1423)
borrador = leerBorrador(texto) ?: return Error(IANoDisponible)
si anterior != null → repo.decidir(anterior.id, false, usuario.id, ahora)        (RF-1422)
guardar SugerenciaIA(UUID, incidencia.id, BORRADOR_INFORME, borrador.comoContenido(), asistente.modelo, ahora)
```
El anterior se decide después de tener el nuevo: si Gemini falla, el anterior sigue a la vista.

### 4.3 `DecidirSugerencia(sugerencia, aceptada, ahora): Resultado<Unit>` (RF-1413, RF-1420, RF-1421, RF-1426)
```
permiso = CLASIFICACION → ASIGNAR_INCIDENCIA; BORRADOR_INFORME → ATENDER_INCIDENCIA
usuario = VerificarPermiso(permiso) → Error se propaga
si sugerencia.aceptada != null → Exito(Unit)              (ya decidida: no se cambia, como en Firestore)
repo.decidir(sugerencia.id, aceptada, usuario.id, ahora) → pendienteSincronizar = true (RF-1429)
```

### 4.4 Subir las sugerencias (RF-1429, PR-08 a PR-10)
`SincronizadorIncidencias`, después de los movimientos: por cada `sugerenciaIADao.pendientesDeSincronizar()` (solo
`aceptada IS NOT NULL`), `sugerenciasIA.document(id).set(aFirestore())` y `marcarSincronizada(id)`; un error
permanente queda en Logcat sin frenar al resto, como las fallas. `vigilarPendientes()` suma
`observarCantidadPendientes()`. Sin descarga (D-7).

### 4.5 Instrucciones para Gemini (PR-18)
- Clasificar: «Eres el asistente del área de Soporte y Sistemas. Clasifica la falla en HARDWARE, SOFTWARE o RED y da
  una prioridad BAJA, MEDIA o ALTA. ALTA: el usuario no puede trabajar o afecta a varias personas; MEDIA: trabaja con
  dificultad; BAJA: molestia menor.» El pedido es solo la descripción.
- Borrador: «Redacta en español, en tercera persona y en pasado, el informe técnico a partir de las notas. No agregues
  piezas, causas ni datos que no estén en lo enviado; si algo no está, deja el campo vacío.» El pedido lleva la
  descripción, las notas y lo ya escrito en cada campo, con su nombre.
- Ninguna función recibe nombres, correos, el responsable, el área, el técnico ni el código del equipo (RNF de
  privacidad); la prueba lo comprueba con una incidencia de prueba que sí los tiene.

### 4.6 `AsistenteIAGemini` (PR-19, P-1 a P-4)
```
disponible = monitorConexion.hayConexion
clasificar(descripcion):
  si !monitorConexion.hayConexionAhora() → Error(SinConexion)
  try modeloClasificacion.generateContent(pedido).text ?: Error(IANoDisponible) → Exito(texto)
  catch CancellationException → relanzar
  catch Exception → Log.w(TAG, …, e); Error(IANoDisponible)   (cuota, servicio, App Check o límite de tiempo)
redactarBorrador(entrada): igual, con el modelo del borrador
```

### 4.7 `AsignacionViewModel` y la sugerencia (PR-26, PR-28, PR-29)
- Al llegar las primeras `opciones`, `tipoElegido` y `prioridadElegida` toman los de la incidencia (una sola vez, para
  no pisar lo que el jefe eligió, RF-1408).
- En la misma primera carga, `viewModelScope.launch`: `estadoSugerencia = GENERANDO` solo si la incidencia está sin
  clasificar y sin sugerencia; `SugerirClasificacion`; cualquier error → `NINGUNA` sin mensaje (RF-1406). La
  sugerencia que se muestra sale de `ObservarSugerencia(CLASIFICACION)`, así una guardada antes también aparece
  (RF-1405).
- `alUsarSugerencia`: copia tipo y prioridad, `usada = true`, oculta la tarjeta. `alDescartarSugerencia`:
  `descartada = true`, oculta. La tarjeta oculta no vuelve mientras la pantalla siga abierta.
- `alAsignar` → `AsignarIncidencia(…, ClasificacionElegida(tipo, prioridad))`; si es `Exito` y hay sugerencia sin
  decidir → `DecidirSugerencia(s, clasificacionAceptada(…))` antes de `asignadaA` (RF-1413). Si falla la decisión, se
  registra en Logcat y la asignación sigue hecha.

### 4.8 `InformeTecnicoViewModel` y el borrador (PR-32, PR-34, PR-35)
- `puedeRedactar = notas no vacías && iaDisponible && borrador != REDACTANDO && !guardando && !cerrada` (RF-1415).
- `alRedactarBorrador`: `REDACTANDO`; `RedactarBorradorInforme(incidencia, EntradaBorrador(descripción, notas,
  campos), ahora)`; `Error` → `errorBorrador = IANoDisponible` o el que llegue, y el estado vuelve al borrador que
  hubiera (RF-1423). El borrador a la vista sale de `ObservarSugerencia(BORRADOR_INFORME)` (RF-1424).
- `alUsarBorrador`: cada campo no vacío del borrador reemplaza al del informe; `textoDeIA = true`;
  `DecidirSugerencia(true)` (RF-1420). `alDescartarBorrador`: `DecidirSugerencia(false)` (RF-1421).
- `alConfirmarCierre`: después del `Exito` del cierre, si queda un borrador sin decidir → `DecidirSugerencia(false)`
  (RF-1426). Se guarda `textoDeIA` en el `SavedStateHandle` para que el aviso sobreviva a una rotación.

## 5. Cómo se pinta en Compose

- **5.1 «Clasificación» en Asignar** (PR-27). `EncabezadoSeccion("Clasificación")` y dos `PestanasSegmentadas`:
  tipo con `listOf(null) + TipoFalla.entries` («Sin clasificar», «Hardware», «Software», «Red») y prioridad con
  `listOf(null) + Prioridad.entries` («Sin prioridad», «Baja», «Media», «Alta»), P-6. Va entre el resumen y «Elige un
  técnico».
- **5.2 Tarjeta de la sugerencia** (PR-30). `GENERANDO`: `TarjetaSugerenciaIA(texto = "Generando sugerencia")` sin
  botones. Lista: `TarjetaSugerenciaIA(titulo = "Sugerencia de IA", texto = "Red, prioridad media\nGenerada
  automáticamente. Tú decides la clasificación.", alUsar, alDescartar)`. `NINGUNA`: nada, sin reservar espacio.
- **5.3 Borrador en el informe** (PR-33, PR-36 a PR-38). Orden: «Mis notas» (`CampoTexto` con `MicrofonoCampo`), el
  botón «Redactar borrador con IA» (`BotonSecundario` con el ícono de destello) y, debajo, «La IA necesita conexión a
  internet.» si no hay red; la tarjeta («Redactando borrador», o el borrador con «Diagnóstico: …», «Trabajo realizado:
  …», «Piezas cambiadas: …», «Observaciones: …» y `textoUsar = "Usar borrador"`); el error de `error.mensaje()`; los
  cuatro campos; «Texto redactado por IA: revísalo antes de confirmar el cierre.» con `bodySmall` y
  `onSurfaceVariant` cuando `textoDeIA`.
- **5.4 Orden en Incidencias** (PR-42, PR-43). Un `FiltroDesplegable("Orden", …)` antes de «Etapa», con «Más
  recientes» y «Más urgentes»; «Limpiar» no lo toca. Con «Más urgentes», `FilaIncidencia(prioridad = …)` muestra la
  etiqueta de la prioridad en cada pendiente.
- Todas las pantallas cambiadas suman vistas previas en claro y oscuro de cada estado nuevo, dentro de `SigtecTheme`.

## 6. Decisiones técnicas

| # | Decisión | Por qué |
|---|---|---|
| P-1 | `Firebase.ai(backend = GenerativeBackend.googleAI())` y los dos `GenerativeModel` se proveen en `IAModule` (`@Provides @Singleton`, calificadores `@ModeloClasificacion` y `@ModeloBorrador`). | `CLAUDE.md`: nada de `getInstance()` ni dependencias armadas a mano dentro de las clases. |
| P-2 | Salida estructurada: `responseMimeType = "application/json"`, `responseSchema` (`Schema.enumeration` para tipo y prioridad; cuatro `Schema.string()`, con piezas y observaciones opcionales) y `temperature = 0.2f`. | Menos respuestas inválidas. Igual el dominio valida (3.1, 3.2): el esquema no reemplaza la validación. |
| P-3 | Límite de tiempo con `RequestOptions(timeoutInMillis = 15_000)` para clasificar y `30_000` para el borrador. | RF-1406 y RF-1423. |
| P-4 | `MODELO_GEMINI = "gemini-3.5-flash-lite"` en una sola constante de `IAModule`; cada sugerencia guarda `asistente.modelo`. | D-6 y RF-1427: cambiar de modelo es una línea, y se sabe qué modelo propuso cada cosa. |
| P-5 | `SugerenciaIA.contenido` guarda el JSON canónico de 3.1 y 3.2. | Se vuelve a leer igual al reabrir (RF-1405, RF-1424) y sube tal como se mostró. |
| P-6 | Los selectores de «Clasificación» tienen la opción nula («Sin clasificar», «Sin prioridad»). | `PestanasSegmentadas` exige una opción elegida; así puede empezar vacío (RF-1408) y el jefe puede quitar una clasificación. |
| P-7 | `SugerenciaIARepository`: `observarSinDecidir(incidenciaId, tipo): Flow<SugerenciaIA?>`, `sinDecidir(…)`, `contar(…)`, `guardar(…)` y `decidir(id, aceptada, decididaPorId, ahora)`, todos con `Resultado`. | Lo mínimo que piden 4.1 a 4.3. |
| P-8 | La decisión se registra después del éxito de la asignación o del cierre, desde el `ViewModel`, no dentro de su transacción. | `asignacion` y `atencion` siguen escribiendo solo sus tablas, y si la decisión falla no deshace la asignación. Si la app muere entre ambos pasos, la sugerencia queda sin decidir y no se sube; no cambia ningún dato. |
| P-9 | Los calificadores de Hilt en las clases nuevas van con `@param:` (`@param:IoDispatcher private val io`). | No suma al aviso de Kotlin que ya existe en `main` (la lista de mantenimiento lo corrige en el resto). |
| P-10 | `AsistenteIA.disponible` envuelve `MonitorConexion.hayConexion`. | El `ViewModel` del informe no depende de `core/sync` (RF-1415). |
| P-11 | La incidencia, su entidad y su regla no cambian; solo `SugerenciaIAEntity` (versión 7). | La clasificación usa campos que ya existen (sección 0). |
| P-12 | No se reintenta solo dentro de la misma pantalla, y el borrador es solo a pedido. | La cuota gratuita es por proyecto (RNF de cuota). |

## 7. Estrategia de pruebas

- **JVM, funciones puras:** `ClasificacionTest` (válida, mayúsculas, tipo o prioridad fuera de los permitidos, falta
  un campo, texto suelto, lee su propio contenido), `BorradorTest` (completo, sin piezas, sin diagnóstico, recorte a
  1000, texto suelto), `ReglasSugerenciaTest` (usada sin cambios, usada y corregida, descartada, sin usar),
  `OrdenIncidenciasTest` (las tres prioridades, sin prioridad al final, empate por antigüedad, solucionadas al final,
  `RECIENTES` sin cambios), `InstruccionesGeminiTest` (lleva la descripción y las notas, no lleva nombre, área,
  técnico ni código del equipo).
- **JVM, casos de uso** con `SesionRepositoryDemo`, `AsistenteIAFalso` y `SugerenciaIARepositoryFalso`:
  `SugerirClasificacionTest` (sin sesión, personal de sistemas, cerrada, ya clasificada, ya sugerida, Gemini falla,
  respuesta inválida, camino feliz con el modelo y la fecha), `RedactarBorradorInformeTest` (otro técnico, cerrada,
  sin notas, falla con un anterior a la vista, anterior sin decidir queda no aceptado, camino feliz),
  `DecidirSugerenciaTest` (permiso según el tipo, ya decidida, quién y cuándo), `ObservarSugerenciaTest`,
  `AsignarIncidenciaTest` (con clasificación, `null` no cambia, límite).
- **JVM, ViewModels** con `MainDispatcherRule`: `AsignacionViewModelTest` (clasificación inicial, generando, lista,
  sin sugerencia, ya clasificada, usar, descartar, corregir, decisión al asignar), `InformeTecnicoViewModelTest`
  (notas, dictado de notas, `puedeRedactar`, redactando, listo, error, sin conexión, usar, descartar, otro borrador,
  decisión al cerrar, el aviso tras rotar), `ListaIncidenciasViewModelTest` (orden con filtros, limpiar no lo cambia).
- **Instrumentada:** `SugerenciaIADaoTest` (sin decidir más reciente, conteo por tipo, decidir, solo las decididas por
  subir, marcar sincronizada) en un celular, con `version` 7.
- **Firestore:** Rules Playground con la regla de PR-07 (crear a nombre propio y con `aceptada`: permite; a nombre de
  otro, sin `aceptada`, tipo inválido, actualizar o borrar: rechaza).
- **Ninguna prueba llama a Gemini.** `AsistenteIAGemini` se prueba en el celular (spec, «Criterios de finalización»).
- **Manual en el celular:** los puntos de la spec, con capturas en claro y oscuro en el scratchpad, nunca en el repo.

## 8. Cobertura de RF

| RF | PR |
|---|---|
| RF-1401 a RF-1407 | PR-12, PR-15, PR-18 a PR-20, PR-23, PR-28, PR-30 |
| RF-1408 a RF-1413 | PR-14, PR-22, PR-25 a PR-27, PR-29, PR-30 |
| RF-1414 a RF-1426 | PR-13, PR-15, PR-18, PR-19, PR-21 a PR-24, PR-31 a PR-38 |
| RF-1427 a RF-1431 | PR-04 a PR-10, PR-16, PR-17, PR-22 (RF-1431 no pide código: `clearAllTables()` ya borra la tabla y `EstadoDatosRepositoryImpl` no la cuenta) |
| RF-1432 | PR-02, PR-03 |
| RF-1433 a RF-1437 | PR-39 a PR-43 |

## 9. Respeto a la constitución

1. **Stack cerrado:** sin dependencias nuevas; `firebase-ai` y `kotlinx-serialization-json` ya están. App Check queda
   para un PR propio si se decide antes de publicar (D-9).
2. **Spec antes que código:** spec #154 aprobada; si el código cambia algo, la spec se actualiza en el mismo PR.
3. **Lógica fuera de la pantalla:** validación, aceptación y orden son funciones puras en `domain`; los permisos van
   en los casos de uso.
4. **Toda regla tiene prueba:** sección 7; cada PR pasa `assembleDebug testDebugUnitTest`, y `lintDebug` corre en
   cada tanda.
5. **Datos protegidos:** sin claves (Firebase AI Logic autoriza con su cuenta de servicio); a Gemini solo va el texto
   del problema; la IA no cambia nada sin «Usar» y la decisión de la persona; el cierre sigue con huella.
6. **Español en el negocio:** clases, funciones, textos y comentarios en español.

## 10. Resumen: archivos compartidos y Room por PR

| PR | Compartido (⚠️) o sensible (🔶) | Room |
|---|---|---|
| PR-01, PR-03 | 🔶 `CLAUDE.md` | — |
| PR-03 | ⚠️ `Rutas.kt` | — |
| PR-04 | ⚠️ `core/domain/model/TipoSugerencia.kt`, `SugerenciaIAEntity.kt`, `SigtecDatabase.kt` | `version` 6 → 7 (se dice en el PR: se pierde lo no sincronizado) |
| PR-05 | ⚠️ `SugerenciaIADao.kt` | consultas nuevas, sin cambio de esquema |
| PR-07 | ⚠️ `firestore.rules` (Nina la publica al fusionarse) | — |
| PR-08 a PR-10 | 🔶 `core/sync/` | — |
| PR-11 | ⚠️ `Resultado.kt`, `MensajesError.kt` | — |
| PR-24, PR-41 | 🔶 `core/ui/components/` | — |
| PR-44 | `MEMORY.md` (solo la línea de la HU-14) | — |
