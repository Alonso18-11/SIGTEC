# Plan 015 — Tablero de indicadores del área (HU-15)

> Basado en `spec.md` (borrador del 2026-11-09, con D-1 a D-4 respondidas) y en `docs/constitution.md`.
> El código vive en `feature/indicadores` (dominio, datos y pantalla). Toca estos archivos compartidos:
> - `core/database/dao/MovimientoDao.kt` (consulta nueva, sin cambiar la entidad ni `version`);
> - `core/navigation/DatosDemo.kt` (se borra: `EQUIPO_DEMO` solo existía para esta pantalla);
> - `MEMORY.md`.
>
> No toca entidades, `firestore.rules`, `Rutas.kt`, el manifest ni las dependencias (Vico 2.1.3 ya está). Cada PR trata un solo tema, lleva la HU al final del título y necesita la aprobación de la otra persona del equipo antes de fusionarse.
> Las rutas son relativas a `app/src/main/java/com/softcorp/sigtec/` (código) y `app/src/test/java/com/softcorp/sigtec/` (pruebas JVM).

## 0. Hallazgos del código real

- **La pestaña ya existe.** `DestinoPrincipal.INDICADORES` se muestra solo con `VER_INDICADORES` (jefe). `IndicadoresNavigation` dibuja `PantallaEnConstruccion` con un botón a `HistorialEquipo(EQUIPO_DEMO)`; el historial sigue en construcción (HU-16).
- **Datos disponibles en Room.**
  - `IncidenciaDao.observarTodas()`: fecha de registro, marca, equipo y `critica`.
  - `MovimientoDao` solo lee por incidencia; falta leer todos los CERRADA (la fecha de cierre). Los movimientos de todas las incidencias bajan con la sincronización.
  - `CatalogoDao.observarTecnicos()` y `IncidenciaDao.observarAsignacionesSinSubir()`: lo mismo que usa la asignación (`cargaEfectiva`, HU-06).
  - `CatalogoDao` tiene los equipos con su oficina.
  - `ObservarEstadoDatos` da `ultimaDescarga` para «Actualizado…».
- **Vico 2.1.3** (`compose-m3`) está en `app/build.gradle.kts` y todavía no se usa en ninguna pantalla.
- **Componentes v2:** `CabeceraHero`, `TarjetaMetrica` (con variante de alerta), `Tarjeta`, `IconosBarraEstadoClaros` y `FondoBarraEstado`, como en Inicio.

## 1. División en PR, dependencias y orden de fusión

| PR | Rama | Título | Base | RF |
|---|---|---|---|---|
| PR-01 | `feat/hu15-dominio-indicadores` | `feat(indicadores): cálculo de los indicadores del área (HU-15)` | `main` | RF-1503 a RF-1508, RF-1511, RF-1514 a RF-1518, RF-1521, RF-1523, RF-1524 |
| PR-02 | `feat/hu15-tablero` | `feat(indicadores): tablero de indicadores con Vico (HU-15)` | PR-01 | RF-1501 a RF-1524 |
| PR-03 | `docs/hu15-cierre` | `docs(indicadores): cierre de la HU-15 con las pruebas manuales (HU-15)` | `main` al día | todos |

## 2. Archivos por PR

Leyenda: 🆕 nuevo, ✏️ modificado, ⚠️ compartido de `CLAUDE.md`, 🔶 sensible fuera de esa lista.

