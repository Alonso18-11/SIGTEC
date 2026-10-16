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

### Fotos de evidencia (PR G a K)

#### PR G — carpeta interna de las fotos (`core`)
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕🔶 `core/data/AlmacenFotos.kt` | `@Singleton`. `@Inject constructor(@ApplicationContext)` más un constructor `internal` que recibe la carpeta raíz, para las pruebas. `archivoNuevo(incidenciaId, id)` (crea la subcarpeta), `borrar(ruta)` y `borrarTodo()`. La carpeta es `filesDir/evidencias` (P-4). Es el único lugar que conoce esa carpeta. | RF-925, RF-928, RF-940, RF-942 |
| ✏️🔶 `res/xml/backup_rules.xml` | `<exclude domain="file" path="evidencias/" />` (Android 11 o anterior). | RF-941 |
| ✏️🔶 `res/xml/data_extraction_rules.xml` | La misma exclusión en `cloud-backup` y en `device-transfer` (Android 12 o superior). | RF-941 |
| 🆕 (test) `core/data/AlmacenFotosTest.kt` | Sobre una `TemporaryFolder` de JUnit 4 (sección 7). | RF-928, RF-942 |

- **Sensibles 🔶:** `core/data` (archivo nuevo fuera del módulo de la HU, D-7) y `res/xml` (D-9).
  **Cómo probarlo:** JUnit; nadie lo usa todavía.
- **Commits:**
  1. `feat(core): carpeta interna de las fotos de evidencia (HU-09)`
  2. `test(core): crear y borrar fotos en una carpeta temporal (HU-09)`
  3. `feat(core): fotos fuera de la copia de seguridad en Android 11 o anterior (HU-09)`
  4. `feat(core): fotos fuera de la copia y la transferencia en Android 12 o superior (HU-09)`

#### PR H — reglas y casos de uso de las fotos
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️⚠️ `core/domain/Resultado.kt` | `data object LimiteFotosAlcanzado : ErrorDominio`. | RF-929 (D-2) |
| ✏️⚠️ `core/ui/MensajesError.kt` | «Llegaste al máximo de 10 fotos por incidencia.» | RF-929 |
| 🆕 `feature/captura/domain/EvidenciaRepository.kt` | Interfaz: `observarIncidencia(id)`, `incidencia(id)` (lectura fresca), `observarEvidencias(incidenciaId)`, `contarEvidencias(incidenciaId)`, `rutaParaNuevaFoto(incidenciaId, evidenciaId)`, `guardar(evidencia)` (si falla, borra el archivo) y `descartarArchivo(ruta)`. | RF-924 a RF-929 |
| 🆕 `feature/captura/domain/ReglasEvidencia.kt` | `MAX_FOTOS = 10` y la función pura `bloqueoParaFotos(...)` (3.6). | RF-923, RF-926, RF-929, RF-931 |
| 🆕 `feature/captura/domain/PrepararFoto.kt` | Genera el id (UUID) y pide la ruta: `FotoNueva(id, ruta)` (4.3). | RF-925 |
| 🆕 `feature/captura/domain/GuardarEvidencia.kt` | Caso de uso (4.4). | RF-925, RF-926, RF-928, RF-929 |
| 🆕 `feature/captura/domain/DescartarFoto.kt` | Borra el archivo de una foto que no llegó a enlazarse (4.5). | RF-928 |
| 🆕 `feature/captura/domain/ObservarCapturaEvidencia.kt` | Combina la incidencia, sus fotos y el usuario en `CapturaEvidencia(incidencia, evidencias, bloqueo)` (4.6). | RF-924, RF-929, RF-931 |
| 🆕 (test) `feature/captura/data/EvidenciaRepositoryFalso.kt` | En memoria, con `fallar` y la lista de archivos descartados. | — |
| 🆕 (test) `feature/captura/domain/ReglasEvidenciaTest.kt`, `GuardarEvidenciaTest.kt`, `ObservarCapturaEvidenciaTest.kt` | Casos de la sección 7. | RF-923, RF-926 a RF-929, RF-931 |

- **Compartidos ⚠️:** `Resultado.kt` y `MensajesError.kt` (D-2). **Cómo probarlo:** solo JUnit.
- **Commits:**
  1. `feat(core): error LimiteFotosAlcanzado y su mensaje (HU-09)`
  2. `feat(captura): contrato del repositorio de evidencias (HU-09)`
  3. `feat(captura): regla de quién puede agregar fotos y hasta cuántas (HU-09)`
  4. `test(captura): regla de las fotos por permiso, técnico, marca y máximo (HU-09)`
  5. `test(captura): repositorio de evidencias falso (HU-09)`
  6. `feat(captura): PrepararFoto con su id y su ruta (HU-09)`
  7. `feat(captura): GuardarEvidencia verifica antes de enlazar la foto (HU-09)`
  8. `test(captura): GuardarEvidencia sin permiso, cerrada, al máximo, correcta y con fallo (HU-09)`
  9. `feat(captura): DescartarFoto borra la foto que no se enlazó (HU-09)`
  10. `feat(captura): ObservarCapturaEvidencia junta la incidencia, sus fotos y el bloqueo (HU-09)`
  11. `test(captura): ObservarCapturaEvidencia con y sin sesión, reasignada y al máximo (HU-09)`

