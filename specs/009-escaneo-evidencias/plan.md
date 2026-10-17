# Plan 009 — Código del equipo por cámara y evidencia fotográfica (HU-09)

> Basado en `spec.md` (aprobada el 2026-10-16, con D-1 a D-12 respondidas) y `docs/constitution.md`.
> La HU hace nacer el módulo `feature/captura` (`domain`, `data`, `di`), amplía `incidencias` (registro y detalle) y
> `atencion` (informe, D-6), y toca el borrado y el aviso de datos locales de `core` y `acceso` (D-7, D-8). Como
> `main` solo admite squash, se entrega en muchos PR de un solo cambio (sección 1): cada uno tiene su rama, commits
> chicos con push inmediato y la HU al final del título, y la aprobación de la otra persona del equipo antes de
> fusionarse (`CLAUDE.md`, «Revisión cruzada»).
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas).

## 0. Adaptaciones de la plantilla y hallazgos del código real

- **Rutas ya listas.** `core/navigation/Rutas.kt` ya tiene `EscanearEtiqueta` y `CapturarEvidencia(incidenciaId)`,
  y `feature/captura/presentation/CapturaNavigation.kt` las registra con `PantallaEnConstruccion`. Esta HU
  reemplaza esas dos y deja `DictarTexto` (HU-10) como está. **No se toca `Rutas.kt`.**
- **El modelo de evidencias existe desde el esqueleto.** `core/domain/model/Evidencia.kt` (con
  `TipoEvidencia { FALLA, PIEZA_CAMBIADA }`), `EvidenciaEntity` (sin campos de sincronización, a propósito:
  «la foto vive solo en este celular») y `EvidenciaDao` (`observarPorIncidencia`, `guardar`, `eliminar`) ya están
  en `SigtecDatabase` (`version = 3`) y en `DatabaseModule`. **No cambia ninguna entidad y `version` no sube.** El único cambio de
  DAO es una consulta nueva para contar las fotos (PR-52, D-8), con su prueba instrumentada.
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
- **Informe técnico.** `InformeTecnicoScreen` (HU-08) no tiene evidencias (spec 008, «Fuera de alcance»). Se
  amplía por D-6.
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
- **El aviso de cierre de sesión (RF-13, HU-01).** `EvaluarCierreSesion` lee `EstadoDatos` y llama a
  `evaluarBorrado(pendientes)` (`core/domain/ReglasSesion.kt`), que devuelve `DecisionBorrado.Confirmar(pendientes)`;
  el texto sale de `TextosSalida.kt` y el diálogo vive en `AjustesSeguridadScreen`. RF-943 agrega las fotos a esa
  cadena sin cambiar sus opciones (4.11).

## 1. División en PR, dependencias y orden de fusión

`main` solo admite squash, así que cada PR es un commit en `main`. La HU se divide en el mayor número de PR que
todavía tienen sentido solos: **un cambio por PR** (una regla pura con sus pruebas, un caso de uso, un repositorio,
un `ViewModel`, una pantalla, una pieza de cámara o de permiso, un ajuste de navegación), y cada uno compila y pasa
`assembleDebug testDebugUnitTest lintDebug` por sí mismo. Son 3 PR de documentación (la spec en el #40, este plan y
las tareas) y 58 de código y cierre (PR-01 a PR-57, más PR-56b por D-13).

**Bases.**
- Si no depende de nada abierto, sale de `main`.
- Si depende de un PR abierto, sale de su rama y en GitHub su base es esa rama (columna «Base»).
- Si además necesita otros PR abiertos (columna «Trae»), se hace `git merge` de sus ramas y la descripción lo dice;
  su diff los incluye hasta que se fusionan. Si ya están en `main`, basta `git pull origin main`.
- Cuando se fusiona la base, GitHub pasa el PR apilado a `main`; entonces se hace `git pull origin main` y se revisa
  `git log main..<rama>`, porque en la HU-08 dos PR apilados quedaron fuera por el squash (`MEMORY.md`).

**CI de los PR apilados (P-13).** Hoy `.github/workflows/ci.yml` solo corre en los PR hacia `main`
(`pull_request: branches: [main]`), así que un PR apilado no tiene CI hasta que su base se fusiona. PR-01 quita ese
filtro para que todos los PR tengan su check. Conviene fusionarlo primero: las ramas que salgan después ya lo traen.

