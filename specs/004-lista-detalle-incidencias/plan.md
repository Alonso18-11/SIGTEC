# Plan 004 — Lista de incidencias con búsqueda y filtros, y detalle (HU-04)

> Basado en `spec.md` y `docs/constitution.md`. RF-42 a RF-57 ya están en `main` desde antes del flujo SDD (PR #8, `1940fcb`, ajustes del PR #9, `6420f40`); este plan **no** los vuelve a implementar. Solo cubre los tres ajustes que la spec aprobada marca como «Decisión» tras la fase de clarificación:
> 1. Mover la búsqueda (RF-43) y los filtros (RF-44) de `ListaIncidenciasViewModel.filtrar()` (presentación) a una función pura de `domain/`.
> 2. Agregar un temporizador propio en `ListaIncidenciasViewModel` y en `DetalleIncidenciaViewModel`, igual que ya hace `RegistroIncidenciaViewModel` para RF-32 (HU-03), para que `ahora` no dependa solo de que cambien los datos (caso límite de `spec.md`).
> 3. Renombrar el chip de filtro «Estado» a «Marca» en `ListaIncidenciasScreen.kt` (parte de RF-44).
>
> Las otras dos observaciones que señaló el revisor (falta de `VerificarPermiso` en `ObservarIncidencias`/`ObservarDetalleIncidencia`, y `puedeAsignar`/`puedeAtender` sin caso de uso propio) quedaron resueltas como «no se cambia» en `spec.md`; este plan no las toca.
>
> Ninguno de los archivos que tocan estos tres ajustes está en la lista de «archivos compartidos» de `CLAUDE.md` (todos viven en `feature/incidencias/*` y sus pruebas). No se necesita visto bueno adicional más allá de la aprobación general de este plan. Ver sección 9.

## 0. Adaptaciones de la plantilla
- **«Algoritmo del mapa»:** no aplica; se usa pseudocódigo puntual en las secciones 1 y 2.
- **«`node --test`»:** igual que en los planes 001 a 003, se usa JUnit 4 y coroutines-test con `./gradlew testDebugUnitTest`, sin dependencias nuevas.
- **«`hoy`/`ahora` como parámetro»:** aplica a la sección 1 (la función de dominio no lee el reloj, recibe `f: FiltrosIncidencias` y la lista ya cargada); no aplica a la sección 2, porque el temporizador vive deliberadamente en el `ViewModel` (presentación), el mismo límite donde el proyecto ya acepta leer el reloj real — ver 2.2.

## 1. Mover la búsqueda y los filtros a `domain/` (RF-43, RF-44)

### 1.1 Diagnóstico
`ListaIncidenciasViewModel.kt` hoy define, en la capa de presentación:
- `data class FiltrosIncidencias(texto, marca, tecnico, equipo)`, con su propiedad `activos`.
- `internal fun List<Incidencia>.filtrar(f: FiltrosIncidencias): List<Incidencia>`, que aplica la búsqueda de texto y los tres filtros combinados.
- Dos funciones privadas de apoyo: `Incidencia.coincideCon(buscado)` y `String.normalizado()` (sin tildes ni mayúsculas), más el regex `MARCAS_DIACRITICAS`.

`coincideCon()` llama a `codigoParaMostrar()` (definida en `TextosIncidencia.kt`, también presentación), que a su vez usa la constante `POR_SINCRONIZAR`. El filtro de técnico compara contra la constante `SIN_TECNICO` (también en `TextosIncidencia.kt`). Para que la función de dominio sea Kotlin puro y no dependa de la capa de presentación, estas tres declaraciones se mueven junto con `FiltrosIncidencias` y `filtrar()`: todas son, en los hechos, parte de la misma regla de negocio («qué texto representa a una incidencia para buscarla, y qué significa "sin técnico"»), no de cómo se dibuja la pantalla.

### 1.2 Diseño
Archivo nuevo `feature/incidencias/domain/FiltrarIncidencias.kt`:

```kotlin
package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import java.text.Normalizer

/** «INC-0124», o esto mientras la sincronización no le asigne número (RF-38, HU-03). */
const val POR_SINCRONIZAR = "Por sincronizar"

/** «INC-0124», o «Por sincronizar» si aún no tiene número (RF-42/RF-48/RF-51/RF-55). */
fun Incidencia.codigoParaMostrar(): String = codigoVisible ?: POR_SINCRONIZAR

/** Texto del filtro de técnico, y de la tarjeta, cuando no hay nadie asignado (RF-44, RF-48). */
const val SIN_TECNICO = "Sin técnico"

data class FiltrosIncidencias(
    val texto: String = "",
    val marca: MarcaSeguimiento? = null,
    val tecnico: String? = null,          // nombre del técnico, o SIN_TECNICO
    val equipo: String? = null
) {
    val activos: Boolean
        get() = texto.isNotBlank() || marca != null || tecnico != null || equipo != null
}

/**
 * RF-43/RF-44: búsqueda de texto (sin mayúsculas ni tildes, por código visible/«Por sincronizar»,
 * equipo o descripción) y filtros combinables de marca, técnico (incluido "Sin técnico") y equipo.
 * Función pura: no toca Room, Firestore ni permisos; el ViewModel solo la invoca sobre los datos
 * que ya tiene cargados. No es un caso de uso con Resultado<T> porque no hay ningún camino de
 * error que validar contra el dominio, solo una transformación de una lista ya en memoria.
 */
fun List<Incidencia>.filtrar(f: FiltrosIncidencias): List<Incidencia> {
    val buscado = f.texto.normalizado()
    return filter { inc ->
        (buscado.isEmpty() || inc.coincideCon(buscado)) &&
                (f.marca == null || inc.marca == f.marca) &&
                (f.tecnico == null || (inc.tecnicoAsignadoNombre ?: SIN_TECNICO) == f.tecnico) &&
                (f.equipo == null || inc.codigoEquipo == f.equipo)
    }
}

private fun Incidencia.coincideCon(buscado: String): Boolean =
    listOf(codigoParaMostrar(), codigoEquipo, descripcion).any { it.normalizado().contains(buscado) }

private fun String.normalizado(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(MARCAS_DIACRITICAS, "").lowercase()

private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")
```

Pseudocódigo de `filtrar()` (sin cambios de comportamiento respecto al código actual, solo de ubicación):
```
función filtrar(incidencias, filtros):
    buscado ← normalizar(filtros.texto)
    devolver incidencias donde, para cada incidencia:
        (buscado vacío O el código-para-mostrar/equipo/descripción normalizados contienen «buscado»)
        Y (filtros.marca es nulo O coincide con la marca de la incidencia)
        Y (filtros.tecnico es nulo O coincide con el técnico asignado, usando "Sin técnico" si no hay ninguno)
        Y (filtros.equipo es nulo O coincide con el código de equipo)
```

`ListaIncidenciasViewModel.kt` queda así de simple (quita la clase y las cuatro funciones privadas, las importa de `domain`):
```kotlin
import com.softcorp.sigtec.feature.incidencias.domain.FiltrosIncidencias
import com.softcorp.sigtec.feature.incidencias.domain.SIN_TECNICO
import com.softcorp.sigtec.feature.incidencias.domain.filtrar
// ...
incidencias = todas.filtrar(f),   // sin cambios: ya era así, solo cambia de dónde viene filtrar()
```

`TextosIncidencia.kt` (presentación) deja de declarar `POR_SINCRONIZAR`, `codigoParaMostrar()` y `SIN_TECNICO`; sigue con el resto de sus funciones de formato (`fechaCorta`, `fechaCompleta`, `horaDe`, las `etiqueta()` de cada enum), que sí son de presentación pura (dan formato para mostrar, no reglas de negocio) y no se mueven, porque el usuario no lo pidió y no forman parte de esta decisión.

Como esas tres declaraciones estaban en el mismo paquete que las pantallas que las usan sin un `import` explícito (visibilidad de paquete), al moverlas a `domain` cuatro archivos necesitan solo agregar la línea de `import` correspondiente, sin otro cambio de lógica: `ListaIncidenciasScreen.kt` (`codigoParaMostrar`, `SIN_TECNICO`, `FiltrosIncidencias`), `DetalleIncidenciaScreen.kt` (`codigoParaMostrar`, `SIN_TECNICO`), `IncidenciasNavigation.kt` (`POR_SINCRONIZAR`) y `TextosIncidenciaTest.kt` (`codigoParaMostrar`).

### 1.3 Alternativas descartadas
| Alternativa | Por qué se descarta |
|---|---|
| Duplicar un literal privado `"Por sincronizar"` y `"Sin técnico"` dentro del nuevo archivo de dominio, en vez de mover las declaraciones reales de `TextosIncidencia.kt`. | Deja dos fuentes de verdad para el mismo texto visible; si algún día cambia el literal (por ejemplo, a «Pendiente de sincronizar»), alguien tendría que recordar actualizarlo en dos sitios. Mover la declaración real es igual de simple y evita ese riesgo. |
| Convertir `filtrar()` en un caso de uso de clase con `@Inject constructor` y `operator fun invoke`, como `RegistrarIncidencia`. | La convención de casos de uso del proyecto (`// HU-03`, `feature/incidencias/domain/RegistrarIncidencia.kt`) existe para acciones que pueden fallar y devuelven `Resultado<T>` (por ejemplo, validando un permiso o un catálogo). Filtrar una lista ya cargada no tiene ningún camino de error de negocio; envolverla en una clase inyectable solo para seguir la forma, sin necesitarla, añade indirección sin beneficio (principio 1 de la constitución). |
| Mover también `opcionesTecnico()` (RF-45) a `domain/`. | El usuario no lo pidió y el revisor no lo marcó como problema; sigue siendo una función pura hoy, en `ListaIncidenciasViewModel.kt`, sin acceso a Room ni permisos. Se deja fuera de esta spec para no ampliar el alcance sin pedido explícito. |
| Mover también `fechaCorta()`/`fechaCompleta()`/las funciones `etiqueta()` de `TextosIncidencia.kt` a `domain/`, ya que también son Kotlin puro. | Ídem: no están en las tres decisiones del usuario; son funciones de formato de texto para la UI (dan el `String` final que ve la persona), no reglas de filtrado o de negocio. |

## 2. Temporizador propio en `ListaIncidenciasViewModel` y `DetalleIncidenciaViewModel` (caso límite de `spec.md`)

### 2.1 Diagnóstico
Hoy ambos `ViewModel` calculan `ahora = Instant.now()` dentro del `combine(...)` que construye su `UiState`, así que solo se recalcula cuando emite de nuevo alguno de los flujos combinados (las incidencias, los movimientos o el usuario). Si la pantalla queda abierta sin que lleguen cambios —por ejemplo, pasada la medianoche—, una etiqueta relativa como «Hoy» (`fechaCorta()`, en `TextosIncidencia.kt`) puede quedar desactualizada.

`RegistroIncidenciaViewModel.kt` ya resuelve el mismo problema para RF-32 con un `flow` propio que emite `Instant.now()` cada minuto:
```kotlin
private val reloj = flow {
    while (true) {
        emit(Instant.now())
        delay(RELOJ_MS)
    }
}
// ...
private companion object { const val RELOJ_MS = 60_000L }
```
y lo combina junto a los demás flujos de su `estado`. Se replica exactamente ese patrón (mismo intervalo de un minuto) en los otros dos `ViewModel`.

### 2.2 Diseño
**`ListaIncidenciasViewModel.kt`:**
```kotlin
private val filtros = MutableStateFlow(FiltrosIncidencias())

// «Hoy» no debe quedar desactualizado si la pantalla queda abierta mucho tiempo sin cambios de datos
private val reloj = flow {
    while (true) {
        emit(Instant.now())
        delay(RELOJ_MS)
    }
}

val estado: StateFlow<ListaIncidenciasUiState> =
    combine(observarIncidencias(), filtros, reloj) { todas, f, ahora ->
        ListaIncidenciasUiState(
            cargando = false,
            incidencias = todas.filtrar(f),
            total = todas.size,
            filtros = f,
            opcionesTecnico = opcionesTecnico(todas),
            opcionesEquipo = todas.map { it.codigoEquipo }.distinct().sorted(),
            ahora = ahora                           // antes: Instant.now() inline
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaIncidenciasUiState())

private companion object { const val RELOJ_MS = 60_000L }
```

**`DetalleIncidenciaViewModel.kt`:** mismo `reloj`, combinado como tercer flujo:
```kotlin
val estado: StateFlow<DetalleIncidenciaUiState> =
    combine(observarDetalle(incidenciaId), observarUsuarioActual(), reloj) { detalle, usuario, ahora ->
        // ... igual que hoy, solo cambia ahora = Instant.now() -> ahora = ahora
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleIncidenciaUiState())

private companion object { const val RELOJ_MS = 60_000L }
```
Se necesitan los imports `kotlinx.coroutines.delay` y `kotlinx.coroutines.flow.flow` en ambos archivos (ya presentes en `RegistroIncidenciaViewModel.kt` como referencia).

Esto no es una regla de dominio que dependa de la fecha (no es un caso de uso ni una función de `domain/`): es el mismo límite de presentación donde el proyecto ya acepta leer el reloj real (`RegistroIncidenciaViewModel`), así que no recibe `ahora` como parámetro ni se mueve a `domain/`.

### 2.3 Alternativas descartadas
| Alternativa | Por qué se descarta |
|---|---|
| Extraer un `reloj`/`TemporizadorReloj` compartido en `core/` que los tres `ViewModel` (registro, lista y detalle) reutilicen. | Es la solución «correcta» si el patrón crece a más pantallas, pero hoy son tres copias idénticas de cuatro líneas; `core/*` está, además, en la lista de archivos que requieren visto bueno antes de tocarlos. Se prefiere la duplicación pequeña y ya probada en `RegistroIncidenciaViewModel` (principio 1) antes que una abstracción nueva en un archivo compartido para un ahorro mínimo. |
| Agregar el temporizador solo a `ListaIncidenciasViewModel` y no a `DetalleIncidenciaViewModel`, ya que el detalle se visita por menos tiempo. | La spec, tras la decisión del usuario, pide el ajuste en los dos `ViewModel` por igual; el detalle también muestra fechas relativas (vía `DetalleIncidenciaScreen.kt`) y puede quedar abierto igual de tiempo. |
| Hacer que el `reloj` emita cada vez que cambie el minuto exacto del reloj del sistema, en vez de cada 60 segundos de `delay`. | Añade complejidad (habría que calcular cuánto falta para el siguiente minuto exacto) sin ninguna ganancia observable: la spec solo exige que la etiqueta no quede desactualizada por mucho tiempo, no al segundo. El propio RF-32 de la HU-03, su precedente, tampoco lo hace. |

## 3. Renombrar el chip de filtro «Estado» a «Marca» (RF-44)

### 3.1 Diagnóstico
En `ListaIncidenciasScreen.kt`, la función privada `Filtros(...)` llama a:
```kotlin
FiltroDesplegable("Estado", f.marca, MarcaSeguimiento.entries, { it.etiqueta() }, alFiltrarMarca)
```
El primer argumento (`"Estado"`) es el texto que se ve en el chip cuando no hay ninguna marca elegida (`titulo` en `FiltroDesplegable`); cuando sí hay una elegida, el chip muestra la etiqueta de esa marca (p. ej. «Asignada»), no la palabra «Estado». Esa palabra es, exactamente, la trampa «estado vs. marca» que señala `CLAUDE.md`: el filtro trabaja sobre `MarcaSeguimiento`, no sobre el `EstadoIncidencia` derivado (Pendiente/Solucionado) que se ve en el detalle (RF-51).

### 3.2 Diseño
Un solo cambio de literal:
```kotlin
FiltroDesplegable("Marca", f.marca, MarcaSeguimiento.entries, { it.etiqueta() }, alFiltrarMarca)
```
No hay ninguna prueba automatizada que dependa del texto «Estado» (se confirmó con `grep`: la única aparición del literal en `app/src` era esa misma línea), así que no hace falta ajustar ningún test. Las vistas previas de `ListaIncidenciasScreen.kt` (`@Preview`) no fijan el texto del chip; se siguen viendo bien sin cambios.

### 3.3 Alternativas descartadas
| Alternativa | Por qué se descarta |
|---|---|
| «Seguimiento» en vez de «Marca». | Ambas palabras evitan la confusión con `EstadoIncidencia`; se prefiere «Marca» porque coincide literalmente con el nombre del tipo de dominio `MarcaSeguimiento` ya usado en `CLAUDE.md` y en esta misma spec, lo que facilita relacionar la palabra del chip con la regla de negocio. |

## 4. Archivos

Leyenda: ✏️ se modifica · 🆕 se crea. Ningún archivo de esta lista está en «archivos compartidos» de `CLAUDE.md`.

| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/incidencias/domain/FiltrarIncidencias.kt` | `FiltrosIncidencias`, `SIN_TECNICO`, `POR_SINCRONIZAR`, `codigoParaMostrar()` y `filtrar()` (búsqueda y filtros, función pura). | RF-43, RF-44 |
| ✏️ `feature/incidencias/presentation/ListaIncidenciasViewModel.kt` | Quita la lógica movida (la importa de `domain`); agrega el `reloj` propio y el `combine` de tres flujos. | RF-44, caso límite |
| ✏️ `feature/incidencias/presentation/DetalleIncidenciaViewModel.kt` | Agrega el `reloj` propio y el `combine` de tres flujos. | caso límite |
| ✏️ `feature/incidencias/presentation/TextosIncidencia.kt` | Quita `POR_SINCRONIZAR`, `codigoParaMostrar()` y `SIN_TECNICO` (se movieron a `domain`). | RF-44 |
| ✏️ `feature/incidencias/presentation/ListaIncidenciasScreen.kt` | Renombra el chip «Estado» → «Marca»; agrega los `import` de `FiltrosIncidencias`, `SIN_TECNICO` y `codigoParaMostrar` desde `domain`. | RF-44 |
| ✏️ `feature/incidencias/presentation/DetalleIncidenciaScreen.kt` | Solo agrega los `import` de `codigoParaMostrar` y `SIN_TECNICO` desde `domain` (sin cambio de comportamiento). | — |
| ✏️ `feature/incidencias/presentation/IncidenciasNavigation.kt` | Solo agrega el `import` de `POR_SINCRONIZAR` desde `domain` (sin cambio de comportamiento). | — |
| 🆕 `app/src/test/.../FiltrarIncidenciasTest.kt` | Pruebas movidas/adaptadas de búsqueda y filtros (antes en `ListaIncidenciasViewModelTest`), probando `filtrar()` directamente. | RF-43, RF-44 |
| ✏️ `app/src/test/.../ListaIncidenciasViewModelTest.kt` | Quita los casos movidos a `FiltrarIncidenciasTest`; actualiza el `import` de `SIN_TECNICO`; agrega el caso del temporizador. | RF-42, RF-45, RF-46, caso límite |
| ✏️ `app/src/test/.../DetalleIncidenciaViewModelTest.kt` | Agrega el caso del temporizador. | caso límite |
| ✏️ `app/src/test/.../TextosIncidenciaTest.kt` | Solo actualiza el `import` de `codigoParaMostrar`. | — |

No se tocan `Rutas.kt`, `SigtecRaiz.kt`, `core/di/*`, `core/domain/model/*`, `core/theme/*` ni ningún otro archivo compartido: esta spec es puramente de `feature/incidencias` y sus pruebas.

## 5. Cómo se pinta en la interfaz
Un solo cambio visual: el chip de filtro que antes mostraba «Estado» cuando no había ninguna marca elegida ahora muestra «Marca»; al elegir una marca, sigue mostrando su etiqueta (p. ej. «Asignada»), igual que hoy. Las `@Preview` existentes de `ListaIncidenciasScreen.kt` (claro, oscuro, filtrada, sin resultados) no fijan ese texto en ningún `assert`, así que se ven correctas sin tocarlas; conviene revisarlas una vez (visualmente) para confirmar que el chip se lee bien con la palabra nueva. El temporizador de los dos `ViewModel` no cambia nada visible de inmediato: solo evita que una fecha relativa («Hoy») se quede desactualizada si la pantalla lleva mucho tiempo abierta sin recibir cambios.

## 6. Estrategia de pruebas

Se usa `./gradlew testDebugUnitTest` con JUnit 4, `kotlinx-coroutines-test`, `MainDispatcherRule` y los dobles existentes (`IncidenciaRepositoryFalso`, `SesionRepositoryDemo`). No se agregan librerías ni dobles nuevos.

| Prueba | Casos | RF |
|---|---|---|
| `FiltrarIncidenciasTest` (nuevo) | Búsqueda por código con o sin prefijo INC; por equipo sin importar mayúsculas; por descripción sin importar tildes; filtro por cada marca; filtro por técnico y por «Sin técnico»; filtro por equipo; combinación de búsqueda y filtros. Se prueban llamando a `lista.filtrar(FiltrosIncidencias(...))` directamente, sin `ViewModel` ni `StateFlow`. | RF-43, RF-44 |
| `ListaIncidenciasViewModelTest` (ajustado) | Se quitan los casos que ya cubre `FiltrarIncidenciasTest`; quedan: orden de la más reciente a la más antigua (RF-42), opciones de técnico y equipo desde los datos cargados (RF-45), limpiar quita todo de un toque (RF-46), y el caso nuevo del temporizador (`ahora` cambia tras avanzar el tiempo virtual sin que cambien los datos). | RF-42, RF-45, RF-46, caso límite |
| `DetalleIncidenciaViewModelTest` (ajustado) | Se agrega el caso nuevo del temporizador, con el mismo patrón de tiempo virtual. Los casos existentes (línea de tiempo, `puedeAsignar`/`puedeAtender`, no encontrada) no cambian. | caso límite |
| `TextosIncidenciaTest` | Sin cambios de comportamiento; solo cambia el `import` de `codigoParaMostrar` a `domain`. | RF-42/RF-48/RF-51/RF-55 |

**Patrón del caso del temporizador** (igual que `RegistroIncidenciaViewModelTest."la fecha mostrada avanza cada minuto..."`, sección 2 de este plan):
```kotlin
@Test
fun `ahora avanza cada minuto sin que cambien los datos`() = runTest {
    val vm = nuevoVm()
    val primera = vm.ui.ahora
    main.dispatcher.scheduler.advanceTimeBy(60_000L)
    main.dispatcher.scheduler.runCurrent()
    assertNotEquals(primera, vm.ui.ahora)   // no se fija el valor exacto, solo que cambió
}
```

**No se agrega prueba de pantalla:** el renombre del chip se verifica con las `@Preview` existentes (visual) y de forma manual; no hay framework de pruebas de Compose en el proyecto.

**Cierre:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde antes de abrir el PR o marcar la tarea como terminada.

## 7. Cobertura de RF

| RF / caso | Dónde vive | Prueba |
|---|---|---|
| RF-43 | `feature/incidencias/domain/FiltrarIncidencias.kt` (`filtrar()`) | `FiltrarIncidenciasTest` (nuevo) |
| RF-44 (filtros) | `feature/incidencias/domain/FiltrarIncidencias.kt` (`filtrar()`, `FiltrosIncidencias`) | `FiltrarIncidenciasTest` (nuevo) |
| RF-44 (chip «Marca») | `ListaIncidenciasScreen.kt` | `@Preview` + manual |
| RF-42, RF-45, RF-46 | `ListaIncidenciasViewModel.estado` (sin cambios de lógica) | `ListaIncidenciasViewModelTest` |
| RF-48, RF-51 (fecha relativa «Hoy») | `TextosIncidencia.fechaCorta()` (sin cambios); consume `ahora` del temporizador | `ListaIncidenciasViewModelTest` / `DetalleIncidenciaViewModelTest` (caso del temporizador) + manual |
| Caso límite — temporizador en la lista | `ListaIncidenciasViewModel.reloj` (nuevo) | `ListaIncidenciasViewModelTest` (caso nuevo, tiempo virtual) |
| Caso límite — temporizador en el detalle | `DetalleIncidenciaViewModel.reloj` (nuevo) | `DetalleIncidenciaViewModelTest` (caso nuevo, tiempo virtual) |

## 8. Respeto a la constitución
1. **Stack cerrado:** sin dependencias nuevas; se reutiliza el mismo patrón de `flow { while (true) { emit(...); delay(...) } }` que ya usa `RegistroIncidenciaViewModel`.
2. **Spec antes que código:** este plan solo implementa las tres «Decisión» que `spec.md` ya marca; no cambia ningún RF existente más allá del texto de RF-44 (que ya se actualizó en la propia spec para describir el resultado final).
3. **Lógica fuera de la pantalla:** la búsqueda y los filtros (RF-43, RF-44) pasan de `ListaIncidenciasViewModel` (presentación) a una función pura de `domain/`; ninguna `Screen` cambia su forma de acceder a datos.
4. **Toda regla tiene prueba:** los tres ajustes quedan cubiertos con pruebas unitarias nuevas o movidas; el único sin prueba automatizada es el texto del chip (es de pantalla), verificado con `@Preview` y de forma manual.
5. **Datos protegidos:** sin cambios; no se toca huella, contraseñas ni tokens.
6. **Español:** los nombres nuevos (`filtrar`, `FiltrosIncidencias`, `SIN_TECNICO`, `POR_SINCRONIZAR`, `codigoParaMostrar`) ya existían en español; solo cambian de archivo.

## 9. Archivos compartidos que toca (requieren visto bueno)
Ninguno. Los once archivos de la sección 4 viven en `feature/incidencias/presentation`, `feature/incidencias/domain` y sus pruebas en `app/src/test`; no aparecen en la lista textual de `CLAUDE.md` (`libs.versions.toml`, `app/build.gradle.kts`, `SigtecDatabase.kt` y entidades, `Rutas.kt`, `SigtecRaiz.kt`, `Resultado.kt`, `core/domain/model/*`, `MensajesError.kt`, `core/theme/*`, `core/di/*`, `AndroidManifest.xml`, `firestore.rules`, `feature/acceso/*` y los casos de uso de sesión), ni se tocan `core/sync/*` ni ningún DAO o entidad de Room. Este plan puede ejecutarse con la aprobación general, sin un paso de confirmación adicional como el que exigió RF-41 en el plan de la HU-03.
