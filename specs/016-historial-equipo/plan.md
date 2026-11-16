# Plan 016 — Historial de atenciones de un equipo (HU-16)

> Basado en `spec.md` (lista el 2026-11-16, con D-1 a D-8 aprobadas) y en `docs/constitution.md`.
> El código vive en `feature/indicadores` (D-6), con ámbito de commit `indicadores`; la descarga de informes, en
> `core/sync` (D-1), y el acceso rápido, en `feature/inicio` (D-5). Toca estos archivos compartidos o sensibles:
> - `core/sync/SincronizacionTiempoReal.kt`, `MapeoFirestore.kt` y `ReglasSincronizacion.kt` (D-1);
> - `core/database/dao/InformeTecnicoDao.kt`, `MovimientoDao.kt` y `CatalogoDao.kt` (solo consultas nuevas, sin cambiar
>   entidades ni `version`);
> - `feature/inicio/*` (acceso «Historial»);
> - `MEMORY.md`.
>
> No toca entidades, `SigtecDatabase`, `firestore.rules`, `Rutas.kt` (`HistorialEquipo(codigoEquipo: String? = null)` ya
> existe), el manifest ni las dependencias. Cada PR trata un bloque, con varios commits chicos y push después de cada
> uno, lleva la HU al final del título y necesita la aprobación de Alonso antes de fusionarse.
> Las rutas son relativas a `app/src/main/java/com/softcorp/sigtec/` (código), `app/src/test/java/com/softcorp/sigtec/`
> (pruebas JVM) y `app/src/androidTest/java/com/softcorp/sigtec/` (pruebas de DAO).

## 0. Hallazgos del código real

- **La ruta y los accesos ya existen.** `Ruta.HistorialEquipo(codigoEquipo: String? = null)` está registrada en
  `IndicadoresNavigation.kt` con `PantallaEnConstruccion`. La abren «Ver historial» del detalle
  (`IncidenciasNavigation`) y la tarjeta del equipo a evaluar (`IndicadoresNavigation`). Nadie la abre sin código.
- **Lo que ya está en Room.**
  - `IncidenciaDao.observarPorEquipo(codigo)` ya existe (más reciente primero). Todas las incidencias bajan a todos
    los perfiles.
  - `MovimientoDao.observarCierres()` trae todos los CERRADA (HU-15); todos los movimientos bajan.
  - `InformeTecnicoDao` solo lee por incidencia. **Los informes no bajan**: solo están los escritos en el propio
    celular (D-4 del plan 008). En Firestore, el documento se llama como la incidencia y no guarda el `id` local.
  - `CatalogoDao.equipoPorCodigo` es `suspend`; falta una versión `Flow`.
- **La regla de «equipo a evaluar»** (90 días, 3 o más) es privada de `Indicadores.kt` (`DIAS_EQUIPO`,
  `MINIMO_EQUIPO`).
- **Escaneo:** `CLAVE_CODIGO_ESCANEADO` en `CapturaNavigation.kt`; `IncidenciasNavigation` lo lee con
  `savedStateHandle.getStateFlow` y lo consume con `alConsumirCodigo`. Se repite el patrón.
- **Componentes:** `CabeceraHero`, `TarjetaMetrica` (con `enCabecera`), `EtiquetaEstado` (con `icono`),
  `IconosBarraEstadoClaros`, `FondoBarraEstado`, `CampoTexto`, `BotonPrincipal`, `BarraSuperior`. La fila de cada
  atención es propia (D-8); `LineaTiempo` no se toca.
- **Inicio:** `AccesosRapidos` solo se muestra con `ResumenInicio.puedeRegistrar`.
- **Textos:** `textoTiempoAtencion` (HU-15) ya formatea con coma sin depender del `Locale`.

## 1. División en PR, dependencias y orden de fusión

| PR | Rama | Título | Base | RF |
|---|---|---|---|---|
| PR-A | `feat/hu16-bajar-informes` | `feat(core): bajar los informes técnicos a todos los celulares (HU-16)` | `main` | RF-1623 a RF-1625 |
| PR-B | `feat/hu16-calculo-historial` | `feat(indicadores): cálculo del historial de un equipo (HU-16)` | `main` | RF-1602, RF-1604, RF-1609 a RF-1615, RF-1622, RF-1626 |
| PR-C | `feat/hu16-pantalla-historial` | `feat(indicadores): pantalla del historial del equipo con búsqueda (HU-16)` | PR-B | RF-1601 a RF-1621 |
| PR-D | `feat/hu16-acceso-inicio` | `feat(inicio): acceso rápido al historial del equipo (HU-16)` | `main` | RF-1606 |
| PR-E | `docs/hu16-cierre` | `docs(indicadores): cierre de la HU-16 con las pruebas manuales (HU-16)` | `main` al día | todos |

