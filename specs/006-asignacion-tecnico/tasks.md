# Tareas 006 — Asignación de trabajo con carga y límite de atención (HU-06)

> Salen de `spec.md` y `plan.md`. Cada tarea dura entre 20 y 30 minutos como máximo y depende solo de las anteriores de su PR. Van agrupadas por PR (A a F, plan sección 1) y, dentro de cada uno, de adentro hacia afuera: reglas puras con sus pruebas → Room/DAO → `core/sync` o repositorio → ViewModel → pantalla → verificación.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/` (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas). ⚠️ = archivo compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista. Ambos necesitan el visto bueno de este plan antes de tocarse.
> Orden de fusión: A → B, C y E en cualquier orden → D (después de B) → F (último).

## PR A — spec, plan y tareas (`docs/hu06-asignacion-spec`, desde `main`)

- [x] **T01 · Crear la rama desde `main` al día**
  `git pull origin main` y `git switch -c docs/hu06-asignacion-spec`.
  RF: —
  Hecho cuando: `git branch --show-current` muestra la rama y `git log -1` coincide con `origin/main`.

- [x] **T02 · Visto bueno de la spec, el plan y las tareas**
  El usuario revisa los tres archivos; cada cambio pedido se refleja primero en `spec.md` y después en `plan.md` y `tasks.md`.
  RF: todos
  Hecho cuando: el usuario aprueba explícitamente, o los cambios pedidos ya están escritos.
  > Aprobado por el usuario el 2026-10-11, junto con la división en 6 PR y el orden de fusión.

- [x] **T03 · Resolver las dudas abiertas**
  Responder las 5 «Dudas abiertas» de `spec.md`: Esperando repuesto, carga actual en Firestore, cierre en la HU-08, alcance de RF-624 y texto bajo el botón. Las dos que dependen de Alonso se le preguntan en el PR.
  RF: RF-605, RF-614, RF-622, RF-624
  Hecho cuando: ninguna duda queda sin respuesta, o la que quede está marcada como no bloqueante con su motivo.
  > Resueltas el 2026-10-11 (spec, «Dudas abiertas»). Esperando repuesto conserva la marca (nuevo RF-625). El cierre de la HU-08 queda como dependencia para Alonso.

- [ ] **T04 · Commits, `MEMORY.md` y PR A en borrador**
  Con `git pull origin main` antes de tocar `MEMORY.md`, separar la línea «HU-06 y HU-07 (Nina)» y dejar la de la HU-06 como «spec aprobada (RF-601 a RF-625); código en los PR B a F». Hacer los 4 commits de `plan.md` (PR A) sin línea de coautoría y abrir el PR en borrador con `.github/pull_request_template.md` («Archivos compartidos que toca: ninguno»), pidiendo revisión a Alonso.
  RF: —
  Hecho cuando: el PR existe con el CI «compilar-y-probar» en verde y Alonso lo aprueba.

## PR B — rechazos de sincronización (`fix/sync-rechazos`, desde `main`, HU-05)

- [x] **T05 · Corregir la spec 005 primero**
  En `specs/005-sincronizacion/`: cambiar RF-68, RF-75 y RF-76, y agregar RF-79 (cambio local que pierde al bajar), RF-80 («Entendido») y RF-81 (contar rechazos al cerrar sesión), según `plan.md` sección 4.8. Actualizar su `plan.md` y `tasks.md`.
  RF: RF-621, RF-624 (dependencia)
  Hecho cuando: la spec 005 describe los 6 puntos de 4.8 y Alonso confirma el texto en el PR.

- [x] **T06 · `FAILED_PRECONDITION` transitorio 🔶**
  Sacarlo de `CODIGOS_PERMANENTES` en `core/sync/ReglasSincronizacion.kt` y cambiar su caso en `ReglasSincronizacionTest` a `assertFalse`.
  RF: — (problema 4)
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ReglasSincronizacionTest"` pasa.