#### PR I — repositorio de evidencias
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/captura/data/EvidenciaRepositoryImpl.kt` | `EvidenciaDao`, las lecturas de `IncidenciaDao` que ya usa `IncidenciaRepositoryImpl`, `AlmacenFotos` y `@IoDispatcher`. Si Room falla al guardar, borra el archivo. El `catch` sigue la convención (4.7). | RF-925, RF-928, RF-939, RF-940 |
| ✏️ `feature/captura/di/CapturaModule.kt` | `@Binds` del repositorio de evidencias. | — |

- **Compartidos:** ninguno. **Room:** sin cambios de entidad ni de DAO. **Cómo probarlo:** compila con Hilt; se
  ve en el celular con K.
- **Commits:**
  1. `feat(captura): repositorio de evidencias en Room y en la carpeta interna (HU-09)`
  2. `feat(captura): @Binds del repositorio de evidencias (HU-09)`

#### PR J — «Evidencia fotográfica» sin cámara: ViewModel y pantalla
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/captura/presentation/CapturaEvidenciaViewModel.kt` | `CapturaEvidenciaUiState(cargando, captura, tipo, guardando, error)`. El tipo elegido vive en el `SavedStateHandle` para sobrevivir a la rotación. `alCambiarTipo`, `alDisparar(): FotoNueva?`, `alFotoTomada(foto)` y `alFalloCaptura(foto)` (4.8). | RF-924 a RF-931 |
| 🆕 `feature/captura/presentation/MiniaturaEvidencia.kt` | Composable que decodifica el archivo reducido fuera del hilo principal y muestra «Foto no disponible» si no puede (P-6). Rótulos «Falla 1» y «Pieza 1». | RF-925, RF-932, RF-935 |
| 🆕 `feature/captura/presentation/EvidenciaFotograficaScreen.kt` | Route + Screen con una ranura `vistaCamara`: selector, «Se guardará como…», disparador, miniaturas, «N fotos» y «Listo» (sección 5.4); vistas previas en claro y oscuro. | RF-924, RF-927, RF-929 a RF-931 |
| 🆕 (test) `feature/captura/presentation/CapturaEvidenciaViewModelTest.kt` | Casos de la sección 7, con `MainDispatcherRule`. | RF-924 a RF-931 |

- **Compartidos:** ninguno. **Cómo probarlo:** JUnit y vistas previas; la ruta sigue en
  `PantallaEnConstruccion` hasta K.
- **Commits:**
  1. `feat(captura): CapturaEvidenciaViewModel con el tipo, el disparo y el guardado (HU-09)`
  2. `test(captura): CapturaEvidenciaViewModel dispara, guarda, falla y respeta el máximo (HU-09)`
  3. `feat(captura): miniatura de una foto sin librerías nuevas (HU-09)`
  4. `feat(captura): pantalla «Evidencia fotográfica» con su selector y sus miniaturas (HU-09)`

#### PR K — tomar la foto con la cámara
| Archivo | Responsabilidad | RF |
|---|---|---|
| 🆕 `feature/captura/presentation/TomarFoto.kt` | `ImageCapture` configurado (lado mayor de 1600 px y calidad JPEG 85) y una función `suspend` que escribe la foto en la ruta de `FotoNueva` (P-5). | RF-925, RNF almacenamiento |
| ✏️ `feature/captura/presentation/EvidenciaFotograficaScreen.kt` | La Route arma `ConPermisoCamara { VistaCamara(...) }` con `ImageCapture`; al disparar, captura y avisa al `ViewModel` del resultado. | RF-924, RF-925, RF-928 |
| ✏️ `feature/captura/presentation/CapturaNavigation.kt` | Reemplaza la `PantallaEnConstruccion` de `CapturarEvidencia`; «Listo» y «Volver» regresan. | RF-924, RF-930 |

- **Compartidos:** ninguno. **Cómo probarlo:** en un celular, como técnico asignado, «Agregar foto» desde el
  detalle (el botón ya existe) → tomar fotos de los dos tipos.
- **Commits:**
  1. `feat(captura): tomar la foto reducida y comprimida con CameraX (HU-09)`
  2. `feat(captura): «Evidencia fotográfica» con el permiso y la cámara (HU-09)`
  3. `feat(captura): conectar «Evidencia fotográfica» en la navegación (HU-09)`

### Fotos en el detalle y en el informe, borrado y cierre (PR L a Q y P)

#### PR L — fotos en el detalle
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️ `feature/incidencias/domain/IncidenciaRepository.kt` | `fun observarEvidencias(incidenciaId): Flow<List<Evidencia>>`. | RF-932, RF-936 |
| ✏️ `feature/incidencias/data/IncidenciaRepositoryImpl.kt` | Inyecta `EvidenciaDao`, solo para leer (igual que `InformeTecnicoDao` en la HU-08). | RF-932 |
| ✏️ `feature/incidencias/domain/ObservarDetalleIncidencia.kt` | `DetalleIncidencia` agrega `evidencias: List<Evidencia> = emptyList()`, combinando `repo.observarEvidencias(id)`. | RF-932, RF-936 |
| 🆕 `feature/captura/presentation/SeccionEvidencias.kt` | Composable sin estado: miniaturas en filas que se acomodan al ancho, la nota «Las fotos se guardan solo en el celular que las tomó.», «Sin fotos en este celular.» y «Agregar foto» solo si `puedeAgregar` (sección 5.5). Lo reutiliza N. | RF-923, RF-932, RF-933 |
| 🆕 `feature/captura/presentation/FotoAmpliada.kt` | `Dialog` a pantalla completa con la foto, su tipo y su fecha y hora; se cierra con «Volver» o atrás. | RF-934, RF-935 |
| ✏️ `feature/incidencias/presentation/DetalleIncidenciaScreen.kt` | Cambia la sección actual por `SeccionEvidencias(puedeAgregar = estado.puedeAtender)`; recuerda qué foto está ampliada; vistas previas con fotos. | RF-923, RF-932 a RF-936 |
| ✏️ (test) `feature/incidencias/data/IncidenciaRepositoryFalso.kt` | Fotos en memoria. | — |
| ✏️ (test) `feature/incidencias/domain/ObservarDetalleIncidenciaTest.kt` | Con fotos, sin fotos y el orden. | RF-932, RF-936 |
| ✏️ (test) `DetalleIncidenciaViewModelTest.kt` | Las fotos llegan al estado; `puedeAtender` sigue la regla de RF-923. | RF-923, RF-932 |

- **Compartidos:** ninguno. `DetalleIncidenciaScreen` importa dos composables de `feature/captura` (P-7).
  **Cómo probarlo:** con K fusionado, el flujo completo de las fotos en el celular.