### PR-01 — dominio
- ✏️🔶 `core/database/dao/MovimientoDao.kt`: `observarCierres(): Flow<List<MovimientoEntity>>` (`WHERE tipo = 'CERRADA'`).
- 🆕 `feature/indicadores/domain/Indicadores.kt`: los modelos (`Indicadores`, `ReparadosDia`, `ConteoEstado`, `CargaTecnico`, `EquipoAEvaluar`) y la función pura `calcularIndicadores(incidencias, cierres, tecnicos, sinSubir, equipos, ahora, zona)`.
- 🆕 `feature/indicadores/domain/IndicadoresRepository.kt`, 🆕 `data/IndicadoresRepositoryImpl.kt` y 🆕 `di/IndicadoresModule.kt`: un `Flow` que combina incidencias, cierres, técnicos, asignaciones sin subir y equipos de Room (solo lectura).
- 🆕 `feature/indicadores/domain/ObservarIndicadores.kt`: `VerificarPermiso(VER_INDICADORES)` y el flujo calculado en `@DefaultDispatcher`, con el reloj que recibe del ViewModel.
- 🆕 Pruebas: `CalcularIndicadoresTest`, `ObservarIndicadoresTest` y el doble `IndicadoresRepositoryFalso`.

### PR-02 — tablero
- 🆕 `feature/indicadores/presentation/IndicadoresViewModel.kt`: `IndicadoresUiState`, el reloj de 60 s (mismo patrón que la lista), el nombre y el cargo de la sesión, y `ultimaDescarga`.
- 🆕 `feature/indicadores/presentation/IndicadoresScreen.kt` (`IndicadoresRoute` + `IndicadoresScreen`): cabecera, métricas, gráfico, estados, carga y equipo; vistas previas en claro y oscuro.
- 🆕 `feature/indicadores/presentation/GraficoReparados.kt`: el gráfico de columnas de Vico con los colores del tema, la columna de hoy en acento y la descripción para TalkBack.
- 🆕 `feature/indicadores/presentation/TextosIndicadores.kt`: «Actualizado…», días, «1,8 días» y «6 h».
- ✏️ `feature/indicadores/presentation/IndicadoresNavigation.kt`: reemplaza `PantallaEnConstruccion` por `IndicadoresRoute`; la tarjeta del equipo navega a `HistorialEquipo(codigo)`.
- ✏️⚠️ borrar `core/navigation/DatosDemo.kt`.
- 🆕 Pruebas: `IndicadoresViewModelTest` y `TextosIndicadoresTest`.

### PR-03 — cierre
- ✏️ `specs/015-tablero-indicadores/*`: estado «completa» y lo probado.
- ✏️ `MEMORY.md` (línea de la HU-15).

## 3. Diseño del cálculo

Todo en una función pura para probarla sin Room:

1. `cierrePorIncidencia`: de los movimientos CERRADA, el más reciente por incidencia (RF-1511).
2. **Reparados por día:** para `d` de 6 a 0, el día local `hoy - d` y cuántos cierres caen en él.
3. **Pendientes y críticos:** las incidencias con estado Pendiente; críticas si `critica` o `fechaRegistro < ahora - 5 días`.
4. **Tiempo promedio:** cierres de los últimos 30 días; promedio de `cierre - registro` en horas, y el texto decide días u horas.
5. **Estados:** conteo por `MarcaSeguimiento` (las 4 pendientes y CERRADA).
6. **Carga:** técnicos con `cargaEfectiva(tecnico, sinSubir[id])`, por nombre; `alcanzoLimite = activas >= limite`.
7. **Equipo a evaluar:** incidencias con registro en los últimos 90 días agrupadas por equipo; el de más incidencias (empate: el de la incidencia más reciente) si tiene 3 o más, con su oficina del catálogo.

Con 2 000 incidencias es lineal (un recorrido por lista y unos mapas) y tarda milisegundos.

## 4. Riesgos
- **Vico 2.1.3 en Compose con el tema:** primera vez que se usa; se prueba en las vistas previas y en el celular. Si los colores por columna se complican, la columna de hoy se marca con el color del acento por separado.
- **Movimientos CERRADA que no bajaron:** una incidencia cerrada sin su movimiento no cuenta hasta que llegue (caso límite de la spec).
- **Datos de prueba:** hay pocas incidencias cerradas en Firestore; para ver el gráfico con varias columnas puede hacer falta cerrar algunas más al probar.