- [x] **T07 · Consultas de rechazo en los DAO 🔶**
  `IncidenciaDao.marcarNecesitaAtencion(id, actualizadoEn): Int` (con guarda), `MovimientoDao.listosParaSubir()`, `MovimientoDao.marcarNecesitaAtencionDe(incidenciaId)`, conteo de incidencias afectadas, `borrarRechazados()` y limpieza de la marca, con sus casos en `MovimientoDaoTest` (nuevo) e `IncidenciaDaoTest`.
  RF: — (problemas 1, 3 y 5)
  Hecho cuando: `./gradlew assembleDebug` compila y `connectedDebugAndroidTest` pasa en un celular.
  > 2026-10-11: `connectedDebugAndroidTest` pasó en el emulador Pixel_8 (Android 14): 7 de 7, 4 de `IncidenciaDaoTest` y 3 de `MovimientoDaoTest`.

- [x] **T08 · Subir los movimientos solo después de su incidencia 🔶**
  `SincronizadorIncidencias.subirPendientes` usa `listosParaSubir()` en vez de `pendientesDeSincronizar()` para los movimientos.
  RF: — (problema 1)
  Hecho cuando: compila y, con una incidencia que falla al subir, su movimiento sigue pendiente (prueba manual en T15).

- [x] **T09 · Rechazo permanente: marca, movimientos y versión del servidor 🔶**
  En `subir(local)`, ante un error permanente: en una transacción de Room se marca la incidencia con guarda y, si se marcó, sus movimientos. Luego se lee el documento del servidor y, si existe, se guarda con `necesitaAtencion = true`.
  RF: RF-621 (dependencia)
  Hecho cuando: compila y `SincronizadorIncidencias.kt` no marca una incidencia que se editó mientras subía.

- [x] **T10 · `atencionAlBajar` y su uso al bajar 🔶**
  Función pura en `ReglasSincronizacion.kt` con 4 casos en `ReglasSincronizacionTest` (sin local; local con marca; local pendiente que pierde; local ya subido). `guardarIncidencias` la usa y marca los movimientos pendientes del cambio que pierde. `debeReemplazarLocal` no cambia.
  RF: RF-624 (dependencia)
  Hecho cuando: las pruebas pasan y `git diff` no muestra cambios en `debeReemplazarLocal`.

- [x] **T11 · Contar incidencias afectadas en `EstadoDatosRepositoryImpl` 🔶**
  `necesitanAtencion` usa el conteo por incidencia afectada de T07 en vez de sumar filas (reemplaza D-6 de la spec 005).
  RF: — (problema 1)
  Hecho cuando: compila y una incidencia rechazada con 2 movimientos cuenta como 1.

- [x] **T12 · Contar los rechazos al cerrar sesión o al entrar otra persona ⚠️**
  `EvaluarCierreSesion` y `EvaluarIngresoDeOtraPersona` usan `pendientesPorEnviar + necesitanAtencion`. Agregar un caso a cada prueba con `EstadoDatosRepositoryFalso.fijarNecesitanAtencion(1)` y 0 pendientes, que debe pedir confirmación.
  RF: — (problema 2)
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*Evaluar*"` pasa.

- [x] **T13 · `DescartarCambiosRechazados` 🔶**
  Agregar `descartarRechazados()` a `EstadoDatosRepository` (interfaz), a su implementación (transacción de Room de 4.8, punto 5) y al doble falso, más el caso de uso en `core/domain/usecase` y su prueba.
  RF: — (problema 3)
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*DescartarCambiosRechazadosTest"` pasa.

- [x] **T14 · Aviso con «Entendido» 🔶**
  En `AvisoConexion.kt`: texto «1 cambio no se pudo enviar» / «N cambios no se pudieron enviar», un `TextButton` «Entendido» que llama al caso de uso desde `AvisoConexionViewModel`, casos nuevos en `AvisoConexionTest` y previews en claro y oscuro.
  RF: — (problema 3)
  Hecho cuando: `AvisoConexionTest` pasa y las previews se ven en Android Studio.

