# Tareas 001 — Inicio de sesión y permisos por perfil (HU-01)

> Salen de `spec.md` y `plan.md`. Cada tarea dura 20-30 min como máximo y depende solo de las anteriores. El orden va de adentro hacia afuera: dominio con pruebas → datos → ViewModel → pantalla → verificación.
> RF-1 a RF-4, RF-8 (salvo su prueba), RF-9 y RF-12 ya están en `main`; aquí solo aparecen cuando una tarea los toca.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/` (pruebas). ⚠️ = archivo compartido, ya coordinado con el equipo.

## Fase 0 — Preparación

- [ ] **T01 · Crear la rama de trabajo**
  Rama `feat/hu01-cierre-sesion-seguro`: desde `main` si la HU-05 ya se fusionó, o desde `feat/hu05-sincronizacion` si sigue en revisión (rama apilada).
  RF: —
  Hecho cuando: `git branch --show-current` muestra `feat/hu01-cierre-sesion-seguro` y `./gradlew testDebugUnitTest` pasa sin cambios.

## Fase 1 — Funciones puras (`core/domain/ReglasSesion.kt` ⚠️)

- [ ] **T02 · `DecisionBorrado` y `evaluarBorrado`**
  Crear `ReglasSesion.kt` con la `sealed interface DecisionBorrado` (`SinRiesgo`, `Confirmar(pendientes)`) y `evaluarBorrado(pendientes: Int)`. Crear `ReglasSesionTest.kt` con los casos 0, 1 y 5.
  RF: RF-13, RF-14
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*ReglasSesionTest"` pasa, y el archivo no tiene ningún `import android`.

- [ ] **T03 · `normalizarCorreo` y su uso en `IniciarSesion` ⚠️**
  Agregar la función, sus pruebas (espacios en los extremos, mayúsculas, espacios internos intactos) y reemplazar la normalización en línea de `core/domain/usecase/IniciarSesion.kt`.
  RF: RF-3
  Hecho cuando: `ReglasSesionTest`, `SesionYPermisosTest` y `LoginViewModelTest` pasan, e `IniciarSesion.kt` ya no contiene `.trim().lowercase()`.

- [ ] **T04 · `perfilDesdeTexto`**
  Agregar la función y sus pruebas: los 3 valores válidos, `null`, un texto mal escrito y un valor en minúsculas.
  RF: RF-7, RF-15
  Hecho cuando: `ReglasSesionTest` pasa y cubre los 6 casos.

- [ ] **T05 · `debeBorrarDatosAlEntrar` y `esOtraPersona`**
  Agregar las dos funciones y sus pruebas.
  - `debeBorrarDatosAlEntrar`: anterior nulo, mismo uid, uid distinto.
  - `esOtraPersona`: sin bloqueado, mismo correo con otro formato, otro correo.
  RF: RF-11
  Hecho cuando: `ReglasSesionTest` pasa y cubre los 6 casos.

- [ ] **T06 · Prueba del menú por perfil**
  Crear `DestinoPrincipalTest.kt` con la tabla de la sección 3.1 del plan para los 3 perfiles. No cambia código de producción.
  RF: RF-1, RF-8
  Hecho cuando: la prueba pasa y comprueba que el jefe no ve «Diccionario» y que solo el técnico ve «Mis tareas».

- [ ] **T07 · Texto «N cambios sin enviar»**
  Crear `feature/acceso/presentation/TextosSalida.kt` con `textoCambiosSinEnviar(n)` y `TextosSalidaTest.kt`.
  RF: RF-11, RF-13
  Hecho cuando: la prueba pasa con 1 → «1 cambio sin enviar» y 2 → «2 cambios sin enviar».

## Fase 2 — Casos de uso

- [ ] **T08 · Doble `EstadoDatosRepositoryFalso`**
  Crear `core/sync/EstadoDatosRepositoryFalso.kt` en `app/src/test`, con un método para fijar `pendientesPorEnviar`.
  RF: RF-11, RF-13, RF-14 (habilita sus pruebas)
  Hecho cuando: el doble compila con `./gradlew testDebugUnitTest` y vive en el mismo paquete que `EstadoDatosRepositoryImpl`.

