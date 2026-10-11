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
- Specs SDD: HU-01 completa en `specs/001-inicio-sesion` (RF-1 a RF-15, pruebas manuales T20–T23 hechas en emulador); HU-02 completa en `specs/002-ingreso-huella` (RF-16 a RF-29, sin pendientes); faltan HU-03 a HU-05. Sus RF aún no se enlazan con los RF del producto (el documento no está en el repo).
- Reemplazar `fallbackToDestructiveMigration` por migraciones reales antes de publicar.
- Las reglas de Firestore no se probaron en el Rules Playground; validarlas con el flujo real de cada HU.
- Google Play (urgente): las cuentas personales nuevas necesitan una prueba cerrada con ~12 testers durante 14 días antes de publicar. Crear la cuenta y empezar la prueba al menos 3 semanas antes de la entrega; verificar la regla vigente en Play Console.
- Issues, milestones y etiquetas de GitHub sin crear.

## Aprendizajes y errores a evitar
- Al entrar otra persona sin cerrar sesión, el borrado de Room lo hace `core/sync` antes de volver a escuchar (D-12): si lo hace el repositorio, Firestore no reenvía lo ya descargado y la lista queda vacía.
- «Cambios sin enviar» cuenta incidencias, no movimientos (D-3); la subida sigue mirando ambos.
- Emulador sin internet pero con Wi-Fi: es el DNS (el router da DNS solo IPv6). Abrirlo con `-dns-server 8.8.8.8,1.1.1.1` o fijar DNS privado `dns.google`.
- Probar «Ingresar de todos modos» con pendientes es difícil: al volver la red, la subida le gana al ingreso.
- Un `LaunchedEffect` keyed por un valor que no cambia entre recreaciones (rotación, tema) se relanza solo porque la composición se destruye y reconstruye; el evento de un solo uso debe vivir en el `ViewModel`, no en la key del efecto.

## Próximos pasos
- (por definir)