| PR | Rama | Título | Base | Trae | RF |
|---|---|---|---|---|---|
| PR-01 | `chore/ci-pr-apiladas` | `chore(ci): correr el CI también en los PR apilados (HU-09)` | `main` | — | — |
| PR-02 | `feat/hu09-normalizar-texto` | `feat(captura): normalizar el texto leído de la etiqueta (HU-09)` | `main` | — | RF-903 |
| PR-03 | `feat/hu09-candidatos-codigo` | `feat(captura): candidatos con la forma de los códigos de equipo (HU-09)` | PR-02 | — | RF-903 |
| PR-04 | `feat/hu09-elegir-codigo` | `feat(captura): elegir el código prefiriendo el catálogo (HU-09)` | PR-03 | — | RF-904, RF-907 |
| PR-05 | `feat/hu09-racha-lectura` | `feat(captura): racha de 3 lecturas iguales para reconocer un código (HU-09)` | `main` | — | RF-905, RF-908 |
| PR-06 | `feat/hu09-catalogo-equipos` | `feat(captura): catálogo de equipos del celular para el escaneo (HU-09)` | `main` | — | RF-904 |
| PR-07 | `feat/hu09-leer-codigo` | `feat(captura): caso de uso LeerCodigoEquipo (HU-09)` | PR-04 | PR-06 | RF-902 a RF-904, RF-907 |
| PR-08 | `feat/hu09-escaneo-viewmodel` | `feat(captura): EscaneoViewModel con la racha y el código reconocido (HU-09)` | PR-07 | PR-05 | RF-905 a RF-908 |
| PR-09 | `feat/hu09-escaneo-pantalla` | `feat(captura): pantalla «Escanear etiqueta» sin cámara (HU-09)` | PR-08 | — | RF-901, RF-906, RF-907, RF-912 |
| PR-10 | `feat/hu09-permiso-manifest` | `feat(captura): permiso de cámara en el manifest, sin exigirla (HU-09)` | `main` | — | RF-916, RF-920 |
| PR-11 | `feat/hu09-regla-permiso` | `feat(captura): regla de qué mostrar según el permiso de cámara (HU-09)` | `main` | — | RF-916 a RF-920, RF-922 |
| PR-12 | `feat/hu09-explicacion-permiso` | `feat(captura): explicación del permiso de cámara (HU-09)` | PR-11 | — | RF-916, RF-918 a RF-920 |
| PR-13 | `feat/hu09-con-permiso-camara` | `feat(captura): pedir el permiso de cámara al usarla (HU-09)` | PR-12 | — | RF-916 a RF-922 |
| PR-14 | `feat/hu09-vista-camara` | `feat(captura): vista de cámara reutilizable con CameraX (HU-09)` | `main` | — | RF-901, RF-915, RF-921 |
| PR-15 | `feat/hu09-lector-texto` | `feat(captura): lector de texto de la etiqueta con ML Kit (HU-09)` | `main` | — | RF-902, RF-914 |
| PR-16 | `feat/hu09-escaneo-camara` | `feat(captura): «Escanear etiqueta» con el permiso y la cámara (HU-09)` | PR-09 | PR-13, PR-14, PR-15 | RF-901, RF-906, RF-916 |
| PR-17 | `feat/hu09-escaneo-navegacion` | `feat(captura): «Escanear etiqueta» en la navegación, con la devolución del código (HU-09)` | PR-16 | — | RF-909, RF-912, RF-913 |
| PR-18 | `feat/hu09-registro-codigo-camara` | `feat(incidencias): el registro recuerda si el código vino de la cámara (HU-09)` | `main` | — | RF-910, RF-911 |
| PR-19 | `feat/hu09-registro-indicacion` | `feat(incidencias): «Leído desde la etiqueta con la cámara» bajo el código (HU-09)` | PR-18 | — | RF-910 |
| PR-20 | `feat/hu09-registro-recibe-codigo` | `feat(incidencias): el registro recibe el código al volver del escaneo (HU-09)` | PR-19 | PR-17 | RF-909, RF-910 |
| PR-21 | `feat/hu09-almacen-fotos` | `feat(core): carpeta interna de las fotos de evidencia (HU-09)` | `main` | — | RF-925, RF-928, RF-940 |
| PR-22 | `feat/hu09-copia-fotos-android11` | `feat(core): fotos fuera de la copia de seguridad en Android 11 o anterior (HU-09)` | `main` | — | RF-941 |
| PR-23 | `feat/hu09-copia-fotos-android12` | `feat(core): fotos fuera de la copia y la transferencia en Android 12 o superior (HU-09)` | `main` | — | RF-941 |
| PR-24 | `feat/hu09-error-limite-fotos` | `feat(core): error LimiteFotosAlcanzado y su mensaje (HU-09)` | `main` | — | RF-929 |
| PR-25 | `feat/hu09-reglas-evidencia` | `feat(captura): regla de quién agrega fotos y hasta cuántas (HU-09)` | PR-24 | — | RF-923, RF-926, RF-929, RF-931, RF-937 |
| PR-26 | `feat/hu09-contrato-evidencias` | `feat(captura): contrato del repositorio de evidencias y su doble (HU-09)` | `main` | — | RF-924 a RF-929 |
| PR-27 | `feat/hu09-repositorio-evidencias` | `feat(captura): repositorio de evidencias en Room y en la carpeta interna (HU-09)` | PR-26 | PR-06, PR-21 | RF-925, RF-928, RF-939, RF-940 |
| PR-28 | `feat/hu09-preparar-foto` | `feat(captura): PrepararFoto con su id y su ruta (HU-09)` | PR-26 | — | RF-925 |
| PR-29 | `feat/hu09-guardar-evidencia` | `feat(captura): GuardarEvidencia verifica antes de enlazar la foto (HU-09)` | PR-28 | PR-25 | RF-925, RF-926, RF-928, RF-929 |
| PR-30 | `feat/hu09-descartar-foto` | `feat(captura): DescartarFoto borra la foto que no se enlazó (HU-09)` | PR-26 | — | RF-928 |
| PR-31 | `feat/hu09-observar-captura` | `feat(captura): ObservarCapturaEvidencia junta la incidencia, sus fotos y el bloqueo (HU-09)` | PR-25 | PR-26 | RF-924, RF-929, RF-931 |
| PR-32 | `feat/hu09-captura-viewmodel` | `feat(captura): CapturaEvidenciaViewModel con el tipo, el disparo y el guardado (HU-09)` | PR-31 | PR-27, PR-29, PR-30 | RF-924 a RF-931 |
| PR-33 | `feat/hu09-miniatura-evidencia` | `feat(captura): miniatura de una foto sin librerías nuevas (HU-09)` | `main` | — | RF-932, RF-935 |
| PR-34 | `feat/hu09-captura-pantalla` | `feat(captura): pantalla «Evidencia fotográfica» sin cámara (HU-09)` | PR-32 | PR-33 | RF-924, RF-927, RF-929 a RF-931 |
| PR-35 | `feat/hu09-tomar-foto` | `feat(captura): tomar la foto reducida y comprimida con CameraX (HU-09)` | `main` | — | RF-925 |
| PR-36 | `feat/hu09-captura-camara` | `feat(captura): «Evidencia fotográfica» con el permiso y la cámara (HU-09)` | PR-34 | PR-13, PR-14, PR-35 | RF-924, RF-925, RF-928 |
| PR-37 | `feat/hu09-captura-navegacion` | `feat(captura): «Evidencia fotográfica» en la navegación (HU-09)` | PR-36 | PR-17 | RF-924, RF-930 |
| PR-38 | `feat/hu09-incidencias-lee-fotos` | `feat(incidencias): el repositorio lee las fotos de la incidencia (HU-09)` | `main` | — | RF-932, RF-936 |
| PR-39 | `feat/hu09-detalle-observa-fotos` | `feat(incidencias): el detalle observa también las fotos (HU-09)` | PR-38 | — | RF-932, RF-936 |
| PR-40 | `feat/hu09-seccion-evidencias` | `feat(captura): sección «Evidencias» reutilizable (HU-09)` | PR-33 | — | RF-923, RF-932, RF-933 |
| PR-41 | `feat/hu09-foto-ampliada` | `feat(captura): foto ampliada con su tipo y su fecha (HU-09)` | PR-33 | — | RF-934, RF-935 |
| PR-42 | `feat/hu09-detalle-fotos` | `feat(incidencias): fotos en el detalle y «Agregar foto» solo para el técnico asignado (HU-09)` | PR-39 | PR-40, PR-41 | RF-923, RF-932 a RF-936 |
| PR-43 | `feat/hu09-repositorio-elimina` | `feat(captura): el repositorio elimina la foto y su archivo (HU-09)` | PR-27 | — | RF-937 |
| PR-44 | `feat/hu09-eliminar-evidencia` | `feat(captura): EliminarEvidencia solo para el técnico asignado y pendiente (HU-09)` | PR-43 | PR-25 | RF-937 |
| PR-45 | `feat/hu09-boton-eliminar-foto` | `feat(captura): «Eliminar foto» con confirmación en la foto ampliada (HU-09)` | PR-41 | — | RF-937 |
| PR-46 | `feat/hu09-detalle-elimina-foto` | `feat(incidencias): eliminar una foto desde el detalle (HU-09)` | PR-42 | PR-44, PR-45 | RF-937 |
| PR-47 | `feat/hu09-informe-observa-fotos` | `feat(atencion): el informe observa las fotos de la incidencia (HU-09)` | PR-31 | PR-27 | RF-938 |
| PR-48 | `feat/hu09-informe-evidencias` | `feat(atencion): sección «Evidencias» en el informe técnico (HU-09)` | PR-47 | PR-40 | RF-938 |
| PR-49 | `feat/hu09-informe-agregar-foto` | `feat(atencion): «Agregar foto» desde el informe técnico (HU-09)` | PR-48 | — | RF-938 |
| PR-50 | `feat/hu09-borrar-fotos-cierre` | `feat(core): borrar las fotos al cerrar sesión (HU-09)` | PR-21 | — | RF-942 |
| PR-51 | `feat/hu09-borrar-fotos-otra-persona` | `feat(core): borrar las fotos al entrar otra persona (HU-09)` | PR-21 | — | RF-942 |
| PR-52 | `feat/hu09-contar-fotos` | `feat(core): EvidenciaDao cuenta las fotos del celular (HU-09)` | `main` | — | RF-943 |
| PR-53 | `feat/hu09-estado-datos-fotos` | `feat(core): el estado de los datos incluye las fotos del celular (HU-09)` | PR-52 | — | RF-943 |
| PR-54 | `feat/hu09-regla-borrado-fotos` | `feat(core): confirmar el cierre de sesión también por las fotos (HU-09)` | PR-53 | — | RF-943 |
| PR-55 | `feat/hu09-textos-fotos` | `feat(acceso): textos de las fotos que se pierden al salir (HU-09)` | `main` | — | RF-943 |
| PR-56 | `feat/hu09-aviso-cierre-fotos` | `feat(acceso): el aviso de cierre de sesión cuenta las fotos (HU-09)` | PR-54 | PR-55 | RF-943 |
| PR-56b | `feat/hu09-aviso-ingreso-fotos` | `feat(acceso): el aviso al entrar otra persona cuenta las fotos (HU-09)` | PR-56 | — | RF-944 |
| PR-57 | `docs/hu09-cierre` | `docs(captura): cierre de la HU-09 con las pruebas manuales (HU-09)` | el último fusionado | todos | RF-901 a RF-943 |