- [ ] **T09 · `EvaluarCierreSesion` ⚠️**
  Crear `core/domain/usecase/EvaluarCierreSesion.kt`, que lee el primer valor de `ObservarEstadoDatos` y devuelve `evaluarBorrado`. Crear `EvaluarCierreSesionTest.kt`.
  RF: RF-13, RF-14
  Hecho cuando: la prueba pasa con 0 → `SinRiesgo` y 3 → `Confirmar(3)`.

- [ ] **T10 · `EvaluarIngresoDeOtraPersona` ⚠️**
  Crear `core/domain/usecase/EvaluarIngresoDeOtraPersona.kt`. Obtiene el dueño bloqueado con `ObservarEstadoAcceso`, usa `esOtraPersona` y `evaluarBorrado`. Crear `EvaluarIngresoDeOtraPersonaTest.kt`.
  RF: RF-11, RF-13
  Hecho cuando: la prueba pasa en los 4 casos del plan: sin bloqueado, mismo dueño, otra persona con 0 pendientes y otra persona con 2.

## Fase 3 — Datos (`core/data/SesionRepositoryFirebase.kt` ⚠️)

- [ ] **T11 · Usar las funciones puras en el repositorio**
  Reemplazar la conversión de perfil por `perfilDesdeTexto` y la condición de borrado por `debeBorrarDatosAlEntrar`. No cambia el comportamiento.
  RF: RF-7, RF-11
  Hecho cuando: `./gradlew assembleDebug testDebugUnitTest` pasa, y el repositorio ya no contiene `Perfil.valueOf` ni la comparación `uidAnterior != uid`.

- [ ] **T12 · Sin conexión al leer el perfil y sin sesiones a medias**
  Traducir `FirebaseFirestoreException` con código `UNAVAILABLE` a `SinConexion`, y hacer `signOut()` en todo fallo posterior a autenticarse, salvo `CancellationException`.
  RF: RF-5, RF-5b
  Hecho cuando: compila; en revisión de código, cada `catch` posterior a `signInWithEmailAndPassword` llama a `signOut()` antes de devolver; y el `Log.w` no incluye el correo.

- [ ] **T13 · Perfil revocado con la sesión abierta**
  Cuando `usuarios/{uid}` pasa de un perfil válido a uno nulo o inválido, ejecutar el mismo cierre que `cerrarSesion()`. Solo aplica si antes había un perfil válido, para no interferir con el caso de RF-7 durante el ingreso.
  RF: RF-15
  Hecho cuando: compila, y con un comentario `// RF-15` que explica por qué solo actúa si antes había un perfil válido.

## Fase 4 — ViewModels

- [ ] **T14 · Confirmación de salida en `AjustesSeguridadViewModel`**
  Agregar `confirmarSalida: Int?` a `AjustesUiState`. Reemplazar `salir()` por `alPulsarCerrarSesion()` y agregar `alConfirmarSalida()` y `alCancelarSalida()`, usando `EvaluarCierreSesion`.
  RF: RF-10, RF-13, RF-14
  Hecho cuando: compila; `AjustesSeguridadScreen` llama a `vm::alPulsarCerrarSesion`; y no queda ningún `if (pendientes > 0)` en el ViewModel (la regla vive en el dominio, principio 3).

- [ ] **T15 · Confirmación de ingreso en `LoginViewModel`**
  Agregar `confirmarIngreso: Int?` a `LoginUiState`, consultar `EvaluarIngresoDeOtraPersona` en `ingresar()`, y agregar `alConfirmarIngreso()` y `alCancelarIngreso()`.
  RF: RF-11
  Hecho cuando: compila y las 10 pruebas existentes de `LoginViewModelTest` siguen pasando.

- [ ] **T16 · Pruebas nuevas de `LoginViewModel`**
  Agregar 3 casos a `LoginViewModelTest`: otra persona con pendientes abre el diálogo y no inicia sesión; confirmar inicia la sesión; cancelar no la inicia.
  RF: RF-11
  Hecho cuando: `LoginViewModelTest` pasa con 13 pruebas.

