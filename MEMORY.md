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
- Specs SDD (una línea por HU; cada quien edita solo la de sus HU):
  - HU-01 `specs/001-inicio-sesion`: completa (RF-1 a RF-15).
  - HU-02 `specs/002-ingreso-huella`: completa (RF-16 a RF-29).
  - HU-03 `specs/003-registro-incidencia`: completa (RF-30 a RF-41).
  - HU-04 `specs/004-lista-detalle-incidencias`: completa (RF-42 a RF-57).
  - HU-05 `specs/005-sincronizacion`: completa (RF-58 a RF-78): contador que se crea si no existe, errores permanentes que piden atención en vez de reintentar, red validada y reloj inyectado.
  - HU-06 `specs/006-asignacion-tecnico` (Nina): completa (RF-601 a RF-625). Reglas publicadas el 2026-10-12 (9:10). El Rules Playground no simula `getAfter()`: los casos que cambian el técnico dan «error desconocido» y se prueban desde la app. «Iniciar atención» de RF-624 se prueba en la HU-08.
  - HU-07 `specs/007-tiempo-real-aviso` (Nina; terminada por Alonso): completa (RF-701 a RF-713). Aviso local en `core/notificaciones`, lo dispara `SincronizacionTiempoReal`; permiso `POST_NOTIFICATIONS` solo al técnico en Android 13+. Con la app ya en la pantalla de desbloqueo, tocar el aviso no relanza la huella (RF-712). Avisar con la app cerrada es de la HU-17.
  - HU-08 `specs/008-bandeja-cierre-informe` (Alonso): completa (RF-801 a RF-829). Reglas publicadas el 2026-10-12. Cerrar resta 1 a la carga del técnico en la misma escritura (RF-823). Con ramas apiladas y squash, comprobar `git log main..<rama>` antes de cerrar la HU: los PR C y D quedaron fuera de #33 y se subieron en #34.
  - HU-09 `specs/009-escaneo-evidencias` (Nina): spec aprobada (RF-901 a RF-944), D-1 a D-13 respondidas (2026-10-16 y 2026-10-17). Spec #40, plan #41 y tareas #42 (apilados). Código en 58 PR de un solo cambio, por tandas de 5: tandas 1 a 4 hechas (PR-01 a PR-20 = #43 a #62); ninguno fusionado aún. PR-07 trae PR-06 y PR-08 trae PR-05 por merge; PR-16 trae 13, 14, 15 y también PR-10 (el permiso del manifest, que faltaba en la cadena); PR-20 trae PR-17. La rama de PR-20 tiene el flujo de escaneo completo para probarlo en el celular. Siguiente: tanda 5 (PR-21 a PR-25). CameraX: `ExperimentalGetImage` pide el `OptIn` de `androidx.annotation` (con el de Kotlin, lint da error). Los apilados no tienen CI hasta que se fusione #43 (PR-01).
  - HU-10 (Alonso): pendiente. Bloque RF-1001….
- `firestore.rules` con el `allow create` de `contadores/incidencias` (RF-69) probado en el Rules Playground y publicado (2026-10-11). No borrar el contador en el proyecto real: con incidencias ya numeradas, recrearlo desde 1 duplicaría INC-0001.
- Clases de Firebase como `FirebaseFirestoreException.Code` no se inicializan en las pruebas JVM: las reglas puras trabajan con el nombre del código (`esCodigoPermanente`). No usar `isReturnDefaultValues` para taparlo.
- Doble `RelojFalso` (`app/src/test/.../core/sync/RelojFalso.kt`) listo pero sin usar: falta una prueba completa de `SincronizadorIncidencias`/`SincronizacionTiempoReal` que lo aproveche.
- Room subió de `version = 1` a `version = 2` por `necesitaAtencion` en `IncidenciaEntity`/`MovimientoEntity`.
- Reemplazar `fallbackToDestructiveMigration` por migraciones reales antes de publicar.
- Copia de seguridad de Android: `allowBackup="true"` con las reglas de ejemplo respalda en la cuenta de Google la base Room y las preferencias de la huella (y podría restaurarlas en otro celular). La HU-09 solo excluye las fotos (D-9 de la spec 009); lo demás va en un PR aparte.
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
