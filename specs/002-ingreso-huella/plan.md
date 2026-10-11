# Plan 002 — Ingreso con huella y bloqueo del celular (HU-02)

> Basado en `spec.md` y `docs/constitution.md`. RF-16 a RF-28 ya existen en `main` desde antes de este flujo SDD (PR #6, `11b7a6a`); este plan **no** los vuelve a implementar. Solo cubre dos huecos que la spec aprobada marca como pendientes:
> 1. El bug de RF-29 (el diálogo de huella se relanza solo al rotar o cambiar de tema con el dueño bloqueado).
> 2. La prueba automatizada que falta para el camino feliz de RF-21 (el dueño desactiva su propia huella).
>
> `MEMORY.md` prevé una rama `fix/huella-relanzada` para el punto 1; aquí se planifica igual, como pidió el usuario, para que quede documentado en el flujo SDD de la spec 002. El coordinador decide en qué rama ejecuta cada tarea.

## 0. Adaptaciones de la plantilla
- **«Algoritmo del mapa»:** no aplica; se usa el pseudocódigo de la sección 3 para el nuevo flujo del evento de huella automática.
- **«`node --test`»:** igual que en el plan 001, se usa JUnit 4 y coroutines-test con `./gradlew testDebugUnitTest`, sin dependencias nuevas.
- **«`hoy` como parámetro»:** ninguna de las dos correcciones depende de la fecha; no aplica.

## 1. Diagnóstico del bug (RF-29)

`LoginRoute` lanza la verificación biométrica así (`LoginScreen.kt`, líneas 55-67):

```kotlin
val pedirHuella = { verificador.verificar(..., alTerminar = vm::alVerificarHuella) }

LaunchedEffect(estado.usuarioBloqueado?.id) {
    if (estado.usuarioBloqueado != null) pedirHuella()
}
```

`LaunchedEffect` solo evita relanzarse cuando su *key* no cambia **dentro de la misma composición continua**. Una rotación o un cambio de tema claro/oscuro recrea la `Activity` (y por tanto toda la composición de `LoginRoute`): el árbol de Compose se destruye y se vuelve a construir desde cero. El `LaunchedEffect` arranca como si fuera la primera vez, sin importar que la key (`usuarioBloqueado?.id`) tenga el mismo valor que antes de rotar, porque no hay continuidad de composición que comparar. El resultado es que, con el dueño bloqueado, cada rotación relanza el diálogo de `BiometricPrompt` sin que el usuario lo pida — justo lo que describe el caso límite de la spec.

La causa, entonces, no es el valor de la key sino **dónde vive la decisión de lanzar**: hoy vive en un efecto de Compose, que no sobrevive a la recreación de la Activity. Lo que sí sobrevive a la recreación es el `ViewModel` (vive en el `NavBackStackEntry`, no en la `Activity`), igual que ya aprovecha el patrón de eventos de un solo uso que describe `CLAUDE.md` (`registradaId` y, en esta misma pantalla, `confirmarIngreso`).

## 2. Diseño de la corrección (RF-29)

Mover la decisión de «hay que ofrecer la huella automáticamente» al `ViewModel`, como un evento de un solo uso ligado a la **transición** hacia `EstadoAcceso.Bloqueada` (no al valor estático de `usuarioBloqueado`), y que la pantalla consume y limpia de inmediato. Como ese evento vive en el `StateFlow` del `ViewModel`, una vez consumido queda en `null` y permanece así aunque la Activity se recree.

### 2.1 `LoginUiState` (presentación)
Agregar un campo de evento de un solo uso:

```kotlin
// Evento de un solo uso: null = nada que ofrecer, el id = lanzar la huella una vez. // RF-29
val ofrecerHuellaParaId: String? = null
```

No se toca `usuarioBloqueado` (sigue siendo el estado reactivo que ya pinta el saludo y habilita «Ingresar con huella»).

### 2.2 `LoginViewModel` (presentación)
Hoy `estado` se construye con un único `combine` de tres flujos. La oferta automática no puede derivarse ahí porque `combine` se re-evalúa en cada recomposición de cualquiera de los tres flujos de entrada (incluida la propia `formulario`, que cambia cuando la pantalla consume el evento) y no distingue «ya estaba bloqueado» de «recién se bloqueó».

Se agrega una segunda observación, independiente del `combine`, que solo reacciona a una transición real del dominio:

```kotlin
init {
    viewModelScope.launch {
        observarEstadoAcceso()
            .map { (it as? EstadoAcceso.Bloqueada)?.usuario?.id }
            .distinctUntilChanged()                 // solo en la transición, no en cada recomposición
            .collect { idBloqueado ->
                if (idBloqueado != null) {
                    formulario.update { it.copy(ofrecerHuellaParaId = idBloqueado) }
                }
            }
    }
}

fun alHuellaOfrecida() = formulario.update { it.copy(ofrecerHuellaParaId = null) }
```

`distinctUntilChanged()` corta el ruido: si la app sigue bloqueada para el mismo usuario, no se vuelve a emitir, aunque el `Flow` de `ObservarEstadoAcceso` reemita el mismo valor (por ejemplo, al reconectar o sincronizar). Si el dueño se desbloquea y se vuelve a bloquear, el id cambia de no-nulo a nulo y de nuevo a no-nulo (aunque sea el mismo id), así que sí se vuelve a emitir, que es el comportamiento correcto: un bloqueo nuevo sí debe volver a ofrecer la huella.

### 2.3 `LoginScreen.kt` (presentación)
`LoginRoute` consume el evento con un `LaunchedEffect` keyed por el propio evento, y lo limpia de inmediato:

```kotlin
LaunchedEffect(estado.ofrecerHuellaParaId) {
    if (estado.ofrecerHuellaParaId != null) {
        pedirHuella()
        vm.alHuellaOfrecida()
    }
}
```

Por qué esto arregla el bug: tras una rotación, el `LaunchedEffect` vuelve a arrancar (igual que antes), pero lee el valor **ya consumido** (`null`) desde el `StateFlow` persistido en el `ViewModel`, porque `alHuellaOfrecida()` se llamó antes de que la rotación ocurriera. No hace falta que Compose «recuerde» nada: la fuente de verdad del evento ya no vive en la composición, vive en el `ViewModel`.

El botón «Ingresar con huella» sigue llamando a `pedirHuella` directamente (acción explícita del usuario), sin pasar por el evento — eso no cambia.

### 2.4 Alternativas descartadas
| Alternativa | Por qué se descarta |
|---|---|
| Guardar un `Boolean` con `rememberSaveable` en `LoginScreen` para no relanzar tras rotar. | `rememberSaveable` sí sobrevive a una recreación de Activity por rotación, pero no sirve como solución general: un cambio de tema también recrea la Activity y, dependiendo de cómo se configure, algunos escenarios no pasan por el mismo ciclo de guardado; además mezcla una regla de «cuándo ofrecer la huella» con el estado de la pantalla, en vez del dominio/ViewModel (principio 3 de la constitución). |
| Quitar `LaunchedEffect` y lanzar `pedirHuella()` solo desde el botón, sin automatismo. | Rompe RF-24 (el sistema debe lanzar la verificación por sí solo al entrar a la pantalla bloqueada), que sí es un requisito ya implementado y aceptado; no es parte de lo pedido. |
| Usar `rememberUpdatedState` o cambiar la key del `LaunchedEffect` a un valor que incluya un contador de recomposición. | No ataca la causa: seguiría relanzándose en cada recreación de Activity porque el contador también se reiniciaría a cero. |
| Detectar la rotación con `LocalConfiguration` y comparar antes/después. | Es un parche de UI para una regla de dominio («ofrecer una sola vez por bloqueo»); además es frágil ante otras causas de recreación (cambio de idioma, modo multiventana). |

## 3. Diseño de la prueba faltante (RF-21)

No hay diseño de producción: `CambiarIngresoConHuella(activar = false)` ya desvincula sin pedir verificación y sin cerrar sesión (líneas 30-32 del archivo). Solo falta el caso de prueba. Se agrega a `EstadoAccesoTest.kt`, junto a los demás casos de `cambiarHuella`, siguiendo el mismo estilo que `` `activar la huella no expulsa a quien la esta activando` ``:

```kotlin
@Test
fun `el dueno desactiva su huella sin verificacion y sin cerrar sesion`() = runTest {
    entra("lramirez@softcorp.pe")
    cambiarHuella(true)

    val resultado = cambiarHuella(false)

    assertEquals(Resultado.Exito(Unit), resultado)
    assertFalse(preferencias.preferenciasHuella.first().activada)
    assertTrue(estado().first() is EstadoAcceso.Activa)   // RF-21: no cierra su sesión
}
```

No usa `bloqueo.bloquear()`: RF-21 ocurre desde Ajustes de seguridad con la sesión activa, no con el celular bloqueado (eso ya lo distingue la spec en «Casos límite»: activar/desactivar desde Ajustes no bloquea esa misma sesión).

## 4. Archivos

Leyenda: ✏️ se modifica · 🆕 se crea · ⚠️ archivo compartido (`feature/acceso/*`, requiere visto bueno según `CLAUDE.md` antes de implementar).

| Archivo | Responsabilidad | RF |
|---|---|---|
| ✏️⚠️ `feature/acceso/presentation/LoginViewModel.kt` | Agrega `ofrecerHuellaParaId` a `LoginUiState`, la observación de la transición a `Bloqueada` y `alHuellaOfrecida()`. | RF-29 |
| ✏️⚠️ `feature/acceso/presentation/LoginScreen.kt` | Cambia la key del `LaunchedEffect` de `estado.usuarioBloqueado?.id` al evento `estado.ofrecerHuellaParaId`, y llama a `vm.alHuellaOfrecida()` tras pedir la huella. | RF-29 |
| ✏️ `app/src/test/.../LoginViewModelTest.kt` | Prueba nueva: tras una transición a `Bloqueada`, el estado expone el evento una sola vez; al consumirlo (`alHuellaOfrecida()`), vuelve a `null` y no reaparece aunque el `combine` se reevalúe por otro motivo. | RF-29 |
| ✏️ `app/src/test/.../EstadoAccesoTest.kt` | Agrega el caso del camino feliz de RF-21 (sección 3). | RF-21 |

No se toca `CambiarIngresoConHuella.kt`, `ObservarEstadoAcceso.kt` ni `Acceso.kt`: su comportamiento ya es correcto.

`feature/acceso/*` está en la lista de «archivos compartidos — pregunta antes» de `CLAUDE.md`. El coordinador debe mostrar este plan al usuario y obtener su visto bueno antes de tocar `LoginViewModel.kt` y `LoginScreen.kt`.

## 5. Cómo se pinta en la interfaz
No hay cambios visuales: el diálogo biométrico (`BiometricPrompt`, fuera de Compose) y la pantalla de acceso se ven igual. El único cambio es *cuándo* se dispara el diálogo automáticamente. No se requieren nuevas `@Preview`: las previews existentes de `LoginScreen.kt` ya cubren `LoginUiState` con `usuarioBloqueado` distinto de `null`, y el nuevo campo `ofrecerHuellaParaId` no afecta el renderizado (no se lee en `LoginScreen`, solo en `LoginRoute`).

## 6. Estrategia de pruebas

Se usa `./gradlew testDebugUnitTest` con JUnit 4, `kotlinx-coroutines-test`, `MainDispatcherRule` y los dobles existentes (`SesionRepositoryDemo`, `PreferenciasSeguridadFalsas`, `BloqueoApp`). No se agregan librerías ni dobles nuevos.

| Prueba | Casos | RF |
|---|---|---|
| `EstadoAccesoTest` (nuevo caso) | El dueño desactiva su huella: `Resultado.Exito`, `activada == false`, sesión sigue `Activa`. | RF-21 |
| `LoginViewModelTest` (nuevo caso) | Al bloquear al dueño, `estado.ofrecerHuellaParaId` trae su id una vez; tras `alHuellaOfrecida()` vuelve a `null`; si se fuerza otra recomposición del `combine` (p. ej. cambiando `correo`), el evento sigue en `null` — simula el efecto de una rotación sin volver a disparar la huella. | RF-29 |

**Verificado a mano, no automatizado (ya documentado así en `spec.md`):** que `BiometricPrompt` realmente no reaparece al rotar el celular o cambiar el tema con el dueño bloqueado. Depende de Compose y de Android; no se puede construir en la JVM (mismo motivo que D-8 del plan 001).

**Cierre:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde antes de abrir el PR o marcar la tarea como terminada.

## 7. Cobertura de RF

| RF | Dónde vive | Prueba |
|---|---|---|
| RF-21 | `CambiarIngresoConHuella` (sin cambios) | `EstadoAccesoTest` (caso nuevo) |
| RF-29 | `LoginViewModel.ofrecerHuellaParaId`, `LoginScreen.LoginRoute` | `LoginViewModelTest` (caso nuevo) + manual (rotación y cambio de tema) |

## 8. Respeto a la constitución
1. **Stack cerrado:** sin dependencias nuevas.
2. **Spec antes que código:** esta corrección solo implementa lo que `spec.md` ya marca como RF-29 (pendiente) y como hueco de prueba de RF-21; no cambia ningún RF. Si al implementar se decide un nombre de campo distinto al propuesto aquí, no hace falta tocar `spec.md` (es detalle de implementación, no de comportamiento).
3. **Lógica fuera de la pantalla:** la decisión de «cuándo ofrecer la huella» pasa de un efecto de Compose a un evento observado en el `ViewModel`, que es donde corresponde: `LoginScreen` solo sigue dibujando y disparando lambdas.
4. **Toda regla tiene prueba:** los dos huecos señalados por QA quedan cubiertos con pruebas unitarias nuevas.
5. **Datos protegidos:** sin cambios; no se toca nada de huella, contraseñas ni tokens.
6. **Español:** nombres nuevos (`ofrecerHuellaParaId`, `alHuellaOfrecida`) siguen la convención del archivo.

## 9. Archivos compartidos que toca (requieren visto bueno)
`feature/acceso/presentation/LoginViewModel.kt` y `feature/acceso/presentation/LoginScreen.kt` — ambos dentro de `feature/acceso/*`, listado explícito de `CLAUDE.md` en «archivos compartidos — pregunta antes». **Pendiente de visto bueno del usuario antes de implementar.**
