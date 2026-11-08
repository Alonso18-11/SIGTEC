# Tareas 012 — Directorio de colaboradores al registrar (HU-12)

> Salen de `spec.md` y `plan.md`. Cada tarea es un commit de 20 a 30 minutos como máximo, con su push inmediato. Van agrupadas por PR (plan, sección 1) y en el orden de la tabla.
>
> **Al empezar cada PR:**
> - crear la rama desde su «Base» al día;
> - hacer `git merge` de las ramas de «Trae» que sigan abiertas;
> - abrir el PR con la plantilla llena: HU-12, criterios, archivos compartidos y qué trae de otros PR.
>
> **Al terminar cada PR:**
> - `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas;
> - el CI en verde;
> - «Cómo probarlo» escrito;
> - «Ready for review».
>
> Las rutas son relativas a `app/src/main/java/com/softcorp/sigtec/` y `app/src/test/java/com/softcorp/sigtec/`.
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

## Antes de PR-02: servidor de prueba
- [ ] **T00 · Recurso `colaboradores` en MockAPI.io**, con los 4 registros del plan (sección 5). Lo hace Alonso, o el asistente con su visto bueno. RF: —
  - Hecho cuando: `GET <base>/colaboradores?equipo=PC-CONT-014` responde en el navegador y `?equipo=NO-EXISTE` da 404.
  - Si el plan gratuito no lo admite, ver P-1.

## PR-01: modelo (base `main`)
- [ ] **T01 · `areaResponsable` en `Incidencia` ⚠️ e `IncidenciaEntity` ⚠️**, y `version = 5` en `SigtecDatabase` ⚠️. RF-1214, RF-1218.
  - Hecho cuando: compila.
- [ ] **T02 · Subir y bajar `areaResponsable` en `MapeoFirestore` 🔶.** RF-1215.
  - Hecho cuando: compila.
- [ ] **T03 · `IncidenciaDaoTest` con y sin área.** RF-1214.
  - Hecho cuando: `./gradlew connectedDebugAndroidTest` pasa en un celular.

## PR-02: API del directorio (base `main`)
- [ ] **T04 · Contrato `DirectorioRepository`** con `Colaborador`, `ResultadoDirectorio` y `ErrorDirectorio` (plan 3.1). RF-1201, RF-1211, RF-1212.
  - Hecho cuando: compila.
- [ ] **T05 · `DirectorioApi`, `ColaboradorDto` y `MapeoDirectorio`** (plan 3.2). RF-1205, RF-1211, RF-1212.
  - Hecho cuando: compila.
- [ ] **T06 · `MapeoDirectorioTest`.** RF-1205, RF-1211, RF-1212.
  - Hecho cuando: la prueba pasa.
- [ ] **T07 · `DirectorioRepositoryImpl` y `DirectorioModule`**, con 10 s y `URL_DIRECTORIO`. RF-1201, RF-1212.
  - Hecho cuando: compila y la app arranca (Hilt resuelve el grafo).
- [ ] **T08 · `DirectorioRepositoryFalso`.** RF: —
  - Hecho cuando: compila en las pruebas.

## PR-03: guardar y mostrar el área (base PR-01)
- [ ] **T09 · `RegistrarIncidencia` recibe `areaResponsable`.** RF-1214, RF-1217.
  - Hecho cuando: compila.
- [ ] **T10 · `RegistrarIncidenciaTest` con área.** RF-1214, RF-1217.
  - Hecho cuando: la prueba pasa.
- [ ] **T11 · «nombre · área» en el detalle**, con su vista previa. RF-1216.
  - Hecho cuando: compila y se ve en las vistas previas clara y oscura.

## PR-04: el formulario (base PR-03, trae PR-02)
- [ ] **T12 · `RegistroIncidenciaViewModel` consulta el directorio** con la espera de 0,6 s, el escaneo inmediato y la cancelación (plan 4). RF-1201 a RF-1205.
  - Hecho cuando: compila.
- [ ] **T13 · Completar, sugerir con «Usar», editar, cambiar el código y guardar el área** (plan 4). RF-1206 a RF-1210, RF-1213, RF-1214.
  - Hecho cuando: compila.
- [ ] **T14 · `RegistroIncidenciaViewModelTest` con los casos de la spec.** RF-1201 a RF-1214.
  - Hecho cuando: la prueba pasa.
- [ ] **T15 · Pantalla: texto de ayuda, área y anexo, buscando, sugerencia y avisos con «Reintentar»**, con vistas previas de cada estado en claro y oscuro. RF-1203, RF-1206, RF-1207, RF-1211, RF-1212.
  - Hecho cuando: compila y se instala en el celular.
- [ ] **T16 · Prueba en el celular** con capturas en claro y oscuro (criterios de finalización de la spec). RF-1201 a RF-1218.
  - Hecho cuando: está descrita en el PR.

## PR-05: cierre (base `main` al día)
- [ ] **T17 · Spec y tareas «completa» y la línea de la HU-12 en `MEMORY.md`.** RF: —
  - Hecho cuando: está fusionado.