- **Commits:**
  1. `feat(incidencias): el repositorio lee las fotos de la incidencia (HU-09)`
  2. `test(incidencias): fotos en el repositorio falso de incidencias (HU-09)`
  3. `feat(incidencias): el detalle observa también las fotos de la incidencia (HU-09)`
  4. `test(incidencias): el detalle con fotos, sin fotos y en orden (HU-09)`
  5. `feat(captura): sección «Evidencias» reutilizable (HU-09)`
  6. `feat(captura): foto ampliada con su tipo y su fecha (HU-09)`
  7. `feat(incidencias): fotos en el detalle y «Agregar foto» solo para el técnico asignado (HU-09)`
  8. `test(incidencias): las fotos llegan al estado del detalle (HU-09)`

#### PR M — eliminar una foto (solo con D-5)
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️ `feature/captura/domain/EvidenciaRepository.kt`, `data/EvidenciaRepositoryImpl.kt` y (test) `EvidenciaRepositoryFalso.kt` | `eliminar(evidencia)`: borra la fila y después el archivo. | RF-937 |
| 🆕 `feature/captura/domain/EliminarEvidencia.kt` | Caso de uso (4.9). | RF-937 |
| 🆕 (test) `feature/captura/domain/EliminarEvidenciaTest.kt` | Casos de la sección 7. | RF-937 |
| ✏️ `feature/captura/presentation/FotoAmpliada.kt` | «Eliminar foto» (`BotonPeligro`) con su confirmación, solo si `puedeEliminar`. | RF-937 |
| ✏️ `feature/incidencias/presentation/DetalleIncidenciaViewModel.kt` | `alEliminarFoto(evidencia)` con su error; inyecta `EliminarEvidencia` (P-7). | RF-937 |
| ✏️ `feature/incidencias/presentation/DetalleIncidenciaScreen.kt` | Pasa `puedeEliminar = estado.puedeAtender` y la acción. | RF-937 |
| ✏️ (test) `DetalleIncidenciaViewModelTest.kt` | Eliminar con éxito y con error. | RF-937 |

- **Compartidos:** ninguno. **Commits:**
  1. `feat(captura): el repositorio elimina la foto y su archivo (HU-09)`
  2. `feat(captura): EliminarEvidencia solo para el técnico asignado y pendiente (HU-09)`
  3. `test(captura): EliminarEvidencia sin permiso, cerrada, correcta y con fallo (HU-09)`
  4. `feat(captura): «Eliminar foto» con confirmación en la foto ampliada (HU-09)`
  5. `feat(incidencias): eliminar una foto desde el detalle (HU-09)`
  6. `test(incidencias): eliminar una foto desde el detalle (HU-09)`

#### PR N — «Evidencias» en el informe técnico (solo con D-6)
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️🔶 `feature/atencion/presentation/InformeTecnicoViewModel.kt` | Combina `ObservarCapturaEvidencia(incidenciaId)` para las fotos y para saber si puede agregar. | RF-938 |
| ✏️🔶 `feature/atencion/presentation/InformeTecnicoScreen.kt` | `SeccionEvidencias` antes de «Confirmar cierre con huella», como en el prototipo 9; la Route recibe `alAgregarFoto`; vistas previas. | RF-938 |
| ✏️🔶 `feature/atencion/presentation/AtencionNavigation.kt` | `alAgregarFoto` navega a `Ruta.CapturarEvidencia(id)`. | RF-938 |
| ✏️🔶 (test) `InformeTecnicoViewModelTest.kt` | Las fotos llegan al estado y lo escrito se conserva. | RF-938 |

- **Sensibles 🔶:** el módulo `atencion` de la HU-08 (Alonso). **Commits:**
  1. `feat(atencion): el informe observa las fotos de la incidencia (HU-09)`
  2. `test(atencion): fotos en el estado del informe sin perder lo escrito (HU-09)`
  3. `feat(atencion): sección «Evidencias» en el informe técnico (HU-09)`
  4. `feat(atencion): «Agregar foto» desde el informe técnico (HU-09)`

#### PR O — borrar las fotos con los datos locales (solo con D-7)
| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️🔶 `core/data/SesionRepositoryFirebase.kt` | Inyecta `AlmacenFotos`; después de `clearAllTables()` en `cerrarSesion()` llama a `borrarTodo()`. Cubre también `cerrarSesionPorRevocacion`, que usa `cerrarSesion()`. | RF-942 |
| ✏️🔶 `core/sync/SincronizacionTiempoReal.kt` | Lo mismo después del `clearAllTables()` de `debeBorrarDatosAlEntrar` (D-12 de la spec 001). | RF-942 |
| ✏️ `specs/001-inicio-sesion/spec.md` | RF-10 y RF-11 incluyen las fotos; se quita «Las fotos se agregarán cuando exista la HU de captura». | RF-942 |
| ✏️ `MEMORY.md` | Se quita el pendiente «Fotos: … (D-7)», después de `git pull origin main`. | — |

- **Sensibles 🔶:** modelo de sesión y `core/sync`: revisión de Alonso (D-7). **Cómo probarlo:** a mano en el
  celular (Firebase no se prueba en la JVM, igual que RF-10 en la spec 001); `borrarTodo()` ya tiene prueba en G.
- **Commits:**
  1. `feat(core): borrar las fotos al cerrar sesión (HU-09)`
  2. `feat(core): borrar las fotos al entrar otra persona (HU-09)`
  3. `docs(acceso): RF-10 y RF-11 incluyen las fotos de evidencia (HU-09)`
  4. `docs(core): pendiente de las fotos resuelto en MEMORY (HU-09)`

#### PR Q — aviso de las fotos al cerrar sesión (solo con D-8, opción a)
Se detalla cuando se responda D-8. Ampliaría la confirmación de RF-13 (`feature/acceso`, HU-01) con «y N fotos de
evidencia», contando las filas de `EvidenciaDao`; toca el modelo de sesión y necesita la revisión de Alonso.

