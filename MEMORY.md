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

## Pendientes conocidos
- Cerrar sesión ejecuta `clearAllTables()` y pierde lo registrado sin señal. Ya especificado como RF-13/RF-14 de `specs/001-inicio-sesion` (avisar «Tienes N registros sin enviar» y confirmar); falta implementarlo.
- Specs SDD: HU-01 con `spec.md` y `plan.md` aprobados (RF-5b, RF-11, RF-13 a RF-15 sin implementar; el cambio de sesión ya está coordinado con el equipo); faltan HU-02 a HU-05. Sus RF aún no se enlazan con los RF del producto (el documento no está en el repo).
- Reemplazar `fallbackToDestructiveMigration` por migraciones reales antes de publicar.
- Las reglas de Firestore no se probaron en el Rules Playground; validarlas con el flujo real de cada HU.
- Google Play (urgente): las cuentas personales nuevas necesitan una prueba cerrada con ~12 testers durante 14 días antes de publicar. Crear la cuenta y empezar la prueba al menos 3 semanas antes de la entrega; verificar la regla vigente en Play Console.
- Issues, milestones y etiquetas de GitHub sin crear.

## Aprendizajes y errores a evitar
- (vacío por ahora)

## Próximos pasos
- (por definir)
