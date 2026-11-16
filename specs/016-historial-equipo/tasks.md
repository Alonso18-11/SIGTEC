# Tareas 016 — Historial de atenciones de un equipo (HU-16)

> Salen de `spec.md` y `plan.md`. Cada tarea es un commit chico con su push inmediato. Van agrupadas por PR (plan,
> sección 1) y en el orden de la tabla.
>
> **Al empezar cada PR:** revisar que Alonso no tenga ramas ni PR de la HU-16, crear la rama desde su «Base» al día y
> abrir el PR con la plantilla llena (HU-16, criterios y archivos compartidos).
>
> **Al terminar cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas, el
> CI en verde, todos los commits de demianj123, «Cómo probarlo» escrito y «Ready for review».
>
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

## PR-A: bajar los informes (base `main`)
- [ ] **T01 · `InformeTecnicoDao.porIncidencia` 🔶** con su caso en `InformeTecnicoDaoTest`. RF-1624.
  - Hecho cuando: compila (la instrumentada se corre con un celular).
- [ ] **T02 · `debeGuardarInforme` en `ReglasSincronizacion` 🔶** con `ReglasSincronizacionTest`. RF-1624.
  - Hecho cuando: pasa (sin local, local subido, local pendiente).
- [ ] **T03 · `aInformeEntity` en `MapeoFirestore` 🔶.** RF-1625.
  - Hecho cuando: compila.
- [ ] **T04 · Escuchar `/informes` en `SincronizacionTiempoReal` 🔶.** RF-1623.
  - Hecho cuando: compila y pasan las pruebas.

## PR-B: cálculo (base `main`)
- [ ] **T05 · `ReglasEquipo` con su prueba y `Indicadores.kt` usándolas.** D-2, RF-1613.
  - Hecho cuando: pasan `ReglasEquipoTest` y `CalcularIndicadoresTest` sin cambios.
- [ ] **T06 · Modelos y `calcularHistorial` con `CalcularHistorialTest`.** RF-1609 a RF-1618.
  - Hecho cuando: pasan los casos de la spec, incluido el de 500 incidencias.
- [ ] **T07 · Consultas por equipo en `MovimientoDao`, `InformeTecnicoDao` y `CatalogoDao` 🔶**, con sus casos
  instrumentados. RF-1622.
  - Hecho cuando: compila.
- [ ] **T08 · `HistorialRepository`, su `Impl` y el `@Binds`**, con el doble. RF-1622, RF-1626.
  - Hecho cuando: compila.
- [ ] **T09 · `ObservarHistorialEquipo` con su prueba.** RF-1602, RF-1604.
  - Hecho cuando: pasa (sin sesión, cada perfil, código normalizado, recalcula con datos nuevos).

## PR-C: pantalla y búsqueda (base PR-B)
- [ ] **T10 · `TextosHistorial` con su prueba.** RF-1612, RF-1616, RF-1617, RF-1618.
  - Hecho cuando: pasa.
- [ ] **T11 · `HistorialEquipoViewModel` con su prueba.** RF-1602 a RF-1605.
  - Hecho cuando: pasa (con código, búsqueda, escaneo, volver a buscar, sin permiso).
- [ ] **T12 · `FilaAtencion`** con vistas previas. RF-1615 a RF-1620, D-8.
  - Hecho cuando: se ve en las vistas previas.
- [ ] **T13 · `HistorialEquipoScreen` según el prototipo 11**, con vistas previas en claro y oscuro. RF-1603, RF-1607
  a RF-1621.
  - Hecho cuando: compila y se ve en las vistas previas.
- [ ] **T14 · `IndicadoresNavigation` sin `PantallaEnConstruccion`.** RF-1601, RF-1605, RF-1620.
  - Hecho cuando: compila y pasa el comando del CI.

## PR-D: acceso en Inicio (base `main`)
- [ ] **T15 · `puedeConsultarHistorial` en `ResumenInicio`** con `ResumenInicioTest`. RF-1606.
  - Hecho cuando: pasa con los tres perfiles.
- [ ] **T16 · «Historial» en los accesos rápidos y su navegación.** RF-1606.
  - Hecho cuando: compila y se ve en las vistas previas.

## PR-E: cierre (base `main` al día)
- [ ] **T17 · Prueba manual en el celular** (spec, «Criterios de finalización»), con capturas en claro y oscuro, y las
  pruebas instrumentadas de los DAO.
- [ ] **T18 · Spec «completa» y `MEMORY.md`.**