#### PR P — pruebas manuales y cierre
| Archivo | Cambio |
|---|---|
| ✏️ `specs/009-escaneo-evidencias/spec.md` | «Estado: borrador» → «Estado: completa», con la fecha y qué se probó. |
| ✏️ `specs/009-escaneo-evidencias/tasks.md` | Tareas marcadas. |
| ✏️ `MEMORY.md` | Línea de la HU-09: «completa (RF-901 a RF-942)», después de `git pull origin main`. |

- **Commits:**
  1. `docs(captura): pruebas manuales de la HU-09 en el celular (HU-09)`
  2. `docs(captura): spec de la HU-09 completa (HU-09)`
  3. `docs(captura): HU-09 completa en MEMORY (HU-09)`

## 3. Funciones puras de lógica

Todas viven en Kotlin puro, sin `import android.*` (principio 3), y se prueban en la JVM.

| # | Firma (dónde) | Regla | RF |
|---|---|---|---|
| 3.1 | `normalizarTexto(texto: String): String` (`CodigoEquipo.kt`) | Mayúsculas; los guiones `‐ ‑ ‒ – — ― −` pasan a `-`; `\s*-\s*` pasa a `-`. | RF-903 |
| 3.2 | `candidatosCodigo(texto: String): List<String>` (`CodigoEquipo.kt`) | Sobre el texto normalizado busca `(?<![A-Z0-9-])([A-Z0-9]{2,4})-([A-Z0-9]{2,5})-([A-Z0-9]{3})(?![A-Z0-9-])`. Corrige cada parte según su posición: en las dos primeras, `0→O`, `1→I`, `5→S`, `8→B`, `2→Z`; en la última, `O→0`, `I→1`, `L→1`, `S→5`, `B→8`, `Z→2`. Se queda solo con lo que después cumple `[A-Z]{2,4}-[A-Z]{2,5}-[0-9]{3}`, sin repetidos y en orden de lectura. Una fecha como `2026-10-15` no pasa, porque `6` no se corrige a letra. | RF-903 |
| 3.3 | `elegirCodigo(candidatos: List<String>, delCatalogo: Set<String>): LecturaCodigo?` (`CodigoEquipo.kt`) | `null` si no hay candidatos. Si alguno está en `delCatalogo`, el primero de esos, con `enCatalogo = true`. Si no, el primero de todos, con `enCatalogo = false`. | RF-904, RF-907 |
| 3.4 | `RachaLectura.siguiente(codigo: String?): RachaLectura` y `val estable: Boolean` (`RachaLectura.kt`) | Sin código, la racha queda vacía. El mismo código suma 1 a `veces`; otro código empieza en 1. `estable` es `veces >= LECTURAS_IGUALES`, con `LECTURAS_IGUALES = 3`. | RF-905, RF-908 |
| 3.5 | `vistaPermiso(tieneCamara: Boolean, concedido: Boolean, yaSePidio: Boolean, androidLoExplicaria: Boolean): VistaPermiso` (`PermisoCamara.kt`) | Sin cámara → `SIN_CAMARA`. Concedido → `CAMARA`. `androidLoExplicaria` (lo que responde `shouldShowRequestPermissionRationale`) → `NEGADO`. Sin pedir todavía → `EXPLICAR`. Ya se pidió y Android no lo explicaría → `IR_A_AJUSTES`. | RF-916 a RF-920, RF-922 |
| 3.6 | `bloqueoParaFotos(usuario: Usuario?, incidencia: Incidencia?, cantidad: Int?): ErrorDominio?` (`ReglasEvidencia.kt`) | Sin usuario, sin `ATENDER_INCIDENCIA`, sin incidencia o con otro técnico asignado → `SinPermiso`. Estado `SOLUCIONADO` → `IncidenciaCerrada`. `cantidad != null && cantidad >= MAX_FOTOS` → `LimiteFotosAlcanzado`. Si no, `null`. Al eliminar se pasa `cantidad = null`, porque el máximo no aplica. | RF-923, RF-926, RF-929, RF-931, RF-937 |

`vistaPermiso` vive en `presentation` porque traduce estados de Android, pero es una función pura y se prueba igual.
Con ella no hace falta guardar entre sesiones si ya se pidió el permiso: si Android dejó de mostrar su diálogo, el
primer «Permitir cámara» vuelve al instante negado y la siguiente vista ya es `IR_A_AJUSTES`.

## 4. Algoritmos en pseudocódigo

### 4.1 `LeerCodigoEquipo(texto)` (RF-902 a RF-904, RF-907)
```text
candidatos = candidatosCodigo(texto)
si candidatos está vacía: devolver null
delCatalogo = candidatos que cumplen catalogo.existe(c)       // uno o dos por imagen
devolver elegirCodigo(candidatos, delCatalogo)
```
No llama a `VerificarPermiso`: leer no cambia datos ni muestra nada que la persona no tenga ya delante (P-1).

### 4.2 `EscaneoViewModel` (RF-905 a RF-908)
```text
textos = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST)
al crearse: viewModelScope.launch { textos.collect { procesar(it) } }      // en orden, uno a la vez (P-9)

alLeerTexto(texto):                       // lo llama el analizador desde su hilo
    si estado.reconocido == null: textos.tryEmit(texto)

procesar(texto):
    si estado.reconocido != null: terminar                                 // RF-906: ya no analiza
    lectura = leerCodigo(texto)
    racha = estado.racha.siguiente(lectura?.codigo)
    estado = estado.copy(racha = racha, reconocido = lectura si racha.estable, si no null)

alVolverAEscanear():  estado = EscaneoUiState()                             // RF-908
```
Mientras hay un código reconocido, la pantalla tampoco le pasa imágenes al lector (RF-906).

