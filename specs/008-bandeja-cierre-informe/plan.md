# Plan 008 — Bandeja del técnico y cierre con informe confirmado por huella (HU-08)

> Basado en `spec.md` (aprobada) y `docs/constitution.md`. La HU toca `core/sync`, `firebase/firestore.rules`,
> una entidad de Room y dos módulos de `feature/` (`incidencias`, que se amplía, y `atencion`, que nace), así
> que se entrega en seis PR pequeños, igual que la HU-06. Cada PR tiene su rama, varios commits chicos con la
> HU al final del título, y la aprobación de la otra persona del equipo antes de fusionarse (`CLAUDE.md`,
> «Revisión cruzada»).
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (pruebas instrumentadas, sin subcarpetas:
> así ya viven `IncidenciaDaoTest.kt`, `MovimientoDaoTest.kt` y `CatalogoDaoTest.kt`).

## 0. Adaptaciones de la plantilla y hallazgos del código real

- **`Rutas.kt` ya tiene las tres rutas de esta HU** (`MisTareas`, `RegistrarInforme(incidenciaId)`,
  `SolicitarRepuesto(incidenciaId)`, esta última de la HU-11): no hace falta tocar ese archivo compartido.
  `AtencionNavigation.kt` ya registra las tres con `PantallaEnConstruccion`; esta HU solo reemplaza las dos
  que le tocan (`MisTareas`, `RegistrarInforme`) y deja `SolicitarRepuesto` como está.
- **`IncidenciaDao.observarBandeja(tecnicoId)` ya existe** (comentario «HU-08» en el código), construida de
  antemano para que la HU-06 probara RF-618. RF-801 la reutiliza sin cambios: ningún DAO nuevo para la bandeja.
- **`informes/{incidenciaId}` ya existe en `firestore.rules`**: el documento se llama como la incidencia, no
  como el informe (`InformeTecnicoEntity.id` es un UUID aparte, con índice único sobre `incidenciaId`). La
  sincronización debe subir a `informes.document(informe.incidenciaId)`, no a `informes.document(informe.id)`.
- **Dónde vive cada caso de uso (decisión, ver también D-1):** `IniciarAtencion` se dispara directo desde el
  botón del detalle (RF-806 a RF-811: no abre ninguna pantalla nueva), así que vive en
  `feature/incidencias/domain`, junto a `ObservarDetalleIncidencia`, y amplía `IncidenciaRepository` (el mismo
  patrón que ya usa `RegistrarIncidencia`). `CerrarIncidenciaConInforme` sí abre una pantalla propia
  («Informe técnico», RF-813), así que vive en un `AtencionRepository` nuevo de `feature/atencion`, con su
  propio `@Binds`, igual que `AsignacionRepository` nació aparte para la HU-06 aunque toque las mismas tablas.
- **Ningún `ViewModel` existente de `feature/incidencias` tiene pruebas JVM todavía** (`RegistroIncidenciaViewModel`,
  `ListaIncidenciasViewModel` y `DetalleIncidenciaViewModel` no tienen test): esta HU no corrige esa deuda de
  otras HU, pero sí agrega prueba a todo lo que crea o amplía (constitución, principio 4).
  `DetalleIncidenciaViewModelTest.kt` se crea aquí por primera vez, solo con los casos nuevos de esta HU.
- **«`ahora`/`confirmadoEn` como parámetro»:** ningún caso de uso llama a `Instant.now()`; lo pasa quien lo
  invoca (el `ViewModel`, o el `ResultadoBiometrico.Exito.instante` que entrega `rememberVerificadorBiometrico()`).
- **Ámbito de los commits:** `atencion` ya está en la lista de `CLAUDE.md`; los cambios de `core/sync` usan
  `core` y el de reglas usa `firebase`, igual que la HU-06.
- **Room:** `SigtecDatabase.version` está en 2 hoy; esta HU la sube a 3 por los dos campos nuevos de
  `InformeTecnicoEntity` (`diagnostico`, `necesitaAtencion`), con `fallbackToDestructiveMigration` (solo
  desarrollo, se avisa en el PR A, igual que el salto de 1 a 2 en la HU-05).
- **No se descarga el informe de otros celulares en esta HU.** RF-827 amplía `ObservarDetalleIncidencia` para
  leer el informe que ya está en Room; si el informe se creó en *este* celular (quien cerró), aparece de
  inmediato. Si otra persona abre el detalle de la misma incidencia en *otro* celular, no lo verá hasta que
  exista un oyente de `/informes` que lo baje — eso es trabajo de la HU-16 («cuando lea estos mismos datos ya
  sincronizados», spec, «Fuera de alcance»), no de esta HU. No es una contradicción con RF-827/RF-829: la
  pieza que esta HU construye (leer de Room) queda lista para que la HU-16 solo agregue la descarga.

## 1. División en PR, dependencias y orden de fusión

| PR | Rama | Título del PR | Sale de | Depende de | Puede ir en paralelo con |
|---|---|---|---|---|---|
| A | `feat/hu08-migracion-iniciar-atencion` | `feat(incidencias): iniciar la atención y migración del informe técnico (HU-08)` | `main` | — | — |
| B | `feat/hu08-atencion-dominio` | `feat(atencion): cerrar con informe y la bandeja del técnico (HU-08)` | `main` con A | A | C |
| C | `feat/sync-cierre-informe` | `feat(core): restar la carga al cerrar y sincronizar el informe técnico (HU-08)` | `main` con A | A | B |
| D | `feat/reglas-cierre-informe` | `feat(firebase): exigir la resta de carga al cerrar y el diagnóstico del informe (HU-08)` | rama de C (**apilada**) | A, C | B |
| E | `feat/hu08-pantallas` | `feat(atencion): pantallas «Mis tareas» e «Informe técnico» (HU-08)` | rama de B (**apilada**) | A, B | D |
| F | `docs/hu08-cierre` | `docs(atencion): cierre de la HU-08 con las pruebas de punta a punta (HU-08)` | rama de E (**apilada**) | todos | — |

**Orden de fusión:** A → B y C en cualquier orden → D (después de C) → E (después de A y B; puede desarrollarse
en paralelo con C/D, pero el flujo completo de principio a fin solo se prueba a mano después de D) → F (último).

**Por qué este orden:**
- A es la base de todo: sin `diagnostico` en el informe ni `MarcaCambiada` en `ErrorDominio`, ningún caso de
  uso nuevo compila. Como la HU-06, el primer PR no tiene nada visible (se prueba solo con JUnit).