**Orden de trabajo:** el de la tabla. Los que salen de `main` pueden revisarse y fusionarse en cualquier orden; los
apilados, después de su base. El flujo completo se ve en el celular en PR-17 y PR-20 (escaneo), PR-37 (fotos),
PR-42 (detalle), PR-49 (informe), PR-51 (borrado), PR-56 y PR-56b (avisos).

## 2. Archivos y commits por PR

Leyenda: 🆕 se crea · ✏️ se modifica · ⚠️ compartido de la lista de `CLAUDE.md` (aprobado con D-1 a D-12) ·
🔶 sensible fuera de esa lista · sin marca: módulo de la HU. Cada commit lleva `(HU-09)` al final y va seguido de su
push; las pruebas van en commits `test(...)` propios. Todos los PR de código terminan con
`./gradlew assembleDebug testDebugUnitTest lintDebug` en verde.

### Proceso
| PR | Archivos | Commits |
|---|---|---|
| PR-01 | ✏️🔶 `.github/workflows/ci.yml`: `pull_request` sin filtro de rama base (P-13). | 1. `chore(ci): correr el CI también en los PR apilados` |

### Lectura del código (PR-02 a PR-09)
| PR | Archivos | Commits |
|---|---|---|
| PR-02 | 🆕 `feature/captura/domain/CodigoEquipo.kt` (`normalizarTexto`, 3.1); 🆕 (test) `CodigoEquipoTest.kt` | 1. `feat(captura): normalizar el texto leído de la etiqueta` · 2. `test(captura): mayúsculas, guiones y espacios del texto leído` |
| PR-03 | ✏️ `CodigoEquipo.kt` (`candidatosCodigo`, 3.2); ✏️ (test) `CodigoEquipoTest.kt` | 1. `feat(captura): candidatos con la forma de los códigos de equipo` · 2. `test(captura): candidatos con confusiones, texto alrededor, fechas y anexos` |
| PR-04 | ✏️ `CodigoEquipo.kt` (`LecturaCodigo`, `elegirCodigo`, 3.3); ✏️ (test) `CodigoEquipoTest.kt` | 1. `feat(captura): elegir el código prefiriendo el catálogo` · 2. `test(captura): elegir con y sin catálogo, y sin candidatos` |
| PR-05 | 🆕 `feature/captura/domain/RachaLectura.kt` (3.4); 🆕 (test) `RachaLecturaTest.kt` | 1. `feat(captura): racha de lecturas iguales` · 2. `test(captura): la racha se cumple con 3 lecturas iguales y se reinicia` |
| PR-06 | 🆕 `feature/captura/domain/CatalogoEquiposRepository.kt`; 🆕 `feature/captura/data/CatalogoEquiposRepositoryImpl.kt` (`CatalogoDao.equipoPorCodigo`, `@IoDispatcher`, `catch` de la convención); 🆕 `feature/captura/di/CapturaModule.kt`; 🆕 (test) `feature/captura/data/CatalogoEquiposRepositoryFalso.kt` | 1. `feat(captura): contrato del catálogo de equipos` · 2. `feat(captura): catálogo de equipos sobre Room` · 3. `feat(captura): módulo de Hilt de captura` · 4. `test(captura): catálogo falso con los equipos de prueba` |
| PR-07 | 🆕 `feature/captura/domain/LeerCodigoEquipo.kt` (4.1); 🆕 (test) `LeerCodigoEquipoTest.kt` | 1. `feat(captura): caso de uso LeerCodigoEquipo` · 2. `test(captura): LeerCodigoEquipo prefiere el catálogo y avisa si no está` |
| PR-08 | 🆕 `feature/captura/presentation/EscaneoViewModel.kt` (4.2, P-9); 🆕 (test) `EscaneoViewModelTest.kt` | 1. `feat(captura): EscaneoViewModel con la racha y el código reconocido` · 2. `test(captura): EscaneoViewModel reconoce, se detiene y vuelve a escanear` |
| PR-09 | 🆕 `feature/captura/presentation/EscanearEtiquetaScreen.kt` (Route + Screen con la ranura `vistaCamara`, 5.2; vistas previas) | 1. `feat(captura): pantalla «Escanear etiqueta» con su marco` · 2. `feat(captura): panel del código reconocido y aviso fuera del catálogo` |

### Permiso y cámara (PR-10 a PR-17)
| PR | Archivos | Commits |
|---|---|---|
| PR-10 | ✏️⚠️ `AndroidManifest.xml`: `CAMERA` y `uses-feature android.hardware.camera` con `required="false"` (D-11). | 1. `feat(captura): permiso de cámara en el manifest, sin exigirla` |
| PR-11 | 🆕 `feature/captura/presentation/PermisoCamara.kt` (`VistaPermiso`, `vistaPermiso`, 3.5); 🆕 (test) `PermisoCamaraTest.kt` | 1. `feat(captura): regla de qué mostrar según el permiso de cámara` · 2. `test(captura): regla del permiso de cámara` |
| PR-12 | 🆕 `feature/captura/presentation/ExplicacionPermisoCamara.kt` (5.1; vistas previas de cada vista) | 1. `feat(captura): explicación del permiso de cámara` · 2. `feat(captura): vistas previas de la explicación del permiso` |
| PR-13 | 🆕 `feature/captura/presentation/ConPermisoCamara.kt` (5.1) | 1. `feat(captura): pedir el permiso de cámara al usarla` · 2. `feat(captura): volver a revisar el permiso al regresar de los ajustes` |
| PR-14 | 🆕 `feature/captura/presentation/VistaCamara.kt` (P-2) | 1. `feat(captura): vista de cámara reutilizable con CameraX` · 2. `feat(captura): aviso cuando la cámara no se puede abrir` |
| PR-15 | 🆕 `feature/captura/presentation/LectorTextoEtiqueta.kt` (P-3) | 1. `feat(captura): lector de texto de la etiqueta con ML Kit` |
| PR-16 | ✏️ `EscanearEtiquetaScreen.kt`: la Route arma `ConPermisoCamara { VistaCamara(...) }` con el lector. | 1. `feat(captura): «Escanear etiqueta» con el permiso y la cámara` |
| PR-17 | ✏️ `feature/captura/presentation/CapturaNavigation.kt`: reemplaza la `PantallaEnConstruccion`, `CLAVE_CODIGO_ESCANEADO` y la devolución (5.3). | 1. `feat(captura): clave del código escaneado` · 2. `feat(captura): «Escanear etiqueta» en la navegación` |

### Registro (PR-18 a PR-20)
| PR | Archivos | Commits |
|---|---|---|
| PR-18 | ✏️ `feature/incidencias/presentation/RegistroIncidenciaViewModel.kt`; ✏️ (test) `RegistroIncidenciaViewModelTest.kt` | 1. `feat(incidencias): el registro recuerda si el código vino de la cámara` · 2. `test(incidencias): código escaneado y su indicación en el registro` |
| PR-19 | ✏️ `RegistroIncidenciaScreen.kt`: `supportingText` según el estado y vista previa. | 1. `feat(incidencias): «Leído desde la etiqueta con la cámara» bajo el código` |
| PR-20 | ✏️ `IncidenciasNavigation.kt` y la Route de `RegistroIncidenciaScreen.kt` (5.3). | 1. `feat(incidencias): el registro recibe el código al volver del escaneo` |

### Carpeta y copia de seguridad (PR-21 a PR-23)
| PR | Archivos | Commits |
|---|---|---|
| PR-21 | 🆕🔶 `core/data/AlmacenFotos.kt` (P-4); 🆕 (test) `core/data/AlmacenFotosTest.kt` | 1. `feat(core): carpeta interna de las fotos de evidencia` · 2. `test(core): crear y borrar fotos en una carpeta temporal` |
| PR-22 | ✏️🔶 `res/xml/backup_rules.xml` | 1. `feat(core): fotos fuera de la copia de seguridad en Android 11 o anterior` |
| PR-23 | ✏️🔶 `res/xml/data_extraction_rules.xml` | 1. `feat(core): fotos fuera de la copia y la transferencia en Android 12 o superior` |