## Fase 5 — Pantallas

- [ ] **T17 · Diálogo de salida en `AjustesSeguridadScreen`**
  `AlertDialog` según la sección 4 del plan (ícono, título, texto con `textoCambiosSinEnviar`, «Cerrar de todos modos» en `colorScheme.error`, «Cancelar»; tocar fuera cancela), con `@Preview` claro y oscuro del diálogo.
  RF: RF-13
  Hecho cuando: las previews se ven en Android Studio en claro y oscuro, y el archivo no tiene hexadecimales ni `fontSize`.

- [ ] **T18 · Diálogo de ingreso en `LoginScreen`**
  `AlertDialog` «¿Ingresar con otra cuenta?» con el nombre del dueño, «Ingresar de todos modos» y «Cancelar», con `@Preview` claro y oscuro.
  RF: RF-11
  Hecho cuando: las previews se ven en claro y oscuro, y el archivo no tiene hexadecimales ni `fontSize`.

## Fase 6 — Verificación

- [ ] **T19 · Comando del CI**
  Correr `./gradlew assembleDebug testDebugUnitTest lintDebug`.
  RF: todos
  Hecho cuando: termina en verde y sin warnings de lint nuevos respecto a `main`.

- [ ] **T20 · Prueba manual: cerrar sesión**
  En el celular o el emulador: registrar una incidencia en modo avión → «Cerrar sesión» → ver el diálogo → cancelar (sigue en Ajustes) → confirmar → entrar con otra cuenta. Repetir sin pendientes.
  RF: RF-10, RF-13, RF-14
  Hecho cuando: el diálogo muestra «1 cambio sin enviar»; cancelar no borra nada; la otra cuenta no ve la incidencia; y sin pendientes se cierra sin diálogo. Las capturas en claro y oscuro quedan en el scratchpad (`CLAUDE.md`, Verificación 6).

- [ ] **T21 · Prueba manual: otra persona con el dueño bloqueado**
  Con `lramirez` como dueño de la huella: crear un pendiente en modo avión → «Bloquear» → ingresar con `atorres`.
  RF: RF-11
  Hecho cuando: aparece «Luis tiene 1 cambio sin enviar…»; cancelar mantiene a Luis bloqueado; y confirmar deja entrar a Ana sin datos de Luis.

- [ ] **T22 · Prueba manual: errores de ingreso**
  Contraseña incorrecta; modo avión al ingresar; varios intentos fallidos seguidos; y una cuenta sin documento en `usuarios`.
  RF: RF-4, RF-5, RF-5b, RF-6, RF-7
  Hecho cuando: cada caso muestra su mensaje exacto de la spec, y después de cada error la app queda en la pantalla de acceso sin sesión (al reabrirla, pide la contraseña).

- [ ] **T23 · Prueba manual: perfil revocado**
  Con la app abierta, cambiar en la consola de Firestore el campo `perfil` de un usuario de prueba a un valor inválido, y luego restaurarlo.
  RF: RF-15
  Hecho cuando: la app vuelve sola a la pantalla de acceso y, al restaurar el perfil y volver a entrar, no hay datos del ingreso anterior.

## Fase 7 — Cierre

- [ ] **T24 · Actualizar la documentación**
  En `spec.md`, quitar las marcas «(pendiente)» y actualizar los criterios de finalización con las pruebas reales. En `MEMORY.md`, actualizar el estado de la HU-01 y agregar la nota de fotos para la HU de captura (D-7).
  RF: todos
  Hecho cuando: `grep "pendiente" spec.md` no encuentra RF pendientes, y `MEMORY.md` sigue en unas 50 líneas como máximo.

- [ ] **T25 · Commit y PR**
  Commit `feat(acceso): confirmar cierre de sesión con cambios sin enviar (HU-01)`, sin la línea de coautoría. PR en borrador con `.github/pull_request_template.md`, incluida la sección «Archivos compartidos que toca», y la prueba manual de T20 a T23 descrita.
  RF: todos
  Hecho cuando: el PR existe en borrador, el CI «compilar-y-probar» está en verde y la descripción enlaza `specs/001-inicio-sesion/`.
