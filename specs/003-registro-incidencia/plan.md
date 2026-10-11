# Plan 003 — Registrar una incidencia con fecha automática (HU-03)

> Basado en `spec.md` y `docs/constitution.md`. RF-30 a RF-40 ya están en `main` desde antes del flujo SDD (PR #7, `fea7683`, ajustes del PR #9, `6420f40`); este plan **no** los vuelve a implementar. Solo cubre tres huecos que la spec aprobada marca como pendientes o como vacíos de prueba:
> 1. `RegistrarIncidencia` recibe `ahora: Instant` como parámetro en vez de llamar a `Instant.now()` internamente (nota de implementación bajo RF-36/RF-33).
> 2. RF-41 (pendiente): validar el código de equipo contra el catálogo sincronizado, lo que exige conectar `core/sync` con la colección `/equipos` que hoy no se descarga.
> 3. `RegistroIncidenciaViewModelTest`, que no existe hoy (RF-30, RF-31, RF-32, RF-40 sin prueba automatizada de ViewModel).
>
> Los puntos 2 y 2.1 en particular tocan `core/sync/*`, `Resultado.kt` y `MensajesError.kt` — archivos compartidos según `CLAUDE.md` (los dos últimos están en su lista textual de «pregunta antes»; `core/sync/*` no está en esa lista literal, pero `CLAUDE.md` lo describe como «el único lugar que habla con Firestore», así que se trata con el mismo cuidado). Ver sección 9.

## 0. Adaptaciones de la plantilla
- **«Algoritmo del mapa»:** no aplica; se usa pseudocódigo puntual en las secciones 1 y 2 para los tres cambios.
- **«`node --test`»:** igual que en los planes 001 y 002, se usa JUnit 4 y coroutines-test con `./gradlew testDebugUnitTest`, sin dependencias nuevas.
- **«`hoy`/`ahora` como parámetro»:** aplica directo al punto 1: es el propio objeto de este plan.

## 1. `ahora: Instant` en `RegistrarIncidencia` (RF-33, RF-36)

### 1.1 Diagnóstico
`RegistrarIncidencia.kt` (línea 41) pone `fechaRegistro = Instant.now()` dentro del propio caso de uso. Es Kotlin puro y no depende de Android, así que no rompe el principio 3 de la constitución, pero es inconsistente con `TextosIncidencia.kt`, que ya recibe `ahora: Instant` en `diasPendiente()` y `fechaCorta()`, y deja sin forma de fijar el instante en una prueba (hoy ninguna prueba comprueba que `fechaRegistro` sea exactamente un valor dado; solo que exista y se use en el `Movimiento`).

### 1.2 Diseño
`RegistrarIncidencia.invoke` agrega un cuarto parámetro `ahora: Instant`, al final (para no romper el orden posicional de los tres primeros, ya usados por nombre en los tests existentes):

```kotlin
suspend operator fun invoke(
    codigoEquipo: String,
    descripcion: String,
    responsable: String,
    ahora: Instant
): Resultado<Incidencia> {
    ...
    fechaRegistro = ahora,   // en vez de Instant.now()
    ...
}
```

El único llamador de producción es `RegistroIncidenciaViewModel.guardar()`. El proyecto no tiene hoy un proveedor de tiempo inyectable (no existe un equivalente a `@IoDispatcher` para el reloj); agregar uno sería una dependencia nueva para un solo llamador. Se decide que el `ViewModel` —que ya es el límite donde el proyecto acepta tocar relojes y temporizadores reales (ahí vive el `flow { while (true) { emit(Instant.now()); delay(...) } }` que refresca `fechaHora` cada minuto para RF-32)— sea quien llame a `Instant.now()` al momento de invocar el caso de uso:

```kotlin
// RegistroIncidenciaViewModel.guardar()
when (val r = registrarIncidencia(actual.codigoEquipo, actual.descripcion, actual.responsable, Instant.now())) {
    ...
}
```

Esto no cambia ningún comportamiento observable: la fecha sigue siendo la del instante real en que se pulsa «Guardar incidencia». Solo cambia *quién* lee el reloj, para que `RegistrarIncidencia` quede testeable con un instante fijo.

### 1.3 Alternativas descartadas
| Alternativa | Por qué se descarta |
|---|---|
| Agregar un `ProveedorDeTiempo` inyectable (interfaz con `fun ahora(): Instant`), con una implementación real y un doble de prueba. | Es la solución «correcta» a largo plazo si más casos de uso necesitaran el reloj, pero hoy sólo lo necesita este, y el propio ejemplo de `TextosIncidencia.kt` ya resuelve el mismo problema con un parámetro simple, sin interfaz nueva. Añadir una abstracción para un solo uso contradice el principio 1 (preferir código simple con lo que ya existe). |
| Dejar `Instant.now()` dentro de `RegistrarIncidencia` y solo agregar la prueba con un margen de tolerancia (`assertTrue(Duration.between(...).abs() < Duration.ofSeconds(1))`). | No prueba el valor exacto, y la spec pide una prueba que fije el instante; además no resuelve la inconsistencia con `TextosIncidencia.kt` que la propia spec señala como hueco real. |

## 2. RF-41 — validar el código de equipo contra el catálogo sincronizado

### 2.1 Diagnóstico
Hoy `RegistrarIncidencia` solo recorta espacios y pone mayúsculas al código del equipo (RF-34); no existe ninguna verificación contra un catálogo. La infraestructura de datos ya existe pero está desconectada:
- `core/domain/model/Equipo.kt` y `core/database/entity/EquipoEntity.kt` (con `toDomain()`/`toEntity()`) ya existen y ya están en `SigtecDatabase` (`EquipoEntity::class`, `version = 1`): **no hace falta migración ni subir `version`**.
- `CatalogoDao.equipoPorCodigo(codigo)` y `CatalogoDao.guardarEquipos(...)` ya existen, pero `guardarEquipos` no se llama desde ningún lugar del código (ni siquiera `guardarTecnicos`, su análogo, tiene un llamador hoy: la descarga de técnicos tampoco está conectada, pero queda fuera de esta spec).
- `firebase/firestore.rules` ya protege `/equipos/{codigo}` de solo lectura (línea 84: `allow read: if autenticado(); allow write: if false;`), y Firestore ya tiene los 4 equipos de prueba reales (`PC-CONT-014`, `PC-LOG-009`, `PC-SIS-003`, `IMP-RRHH-002`), según `CLAUDE.md`.
- Ningún worker ni listener de `core/sync` baja hoy la colección `/equipos`.

Faltan, de adentro hacia afuera: (a) un caso nuevo de `ErrorDominio`; (b) que `core/sync` baje `/equipos` a Room; (c) que el repositorio de incidencias exponga una forma de consultar el catálogo; (d) que `RegistrarIncidencia` la use.

### 2.2 `ErrorDominio.EquipoNoEncontrado` ⚠️ (`core/domain/Resultado.kt`)
Se agrega un caso nuevo al `sealed interface ErrorDominio`:

```kotlin
data object EquipoNoEncontrado : ErrorDominio   // RF-41: el código no existe en el catálogo sincronizado
```

El `when` exhaustivo de `core/ui/MensajesError.kt` ⚠️ obliga a agregar su texto (único `when` exhaustivo sobre `ErrorDominio` encontrado en el código; se revisó con `grep` y no hay otro):

```kotlin
ErrorDominio.EquipoNoEncontrado -> "El código de equipo no existe en el catálogo. Verifícalo e inténtalo de nuevo."
```

### 2.3 Bajar `/equipos` en `core/sync` ⚠️
Se replica el patrón ya usado para `incidencias`/`movimientos` en `SincronizacionTiempoReal.kt`, pero más simple: es un catálogo de solo lectura (Firestore nunca recibe escrituras de `/equipos` desde la app, `firestore.rules` ya lo impide), así que no hace falta la lógica de conflictos (`debeReemplazarLocal`) ni marcar nada como pendiente de sincronizar — `EquipoEntity` no tiene (ni necesita) `pendienteSincronizar`/`actualizadoEn`.

**`MapeoFirestore.kt`** (nuevo mapeo, sección «Catálogo»):
```kotlin
internal fun DocumentSnapshot.aEquipoEntity(): EquipoEntity? = runCatching {
    EquipoEntity(
        codigo = id,                      // el id del documento es el código, igual que /equipos/{codigo} en las reglas
        tipo = getString("tipo")!!,
        marca = getString("marca")!!,
        modelo = getString("modelo")!!,
        oficina = getString("oficina")!!,
        responsableNombre = getString("responsableNombre")!!
    )
}.getOrNull()
```
> Decisión abierta marcada en la sección 9/«Dudas»: los nombres de campo (`tipo`, `marca`, `modelo`, `oficina`, `responsableNombre`) se asumen iguales a las propiedades de `EquipoEntity`, siguiendo el mismo estilo 1-a-1 que ya usa `aIncidenciaEntity()`/`aFirestore()`. Como los 4 equipos de prueba ya existen en Firestore desde antes de esta spec, hay que confirmar contra los documentos reales al probar en el emulador/celular (tarea de verificación manual); si los nombres reales difieren, se ajusta el mapeo sin que eso cambie ninguna otra parte del diseño.

**`SincronizacionTiempoReal.kt`** (inyecta `CatalogoDao`, ya provisto por `DatabaseModule.provideCatalogoDao`, sin módulo de DI nuevo):
```kotlin
class SincronizacionTiempoReal @Inject constructor(
    ...,
    private val catalogoDao: CatalogoDao,   // nuevo
    ...
) {
    fun iniciar() {
        ...
        emitAll(
            merge(
                escuchar("incidencias", ::guardarIncidencias),
                escuchar("movimientos", ::guardarMovimientos),
                escuchar("equipos", ::guardarEquipos),     // nuevo, RF-41
                vigilarPendientes()
            )
        )
    }

    private suspend fun guardarEquipos(snap: QuerySnapshot) {
        val nuevos = snap.documentChanges
            .filter { it.type != DocumentChange.Type.REMOVED }
            .mapNotNull { it.document.aEquipoEntity() }
        if (nuevos.isNotEmpty()) catalogoDao.guardarEquipos(nuevos)
    }
}
```
`escuchar()` ya es genérico (recibe el nombre de la colección y una función `guardar`), así que no hace falta tocar su firma; solo se agrega una tercera rama al `merge(...)` existente. `SigtecApp.kt` **no cambia**: sigue llamando a `sincronizacion.iniciar()` sin parámetros nuevos.

### 2.4 `IncidenciaRepository` — consultar el catálogo
Se revisó cómo otros casos de uso acceden a datos: ningún caso de uso en `core/domain/usecase/*` ni en `feature/*/domain/*` inyecta un DAO directamente (`grep "Dao\b"` sobre esas carpetas no encuentra nada); todos pasan por la interfaz de repositorio de su propio módulo. Se sigue esa misma convención: `CatalogoDao` se inyecta en `IncidenciaRepositoryImpl` (capa `data`, igual que `IncidenciaDao`/`MovimientoDao`), no en `RegistrarIncidencia`.

**`feature/incidencias/domain/IncidenciaRepository.kt`** (interfaz, agrega un método):
```kotlin
/** True si el código ya existe en el catálogo de equipos sincronizado (RF-41). */
suspend fun existeEquipo(codigo: String): Boolean
```

**`feature/incidencias/data/IncidenciaRepositoryImpl.kt`** (implementación):
```kotlin
class IncidenciaRepositoryImpl @Inject constructor(
    private val db: SigtecDatabase,
    private val incidenciaDao: IncidenciaDao,
    private val movimientoDao: MovimientoDao,
    private val catalogoDao: CatalogoDao,      // nuevo
    @IoDispatcher private val io: CoroutineDispatcher
) : IncidenciaRepository {
    ...
    override suspend fun existeEquipo(codigo: String): Boolean =
        withContext(io) { catalogoDao.equipoPorCodigo(codigo) != null }
}
```
No hace falta `@Binds` nuevo: `IncidenciasModule.kt` ya liga `IncidenciaRepositoryImpl` a `IncidenciaRepository`; el nuevo parámetro del constructor lo resuelve Hilt solo (`CatalogoDao` ya se provee en `DatabaseModule`).

**`app/src/test/.../IncidenciaRepositoryFalso.kt`** (doble de prueba): agrega un catálogo en memoria, poblado por defecto con los 4 equipos de prueba reales, para que las pruebas existentes de `RegistrarIncidenciaTest` (que usan `"PC-CONT-014"`) no se rompan:
```kotlin
class IncidenciaRepositoryFalso(
    var fallar: Boolean = false,
    private val equiposValidos: MutableSet<String> = mutableSetOf(
        "PC-CONT-014", "PC-LOG-009", "PC-SIS-003", "IMP-RRHH-002"
    )
) : IncidenciaRepository {
    ...
    override suspend fun existeEquipo(codigo: String): Boolean = codigo in equiposValidos

    fun agregarEquipo(codigo: String) { equiposValidos += codigo }   // para probar catálogos distintos si hiciera falta
}
```

### 2.5 `RegistrarIncidencia` — usar el catálogo
Se consulta el catálogo **después** de recortar/validar que no esté en blanco (RF-35) y **antes** de crear la incidencia, porque RF-41 dice «tras recortar espacios y pasar a mayúsculas», y un código vacío ya tiene su propio error (`CampoObligatorio`), no `EquipoNoEncontrado`:

```kotlin
suspend operator fun invoke(
    codigoEquipo: String, descripcion: String, responsable: String, ahora: Instant
): Resultado<Incidencia> {
    val usuario = when (val r = verificarPermiso(Permiso.REGISTRAR_INCIDENCIA)) {
        is Resultado.Exito -> r.valor
        is Resultado.Error -> return r
    }
    val codigo = codigoEquipo.trim().uppercase()
    val texto = descripcion.trim()
    val quien = responsable.trim()
    if (codigo.isEmpty() || texto.isEmpty() || quien.isEmpty()) {
        return Resultado.Error(ErrorDominio.CampoObligatorio)
    }
    if (!repo.existeEquipo(codigo)) {                  // RF-41
        return Resultado.Error(ErrorDominio.EquipoNoEncontrado)
    }
    val incidencia = Incidencia(..., fechaRegistro = ahora, ...)
    ...
}
```

### 2.6 Alternativas descartadas
| Alternativa | Por qué se descarta |
|---|---|
| Inyectar `CatalogoDao` directamente en `RegistrarIncidencia`. | Ningún caso de uso del proyecto inyecta un DAO; todos pasan por el repositorio de su propio módulo (ver 2.4). Romper esa convención aquí la haría inconsistente sin ganar nada, porque `IncidenciaRepository` ya es el lugar natural para una pregunta sobre datos que terminan en Room. |
| Validar el código en el `ViewModel` o en la `Screen` (deshabilitar el botón si no coincide con la lista). | Viola el principio 3 (la regla de negocio debe vivir en `domain/`, no en presentación); además la lista de equipos válidos solo está completa en Room, no es práctica para validar carácter por carácter en la UI mientras se escribe. |
| Hacer que `core/sync` descargue `/equipos` con un `get()` de una sola vez (sin listener en tiempo real), en vez de reusar `escuchar(...)`. | Funcionaría, pero obliga a mantener dos mecanismos de descarga distintos en el mismo archivo (uno con listener, otro sin) para un caso que el helper genérico `escuchar(...)` ya resuelve sin cambios; se prefiere lo simple y uniforme (principio 1). Si en el futuro se nota que escuchar `/equipos` en tiempo real es innecesario (el catálogo casi no cambia), se puede revisar, pero no es parte de esta spec. |
| Agregar `pendienteSincronizar`/`actualizadoEn` a `EquipoEntity` para que siga el mismo molde que las demás entidades. | `EquipoEntity` nunca se escribe desde la app (es de solo lectura, `firestore.rules` ya lo impone); esos campos no tendrían ningún lector. Se prefiere no agregar campos sin uso. |

## 3. `RegistroIncidenciaViewModelTest` (nuevo)

### 3.1 Diagnóstico
No existe hoy ningún archivo de prueba para `RegistroIncidenciaViewModel`. La spec marca a RF-30, RF-31, RF-32 y RF-40 como «sin prueba automatizada», y pide agregarla en este plan.

### 3.2 Diseño
Se crea `app/src/test/java/com/softcorp/sigtec/RegistroIncidenciaViewModelTest.kt`, con el mismo estilo que `ListaIncidenciasViewModelTest.kt` y `LoginViewModelTest.kt`: `MainDispatcherRule`, `SesionRepositoryDemo` (ya usado por `RegistrarIncidenciaTest`) para simular `ObservarUsuarioActual`, `IncidenciaRepositoryFalso` para `RegistrarIncidencia`, y `backgroundScope.launch(UnconfinedTestDispatcher(...)) { vm.estado.collect {} }` para que el `StateFlow` con `WhileSubscribed` tenga un observador.

Casos mínimos:
1. **RF-31 — botón deshabilitado si faltan datos:** con sesión iniciada pero algún campo vacío, `estado.value.puedeGuardar == false`; al completar los tres campos, pasa a `true`. También: sin sesión (`registradoPor == null`), aunque los tres campos estén completos, `puedeGuardar == false`.
2. **RF-32 — la fecha mostrada avanza cada minuto:** con `runTest`, avanzar el tiempo virtual con `advanceTimeBy(RELOJ_MS)` (o el equivalente del `TestScope`) y comprobar que `estado.value.fechaHora` cambió. Como el reloj interno usa `Instant.now()` real (no inyectable, ver sección 1: se decidió no agregar un proveedor de tiempo para un solo llamador), la prueba no fija el valor exacto; comprueba que, tras avanzar el *scheduler* virtual el intervalo configurado, se emite una nueva fecha distinta de la primera (con un margen, p. ej. `assertTrue(segunda.isAfter(primera) || segunda != primera)`), replicando cómo ya se probó el reloj de otras pantallas del proyecto si existiera un precedente, o, si no hay ninguno, documentando que es una prueba de que *hay* una segunda emisión, no del valor exacto.
3. **RF-40 — evento de un solo uso tras guardar:** con un repositorio falso y catálogo válido, llamar a `vm.guardar()`, avanzar el dispatcher, y comprobar `estado.value.registradaId != null` y que coincide con el id devuelto por el repositorio falso; luego `vm.alNavegarAlDetalle()` y comprobar que vuelve a `null`. (El texto «Incidencia registrada · Por sincronizar» en sí ya lo cubre `TextosIncidenciaTest` sobre `codigoParaMostrar()`, según el criterio de finalización de la spec; aquí solo se prueba el evento y la navegación, no el texto literal, que vive en la `Screen`.)
4. **Camino de error (RF-41 de extremo a extremo vía ViewModel):** con un código de equipo que no está en el catálogo falso, `vm.guardar()` deja `estado.value.error == ErrorDominio.EquipoNoEncontrado` y `registradaId == null`, y el botón puede reintentarse (`guardando == false`).

### 3.3 Alternativas descartadas
| Alternativa | Por qué se descarta |
|---|---|
| Probar el refresco del reloj (RF-32) con `Thread.sleep` real en vez de tiempo virtual. | Haría la prueba lenta y no determinista; el proyecto ya usa `runTest`/`advanceTimeBy` para temporizadores (ver `MainDispatcherRule` y los demás `*ViewModelTest`). |
| No probar RF-32 porque el reloj no es inyectable. | La spec pide explícitamente cubrir RF-32; con tiempo virtual de `kotlinx-coroutines-test` alcanza para comprobar que *hay* una segunda emisión sin necesitar fijar el instante exacto. |

## 4. Archivos

Leyenda: ✏️ se modifica · 🆕 se crea · ⚠️ archivo compartido (requiere visto bueno según `CLAUDE.md` o, en el caso de `core/sync/*`, tratado con el mismo cuidado aunque no esté en su lista literal).

| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️ `feature/incidencias/domain/RegistrarIncidencia.kt` | Recibe `ahora: Instant`; consulta `repo.existeEquipo(codigo)` y devuelve `EquipoNoEncontrado` si no existe. | RF-33, RF-36, RF-41 |
| ✏️ `feature/incidencias/presentation/RegistroIncidenciaViewModel.kt` | Pasa `Instant.now()` al invocar `RegistrarIncidencia`. | RF-33, RF-36 |
| ✏️⚠️ `core/domain/Resultado.kt` | Agrega `ErrorDominio.EquipoNoEncontrado`. | RF-41 |
| ✏️⚠️ `core/ui/MensajesError.kt` | Agrega el texto de `EquipoNoEncontrado`. | RF-41 |
| ✏️⚠️ `core/sync/MapeoFirestore.kt` | Agrega `DocumentSnapshot.aEquipoEntity()`. | RF-41 |
| ✏️⚠️ `core/sync/SincronizacionTiempoReal.kt` | Inyecta `CatalogoDao`; agrega `escuchar("equipos", ::guardarEquipos)` y `guardarEquipos(snap)`. | RF-41 |
| ✏️ `feature/incidencias/domain/IncidenciaRepository.kt` | Agrega `existeEquipo(codigo): Boolean`. | RF-41 |
| ✏️ `feature/incidencias/data/IncidenciaRepositoryImpl.kt` | Inyecta `CatalogoDao`; implementa `existeEquipo`. | RF-41 |
| ✏️ `app/src/test/.../feature/incidencias/data/IncidenciaRepositoryFalso.kt` | Agrega el catálogo en memoria (4 equipos de prueba por defecto) y `existeEquipo`. | RF-41 |
| ✏️ `app/src/test/.../RegistrarIncidenciaTest.kt` | Agrega `ahora` a todas las llamadas existentes; nuevo caso que fija el instante (RF-36); nuevo caso de equipo no encontrado (RF-41). | RF-36, RF-41 |
| 🆕 `app/src/test/.../RegistroIncidenciaViewModelTest.kt` | Cubre RF-31, RF-32, RF-40 y el camino de error de RF-41 a nivel de `ViewModel`. | RF-31, RF-32, RF-40, RF-41 |

No se tocan `firebase/firestore.rules` (ya protege `/equipos` desde antes de esta spec, según la propia nota de RF-41), `SigtecDatabase.kt` (no sube `version`), `SigtecApp.kt`, `DatabaseModule.kt` ni `IncidenciasModule.kt` (Hilt resuelve los nuevos parámetros de constructor sin módulos nuevos).

## 5. Cómo se pinta en la interfaz
No hay cambios visuales. `RegistroIncidenciaScreen.kt` ya muestra cualquier `ErrorDominio` con `error.mensaje()` debajo del botón «Guardar incidencia» (igual que hoy muestra `Desconocido` o `CampoObligatorio`); el texto nuevo de `EquipoNoEncontrado` aparece ahí sin tocar la pantalla. No se agregan `@Preview` nuevas: las existentes ya cubren el estado con `error != null`.

## 6. Estrategia de pruebas

Se usa `./gradlew testDebugUnitTest` con JUnit 4, `kotlinx-coroutines-test`, `MainDispatcherRule` y los dobles existentes (`SesionRepositoryDemo`, `IncidenciaRepositoryFalso`). No se agregan librerías ni dobles nuevos.

| Prueba | Casos nuevos/cambiados | RF |
|---|---|---|
| `RegistrarIncidenciaTest` | Todas las llamadas existentes reciben un cuarto argumento `ahora`; nuevo caso que fija `Instant.parse("2025-01-01T10:00:00Z")` y comprueba `fechaRegistro` exacta; nuevo caso con un código fuera del catálogo falso → `Resultado.Error(ErrorDominio.EquipoNoEncontrado)` y nada se guarda. | RF-33, RF-34, RF-35, RF-36, RF-37, RF-39, RF-41 |
| `RegistroIncidenciaViewModelTest` (nuevo) | Botón deshabilitado sin sesión o con campos vacíos (RF-31); la fecha mostrada cambia tras avanzar el tiempo virtual (RF-32); evento `registradaId` tras guardar y su limpieza con `alNavegarAlDetalle()` (RF-40); error `EquipoNoEncontrado` visible en el estado sin navegar (RF-41). | RF-31, RF-32, RF-40, RF-41 |

**No se agrega prueba unitaria para la bajada de `/equipos` en `core/sync`:** ni `SincronizacionTiempoReal` ni las funciones de mapeo de `MapeoFirestore.kt` (`aIncidenciaEntity`, `aMovimientoEntity`, y ahora `aEquipoEntity`) tienen pruebas JVM hoy, porque dependen de tipos reales de Firebase (`DocumentSnapshot`, `QuerySnapshot`) y el proyecto no usa un framework de mocks (no hay Mockito ni similar en `libs.versions.toml`); se mantiene el mismo criterio que ya existe para `incidencias`/`movimientos`: se verifica a mano. Esto se deja explícito para que el coordinador no lo de por pendiente.

**Verificado a mano, no automatizado:** que la colección `/equipos` real de Firestore baja a Room con los nombres de campo asumidos en 2.3 (si no coinciden, se ajusta el mapeo); que un código de equipo válido permite guardar y uno inválido muestra el error en el celular; que los nombres de campo reales de los 4 equipos de prueba coinciden con el mapeo.

**Cierre:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde antes de abrir el PR o marcar la tarea como terminada.

## 7. Cobertura de RF

| RF | Dónde vive | Prueba |
|---|---|---|
| RF-30 | `RegistroIncidenciaViewModel.estado` (sin cambios) | `RegistroIncidenciaViewModelTest` (nuevo) |
| RF-31 | `RegistroIncidenciaUiState.puedeGuardar` (sin cambios) | `RegistroIncidenciaViewModelTest` (nuevo) |
| RF-32 | `RegistroIncidenciaViewModel.reloj` (sin cambios) | `RegistroIncidenciaViewModelTest` (nuevo, tiempo virtual) |
| RF-33 | `RegistrarIncidencia` (`VerificarPermiso`, sin cambios de lógica) | `RegistrarIncidenciaTest` |
| RF-34, RF-35 | `RegistrarIncidencia` (sin cambios de lógica) | `RegistrarIncidenciaTest` |
| RF-36 | `RegistrarIncidencia` — ahora recibe `ahora: Instant` | `RegistrarIncidenciaTest` (caso nuevo con instante fijo) |
| RF-37, RF-38, RF-39 | `RegistrarIncidencia` / `IncidenciaRepositoryImpl` (sin cambios de lógica) | `RegistrarIncidenciaTest` |
| RF-40 | `RegistroIncidenciaViewModel.registradaId` (sin cambios) | `RegistroIncidenciaViewModelTest` (nuevo) + manual |
| RF-41 | `core/sync` (bajada de `/equipos`) + `IncidenciaRepository.existeEquipo` + `RegistrarIncidencia` | `RegistrarIncidenciaTest` (caso nuevo) + `RegistroIncidenciaViewModelTest` (caso nuevo) + manual para la bajada real |

## 8. Respeto a la constitución
1. **Stack cerrado:** sin dependencias nuevas; se reutiliza `CatalogoDao`, `EquipoEntity` y el patrón de listener ya existente en `core/sync`.
2. **Spec antes que código:** este plan solo implementa lo que `spec.md` ya marca como pendiente (nota de RF-36 y RF-41); no cambia ningún RF existente. Al cerrar, `spec.md` se actualiza para quitar la marca «(pendiente)» de RF-41 (tarea de cierre en `tasks.md`).
3. **Lógica fuera de la pantalla:** la validación del catálogo vive en `RegistrarIncidencia` (dominio), consultando el repositorio; `RegistroIncidenciaScreen` no cambia.
4. **Toda regla tiene prueba:** los tres puntos del plan quedan cubiertos con pruebas unitarias nuevas o ajustadas, salvo la bajada real de Firestore (sin frameworks de mock disponibles), que queda como verificación manual explícita.
5. **Datos protegidos:** sin cambios; no se toca huella, contraseñas ni tokens. `/equipos` sigue siendo de solo lectura para la app.
6. **Español:** nombres nuevos (`existeEquipo`, `EquipoNoEncontrado`, `guardarEquipos` interno, `aEquipoEntity`) siguen la convención del archivo que los recibe (dominio en español, sufijos de Android en inglés donde ya correspondía).

## 9. Archivos compartidos que toca (requieren visto bueno)
- `core/domain/Resultado.kt` — listado explícito de `CLAUDE.md` en «archivos compartidos — pregunta antes».
- `core/ui/MensajesError.kt` — listado explícito de `CLAUDE.md` en «archivos compartidos — pregunta antes».
- `core/sync/MapeoFirestore.kt` y `core/sync/SincronizacionTiempoReal.kt` — no están en la lista textual de `CLAUDE.md`, pero `core/sync` es, según el propio `CLAUDE.md`, «el único lugar que habla con Firestore»; se trata con el mismo cuidado y requiere el mismo visto bueno antes de tocarlo.

**Pendiente de visto bueno explícito del usuario sobre estos cuatro archivos antes de implementar la sección 2 completa (RF-41).** La sección 1 (`ahora: Instant`) y la sección 3 (`RegistroIncidenciaViewModelTest`) no tocan archivos compartidos y no requieren ese visto bueno adicional, más allá de la aprobación general de este plan.