- B y C dependen solo de A y no comparten ningún archivo entre sí (B es `feature/atencion`; C es `core/sync`
  y `core/database/dao`), así que pueden avanzar en paralelo, igual que C y E en la HU-06.
- D va apilada sobre C porque la regla de Firestore debe coincidir exactamente con lo que la transacción del
  cliente espera (`cambiosDeCarga` ampliada): revisar la regla sin ver primero el código de C sería adivinar.
- E necesita A (para el botón «Iniciar atención» y la sección del informe en el detalle) y B (para
  `CerrarIncidenciaConInforme` y `AtencionRepository`). No necesita C ni D para compilar ni para las pruebas
  unitarias (el repositorio de `feature/atencion` solo escribe en Room), pero sin D en el servidor la carga
  del técnico quedaría desfasada al cerrar — la misma razón por la que la HU-06 fusionó su pantalla (F) al
  final, después de su PR de carga (D).
- F es el único PR con las pruebas manuales de punta a punta (RF-624/RF-825 con dos celulares) y el cierre de
  la spec; necesita que todo lo demás esté fusionado.
- Si A sigue en revisión cuando se empiece B, C o E, sus ramas salen de la de A (ramas apiladas) y se rebasan
  sobre `main` cuando A se fusione — igual que en la HU-06.

## 2. Archivos y commits por PR

Leyenda: 🆕 se crea · ✏️ se modifica · ⚠️ compartido de `CLAUDE.md`, **ya autorizado para esta HU** (no hace
falta volver a preguntar) · 🔶 sensible fuera de esa lista (`core/sync`, DAO usados por otros módulos,
archivos de otro módulo `feature/`) · sin marca: archivo propio del módulo de la HU, sin restricción especial.

### PR A — migración del informe e «Iniciar atención»

| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️⚠️ `core/domain/model/InformeTecnico.kt` | Nuevo campo `diagnostico: String`, antes de `trabajoRealizado` (mismo orden que RF-813 y RF-827). | RF-813, RF-827 |
| ✏️⚠️ `core/database/entity/InformeTecnicoEntity.kt` | Mismo campo, más `necesitaAtencion: Boolean = false` (lo usa C); `toDomain()`/`toEntity()` actualizados. | RF-813, RF-825 (base) |
| ✏️⚠️ `core/database/SigtecDatabase.kt` | `version = 3`. | RNF persistencia local |
| ✏️⚠️ `core/domain/Resultado.kt` | `data object MarcaCambiada : ErrorDominio`. | RF-808, RF-812 |
| ✏️⚠️ `core/ui/MensajesError.kt` | `"Esta incidencia ya cambió de estado. Vuelve a abrirla para ver su situación actual."` | RF-808, RF-812 |
| ✏️ `feature/incidencias/domain/IncidenciaRepository.kt` | `suspend fun porId(id): Incidencia?`, `suspend fun iniciarAtencion(incidencia, movimiento, ahora): Resultado<Unit>`, `fun observarInforme(incidenciaId): Flow<InformeTecnico?>`. | RF-807–RF-810, RF-828 |
| ✏️ `feature/incidencias/data/IncidenciaRepositoryImpl.kt` | Implementa los tres; inyecta `InformeTecnicoDao` (solo lectura). | RF-807–RF-810, RF-828 |
| 🆕 `feature/incidencias/domain/IniciarAtencion.kt` | Caso de uso (pseudocódigo 4.1) con `bloqueoParaIniciar` como función pura. | RF-806–RF-811 |
| ✏️ `feature/incidencias/domain/ObservarDetalleIncidencia.kt` | `DetalleIncidencia` agrega `informe: InformeTecnico?`, combinando `repo.observarInforme(id)`. | RF-827, RF-828, RF-829 |
| ✏️ (test) `app/src/test/.../feature/incidencias/data/IncidenciaRepositoryFalso.kt` | `porId`, `iniciarAtencion`, `observarInforme` en memoria, con `fallar`. | — |
| 🆕 (test) `app/src/test/.../feature/incidencias/domain/IniciarAtencionTest.kt` | Casos de la sección 7. | RF-806–RF-811 |
| 🆕 (test) `app/src/test/.../feature/incidencias/domain/ObservarDetalleIncidenciaTest.kt` | Con informe, sin informe, incidencia inexistente. | RF-827, RF-828 |

- **Compartidos ⚠️ (ya autorizados):** los cinco de la tabla. **Room:** `version` 2 → 3, `fallbackToDestructiveMigration`
  (se avisa en el PR: borra lo no sincronizado en los celulares de prueba).
- **Cómo probarlo:** sin cambio visible (el botón y la sección del informe llegan en E); se indica así en la
  descripción del PR, igual que hizo la HU-06 con su PR E.
- **Commits:**
  1. `docs(atencion): plan y tareas de la bandeja y el cierre con informe (HU-08)`
  2. `docs(asignacion): corregir la frase de tareas ya resueltas en la HU-06 (HU-08)`
  3. `feat(core): diagnóstico en el informe técnico y version 3 de Room (HU-08)`
  4. `feat(core): error MarcaCambiada y su mensaje (HU-08)`
  5. `feat(incidencias): IniciarAtencion verifica el permiso, el técnico asignado y la marca vigente (HU-08)`
  6. `feat(incidencias): el detalle observa también el informe técnico de la incidencia (HU-08)`

### PR B — dominio y datos de «cerrar con informe» y la bandeja

| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/atencion/domain/AtencionRepository.kt` | Interfaz: `incidencia(id)`, `observarBandeja(tecnicoId)`, `cerrarConInforme(incidencia, movimiento, informe, ahora)`. | RF-801, RF-819 |
| 🆕 `feature/atencion/domain/ObservarBandeja.kt` | Combina `ObservarUsuarioActual` con `repo.observarBandeja(usuario.id)`; lista vacía sin sesión. | RF-801, RF-803, RF-805 |
| 🆕 `feature/atencion/domain/CerrarIncidenciaConInforme.kt` | Caso de uso (pseudocódigo 4.2), con `bloqueoParaCerrar` y `recortarCampos` como funciones puras. | RF-812–RF-820 |
| 🆕 `feature/atencion/data/AtencionRepositoryImpl.kt` | Solo Room, `db.withTransaction`, `@IoDispatcher`, `catch` con `Log.w` (pseudocódigo 4.3). | RF-819, RF-824 |
| 🆕 `feature/atencion/di/AtencionModule.kt` | `@Binds` del repositorio. | — |
| 🆕 (test) `app/src/test/.../feature/atencion/data/AtencionRepositoryFalso.kt` | En memoria, con `fallar`. | — |
| 🆕 (test) `app/src/test/.../feature/atencion/domain/CerrarIncidenciaConInformeTest.kt` | Casos de la sección 7. | RF-812–RF-820 |
| 🆕 (test) `app/src/test/.../feature/atencion/domain/ObservarBandejaTest.kt` | Ordenada, vacía, sin sesión. | RF-801, RF-803, RF-805 |

- **Compartidos:** ninguno. **Room:** sin cambios de entidad (ya hechos en A).
- **Cómo probarlo:** sin cambio visible (llega en E).
- **Commits:**
  1. `feat(atencion): AtencionRepository y ObservarBandeja con la cuenta del técnico (HU-08)`
  2. `feat(atencion): CerrarIncidenciaConInforme recorta los campos y valida la marca (HU-08)`
  3. `feat(atencion): repositorio en Room con la transacción de cierre y su @Binds (HU-08)`

### PR C — restar la carga al cerrar y sincronizar el informe (apilable en paralelo con B)

| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️🔶 `core/sync/ReglasSincronizacion.kt` | `cambiosDeCarga` gana dos parámetros (`cerradaServidor`, `cerradaLocal`): generaliza RF-620 y RF-822 en una sola regla (sección 3.1). | RF-822 |
| ✏️🔶 `core/sync/SincronizadorIncidencias.kt` | `actualizarConCarga` pasa la marca de antes y de después; nuevo `subir(informe: InformeTecnicoEntity)`; `subirPendientes` recorre `informeTecnicoDao.listosParaSubir()`; `rechazar` marca también los informes de la incidencia; `hayPendientes` los cuenta (sección 4.5). | RF-822, RF-824, RF-825 |
| ✏️🔶 `core/database/dao/InformeTecnicoDao.kt` | `listosParaSubir()` (igual que `MovimientoDao`, análoga pero sin la rama de «sin número»: un informe nunca existe sin que su incidencia ya tenga número, porque solo se cierra algo ya asignado), `marcarSincronizado(id)`, `marcarNecesitaAtencion(id)` (fallo propio), `marcarNecesitaAtencionDe(incidenciaId)` (fallo del padre), `borrarRechazados()`. | RF-825 |
| ✏️🔶 `core/sync/MapeoFirestore.kt` | `InformeTecnicoEntity.aFirestore(): Map<String, Any?>` con `diagnostico`. | RF-825, RNF trazabilidad |
| ✏️🔶 `core/sync/EstadoDatosRepositoryImpl.kt` | `descartarRechazados()` también llama `informeTecnicoDao.borrarRechazados()`. | RF-825 |
| ✏️ (test) `core/sync/ReglasSincronizacionTest.kt` | Casos nuevos de `cambiosDeCarga` (sección 7). | RF-822 |
| 🆕 (androidTest) `app/src/androidTest/.../InformeTecnicoDaoTest.kt` | `listosParaSubir` con padre pendiente, rechazado y sincronizado; `marcarNecesitaAtencionDe`; `borrarRechazados`. | RF-825 |

- **Compartidos:** ninguno de la lista oficial de `CLAUDE.md` (los archivos `core/sync` y los DAO ya se tratan
  como sensibles por costumbre del equipo desde la HU-06, no porque estén en esa lista literal).
  **Room:** sin cambio de entidad (ya está en A); dos consultas nuevas en `InformeTecnicoDao`.
- **Cómo probarlo:** la transacción real y la escucha no se pueden probar sin la pantalla de E; queda para F,
  igual que la HU-06 dejó su transacción real para T41/T42.
- **Commits:**
  1. `feat(core): cambiosDeCarga también resta al cerrar una incidencia (HU-08)`
  2. `feat(core): subir la incidencia cerrada resta la carga del técnico en la misma transacción (HU-08)`
  3. `feat(core): el informe técnico espera a que su incidencia suba, igual que los movimientos (HU-08)`
  4. `feat(core): descartar los informes rechazados junto con sus incidencias (HU-08)`

### PR D — reglas de Firestore (apilado sobre C)

| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️⚠️ `firebase/firestore.rules` | (1) `marcaAntes()`/`marcaDespues()`/`seCierra()`/`cargaRestadaAlCerrar()`; la rama del técnico en `incidencias.update` exige la resta cuando `seCierra()`. (2) `informes.create` exige `diagnostico is string && diagnostico.size() > 0`. | RF-823, RNF trazabilidad |

- **Compartido ⚠️ (ya autorizado):** `firebase/firestore.rules`. La publica a mano el usuario o Alonso, después
  de probarla en el Rules Playground y de verificar en la consola que `incidenciasActivas` de cada técnico
  sigue coincidiendo con sus incidencias pendientes; ningún agente la publica (igual que la HU-06).
- **Room:** sin cambios.
- **Commits:**
  1. `feat(firebase): exigir la resta de carga al cerrar y el diagnóstico del informe (HU-08)`
  2. `docs(atencion): reglas publicadas y verificadas en consola (HU-08)`

### PR E — pantallas (apilado sobre B)

| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/atencion/presentation/MisTareasViewModel.kt` | `MisTareasUiState`, reloj de un minuto (patrón de `MEMORY.md`), combina `ObservarBandeja`. | RF-801, RF-803, RF-805 |
| 🆕 `feature/atencion/presentation/MisTareasScreen.kt` | `MisTareasRoute` + `MisTareasScreen`, tarjetas con `EtiquetaIncidencia`, mensaje de bandeja vacía, previews claro/oscuro. | RF-801–RF-804 |
| 🆕 `feature/atencion/presentation/InformeTecnicoViewModel.kt` | `InformeTecnicoUiState` (cuatro campos, `guardando`, `error`, `disponibilidadBiometrica`, evento `cerrada`), `alConfirmarCierre(confirmadoEn)` recibido ya con la huella validada. | RF-813–RF-820 |
| 🆕 `feature/atencion/presentation/InformeTecnicoScreen.kt` | `InformeTecnicoRoute` (pide `rememberVerificadorBiometrico()` y llama al `ViewModel` solo si hay éxito) + `InformeTecnicoScreen`, previews claro/oscuro. | RF-813–RF-817 |
| ✏️🔶 `feature/atencion/presentation/AtencionNavigation.kt` | Reemplaza las dos `PantallaEnConstruccion` de `MisTareas` y `RegistrarInforme`; `SolicitarRepuesto` sigue igual (HU-11). | RF-801, RF-813 |
| ✏️🔶 `feature/incidencias/presentation/DetalleIncidenciaViewModel.kt` | `puedeIniciarAtencion` (marca == Asignada, técnico asignado, permiso), `iniciandoAtencion`, `errorAtencion`, `alIniciarAtencion()`. | RF-806–RF-811 |
| ✏️🔶 `feature/incidencias/presentation/DetalleIncidenciaScreen.kt` | Botón «Iniciar atención» en `BarraAcciones`; sección «Informe técnico» (diagnóstico, solución, piezas, observaciones, quién y cuándo) cuando `incidencia.estado == SOLUCIONADO` y hay informe. | RF-806, RF-827 |
| 🆕 (test) `app/src/test/.../feature/atencion/presentation/MisTareasViewModelTest.kt` | Casos de la sección 7. | RF-801, RF-803, RF-805 |
| 🆕 (test) `app/src/test/.../feature/atencion/presentation/InformeTecnicoViewModelTest.kt` | Casos de la sección 7, con `MainDispatcherRule`. | RF-813–RF-820 |
| 🆕 (test) `app/src/test/.../feature/incidencias/presentation/DetalleIncidenciaViewModelTest.kt` | Solo los casos nuevos de esta HU (ver nota de la sección 0). | RF-806–RF-811 |

