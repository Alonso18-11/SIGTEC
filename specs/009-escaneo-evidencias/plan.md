# Plan 009 — Código del equipo por cámara y evidencia fotográfica (HU-09)

> Basado en `spec.md` (borrador, pendiente de aprobación con sus decisiones D-1 a D-12) y `docs/constitution.md`.
> La HU hace nacer el módulo `feature/captura` (`domain`, `data`, `di`), amplía `incidencias` (registro y
> detalle) y, si se aprueban D-6 y D-7, toca `atencion` y el borrado de datos locales de `core`. Se entrega en
> muchos PR pequeños (sección 1): cada uno tiene su rama, un commit por archivo o por prueba, con la HU al final
> del título, y la aprobación de la otra persona del equipo antes de fusionarse (`CLAUDE.md`, «Revisión cruzada»).
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas).

## 0. Adaptaciones de la plantilla y hallazgos del código real

- **Rutas ya listas.** `core/navigation/Rutas.kt` ya tiene `EscanearEtiqueta` y `CapturarEvidencia(incidenciaId)`,
  y `feature/captura/presentation/CapturaNavigation.kt` las registra con `PantallaEnConstruccion`. Esta HU
  reemplaza esas dos y deja `DictarTexto` (HU-10) como está. **No se toca `Rutas.kt`.**
- **El modelo de evidencias existe desde el esqueleto.** `core/domain/model/Evidencia.kt` (con
  `TipoEvidencia { FALLA, PIEZA_CAMBIADA }`), `EvidenciaEntity` (sin campos de sincronización, a propósito:
  «la foto vive solo en este celular») y `EvidenciaDao` (`observarPorIncidencia`, `guardar`, `eliminar`) ya están
  en `SigtecDatabase` (`version = 3`) y en `DatabaseModule`. **No cambia ninguna entidad ni ningún DAO, y
  `version` no sube.**
- **Sin dependencias nuevas.** CameraX 1.4.2 (`core`, `camera2`, `lifecycle`, `view`), ML Kit
  `text-recognition` 16.0.1 y `kotlinx-coroutines-play-services` ya están en `libs.versions.toml` y en
  `app/build.gradle.kts`. `text-recognition` es la variante que trae el modelo dentro de la app, así que lee sin
  conexión desde el primer uso (RNF «Sin conexión»). Las miniaturas se decodifican con `BitmapFactory`, sin
  librería de imágenes (P-6).
- **`feature/captura` solo tiene `presentation/CapturaNavigation.kt`.** Nacen `domain`, `data` y `di`.
- **Registro.** `RegistroIncidenciaScreen` ya tiene el botón de cámara (`alEscanear`) y un `supportingText`
  neutral («Escríbelo o léelo de la etiqueta con la cámara», comentario «Neutral hasta la HU-09»).
- **Detalle, después del PR #39.** `DetalleIncidenciaScreen` ya tiene `Seccion("Evidencias")` con un
  `BotonSecundario` «Agregar foto» (`AccionesDetalle.alAgregarFoto` → `Ruta.CapturarEvidencia`), **visible para
  todos**. Esta HU lo limita a `puedeAtender`, que ya existe en `DetalleIncidenciaUiState` (RF-923).
- **Informe técnico.** `InformeTecnicoScreen` (HU-08) no tiene evidencias (spec 008, «Fuera de alcance»). Solo
  se amplía si se aprueba D-6.
- **Los datos locales se borran en dos lugares.** `SesionRepositoryFirebase.cerrarSesion()` (que también usa
  `cerrarSesionPorRevocacion`) y `SincronizacionTiempoReal` (al entrar otra persona, D-12 de la spec 001). Los
  dos llaman a `clearAllTables()`; RF-942 agrega en ambos el borrado de la carpeta de fotos (D-7).
- **Copia de seguridad.** El manifest tiene `allowBackup="true"` con `backup_rules.xml` y
  `data_extraction_rules.xml` de ejemplo, sin reglas. RF-941 agrega solo la exclusión de la carpeta de fotos (D-9).
- **Errores.** `IncidenciaCerrada` (RF-609, HU-06) ya existe y se reutiliza para RF-926. Solo
  `LimiteFotosAlcanzado` es nuevo (D-2).
