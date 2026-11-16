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