- **Compartidos:** ninguno de la lista oficial (`Rutas.kt` no cambia, ver sección 0). **Sensibles 🔶:**
  `AtencionNavigation.kt` y los dos archivos de `DetalleIncidenciaScreen`/`ViewModel`, que son de la HU-04
  ampliados por esta HU (permitido explícitamente por RF-828 para `ObservarDetalleIncidencia`, y por simetría
  para su pantalla y su `ViewModel`).
- **Room:** sin cambios.
- **Commits:**
  1. `feat(atencion): pantalla «Mis tareas» con la bandeja ordenada por antigüedad (HU-08)`
  2. `feat(atencion): pantalla «Informe técnico» con la confirmación biométrica (HU-08)`
  3. `feat(incidencias): botón «Iniciar atención» en el detalle (HU-08)`
  4. `feat(incidencias): el detalle muestra el informe técnico de una incidencia cerrada (HU-08)`

### PR F — verificación de punta a punta y cierre (apilado sobre E)

Sin archivos de código nuevos; solo pruebas manuales (sección 7) y:
| Archivo | Cambio |
|---|---|
| ✏️ `specs/008-bandeja-cierre-informe/spec.md` | «Estado: borrador» → «Estado: completa», con la fecha y qué se probó. |
| ✏️ `MEMORY.md` | Línea de la HU-08: «completa (RF-801 a RF-829)», después de `git pull origin main`. |

- **Commits:** `docs(atencion): cierre de la HU-08 con las pruebas de punta a punta (HU-08)`

## 3. Funciones puras de lógica

| # | Firma (dónde) | Regla | RF |
|---|---|---|---|
| 3.1 | `cambiosDeCarga(tecnicoServidor: String?, cerradaServidor: Boolean, tecnicoLocal: String?, cerradaLocal: Boolean): Map<String, Int>` (`ReglasSincronizacion`, ampliada) | `activoServidor = tecnicoServidor?.takeUnless { cerradaServidor }`; `activoLocal` igual con los locales. Si son iguales, mapa vacío; si no, `activoServidor → -1` (si no es `null`) y `activoLocal → +1` (si no es `null`). Generaliza en una sola regla el cambio de técnico (RF-620, HU-06) y el cierre (RF-822): cerrar es «el técnico deja de estar activo» sin cambiar de id. | RF-620 (ya probado), RF-822 |
| 3.2 | `bloqueoParaIniciar(incidencia: Incidencia, tecnicoId: String): ErrorDominio?` (`IniciarAtencion.kt`) | `incidencia.tecnicoAsignadoId != tecnicoId` → `SinPermiso` (RF-807); si no, `incidencia.marca != ASIGNADA` → `MarcaCambiada` (RF-808); si no, `null`. | RF-807, RF-808 |
| 3.3 | `bloqueoParaCerrar(incidencia: Incidencia, tecnicoId: String): ErrorDominio?` (`CerrarIncidenciaConInforme.kt`) | Mismo primer caso que 3.2 (`SinPermiso`); si no, `incidencia.marca !in {ASIGNADA, EN_ATENCION, ESPERANDO_REPUESTO}` → `MarcaCambiada` (RF-812). | RF-812 |
| 3.4 | `recortarCampos(diagnostico, trabajoRealizado, piezasCambiadas, observaciones): CamposInforme` (`CerrarIncidenciaConInforme.kt`) | Recorta los cuatro; las dos opcionales que queden vacías pasan a `null`. | RF-818 |
| 3.5 | `tecnicoDesdeDatos` (HU-06, sin cambios) | — | — |

`ReglasSincronizacion.kt` sigue sin importar nada de `android.*` (principio 3). `IniciarAtencion.kt` y
`CerrarIncidenciaConInforme.kt` no son archivos «puros» en sí (tienen `@Inject` y llaman al repositorio, igual
que `AsignarIncidencia.kt`), pero sus funciones 3.2 a 3.4 sí lo son y se prueban por separado, sin mocks.

## 4. Algoritmos en pseudocódigo

### 4.1 `IniciarAtencion(incidenciaId, ahora)` (RF-806 a RF-811)
```
tecnico = verificarPermiso(ATENDER_INCIDENCIA)              // SinPermiso si falla (RF-807)
incidencia = repo.porId(incidenciaId) ?: Desconocido("incidencia no encontrada")
bloqueoParaIniciar(incidencia, tecnico.id)?.let { return Error(it) }   // RF-807, RF-808 (3.2)
actualizada = incidencia.copy(marca = EN_ATENCION)
movimiento = Movimiento(UUID, incidencia.id, ATENCION_INICIADA, tecnico.nombre, ahora)
repo.iniciarAtencion(actualizada, movimiento, ahora) -> Exito(actualizada) | su Error   // RF-809, RF-810
```
`IncidenciaRepositoryImpl.iniciarAtencion` es una transacción de dos escrituras, igual que `registrar` (4.3
de la spec 003): `incidenciaDao.guardar` + `movimientoDao.guardar`, ambos `pendienteSincronizar = true`.

