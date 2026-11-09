# Tareas 015 — Tablero de indicadores del área (HU-15)

> Salen de `spec.md` y `plan.md`. Cada tarea es un commit de 20 a 30 minutos como máximo, con su push inmediato. Van agrupadas por PR (plan, sección 1) y en el orden de la tabla.
>
> **Al empezar cada PR:** crear la rama desde su «Base» al día y abrir el PR con la plantilla llena (HU-15, criterios y archivos compartidos).
>
> **Al terminar cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas, el CI en verde, «Cómo probarlo» escrito y «Ready for review».
>
> Las rutas son relativas a `app/src/main/java/com/softcorp/sigtec/` y `app/src/test/java/com/softcorp/sigtec/`.
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

> **Estado (2026-11-09).** PR-01 y PR-02 fusionados en #149 y #150 (#148 es la spec; #151 corrige la descarga de la HU-05). T10 lo probó Alonso salvo el gráfico con cierres en los últimos 7 días. T11 es este cierre.

## PR-01: dominio (base `main`)
- [x] **T01 · `observarCierres` en `MovimientoDao` 🔶.** RF-1511.
  - Hecho cuando: compila.
- [x] **T02 · Modelos y `calcularIndicadores` con `CalcularIndicadoresTest`.** RF-1505 a RF-1508, RF-1511, RF-1514 a RF-1518, RF-1521, RF-1523.
  - Hecho cuando: pasan todos los casos de la spec, incluida la de 2 000 incidencias.
- [x] **T03 · `IndicadoresRepository`, su `Impl` y el `@Binds`**, con el doble. RF-1504, RF-1524.
  - Hecho cuando: compila.
- [x] **T04 · `ObservarIndicadores` con su prueba.** RF-1503, RF-1504.
  - Hecho cuando: pasa (sin sesión, técnico y jefe).

## PR-02: tablero (base PR-01)
- [x] **T05 · `TextosIndicadores` con su prueba.** RF-1502, RF-1512, RF-1514.
  - Hecho cuando: pasa.
- [x] **T06 · `IndicadoresViewModel` con su prueba.** RF-1501 a RF-1504.
  - Hecho cuando: pasa.
- [x] **T07 · `GraficoReparados` con Vico.** RF-1509, RF-1510, RF-1513.
  - Hecho cuando: se ve en las vistas previas.
- [x] **T08 · `IndicadoresScreen` según el prototipo 10**, con vistas previas en claro y oscuro. RF-1501, RF-1505, RF-1515 a RF-1522.
  - Hecho cuando: compila y se ve en las vistas previas.
- [x] **T09 · `IndicadoresNavigation` sin `PantallaEnConstruccion` y borrar `DatosDemo.kt` ⚠️.** RF-1522.
  - Hecho cuando: en el celular, con el jefe, se ve el tablero y la tarjeta del equipo abre el historial.

## PR-03: cierre (base `main` al día)
- [x] **T10 · Prueba manual en el celular** (spec, «Criterios de finalización»), con capturas en claro y oscuro.
- [x] **T11 · Spec «completa» y `MEMORY.md`.**