- [ ] **T15 · Verificación, `MEMORY.md` y PR B**
  Correr el comando del CI. Hacer la prueba manual de la spec 005: rechazo provocado con una regla temporal en la consola, que el usuario pone y quita. Comprobar el aviso, que el movimiento no aparece en la consola, «Entendido» y el aviso al cerrar sesión. Proponer a Alonso el cambio de la línea de la HU-05 en `MEMORY.md`. Hacer los 7 commits de `plan.md` (PR B).
  RF: —
  Hecho cuando: CI en verde, prueba manual descrita en el PR y aprobación de Alonso.

## PR C — bajar los técnicos (`feat/sync-tecnicos`, desde `main` con A)

- [x] **T16 · `tecnicoDesdeDatos` y su prueba 🔶**
  Función pura en `core/sync/MapeoFirestore.kt` (plan 3.8) y `DocumentSnapshot.aTecnicoEntity()`, que la usa; `MapeoTecnicoTest` con 3 casos.
  RF: RF-619
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*MapeoTecnicoTest"` pasa.

- [x] **T17 · `CatalogoDao.borrarTecnicos` 🔶**
  `@Query("DELETE FROM tecnicos WHERE id IN (:ids)")` y `CatalogoDaoTest` instrumentada (guardar 2, borrar 1).
  RF: RF-619
  Hecho cuando: `connectedDebugAndroidTest` pasa en un celular.

- [ ] **T18 · Escuchar `usuarios` con perfil técnico 🔶**
  `escuchar(...)` recibe una `Query` y un nombre; las 3 llamadas existentes pasan su colección. Se agrega la escucha de `usuarios` con `whereEqualTo("perfil", "TECNICO")` y `guardarTecnicos(snap)` (plan 4.5).
  RF: RF-619
  Hecho cuando: compila, y con el Database Inspector de Android Studio la tabla `tecnicos` tiene los 4 técnicos de prueba con su límite.
  > 2026-10-11: código hecho. La consulta es un parámetro opcional de `escuchar(...)` (por defecto, la colección entera), así que las 3 llamadas existentes no cambiaron. Falta que el usuario compruebe la tabla en el Database Inspector: el agente no inicia sesión con las cuentas de prueba.

- [ ] **T19 · Verificación y PR C**
  Correr el comando del CI. Prueba manual: con la app abierta, cambiar en la consola `incidenciasActivas` de un técnico y verlo cambiar en la tabla; dejarlo como estaba. Marcar T16–T18 y hacer los 4 commits de `plan.md` (PR C).
  RF: RF-619
  Hecho cuando: CI en verde y aprobación de Alonso.
  > 2026-10-11: `assembleDebug testDebugUnitTest lintDebug` en verde en local (136 pruebas JVM; lint sin avisos nuevos) y `connectedDebugAndroidTest` 9 de 9 en el emulador (incluye `CatalogoDaoTest`). Falta la prueba manual con la consola, a cargo del usuario.

## PR D — carga del técnico y reglas (`feat/sync-carga-tecnico`, apilada sobre B)

- [ ] **T20 · `cambiosDeCarga` y `cargaValida` 🔶**
  Funciones puras en `ReglasSincronizacion.kt` (plan 3.6 y 3.7), con casos en `ReglasSincronizacionTest`: sin cambio, asignar, reasignar, quitar; dentro, en el límite, sobre el límite y bajo 0.
  RF: RF-620, RF-621
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ReglasSincronizacionTest"` pasa.

- [ ] **T21 · Transacción `actualizarConCarga` 🔶**
  En `SincronizadorIncidencias.subir(local)`, una incidencia con número pasa por la transacción de 4.6. Si devuelve `CARGA_RECHAZADA`, se usa el rechazo de T09.
  RF: RF-620, RF-621
  Hecho cuando: compila y una incidencia que no cambia de técnico se sube igual que antes (sin escrituras en `usuarios`).

- [ ] **T22 · Reglas de `incidencias` ⚠️**
  Aplicar el diff de 4.7 a `firebase/firestore.rules` sin tocar la regla de `usuarios` ni la del contador.
  RF: RF-622, RF-623
  Hecho cuando: el archivo tiene las funciones de apoyo y el `allow update` nuevo, y `git diff` solo muestra el bloque de `incidencias` y sus funciones.

