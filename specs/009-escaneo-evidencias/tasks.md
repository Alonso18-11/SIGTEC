# Tareas 009 — Código del equipo por cámara y evidencia fotográfica (HU-09)

> Salen de `spec.md` y `plan.md`. Cada tarea dura entre 20 y 30 minutos como máximo, termina en **un commit con
> su push inmediato** (antes del push se revisa el estado del PR) y depende solo de las anteriores de su PR. Van
> agrupadas por PR (plan, sección 1) y, dentro de cada uno, de adentro hacia afuera: reglas puras y dominio con sus
> pruebas → datos → `ViewModel` → pantalla → navegación → verificación.
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM). ⚠️ = compartido de `CLAUDE.md` (se aprueba junto con el plan); 🔶 = sensible fuera de esa lista.
> **M, N, O y Q solo se hacen si se aprueban D-5, D-6, D-7 y D-8 (opción a).**
> Cada «Abrir el PR» es: `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas,
> PR en borrador con la plantilla llena (HU-09, criterio 10 u 11, archivos compartidos), revisión pedida a
> Alonso18-11 y, al terminar, «Ready for review».
> Orden de fusión: A → B → D; C antes de E; E → F; G y H → I → J → K y L; L → M y N; G → O → Q; P al final.

## PR A — extraer el código del texto (`feat/hu09-lectura-codigo`, desde `main`)

- [ ] **T01 · Crear la rama desde `main` al día**
  `git switch main`, `git pull origin main` y `git switch -c feat/hu09-lectura-codigo`.
  RF: —
  Hecho cuando: `git branch --show-current` muestra la rama y `git log -1` coincide con `origin/main`.

- [ ] **T02 · `normalizarTexto`**
  En `feature/captura/domain/CodigoEquipo.kt`: mayúsculas, guiones unificados y sin espacios alrededor (plan 3.1).
  RF: RF-903
  Hecho cuando: `./gradlew assembleDebug` compila.

- [ ] **T03 · `candidatosCodigo`**
  En el mismo archivo: la expresión de 3.2 y la corrección de cada parte según su posición.
  RF: RF-903
  Hecho cuando: compila.

- [ ] **T04 · `LecturaCodigo` y `elegirCodigo`**
  `data class LecturaCodigo(codigo, enCatalogo)` y la regla de 3.3.
  RF: RF-904, RF-907
  Hecho cuando: compila.

- [ ] **T05 · `CodigoEquipoTest`**
  En `feature/captura/domain/`, los casos de la sección 7 del plan (incluido el texto del prototipo 13, una fecha y
  un anexo que no deben salir).
  RF: RF-903, RF-904, RF-907
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*CodigoEquipoTest"` pasa.

- [ ] **T06 · `RachaLectura`**
  `feature/captura/domain/RachaLectura.kt` con `siguiente`, `estable` y `LECTURAS_IGUALES = 3` (plan 3.4).
  RF: RF-905, RF-908
  Hecho cuando: compila.

- [ ] **T07 · `RachaLecturaTest`**
  Los cuatro casos de la sección 7.
  RF: RF-905, RF-908
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*RachaLecturaTest"` pasa.

- [ ] **T08 · Abrir el PR A**
  Compartidos: ninguno. «Cómo probarlo»: solo JUnit.
  RF: —
  Hecho cuando: el PR está abierto con el check «compilar-y-probar» en verde.

## PR B — leer el código con el catálogo (`feat/hu09-leer-codigo-catalogo`, apilada sobre A)

- [ ] **T09 · Crear la rama**
  Desde la rama de A (o desde `main`, si A ya se fusionó).
  RF: —
  Hecho cuando: `git log --oneline -3` muestra los commits de A debajo.

- [ ] **T10 · `CatalogoEquiposRepository`**
  Interfaz en `feature/captura/domain/` con `suspend fun existe(codigo: String): Boolean`.
  RF: RF-904, RF-907
  Hecho cuando: compila.

- [ ] **T11 · `LeerCodigoEquipo`**
  Caso de uso con `@Inject constructor` y `suspend operator fun invoke(texto)` (plan 4.1), sin `VerificarPermiso`
  (P-1).
  RF: RF-902 a RF-904, RF-907
  Hecho cuando: compila.

- [ ] **T12 · `CatalogoEquiposRepositoryFalso`**
  En `feature/captura/data/` de `app/src/test`, con los 4 equipos de prueba (`PC-CONT-014`, `PC-LOG-009`,
  `PC-SIS-003`, `IMP-RRHH-002`).
  RF: —
  Hecho cuando: compila con `testDebugUnitTest`.

- [ ] **T13 · `LeerCodigoEquipoTest`**
  Los cuatro casos de la sección 7.
  RF: RF-904, RF-907
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*LeerCodigoEquipoTest"` pasa.

- [ ] **T14 · `CatalogoEquiposRepositoryImpl`**
  En `feature/captura/data/`: `CatalogoDao.equipoPorCodigo`, `@IoDispatcher` y el `catch` de la convención
  (relanza `CancellationException`, `Log.w` y `false`).
  RF: RF-904
  Hecho cuando: compila.

- [ ] **T15 · `CapturaModule`**
  `feature/captura/di/CapturaModule.kt` con el `@Binds` del catálogo.
  RF: —
  Hecho cuando: `./gradlew assembleDebug` compila (Hilt resuelve el grafo).

- [ ] **T16 · Abrir el PR B**
  Compartidos: ninguno.
  RF: —
  Hecho cuando: el PR está abierto con el CI en verde.
