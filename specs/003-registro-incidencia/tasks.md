# Tareas 003 — Registrar una incidencia con fecha automática (HU-03)

> Salen de `spec.md` y `plan.md`. RF-30 a RF-40 ya están en `main` desde el PR #7 (`fea7683`) y el PR #9 (`6420f40`); ninguna tarea los reimplementa. Solo hay tres pendientes reales: `ahora: Instant` (RF-36), el catálogo de equipos (RF-41) y `RegistroIncidenciaViewModelTest` (RF-30/31/32/40).
> Cada tarea dura 20-30 min como máximo. Orden de adentro hacia afuera y de lo más aislado a lo más grande: primero `ahora: Instant` (un solo archivo de dominio y su llamador), después el catálogo de equipos (toca `core/sync`, dominio y datos), después la prueba del ViewModel, y al final la verificación.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/` (pruebas). ⚠️ = archivo compartido (`core/domain/Resultado.kt`, `core/ui/MensajesError.kt`, `core/sync/*`), **requiere visto bueno del usuario antes de tocarlo**.

## Fase 0 — Preparación

- [x] **T01 · Confirmar con el usuario el diseño de RF-41 antes de tocar archivos compartidos**
  Mostrar la sección 2 de `plan.md` (nuevo caso de `ErrorDominio`, el mapeo y el listener nuevo en `core/sync`) porque toca `Resultado.kt`, `MensajesError.kt` y `core/sync/*`. La sección 1 (`ahora: Instant`) y la sección 3 (prueba del ViewModel) no tocan archivos compartidos y pueden avanzar con la aprobación general del plan.
  RF: RF-41
  Hecho cuando: el usuario aprueba explícitamente el diseño de la sección 2 (o pide un cambio, que se refleja en `plan.md` antes de seguir).

- [x] **T02 · Crear o confirmar la rama de trabajo**
  Crear (o retomar) una rama propia de esta spec desde `main` al día, por ejemplo `feat/hu03-ahora-y-catalogo-sdd`.
  RF: —
  Hecho cuando: `git branch --show-current` muestra la rama elegida y `./gradlew testDebugUnitTest` pasa sin cambios respecto a `main`.

## Fase 1 — `ahora: Instant` en `RegistrarIncidencia` (aislado, sin archivos compartidos)

- [x] **T03 · Cambiar la firma de `RegistrarIncidencia.invoke` para recibir `ahora: Instant`**
  En `feature/incidencias/domain/RegistrarIncidencia.kt`, agregar `ahora: Instant` como cuarto parámetro de `operator fun invoke(...)` y usarlo en `fechaRegistro = ahora` en vez de `Instant.now()`.
  RF: RF-33, RF-36
  Hecho cuando: el archivo compila sin `Instant.now()` interno y el comentario `// automática, no editable` sigue describiendo el campo.

- [x] **T04 · Pasar `Instant.now()` desde `RegistroIncidenciaViewModel.guardar()`**
  En `feature/incidencias/presentation/RegistroIncidenciaViewModel.kt`, cambiar la llamada a `registrarIncidencia(actual.codigoEquipo, actual.descripcion, actual.responsable, Instant.now())`.
  RF: RF-33, RF-36
  Hecho cuando: `./gradlew assembleDebug` compila; `RegistroIncidenciaViewModel.kt` sigue sin ningún `import android.*` nuevo.

- [x] **T05 · Actualizar `RegistrarIncidenciaTest.kt` para el nuevo parámetro**
  Agregar un `ahora` (por ejemplo `Instant.parse("2025-01-01T10:00:00Z")`) a todas las llamadas existentes (`registrarComoLuis()` y las llamadas directas de `sin sesion no registra`, `si Room falla devuelve el error`, `un campo en blanco no registra nada`), y agregar un caso nuevo que fija un instante distinto y comprueba `assertEquals(elInstanteFijado, inc.fechaRegistro)`.
  RF: RF-36
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RegistrarIncidenciaTest"` pasa, incluido el caso nuevo.

## Fase 2 — RF-41: catálogo de equipos ⚠️ (toca archivos compartidos)

- [x] **T06 · Agregar `ErrorDominio.EquipoNoEncontrado` ⚠️**
  En `core/domain/Resultado.kt`, agregar `data object EquipoNoEncontrado : ErrorDominio` con un comentario `// RF-41`.
  RF: RF-41
  Hecho cuando: compila (el `when` de `MensajesError.kt` todavía no es exhaustivo; se completa en T07).

- [x] **T07 · Agregar el texto del error en `MensajesError.kt` ⚠️**
  Agregar `ErrorDominio.EquipoNoEncontrado -> "El código de equipo no existe en el catálogo. Verifícalo e inténtalo de nuevo."` al `when` de `mensaje()`.
  RF: RF-41
  Hecho cuando: `./gradlew assembleDebug` compila (el `when` exhaustivo vuelve a cerrar) y ningún otro `when` sobre `ErrorDominio` quedó sin el caso nuevo (confirmado con `grep -rn "ErrorDominio\." app/src/main` antes de abrir el PR).

- [x] **T08 · Agregar `DocumentSnapshot.aEquipoEntity()` en `MapeoFirestore.kt` ⚠️**
  Agregar la función de mapeo bajo una sección nueva «Catálogo», siguiendo el estilo de `aIncidenciaEntity()` (usa `id` del documento como `codigo`, `runCatching { }.getOrNull()` para ignorar documentos mal formados).
  RF: RF-41
  Hecho cuando: compila y la función no se usa todavía (se conecta en T09).

- [x] **T09 · Bajar `/equipos` en `SincronizacionTiempoReal.kt` ⚠️**
  Inyectar `CatalogoDao` en el constructor, agregar `escuchar("equipos", ::guardarEquipos)` al `merge(...)` de `iniciar()`, y el método privado `guardarEquipos(snap: QuerySnapshot)` que filtra los cambios distintos de `REMOVED`, los mapea con `aEquipoEntity()` y llama a `catalogoDao.guardarEquipos(...)` si la lista no está vacía.
  RF: RF-41
  Hecho cuando: `./gradlew assembleDebug` compila; `SigtecApp.kt` no cambió (Hilt resuelve el nuevo parámetro del constructor solo).

- [x] **T10 · Agregar `existeEquipo` a `IncidenciaRepository` (interfaz)**
  En `feature/incidencias/domain/IncidenciaRepository.kt`, agregar `suspend fun existeEquipo(codigo: String): Boolean` con su comentario `// RF-41`.
  RF: RF-41
  Hecho cuando: compila (no implementado todavía en `IncidenciaRepositoryImpl`, se completa en T11).

- [x] **T11 · Implementar `existeEquipo` en `IncidenciaRepositoryImpl`**
  Inyectar `CatalogoDao` en el constructor de `feature/incidencias/data/IncidenciaRepositoryImpl.kt` e implementar `existeEquipo` con `withContext(io) { catalogoDao.equipoPorCodigo(codigo) != null }`.
  RF: RF-41
  Hecho cuando: `./gradlew assembleDebug` compila sin agregar ningún `@Binds` ni módulo de DI nuevo (`CatalogoDao` ya se provee en `DatabaseModule`).

- [x] **T12 · Agregar el catálogo en memoria a `IncidenciaRepositoryFalso`**
  En el doble de prueba, agregar un `MutableSet<String>` de equipos válidos (por defecto los 4 de prueba: `PC-CONT-014`, `PC-LOG-009`, `PC-SIS-003`, `IMP-RRHH-002`), implementar `existeEquipo` y agregar `fun agregarEquipo(codigo: String)` para los casos que necesiten un catálogo distinto.
  RF: RF-41
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RegistrarIncidenciaTest"` sigue pasando sin cambios adicionales (los códigos ya usados en las pruebas existentes están en el set por defecto).

- [x] **T13 · Consultar el catálogo desde `RegistrarIncidencia`**
  Tras el chequeo de campos en blanco (RF-35) y antes de construir la `Incidencia`, agregar `if (!repo.existeEquipo(codigo)) return Resultado.Error(ErrorDominio.EquipoNoEncontrado)`.
  RF: RF-41
  Hecho cuando: compila y el orden de validaciones queda: permiso → campos en blanco → catálogo → guardar.

- [x] **T14 · Agregar los casos de RF-41 a `RegistrarIncidenciaTest.kt`**
  Agregar un caso con un código fuera del catálogo falso (por ejemplo `"PC-CONT-14"`, el mismo del caso límite de la spec) que compruebe `Resultado.Error(ErrorDominio.EquipoNoEncontrado)` y que `repo.incidencias`/`repo.movimientos` sigan vacíos.
  RF: RF-41
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RegistrarIncidenciaTest"` pasa con el caso nuevo.

## Fase 3 — `RegistroIncidenciaViewModelTest` (nuevo)

- [x] **T15 · Crear `RegistroIncidenciaViewModelTest.kt` con el caso de RF-31**
  Archivo nuevo en `app/src/test/java/com/softcorp/sigtec/`, con `MainDispatcherRule`, `SesionRepositoryDemo` e `IncidenciaRepositoryFalso`. Caso: sin sesión o con algún campo vacío, `estado.value.puedeGuardar == false`; al completar los tres campos con sesión iniciada, pasa a `true`.
  RF: RF-30, RF-31
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RegistroIncidenciaViewModelTest"` pasa con este primer caso.

- [x] **T16 · Agregar el caso de RF-32 (reloj)**
  Con tiempo virtual (`runTest`/`advanceTimeBy`), comprobar que `estado.value.fechaHora` cambia tras avanzar el intervalo del reloj (`RELOJ_MS`), sin fijar el valor exacto (el reloj usa `Instant.now()` real, ver sección 1 del plan).
  RF: RF-32
  Hecho cuando: el nuevo caso pasa junto con el de T15.

- [x] **T17 · Agregar el caso de RF-40 (evento de un solo uso)**
  Con un código de equipo válido en el catálogo falso, llamar a `vm.guardar()`, avanzar el dispatcher y comprobar `estado.value.registradaId` no nulo y coincide con el id devuelto por `IncidenciaRepositoryFalso`; luego `vm.alNavegarAlDetalle()` y comprobar que vuelve a `null`.
  RF: RF-40
  Hecho cuando: el nuevo caso pasa junto con los anteriores.

- [x] **T18 · Agregar el caso de error de RF-41 a nivel de `ViewModel`**
  Con un código de equipo fuera del catálogo falso, llamar a `vm.guardar()` y comprobar `estado.value.error == ErrorDominio.EquipoNoEncontrado`, `registradaId == null` y `guardando == false` (se puede reintentar).
  RF: RF-41
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RegistroIncidenciaViewModelTest"` pasa con los cuatro casos (T15-T18).

## Fase 4 — Verificación

- [x] **T19 · Comando del CI**
  Correr `./gradlew assembleDebug testDebugUnitTest lintDebug`.
  RF: RF-33, RF-36, RF-41
  Hecho cuando: termina en verde y sin warnings de lint nuevos respecto a `main`.

- [x] **T20 · Prueba manual — NO automática: equipo válido e inválido en el celular**
  **Detenerse aquí si no hay celular o emulador disponible; esta tarea no se puede completar solo con el comando del CI.**
  Iniciar sesión (cualquier perfil), abrir «Nueva incidencia», completar el formulario con un código de equipo válido (por ejemplo `PC-CONT-014`) y guardar: debe registrarse con éxito y mostrar «Incidencia registrada · Por sincronizar». Repetir con un código inválido (por ejemplo `PC-CONT-14` o `PC`): debe mostrar el mensaje de equipo no encontrado y no navegar, dejando el formulario con lo escrito para corregir. Confirmar en Logcat o en la consola de Firestore que `/equipos` efectivamente bajó a Room (o revisar si el mapeo de campos de T08 coincide con los documentos reales; si no, ajustarlo).
  RF: RF-41
  Hecho cuando: el equipo válido registra y navega al detalle; el equipo inválido muestra el error y no guarda nada; no hay excepciones nuevas en `adb logcat *:W`.
  > Confirmado por el usuario en el emulador: "T20 ok".

- [x] **T21 · Prueba manual — NO automática: `ahora: Instant` no cambió el comportamiento**
  **Detenerse aquí si no hay celular o emulador disponible.**
  Completar el formulario, esperar más de un minuto sin guardar (comprobando que la fecha mostrada avanza, ya cubierto desde antes de esta spec) y guardar: la fecha del detalle debe ser la del instante real en que se pulsó «Guardar», no la de cuando se abrió el formulario.
  RF: RF-33, RF-36
  Hecho cuando: la fecha del detalle coincide con el momento del guardado, no con el de apertura del formulario.
  > Confirmado por el usuario en el emulador: "T21 ok".

## Fase 5 — Cierre

- [x] **T22 · Actualizar `spec.md`**
  Quitar la marca «(pendiente)» del encabezado y de RF-41; actualizar «Criterios de finalización» para reflejar que RF-41 ya está implementado y probado, y que RF-30/31/32/40 ya tienen prueba automatizada de `RegistroIncidenciaViewModel`; mover la nota de RF-36 (`Instant.now()` interno) y la de RF-41 en «Dudas abiertas» a «resuelto» si procede.
  RF: todos
  Hecho cuando: `grep "pendiente" specs/003-registro-incidencia/spec.md` no encuentra ningún RF marcado como pendiente de implementación.

- [x] **T23 · Actualizar `MEMORY.md`**
  Si corresponde, anotar en «Aprendizajes y errores a evitar» que `core/sync` ya baja `/equipos` con el mismo `escuchar(...)` genérico usado para `incidencias`/`movimientos`, y que los casos de uso nunca inyectan un DAO directamente (siempre pasan por el repositorio del módulo).
  RF: —
  Hecho cuando: `MEMORY.md` sigue en ~50 líneas como máximo.

- [x] **T24 · Commit y PR**
  Commits en Conventional Commits en español con la HU al final, por ejemplo `refactor(incidencias): recibir el instante del registro como parametro (HU-03)` y `feat(incidencias): validar el codigo de equipo contra el catalogo (HU-03)`, sin línea de coautoría. PR con `.github/pull_request_template.md`, incluida la sección «Archivos compartidos que toca» (citando `core/domain/Resultado.kt`, `core/ui/MensajesError.kt`, `core/sync/MapeoFirestore.kt` y `core/sync/SincronizacionTiempoReal.kt`), y las pruebas manuales de T20 y T21 descritas.
  RF: todos
  Hecho cuando: el PR existe, el CI «compilar-y-probar» está en verde, y la descripción enlaza `specs/003-registro-incidencia/`.