- [ ] **T23 · Rules Playground — a cargo del usuario, no de un agente**
  Probar con usuarios reales: el jefe asigna con la carga +1 (permitido); asigna sin mover la carga (denegado); reasigna con +1 y −1 (permitido); asigna sobre el límite (denegado por `usuarios`); el técnico asignado actualiza sin cambiar el técnico (permitido); el técnico cambia `tecnicoAsignadoId` (denegado); el técnico anterior actualiza tras la reasignación (denegado, RF-624).
  RF: RF-622, RF-623, RF-624
  Hecho cuando: los 7 casos dan el resultado esperado.

- [ ] **T24 · Verificar la carga en la consola — a cargo del usuario**
  Antes de publicar, comprobar para cada técnico que `usuarios/{uid}.incidenciasActivas` coincide con el número de incidencias pendientes (no cerradas) que tiene asignadas en `incidencias`. Nadie asignó a mano, así que se espera que coincidan; si alguna no coincide, corregirla en la consola y anotarlo en el PR.
  RF: RF-622
  Hecho cuando: la carga de cada técnico coincide con lo que muestra el filtro por técnico de la lista.

- [ ] **T25 · Publicar las reglas — a cargo del usuario o de Alonso**
  Pegar `firebase/firestore.rules` en la consola después de T23 y T24.
  RF: RF-622, RF-623
  Hecho cuando: la consola muestra la versión publicada con la fecha, anotada en `MEMORY.md`.

- [ ] **T26 · Verificación y PR D**
  Correr el comando del CI. La transacción real no se puede probar todavía, porque sin la pantalla de F no hay forma de asignar desde la app: se prueba en T41 y T42, y así se indica en «Cómo probarlo». Comprobar a mano que registrar y subir una incidencia sigue funcionando igual. Hacer los 4 commits de `plan.md` (PR D).
  RF: RF-620–RF-623
  Hecho cuando: CI en verde y aprobación de Alonso.

## PR E — dominio y datos (`feat/asignacion-dominio`, desde `main` con A)

- [x] **T27 · Errores de asignación ⚠️**
  `IncidenciaPorSincronizar`, `IncidenciaCerrada` y `YaAsignada(tecnicoNombre)` en `core/domain/Resultado.kt`, con sus textos de la spec en `core/ui/MensajesError.kt`.
  RF: RF-609, RF-610, RF-611
  Hecho cuando: `./gradlew assembleDebug` compila (el `when` exhaustivo de `mensaje()` cierra).

- [x] **T28 · `ReglasAsignacion` y su prueba**
  Funciones 3.1 a 3.5 y 3.11 (`marcaAlAsignar`) en `feature/asignacion/domain/ReglasAsignacion.kt`, con `OpcionTecnico`, y `ReglasAsignacionTest` con los casos de `plan.md` sección 7.
  RF: RF-602–RF-604, RF-607, RF-609, RF-610, RF-613–RF-615, RF-625
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ReglasAsignacionTest"` pasa y el archivo no importa nada de `android.*`.

- [x] **T29 · `AsignacionRepository` y su doble falso**
  Interfaz en `feature/asignacion/domain` (incidencia, técnico, técnicos, oficina, asignaciones sin subir, `asignar`) y `AsignacionRepositoryFalso` en `app/src/test/.../feature/asignacion/data`, en memoria y con `fallar`.
  RF: RF-601, RF-602, RF-613
  Hecho cuando: compila con `./gradlew testDebugUnitTest`.

- [x] **T30 · `AsignarIncidencia` con sus pruebas**
  Caso de uso de 4.2 (`@Inject constructor`, `operator fun invoke(incidenciaId, tecnicoId, ahora)`) y `AsignarIncidenciaTest` con los 10 casos de `plan.md` sección 7, incluida la reasignación desde Esperando repuesto, que conserva la marca.
  RF: RF-608–RF-616, RF-625
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*AsignarIncidenciaTest"` pasa.