### 4.3 `PrepararFoto(incidenciaId)` (RF-925)
```text
id = UUID nuevo
devolver FotoNueva(id, ruta = repo.rutaParaNuevaFoto(incidenciaId, id))
```

### 4.4 `GuardarEvidencia(incidenciaId, foto, tipo, ahora)` (RF-925, RF-926, RF-928, RF-929)
```text
usuario = verificarPermiso(ATENDER_INCIDENCIA)
    si es error: repo.descartarArchivo(foto.ruta); devolver el error
incidencia = repo.incidencia(incidenciaId)                  // lectura fresca: pudo cambiar al disparar
cantidad = repo.contarEvidencias(incidenciaId)
bloqueo = bloqueoParaFotos(usuario, incidencia, cantidad)
    si no es null: repo.descartarArchivo(foto.ruta); devolver Error(bloqueo)
evidencia = Evidencia(foto.id, incidenciaId, tipo, foto.ruta, fechaCaptura = ahora)
devolver repo.guardar(evidencia) convertido a Resultado<Evidencia>   // si Room falla, guardar ya borró el archivo
```

### 4.5 `DescartarFoto(foto)` (RF-928)
```text
repo.descartarArchivo(foto.ruta)                            // la cámara falló o escribió a medias
```

### 4.6 `ObservarCapturaEvidencia(incidenciaId)` (RF-924, RF-929, RF-931)
```text
combine(repo.observarIncidencia(id), repo.observarEvidencias(id), observarUsuarioActual()) {
    incidencia, fotos, usuario ->
    incidencia?.let { CapturaEvidencia(it, fotos, bloqueo = bloqueoParaFotos(usuario, it, fotos.size)) }
}
```
Como observa Room, si la incidencia se cierra o se reasigna desde otro celular, el bloqueo aparece solo (RF-931).

### 4.7 `EvidenciaRepositoryImpl.guardar` (RF-928)
```text
withContext(io) {
    try   { evidenciaDao.guardar(evidencia.toEntity()); Exito(Unit) }
    catch CancellationException { almacen.borrar(evidencia.rutaArchivo); relanzar }
    catch Exception e { almacen.borrar(evidencia.rutaArchivo); Log.w(TAG, "No se pudo enlazar la foto", e)
                        Error(Desconocido(e.message)) }
}
```
`rutaParaNuevaFoto` devuelve `almacen.archivoNuevo(incidenciaId, id).absolutePath` (P-4).

### 4.8 `CapturaEvidenciaViewModel` (RF-924 a RF-931)
```text
tipo = savedStateHandle.getStateFlow("tipo", FALLA)                      // sobrevive a la rotación
estado = combine(observarCaptura(id), tipo, accion) en un CapturaEvidenciaUiState

alDisparar(): FotoNueva?
    si accion.guardando o estado.captura?.bloqueo != null: devolver null    // RF-927, RF-929, RF-931
    accion.guardando = true; accion.error = null
    devolver prepararFoto(id)

alFotoTomada(foto):   launch { r = guardarEvidencia(id, foto, tipo.value, Instant.now())
                               accion = (guardando = false, error = r como error o null) }
alFalloCaptura(foto): launch { descartarFoto(foto); accion = (guardando = false, error = Desconocido(null)) }
```

### 4.9 `EliminarEvidencia(evidencia)` (RF-937, solo con D-5)
```text
usuario = verificarPermiso(ATENDER_INCIDENCIA); si es error, devolverlo
incidencia = repo.incidencia(evidencia.incidenciaId)
bloqueoParaFotos(usuario, incidencia, cantidad = null)?.let { devolver Error(it) }
devolver repo.eliminar(evidencia)                          // primero la fila, después el archivo
```

### 4.10 Borrado de las fotos con los datos locales (RF-942, solo con D-7)
```text
clearAllTables()           // primero Room: ninguna fila queda apuntando a un archivo ya borrado
almacen.borrarTodo()       // si falla, se registra con Log.w y no impide cerrar sesión ni entrar;
                           // los archivos sin fila no se ven y se borran en el siguiente borrado
```

## 5. Cómo se pinta en Compose

Todas las pantallas nuevas usan `Scaffold(contentWindowInsets = WindowInsets(0))` con `BarraSuperior(titulo,
alVolver)`, solo roles de `MaterialTheme.colorScheme` y estilos de `MaterialTheme.typography`, `Espaciado.*` y
los botones de `Botones.kt`. Las `Screen` reciben estado y lambdas, y la vista de la cámara llega como una ranura
`vistaCamara: @Composable () -> Unit`: así las vistas previas muestran un recuadro `surfaceVariant` con «Vista de la
cámara», como el prototipo, sin abrir ninguna cámara.

### 5.1 `ConPermisoCamara(textoNegado, contenido)` (PR C)
- `concedido` sale de `ContextCompat.checkSelfPermission` y se vuelve a leer en cada `ON_RESUME`
  (`LifecycleEventEffect`), para enterarse al volver de los ajustes (RF-919) o si lo quitaron desde ahí (RF-922).
- `yaSePidio` es un `rememberSaveable`; `androidLoExplicaria` sale de `shouldShowRequestPermissionRationale` de la
  `Activity` (`MainActivity` ya es una `AppCompatActivity`); `tieneCamara`, de
  `hasSystemFeature(FEATURE_CAMERA_ANY)`.
- `rememberLauncherForActivityResult(RequestPermission())` pide el permiso; «Abrir ajustes» lanza
  `ACTION_APPLICATION_DETAILS_SETTINGS` con el paquete de la app.
- `when (vistaPermiso(...))`: `CAMARA` dibuja `contenido()`; las demás dibujan `ExplicacionPermisoCamara`: ícono,
  título, el texto de RF-916 o de RF-918 a RF-920 y `BotonPrincipal` «Permitir cámara» o «Abrir ajustes».

