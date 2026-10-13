# SIGTEC

App Android nativa para el área de Soporte y Sistemas de Soft Corporation. Permite registrar, asignar, atender y cerrar incidencias técnicas de equipos de cómputo, sobre todo desde el celular del técnico, que trabaja fuera de su escritorio.

Proyecto del curso Desarrollo de Sistemas Móviles (UNMSM).

## Perfiles

| Perfil | Qué hace |
|---|---|
| Jefe de Soporte y Sistemas | Registra y consulta incidencias, las asigna o reasigna a un técnico y ve los indicadores. |
| Técnico | Registra, consulta y atiende las incidencias que tiene asignadas, y usa el diccionario de fallas. |
| Personal de sistemas | Registra y consulta incidencias, y consulta el diccionario de fallas. |

## Cómo funciona

- **Funciona sin conexión.** Todo se guarda primero en el celular (Room) y las pantallas leen siempre de ahí.
- **Se sincroniza con Firebase.** `core/sync` sube los cambios a Firestore con WorkManager y baja en tiempo real lo que cambian los demás.
- **Costo cero (plan Spark de Firebase).** No usa Cloud Functions, FCM con servidor, Cloud Storage ni Analytics. Las notificaciones son locales.
- **La huella protege la sesión del dueño del celular**: desbloquea una sesión ya iniciada, no identifica a la persona.

## Stack

- Kotlin 2.2.10, integrado en AGP 9.3.3, con KSP.
- Jetpack Compose con Material 3 y Navigation Compose con rutas tipadas.
- Hilt, Room, DataStore y WorkManager.
- Firebase: Authentication (correo y contraseña), Cloud Firestore y AI Logic.
- minSdk 26 (Android 8.0); targetSdk y compileSdk 37.

Todas las versiones están en [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Requisitos

- Android Studio con soporte para AGP 9.3.
- JDK 25, el que fija [`gradle/gradle-daemon-jvm.properties`](gradle/gradle-daemon-jvm.properties).
- Un celular o emulador con Android 8.0 o superior.

## Configuración

1. Clona el repositorio y ábrelo en Android Studio.
2. Pide al equipo el archivo `google-services.json` por un canal privado y cópialo en `app/`.
   - Está en `.gitignore`: **nunca se sube al repositorio**.
   - Sin él, el build falla con «File google-services.json is missing».
   - En el CI llega desde el secreto `GOOGLE_SERVICES_JSON`.
3. Sincroniza Gradle y ejecuta la app.
4. Para ingresar necesitas una cuenta del proyecto de Firebase. Las cuentas de prueba también se piden al equipo; no están en el repositorio.

Las reglas de Firestore viven en [`firebase/firestore.rules`](firebase/firestore.rules). Se cambian en el repositorio, se revisan en el PR y después se publican a mano en la consola de Firebase.

## Comandos

```bash
./gradlew assembleDebug                 # compilar
./gradlew testDebugUnitTest             # pruebas unitarias (JVM)
./gradlew lintDebug                     # lint de Android
./gradlew connectedDebugAndroidTest     # pruebas instrumentadas: requieren celular o emulador

# Lo mismo que el CI; debe pasar antes de abrir un PR
./gradlew assembleDebug testDebugUnitTest lintDebug
```

## Cómo trabajamos

- **Spec antes que código.** Cada historia de usuario tiene su carpeta en [`specs/`](specs/) con `spec.md`, `plan.md` y `tasks.md`.
- **Reglas del proyecto:** [`docs/constitution.md`](docs/constitution.md) y [`CLAUDE.md`](CLAUDE.md), que incluye convenciones y problemas comunes con su solución.
- **Estado y pendientes:** [`MEMORY.md`](MEMORY.md).
- **Ramas y commits:** ramas `tipo/descripcion-corta` y commits en Conventional Commits en español, con la HU al final.
- **PR:** se abren con la [plantilla](.github/pull_request_template.md). `main` está protegida: exige el CI en verde y la aprobación de la otra persona del equipo.

## Equipo

- Nina ([@demianj123](https://github.com/demianj123))
- Alonso ([@Alonso18-11](https://github.com/Alonso18-11))