- [ ] **T31 · `ObservarOpcionesAsignacion` con su prueba**
  Combina la incidencia, su oficina, los técnicos y las asignaciones sin subir (4.1) en `OpcionesAsignacion` con su bloqueo; `ObservarOpcionesAsignacionTest` con 3 casos.
  RF: RF-601, RF-602, RF-606, RF-607
  Hecho cuando: la prueba pasa.

- [x] **T32 · Consultas nuevas en `IncidenciaDao` y `CatalogoDao` 🔶**
  `observarAsignacionesSinSubir()` y `asignacionesSinSubir(tecnicoId)` (4.4) y `CatalogoDao.tecnicoPorId(id)`, con un caso en `IncidenciaDaoTest`: una asignación con movimiento pendiente cuenta; una incidencia pendiente sin movimiento de asignación no cuenta.
  RF: RF-602, RF-612
  Hecho cuando: `connectedDebugAndroidTest` pasa en un celular.

- [ ] **T33 · `AsignacionRepositoryImpl` y `AsignacionModule`**
  Implementación de 4.3 (`@IoDispatcher`, `db.withTransaction`, `catch` que relanza `CancellationException` y registra con `Log.w`) y `@Binds` en `feature/asignacion/di/AsignacionModule.kt`.
  RF: RF-613, RF-616
  Hecho cuando: `./gradlew assembleDebug` compila (Hilt resuelve el repositorio).

- [ ] **T34 · Verificación y PR E**
  Correr el comando del CI. Hacer los 6 commits de `plan.md` (PR E). El PR no cambia nada visible (la pantalla llega en F): así se indica en «Cómo probarlo».
  RF: RF-602, RF-608–RF-616
  Hecho cuando: CI en verde y aprobación de Alonso.

## PR F — pantalla y detalle (`feat/asignacion-pantalla`, apilada sobre E; se fusiona después de C y D)

- [ ] **T35 · `TextosAsignacion` y su prueba**
  `iniciales(nombre)`, el texto del botón (asignar o reasignar) y la línea «La incidencia aparecerá en la bandeja de <nombre>», con `TextosAsignacionTest`.
  RF: RF-601, RF-605
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*TextosAsignacionTest"` pasa.

- [ ] **T36 · `AsignacionViewModel` con su prueba**
  `AsignacionUiState` y sus derivados, `alElegir`, `alAsignar` (con `Instant.now()`) y `alNavegarAlDetalle`, más `AsignacionViewModelTest` con los casos de `plan.md` sección 7 (`MainDispatcherRule`, `SesionRepositoryDemo`, `AsignacionRepositoryFalso`).
  RF: RF-605, RF-607, RF-612, RF-616, RF-617
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*AsignacionViewModelTest"` pasa.

- [ ] **T37 · `AsignacionScreen` según el prototipo 06**
  `AsignacionRoute` (`hiltViewModel()`, `collectAsStateWithLifecycle()`) y `AsignacionScreen` (solo dibuja), según `plan.md` sección 5, con las 4 previews en claro y oscuro dentro de `SigtecTheme`.
  RF: RF-601–RF-607
  Hecho cuando: compila, las previews se ven en claro y oscuro, y no hay hexadecimales, `fontSize` ni `Dispatchers` en el archivo.

- [ ] **T38 · Conectar la navegación y el aviso 🔶**
  `CLAVE_AVISO` pasa a `internal` en `IncidenciasNavigation.kt`. `AsignacionNavigation.kt` reemplaza `PantallaEnConstruccion` por `AsignacionRoute`; al asignar, deja «Asignada a <nombre>» en `previousBackStackEntry` y hace `popBackStack()`.
  RF: RF-617
  Hecho cuando: compila y `AsignacionNavigation.kt` ya no importa `PantallaEnConstruccion`.