- **Efecto del PR #39 en esta HU.** Las pantallas nuevas usan `BarraSuperior` (solo trae la flecha «Volver», no
  la «X» de los prototipos: D-10), los botones de `Botones.kt`, `Seccion`/`EncabezadoSeccion` y `Espaciado.*`
  (que ya incluye `sm = 12.dp`). Los textos del registro y del detalle que se tocan ya tienen el estilo nuevo.
- **«`ahora` como parámetro».** `GuardarEvidencia` recibe `ahora: Instant`; el `ViewModel` pasa
  `Instant.now()` cuando la cámara termina de escribir la foto. Ningún caso de uso llama a `now()`.
- **Ámbitos de los commits.** `captura` (módulo de esta HU), `incidencias` (registro y detalle), `atencion`
  (informe, D-6) y `core` (carpeta de fotos y su borrado). Las pruebas van en commits `test(...)` propios.
- **Cámara del emulador.** El emulador simula la cámara con una escena virtual o la webcam de la PC; sirve para
  ver que la pantalla abre, pero el reconocimiento real y las fotos se prueban en un celular (spec, «Criterios de
  finalización»).

## 1. División en PR, dependencias y orden de fusión

El PR de esta spec es el #40 (`docs/hu09-camara-spec`). La implementación se divide en 16 PR, más uno opcional
(Q) que depende de D-8. M, N, O y Q dependen de una decisión: si no se aprueba, el PR no se abre y sus RF se
quitan de la spec.

| PR | Rama | Título del PR | Sale de | Depende de | En paralelo con |
|---|---|---|---|---|---|
| A | `feat/hu09-lectura-codigo` | `feat(captura): extraer el código del equipo del texto leído (HU-09)` | `main` | — | C, G, H |
| B | `feat/hu09-leer-codigo-catalogo` | `feat(captura): leer el código prefiriendo el catálogo del celular (HU-09)` | rama de A (**apilada**) | A | C, G, H |
| C | `feat/hu09-permiso-camara` | `feat(captura): permiso de cámara pedido al usarla, con su explicación (HU-09)` | `main` | — | A, B, D, G, H |
| D | `feat/hu09-escaneo-pantalla` | `feat(captura): pantalla «Escanear etiqueta» y su ViewModel (HU-09)` | rama de B (**apilada**) | B | C, G, H |
| E | `feat/hu09-escaneo-camara` | `feat(captura): leer la etiqueta con CameraX y ML Kit (HU-09)` | rama de D (**apilada**), con C ya en `main` | C, D | G a J |
| F | `feat/hu09-escaneo-registro` | `feat(incidencias): «Nueva incidencia» recibe el código escaneado (HU-09)` | rama de E (**apilada**) | E | G a J |
| G | `feat/hu09-carpeta-fotos` | `feat(core): carpeta interna de las fotos, fuera de la copia de seguridad (HU-09)` | `main` | — | A a F, H |
| H | `feat/hu09-evidencias-dominio` | `feat(captura): reglas y casos de uso de las fotos de evidencia (HU-09)` | `main` | — | A a G |
| I | `feat/hu09-evidencias-datos` | `feat(captura): repositorio de evidencias en Room y en la carpeta interna (HU-09)` | `main` con G y H | G, H | A a F |
| J | `feat/hu09-evidencia-pantalla` | `feat(captura): pantalla «Evidencia fotográfica» y su ViewModel (HU-09)` | rama de I (**apilada**) | I | A a F |
| K | `feat/hu09-evidencia-camara` | `feat(captura): tomar la foto con CameraX y guardarla (HU-09)` | rama de J (**apilada**), con E ya en `main` | E, J | L, O |
| L | `feat/hu09-evidencias-detalle` | `feat(incidencias): fotos en el detalle, ampliadas y «Agregar foto» solo para el técnico asignado (HU-09)` | rama de J (**apilada**) | J | K, O |
| M | `feat/hu09-eliminar-foto` | `feat(incidencias): eliminar una foto desde la vista ampliada (HU-09)` — **solo con D-5** | rama de L (**apilada**) | L | N, O |
| N | `feat/hu09-evidencias-informe` | `feat(atencion): sección «Evidencias» en el informe técnico (HU-09)` — **solo con D-6** | rama de L (**apilada**) | L | M, O |
| O | `feat/hu09-borrar-fotos-sesion` | `feat(core): borrar las fotos al cerrar sesión o al entrar otra persona (HU-09)` — **solo con D-7** | `main` con G | G | A a N |
| Q | `feat/hu09-aviso-fotos-cierre` | `feat(acceso): avisar cuántas fotos se borran al cerrar sesión (HU-09)` — **solo con D-8 (a)** | rama de O (**apilada**) | O | M, N |
| P | `docs/hu09-cierre` | `docs(captura): cierre de la HU-09 con las pruebas manuales (HU-09)` | rama del último PR fusionado | todos | — |