### Dominio de las fotos (PR-24 a PR-31)
| PR | Archivos | Commits |
|---|---|---|
| PR-24 | ✏️⚠️ `core/domain/Resultado.kt`; ✏️⚠️ `core/ui/MensajesError.kt` (D-2) | 1. `feat(core): error LimiteFotosAlcanzado` · 2. `feat(core): mensaje del máximo de fotos` |
| PR-25 | 🆕 `feature/captura/domain/ReglasEvidencia.kt` (3.6); 🆕 (test) `ReglasEvidenciaTest.kt` | 1. `feat(captura): regla de quién agrega fotos y hasta cuántas` · 2. `test(captura): regla por permiso, técnico, marca y máximo` |
| PR-26 | 🆕 `feature/captura/domain/EvidenciaRepository.kt`; 🆕 (test) `feature/captura/data/EvidenciaRepositoryFalso.kt` | 1. `feat(captura): contrato del repositorio de evidencias` · 2. `test(captura): repositorio de evidencias falso` |
| PR-27 | 🆕 `feature/captura/data/EvidenciaRepositoryImpl.kt` (4.7); ✏️ `CapturaModule.kt` | 1. `feat(captura): repositorio de evidencias en Room y en la carpeta interna` · 2. `feat(captura): @Binds del repositorio de evidencias` |
| PR-28 | 🆕 `feature/captura/domain/PrepararFoto.kt` (4.3); 🆕 (test) `PrepararFotoTest.kt` | 1. `feat(captura): PrepararFoto con su id y su ruta` · 2. `test(captura): PrepararFoto da ids distintos dentro de la incidencia` |
| PR-29 | 🆕 `feature/captura/domain/GuardarEvidencia.kt` (4.4); 🆕 (test) `GuardarEvidenciaTest.kt` | 1. `feat(captura): GuardarEvidencia verifica antes de enlazar la foto` · 2. `test(captura): GuardarEvidencia sin permiso, cerrada, al máximo, correcta y con fallo` |
| PR-30 | 🆕 `feature/captura/domain/DescartarFoto.kt` (4.5); 🆕 (test) `DescartarFotoTest.kt` | 1. `feat(captura): DescartarFoto borra la foto que no se enlazó` · 2. `test(captura): DescartarFoto` |
| PR-31 | 🆕 `feature/captura/domain/ObservarCapturaEvidencia.kt` (4.6); 🆕 (test) `ObservarCapturaEvidenciaTest.kt` | 1. `feat(captura): ObservarCapturaEvidencia junta la incidencia, sus fotos y el bloqueo` · 2. `test(captura): ObservarCapturaEvidencia con y sin sesión, reasignada y al máximo` |

### Captura de evidencia (PR-32 a PR-37)
| PR | Archivos | Commits |
|---|---|---|
| PR-32 | 🆕 `feature/captura/presentation/CapturaEvidenciaViewModel.kt` (4.8); 🆕 (test) `CapturaEvidenciaViewModelTest.kt` | 1. `feat(captura): CapturaEvidenciaViewModel con el tipo y el disparo` · 2. `test(captura): CapturaEvidenciaViewModel dispara, guarda, falla y respeta el máximo` |
| PR-33 | 🆕 `feature/captura/presentation/MiniaturaEvidencia.kt` (5.6, P-6) | 1. `feat(captura): decodificar una foto reducida con su orientación` · 2. `feat(captura): miniatura de una foto con «Foto no disponible»` |
| PR-34 | 🆕 `feature/captura/presentation/EvidenciaFotograficaScreen.kt` (5.4; vistas previas) | 1. `feat(captura): pantalla «Evidencia fotográfica» con su selector` · 2. `feat(captura): miniaturas, disparador y «Listo» de la evidencia` |
| PR-35 | 🆕 `feature/captura/presentation/TomarFoto.kt` (P-5) | 1. `feat(captura): tomar la foto reducida y comprimida con CameraX` |
| PR-36 | ✏️ `EvidenciaFotograficaScreen.kt`: la Route arma el permiso y la cámara con `ImageCapture`. | 1. `feat(captura): «Evidencia fotográfica» con el permiso y la cámara` |
| PR-37 | ✏️ `CapturaNavigation.kt`: reemplaza la `PantallaEnConstruccion` de `CapturarEvidencia`. | 1. `feat(captura): «Evidencia fotográfica» en la navegación` |

### Detalle (PR-38 a PR-42)
| PR | Archivos | Commits |
|---|---|---|
| PR-38 | ✏️ `feature/incidencias/domain/IncidenciaRepository.kt`, ✏️ `data/IncidenciaRepositoryImpl.kt` (inyecta `EvidenciaDao` solo para leer); ✏️ (test) `IncidenciaRepositoryFalso.kt` | 1. `feat(incidencias): el repositorio lee las fotos de la incidencia` · 2. `test(incidencias): fotos en el repositorio falso de incidencias` |
| PR-39 | ✏️ `feature/incidencias/domain/ObservarDetalleIncidencia.kt`; ✏️ (test) `ObservarDetalleIncidenciaTest.kt` | 1. `feat(incidencias): el detalle observa también las fotos` · 2. `test(incidencias): el detalle con fotos, sin fotos y en orden` |
| PR-40 | 🆕 `feature/captura/presentation/SeccionEvidencias.kt` (5.5; vistas previas) | 1. `feat(captura): sección «Evidencias» reutilizable` |
| PR-41 | 🆕 `feature/captura/presentation/FotoAmpliada.kt` (5.5) | 1. `feat(captura): foto ampliada con su tipo y su fecha` |
| PR-42 | ✏️ `DetalleIncidenciaScreen.kt` (P-7); ✏️ (test) `DetalleIncidenciaViewModelTest.kt` | 1. `feat(incidencias): fotos en el detalle y «Agregar foto» solo para el técnico asignado` · 2. `feat(incidencias): ampliar una foto desde el detalle` · 3. `test(incidencias): las fotos llegan al estado del detalle` |

### Eliminar una foto, D-5 (PR-43 a PR-46)
| PR | Archivos | Commits |
|---|---|---|
| PR-43 | ✏️ `EvidenciaRepository.kt`, ✏️ `EvidenciaRepositoryImpl.kt`, ✏️ (test) `EvidenciaRepositoryFalso.kt` | 1. `feat(captura): el repositorio elimina la foto y su archivo` · 2. `test(captura): eliminar en el repositorio falso` |
| PR-44 | 🆕 `feature/captura/domain/EliminarEvidencia.kt` (4.9); 🆕 (test) `EliminarEvidenciaTest.kt` | 1. `feat(captura): EliminarEvidencia solo para el técnico asignado y pendiente` · 2. `test(captura): EliminarEvidencia sin permiso, cerrada, correcta y con fallo` |
| PR-45 | ✏️ `FotoAmpliada.kt`: `BotonPeligro` y su confirmación. | 1. `feat(captura): «Eliminar foto» con confirmación en la foto ampliada` |
| PR-46 | ✏️ `DetalleIncidenciaViewModel.kt`, ✏️ `DetalleIncidenciaScreen.kt`; ✏️ (test) `DetalleIncidenciaViewModelTest.kt` | 1. `feat(incidencias): eliminar una foto desde el detalle` · 2. `test(incidencias): eliminar una foto desde el detalle` |

