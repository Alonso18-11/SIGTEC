# Plan 012 — Directorio de colaboradores al registrar (HU-12)

> Basado en `spec.md` (aprobada el 2026-11-08, con D-1 a D-6 respondidas) y en `docs/constitution.md`.
> El código vive en `feature/incidencias` (registro y detalle). Toca estos archivos compartidos:
> - el modelo `Incidencia` y su entidad (sube `version` a 5);
> - `core/sync/MapeoFirestore.kt`;
> - `MEMORY.md`.
>
> No toca `firestore.rules`, `Rutas.kt`, el manifest ni las dependencias. Cada PR trata un solo tema, lleva la HU al final del título y necesita la aprobación de la otra persona del equipo antes de fusionarse.
> Las rutas son relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/` (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas).

## 0. Hallazgos del código real

- **El responsable es texto libre.** `Incidencia.usuarioResponsable: String` se guarda en Room (`IncidenciaEntity`) y en Firestore (`MapeoFirestore`). `RegistrarIncidencia(codigo, descripcion, responsable, ahora)` lo recorta y exige que no esté vacío.
- **El catálogo de equipos ya está en el celular.** `CatalogoDao.equipoPorCodigo` e `IncidenciaRepository.existeEquipo(codigo)` (RF-41) sirven para D-5 sin consultas nuevas. Su `responsableNombre` no se usa (D-3).
- **El formulario ya reacciona al código.**
  - `RegistroIncidenciaViewModel` tiene `alCambiarCodigo` (texto) y `alUsarCodigoEscaneado` (HU-09), que son los dos disparadores de RF-1201 y RF-1202.
  - El campo «Usuario responsable» de `RegistroIncidenciaScreen` tiene el comentario «el directorio es la HU-12».
- **Firestore no limita los campos al crear.**
  - La regla `create` de `incidencias` solo exige `registradoPorId` y `marca`.
  - El `update` del jefe y del técnico no mira `areaResponsable`.
  - Por eso **no cambia `firestore.rules`**.
- **Patrón de la HU-11 para copiar.** En `feature/atencion` ya existen:
  - `LogisticaRepository` con `ResultadoLogistica`/`ErrorLogistica`;
  - `LogisticaApi`, los DTO y `MapeoLogistica` (que se prueba en la JVM);
  - `LogisticaRepositoryImpl`, con el 404 de MockAPI tratado como «sin coincidencias»;
  - `LogisticaModule`, con 10 s y la dirección base;
  - la búsqueda con espera y cancelación de `SolicitudRepuestoViewModel` (`debounce` y `collectLatest`).
- **Retrofit está y se usa.** No hay dependencias nuevas, y el permiso `INTERNET` ya está en el manifest combinado (HU-11).

## 1. División en PR, dependencias y orden de fusión

| PR | Rama | Título | Base | Trae | RF |
|---|---|---|---|---|---|
| PR-01 | `feat/hu12-area-responsable` | `feat(core): la incidencia guarda el área del responsable (HU-12)` | `main` | — | RF-1214, RF-1215, RF-1218 |
| PR-02 | `feat/hu12-api-directorio` | `feat(incidencias): API del directorio con Retrofit detrás de un repositorio (HU-12)` | `main` | — | RF-1201, RF-1205, RF-1211, RF-1212 |
| PR-03 | `feat/hu12-registrar-area` | `feat(incidencias): RegistrarIncidencia guarda el área y el detalle la muestra (HU-12)` | PR-01 | — | RF-1214, RF-1216, RF-1217 |
| PR-04 | `feat/hu12-registro-directorio` | `feat(incidencias): el registro consulta el directorio y completa el responsable (HU-12)` | PR-03 | PR-02 | RF-1201 a RF-1213 |
| PR-05 | `docs/hu12-cierre` | `docs(incidencias): cierre de la HU-12 con las pruebas manuales (HU-12)` | `main` al día | todos | RF-1201 a RF-1218 |

**Orden de fusión:**
1. PR-01 (modelo) y PR-02 (API), en paralelo.
2. PR-03, sobre PR-01.
3. PR-04, sobre PR-03, con un merge de PR-02.
4. PR-05, cuando todo esté en `main`.

**Antes de PR-02:** crear en MockAPI.io el recurso `colaboradores` (sección 5) en el proyecto de la HU-11 y confirmar que el plan gratuito lo admite (duda abierta de la spec). Lo hace Alonso, o lo hace el asistente con su visto bueno.

## 2. Archivos por PR

Leyenda: 🆕 nuevo, ✏️ modificado, ⚠️ compartido de `CLAUDE.md`, 🔶 sensible fuera de esa lista.

### PR-01 — modelo
- ✏️⚠️ `core/domain/model/Incidencia.kt`: `areaResponsable: String? = null`, después de `usuarioResponsable`.
- ✏️⚠️ `core/database/entity/IncidenciaEntity.kt`: la columna `areaResponsable` (nullable) y `toDomain()`/`toEntity()`.
- ✏️⚠️ `core/database/SigtecDatabase.kt`: `version = 5`, con el comentario de la HU-12.
- ✏️🔶 `core/sync/MapeoFirestore.kt`:
  - `"areaResponsable" to areaResponsable` al subir;
  - `getString("areaResponsable")` al bajar (`null` si no está, RF-1215).
- ✏️ `androidTest/.../IncidenciaDaoTest.kt`: guarda y lee una incidencia con área y otra sin área.

### PR-02 — API del directorio
- 🆕 `feature/incidencias/domain/DirectorioRepository.kt`: el contrato, `Colaborador`, `ResultadoDirectorio` y `ErrorDirectorio` (sección 3.1).
- 🆕 `feature/incidencias/data/DirectorioApi.kt`: la interfaz de Retrofit y `ColaboradorDto` (`@Serializable`).
- 🆕 `feature/incidencias/data/MapeoDirectorio.kt`: elige la coincidencia exacta y traduce las excepciones (sección 3.2).
- 🆕 `feature/incidencias/data/DirectorioRepositoryImpl.kt`, con `@IoDispatcher`, el `catch` de la convención y `TAG`.
- 🆕 `feature/incidencias/di/DirectorioModule.kt`:
  - `@Binds` del repositorio;
  - `URL_DIRECTORIO`;
  - OkHttp con 10 s;
  - el `Json` con `ignoreUnknownKeys`.
- 🆕 `test/.../feature/incidencias/data/MapeoDirectorioTest.kt`.
- 🆕 `test/.../feature/incidencias/data/DirectorioRepositoryFalso.kt`.

### PR-03 — guardar el área y mostrarla
- ✏️ `feature/incidencias/domain/RegistrarIncidencia.kt`: el parámetro `areaResponsable: String? = null`, recortado; si queda vacío, `null` (RF-1214).
- ✏️ `test/.../RegistrarIncidenciaTest.kt`:
  - con área;
  - con área de solo espacios, que se guarda `null`;
  - sin área.
- ✏️ `feature/incidencias/presentation/DetalleIncidenciaScreen.kt`: «nombre · área» en «Usuario responsable», y una vista previa con área (RF-1216).

### PR-04 — el formulario
- ✏️ `feature/incidencias/presentation/RegistroIncidenciaViewModel.kt`: recibe `DirectorioRepository` e `IncidenciaRepository` (para `existeEquipo`); el detalle está en la sección 4.
- ✏️ `feature/incidencias/presentation/RegistroIncidenciaScreen.kt`:
  - texto de ayuda, área y anexo, buscando, sugerencia con «Usar»;
  - avisos con «Reintentar»;
  - vistas previas de cada estado.
- ✏️ `test/.../RegistroIncidenciaViewModelTest.kt`: los casos de la spec.

### PR-05 — cierre
- ✏️ `specs/012-directorio-colaboradores/{spec,tasks}.md` y ✏️ `MEMORY.md` (solo la línea de la HU-12).

## 3. Contratos

### 3.1 Dominio (`feature/incidencias/domain/DirectorioRepository.kt`)
```kotlin
/** HU-12: la API del directorio de colaboradores (sección 14 del producto), hoy contra MockAPI.io (D-1). */
interface DirectorioRepository {
    /** RF-1201: el responsable del equipo [codigoEquipo]; `NO_ENCONTRADO` si el directorio no lo tiene (RF-1211). */
    suspend fun responsableDe(codigoEquipo: String): ResultadoDirectorio
}