### 5.2 «Escanear etiqueta» (PR D y E)
- `BarraSuperior("Escanear etiqueta", alVolver)` y, debajo, un `Box` que ocupa todo: la ranura de la cámara y, encima,
  un `Canvas` que oscurece con `colorScheme.scrim` (con transparencia) todo lo que queda fuera del marco y dibuja
  sus cuatro esquinas con `colorScheme.primary`. El marco mide el 85 % del ancho, con proporción 2:1, como la
  etiqueta.
- Bajo el marco, la indicación en una pastilla `inverseSurface`/`inverseOnSurface`, que se lee sobre cualquier
  imagen en claro y en oscuro (texto blanco fijo no está permitido fuera del tema).
- Con un código reconocido, un panel `surface` abajo: ícono de check sobre `SigtecTheme.estados.solucionado`,
  «Código reconocido · sin conexión» (`bodyMedium`, `onSurfaceVariant`), el código (`headlineSmall`) y, si no está en
  el catálogo, «No está en el catálogo de este celular» con ícono y el color de `SigtecTheme.estados.sinAsignar`;
  luego `BotonSecundario` «Volver a escanear» y `BotonPrincipal` «Usar este código», cada uno con `weight(1f)`.
- En E, la Route arma `ConPermisoCamara(textoNegado = "También puedes escribir el código a mano en el formulario.")
  { VistaCamara(casos = [Preview, ImageAnalysis con LectorTextoEtiqueta]) }` y solo pasa textos al `ViewModel`
  mientras `reconocido == null`.

### 5.3 Devolver el código por `savedStateHandle` (PR E y F, P-8)
```kotlin
// CapturaNavigation.kt: la única clave del resultado, también para la HU-16
const val CLAVE_CODIGO_ESCANEADO = "codigoEscaneado"

composable<Ruta.EscanearEtiqueta> {
    EscanearEtiquetaRoute(
        alVolver = { navController.popBackStack() },
        alUsarCodigo = { codigo ->
            navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_CODIGO_ESCANEADO, codigo)
            navController.popBackStack()
        }
    )
}

// IncidenciasNavigation.kt
composable<Ruta.RegistroIncidencia> { entrada ->
    val codigoEscaneado by entrada.savedStateHandle
        .getStateFlow<String?>(CLAVE_CODIGO_ESCANEADO, null).collectAsStateWithLifecycle()
    RegistroIncidenciaRoute(
        /* … lo de hoy … */
        codigoEscaneado = codigoEscaneado,
        alConsumirCodigo = { entrada.savedStateHandle[CLAVE_CODIGO_ESCANEADO] = null }
    )
}

// RegistroIncidenciaRoute
LaunchedEffect(codigoEscaneado) {
    codigoEscaneado?.let { vm.alUsarCodigoEscaneado(it); alConsumirCodigo() }
}
```
Como la clave se borra al usarla, una rotación o un cambio de tema no vuelve a aplicar el código (el aprendizaje de
`MEMORY.md` sobre las claves de `LaunchedEffect`).

### 5.4 «Evidencia fotográfica» (PR J y K)
- `BarraSuperior("Evidencia fotográfica", alVolver)` y, debajo, «INC-0124 · PC-CONT-014» (`bodyMedium`,
  `onSurfaceVariant`), porque `BarraSuperior` no tiene subtítulo.
- `SingleChoiceSegmentedButtonRow` con «Falla» y «Pieza cambiada».
- La ranura de la cámara, recortada con la forma del tema, con la pastilla «Se guardará como: foto de la falla».
- Una fila desplazable de `MiniaturaEvidencia` de 64 dp, cada una con su rótulo («Falla 1», «Pieza 1»).
- Abajo: «N fotos» (`bodyMedium`), el disparador y `BotonPrincipal` «Listo». El disparador es un círculo de 72 dp
  con borde `primary` y `contentDescription = "Tomar foto"`, deshabilitado mientras guarda o con bloqueo (P-11).
- Sobre esa fila, el mensaje del bloqueo (RF-929, RF-931) o del error (`error.mensaje()`).
- En K, la Route arma `ConPermisoCamara { VistaCamara(casos = [Preview, ImageCapture]) }`. Al disparar:
  `vm.alDisparar()?.let { foto -> lanzar tomarFoto(imageCapture, foto.ruta) y avisar alFotoTomada(foto) o
  alFalloCaptura(foto) }`.

### 5.5 `SeccionEvidencias` y `FotoAmpliada` (PR L, M y N)
- `Seccion("Evidencias")` con un `FlowRow` de miniaturas cuadradas de 96 dp (borde punteado `outlineVariant` mientras
  carga, como el prototipo), cada una con su rótulo y tocable; si `puedeAgregar`, al final va el
  `BotonSecundario` «Agregar foto» que ya existe; debajo, la nota de RF-933 (`bodySmall`, `onSurfaceVariant`).
- `FotoAmpliada` es un `Dialog` a pantalla completa (`usePlatformDefaultWidth = false`) con `BarraSuperior(tipo,
  alVolver)`, la foto ajustada al ancho y la fecha y hora de captura. Con D-5 suma `BotonPeligro` «Eliminar foto» y
  un `AlertDialog` de confirmación.

### 5.6 `MiniaturaEvidencia(ruta, ladoPx)` (PR J, P-6)
```text
estado = produceState(Cargando, ruta) {
    value = withContext(Dispatchers.IO) { decodificarReducida(ruta, ladoPx) } ?: NoDisponible
}
decodificarReducida: BitmapFactory con inJustDecodeBounds → inSampleSize (potencia de 2) → decodificar →
                     girar con Matrix según la orientación de android.media.ExifInterface
                     (CameraX guarda la orientación en el EXIF, no en los píxeles)
Cargando → recuadro punteado · NoDisponible → «Foto no disponible» · Lista → Image(bitmap, contentScale = Crop)
```
`FotoAmpliada` usa la misma función con un lado de 2048 px.

## 6. Decisiones técnicas

