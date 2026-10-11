# Tareas 002 — Ingreso con huella y bloqueo del celular (HU-02)

> Salen de `spec.md` y `plan.md`. RF-16 a RF-28 ya están en `main` desde el PR #6 (`11b7a6a`); ninguna tarea los reimplementa. Solo hay dos pendientes reales: el bug de RF-29 y la prueba faltante de RF-21.
> Cada tarea dura 20-30 min como máximo. Orden de adentro hacia afuera: dominio/prueba más aislada primero, luego ViewModel, luego pantalla, luego verificación.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/` (pruebas). ⚠️ = archivo compartido (`feature/acceso/*`), **requiere visto bueno del usuario antes de tocarlo** (`CLAUDE.md`).
> Nota de rama: `MEMORY.md` prevé `fix/huella-relanzada` para el bug de RF-29. El coordinador decide si estas tareas corren en esa rama o en una rama de esta spec; en cualquier caso, el PR debe citar tanto la HU-02 como esta spec.

## Fase 0 — Preparación

- [x] **T01 · Confirmar con el usuario el diseño de RF-29 antes de tocar código**
  Mostrar la sección 2 de `plan.md` (el evento de un solo uso `ofrecerHuellaParaId` en `LoginViewModel` y el cambio de key en `LoginScreen`) porque toca `feature/acceso/*`, archivo compartido según `CLAUDE.md`.
  RF: RF-29
  Hecho cuando: el usuario aprueba explícitamente el diseño (o pide un cambio, que se refleja en `plan.md` antes de seguir).
  Nota: el usuario aprobó explícitamente el diseño de la sección 2 antes de que empezara la implementación de T03, fuera del flujo normal de esta tarea.

- [x] **T02 · Crear o confirmar la rama de trabajo**
  Si se usa `fix/huella-relanzada` (prevista en `MEMORY.md`): verificar que exista y esté al día con `main`. Si se decide una rama propia de esta spec, crearla desde `main`.
  RF: —
  Hecho cuando: `git branch --show-current` muestra la rama elegida y `./gradlew testDebugUnitTest` pasa sin cambios.
  Nota: se usa la rama ya existente `feat/hu02-huella-sdd` (propia de esta spec) en vez de `fix/huella-relanzada`, para no cambiar de rama a mitad de la sesión.

## Fase 1 — Prueba faltante de RF-21 (no toca código de producción)

- [x] **T03 · Agregar el caso feliz de RF-21 a `EstadoAccesoTest.kt`**
  Agregar `` `el dueno desactiva su huella sin verificacion y sin cerrar sesion` ``: el dueño entra, activa su huella, la desactiva (`cambiarHuella(false)`), y se comprueba `Resultado.Exito(Unit)`, `activada == false` y que `estado().first()` sigue siendo `EstadoAcceso.Activa` (no se cierra su sesión). No usar `bloqueo.bloquear()`: RF-21 ocurre con la sesión activa, desde Ajustes de seguridad.
  RF: RF-21
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*EstadoAccesoTest"` pasa con el nuevo caso, y ningún archivo de producción cambió (`CambiarIngresoConHuella.kt` no se toca).

## Fase 2 — Corrección del bug de RF-29 (dominio del evento en el ViewModel) ⚠️

- [x] **T04 · Agregar el evento `ofrecerHuellaParaId` a `LoginUiState` ⚠️**
  En `feature/acceso/presentation/LoginViewModel.kt`, agregar el campo `ofrecerHuellaParaId: String? = null` a `LoginUiState`, con el comentario `// RF-29` explicando que es un evento de un solo uso.
  RF: RF-29
  Hecho cuando: compila y el campo tiene valor por defecto `null`, siguiendo el mismo estilo que `confirmarIngreso`.

- [x] **T05 · Observar la transición a `Bloqueada` y exponer el evento ⚠️**
  En el mismo archivo, agregar en `init { }` la observación de `observarEstadoAcceso()` con `.map { (it as? EstadoAcceso.Bloqueada)?.usuario?.id }.distinctUntilChanged()`, que actualiza `formulario` con el id cuando no es nulo. Agregar `fun alHuellaOfrecida()` que limpia el campo a `null`.
  RF: RF-29
  Hecho cuando: `./gradlew assembleDebug` compila y no hay ningún `if (pendientes > 0)` ni lógica de Android en el `ViewModel` (principio 3); `LoginViewModel.kt` sigue sin ningún `import android.*` salvo los ya existentes de Jetpack.

- [x] **T06 · Prueba del evento de un solo uso en `LoginViewModelTest`**
  Agregar un caso: al transicionar el `EstadoAcceso` a `Bloqueada` (con el doble `SesionRepositoryDemo`/`PreferenciasSeguridadFalsas`/`BloqueoApp` ya usados en el archivo), `estado.value.ofrecerHuellaParaId` trae el id del dueño; tras llamar `vm.alHuellaOfrecida()`, vuelve a `null`; al forzar otra recomposición del `combine` (por ejemplo `vm.alCambiarCorreo("x")`), el evento sigue en `null` — esto reproduce, a nivel de `ViewModel`, lo que antes causaba el relanzamiento en Compose.
  RF: RF-29
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*LoginViewModelTest"` pasa con el caso nuevo, usando `MainDispatcherRule`.

- [x] **T07 · Cambiar la key del `LaunchedEffect` en `LoginScreen.kt` ⚠️**
  Reemplazar `LaunchedEffect(estado.usuarioBloqueado?.id) { if (estado.usuarioBloqueado != null) pedirHuella() }` por `LaunchedEffect(estado.ofrecerHuellaParaId) { if (estado.ofrecerHuellaParaId != null) { pedirHuella(); vm.alHuellaOfrecida() } }` en `LoginRoute`. El botón «Ingresar con huella» sigue llamando a `pedirHuella` directamente, sin pasar por el evento.
  RF: RF-29
  Hecho cuando: `./gradlew assembleDebug` compila, las `@Preview` existentes de `LoginScreen.kt` siguen compilando sin cambios (no leen `ofrecerHuellaParaId`), y el archivo no agrega hexadecimales ni `fontSize`.

## Fase 3 — Verificación

- [x] **T08 · Comando del CI**
  Correr `./gradlew assembleDebug testDebugUnitTest lintDebug`.
  RF: RF-21, RF-29
  Hecho cuando: termina en verde y sin warnings de lint nuevos respecto a `main`.

- [x] **T09 · Prueba manual — NO automática: reproducir y verificar el arreglo de RF-29**
  **Detenerse aquí si no hay celular o emulador disponible; esta tarea no se puede completar solo con el comando del CI.**
  En el emulador o celular: iniciar sesión como `lramirez`, activar la huella en Ajustes de seguridad, bloquear la app (`BloquearApp`/«Bloquear SIGTEC»). Confirmar que el diálogo de huella aparece solo al entrar a la pantalla de acceso (sigue cumpliendo RF-24). Rotar el celular (o `adb shell settings put system accelerometer_rotation 1` + rotar, o `adb shell cmd uimode night yes` para cambiar el tema) mientras el diálogo no está visible (ya se canceló o respondió antes) y comprobar que **no** vuelve a aparecer solo. Repetir con un cambio de tema claro/oscuro. Luego, pulsar «Ingresar con huella» manualmente y comprobar que sigue funcionando como acción explícita.
  RF: RF-29
  Hecho cuando: tras rotar o cambiar de tema con el dueño bloqueado y el diálogo ya cerrado, `BiometricPrompt` no se relanza solo; solo se relanza al bloquear de nuevo o al pulsar «Ingresar con huella». Dejar la captura (opcional) en el scratchpad, nunca en el repo.
  Nota: el usuario probó este caso en el emulador y confirmó explícitamente que salió como se esperaba (no se relanza el diálogo al rotar ni al cambiar de tema; «Ingresar con huella» sigue funcionando como acción explícita).

- [x] **T10 · Prueba manual — NO automática: RF-21 en el celular**
  **Detenerse aquí si no hay celular o emulador disponible.**
  Con `lramirez` con sesión activa (sin bloquear): en Ajustes de seguridad, desactivar el interruptor de huella. Confirmar que no pide verificación biométrica adicional y que la sesión sigue activa (no vuelve a la pantalla de acceso).
  RF: RF-21
  Hecho cuando: el interruptor queda apagado sin pedir huella/PIN, y la app se queda en la pantalla donde estaba (no hay cierre de sesión).
  Nota: el usuario probó este caso en el emulador y confirmó explícitamente que salió como se esperaba (sin pedir verificación adicional y sin cerrar sesión).

## Fase 4 — Cierre

- [x] **T11 · Actualizar `spec.md`**
  Quitar la marca «(pendiente)» de RF-29 en el encabezado y en el requisito mismo; mover el punto del bug conocido en «Casos límite» a pasado («se corrigió») o quitarlo si ya no aplica; actualizar «Criterios de finalización» para que diga que RF-21 ya tiene prueba automatizada y que RF-29 ya no es un bug conocido.
  RF: RF-21, RF-29
  Hecho cuando: `grep "pendiente" specs/002-ingreso-huella/spec.md` no encuentra ningún RF pendiente.

- [x] **T12 · Actualizar `MEMORY.md`**
  Quitar la línea del «Bug menor (HU-02)» de «Pendientes conocidos» (ya no aplica) y, si corresponde, anotar en «Aprendizajes y errores a evitar» que un `LaunchedEffect` keyed por un valor estático no sobrevive a la recreación de la Activity; el evento de un solo uso debe vivir en el `ViewModel`.
  RF: —
  Hecho cuando: `MEMORY.md` sigue en ~50 líneas como máximo y ya no menciona el bug como pendiente.

- [x] **T13 · Commit y PR**
  Commit(s) Conventional Commits en español con la HU al final, por ejemplo `fix(acceso): no relanzar la huella al rotar el celular (HU-02)` y `test(acceso): cubrir la desactivacion de huella por el dueño (HU-02)`, sin línea de coautoría. PR con `.github/pull_request_template.md`, incluida la sección «Archivos compartidos que toca» (citando `feature/acceso/presentation/LoginViewModel.kt` y `LoginScreen.kt`), y las pruebas manuales de T09 y T10 descritas.
  RF: todos
  Hecho cuando: el PR existe, el CI «compilar-y-probar» está en verde, y la descripción enlaza `specs/002-ingreso-huella/`.