data class Colaborador(val nombre: String, val area: String, val anexo: String?)

sealed interface ResultadoDirectorio {
    data class Encontrado(val colaborador: Colaborador) : ResultadoDirectorio
    data class Fallo(val error: ErrorDirectorio) : ResultadoDirectorio
}

enum class ErrorDirectorio { NO_ENCONTRADO, SIN_RESPUESTA }
```
Las fallas del directorio son de otro sistema, igual que las de Logística, por eso no son `ErrorDominio` y no tocan `MensajesError.kt`. Los textos de RF-1211 y RF-1212 viven en la pantalla.

### 3.2 Datos
```kotlin
interface DirectorioApi {
    // MockAPI filtra por coincidencia parcial; la exacta se elige en MapeoDirectorio (RF-1205)
    @GET("colaboradores")
    suspend fun buscarPorEquipo(@Query("equipo") codigoEquipo: String): List<ColaboradorDto>
}

@Serializable
data class ColaboradorDto(val id: String, val nombre: String, val area: String, val anexo: String? = null, val equipo: String)
```
`MapeoDirectorio`:
- `List<ColaboradorDto>.responsableDe(codigo): Colaborador?` elige el primero cuyo `equipo.trim()` es igual a `codigo.trim()` sin distinguir mayúsculas y con `nombre` no vacío. Si hay más de uno, registra en Logcat desde el repositorio.
- `Throwable.aErrorDirectorio()`:
  - un `HttpException` 404 es `NO_ENCONTRADO` (MockAPI responde 404 cuando no encuentra nada, igual que en la HU-11);
  - `IOException`, `SerializationException` y el resto de los HTTP son `SIN_RESPUESTA`.
- La lista vacía o sin coincidencia exacta también es `NO_ENCONTRADO`.

## 4. ViewModel del registro (PR-04)

Estado nuevo en `RegistroIncidenciaUiState`, con valores por defecto:
```kotlin
val directorio: EstadoDirectorio = EstadoDirectorio.Inactivo,
val responsableDelDirectorio: Boolean = false,   // RF-1206/RF-1208: el campo tiene lo que trajo el directorio, sin editar
// derivados: areaMostrada / anexoMostrado salen de directorio cuando responsableDelDirectorio = true
```
```kotlin
sealed interface EstadoDirectorio {
    data object Inactivo : EstadoDirectorio
    data object Buscando : EstadoDirectorio                               // RF-1203
    data class Encontrado(val colaborador: Colaborador) : EstadoDirectorio // RF-1206 o la sugerencia de RF-1207
    data object NoEncontrado : EstadoDirectorio                            // RF-1211
    data object SinRespuesta : EstadoDirectorio                            // RF-1212
}
```
Flujo:
- Un `MutableStateFlow<Consulta?>` con el código y si es inmediata.
- `alCambiarCodigo` emite una consulta normal y `alUsarCodigoEscaneado` una inmediata.
- `collectLatest` hace la cancelación de RF-1204:
  - si la consulta no es inmediata, `delay(600)`;
  - si `existeEquipo(codigo)` es falso, `Inactivo`;
  - si no, `Buscando`, después `responsableDe(codigo)` y el estado que corresponda.
- Al llegar `Encontrado`, si el responsable está vacío o `responsableDelDirectorio`, se escribe el nombre y `responsableDelDirectorio = true` (RF-1206). Si no, queda como sugerencia (RF-1207).
- `alCambiarResponsable(valor)` pone `responsableDelDirectorio = false` (RF-1208).
- `alUsarDelDirectorio()` aplica RF-1206 con el colaborador del estado `Encontrado`.
- `alReintentarDirectorio()` vuelve a emitir el código actual como consulta inmediata.
- Al cambiar el código con `responsableDelDirectorio = true`:
  - se vacía el responsable;
  - `responsableDelDirectorio = false`;
  - `directorio = Inactivo` (RF-1209).
- `guardar()` pasa `areaResponsable = colaborador.area` solo si `responsableDelDirectorio` (RF-1214). No espera a la consulta (RF-1213).
- El `reloj` de 60 s y el resto del formulario no cambian.

## 5. Servidor de prueba (D-1)

Recurso `colaboradores` en el proyecto de MockAPI de la HU-11 (`URL_LOGISTICA`), con los campos `nombre` (string), `area` (string), `anexo` (string) y `equipo` (string).

Lleva un registro por equipo de los 4 que hay en Firestore. Los nombres son ficticios:

| equipo | nombre | area | anexo |
|---|---|---|---|
| PC-CONT-014 | María Quispe Huamán | Contabilidad | 2314 |
| PC-LOG-009 | Juan Paredes Ríos | Logística | 2120 |
| PC-SIS-003 | Carla Huerta Salinas | Sistemas | 2401 |
| IMP-RRHH-002 | Rosa Chávez Medina | Recursos Humanos | 2207 |

Para comprobarlo, `GET <base>/colaboradores?equipo=PC-CONT-014` responde en el navegador y `?equipo=NO-EXISTE` responde 404.

## 6. Pruebas
- `MapeoDirectorioTest` (JVM):
  - coincidencia exacta entre parciales (`PC-CONT-01` frente a `PC-CONT-014`);
  - mayúsculas y espacios;
  - nombre vacío;
  - lista vacía;
  - 404 → `NO_ENCONTRADO`;
  - `IOException` y `SerializationException` → `SIN_RESPUESTA`.
- `RegistrarIncidenciaTest`: el área se guarda recortada, vacía → `null`, y el resto no cambia (RF-1217).
- `RegistroIncidenciaViewModelTest`, con `DirectorioRepositoryFalso`, `IncidenciaRepositoryFalso` y `MainDispatcherRule`, usando `advanceTimeBy`:
  - no consulta antes de 0,6 s ni con un código fuera del catálogo;
  - el escaneo consulta enseguida;
  - el último código gana;
  - completa nombre y área;
  - no pisa lo escrito a mano y «Usar» lo aplica;
  - editar quita el área;
  - cambiar el código vacía lo completado;
  - `NoEncontrado` y `SinRespuesta` con «Reintentar»;
  - guardar pasa el área solo si vino del directorio.
- `IncidenciaDaoTest` (instrumentada): la columna nueva.
- Manual: los criterios de finalización de la spec.

## 7. Riesgos y alternativas
- **P-1 · Plan gratuito de MockAPI.** Si no admite un tercer recurso, se usa otro proyecto en otra cuenta gratuita que crea Alonso, y solo cambia `URL_DIRECTORIO`.
- **P-2 · `version` 5 borra lo no sincronizado** en los celulares de prueba al instalar. Hay que sincronizar antes de instalar PR-01 y decirlo en el PR.
- **P-3 · Retrofit duplicado.** `DirectorioModule` arma su propio Retrofit en lugar de compartir el de `LogisticaModule`. Así no se toca `core/di` (compartido) ni se cruzan módulos de `feature/`, y cada API puede apuntar a su servidor real. Son unas 20 líneas repetidas; compartirlo queda para otro PR si el equipo lo quiere.
- **P-4 · Incidencias de otro celular con una app anterior.** Si un celular sin la HU-12 sube una incidencia, el campo `areaResponsable` no existe y se lee `null` (RF-1215). No rompe nada.
