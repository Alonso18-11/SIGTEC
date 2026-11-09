# Tareas 013 — Diccionario de fallas con sugerencias (HU-13)

> Salen de `spec.md` y `plan.md`. Cada tarea es un commit de 20 a 30 minutos como máximo, con su push inmediato. Van agrupadas por PR (plan, sección 1) y en el orden de la tabla.
>
> **Al empezar cada PR:**
> - crear la rama desde su «Base» al día;
> - abrir el PR con la plantilla llena: HU-13, criterios, archivos compartidos y qué trae de otros PR.
>
> **Al terminar cada PR:**
> - `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas;
> - el CI en verde;
> - «Cómo probarlo» escrito;
> - «Ready for review».
>
> Las rutas son relativas a `app/src/main/java/com/softcorp/sigtec/` y `app/src/test/java/com/softcorp/sigtec/`.
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

> **Estado (2026-11-09).** PR-01 a PR-06 fusionados en #140 a #145 (#139 es la spec). T04: `FallaDiccionarioDaoTest` queda para correrlo en el celular. T22 lo probó Alonso salvo «Guardar en el diccionario» y los perfiles de personal de sistemas y jefe. T23 es este cierre.

## PR-01: modelo (base `main`)
- [x] **T01 · `autorId`, `incidenciaOrigenId` y `registradaEn` en `FallaDiccionario` ⚠️ y su entidad ⚠️**, con `version = 6` en `SigtecDatabase` ⚠️. RF-1313, RF-1318.
  - Hecho cuando: compila.
- [x] **T02 · `existeConOrigen` y `porId` en `FallaDiccionarioDao`; quitar `buscar`.** RF-1319.
  - Hecho cuando: compila.
- [x] **T03 · Mapeo de fallas en `MapeoFirestore` 🔶.** RF-1320.
  - Hecho cuando: compila.
- [x] **T04 · `FallaDiccionarioDaoTest`.** RF-1301, RF-1319.
  - Hecho cuando: compila (`assembleDebugAndroidTest`) y Alonso lo corre en el celular.

## PR-02: reglas (base `main`)
- [x] **T05 · Regla `create` de `fallas` en `firestore.rules` ⚠️.** RF-1323.
  - Hecho cuando: los casos del Rules Playground están en el PR; después de fusionar, Alonso la publica.

## PR-03: sincronización (base PR-01)
- [x] **T06 · Subir fallas pendientes en `SincronizadorIncidencias` 🔶** y contarlas en `hayPendientes`. RF-1321.
  - Hecho cuando: compila.
- [x] **T07 · Escuchar `fallas` en `SincronizacionTiempoReal` 🔶** con `debeReemplazarLocal`. RF-1322.
  - Hecho cuando: compila.
- [x] **T08a · Sumar las fallas sin subir en `EstadoDatosRepositoryImpl` 🔶.** RF-1331.
  - Hecho cuando: compila.
- [x] **T08 · `MapeoFallaTest`.** RF-1320.
  - Hecho cuando: pasa.

## PR-04: dominio (base PR-01)
- [x] **T09 · `DiccionarioRepository`, su `Impl` y el `@Binds`**, con el doble `DiccionarioRepositoryFalso`. RF-1313.
  - Hecho cuando: compila.
- [x] **T10 · `normalizar` y `filtrarFallas` con `BuscarFallasTest`.** RF-1304 a RF-1306.
  - Hecho cuando: pasan, incluida la de 500 fallas en menos de 3 s.
- [x] **T11 · `fallasParecidas` con `FallasParecidasTest`.** RF-1325, RF-1326.
  - Hecho cuando: pasan todos los casos de la spec.
- [x] **T12 · `ObservarDiccionario` y `RegistrarFalla` con sus pruebas.** RF-1308, RF-1313, RF-1314, RF-1318.
  - Hecho cuando: pasan (sin sesión, sin permiso, personal de sistemas, campos vacíos y camino feliz).

## PR-05: pantallas (base PR-04)
- [x] **T13 · Ruta `RegistrarFalla` en `Rutas.kt` ⚠️ y `CampoDictado.SINTOMAS` 🔶.** RF-1310, RF-1311.
  - Hecho cuando: compila.
- [x] **T14 · `DiccionarioViewModel` con su prueba.** RF-1301 a RF-1309.
  - Hecho cuando: pasa.
- [x] **T15 · `DiccionarioScreen` según el prototipo 14**, con vistas previas en claro y oscuro (lista, vacía, sin resultados, personal de sistemas). RF-1301 a RF-1309.
  - Hecho cuando: compila y se ve en las vistas previas.
- [x] **T16 · `RegistrarFallaViewModel` con su prueba** (validación, dictado, origen desde una incidencia). RF-1310 a RF-1318.
  - Hecho cuando: pasa.
- [x] **T17 · `RegistrarFallaScreen`**, con vistas previas. RF-1310 a RF-1312.
  - Hecho cuando: compila.
- [x] **T18 · `DiccionarioNavigation` sin `PantallaEnConstruccion`, y `SigtecRaiz` ⚠️.** RF-1301, RF-1310.
  - Hecho cuando: en el celular se registra una falla y aparece en la lista.

## PR-06: detalle (base PR-05)
- [x] **T19 · Sugerencias y estado del diccionario en `DetalleIncidenciaViewModel`**, con sus pruebas. RF-1316, RF-1319, RF-1324 a RF-1330.
  - Hecho cuando: pasan.
- [x] **T20 · Sección «Fallas parecidas» y botón «Guardar en el diccionario» en `DetalleIncidenciaScreen`**, con vistas previas. RF-1316, RF-1319, RF-1324, RF-1327.
  - Hecho cuando: compila y se ve en las vistas previas.
- [x] **T21 · Navegar a `RegistrarFalla(incidenciaId)` desde el detalle.** RF-1317.
  - Hecho cuando: en el celular se abre el formulario lleno desde una incidencia cerrada.

## PR-07: cierre (base `main` al día)
- [x] **T22 · Prueba manual completa en el celular** (spec, «Criterios de finalización»), con capturas en claro y oscuro.
- [x] **T23 · Spec «completa», `MEMORY.md` y `CLAUDE.md` (19 rutas).**
