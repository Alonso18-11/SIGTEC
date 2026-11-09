# Plan 013 — Diccionario de fallas con sugerencias (HU-13)

> Basado en `spec.md` (borrador del 2026-11-09, con D-1 a D-4 respondidas) y en `docs/constitution.md`.
> El código vive en `feature/diccionario` (dominio, datos y pantallas) y en el detalle de `feature/incidencias`. Toca estos archivos compartidos:
> - el modelo `FallaDiccionario` y su entidad (sube `version` a 6);
> - `core/sync` (mapeo, subida y escucha);
> - `firebase/firestore.rules` (regla `create` de `fallas`);
> - `core/navigation/Rutas.kt` (ruta nueva `RegistrarFalla`);
> - `feature/captura/domain/TextoDictado.kt` (campo de dictado `SINTOMAS`);
> - `MEMORY.md` y `CLAUDE.md` (pasa de 18 a 19 rutas).
>
> No toca el manifest ni las dependencias. Cada PR trata un solo tema, lleva la HU al final del título y necesita la aprobación de la otra persona del equipo antes de fusionarse.
> Las rutas son relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/` (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas).

## 0. Hallazgos del código real

- **El diccionario ya tiene base, pero sin uso.**
  - `FallaDiccionario(id, titulo, sintomas, componente, tipo, solucion, autorNombre)`, con `tipo: TipoFalla` (`HARDWARE`, `SOFTWARE`, `RED`).
  - `FallaDiccionarioEntity` tiene `pendienteSincronizar`, pero no `actualizadoEn` ni `autorId`.
  - `FallaDiccionarioDao` tiene `observarTodas`, `buscar` (con `LIKE`, que distingue tildes), `guardar`, `guardarTodas` y `pendientesDeSincronizar`.
- **Permisos listos.** `Permiso.REGISTRAR_FALLA` (solo técnico) y `CONSULTAR_DICCIONARIO` (técnico y personal de sistemas) ya existen en `Perfil.kt`; la pestaña «Diccionario» ya se filtra por `CONSULTAR_DICCIONARIO` en `DestinoPrincipal`.
- **Reglas.** `match /fallas/{id}` permite `create` a `esTecnico()` sin validar campos; `update` y `delete` están cerrados.
- **Sincronización.**
  - `SincronizadorIncidencias.subirPendientes` sube incidencias, movimientos, informes y solicitudes, y `hayPendientes` decide si el worker sigue; ahí se agregan las fallas.
  - `SincronizacionTiempoReal` escucha cada colección con `escuchar(nombre, guardar)`; se agrega `escuchar("fallas", ::guardarFallas)`.
  - `debeReemplazarLocal` (en `ReglasSincronizacion.kt`) ya resuelve «no pisar lo local sin subir».
- **Detalle.** `ObservarDetalleIncidencia` ya combina incidencia, movimientos, informe (solo el de Room), evidencias y solicitudes. El informe trae `diagnostico` y `trabajoRealizado` para llenar «Registrar falla».
- **Dictado.** `CampoDictado` tiene `DESCRIPCION`, `DIAGNOSTICO`, `SOLUCION` y `OBSERVACIONES`; el texto vuelve por `savedStateHandle` y el ViewModel lo une con `unirTexto`. Falta `SINTOMAS`.
- **Componentes v2.** `FiltrosChips`, `Tarjeta`, `FabExtendido`, `CampoTexto`, `PestanasSegmentadas`, `BarraAccionInferior` y `EtiquetaEstado` ya existen. El buscador en píldora es privado de `ListaIncidenciasScreen`: se copia su estilo, sin moverlo a `core`.

## 1. División en PR, dependencias y orden de fusión

| PR | Rama | Título | Base | RF |
|---|---|---|---|---|
| PR-01 | `feat/hu13-modelo-falla` | `feat(core): la falla guarda su autor, su origen y su fecha (HU-13)` | `main` | RF-1313, RF-1318, RF-1320 |
| PR-02 | `feat/hu13-reglas-fallas` | `feat(firebase): reglas de creación de fallas (HU-13)` | `main` | RF-1323 |
| PR-03 | `feat/hu13-sync-fallas` | `feat(core): subir y bajar las fallas del diccionario (HU-13)` | PR-01 | RF-1315, RF-1320 a RF-1322 |
| PR-04 | `feat/hu13-dominio-diccionario` | `feat(diccionario): buscar, registrar y sugerir fallas en el dominio (HU-13)` | PR-01 | RF-1304 a RF-1306, RF-1313, RF-1314, RF-1318, RF-1325, RF-1326 |
| PR-05 | `feat/hu13-pantallas-diccionario` | `feat(diccionario): pantallas del diccionario y de registrar falla (HU-13)` | PR-04 | RF-1301 a RF-1315 |
| PR-06 | `feat/hu13-sugerencias-detalle` | `feat(incidencias): fallas parecidas y guardar en el diccionario desde el detalle (HU-13)` | PR-05 | RF-1316 a RF-1319, RF-1324 a RF-1330 |
| PR-07 | `docs/hu13-cierre` | `docs(diccionario): cierre de la HU-13 con las pruebas manuales (HU-13)` | `main` al día | todos |

**Orden de fusión:**
1. PR-01 y PR-02, en paralelo.
2. PR-03 y PR-04, sobre PR-01 (en paralelo).
3. PR-05, sobre PR-04.
4. PR-06, sobre PR-05.
5. PR-07, cuando todo esté en `main`. Después de fusionar PR-02, Alonso publica las reglas en la consola.

## 2. Archivos por PR

Leyenda: 🆕 nuevo, ✏️ modificado, ⚠️ compartido de `CLAUDE.md`, 🔶 sensible fuera de esa lista.

### PR-01 — modelo
- ✏️⚠️ `core/domain/model/FallaDiccionario.kt`: `autorId: String`, `incidenciaOrigenId: String? = null` y `registradaEn: Instant`.
- ✏️⚠️ `core/database/entity/FallaDiccionarioEntity.kt`: las columnas `autorId`, `incidenciaOrigenId` (nullable, con índice) y `actualizadoEn` (la fecha de registro, porque las fallas no se editan), y `toDomain()`/`toEntity()`.
- ✏️ `core/database/dao/FallaDiccionarioDao.kt`: `existeConOrigen(incidenciaId): Flow<Boolean>` y `porId(id)`; se quita `buscar` (el filtro pasa a Kotlin, para ignorar tildes, RF-1304).
- ✏️⚠️ `core/database/SigtecDatabase.kt`: `version = 6`, con el comentario de la HU-13.
- ✏️🔶 `core/sync/MapeoFirestore.kt`: `FallaDiccionarioEntity.toFirestore()` y `DocumentSnapshot.toFallaEntity()` (con `registradaEn` como `Timestamp`, RF-1320).
- 🆕 `androidTest/.../FallaDiccionarioDaoTest.kt`: guardar, ordenar por título, pendientes y `existeConOrigen`.

### PR-02 — reglas
- ✏️⚠️ `firebase/firestore.rules`, en `match /fallas/{id}`:
  - `create` si `esTecnico()`, `autorId == request.auth.uid`, las claves son exactamente las de RF-1320, `titulo`, `sintomas`, `componente` y `solucion` son textos no vacíos, y `tipo` está en `['HARDWARE', 'SOFTWARE', 'RED']`;
  - `update` y `delete` siguen en `false` (D-4).
- En el PR: los casos del Rules Playground (spec, «Criterios de finalización»).

### PR-03 — sincronización
- ✏️🔶 `core/sync/SincronizadorIncidencias.kt`: inyecta `FallaDiccionarioDao`; `subir(falla)` con `set` en `fallas/{id}` y, al confirmarse, `pendienteSincronizar = false`; lo suma a `subirPendientes` y a `hayPendientes`.
- ✏️🔶 `core/sync/SincronizacionTiempoReal.kt`: `escuchar("fallas", ::guardarFallas)`; `guardarFallas` guarda cada documento salvo si la local tiene cambios sin subir y no es más antigua (`debeReemplazarLocal`).
- 🆕 `test/.../core/sync/MapeoFallaTest.kt`: ida y vuelta del mapeo, con y sin origen. (Con `Timestamp` en la JVM: si la clase de Firebase no se inicializa, se prueba solo el mapa de subida, como en las demás pruebas de mapeo.)

### PR-04 — dominio
- 🆕 `feature/diccionario/domain/DiccionarioRepository.kt`: `observarTodas()`, `existeConOrigen(incidenciaId)` y `guardar(falla): Resultado<Unit>`.
- 🆕 `feature/diccionario/data/DiccionarioRepositoryImpl.kt` y 🆕 `feature/diccionario/di/DiccionarioModule.kt` (`@Binds`), con `@IoDispatcher` y el `catch` de la convención.
- 🆕 `feature/diccionario/domain/BuscarFallas.kt`: funciones puras `normalizar(texto)`, `filtrarFallas(fallas, texto, tipo)` (RF-1304, RF-1305).
- 🆕 `feature/diccionario/domain/FallasParecidas.kt`: función pura `fallasParecidas(descripcion, fallas, maximo = 3)` con las palabras vacías, los plurales y los pesos de RF-1325 (la rareza es `ln(1 + N / fallasConLaPalabra)`).
- 🆕 `feature/diccionario/domain/ObservarDiccionario.kt`: `VerificarPermiso(CONSULTAR_DICCIONARIO)` y la lista, más si la persona puede registrar (RF-1308, RF-1309).
- 🆕 `feature/diccionario/domain/RegistrarFalla.kt`: `VerificarPermiso(REGISTRAR_FALLA)`, valida los 5 campos, recorta, arma la falla con el autor de la sesión, `UUID`, `ahora` y el origen, y guarda (RF-1313, RF-1314, RF-1318).
- ✏️⚠️ `core/domain/Resultado.kt` y ✏️⚠️ `core/ui/MensajesError.kt`: solo si hace falta un error nuevo (por ejemplo `FallaIncompleta`); si `CamposObligatorios` ya existe, se reutiliza.
- 🆕 Pruebas: `FallasParecidasTest`, `BuscarFallasTest`, `RegistrarFallaTest`, `ObservarDiccionarioTest`, y el doble `DiccionarioRepositoryFalso`.

### PR-05 — pantallas
- ✏️⚠️ `core/navigation/Rutas.kt`: `@Serializable data class RegistrarFalla(val incidenciaOrigenId: String? = null) : Ruta`.
- ✏️🔶 `feature/captura/domain/TextoDictado.kt`: `CampoDictado.SINTOMAS`.
- 🆕 `feature/diccionario/presentation/DiccionarioViewModel.kt` y `DiccionarioScreen.kt` (`DiccionarioRoute` + `DiccionarioScreen`), con el aviso «Falla registrada en el diccionario» como evento de un solo uso.
- 🆕 `feature/diccionario/presentation/RegistrarFallaViewModel.kt` y `RegistrarFallaScreen.kt`: lee el origen con `toRoute`, llena el formulario desde la incidencia y su informe (RF-1317), dicta síntomas y solución.
- ✏️ `feature/diccionario/presentation/DiccionarioNavigation.kt`: reemplaza `PantallaEnConstruccion`, registra `RegistrarFalla` y el retorno del dictado; `diccionarioGraph(navController)`.
- ✏️⚠️ `core/navigation/SigtecRaiz.kt`: pasa `navController` a `diccionarioGraph`.
- 🆕 Pruebas: `DiccionarioViewModelTest` y `RegistrarFallaViewModelTest`.
- Para llenar el formulario desde una incidencia, `RegistrarFallaViewModel` usa `ObservarDetalleIncidencia` de `feature/incidencias` (lectura; no se duplica).

### PR-06 — detalle
- ✏️ `feature/incidencias/presentation/DetalleIncidenciaViewModel.kt`: combina el diccionario (vía `DiccionarioRepository`) y calcula `fallasParecidas` en `Dispatchers.Default` (inyectado), solo si la incidencia está Pendiente y la persona tiene `CONSULTAR_DICCIONARIO`; expone `fallasParecidas`, `puedeGuardarEnDiccionario` y `yaEnDiccionario`.
- ✏️ `feature/incidencias/presentation/DetalleIncidenciaScreen.kt`: sección «Fallas parecidas» (RF-1324 a RF-1327) y, en la sección del informe, «Guardar en el diccionario» o «Ya está en el diccionario» (RF-1316, RF-1319).
- ✏️ `feature/incidencias/presentation/IncidenciasNavigation.kt`: «Guardar en el diccionario» navega a `Ruta.RegistrarFalla(incidenciaId)`.
- ✏️ `test/.../DetalleIncidenciaViewModelTest.kt`: sugerencias para técnico, ninguna para jefe ni en Solucionada, y «Ya está en el diccionario».

### PR-07 — cierre
- ✏️ `specs/013-diccionario-fallas/*`: estado «completa» y lo probado.
- ✏️ `MEMORY.md` (línea de la HU-13) y ✏️ `CLAUDE.md` («las 19 rutas»).

## 3. Diseño del parecido (RF-1325)

```
normalizar("La impresora NO imprime; luces rojas") = ["impresora", "imprime", "luz", "roja"]
```
1. Minúsculas, quitar tildes (`Normalizer` NFD sin marcas), cambiar todo lo que no sea letra o número por espacio.
2. Partir en palabras; descartar las de menos de 3 letras y las palabras vacías (una lista fija de unas 60 en español).
3. Plurales: `-ces` → `-z`; `-es` → quitar si queda una palabra de 4 letras o más terminada en consonante; `-s` → quitar si queda de 4 o más.
4. Para la incidencia: el conjunto de palabras de la descripción. Para cada falla: los conjuntos del título, el componente y los síntomas.
5. `rareza(p) = ln(1 + N / fallasQueContienen(p))`, con `N` = fallas del diccionario.
6. `puntaje(falla) = Σ p ∈ descripción: rareza(p) × (3·[p ∈ título] + 2·[p ∈ componente] + 1·[p ∈ síntomas])`.
7. Se quedan las fallas con puntaje > 0, ordenadas por puntaje descendente y luego por título; las primeras `maximo`.

Con 500 fallas de 50 palabras el cálculo es lineal (unas 25 000 comprobaciones en conjuntos) y tarda milisegundos; la prueba de RF-1306 lo verifica con un margen amplio.

## 4. Riesgos
- **Pesos poco afinados:** las sugerencias pueden parecer raras con pocas fallas. Se prueba con las fallas del prototipo y se ajusta en PR-04 si hace falta; la HU-14 puede mejorarlo con Gemini.
- **Migración destructiva:** al instalar la versión 6 se pierde lo no sincronizado; se avisa en PR-01 y se instala con todo subido.
- **Reglas no publicadas:** sin publicar PR-02, las fallas sin `autorId` o con campos extra igual se aceptarían; con las reglas viejas no hay error en la app. Se publica tras fusionar.
- **Detalle más pesado:** el detalle ya combina 5 flujos; las sugerencias se calculan aparte y fuera del hilo principal para no frenar la primera pintura.