### 4.2 `CerrarIncidenciaConInforme(incidenciaId, diagnostico, trabajoRealizado, piezasCambiadas, observaciones, confirmadoEn)` (RF-812 a RF-820)
```
tecnico = verificarPermiso(ATENDER_INCIDENCIA)
incidencia = repo.incidencia(incidenciaId) ?: Desconocido("incidencia no encontrada")
bloqueoParaCerrar(incidencia, tecnico.id)?.let { return Error(it) }     // RF-812 (3.3)
campos = recortarCampos(diagnostico, trabajoRealizado, piezasCambiadas, observaciones)   // RF-818 (3.4)
si campos.diagnostico.isEmpty() || campos.trabajoRealizado.isEmpty(): return Error(CampoObligatorio)
informe = InformeTecnico(UUID, incidencia.id, campos.diagnostico, campos.trabajoRealizado,
                          campos.piezasCambiadas, campos.observaciones,
                          fechaCierre = confirmadoEn, confirmadoEn = confirmadoEn)
actualizada = incidencia.copy(marca = CERRADA)
movimiento = Movimiento(UUID, incidencia.id, CERRADA, tecnico.nombre, confirmadoEn)
repo.cerrarConInforme(actualizada, movimiento, informe, confirmadoEn) -> Exito(actualizada) | su Error  // RF-819, RF-820
```
`confirmadoEn` llega ya validado por `rememberVerificadorBiometrico()` (RF-815); el caso de uso nunca pide la
huella ni sabe que existe, solo recibe el instante (principio 3: la lógica biométrica de Android queda en
`core/biometric`, fuera del dominio).

### 4.3 `AtencionRepositoryImpl.cerrarConInforme` (RF-819)
```
withContext(io) {
  try {
    db.withTransaction {
      incidenciaDao.guardar(actualizada.toEntity(pendienteSincronizar = true, actualizadoEn = ahora))
      movimientoDao.guardar(movimiento.toEntity(pendienteSincronizar = true))
      informeTecnicoDao.guardar(informe.toEntity(pendienteSincronizar = true))
    }
    Exito(Unit)
  } catch (CancellationException) { relanzar }
    catch (e) { Log.w(TAG, "Error al cerrar la incidencia ${incidencia.id}", e); Error(Desconocido(e.message)) }
}
```
Los tres o ninguno, en la misma transacción (RF-819); nunca falta el informe, el movimiento o el cambio de marca.

### 4.4 Carga al cerrar, ampliando la transacción de la HU-06 (RF-822)
```
actualizarConCarga(local):
    remotaSnap = tx.get(incidencias/{local.id})
    cambios = cambiosDeCarga(
        remotaSnap.getString("tecnicoAsignadoId"), remotaSnap.getString("marca") == "CERRADA",
        local.tecnicoAsignadoId, local.marca == CERRADA
    )                                                         // 3.1
    ... (perfiles, cargaValida y las escrituras, igual que en la HU-06; ver plan 006, sección 4.6)
```
Si la incidencia no cambia de técnico ni se cierra, `cambios` sigue vacío y la transacción solo guarda la
incidencia, como siempre. Si se cierra sin cambiar de técnico, `activoServidor` y `activoLocal` difieren solo
en que el local ya no cuenta (`cerradaLocal = true`), así que el único cambio es `tecnicoAsignadoId → -1`.

### 4.5 El informe espera a que su incidencia suba (RF-825, RNF «Sincronización de informes»)
Hoy `InformeTecnicoDao` solo tiene `pendientesDeSincronizar()` (sin esperar al padre); este mecanismo es
nuevo, construido igual que el de `MovimientoDao` (plan 006, sección 4.8, punto 1), pero sin la rama de
«incidencia sin número» de `MovimientoDao.borrarRechazados()`: cerrar exige que la incidencia ya esté
Asignada o más adelante (RF-812), y eso ya exige que tenga número (RF-610, HU-06); un informe nunca puede
pertenecer a una incidencia que no llegó a existir en el servidor.
```
InformeTecnicoDao.listosParaSubir():
SELECT inf.* FROM informes_tecnicos inf LEFT JOIN incidencias i ON i.id = inf.incidenciaId
WHERE inf.pendienteSincronizar = 1
  AND (i.id IS NULL OR (i.pendienteSincronizar = 0 AND i.necesitaAtencion = 0))

SincronizadorIncidencias.subirPendientes(ahora):
    para cada incidencia pendiente: subir(incidencia)
    para cada movimiento en movimientoDao.listosParaSubir(): subir(movimiento)
    para cada informe en informeTecnicoDao.listosParaSubir(): subir(informe)        // NUEVO
    ...

subir(informe: InformeTecnicoEntity):
    documento = firestore.collection("informes").document(informe.incidenciaId)    // id = incidenciaId, no informe.id
    si !documento.get().exists(): documento.set(informe.aFirestore())
    informeTecnicoDao.marcarSincronizado(informe.id)
    -> igual que subir(movimiento): permanente marca necesitaAtencion sobre sí mismo; pasajero reintenta

rechazar(local: IncidenciaEntity):                       // ampliado (antes solo marcaba movimientos)
    marcada = incidenciaDao.marcarNecesitaAtencion(local.id, local.actualizadoEn) > 0
    si marcada:
        movimientoDao.marcarNecesitaAtencionDe(local.id)
        informeTecnicoDao.marcarNecesitaAtencionDe(local.id)    // NUEVO
```
Por qué el orden importa (RF-825): si el jefe reasignó y esa escritura llega primero al servidor, la
actualización del técnico anterior a marca Cerrada se **rechaza en la regla** (`tecnicoAntes() != request.auth.uid`
ya no se cumple, sección 4.6). Eso hace que la incidencia entre a `rechazar(...)`, que marca su movimiento
`CERRADA` y su informe como `necesitaAtencion`: ninguno de los dos llega a subir, y `informes/{incidenciaId}`
nunca se crea. El informe nunca se intenta subir antes que su incidencia porque `listosParaSubir()` lo
bloquea mientras la incidencia siga pendiente o marcada.

`EstadoDatosRepositoryImpl.descartarRechazados()` (RF-80 de la HU-05) agrega un paso:
```
db.withTransaction {
    movimientoDao.borrarRechazados()
    informeTecnicoDao.borrarRechazados()          // NUEVO: DELETE FROM informes_tecnicos WHERE necesitaAtencion = 1
    incidenciaDao.borrarRechazadasSinNumero()
    incidenciaDao.quitarMarcaDeAtencion()
}
```