- [ ] **T39 · «Reasignar técnico» en el detalle 🔶**
  En `DetalleIncidenciaScreen.kt`, el botón dice «Reasignar técnico» si `tecnicoAsignadoId != null`; se ajusta la preview del jefe.
  RF: RF-605
  Hecho cuando: compila y la preview muestra los dos textos.

- [ ] **T40 · Verificación con `adb` en el emulador o celular**
  `./gradlew installDebug`, entrar como `cmendoza`, abrir una incidencia con número y «Asignar técnico». Capturas en el scratchpad (nunca en el repo) en claro y oscuro (`adb shell cmd uimode night yes|no`); revisar `adb logcat *:W`.
  RF: RF-601–RF-607
  Hecho cuando: la pantalla coincide con el prototipo 06 en ambos temas, sin doble margen en la barra superior y sin errores en Logcat.

- [ ] **T41 · Prueba manual — NO automática: asignar y reasignar**
  **Detenerse aquí si no hay celular o emulador.**
  Asignar a un técnico disponible y comprobar el aviso, la marca, el técnico y el movimiento «A <técnico>, por Carlos Mendoza» en el detalle. En la consola, comprobar que la carga del técnico subió 1. Reasignar a otro y comprobar +1 y −1 en la consola, el movimiento «De … a …» y que la marca vuelve a Asignada. Intentar elegir a uno en su límite: no se puede, y se ve el motivo. La reasignación desde Esperando repuesto (RF-625) solo se cubre con pruebas unitarias hasta que la app pueda llegar a esa marca (HU-11).
  RF: RF-603, RF-605, RF-613–RF-617, RF-620, RF-622
  Hecho cuando: todo coincide con la spec y la carga de la consola coincide con el filtro por técnico de la lista.

- [ ] **T42 · Prueba manual — NO automática: sin conexión, sin número y límite en el servidor**
  **Detenerse aquí si no hay celular o emulador.**
  (a) Modo avión: asignar, ver el aviso de cambio por enviar, desactivarlo y verlo subir. (b) Abrir la pantalla con una incidencia «Por sincronizar» y ver el motivo con el botón deshabilitado. (c) Para el límite en el servidor: en la consola, subir la carga de un técnico a su límite sin que baje al celular (modo avión), asignarle y quitar el modo avión. El cambio debe rechazarse, el celular debe mostrar la versión del servidor con el aviso «no se pudo enviar», y el movimiento no debe aparecer en la consola. Al final, dejar la carga como estaba.
  RF: RF-602, RF-607, RF-610, RF-621
  Hecho cuando: los tres casos se comportan como dice la spec.

- [ ] **T43 · Prueba manual — NO automática: llegada al técnico (dos celulares)**
  **Detenerse aquí si no hay dos dispositivos.**
  Con el jefe en un dispositivo y el técnico elegido en otro: tras asignar, en el celular del técnico la incidencia aparece en el filtro por su nombre y su detalle le muestra «Solicitar repuesto» y «Cerrar con informe».
  RF: RF-618
  Hecho cuando: la incidencia llega en segundos sin refrescar nada.

- [ ] **T44 · RF-624 — parte posible antes de la HU-08**
  Confirmar el caso «técnico anterior actualiza tras la reasignación → denegado» del Rules Playground (T23) y que T42 (c) deja el celular con la versión del servidor y sin movimiento huérfano. Anotar en la spec que la prueba completa (con «Iniciar atención») se repite en la HU-08, como se acordó en T03.
  RF: RF-624
  Hecho cuando: la spec dice qué parte de RF-624 quedó probada y cuál pasa a la HU-08.

- [ ] **T45 · Cierre: spec, `MEMORY.md` y PR F**
  En `spec.md`, cambiar «Estado: aprobada» a «Estado: completa», con la nota de RF-624. En `MEMORY.md` (después de `git pull origin main`), actualizar solo la línea de la HU-06: «completa (RF-601 a RF-625)». Hacer los 5 commits de `plan.md` (PR F) y abrir el PR con capturas y las pruebas manuales T40–T44.
  RF: todos
  Hecho cuando: CI en verde, aprobación de Alonso, y el PR se fusiona después de C y D.
