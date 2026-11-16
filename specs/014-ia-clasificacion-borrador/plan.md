# Plan 014 — Clasificación, prioridad y borrador del informe con IA (HU-14)

> Basado en `spec.md` (aprobada el 2026-11-15, con D-1 a D-11) y `docs/constitution.md`.
> La HU hace nacer el módulo `feature/ia` (`domain`, `data`, `di`), amplía `asignacion` (clasificación), `atencion`
> (borrador del informe), `incidencias` (orden por urgencia y el menú del detalle) y `captura` (un campo dictable más),
> y toca `core` (modelo y tabla de sugerencias, subida, dos componentes) y `firestore.rules`. Como `main` solo admite
> squash, se entrega en muchos PR de un solo cambio (sección 1): cada uno tiene su rama, commits chicos con push
> inmediato y la HU al final del título, y la aprobación de la otra persona del equipo antes de fusionarse.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (instrumentadas).

## 0. Hallazgos del código real

- **La incidencia ya sabe clasificarse.** `Incidencia.tipoFalla` y `Incidencia.prioridad` existen, `IncidenciaEntity`
  los guarda, `MapeoFirestore` los sube y los baja, y la regla de `/incidencias` deja que el jefe los cambie. **No
  cambia la incidencia, su entidad ni su regla.** `AsignacionScreen` ya muestra la clasificación si existe, con el
  comentario «Sin la sugerencia de IA del prototipo: es de la HU-14».
- **La tabla de sugerencias existe sin uso.** `SugerenciaIAEntity` (`id`, `incidenciaId`, `tipo`, `contenido`,
  `aceptada`, `pendienteSincronizar`) no tiene `actualizadoEn` ni fechas; `SugerenciaIADao` (`porIncidencia`,
  `responder`, `guardar`) no lo usa nadie. `SigtecDatabase` está en `version = 6`. RF-1427 cambia la entidad, así que
  sube a 7 (PR-04).
- **`TipoSugerencia.SOLUCIONES_PARECIDAS` no se usa** (la HU-13 no guarda sus sugerencias, D-1 de la spec 013). La HU
  no lo toca; quitarlo va en la lista de mantenimiento.
- **Regla actual de `sugerenciasIA`:** crear con sesión y actualizar solo `aceptada`. Pasa a «solo crear, decidida y a
  nombre propio» (D-7, PR-07).
- **Subida.** `SincronizadorIncidencias` sube todas las colecciones y decide si quedan pendientes;
  `SincronizacionTiempoReal.vigilarPendientes()` combina cuatro flujos de pendientes. Se suma uno más: `combine` con
  tipos admite hasta cinco (PR-09 y PR-10).
- **`TarjetaSugerenciaIA`** tiene el botón «Usar» con texto fijo; RF-1419 pide «Usar borrador» (PR-24).
- **Informe técnico.** Cuatro campos (`diagnostico`, `trabajoRealizado`, `piezasCambiadas`, `observaciones`), dictado
  con `CampoDictado` y `CAMPOS_INFORME` en `AtencionNavigation`. Hay dos `when` exhaustivos sobre `CampoDictado`
  (`tituloDictado` en captura e `InformeTecnicoViewModel.alRecibirDictado`); `RegistrarFallaViewModel` usa `else`.
  Agregar `NOTAS` (PR-31) obliga a tocar los dos primeros.
- **Detalle.** El menú «Más opciones» tiene un solo ítem, «Abrir asistente», que navega a `Ruta.Asistente`, sin
  destino desde #144. Al quitar el ítem (PR-02) se quita el botón del menú entero. `Ruta.Asistente` se borra después
  (PR-03, ⚠️ `Rutas.kt`).
- **Lista.** El orden lo da el repositorio (más reciente primero, HU-04); los filtros son `FiltroDesplegable`
  privados de `ListaIncidenciasScreen`; `FilaIncidencia` no muestra la prioridad.
- **Conexión.** `core/sync/MonitorConexion` ofrece `hayConexion: Flow<Boolean>` y `hayConexionAhora()`.
- **Sin dependencias nuevas.** `firebase-ai` ya está en `app/build.gradle.kts` (BoM 34.19.0) y
  `kotlinx-serialization-json` también, para leer el JSON de Gemini. App Check se aplaza en la consola (D-9).
- **Errores.** No hay un caso para la IA: se agrega `ErrorDominio.IANoDisponible` (PR-11).
- **Warnings de Kotlin.** En `main`, cada calificador de Hilt sobre una propiedad del constructor
  (`@IoDispatcher private val io`) da el aviso «This annotation is currently applied to the value parameter only».
  Las clases nuevas de esta HU usan `@param:` desde el principio (P-9) para no sumar avisos.