**Orden de fusión:** A → B → D; C en cualquier momento antes de E; E → F. En paralelo: G y H desde el principio,
I cuando estén los dos, y luego J. K necesita E y J; L necesita J; M y N necesitan L. O solo necesita G, y Q
necesita O. P va al final.

**Por qué esta división:**
- **A y B separados.** A es Kotlin puro con sus pruebas: se revisa en minutos y no toca Android ni Hilt. B agrega
  el repositorio del catálogo, su `@Binds` y el caso de uso que junta todo.
- **C aparte y temprano.** Es el único PR del escaneo que toca un archivo compartido (`AndroidManifest.xml`,
  D-11); conviene que Alonso lo revise solo.
- **D y E separados, igual que J y K.** D (y J) traen el `ViewModel`, sus pruebas y la pantalla con vistas
  previas, sin cámara: se revisan y prueban en la JVM. E (y K) conectan la cámara real, que solo se verifica en un
  celular. Así, si la cámara da problemas, no frena la revisión de la lógica.
- **F aparte.** Es otro módulo (`incidencias`, HU-03) y es el punto donde el escaneo por fin se ve completo.
- **G aparte.** Crea el único lugar de `core` que conoce la carpeta de fotos (lo usan I y O) y toca las reglas de
  copia de seguridad (`res/xml`): son decisiones de infraestructura (D-7, D-9) que conviene revisar solas.
- **H, I separados.** Dominio con el error nuevo (`Resultado.kt` y `MensajesError.kt`, compartidos, D-2) y sus
  pruebas; después, los datos.
- **L, M y N separados.** L cumple el criterio de la HU en el detalle. M y N dependen de decisiones (D-5, D-6):
  si no se aprueban, simplemente no se abren.
- **O y Q aparte.** Tocan el modelo de sesión y `core/sync`, que `CLAUDE.md` pide coordinar con Alonso; van en
  PR propios para que él los revise con foco.
- **P al final.** Pruebas manuales de punta a punta en un celular y cierre de la spec.

**Ramas apiladas.** Si el PR anterior sigue en revisión, la rama siguiente sale de la suya y, cuando el anterior
se fusiona, se actualiza con `git pull origin main`. Antes de dar por cerrado cada PR apilado, se revisa
`git log main..<rama>`: en la HU-08, dos PR apilados quedaron fuera de la fusión por el squash (`MEMORY.md`).

## 2. Archivos y commits por PR

Leyenda: 🆕 se crea · ✏️ se modifica · ⚠️ compartido de la lista de `CLAUDE.md` (necesita visto bueno; se
aprueba junto con este plan) · 🔶 sensible fuera de esa lista (`core/data`, `core/sync`, `res/xml`, archivos
de otro módulo `feature/`) · sin marca: archivo del módulo de la HU. Cada commit lleva `(HU-09)` al final y va
seguido de su push.

### Escaneo del código (PR A a F)