### 4.6 Reglas de Firestore (RF-823, RNF de trazabilidad)
```diff
     function tecnicoAntes()   { return resource.data.get('tecnicoAsignadoId', null); }
     function tecnicoDespues() { return request.resource.data.get('tecnicoAsignadoId', null); }
     function cambiaTecnico()  { return tecnicoDespues() != tecnicoAntes(); }
+    function marcaAntes()    { return resource.data.marca; }
+    function marcaDespues()  { return request.resource.data.marca; }
+    function seCierra() { return marcaAntes() != 'CERRADA' && marcaDespues() == 'CERRADA'; }
+    function cargaRestadaAlCerrar() {
+      return cargaDespuesDe(request.auth.uid) == cargaDe(request.auth.uid) - 1;
+    }
...
       allow update: if (esJefe() && (!cambiaTecnico() || (resource.data.marca != 'CERRADA' && cargaMovida())))
-        || (esTecnico() && tecnicoAntes() == request.auth.uid && !cambiaTecnico());
+        || (esTecnico() && tecnicoAntes() == request.auth.uid && !cambiaTecnico()
+            && (!seCierra() || cargaRestadaAlCerrar()));
```
Y en `match /informes/{incidenciaId}`:
```diff
       allow create: if esTecnico()
         && get(/databases/$(database)/documents/incidencias/$(incidenciaId)).data.tecnicoAsignadoId == request.auth.uid
-        && request.resource.data.confirmadoEn is timestamp;
+        && request.resource.data.confirmadoEn is timestamp
+        && request.resource.data.diagnostico is string && request.resource.data.diagnostico.size() > 0;
```
`cargaRestadaAlCerrar()` es una lectura más de `getAfter()`/`get()` sobre el mismo uid que ya se usaba para
`cargaMovida()`; el peor caso sigue muy por debajo del límite de 10 lecturas de las reglas (HU-06, sección 4.7).

## 5. Cómo se pinta en Compose (PR E)

- **«Mis tareas»** (`Scaffold(contentWindowInsets = WindowInsets(0))`, `TopAppBar(title = "Mis tareas", windowInsets = WindowInsets(0))`,
  sin flecha de volver: es un destino del menú principal, igual que «Incidencias»):
  - `LazyColumn` con las mismas tarjetas que `ListaIncidenciasScreen.TarjetaIncidencia` (código visible o «Por
    sincronizar», equipo, descripción, `EtiquetaIncidencia(marca, critica, diasPendiente)`), sin técnico (ya se
    sabe que son todas del mismo técnico) y sin filtros ni buscador (RF-801, «Fuera de alcance»).
  - RF-803: lista vacía → texto centrado «No tienes incidencias pendientes.» en `bodyLarge`, `onSurfaceVariant`.
  - RF-804: `onClick` de la tarjeta llama a `alAbrir(incidencia.id)`.
- **«Informe técnico»** (`TopAppBar` con flecha de volver):
  - Resumen breve de la incidencia arriba (igual estilo que el resumen de «Asignar incidencia», HU-06).
  - Cuatro `OutlinedTextField`: «Diagnóstico» y «Solución aplicada» (`isError` si están vacíos y se intentó
    confirmar), «Piezas cambiadas» y «Observaciones» (opcionales, sin asterisco).
  - Botón «Confirmar cierre» de ancho completo, deshabilitado mientras `!estado.puedeConfirmar` (RF-814);
    si `disponibilidadBiometrica == SIN_SEGURIDAD`, el botón queda deshabilitado y debajo se explica que debe
    configurar un bloqueo de pantalla (RF-816), en vez de intentar la huella.
  - `onClick`: la `Route` llama a `verificador.verificar(titulo = "Confirma el cierre")`; en
    `ResultadoBiometrico.Exito(instante)` llama a `vm.alConfirmarCierre(instante)`; en `Cancelado` o `Error` no
    llama a nada (RF-817: el formulario queda como estaba).
  - El error, si lo hay, va debajo del botón con `error.mensaje()` en `error` (RF-810/RF-812 reflejados aquí
    como `MarcaCambiada`, o `CampoObligatorio`/`Desconocido`).
  - **Previews (claro y oscuro):** formulario vacío (botón deshabilitado), completo y listo, `SIN_SEGURIDAD`.
- **Detalle de la incidencia** (`DetalleIncidenciaScreen.kt`, ampliado):
  - `BarraAcciones` agrega, antes de «Solicitar repuesto»/«Cerrar con informe», un botón «Iniciar atención»
    cuando `estado.puedeIniciarAtencion`, deshabilitado mientras `estado.iniciandoAtencion` (RF-811); el error,
    si lo hay, se muestra con `error.mensaje()` debajo de la barra.
  - Nueva sección «Informe técnico» en `Contenido(...)`, visible solo si `incidencia.estado == SOLUCIONADO` y
    `detalle.informe != null`: diagnóstico, solución aplicada, piezas cambiadas (si las hay), observaciones (si
    las hay), y una línea «Confirmado por `<nombre del técnico asignado>` · `<fecha y hora de confirmadoEn>`»
    (reutiliza `fechaCompleta(...)`, ya usada en `TarjetaDatos`). Si la incidencia está Cerrada pero el informe
    aún no llegó a este celular (ver sección 0), la sección simplemente no aparece: no es un error, solo no
    hay nada que mostrar todavía.
  - Previews nuevas: «Técnico, en atención con botón de iniciar» y «Cerrada con informe» (claro y oscuro).
- Solo roles de `MaterialTheme.colorScheme`, estilos de `typography` y `Espaciado.*`; área táctil mínima en los
  botones nuevos; nada de hexadecimales ni `fontSize` fuera de `core/theme/`.