Las decisiones de producto (D-1 a D-12) están en «Dudas abiertas» de `spec.md`. Estas son las del cómo:

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| P-1 | `LeerCodigoEquipo` no llama a `VerificarPermiso`. | Leer no cambia datos ni muestra nada que la persona no tenga delante; el escaneo se abre desde pantallas que ya exigen sesión. | Exigir `REGISTRAR_INCIDENCIA`: ataría el escaneo a quien lo abre, y la búsqueda del historial (HU-16) usa `CONSULTAR_HISTORIAL`. |
| P-2 | `VistaCamara` sobre `ProcessCameraProvider.awaitInstance`, con los casos de uso explícitos (`Preview` más `ImageAnalysis` o `ImageCapture`). | Permite fijar `KEEP_ONLY_LATEST`, la resolución y la calidad JPEG. `awaitInstance` es `suspend` (CameraX 1.4) y evita el `ListenableFuture` de `getInstance()`. | `LifecycleCameraController` de `camera-view`: menos código, pero no deja fijar la calidad JPEG (RNF de almacenamiento). |
| P-3 | Analizador propio con ML Kit: `InputImage.fromMediaImage(imagen, rotación)` y `close()` del `ImageProxy` en `addOnCompleteListener`. | `camera-mlkit-vision` (`MlKitAnalyzer`) no está en `libs.versions.toml`. | Agregar `camera-mlkit-vision`: sería una dependencia nueva (principio 1). |
| P-4 | Fotos en `filesDir/evidencias/<incidenciaId>/<id>.jpg`; Room guarda la ruta absoluta. | `Evidencia.rutaArchivo` ya existe para eso; `filesDir` es privado de la app; una carpeta por incidencia deja borrar por grupos. Como las fotos no van en la copia de seguridad (RF-941), nunca se restauran en otro celular con otra ruta. | Ruta relativa a `filesDir`: solo serviría si las fotos se restauraran. `cacheDir`: Android puede vaciarla solo. |
| P-5 | La foto se toma ya reducida (`ResolutionSelector` cercano a 1600 × 1200) y con `setJpegQuality(85)`. | No hay que decodificar ni recomprimir en el celular. | Tomarla a resolución completa y reducirla después: doble trabajo y un archivo temporal grande. |
| P-6 | Miniaturas con `BitmapFactory` (`inSampleSize`) y la orientación de `android.media.ExifInterface` (API 24, por debajo de minSdk 26), decodificadas en un composable con `produceState` y `withContext(Dispatchers.IO)`. | No agrega dependencias. La regla de `CLAUDE.md` de no escribir `Dispatchers.IO` es para los repositorios (para inyectar el despachador en las pruebas); este composable no tiene prueba JVM que lo necesite. | Coil (dependencia nueva). Decodificar en el `ViewModel`: guardaría imágenes en el estado y obligaría al detalle a depender de `captura/data`. Si se prefiere no escribir `Dispatchers.IO` en ningún lado, se inyecta un decodificador con `@IoDispatcher` mediante un `EntryPoint` de Hilt. |
| P-7 | El detalle (`incidencias`) usa `SeccionEvidencias` y `FotoAmpliada` de `captura` y, con D-5, el caso de uso `EliminarEvidencia`. | Las fotos son de `captura`; el detalle ya navega a sus rutas. Así hay un solo dibujo de las fotos. | Copiar los composables en `incidencias`; o una ruta nueva para la foto ampliada, que tocaría `Rutas.kt` (compartido). |
| P-8 | El código vuelve por el `savedStateHandle` de la entrada anterior, con una sola clave pública (`CLAVE_CODIGO_ESCANEADO`), que la navegación lee y entrega como evento que se borra al usarlo. | Es lo que pide `CLAUDE.md` y sirve igual para la HU-16. | Leerlo desde el `SavedStateHandle` del `ViewModel`: es otro objeto distinto del de la entrada de navegación y el valor no llegaría. |
| P-9 | Los textos del analizador entran por un `MutableSharedFlow` de capacidad 1 que descarta los viejos y se procesan en orden. | El analizador llama desde otro hilo; así nunca se procesan dos a la vez ni se acumulan. | Una corrutina por texto: dos consultas al catálogo podrían cruzarse y desordenar la racha. |
| P-10 | `GuardarEvidencia` vuelve a verificar todo después de tomar la foto, aunque el disparador estuviera habilitado. | La incidencia puede cerrarse o reasignarse mientras se toma (RF-931); si no pasa, se borra el archivo. | Verificar solo antes de disparar: dejaría fotos enlazadas a una incidencia ajena. |
| P-11 | El disparador es circular. | Es la forma que todos reconocen como «tomar foto». Es la única excepción a las esquinas de 3 a 8 dp del rediseño. | Un botón rectangular de `Botones.kt`: confunde con «Listo». |
| P-12 | Las fotos se guardan al tomarlas, una por una, y no al pulsar «Listo». | Si la app se cierra o falla a mitad, no se pierde nada (RF-930). | Juntarlas y guardarlas al final: una sola falla perdería todas. |

## 7. Estrategia de pruebas

`./gradlew testDebugUnitTest` con JUnit 4, `kotlinx-coroutines-test`, `MainDispatcherRule` y dobles propios
(`SesionRepositoryDemo`, `IncidenciaRepositoryFalso`, y los nuevos `CatalogoEquiposRepositoryFalso` y
`EvidenciaRepositoryFalso`). No se agregan librerías. Las pruebas de `captura` van en
`feature/captura/{domain,data,presentation}`, con el mismo paquete que la clase real.