#### PR A — extraer el código del texto (Kotlin puro)
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/captura/domain/CodigoEquipo.kt` | `normalizarTexto`, `candidatosCodigo`, `elegirCodigo` y `data class LecturaCodigo(codigo, enCatalogo)` (sección 3.1 a 3.3). | RF-903, RF-904 |
| 🆕 `feature/captura/domain/RachaLectura.kt` | `data class RachaLectura(codigo, veces)` con `siguiente(lectura)` y `estable` (3.4). | RF-905, RF-908 |
| 🆕 (test) `feature/captura/domain/CodigoEquipoTest.kt` | Casos de la sección 7. | RF-903, RF-904 |
| 🆕 (test) `feature/captura/domain/RachaLecturaTest.kt` | Casos de la sección 7. | RF-905, RF-908 |

- **Compartidos:** ninguno. **Room:** sin cambios. **Cómo probarlo:** solo JUnit.
- **Commits:**
  1. `feat(captura): normalizar el texto leído de la etiqueta (HU-09)`
  2. `feat(captura): candidatos con la forma de los códigos de equipo (HU-09)`
  3. `feat(captura): elegir el código prefiriendo los del catálogo (HU-09)`
  4. `test(captura): extracción del código con confusiones, espacios y varios candidatos (HU-09)`
  5. `feat(captura): racha de lecturas iguales para dar un código por reconocido (HU-09)`
  6. `test(captura): la racha se cumple con 3 lecturas iguales y se reinicia (HU-09)`

#### PR B — leer el código con el catálogo del celular
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/captura/domain/CatalogoEquiposRepository.kt` | Interfaz: `suspend fun existe(codigo): Boolean`. | RF-904, RF-907 |
| 🆕 `feature/captura/domain/LeerCodigoEquipo.kt` | Caso de uso (pseudocódigo 4.1). | RF-902 a RF-904, RF-907 |
| 🆕 `feature/captura/data/CatalogoEquiposRepositoryImpl.kt` | `CatalogoDao.equipoPorCodigo`, `@IoDispatcher`; el `catch` relanza `CancellationException`, registra con `Log.w` y responde `false`. | RF-904 |
| 🆕 `feature/captura/di/CapturaModule.kt` | `@Binds` del repositorio. | — |
| 🆕 (test) `feature/captura/data/CatalogoEquiposRepositoryFalso.kt` | Catálogo en memoria con los 4 equipos de prueba. | — |
| 🆕 (test) `feature/captura/domain/LeerCodigoEquipoTest.kt` | Casos de la sección 7. | RF-904, RF-907 |

- **Compartidos:** ninguno (`CatalogoDao` se usa tal cual). **Cómo probarlo:** solo JUnit.
- **Commits:**
  1. `feat(captura): contrato del catálogo de equipos para el escaneo (HU-09)`
  2. `feat(captura): caso de uso LeerCodigoEquipo (HU-09)`
  3. `test(captura): catálogo falso con los equipos de prueba (HU-09)`
  4. `test(captura): LeerCodigoEquipo prefiere el catálogo y avisa si no está (HU-09)`
  5. `feat(captura): catálogo de equipos sobre Room (HU-09)`
  6. `feat(captura): módulo de Hilt del escaneo (HU-09)`

#### PR C — permiso de cámara
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️⚠️ `AndroidManifest.xml` | `<uses-permission android:name="android.permission.CAMERA" />` y `<uses-feature android:name="android.hardware.camera" android:required="false" />` (lint `PermissionImpliesUnsupportedHardware`). | RF-916, RF-920 (D-11) |
| 🆕 `feature/captura/presentation/PermisoCamara.kt` | `enum class VistaPermiso` y la función pura `vistaPermiso(...)` (3.5). | RF-916 a RF-920, RF-922 |
| 🆕 `feature/captura/presentation/ExplicacionPermisoCamara.kt` | Composable sin estado con el texto de cada `VistaPermiso` y sus botones; vistas previas en claro y oscuro. | RF-916, RF-918 a RF-920 |
| 🆕 `feature/captura/presentation/ConPermisoCamara.kt` | Composable con estado: consulta el permiso, lo pide, abre los ajustes y vuelve a consultarlo al volver a primer plano (sección 5.1). | RF-916 a RF-922 |
| 🆕 (test) `feature/captura/presentation/PermisoCamaraTest.kt` | Tabla de la sección 7. | RF-916 a RF-920, RF-922 |

- **Compartidos ⚠️:** `AndroidManifest.xml` (D-11). **Cómo probarlo:** vistas previas y JUnit; en el celular
  llega con E.
- **Commits:**
  1. `feat(captura): permiso de cámara en el manifest, sin exigirla (HU-09)`
  2. `feat(captura): regla de qué mostrar según el permiso de cámara (HU-09)`
  3. `test(captura): regla del permiso de cámara (HU-09)`
  4. `feat(captura): explicación del permiso de cámara con sus vistas previas (HU-09)`
  5. `feat(captura): pedir el permiso de cámara al usarla y volver a revisarlo (HU-09)`

