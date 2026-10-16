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
las tareas) y 57 de código y cierre (PR-01 a PR-57). Si se aprueba D-13 de la spec, se agrega PR-56b.

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
| PR-57 | `docs/hu09-cierre` | `docs(captura): cierre de la HU-09 con las pruebas manuales (HU-09)` | el último fusionado | todos | RF-901 a RF-943 |

**Orden de trabajo:** el de la tabla. Los que salen de `main` pueden revisarse y fusionarse en cualquier orden; los
apilados, después de su base. El flujo completo se ve en el celular en PR-17 y PR-20 (escaneo), PR-37 (fotos),
PR-42 (detalle), PR-49 (informe), PR-51 (borrado) y PR-56 (aviso).

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

### Aviso de las fotos al cerrar sesión, D-8 (PR-52 a PR-56)
| PR | Archivos | Commits |
|---|---|---|
| PR-52 | ✏️🔶 `core/database/dao/EvidenciaDao.kt` (`contar(): Flow<Int>`); 🆕 (androidTest) `EvidenciaDaoTest.kt` | 1. `feat(core): EvidenciaDao cuenta las fotos del celular` · 2. `test(core): EvidenciaDao cuenta y ordena las fotos` |
| PR-53 | ✏️⚠️ `core/domain/model/EstadoDatos.kt` (`fotosEnCelular: Int = 0`); ✏️🔶 `core/sync/EstadoDatosRepositoryImpl.kt` | 1. `feat(core): el estado de los datos incluye las fotos del celular` · 2. `feat(core): contar las fotos al observar el estado de los datos` |
| PR-54 | ✏️🔶 `core/domain/ReglasSesion.kt` (`evaluarBorrado`, `DecisionBorrado.Confirmar` con fotos, 4.11); ✏️🔶 `core/domain/usecase/EvaluarCierreSesion.kt`; ✏️ (test) `ReglasSesionTest.kt`, `EvaluarCierreSesionTest.kt` | 1. `feat(core): la regla de borrado cuenta las fotos` · 2. `test(core): confirmar con fotos y sin cambios sin enviar` · 3. `feat(core): EvaluarCierreSesion pasa las fotos del celular` · 4. `test(core): EvaluarCierreSesion con fotos` |
| PR-55 | ✏️🔶 `feature/acceso/presentation/TextosSalida.kt` (`textoFotosEvidencia`, `textoAvisoSalida`, 4.11); ✏️ (test) `TextosSalidaTest.kt` | 1. `feat(acceso): texto de las fotos de evidencia en singular y plural` · 2. `feat(acceso): texto del aviso de salida con cambios y fotos` · 3. `test(acceso): textos del aviso con cambios, con fotos y con ambos` |
| PR-56 | ✏️🔶 `AjustesSeguridadViewModel.kt` y `AjustesSeguridadScreen.kt`; ✏️ `specs/001-inicio-sesion/spec.md` (RF-13) | 1. `feat(acceso): el aviso de cierre de sesión cuenta las fotos` · 2. `docs(acceso): RF-13 cuenta las fotos de evidencia` |

### Cierre (PR-57)
| PR | Archivos | Commits |
|---|---|---|
| PR-57 | ✏️ `specs/009-escaneo-evidencias/spec.md` («completa»), ✏️ `tasks.md` (marcadas), ✏️ `MEMORY.md` (línea de la HU-09) | 1. `docs(captura): pruebas manuales de la HU-09 en el celular` · 2. `docs(captura): spec de la HU-09 completa` · 3. `docs(captura): HU-09 completa en MEMORY` |
