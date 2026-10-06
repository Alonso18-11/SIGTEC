# Constitución de SIGTEC

Seis reglas que ningún cambio puede romper. Si una spec, un plan o un PR las contradice, gana esta constitución.

1. **Stack cerrado y simple.** Solo se usan las librerías de `gradle/libs.versions.toml`; agregar o actualizar una exige su propio PR aprobado. Antes que una librería nueva, se prefiere código simple con lo que ya existe.
2. **Spec antes que código.** Cada HU, desde la HU-01, tiene `specs/NNN-nombre/spec.md` con criterios de aceptación; el PR cita la HU y cubre cada criterio. Si el código cambia el comportamiento, la spec se actualiza en el mismo PR.
3. **Lógica fuera de la pantalla.** Reglas y permisos viven en casos de uso de `domain/`, en Kotlin puro (ningún `import android.*`). Las `Screen` solo dibujan el estado y llaman lambdas; nunca deciden reglas ni acceden a Room o Firestore.
4. **Toda regla tiene prueba.** Cada caso de uso y ViewModel nuevo tiene prueba en `app/src/test` con JUnit, coroutines-test y dobles de prueba propios, sin agregar librerías. `./gradlew assembleDebug testDebugUnitTest lintDebug` pasa antes de abrir el PR.
5. **Datos del usuario protegidos.** Contraseñas, huellas, claves y tokens nunca se guardan en el código, el repo ni Room, y `google-services.json` no se commitea; la única excepción es la sesión que Firebase Auth guarda por sí misma en el celular. Cerrar una incidencia o pedir un repuesto exige confirmación biométrica, y la IA solo sugiere: nada cambia sin aceptación humana.
6. **Español en el negocio.** Clases, funciones, variables, comentarios y textos de UI van en español; solo los sufijos que impone Android (`ViewModel`, `Screen`, `Dao`, `Entity`, `Worker`) quedan en inglés.