PR-A, PR-B y PR-D son independientes. PR-C se apila sobre PR-B. PR-E se abre cuando haya prueba en el celular; hasta
entonces, el estado va en la línea de `MEMORY.md` de la spec (#213).

## 2. Archivos por PR

Leyenda: 🆕 nuevo, ✏️ modificado, ⚠️ compartido de `CLAUDE.md`, 🔶 sensible fuera de esa lista.

### PR-A — bajar los informes (D-1)
- ✏️🔶 `core/database/dao/InformeTecnicoDao.kt`: `suspend fun porIncidencia(incidenciaId): InformeTecnicoEntity?`.
- ✏️🔶 `core/sync/ReglasSincronizacion.kt`: `debeGuardarInforme(local: InformeTecnicoEntity?) = local == null`
  (RF-1624).
- ✏️🔶 `core/sync/MapeoFirestore.kt`: `DocumentSnapshot.aInformeEntity()` con `id = incidenciaId = documento.id` y
  `pendienteSincronizar = false` (RF-1625); `null` si falta un campo obligatorio.
- ✏️🔶 `core/sync/SincronizacionTiempoReal.kt`: `escuchar("informes", ::guardarInformes)`, que salta `REMOVED` y las
  escrituras propias sin confirmar, como las solicitudes.
- 🆕/✏️ Pruebas: `ReglasSincronizacionTest` (sin local, con local subido, con local pendiente) e
  `InformeTecnicoDaoTest.porIncidencia` (instrumentada).

### PR-B — cálculo (D-2, D-3)
- 🆕 `feature/indicadores/domain/ReglasEquipo.kt`: `DIAS_EQUIPO`, `MINIMO_EQUIPO`, `registradaEnTresMeses(incidencia,
  ahora)` y `esEquipoCritico(incidenciasEnTresMeses)` (D-2).
- ✏️ `feature/indicadores/domain/Indicadores.kt`: `equipoAEvaluar` usa esas reglas (sin cambiar su resultado).
- 🆕 `feature/indicadores/domain/HistorialEquipo.kt`: `AtencionEquipo`, `HistorialEquipo`, `normalizarCodigoEquipo` y la
  función pura `calcularHistorial(codigo, equipo, incidencias, cierres, informes, ahora)`.
- ✏️🔶 `core/database/dao/MovimientoDao.kt`: `observarCierresDeEquipo(codigo)` (CERRADA con `JOIN` a incidencias).
- ✏️🔶 `core/database/dao/InformeTecnicoDao.kt`: `observarDeEquipo(codigo)` (con `JOIN` a incidencias).
- ✏️🔶 `core/database/dao/CatalogoDao.kt`: `observarEquipo(codigo): Flow<EquipoEntity?>`.
- 🆕 `feature/indicadores/domain/HistorialRepository.kt` (`DatosHistorial`), 🆕 `data/HistorialRepositoryImpl.kt` y
  ✏️ `di/IndicadoresModule.kt` (`@Binds`).
- 🆕 `feature/indicadores/domain/ObservarHistorialEquipo.kt`: `VerificarPermiso(CONSULTAR_HISTORIAL)`, normaliza el
  código y calcula en `@DefaultDispatcher` con el reloj del ViewModel.
- 🆕 Pruebas: `CalcularHistorialTest`, `ReglasEquipoTest`, `ObservarHistorialEquipoTest`, el doble
  `HistorialRepositoryFalso`, y casos nuevos en `MovimientoDaoTest`, `InformeTecnicoDaoTest` y `CatalogoDaoTest`.

### PR-C — pantalla y búsqueda (D-4, D-5, D-7, D-8)
- 🆕 `feature/indicadores/presentation/TextosHistorial.kt`: fecha `dd/MM/aaaa`, estado en texto, promedio en días u
  horas con su etiqueta, línea de detalle y descripción para TalkBack.
- 🆕 `feature/indicadores/presentation/HistorialEquipoViewModel.kt`: `HistorialEquipoUiState`; lee `codigoEquipo` del
  `SavedStateHandle`; texto de búsqueda y código buscado en el `SavedStateHandle` (rotación); reloj de 60 s;
  `alCambiarTexto`, `alBuscar`, `alUsarCodigoEscaneado`, `alVolverABuscar`.
- 🆕 `feature/indicadores/presentation/FilaAtencion.kt`: punto, línea y tres niveles de texto (D-8), un solo elemento
  para TalkBack con rol de botón.
- 🆕 `feature/indicadores/presentation/HistorialEquipoScreen.kt` (`HistorialEquipoRoute` + `HistorialEquipoScreen`):
  búsqueda con `BarraSuperior`, `CampoTexto` y escanear; historial con `CabeceraHero`, métricas, aviso y atenciones;
  `BackHandler` que vuelve a la búsqueda si se llegó por ella; vistas previas en claro y oscuro.
- ✏️ `feature/indicadores/presentation/IndicadoresNavigation.kt`: reemplaza `PantallaEnConstruccion`, pasa el código
  escaneado y navega al detalle y al escaneo.
- 🆕 Pruebas: `HistorialEquipoViewModelTest` y `TextosHistorialTest`.

### PR-D — acceso en Inicio
- ✏️ `feature/inicio/domain/ResumenInicio.kt`: `puedeConsultarHistorial` con `CONSULTAR_HISTORIAL`.
- ✏️ `feature/inicio/presentation/InicioScreen.kt`: «Historial» en los accesos rápidos; la fila se muestra si hay al
  menos un acceso.
- ✏️ `feature/inicio/presentation/InicioNavigation.kt`: navega a `Ruta.HistorialEquipo()`.
- ✏️ Pruebas: `ResumenInicioTest`.

### PR-E — cierre
- ✏️ `specs/016-historial-equipo/*`: estado «completa» y lo probado.
- ✏️ `MEMORY.md` (línea de la HU-16).

## 3. Diseño del cálculo

`calcularHistorial` es una función pura para probarla sin Room:
1. `atenciones`: las incidencias del equipo, de la más reciente a la más antigua por `fechaRegistro` (D-4); para cada
   una, su informe (por `incidenciaId`) y su técnico (`tecnicoAsignadoNombre`, que se conserva al cerrar).
2. `recientes`: las registradas desde `ahora - 90 días` (`registradaEnTresMeses`).
3. **Atenciones:** `recientes.size`.
4. **Piezas:** las cerradas de `recientes` cuyo informe tiene `piezasCambiadas` con texto (`isNotBlank`).
5. **Promedio:** de las cerradas de `recientes` con movimiento CERRADA (el más reciente, como RF-1511), el promedio de
   `cierre - registro` en horas; `null` si no hay ninguna. Las duraciones negativas se descartan, como en el tablero.
6. **Crítico:** `esEquipoCritico(recientes.size)`.

Es lineal con mapas por `incidenciaId`; con 500 incidencias tarda milisegundos.

## 4. Diseño de la pantalla

- **Estado:** `modoBusqueda` (llegó sin código), `texto`, `codigo` (el que se muestra; `null` = formulario),
  `sinPermiso`, `historial`. El código del argumento se muestra directo; el buscado se guarda en el `SavedStateHandle`.
- **Escaneo:** igual que el registro: `IndicadoresNavigation` lee `CLAVE_CODIGO_ESCANEADO` con `getStateFlow`, el
  `Route` llama a `alUsarCodigoEscaneado` y lo consume (RF-1605).
- **Volver:** con código de argumento, sale de la pantalla; con código buscado, vuelve al formulario con el texto.
- **Cabecera:** volver, «Código del equipo», el código (`headlineLarge`), las dos líneas del catálogo (o «Equipo fuera
  del catálogo»), la etiqueta «Equipo crítico» y las tres `TarjetaMetrica` con la misma altura.
- **Aviso:** `Surface` con `SigtecTheme.estados.advertencia` (el ámbar claro de `AvisoConexion`) e ícono de
  información, como el prototipo.
- **Separadores:** sin «·» (retema v2): la primera línea es «27/09/2026 - INC-0124» con la etiqueta de estado aparte, y
  el trabajo realizado y el técnico van en líneas separadas, como el PDF.

## 5. Riesgos
- **Informes ya subidos con otro `id`:** en el celular que lo escribió el `id` local es un UUID; en los demás será el
  `incidenciaId`. No choca porque la descarga mira primero si hay uno para esa incidencia (RF-1624) y el índice único
  es por `incidenciaId`.
- **DAO sin probar en el celular:** las pruebas instrumentadas nuevas quedan escritas; se corren cuando haya un
  celular conectado.
- **Datos de prueba:** pocos equipos con 3 o más incidencias en 90 días; para ver «Equipo crítico» puede hacer falta
  registrar algunas en `PC-CONT-014`.