#### PR D — «Escanear etiqueta» sin cámara: ViewModel y pantalla
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/captura/presentation/EscaneoViewModel.kt` | `EscaneoUiState(racha, reconocido)`; los textos entran por un flujo de capacidad 1 que descarta los viejos y se procesan en orden; `alLeerTexto`, `alVolverAEscanear` (4.2). | RF-902, RF-905 a RF-908 |
| 🆕 `feature/captura/presentation/EscanearEtiquetaScreen.kt` | `EscanearEtiquetaRoute` + `EscanearEtiquetaScreen`, con una ranura `vistaCamara` (sección 5.2); vistas previas buscando, reconocido y fuera del catálogo, en claro y oscuro. | RF-901, RF-906, RF-907, RF-912 |
| 🆕 (test) `feature/captura/presentation/EscaneoViewModelTest.kt` | Casos de la sección 7, con `MainDispatcherRule`. | RF-905 a RF-908 |

- **Compartidos:** ninguno. **Cómo probarlo:** JUnit y vistas previas (la navegación sigue en
  `PantallaEnConstruccion` hasta E).
- **Commits:**
  1. `feat(captura): EscaneoViewModel con la racha y el código reconocido (HU-09)`
  2. `test(captura): EscaneoViewModel reconoce, se detiene y vuelve a escanear (HU-09)`
  3. `feat(captura): pantalla «Escanear etiqueta» con su marco y el código reconocido (HU-09)`

#### PR E — cámara y ML Kit en el escaneo
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/captura/presentation/VistaCamara.kt` | `PreviewView` en `AndroidView`; `ProcessCameraProvider.awaitInstance` (P-2); enlaza los casos de uso que recibe al ciclo de vida de la pantalla; cámara trasera o la que haya; avisa si no abre. | RF-901, RF-915, RF-921 |
| 🆕 `feature/captura/presentation/LectorTextoEtiqueta.kt` | `ImageAnalysis.Analyzer` con el reconocedor de ML Kit; cierra cada imagen al terminar (P-3). | RF-902, RNF fluidez |
| ✏️ `feature/captura/presentation/EscanearEtiquetaScreen.kt` | La Route arma `ConPermisoCamara { VistaCamara(...) }` y entrega cada texto a `alLeerTexto`. | RF-901, RF-916 |
| ✏️ `feature/captura/presentation/CapturaNavigation.kt` | Reemplaza la `PantallaEnConstruccion` de `EscanearEtiqueta`; `const val CLAVE_CODIGO_ESCANEADO`; «Usar este código» lo deja en `previousBackStackEntry?.savedStateHandle` y vuelve. | RF-909, RF-912, RF-913 |

- **Compartidos:** ninguno. **Cómo probarlo:** en un celular, abrir «Nueva incidencia» → cámara → leer una
  etiqueta. El formulario todavía no recibe el código (llega en F).
- **Commits:**
  1. `feat(captura): vista de cámara reutilizable con CameraX (HU-09)`
  2. `feat(captura): lector de texto de la etiqueta con ML Kit (HU-09)`
  3. `feat(captura): «Escanear etiqueta» con el permiso y la cámara (HU-09)`
  4. `feat(captura): «Usar este código» devuelve el código a quien abrió el escaneo (HU-09)`

#### PR F — «Nueva incidencia» recibe el código
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️ `feature/incidencias/presentation/RegistroIncidenciaViewModel.kt` | `codigoLeidoConCamara` en el estado; `alUsarCodigoEscaneado(codigo)`; `alCambiarCodigo` lo apaga. | RF-910, RF-911 |
| ✏️ `feature/incidencias/presentation/RegistroIncidenciaScreen.kt` | La Route recibe `codigoEscaneado` y `alConsumirCodigo`; `supportingText` «Leído desde la etiqueta con la cámara»; vista previa nueva. | RF-910 |
| ✏️ `feature/incidencias/presentation/IncidenciasNavigation.kt` | Lee `CLAVE_CODIGO_ESCANEADO` del `savedStateHandle` de su propia entrada y lo borra al usarlo (sección 5.3). | RF-909, RF-910 |
| ✏️ (test) `RegistroIncidenciaViewModelTest.kt` | Casos nuevos de la sección 7. | RF-910, RF-911 |

- **Compartidos:** ninguno (es el módulo de la HU-03). **Cómo probarlo:** el flujo completo del escaneo en el
  celular.
- **Commits:**
  1. `feat(incidencias): el registro guarda si el código vino de la cámara (HU-09)`
  2. `test(incidencias): código escaneado y su indicación en el registro (HU-09)`
  3. `feat(incidencias): «Leído desde la etiqueta con la cámara» bajo el código (HU-09)`
  4. `feat(incidencias): el registro recibe el código al volver del escaneo (HU-09)`