## 6. Decisiones técnicas

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D-1 | `IniciarAtencion` en `feature/incidencias/domain` (amplía `IncidenciaRepository`); `CerrarIncidenciaConInforme` en `feature/atencion/domain` (repositorio nuevo). | El primero se dispara desde el propio `DetalleIncidenciaViewModel` sin abrir pantalla (RF-806–RF-811): ponerlo en otro módulo obligaría a que `feature/incidencias` dependiera de `feature/atencion`, invirtiendo la dirección normal (el detalle no depende de la bandeja). El segundo abre su propia pantalla (RF-813) y necesita `InformeTecnico`, un dato nuevo que no le pertenecía a `feature/incidencias`: mismo patrón que `AsignacionRepository` en la HU-06, que nació aparte aunque toca las mismas tablas. | Los dos en `feature/atencion`: el detalle tendría que inyectar un caso de uso de otro módulo. Los dos en `feature/incidencias`: ese módulo terminaría con la lógica de cierre e informe de otra HU completa. |
| D-2 | `cambiosDeCarga` generaliza RF-620 y RF-822 en una sola función con «técnico activo antes/después» (3.1), en vez de una segunda función paralela. | Cerrar es, para la carga, el mismo fenómeno que reasignar: alguien deja de tener la incidencia activa. Una sola regla evita que las dos puedan desincronizarse con el tiempo. | Una función nueva `cambiosDeCargaAlCerrar` aparte: duplicaría la mitad de la lógica de `cambiosDeCarga` y un futuro cambio (por ejemplo, HU-11 reabriendo por repuesto) tendría que tocar dos funciones. |
| D-3 | El mecanismo padre/hijo de `InformeTecnicoDao` se construye igual que el de `MovimientoDao` (plan 006, 4.8), pero sin la rama de «incidencia sin número». | Un informe solo puede existir sobre una incidencia que ya pasó por Asignada (RF-610 ya exige número para asignar), así que ese caso no puede ocurrir; agregarlo sería código muerto sin prueba posible. | Copiar la consulta de `MovimientoDao` tal cual, con la rama de más: pasaría la revisión igual, pero quedaría una condición que ninguna prueba puede ejercitar. |
| D-4 | No se agrega un oyente de `/informes` para bajarlos a otros celulares en esta HU. | RF-827 a RF-829 piden ver el informe «en el propio detalle», y la spec ya anota que la vista de varios técnicos es la HU-16, «cuando lea estos mismos datos ya sincronizados» — frase que ya asume que bajar es trabajo de esa HU, no de esta. `ObservarDetalleIncidencia` ya queda leyendo de Room: cuando la HU-16 agregue la descarga, esta pantalla lo mostrará sin cambios. | Agregar el oyente ahora: toca `SincronizacionTiempoReal.kt` sin que ningún RF de esta spec lo pida, y duplicaría trabajo si la HU-16 decide bajarlos de otra forma (por ejemplo, paginados). |
| D-5 | El documento de Firestore del informe usa `incidenciaId` como id (`informes/{incidenciaId}`), no el `id` propio del informe. | La regla ya existía así (`match /informes/{incidenciaId}`) antes de esta spec; cambiarla sería tocar una decisión ya tomada sin ningún RF que lo pida. Además, un id predecible simplifica `listosParaSubir()`: nunca hay que buscar si ya existe por otro campo. | Subir con `informe.id` y agregar un campo `incidenciaId` dentro del documento para buscarlo: exige una consulta extra o una regla distinta a la que ya está escrita. |
| D-6 | `IniciarAtencion`/`CerrarIncidenciaConInforme` revalidan la marca con una lectura fresca de Room (`repo.porId`/`repo.incidencia`), no con el estado que ya tiene la pantalla. | Es la misma defensa que ya usa `AsignarIncidencia` (D-5 del plan 006) contra el caso raro de RF-808/RF-812: los datos cambiaron entre que se pintó la pantalla y se tocó el botón. | Confiar en el estado de la pantalla: el botón podría seguir visible un instante después de que la marca ya cambió, y el error nunca se detectaría a tiempo. |
| D-7 | `MarcaCambiada` no reintenta ni bloquea el botón de forma permanente: en cuanto el `Flow` de `observarDetalle` reciba la marca real, `puedeIniciarAtencion`/`puedeAtender` se recalculan solos y el botón desaparece. | El propio RF-808 dice que es una carrera «muy breve»: no hace falta un candado adicional en el `ViewModel» cuando el mismo mecanismo reactivo que ya existe (HU-04) resuelve el caso en el siguiente ciclo. | Guardar un flag local permanente «esta incidencia ya no se puede tocar»: complejidad extra para un caso que la spec ya califica de no principal. |
| D-8 | El botón «Confirmar cierre» recibe el `confirmadoEn` de `ResultadoBiometrico.Exito` directo de la `Route`, y el `ViewModel` nunca llama a `rememberVerificadorBiometrico()` ni a `Instant.now()`. | Mismo patrón que `AjustesSeguridadScreen`/`AjustesSeguridadViewModel` (único precedente de biometría hoy): la composición pide la huella, el `ViewModel` solo reacciona al resultado. Mantiene el `ViewModel` con fecha inyectable para pruebas (RNF de la spec). | Que el `ViewModel` dispare la biometría: no puede, porque `VerificadorBiometrico` exige una `FragmentActivity` (`LocalActivity.current`), algo que el dominio/presentación puro no debe tocar. |

## 7. Estrategia de pruebas

`./gradlew testDebugUnitTest`, JUnit 4, coroutines-test, `MainDispatcherRule` y dobles propios
(`SesionRepositoryDemo`, `IncidenciaRepositoryFalso` ampliado, nuevos `AtencionRepositoryFalso`). Sin
dependencias nuevas.

| Prueba | Casos | RF |
|---|---|---|
| `IniciarAtencionTest` (A) | Sin sesión/sin permiso → `SinPermiso`; técnico distinto al asignado → `SinPermiso`; marca ya no es Asignada → `MarcaCambiada`; camino feliz (En atención, movimiento `ATENCION_INICIADA`, `ahora` fijo); Room falla → `Desconocido` y nada guardado. | RF-806–RF-811 |
| `ObservarDetalleIncidenciaTest` (A) | Incidencia con informe (lo incluye); sin informe (`null`); incidencia inexistente (`null` completo). | RF-827, RF-828 |
| `CerrarIncidenciaConInformeTest` (B) | Sin sesión/permiso; técnico distinto al asignado; marca Sin asignar/Cerrada → `MarcaCambiada`; marca Asignada, En atención y Esperando repuesto → los tres permiten cerrar; diagnóstico o solución vacíos tras recortar (incluido solo espacios) → `CampoObligatorio`; piezas/observaciones en blanco → quedan `null`; camino feliz (incidencia Cerrada, movimiento `CERRADA`, informe con `confirmadoEn` fijo); Room falla → nada guardado. | RF-812–RF-820 |
| `ObservarBandejaTest` (B) | Ordenada de la más antigua a la más reciente; vacía; sin sesión → lista vacía. | RF-801, RF-803, RF-805 |
| `ReglasSincronizacionTest` (C) | `cambiosDeCarga` ampliada: cerrar sin cambiar de técnico (-1 al único); cerrar y además sería un cambio de técnico (caso teórico, no alcanzable desde la app pero la función debe responder algo consistente); reabrir (ninguno de los dos marca «cerrada», comportamiento igual al de antes); sin cambios. | RF-822 |
| `MisTareasViewModelTest` (E) | Bandeja ordenada; vacía; el reloj avanza sin recargar los datos (mismo patrón que `AsignacionViewModelTest`, `MEMORY.md`). | RF-801, RF-803, RF-805 |
| `InformeTecnicoViewModelTest` (E) | Botón deshabilitado con campos vacíos, con solo diagnóstico, con solo solución, guardando; `SIN_SEGURIDAD` deshabilita aunque los campos estén completos; `alConfirmarCierre` con éxito emite `cerrada = true`; error (`MarcaCambiada`, `CampoObligatorio`, `Desconocido`) queda en `error` y permite reintentar sin perder el texto. | RF-813–RF-820 |
| `DetalleIncidenciaViewModelTest` (E, nuevo archivo) | `puedeIniciarAtencion` solo en marca Asignada y para el técnico asignado; `alIniciarAtencion` deshabilita el botón mientras guarda y no duplica el movimiento con un segundo toque; error de `IniciarAtencion` queda en `errorAtencion`. | RF-806, RF-807, RF-811 |
| Instrumentadas en celular (`connectedDebugAndroidTest`) | `InformeTecnicoDaoTest` (C): `listosParaSubir` con padre pendiente (no sale), rechazado (no sale), sincronizado (sale); `marcarNecesitaAtencionDe`; `borrarRechazados`. | RF-825 |

**Verificado a mano, no automatizado (dependen de Firestore o `BiometricPrompt` reales):** la huella o el
bloqueo de pantalla real (RF-815 a RF-817, el emulador la simula con `adb -e emu finger touch 1`); la
transacción real de la carga al cerrar y la regla con `getAfter()` (RF-822, RF-823), revisando la consola; las
reglas en el Rules Playground (RF-823, diagnóstico no vacío); el conflicto completo de RF-624 (HU-06) y
RF-825 con dos celulares (sección 8 de `tasks.md`); y las pantallas con `adb` en claro y oscuro.

**Cierre de cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde.

## 8. Cobertura de RF

| RF | Dónde vive | Prueba |
|---|---|---|
| RF-801–RF-803, RF-805 | `ObservarBandeja`, `MisTareasViewModel`, `IncidenciaDao.observarBandeja` (ya existía) | `ObservarBandejaTest`, `MisTareasViewModelTest` + manual |
| RF-804 | `MisTareasScreen` | preview + manual |
| RF-806, RF-807, RF-811 | `DetalleIncidenciaViewModel`, `IniciarAtencion` | `DetalleIncidenciaViewModelTest`, `IniciarAtencionTest` |
| RF-808 | `bloqueoParaIniciar` | `IniciarAtencionTest` |
| RF-809, RF-810 | `IniciarAtencion`, `IncidenciaRepositoryImpl.iniciarAtencion` | `IniciarAtencionTest` |
| RF-812 | `bloqueoParaCerrar` | `CerrarIncidenciaConInformeTest` |
| RF-813, RF-814 | `InformeTecnicoScreen`, `InformeTecnicoUiState.puedeConfirmar` | `InformeTecnicoViewModelTest` + preview |
| RF-815 a RF-817 | `InformeTecnicoScreen`/`Route`, `rememberVerificadorBiometrico()` | manual (huella/PIN, cancelar) |
| RF-818 | `recortarCampos` | `CerrarIncidenciaConInformeTest` |
| RF-819, RF-820 | `CerrarIncidenciaConInforme`, `AtencionRepositoryImpl.cerrarConInforme` | `CerrarIncidenciaConInformeTest` |
| RF-821 | `IncidenciaDao.observarBandeja` (ya existía) + `estado` derivado | manual |
| RF-822 | `cambiosDeCarga` ampliada, `SincronizadorIncidencias.actualizarConCarga` | `ReglasSincronizacionTest` + manual (consola) |
| RF-823 | `firebase/firestore.rules` | Rules Playground (usuario) |
| RF-824 | `AtencionRepositoryImpl` (solo Room), `SincronizadorIncidencias` (solo sync) | revisión de código + manual |
| RF-825 | `InformeTecnicoDao.listosParaSubir`, `SincronizadorIncidencias.rechazar` | `InformeTecnicoDaoTest` + manual (dos celulares, `tasks.md` sección 8) |
| RF-826 | `DetalleIncidenciaViewModel.puedeAtender` (ya existía, HU-06) | manual |
| RF-827, RF-828 | `ObservarDetalleIncidencia`, `IncidenciaRepository.observarInforme` | `ObservarDetalleIncidenciaTest` + preview |
| RF-829 | Alcance (sin código propio: ver «Fuera de alcance» de la spec) | — |

## 9. Respeto a la constitución

1. **Stack cerrado:** sin dependencias nuevas; `libs.versions.toml` no cambia.
2. **Spec antes que código:** `spec.md` ya está aprobada; este plan no la contradice en ningún punto (la única
   duda que apareció al revisar el código real —la descarga de informes a otros celulares— se resuelve como
   decisión técnica D-4, sin necesitar un RF nuevo ni cambiar los que ya existen).
3. **Lógica fuera de la pantalla:** permiso, bloqueos de marca y recorte de campos viven en
   `feature/incidencias/domain` y `feature/atencion/domain` (Kotlin puro en las funciones de la sección 3); las
   `Screen` solo dibujan y llaman lambdas.
4. **Toda regla tiene prueba:** cada función pura, caso de uso, `ViewModel` y consulta nueva tiene prueba
   (sección 7). Lo que depende de Firestore o de `BiometricPrompt` real se verifica a mano.
5. **Datos protegidos:** el cierre exige confirmación biométrica (RF-815, RF-23) y guarda `confirmadoEn`; no se
   relaja ninguna regla existente, solo se agregan condiciones nuevas (RF-823, diagnóstico no vacío).
6. **Español:** clases, funciones, textos y pruebas en español; solo los sufijos de Android (`ViewModel`,
   `Screen`, `Dao`, `Entity`) en inglés.

## 10. Resumen: archivos compartidos y Room por PR (requieren visto bueno)

| PR | Compartidos ⚠️ (ya autorizados para esta HU) | Sensibles 🔶 | Room |
|---|---|---|---|
| A | `InformeTecnico.kt`, `InformeTecnicoEntity.kt`, `SigtecDatabase.kt`, `Resultado.kt`, `MensajesError.kt` | — | `version` 2 → 3, `fallbackToDestructiveMigration` |
| B | ninguno | — | sin cambios |
| C | ninguno de la lista oficial | `core/sync/*`, `InformeTecnicoDao` | solo DAO; `version` sigue en 3 |
| D | `firebase/firestore.rules` | — | sin cambios |
| E | ninguno (`Rutas.kt` ya tenía las rutas) | `AtencionNavigation.kt`, `DetalleIncidenciaViewModel.kt`, `DetalleIncidenciaScreen.kt` (módulo `incidencias`, de otra HU) | sin cambios |
| F | — | — | sin cambios |

Ningún PR toca `libs.versions.toml`, `app/build.gradle.kts`, las entidades que no sean `InformeTecnicoEntity`,
`Rutas.kt`, `SigtecRaiz.kt`, `core/domain/model/*` fuera de `InformeTecnico.kt`, `core/theme/*`, `core/di/*`,
`AndroidManifest.xml` ni `feature/acceso/*`.