### Informe técnico, D-6 (PR-47 a PR-49)
| PR | Archivos | Commits |
|---|---|---|
| PR-47 | ✏️🔶 `feature/atencion/presentation/InformeTecnicoViewModel.kt`; ✏️🔶 (test) `InformeTecnicoViewModelTest.kt` | 1. `feat(atencion): el informe observa las fotos de la incidencia` · 2. `test(atencion): fotos en el estado del informe sin perder lo escrito` |
| PR-48 | ✏️🔶 `InformeTecnicoScreen.kt` | 1. `feat(atencion): sección «Evidencias» en el informe técnico` |
| PR-49 | ✏️🔶 la Route de `InformeTecnicoScreen.kt` y ✏️🔶 `AtencionNavigation.kt` | 1. `feat(atencion): «Agregar foto» desde el informe técnico` |

### Borrado con los datos locales, D-7 (PR-50 y PR-51)
| PR | Archivos | Commits |
|---|---|---|
| PR-50 | ✏️🔶 `core/data/SesionRepositoryFirebase.kt` (4.10); ✏️ `specs/001-inicio-sesion/spec.md` (RF-10) | 1. `feat(core): borrar las fotos al cerrar sesión` · 2. `docs(acceso): RF-10 incluye las fotos de evidencia` |
| PR-51 | ✏️🔶 `core/sync/SincronizacionTiempoReal.kt` (4.10); ✏️ `specs/001-inicio-sesion/spec.md` (RF-11); ✏️ `MEMORY.md` | 1. `feat(core): borrar las fotos al entrar otra persona` · 2. `docs(acceso): RF-11 incluye las fotos de evidencia` · 3. `docs(core): pendiente de las fotos resuelto en MEMORY` |

### Avisos de las fotos al salir y al entrar otra persona, D-8 y D-13 (PR-52 a PR-56b)
| PR | Archivos | Commits |
|---|---|---|
| PR-52 | ✏️🔶 `core/database/dao/EvidenciaDao.kt` (`contar(): Flow<Int>`); 🆕 (androidTest) `EvidenciaDaoTest.kt` | 1. `feat(core): EvidenciaDao cuenta las fotos del celular` · 2. `test(core): EvidenciaDao cuenta y ordena las fotos` |
| PR-53 | ✏️⚠️ `core/domain/model/EstadoDatos.kt` (`fotosEnCelular: Int = 0`); ✏️🔶 `core/sync/EstadoDatosRepositoryImpl.kt` | 1. `feat(core): el estado de los datos incluye las fotos del celular` · 2. `feat(core): contar las fotos al observar el estado de los datos` |
| PR-54 | ✏️🔶 `core/domain/ReglasSesion.kt` (`evaluarBorrado`, `DecisionBorrado.Confirmar` con fotos, 4.11); ✏️🔶 `core/domain/usecase/EvaluarCierreSesion.kt`; ✏️ (test) `ReglasSesionTest.kt`, `EvaluarCierreSesionTest.kt` | 1. `feat(core): la regla de borrado cuenta las fotos` · 2. `test(core): confirmar con fotos y sin cambios sin enviar` · 3. `feat(core): EvaluarCierreSesion pasa las fotos del celular` · 4. `test(core): EvaluarCierreSesion con fotos` |
| PR-55 | ✏️🔶 `feature/acceso/presentation/TextosSalida.kt` (`textoFotosEvidencia`, `textoAvisoSalida`, 4.11); ✏️ (test) `TextosSalidaTest.kt` | 1. `feat(acceso): texto de las fotos de evidencia en singular y plural` · 2. `feat(acceso): texto del aviso de salida con cambios y fotos` · 3. `test(acceso): textos del aviso con cambios, con fotos y con ambos` |
| PR-56 | ✏️🔶 `AjustesSeguridadViewModel.kt` y `AjustesSeguridadScreen.kt`; ✏️ `specs/001-inicio-sesion/spec.md` (RF-13) | 1. `feat(acceso): el aviso de cierre de sesión cuenta las fotos` · 2. `docs(acceso): RF-13 cuenta las fotos de evidencia` |
| PR-56b | ✏️🔶 `core/domain/usecase/EvaluarIngresoDeOtraPersona.kt`; ✏️🔶 `TextosSalida.kt` (`textoAvisoIngreso`); ✏️🔶 `LoginViewModel.kt` y `LoginScreen.kt`; ✏️ (test) `EvaluarIngresoDeOtraPersonaTest.kt`, `TextosSalidaTest.kt`; ✏️ `specs/001-inicio-sesion/spec.md` (RF-11) | 1. `feat(core): EvaluarIngresoDeOtraPersona pasa las fotos del celular` · 2. `test(core): ingreso de otra persona con fotos` · 3. `feat(acceso): texto del aviso de ingreso con cambios y fotos` · 4. `test(acceso): textos del aviso de ingreso` · 5. `feat(acceso): el aviso al entrar otra persona cuenta las fotos` · 6. `docs(acceso): RF-11 cuenta las fotos de evidencia` |

### Cierre (PR-57)
| PR | Archivos | Commits |
|---|---|---|
| PR-57 | ✏️ `specs/009-escaneo-evidencias/spec.md` («completa»), ✏️ `tasks.md` (marcadas), ✏️ `MEMORY.md` (línea de la HU-09) | 1. `docs(captura): pruebas manuales de la HU-09 en el celular` · 2. `docs(captura): spec de la HU-09 completa` · 3. `docs(captura): HU-09 completa en MEMORY` |

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

### 4.9 `EliminarEvidencia(evidencia)` (RF-937, D-5)
```text
usuario = verificarPermiso(ATENDER_INCIDENCIA); si es error, devolverlo
incidencia = repo.incidencia(evidencia.incidenciaId)
bloqueoParaFotos(usuario, incidencia, cantidad = null)?.let { devolver Error(it) }
devolver repo.eliminar(evidencia)                          // primero la fila, después el archivo
```

### 4.10 Borrado de las fotos con los datos locales (RF-942, D-7)
```text
clearAllTables()           // primero Room: ninguna fila queda apuntando a un archivo ya borrado
almacen.borrarTodo()       // si falla, se registra con Log.w y no impide cerrar sesión ni entrar;
                           // los archivos sin fila no se ven y se borran en el siguiente borrado
```

### 4.11 Aviso de las fotos al cerrar sesión (RF-943, D-8)
```text
evaluarBorrado(pendientes, fotos = 0):                       // ReglasSesion.kt
    si pendientes > 0 o fotos > 0: Confirmar(pendientes, fotos) si no: SinRiesgo
EvaluarCierreSesion():
    estado = observarEstadoDatos().first()
    evaluarBorrado(estado.pendientesPorEnviar + estado.necesitanAtencion, estado.fotosEnCelular)
textoAvisoSalida(pendientes, fotos):                         // TextosSalida.kt
    con los dos: "Tienes ${textoCambiosSinEnviar(p)} y ${textoFotosEvidencia(f)} en este celular. Si cierras sesión ahora, se perderán."
    solo fotos:  "Tienes ${textoFotosEvidencia(f)} en este celular. Si cierras sesión ahora, ${textoSePerdera(f)}."
    solo cambios: el texto de RF-13, sin cambios
textoFotosEvidencia(f): "1 foto de evidencia" o "$f fotos de evidencia"
```
`fotos` tiene valor por defecto para que `EvaluarIngresoDeOtraPersona` siga compilando hasta PR-56b, que le pasa
`estado.fotosEnCelular` y usa `textoAvisoIngreso(nombre, pendientes, fotos)` con la misma forma que
`textoAvisoSalida` («{Nombre} tiene … en este celular. Si ingresas, se perderán.») (RF-944, D-13).

## 5. Cómo se pinta en Compose

Todas las pantallas nuevas usan `Scaffold(contentWindowInsets = WindowInsets(0))` con `BarraSuperior(titulo,
alVolver)`, solo roles de `MaterialTheme.colorScheme` y estilos de `MaterialTheme.typography`, `Espaciado.*` y
los botones de `Botones.kt`. Las `Screen` reciben estado y lambdas, y la vista de la cámara llega como una ranura
`vistaCamara: @Composable () -> Unit`: así las vistas previas muestran un recuadro `surfaceVariant` con «Vista de la
cámara», como el prototipo, sin abrir ninguna cámara.