| Prueba (PR) | Casos | RF |
|---|---|---|
| `CodigoEquipoTest` (A) | Mayúsculas, guiones raros y espacios alrededor del guion; `PC-C0NT-O14` → `PC-CONT-014`; el texto del prototipo 13 («SOFT CORPORATION · ACTIVO FIJO», «Área: Contabilidad») da solo `PC-CONT-014`; una fecha (`2026-10-15`) y un anexo (`Anexo 2314`) no son candidatos; dos códigos salen sin repetir y en orden; `elegirCodigo` prefiere el del catálogo, toma el primero si ninguno está y devuelve `null` sin candidatos. | RF-903, RF-904, RF-907 |
| `RachaLecturaTest` (A) | Tres iguales seguidas dan `estable`; dos iguales y una distinta, no; una lectura sin código reinicia; una racha nueva empieza vacía. | RF-905, RF-908 |
| `LeerCodigoEquipoTest` (B) | Código del catálogo → `enCatalogo = true`; fuera del catálogo → `false`; dos códigos con uno en el catálogo → ese; texto sin código → `null`. | RF-904, RF-907 |
| `PermisoCamaraTest` (C) | La tabla de 3.5: sin cámara; concedido; sin pedir; negado con explicación; negado sin explicación después de pedirlo; quitado desde ajustes. | RF-916 a RF-920, RF-922 |
| `EscaneoViewModelTest` (D) | Tres textos con el mismo código → `reconocido`; alternancia → nada; con un código reconocido, los textos nuevos se ignoran; `alVolverAEscanear` limpia todo; un código fuera del catálogo llega con `enCatalogo = false`. | RF-905 a RF-908 |
| `RegistroIncidenciaViewModelTest` (F, se amplía) | El código escaneado reemplaza el escrito y enciende `codigoLeidoConCamara`; editarlo a mano lo apaga; recibir un código limpia un error anterior. | RF-910, RF-911 |
| `AlmacenFotosTest` (G) | `archivoNuevo` crea la subcarpeta de la incidencia dentro de la raíz; `borrar` borra ese archivo y no falla si ya no existe; `borrarTodo` vacía la carpeta de fotos y no toca nada fuera de ella. | RF-928, RF-940, RF-942 |
| `ReglasEvidenciaTest` (H) | Sin usuario; jefe (sin `ATENDER_INCIDENCIA`); otro técnico; incidencia inexistente; cerrada; con 10 fotos; con 9 fotos pasa; con `cantidad = null` (eliminar) pasa aunque tenga 10. | RF-923, RF-926, RF-929, RF-931, RF-937 |
| `GuardarEvidenciaTest` (H) | Sin sesión → `SinPermiso` y el archivo descartado; otro técnico → `SinPermiso` y descartado; cerrada → `IncidenciaCerrada` y descartado; al máximo → `LimiteFotosAlcanzado` y descartado; correcto → la evidencia con su tipo, su ruta y `fechaCaptura == ahora`; el repositorio falla → el error y nada guardado. | RF-925, RF-926, RF-928, RF-929 |
| `ObservarCapturaEvidenciaTest` (H) | Técnico asignado sin bloqueo; sin sesión → `SinPermiso`; al llegar a 10 → `LimiteFotosAlcanzado`; al reasignarse en el repositorio falso → `SinPermiso`; incidencia inexistente → `null`. | RF-924, RF-929, RF-931 |
| `CapturaEvidenciaViewModelTest` (J) | Empieza en «Falla» y cambia; el tipo sale del `SavedStateHandle` (rotación); `alDisparar` devuelve una `FotoNueva` y deshabilita el disparador; un segundo `alDisparar` mientras guarda devuelve `null`; `alFotoTomada` guarda con el tipo elegido y vuelve a habilitar; `alFalloCaptura` descarta el archivo y muestra el error; con bloqueo, `alDisparar` devuelve `null`. | RF-924 a RF-931 |
| `ObservarDetalleIncidenciaTest` y `DetalleIncidenciaViewModelTest` (L, se amplían) | Las fotos llegan al detalle en orden; sin fotos, lista vacía; `puedeAtender` (que muestra «Agregar foto») solo para el técnico asignado mientras siga pendiente. | RF-923, RF-932, RF-936 |
| `EliminarEvidenciaTest` y `DetalleIncidenciaViewModelTest` (M) | Sin permiso; otro técnico; cerrada → `IncidenciaCerrada`; correcto; fallo del repositorio; el error llega al estado del detalle. | RF-937 |
| `InformeTecnicoViewModelTest` (N, se amplía) | Las fotos llegan al estado del informe; lo escrito en el formulario se conserva. | RF-938 |

**No se agregan pruebas instrumentadas:** no cambia ninguna entidad ni ningún DAO (`EvidenciaDao` se usa tal cual),
que es lo que `CLAUDE.md` pide cubrir con ellas.

**Sin prueba JVM, igual que en las HU anteriores:** `CatalogoEquiposRepositoryImpl` y `EvidenciaRepositoryImpl`
(dependen de Room, como los demás `*RepositoryImpl`), `VistaCamara`, `LectorTextoEtiqueta`, `TomarFoto`,
`ConPermisoCamara` y `MiniaturaEvidencia` (dependen de la cámara, de ML Kit o de Android), y el borrado en
`SesionRepositoryFirebase`/`SincronizacionTiempoReal` (Firebase; `borrarTodo()` sí se prueba en G).

**Verificado a mano en un celular real** (el emulador simula la cámara), con capturas en claro y oscuro:
- E: el permiso (explicación, negar, negar otra vez, «Abrir ajustes»), la lectura de las 4 etiquetas de prueba en
  modo avión, la velocidad (menos de 2 segundos) y que la cámara se suelta al salir.
- F: el código llega a «Nueva incidencia» con su indicación; editarlo la quita; salir sin confirmar no cambia nada.
- K: fotos de los dos tipos, el máximo de 10, el doble toque, y el disparador deshabilitado si la incidencia se
  cierra desde otro celular.
- L, M y N: las fotos en el detalle (y en el informe con D-6), ampliadas, eliminadas (con D-5), y otro técnico sin
  «Agregar foto»; como jefe, en otro celular, la misma incidencia sin fotos.
- O: después de cerrar sesión, y después de entrar otra persona, la carpeta `files/evidencias` vacía
  (`adb shell run-as com.softcorp.sigtec ls files/evidencias`).

**Cierre de cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de
lint antes de abrirlo o de marcarlo listo.
