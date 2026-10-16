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