### 5.1 `ConPermisoCamara(textoNegado, contenido)` (PR-11 a PR-13)
- `concedido` sale de `ContextCompat.checkSelfPermission` y se vuelve a leer en cada `ON_RESUME`
  (`LifecycleEventEffect`), para enterarse al volver de los ajustes (RF-919) o si lo quitaron desde ahí (RF-922).
- `yaSePidio` es un `rememberSaveable`; `androidLoExplicaria` sale de `shouldShowRequestPermissionRationale` de la
  `Activity` (`MainActivity` ya es una `AppCompatActivity`); `tieneCamara`, de
  `hasSystemFeature(FEATURE_CAMERA_ANY)`.
- `rememberLauncherForActivityResult(RequestPermission())` pide el permiso; «Abrir ajustes» lanza
  `ACTION_APPLICATION_DETAILS_SETTINGS` con el paquete de la app.
- `when (vistaPermiso(...))`: `CAMARA` dibuja `contenido()`; las demás dibujan `ExplicacionPermisoCamara`: ícono,
  título, el texto de RF-916 o de RF-918 a RF-920 y `BotonPrincipal` «Permitir cámara» o «Abrir ajustes».

### 5.2 «Escanear etiqueta» (PR-09 y PR-16)
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
- En PR-16, la Route arma `ConPermisoCamara(textoNegado = "También puedes escribir el código a mano en el formulario.")
  { VistaCamara(casos = [Preview, ImageAnalysis con LectorTextoEtiqueta]) }` y solo pasa textos al `ViewModel`
  mientras `reconocido == null`.

### 5.3 Devolver el código por `savedStateHandle` (PR-17 y PR-20, P-8)
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

### 5.4 «Evidencia fotográfica» (PR-34 y PR-36)
- `BarraSuperior("Evidencia fotográfica", alVolver)` y, debajo, «INC-0124 · PC-CONT-014» (`bodyMedium`,
  `onSurfaceVariant`), porque `BarraSuperior` no tiene subtítulo.
- `SingleChoiceSegmentedButtonRow` con «Falla» y «Pieza cambiada».
- La ranura de la cámara, recortada con la forma del tema, con la pastilla «Se guardará como: foto de la falla».
- Una fila desplazable de `MiniaturaEvidencia` de 64 dp, cada una con su rótulo («Falla 1», «Pieza 1»).
- Abajo: «N fotos» (`bodyMedium`), el disparador y `BotonPrincipal` «Listo». El disparador es un círculo de 72 dp
  con borde `primary` y `contentDescription = "Tomar foto"`, deshabilitado mientras guarda o con bloqueo (P-11).
- Sobre esa fila, el mensaje del bloqueo (RF-929, RF-931) o del error (`error.mensaje()`).
- En PR-36, la Route arma `ConPermisoCamara { VistaCamara(casos = [Preview, ImageCapture]) }`. Al disparar:
  `vm.alDisparar()?.let { foto -> lanzar tomarFoto(imageCapture, foto.ruta) y avisar alFotoTomada(foto) o
  alFalloCaptura(foto) }`.

### 5.5 `SeccionEvidencias` y `FotoAmpliada` (PR-40, PR-41, PR-45 y PR-48)
- `Seccion("Evidencias")` con un `FlowRow` de miniaturas cuadradas de 96 dp (borde punteado `outlineVariant` mientras
  carga, como el prototipo), cada una con su rótulo y tocable; si `puedeAgregar`, al final va el
  `BotonSecundario` «Agregar foto» que ya existe; debajo, la nota de RF-933 (`bodySmall`, `onSurfaceVariant`).
- `FotoAmpliada` es un `Dialog` a pantalla completa (`usePlatformDefaultWidth = false`) con `BarraSuperior(tipo,
  alVolver)`, la foto ajustada al ancho y la fecha y hora de captura. En PR-45 suma `BotonPeligro` «Eliminar foto» y
  un `AlertDialog` de confirmación.

### 5.6 `MiniaturaEvidencia(ruta, ladoPx)` (PR-33, P-6)
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
| P-13 | PR-01 quita el filtro `branches: [main]` del disparador `pull_request` del CI. | Con el filtro, un PR apilado (base = otra rama) no corre el CI hasta que su base se fusiona, y no se podría marcar «listo» con el check en verde. El check sigue llamándose `compilar-y-probar`. | Esperar a que cada base se fusione para ver el CI: frena toda la cadena. |
| P-14 | Un PR que necesita más de un PR abierto sale de la rama de uno («Base») y trae los otros con `git merge` («Trae»). | GitHub admite una sola base; así compila y pasa el CI solo, y su diff se reduce a su cambio en cuanto los otros se fusionan. | Apilar todo en una sola cadena: cada arreglo pedido en un PR temprano obligaría a propagar merges por decenas de ramas. |

## 7. Estrategia de pruebas

`./gradlew testDebugUnitTest` con JUnit 4, `kotlinx-coroutines-test`, `MainDispatcherRule` y dobles propios
(`SesionRepositoryDemo`, `IncidenciaRepositoryFalso`, y los nuevos `CatalogoEquiposRepositoryFalso` y
`EvidenciaRepositoryFalso`). No se agregan librerías. Las pruebas de `captura` van en
`feature/captura/{domain,data,presentation}`, con el mismo paquete que la clase real.

