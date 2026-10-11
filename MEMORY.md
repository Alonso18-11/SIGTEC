# MEMORY.md — SIGTEC

Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte.
Las reglas permanentes van en `CLAUDE.md`, no aquí.

## Estado actual
- En `main`: HU-01 (login con Firebase), HU-02 (huella), HU-03 (registro de incidencia), HU-04 (lista y detalle) y HU-05 (sincronización con Firestore, subida con WorkManager y aviso de conexión).
- Siguen con `PantallaEnConstruccion`: asignación, atención (bandeja, repuestos, informe), captura (cámara, OCR, fotos, dictado), diccionario, asistente IA, indicadores e historial.
- Aún no existen: API de Logística y del directorio (Retrofit), workers diarios (repuestos, resumen, casos demorados) ni funciones de IA.

## Decisiones (y por qué)
- La huella protege el celular de su dueño; no identifica a la persona. Por eso, cerrar sesión la desvincula y bloquear la conserva. Esto difiere de la especificación original del producto, que aún debe actualizarse.
- Fotos solo en almacenamiento interno y notificaciones locales: el proyecto debe costar cero (plan Spark de Firebase).

- Flujo multiagente en `.claude/agents/` (coordinador, planificador, implementador, revisor); el coordinador se usa con `claude --agent coordinador`. Las restricciones de escritura y comandos van en hooks de `.claude/hooks/`, porque `tools` no filtra por ruta ni por comando.

## Pendientes conocidos
- Fotos: cuando exista la HU de captura, ampliar RF-10/RF-11 de `specs/001-inicio-sesion` para borrar también las fotos internas al cambiar de persona (D-7).
- Specs SDD completas (sin pendientes propios): HU-01 a HU-04. HU-05 (`specs/005-sincronizacion`) completa tras esta revisión de 5 correcciones: RF-69 (el contador `contadores/incidencias` se crea con `tx.set` si no existía; antes fallaba con `tx.update`), RF-75 (error permanente `PERMISSION_DENIED`/`FAILED_PRECONDITION`/`INVALID_ARGUMENT` al subir a Firestore se marca `necesitaAtencion` en Room y deja de reintentarse; el transitorio sigue reintentando), RF-76/77 (`AvisoConexion.kt` ya tiene un tercer estado "necesita atención"), RF-78 (`SubirCambiosWorker` exige salida a internet validada, no solo "conectada") y el reloj inyectado (`SincronizadorIncidencias`/`SincronizacionTiempoReal` ya no llaman a `Instant.now()`).
- `firestore.rules` con el `allow create` de `contadores/incidencias` (RF-69) probado en el Rules Playground y publicado (2026-10-11). No borrar el contador en el proyecto real: con incidencias ya numeradas, recrearlo desde 1 duplicaría INC-0001.
- Clases de Firebase como `FirebaseFirestoreException.Code` no se inicializan en las pruebas JVM: las reglas puras trabajan con el nombre del código (`esCodigoPermanente`). No usar `isReturnDefaultValues` para taparlo.
- Doble `RelojFalso` (`app/src/test/.../core/sync/RelojFalso.kt`) listo pero sin usar: falta una prueba completa de `SincronizadorIncidencias`/`SincronizacionTiempoReal` que lo aproveche.
- Room subió de `version = 1` a `version = 2` por `necesitaAtencion` en `IncidenciaEntity`/`MovimientoEntity`.
- Reemplazar `fallbackToDestructiveMigration` por migraciones reales antes de publicar.
- Google Play (urgente): las cuentas personales nuevas necesitan una prueba cerrada con ~12 testers durante 14 días antes de publicar. Crear la cuenta y empezar la prueba al menos 3 semanas antes de la entrega; verificar la regla vigente en Play Console.
- Issues, milestones y etiquetas de GitHub sin crear.

## Aprendizajes y errores a evitar
- Al entrar otra persona sin cerrar sesión, el borrado de Room lo hace `core/sync` antes de volver a escuchar (D-12): si lo hace el repositorio, Firestore no reenvía lo ya descargado y la lista queda vacía.
- «Cambios sin enviar» cuenta incidencias, no movimientos (D-3); la subida sigue mirando ambos.
- Emulador sin internet pero con Wi-Fi: es el DNS (el router da DNS solo IPv6). Abrirlo con `-dns-server 8.8.8.8,1.1.1.1` o fijar DNS privado `dns.google`.
- Probar «Ingresar de todos modos» con pendientes es difícil: al volver la red, la subida le gana al ingreso.
- Un `LaunchedEffect` keyed por un valor que no cambia entre recreaciones (rotación, tema) se relanza solo porque la composición se destruye y reconstruye; el evento de un solo uso debe vivir en el `ViewModel`, no en la key del efecto.
- `core/sync` ya baja `/equipos` (catálogo, solo lectura, sin lógica de conflictos) con el mismo `escuchar(...)` genérico usado para `incidencias`/`movimientos`. Los casos de uso nunca inyectan un DAO directamente: pasan por el repositorio del módulo (`IncidenciaRepository.existeEquipo`, no `CatalogoDao` en `RegistrarIncidencia`).

## Próximos pasos
- (por definir)