| Prueba (PR) | Casos | RF |
|---|---|---|
| `CodigoEquipoTest` (PR-02 a PR-04) | Mayúsculas, guiones raros y espacios alrededor del guion; `PC-C0NT-O14` → `PC-CONT-014`; el texto del prototipo 13 («SOFT CORPORATION · ACTIVO FIJO», «Área: Contabilidad») da solo `PC-CONT-014`; una fecha (`2026-10-15`) y un anexo (`Anexo 2314`) no son candidatos; dos códigos salen sin repetir y en orden; `elegirCodigo` prefiere el del catálogo, toma el primero si ninguno está y devuelve `null` sin candidatos. | RF-903, RF-904, RF-907 |
| `RachaLecturaTest` (PR-05) | Tres iguales seguidas dan `estable`; dos iguales y una distinta, no; una lectura sin código reinicia; una racha nueva empieza vacía. | RF-905, RF-908 |
| `LeerCodigoEquipoTest` (PR-07) | Código del catálogo → `enCatalogo = true`; fuera del catálogo → `false`; dos códigos con uno en el catálogo → ese; texto sin código → `null`. | RF-904, RF-907 |
| `PermisoCamaraTest` (PR-11) | La tabla de 3.5: sin cámara; concedido; sin pedir; negado con explicación; negado sin explicación después de pedirlo; quitado desde ajustes. | RF-916 a RF-920, RF-922 |
| `EscaneoViewModelTest` (PR-08) | Tres textos con el mismo código → `reconocido`; alternancia → nada; con un código reconocido, los textos nuevos se ignoran; `alVolverAEscanear` limpia todo; un código fuera del catálogo llega con `enCatalogo = false`. | RF-905 a RF-908 |
| `RegistroIncidenciaViewModelTest` (PR-18, se amplía) | El código escaneado reemplaza el escrito y enciende `codigoLeidoConCamara`; editarlo a mano lo apaga; recibir un código limpia un error anterior. | RF-910, RF-911 |
| `AlmacenFotosTest` (PR-21) | `archivoNuevo` crea la subcarpeta de la incidencia dentro de la raíz; `borrar` borra ese archivo y no falla si ya no existe; `borrarTodo` vacía la carpeta de fotos y no toca nada fuera de ella. | RF-928, RF-940, RF-942 |
| `ReglasEvidenciaTest` (PR-25) | Sin usuario; jefe (sin `ATENDER_INCIDENCIA`); otro técnico; incidencia inexistente; cerrada; con 10 fotos; con 9 fotos pasa; con `cantidad = null` (eliminar) pasa aunque tenga 10. | RF-923, RF-926, RF-929, RF-931, RF-937 |
| `GuardarEvidenciaTest` (PR-29) | Sin sesión → `SinPermiso` y el archivo descartado; otro técnico → `SinPermiso` y descartado; cerrada → `IncidenciaCerrada` y descartado; al máximo → `LimiteFotosAlcanzado` y descartado; correcto → la evidencia con su tipo, su ruta y `fechaCaptura == ahora`; el repositorio falla → el error y nada guardado. | RF-925, RF-926, RF-928, RF-929 |
| `ObservarCapturaEvidenciaTest` (PR-31) | Técnico asignado sin bloqueo; sin sesión → `SinPermiso`; al llegar a 10 → `LimiteFotosAlcanzado`; al reasignarse en el repositorio falso → `SinPermiso`; incidencia inexistente → `null`. | RF-924, RF-929, RF-931 |
| `CapturaEvidenciaViewModelTest` (PR-32) | Empieza en «Falla» y cambia; el tipo sale del `SavedStateHandle` (rotación); `alDisparar` devuelve una `FotoNueva` y deshabilita el disparador; un segundo `alDisparar` mientras guarda devuelve `null`; `alFotoTomada` guarda con el tipo elegido y vuelve a habilitar; `alFalloCaptura` descarta el archivo y muestra el error; con bloqueo, `alDisparar` devuelve `null`. | RF-924 a RF-931 |
| `ObservarDetalleIncidenciaTest` y `DetalleIncidenciaViewModelTest` (PR-39 y PR-42, se amplían) | Las fotos llegan al detalle en orden; sin fotos, lista vacía; `puedeAtender` (que muestra «Agregar foto») solo para el técnico asignado mientras siga pendiente. | RF-923, RF-932, RF-936 |
| `EliminarEvidenciaTest` y `DetalleIncidenciaViewModelTest` (PR-44 y PR-46) | Sin permiso; otro técnico; cerrada → `IncidenciaCerrada`; correcto; fallo del repositorio; el error llega al estado del detalle. | RF-937 |
| `InformeTecnicoViewModelTest` (PR-47, se amplía) | Las fotos llegan al estado del informe; lo escrito en el formulario se conserva. | RF-938 |
| `PrepararFotoTest` (PR-28) | Ids distintos en dos llamadas; la ruta es la que da el repositorio para esa incidencia. | RF-925 |
| `DescartarFotoTest` (PR-30) | La ruta queda en la lista de descartadas del repositorio falso. | RF-928 |
| `EvidenciaDaoTest` (PR-52, instrumentada) | `contar` sube al guardar y baja al eliminar; `observarPorIncidencia` ordena por fecha de captura. | RF-932, RF-943 |
| `ReglasSesionTest` y `EvaluarCierreSesionTest` (PR-54, se amplían) | Solo fotos → `Confirmar`; cambios y fotos → `Confirmar` con los dos números; nada → `SinRiesgo`. | RF-943 |
| `TextosSalidaTest` (PR-55, se amplía) | 1 y varias fotos; solo cambios (texto de RF-13 sin cambios); solo fotos; cambios y fotos. | RF-943 |
| `EvaluarIngresoDeOtraPersonaTest` y `TextosSalidaTest` (PR-56b, se amplían) | Otra persona con solo fotos → `Confirmar`; misma persona con fotos → `SinRiesgo`; textos de ingreso con cambios, con fotos y con ambos. | RF-944 |

**Prueba instrumentada:** solo `EvidenciaDaoTest` (PR-52), porque es el único DAO que cambia; corre en un celular o
emulador, no en el CI.

**Sin prueba JVM, igual que en las HU anteriores:** `CatalogoEquiposRepositoryImpl` y `EvidenciaRepositoryImpl`
(dependen de Room, como los demás `*RepositoryImpl`), `VistaCamara`, `LectorTextoEtiqueta`, `TomarFoto`,
`ConPermisoCamara` y `MiniaturaEvidencia` (dependen de la cámara, de ML Kit o de Android), y el borrado en
`SesionRepositoryFirebase`/`SincronizacionTiempoReal` (Firebase; `borrarTodo()` sí se prueba en G).

**Verificado a mano en un celular real** (el emulador simula la cámara), con capturas en claro y oscuro:
- PR-16 y PR-17: el permiso (explicación, negar, negar otra vez, «Abrir ajustes»), la lectura de las 4 etiquetas de prueba en
  modo avión, la velocidad (menos de 2 segundos) y que la cámara se suelta al salir.
- PR-20: el código llega a «Nueva incidencia» con su indicación; editarlo la quita; salir sin confirmar no cambia nada.
- PR-36 y PR-37: fotos de los dos tipos, el máximo de 10, el doble toque, y el disparador deshabilitado si la incidencia se
  cierra desde otro celular.
- PR-42, PR-46 y PR-49: las fotos en el detalle y en el informe, ampliadas y eliminadas, y otro técnico sin
  «Agregar foto»; como jefe, en otro celular, la misma incidencia sin fotos.
- PR-50 y PR-51: después de cerrar sesión, y después de entrar otra persona, la carpeta `files/evidencias` vacía
  (`adb shell run-as com.softcorp.sigtec ls files/evidencias`).
- PR-56: cerrar sesión con fotos y sin cambios sin enviar, y con los dos, y ver el texto de RF-943 en claro y oscuro.
- PR-56b: con el dueño bloqueado y fotos sin cambios, entrar con otra cuenta y ver el aviso de RF-944.

**Cierre de cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de
lint antes de abrirlo o de marcarlo listo.

## 8. Cobertura de RF

| RF | Dónde vive | Prueba |
|---|---|---|
| RF-901 | `EscanearEtiquetaScreen`, `VistaCamara`; el botón de cámara del registro ya existe | Vistas previas + manual (PR-16) |
| RF-902 | `LectorTextoEtiqueta` + `LeerCodigoEquipo` | `LeerCodigoEquipoTest` + manual en modo avión |
| RF-903 | `normalizarTexto`, `candidatosCodigo` | `CodigoEquipoTest` |
| RF-904 | `elegirCodigo`, `LeerCodigoEquipo` | `CodigoEquipoTest`, `LeerCodigoEquipoTest` |
| RF-905 | `RachaLectura`, `EscaneoViewModel` | `RachaLecturaTest`, `EscaneoViewModelTest` |
| RF-906 | `EscaneoViewModel` (deja de procesar), panel de `EscanearEtiquetaScreen` | `EscaneoViewModelTest` + vistas previas |
| RF-907 | `elegirCodigo` (`enCatalogo`), panel con el aviso | `CodigoEquipoTest`, `LeerCodigoEquipoTest`, `EscaneoViewModelTest` |
| RF-908 | `EscaneoViewModel.alVolverAEscanear` | `EscaneoViewModelTest` |
| RF-909 | `CapturaNavigation` (`previousBackStackEntry?.savedStateHandle`) | Manual (PR-17, PR-20) |
| RF-910 | `RegistroIncidenciaViewModel`, `RegistroIncidenciaScreen`, `IncidenciasNavigation` | `RegistroIncidenciaViewModelTest` + manual |
| RF-911 | `RegistroIncidenciaViewModel.alCambiarCodigo` | `RegistroIncidenciaViewModelTest` |
| RF-912 | `CapturaNavigation` («Volver» sin dejar nada) | Manual |
| RF-913 | `CLAVE_CODIGO_ESCANEADO`, única forma de devolver el código | Manual con el registro; la HU-16 la reutiliza |
| RF-914 | `LectorTextoEtiqueta` (no escribe nada) | Revisión de PR-15 |
| RF-915 | `VistaCamara` (enlazada al ciclo de vida) | Manual |
| RF-916 a RF-920, RF-922 | `vistaPermiso`, `ConPermisoCamara`, `ExplicacionPermisoCamara`, manifest | `PermisoCamaraTest` + vistas previas + manual |
| RF-921 | `VistaCamara` (error al abrir) | Manual, con otra app usando la cámara |
| RF-923 | `DetalleIncidenciaScreen` (`puedeAtender`), `bloqueoParaFotos` | `ReglasEvidenciaTest`, `DetalleIncidenciaViewModelTest` |
| RF-924 | `EvidenciaFotograficaScreen`, `ObservarCapturaEvidencia` | `ObservarCapturaEvidenciaTest`, `CapturaEvidenciaViewModelTest` |
| RF-925 | `PrepararFoto`, `TomarFoto`, `GuardarEvidencia`, `AlmacenFotos` | `GuardarEvidenciaTest`, `AlmacenFotosTest` + manual |
| RF-926 | `GuardarEvidencia` con `bloqueoParaFotos` | `GuardarEvidenciaTest`, `ReglasEvidenciaTest` |
| RF-927 | `CapturaEvidenciaViewModel.alDisparar` | `CapturaEvidenciaViewModelTest` + manual |
| RF-928 | `GuardarEvidencia`, `DescartarFoto`, `EvidenciaRepositoryImpl.guardar` | `GuardarEvidenciaTest`, `CapturaEvidenciaViewModelTest` + manual |
| RF-929 | `bloqueoParaFotos`, `LimiteFotosAlcanzado` | `ReglasEvidenciaTest`, `GuardarEvidenciaTest`, `ObservarCapturaEvidenciaTest` |
| RF-930 | `CapturaNavigation`; cada foto se guarda al tomarla (P-12) | Manual |
| RF-931 | `ObservarCapturaEvidencia` (reactivo) y `GuardarEvidencia` (P-10) | `ObservarCapturaEvidenciaTest` + manual con dos celulares |
| RF-932, RF-936 | `ObservarDetalleIncidencia`, `SeccionEvidencias` | `ObservarDetalleIncidenciaTest`, `DetalleIncidenciaViewModelTest` |
| RF-933 | `SeccionEvidencias` | Vistas previas + manual |
| RF-934 | `FotoAmpliada` | Vistas previas + manual |
| RF-935 | `MiniaturaEvidencia` | Manual, borrando un archivo con `adb shell run-as` |
| RF-937 | `EliminarEvidencia`, `FotoAmpliada`, `DetalleIncidenciaViewModel` (D-5) | `EliminarEvidenciaTest`, `DetalleIncidenciaViewModelTest` |
| RF-938 | `InformeTecnicoViewModel`, `InformeTecnicoScreen` (D-6) | `InformeTecnicoViewModelTest` |
| RF-939 | `EvidenciaEntity` sin campos de sincronización; `core/sync` no la conoce | Revisión + manual con otro celular |
| RF-940 | `AlmacenFotos` (`filesDir`) | `AlmacenFotosTest` |
| RF-941 | `backup_rules.xml`, `data_extraction_rules.xml` | Revisión de PR-22 y PR-23 |
| RF-942 | `SesionRepositoryFirebase`, `SincronizacionTiempoReal`, `AlmacenFotos.borrarTodo` (D-7) | `AlmacenFotosTest` + manual |
| RF-943 | `EvidenciaDao.contar`, `EstadoDatos.fotosEnCelular`, `evaluarBorrado`, `textoAvisoSalida`, `AjustesSeguridadScreen` (D-8) | `EvidenciaDaoTest`, `ReglasSesionTest`, `EvaluarCierreSesionTest`, `TextosSalidaTest` + manual |
| RF-944 | `EvaluarIngresoDeOtraPersona`, `textoAvisoIngreso`, `LoginScreen` (D-13) | `EvaluarIngresoDeOtraPersonaTest`, `TextosSalidaTest` + manual |

## 9. Respeto a la constitución
1. **Stack cerrado:** sin dependencias nuevas. CameraX, ML Kit y `kotlinx-coroutines-play-services` ya estaban en
   `libs.versions.toml`; P-3 descarta `camera-mlkit-vision` y P-6 descarta Coil por la misma razón.
2. **Spec antes que código:** la spec se aprobó antes del primer PR de código. PR-50, PR-51, PR-56 y PR-56b actualizan
   RF-10, RF-11 y RF-13 de la spec 001 en el mismo PR que cambia ese comportamiento.
3. **Lógica fuera de la pantalla:** las reglas viven en `feature/captura/domain`, en Kotlin puro (`CodigoEquipo`,
   `RachaLectura`, `ReglasEvidencia` y los casos de uso). `vistaPermiso` es una función pura. Las `Screen` solo
   dibujan; `ConPermisoCamara`, `VistaCamara`, `LectorTextoEtiqueta` y `TomarFoto` son el pegamento con Android y
   no deciden reglas.
4. **Toda regla tiene prueba:** sección 7. Lo que no se puede probar en la JVM (cámara, ML Kit, Firebase) se prueba
   a mano en un celular real y queda escrito en cada PR.
5. **Datos del usuario protegidos:** el escaneo no guarda imágenes ni texto (RF-914); las fotos quedan en el
   almacenamiento interno, fuera de la copia de seguridad, y se borran con los datos locales (RF-940 a RF-942).
   La lectura del código es IA en el propio celular (ML Kit): no cambia ningún dato sin que la persona lo confirme
   (RF-906). No se toca la confirmación biométrica de cierres ni de repuestos.
6. **Español en el negocio:** `LeerCodigoEquipo`, `RachaLectura`, `AlmacenFotos`, `VistaCamara`,
   `LectorTextoEtiqueta`; solo los sufijos que impone Android quedan en inglés (`ViewModel`, `Screen`, `Impl`).

## 10. Resumen: archivos compartidos y Room por PR

| PR | Compartidos ⚠️ (lista de `CLAUDE.md`) | Sensibles 🔶 | Room |
|---|---|---|---|
| PR-01 | — | `.github/workflows/ci.yml` (P-13) | — |
| PR-10 | `AndroidManifest.xml` (D-11) | — | — |
| PR-21 a PR-23 | — | `core/data/AlmacenFotos.kt` (nuevo, D-7), `res/xml/backup_rules.xml` y `data_extraction_rules.xml` (D-9) | — |
| PR-24 | `core/domain/Resultado.kt`, `core/ui/MensajesError.kt` (D-2) | — | — |
| PR-47 a PR-49 | — | `feature/atencion/presentation/*` de la HU-08 (D-6) | — |
| PR-50, PR-51 | — | `core/data/SesionRepositoryFirebase.kt`, `core/sync/SincronizacionTiempoReal.kt`, `specs/001-inicio-sesion/spec.md` (D-7) | — |
| PR-52 | — | `core/database/dao/EvidenciaDao.kt` | Consulta nueva; entidad igual, `version` no sube |
| PR-53 | `core/domain/model/EstadoDatos.kt` (D-8) | `core/sync/EstadoDatosRepositoryImpl.kt` | — |
| PR-54 a PR-56b | — | `core/domain/ReglasSesion.kt`, `EvaluarCierreSesion.kt`, `EvaluarIngresoDeOtraPersona.kt`, `feature/acceso/presentation/*` y la spec 001 (modelo de sesión, D-8 y D-13) | — |
| PR-57 | `MEMORY.md` (solo la línea de la HU-09) | — | — |

Los demás PR no tocan archivos compartidos ni sensibles. Ningún PR toca `libs.versions.toml`,
`app/build.gradle.kts`, `SigtecDatabase.kt`, las entidades, `Rutas.kt`, `SigtecRaiz.kt`, `core/theme/*`,
`core/di/*` ni `firebase/firestore.rules`. Los PR de PR-50 a PR-56b tocan el modelo de sesión: Alonso los revisa con
ese foco (`CLAUDE.md`).
