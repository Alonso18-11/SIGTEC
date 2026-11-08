# Plan: retema visual v2 (`feat(core): retema v2`)

> **Estado:** preguntas respondidas por Alonso el 2026-11-08 (ver «Decisiones»). Falta la aprobación de Nina antes de escribir código.
>
> **Fuentes:** `DESIGN.md` (secciones 2, 3 y 4), `docs/prototipos/` (01, 02, 03, 05 y 06), `CLAUDE.md` y `docs/constitution.md`, y el código de `core/theme/` y `core/ui/components/` en `main` (commit `85119d8`).
>
> **Alcance:** solo `core/theme/` y `core/ui/components/`, con sus vistas previas. Fuera de alcance: pantallas nuevas, lógica, rutas y modelos.
>
> No es una HU, así que no lleva `spec.md` con requisitos EARS (ver la pregunta P-1).
>
> **Archivos sin commitear:** `DESIGN.md`, `docs/prototipos/` y el cambio de `CLAUDE.md` todavía están solo en la carpeta de Alonso (ver P-2).

## Decisiones (respondidas por Alonso el 2026-11-08)

| Pregunta | Decisión |
|---|---|
| P-1 · Flujo SDD | Solo este `plan.md` en `specs/retema-v2/`, sin `spec.md`, aprobado por PR. |
| P-2 · Archivos sin commitear | `DESIGN.md` y `docs/prototipos/` entran en este PR (#126). El cambio de `CLAUDE.md` va en el commit de documentación del PR C. |
| P-3 · Peso 800 | Se usa Bold (700). No se agrega `inter_extrabold.ttf`. `DESIGN.md` lo anota. |
| P-4 · `hero`, `acento`, `sobreAcento` | En `SigtecTheme.marca` (`ColoresMarca.kt`), aparte de los estados. `DESIGN.md` corregido. |
| P-5 · Nombres | `FilaCargaTecnico`, `TarjetaSugerenciaIA` y `PestanasSegmentadas`. `DESIGN.md` corregido. |
| P-6 · Línea de la barra superior | Se quita. El texto de `CLAUDE.md` se corrige en el PR C. |
| P-7 · Alto de los botones | Principal, secundario y peligro a 56 dp; `BotonTexto` a 48 dp. `DESIGN.md` corregido. |
| P-8 · `EncabezadoSeccion` | Pasa a `titleMedium`, sin mayúsculas. |
| P-9 · Barra de navegación | Se crea `BarraNavegacion` (indicador `primaryContainer`) y se usa en `SigtecRaiz.kt` con los destinos de hoy. Las pestañas por perfil de `DESIGN.md` 5 quedan para la HU del Asistente o del jefe. |
| P-10 · Texto del aviso de conexión | Se cambia en este trabajo, junto con `AvisoConexionTest` y el texto de RF-76 de la spec 005. |
| P-11 · Campos blancos | Se crea `CampoTexto` compartido (fondo `surface`, forma de 16 dp, borde `outline`). Las pantallas lo adoptan en sus PR de rediseño. |
| P-12 · Borde de los campos | `outline` (4,5:1). `outlineVariant` queda para divisores y chips sin seleccionar. `DESIGN.md` corregido. |
| P-13 · Datos de las vistas previas | Los de siempre (Luis Ramírez, INC-0124…). La regla de `DESIGN.md` 7 vale para el código de producción, no para `@Preview`. `DESIGN.md` corregido. |
| P-14 · PR | Tres PR apilados (sección 3). |
| P-15 · «·» en `feature/` | Un PR aparte, `chore(core): textos sin punto medio`, después del retema. |
| Modo oscuro | Se corrigen los tres puntos que no llegan a 3:1 (sección 7): punto de estado en tono claro, tarjetas con borde y `hero` algo más claro que el fondo. La tabla final de contrastes se muestra antes de seguir con el PR B. |
| Textos de la sección 5 | Aprobados todos. Los de `core/` van en estos PR; los de las imágenes, en los PR de cada pantalla. |

## 1. Inventario

Rutas relativas a `app/src/main/java/com/softcorp/sigtec/`. Leyenda: ✏️ cambia, 🆕 se crea, ⚠️ compartido de `CLAUDE.md`.

### 1.1 `core/theme/` (todo ⚠️)

| Archivo | Cambio |
|---|---|
| ✏️ `Color.kt` | Se reemplaza la paleta Atlassian por la v2 (modo claro de la sección 3) y su versión oscura (sección 7 de este plan). Se definen también los roles que `DESIGN.md` no trae (ver 2.4). |
| ✏️ `ColoresEstado.kt` | `ColorEstado` gana un tercer color, `punto`. Los seis estados toman los valores v2. `advertencia` pasa al ámbar del aviso de conexión (ver 2.2). |
| 🆕 `ColoresMarca.kt` | `hero`, `acento` y `sobreAcento`, con su `CompositionLocal` y acceso por `SigtecTheme.marca` (ver P-4). |
| ✏️ `Theme.kt` | Provee `ColoresMarca`. El color dinámico sigue desactivado (no se usa `dynamicColorScheme`). |
| ✏️ `Type.kt` | Escala v2 (sección 3 de `DESIGN.md`) con Inter. Faltan el peso 800 y los altos de línea (ver 2.5). |
| ✏️ `Shapes.kt` | Esquinas v2, asignadas a los cinco tamaños de Material 3, más `FormaCompleta` para botones, chips y etiquetas (ver 2.7). |
| ✏️ `Dimens.kt` | Se suman `AltoBoton = 56.dp`, `AltoChip = 40.dp`, `AltoEtiqueta = 28.dp` y `Elevacion.tarjeta = 2.dp` y `Elevacion.flotante`. `Espaciado` y `AreaTactilMinima` no cambian. |
| ✏️ `TemaPreview.kt` | El muestrario pasa a mostrar la v2 (colores, estados, botones, tipografía). |

### 1.2 `core/ui/components/`: componentes que ya existen (todo ⚠️)

| Archivo | Cambio | Pantallas que lo usan (lo que puede romperse) |
|---|---|---|
| ✏️ `Botones.kt` | Los cuatro pasan a forma completa. Principal, secundario y peligro miden 56 dp; el secundario pasa de relleno gris a borde `primary`; el peligro, de relleno `errorContainer` a borde `error`. | Principal: Asignacion, Detalle, Dictar, EscanearEtiqueta, EvidenciaFotografica, ExplicacionPermiso, InformeTecnico, Login, RegistroIncidencia, SolicitudRepuesto. Secundario: AjustesSeguridad, Detalle, EscanearEtiqueta, Login, SeccionEvidencias, VistaCamara. Texto: AjustesSeguridad, FotoAmpliada, ListaIncidencias, Login, MisTareas, OfertaHuellaDialogo, RegistroIncidencia, SolicitudRepuesto. Peligro: AjustesSeguridad, Dictar, FotoAmpliada. **Riesgo:** filas con dos botones lado a lado y botones de texto dentro de avisos («Usar», «Reintentar», «Entendido») crecen de alto (ver P-7). |
| ✏️ `BarraSuperior.kt` | Pasa a `titleLarge` v2 (22 sp) y se quita la línea inferior (ver P-6). | 13 pantallas: AjustesSeguridad, Asignacion, Detalle, Dictar, EscanearEtiqueta, EstadoRepuestos, EvidenciaFotografica, FotoAmpliada, InformeTecnico, ListaIncidencias, MisTareas, RegistroIncidencia, SolicitudRepuesto. |
| ✏️ `EtiquetaEstado.kt` | Forma completa, 28 dp de alto, punto de 8 dp más texto en `labelMedium`, sin mayúsculas. Gana un parámetro `icono` para «Crítico» (ícono de alerta en lugar del punto). | Detalle, EstadoRepuestos, PantallaEnConstruccion, TemaPreview. |
| ✏️ `EtiquetaIncidencia.kt` | Usa el punto o el ícono. Textos nuevos: «Crítico, N días» y «Esperando repuesto, N días» (sección 5). | Asignacion, Detalle, TextosIncidencia, FilaIncidencia. |
| ✏️ `FilaIncidencia.kt` | Pasa de fila plana con divisor a tarjeta (`surface`, 20 dp, elevación 2 dp): código y hora arriba, título, y abajo etiqueta más avatar con nombre. El margen entre tarjetas va dentro del componente, para no tocar las listas. | ListaIncidencias, MisTareas. |
| 🆕 `Avatar.kt` (sale de `FilaIncidencia.kt`) | Se mueve a su propio archivo sin cambiar el nombre ni el paquete. Gana `esUsuarioActual`: `acento` con `sobreAcento` para el usuario actual, `secondaryContainer` para los demás. | AjustesSeguridad, Detalle, FilaIncidencia. |
| ✏️ `AvisoConexion.kt` | Sin conexión: contenedor ámbar claro con ícono de nube tachada. Textos nuevos sin «·» (sección 5). Cambian `AvisoConexionTest` y el texto de RF-76 en `specs/005-sincronizacion` (ver P-10). | ListaIncidencias, TextosSalida. |
| ✏️ `EncabezadoSeccion.kt` | Hoy es `labelSmall` en mayúsculas, al estilo Jira. `DESIGN.md` no lo nombra, pero define «Título de sección» como `titleMedium` (17 sp, 800) sin mayúsculas (ver P-8). | `EncabezadoSeccion`: Asignacion, RegistroIncidencia. `Seccion`: AjustesSeguridad, Asignacion, Detalle, RegistroIncidencia, SeccionEvidencias. |
| ✏️ `PantallaEnConstruccion.kt` (temporal) | Solo el texto «Pendiente · HU-xx» y sus vistas previas (sección 5). | Diccionario, Indicadores, Inicio (navegación). |

### 1.3 `core/ui/components/`: componentes nuevos (sección 4 de `DESIGN.md`)

Antes de crear cada uno revisé si ya existía. Hay versiones privadas dentro de pantallas, que **no se reemplazan en este PR** (eso sería rediseñar pantallas). Se marcan para los PR de cada pantalla.

| Componente | Nombre propuesto (P-5) | Ya existe algo parecido |
|---|---|---|
| 🆕 `CabeceraHero` | `CabeceraHero` | No |
| 🆕 `TarjetaMetrica` | `TarjetaMetrica` | No |
| 🆕 `AvanceIncidencia` | `AvanceIncidencia` | No |
| 🆕 `TecnicoCargaRow` | `FilaCargaTecnico` («Row» no es un sufijo que imponga Android) | `AsignacionScreen.TarjetaTecnico` y `EstadoTecnico` (privados) |
| 🆕 `SugerenciaIA` | `TarjetaSugerenciaIA` (`SugerenciaIA` ya es un modelo de `core/domain/model/TipoSugerencia.kt`) | No |
| 🆕 `SeguimientoRepuesto` | `SeguimientoRepuesto` | `EstadoRepuestosScreen.Pasos` y `Paso` (privados, HU-11) |
| 🆕 `FiltrosChips` | `FiltrosChips` | `Filtros` privados en ListaIncidencias y EstadoRepuestos (`FilterChip`) |
| 🆕 `PestañasSegmentadas` | `PestanasSegmentadas` (sin «ñ» en el nombre del archivo y de la función) | `SegmentedButton` en EvidenciaFotografica |
| 🆕 `FabExtendido` | `FabExtendido` | `FloatingActionButton` en ListaIncidencias |
| 🆕 `BarraAccionInferior` | `BarraAccionInferior` | `BarraGuardar` privada en RegistroIncidencia |
| 🆕 `BarraNavegacion` | `BarraNavegacion` | `NavigationBar` dentro de `core/navigation/SigtecRaiz.kt` (ver P-9) |
| 🆕 `LineaTiempo` | `LineaTiempo` | `DetalleIncidenciaScreen.LineaDeTiempo` (privada) |
| 🆕 `CampoTexto` (P-11, no está en `DESIGN.md` 4) | `CampoTexto` | `OutlinedTextField` directo en cada pantalla |

Cada componente nuevo tendrá vista previa en claro y oscuro, con datos de prueba genéricos. `DESIGN.md` 7 pide no copiar «Luis Ramírez» ni «INC-0124» al código, pero las pantallas actuales ya los usan en sus vistas previas (ver P-13).

### 1.4 Lo que se rompe sin tocar componentes

- **Fondo de pantalla.** `background` pasa de blanco a `#EEF4F5`. Todas las pantallas con `Scaffold` toman el fondo teal claro. Las que dibujan contenido plano con divisores (Detalle, Registro, Asignación, Ajustes) se verán distintas, sin romperse.
- **Campos de texto.** `OutlinedTextField` tiene fondo transparente; en el prototipo 01 los campos son blancos sobre el fondo teal. No hay un componente de campo compartido (ver P-11).
- **Formas.** `MaterialTheme.shapes.small` (12 usos) y `medium` (12 usos) pasan de 4 y 6 dp a 16 y 20 dp. Archivos afectados:
  - `small`: AvisoConexion, Botones, EscanearEtiqueta, EvidenciaFotografica, ListaIncidencias, MiniaturaEvidencia, SolicitudRepuesto;
  - `medium`: AjustesSeguridad, Asignacion, Detalle, Dictar, EstadoRepuestos, EvidenciaFotografica, RegistroIncidencia, SolicitudRepuesto;
  - `large`: Login.
- **Tipografía.** Cambian los tamaños de toda la app: 47 usos de `bodyMedium`, 30 de `bodySmall` y 10 de `labelMedium`. Textos que hoy caben en una línea pueden pasar a dos.
- **Ajuste mínimo permitido:** si algo queda cortado o encimado por el cambio de tema, se corrige en la pantalla con el cambio más pequeño posible y se anota en el PR.

## 2. Diferencias entre `DESIGN.md` y el código que la sección 2 no cubre

1. **`ColorEstado` tiene dos colores y la v2 pide tres.** Hoy son `contenido` y `contenedor`; la v2 suma el punto. Se agrega `punto` a la clase, y como los usos existentes nombran los campos, no cambian.
2. **`advertencia` no está en `DESIGN.md`.** Es un estado del código (5 usos: sin sensor de huella, aviso de conexión). Los colores del aviso de conexión de la sección 4 (`#FFEFD0` y `#6E3D00`) no están en ninguna tabla de tokens. Propuesta: `advertencia` toma esos dos colores.
3. **`hero`, `acento` y `sobreAcento` no son estados.** `DESIGN.md` dice que «se exponen como `SigtecTheme.estados`», pero meterlos ahí mezcla marca con estado de incidencia (ver P-4).
4. **Roles de Material 3 que `DESIGN.md` no define.** En claro faltan `onSecondary`, `onSecondaryContainer`, `onTertiary`, `onError`, `onBackground`, `inverseSurface`, `inverseOnSurface`, `inversePrimary`, los cinco `surfaceContainer*` y `scrim`. En oscuro solo hay 8 roles; faltan `secondary*`, `tertiary*`, `error*`, `outline*`, `surfaceVariant` y los contenedores. Propuesta: derivarlos del mismo tono y mostrarlos en una tabla en el primer commit para aprobarlos antes de seguir.
5. **Tipografía.**
   - `DESIGN.md` usa peso 800 en títulos, pero `res/font` solo tiene Inter 400, 500, 600 y 700.
   - No define altos de línea ni los estilos `display*`, `headlineSmall`, `labelLarge` y `labelSmall`, que la app usa (`labelSmall` en 6 lugares).
   - Ver P-3.
6. **`EncabezadoSeccion` no aparece en `DESIGN.md`.** Ver P-8.
7. **Formas.** `DESIGN.md` da las medidas pero no a qué tamaño de Material 3 corresponde cada una. Propuesta, pensada para que los componentes de Material 3 tomen la forma correcta solos (`OutlinedTextField` usa `extraSmall`, `Card` usa `medium`, los diálogos `extraLarge`):

   | Tamaño | Hoy | v2 | Lo usan |
   |---|---|---|---|
   | `extraSmall` | 3 dp | 16 dp | campos de texto, menús |
   | `small` | 4 dp | 16 dp | paneles y avisos pequeños |
   | `medium` | 6 dp | 20 dp | tarjetas de lista |
   | `large` | 8 dp | 24 dp | tarjetas destacadas |
   | `extraLarge` | 8 dp | 32 dp | cabecera oscura, hoja inferior, diálogos |
   | `FormaCompleta` (nueva) | no existe | 50 % | botones, chips, etiquetas, FAB |

8. **Botón de texto a 56 dp.** La tabla pide 56 dp para los cuatro botones, pero `BotonTexto` se usa en línea, dentro de avisos y filas (ver P-7).
9. **Chips de filtro de 40 dp contra la regla de 48 dp.** Se dibujan de 40 dp dentro de un área tocable de 48 dp (`minimumInteractiveComponentSize`, que Material 3 ya aplica), así que se cumplen las dos cosas.
10. **Bordes de campo con `outlineVariant`.** `#B9C9CD` sobre blanco da 1,71:1, por debajo del 3:1 que WCAG pide para el borde de un control (ver P-12).
11. **Elevación en modo oscuro.** Las sombras casi no se ven sobre fondos oscuros, y la tarjeta (`#172327`) contra el fondo (`#0F1A1D`) da 1,10:1. Sin borde ni diferencia tonal, las tarjetas no se distinguen (ver sección 7).
12. **Componentes con nombre en inglés o que chocan con el dominio.** Ver 1.3 y P-5.

## 3. Orden de trabajo

Tres PR apilados (P-14), con un commit por paso y su push. En cada commit corre `./gradlew assembleDebug testDebugUnitTest lintDebug`.

| PR | Rama | Título | Base | Commits |
|---|---|---|---|---|
| A | `feat/retema-v2-tema` | `feat(core): retema v2, paleta, formas y tipografía` | `main` | 1 a 4 |
| B | `feat/retema-v2-componentes` | `feat(core): retema v2, componentes existentes` | A | 5 a 10 |
| C | `feat/retema-v2-nuevos` | `feat(core): retema v2, componentes nuevos` | B | 11 a 17 |

| # | Commit | Archivos |
|---|---|---|
| 1 | `feat(core): paleta v2 en modo claro, marca y estados con punto` | `Color.kt`, `ColoresEstado.kt`, `ColoresMarca.kt`, `Theme.kt` |
| 2 | `feat(core): modo oscuro v2 con contrastes revisados` | `Color.kt`, `ColoresEstado.kt`, `ColoresMarca.kt` |
| 3 | `feat(core): formas, alturas y elevación v2` | `Shapes.kt`, `Dimens.kt` |
| 4 | `feat(core): tipografía v2 con Inter` | `Type.kt` (Bold 700 donde `DESIGN.md` decía 800, P-3) |
| 5 | `feat(core): botones v2` | `Botones.kt` |
| 6 | `feat(core): barra superior y encabezado de sección v2` | `BarraSuperior.kt`, `EncabezadoSeccion.kt` |
| 7 | `feat(core): etiquetas de estado con punto e ícono` | `EtiquetaEstado.kt`, `EtiquetaIncidencia.kt` |
| 8 | `refactor(core): Avatar en su propio archivo, con el usuario actual en acento` | `Avatar.kt`, `FilaIncidencia.kt` |
| 9 | `feat(core): FilaIncidencia como tarjeta` | `FilaIncidencia.kt` |
| 10 | `feat(core): aviso de conexión v2` | `AvisoConexion.kt`, `AvisoConexionTest.kt`, `specs/005-sincronizacion/spec.md` (texto de RF-76) |
| 11 | `feat(core): CabeceraHero y TarjetaMetrica` | dos archivos nuevos |
| 12 | `feat(core): AvanceIncidencia, SeguimientoRepuesto y LineaTiempo` | tres archivos nuevos |
| 13 | `feat(core): FiltrosChips, PestanasSegmentadas y CampoTexto` | tres archivos nuevos |
| 14 | `feat(core): FabExtendido, BarraAccionInferior y BarraNavegacion` | tres archivos nuevos, y `SigtecRaiz.kt` ⚠️ usa `BarraNavegacion` (P-9) |
| 15 | `feat(core): TarjetaSugerenciaIA y FilaCargaTecnico` | dos archivos nuevos |
| 16 | `fix(core): ajustes mínimos de pantallas por el cambio de tema` | solo lo que se rompa (sección 1.4), uno por uno en el PR |
| 17 | `docs(core): muestrario v2, CLAUDE.md y MEMORY.md` | `TemaPreview.kt`, `CLAUDE.md` (texto de «Diseño» y «Barra superior»), `MEMORY.md` |

El PR A cambia toda la app de golpe (fondo, formas, tipografía); cada PR se prueba en el celular antes de pasar al siguiente.

## 4. Verificación

1. **Compilación y pruebas.** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde en cada commit, con lint igual que la línea base (RedundantLabel 1, AndroidGradlePluginVersion 1, GradleDependency 18, NewerVersionAvailable 10, UnusedResources 7). Si se agrega la fuente ExtraBold, `UnusedResources` no debe subir.
2. **Búsquedas automáticas** sobre el diff:
   - sin hexadecimales ni `fontSize` fuera de `core/theme/`;
   - sin «·» en los archivos tocados;
   - sin `dynamicColor`;
   - todo `clickable`, `IconButton` y chip nuevo con `AreaTactilMinima` o `minimumInteractiveComponentSize`.
3. **Contraste.** Un script en el scratchpad (fuera del repo) calcula WCAG para cada par de texto y fondo y para cada borde, en claro y oscuro. La tabla resultante va en el PR.
4. **Vistas previas.** Cada componente tiene `@Preview` en claro y oscuro. Las reviso en Android Studio junto a la imagen del prototipo (01, 02, 03, 05 y 06), comparando forma, color, medidas y jerarquía, e ignorando los textos que cambian las reglas de redacción. No se agrega una librería de capturas de vistas previas (constitución 1).
5. **En el celular.** Instalar en el Xiaomi y recorrer, en claro y oscuro (`adb shell cmd uimode night yes|no`), las pantallas que usan los componentes tocados: Login, Lista, Detalle, Mis tareas, Registro, Asignación, Estado de repuestos y Ajustes. Así se buscan textos cortados, botones encimados y barras con doble margen.
   - La navegación por `adb shell input` necesita «Depuración USB (configuración de seguridad)» en el Xiaomi. Si Alonso no la activa, él navega y yo tomo las capturas.
   - Las capturas van al scratchpad, nunca al repo, y en el PR se adjuntan las del antes y el después.

## 5. Textos que cambian por las reglas de redacción

**No se aplican hasta que los apruebes.** Las imágenes no son código, así que de ellas solo cuentan los textos que algún componente de este PR va a mostrar o usar en sus vistas previas. El resto queda listado para los PR de cada pantalla.

### 5.1 Textos que hoy están en `core/` (este PR los cambia)

| Dónde | Hoy | Propuesta |
|---|---|---|
| `EtiquetaIncidencia` | «Caso crítico · N días» | «Crítico, N días» |
| `EtiquetaIncidencia` | «Esperando repuesto · N días» | «Esperando repuesto, N días» |
| `FilaIncidencia` | «INC-0124 · PC-CONT-014» | «INC-0124 - PC-CONT-014» |
| `AvisoConexion` (sin conexión) | «Sin conexión · mostrando lo descargado a las 09:40 · 2 cambios por enviar» | «Sin conexión. Datos de las 09:40, 2 cambios por enviar.» (sin descarga: «Sin conexión»; sin pendientes: «Sin conexión. Datos de las 09:40.») |
| `AvisoConexion` (enviando, rechazo) | «Enviando 1 cambio…», «2 cambios no se pudieron enviar», «Entendido» | Sin cambios |
| `PantallaEnConstruccion` | «Pendiente · HU-13» | «Pendiente (HU-13)» |
| Vista previa de `PantallaEnConstruccion` | «Hola, Luis · Técnico» | «Hola, Luis» |
| `TemaPreview` | «Caso crítico · 6 días» | «Crítico, 6 días» |
| Nombres de `@Preview` en `core/` | «Filas · claro», «Sin conexión · oscuro», etc. | «Filas, claro», «Sin conexión, oscuro» |
| Comentarios en `core/` con «·» | (varios) | Se reescriben con coma o guion |

### 5.2 Textos de `DESIGN.md` que usan los componentes nuevos

| Componente | Texto de `DESIGN.md` | Propuesta |
|---|---|---|
| `FilaCargaTecnico` | «Alcanzó su límite de atención» | Sin cambios (aviso real, RF-06) |
| `FilaCargaTecnico` | «Recomendado» | Sin cambios |
| `TarjetaSugerenciaIA` | «IA», «Usar», «Descartar», «Pedir cambios» | Sin cambios |
| `AvanceIncidencia` | «Asignada», «En atención», «Repuesto», «Cierre» | Sin cambios |
| `SeguimientoRepuesto` | «Solicitado», «Despachado», «Entregado» | Sin cambios |
| `FabExtendido` | «Nueva», «Registrar falla» | Sin cambios |
| `AvisoConexion` | «la hora de la última sincronización» | Ver la fila de `AvisoConexion` en 5.1 |

### 5.3 Textos de las imágenes (para las vistas previas y para los PR de cada pantalla)

| Imagen | Hoy | Propuesta |
|---|---|---|
| 01 Login | «Cada incidencia, bajo control. Desde tu bolsillo.» | Quitar (eslogan) |
| 01 Login | «Hola de nuevo» | «Iniciar sesión» |
| 01 Login | Marcador «Tu contraseña» | Quitar (la etiqueta ya dice «Contraseña») |
| 01 Login | «Acceso exclusivo para el personal de Soporte y Sistemas · Soft Corporation» | Quitar |
| 02 Inicio | «INC-0124 · PC-CONT-014» | «INC-0124 - PC-CONT-014» |
| 02 Inicio | «Contabilidad · María Quispe» | «María Quispe, Contabilidad» |
| 02 Inicio | «Paso 2 de 4 · Diagnóstico» | «Paso 2 de 4: diagnóstico» |
| 02 Inicio | «INC-0116 · despachado por Logística» | «INC-0116, despachado por Logística» |
| 02 Inicio | «Accesos rápidos» | Quitar (los íconos ya tienen nombre) |
| 02 Inicio | «Buenos días, Luis», «Técnico de soporte», «hace 2 h 10 min», «Continuar» | Sin cambios |
| 03 Incidencias | «Todas · 5», «Sin asignar · 1», «En atención · 1» | «Todas (5)», «Sin asignar (1)», «En atención (1)» |
| 03 Incidencias | «5 en total» | Quitar (repite «Todas (5)») |
| 03 Incidencias | «INC-0127 · IMP-RRHH-002» y las demás | «INC-0127 - IMP-RRHH-002» |
| 03 Incidencias | «Crítico · 6 días» | «Crítico, 6 días» |
| 03 Incidencias | «Hoy 10:42», «Sin técnico», «Nueva», buscador «Código de equipo, usuario o falla» | Sin cambios |
| 05 Detalle | «Contabilidad · Anexo 2314» | «Contabilidad, anexo 2314» |
| 05 Detalle | «Ver historial →» | «Ver historial» |
| 05 Detalle | «Qué probar primero», «IA», «Agregar foto», «Repuesto», «Cerrar con informe» | Sin cambios |
| 06 Mis tareas | «Sin conexión · mostrando lo descargado a las 09:40. Se sincronizará solo.» | «Sin conexión. Datos de las 09:40.» |
| 06 Mis tareas | «Repuestos · 2» | «Repuestos (2)» |
| 06 Mis tareas | «Logística · Juan Paredes», «Sistemas · Diana Rojas» | «Juan Paredes, Logística», «Diana Rojas, Sistemas» |
| 06 Mis tareas | «Esperando repuesto · 4 días» | «Esperando repuesto, 4 días» |
| 06 Mis tareas | «Asignada · sin iniciar» | «Asignada» (el botón «Iniciar atención» ya lo dice) |
| 06 Mis tareas | «Continuar →» | «Continuar» |
| 06 Mis tareas | «3 pendientes», «Desde 23/09», «Llega mañana», «Iniciar atención» | Sin cambios |

### 5.4 Fuera de este PR

En las pantallas de `feature/` hay 139 líneas con «·» repartidas en 33 archivos, y 16 en las pruebas. Las reglas valen para toda la app, pero tocarlas aquí sería rediseñar pantallas (ver P-15).

## 6. Riesgos y preguntas abiertas

**Contradicciones entre `DESIGN.md`, `CLAUDE.md` y el código.** No las decido yo.

- **P-6 · Línea inferior de la barra superior.** `CLAUDE.md` dice que `BarraSuperior` «ya trae `WindowInsets(0)` y la línea inferior»; `DESIGN.md` 4 la quita. ¿Se quita, y se corrige ese texto de `CLAUDE.md` en el commit 17?
- **P-4 · Dónde viven `hero`, `acento` y `sobreAcento`.** `DESIGN.md` 3 dice «se exponen como `SigtecTheme.estados`». Propongo `SigtecTheme.marca`, aparte, porque no son estados de incidencia. ¿O van dentro de `estados`, como dice el documento?
- **P-8 · `EncabezadoSeccion`.** Hoy va en mayúsculas pequeñas (estilo Jira, que `DESIGN.md` 2 reemplaza). ¿Pasa a `titleMedium` sin mayúsculas, como el «Título de sección» de la tabla de tipografía? Cambia la apariencia de Ajustes, Asignación, Detalle y Registro.
- **P-9 · `BarraNavegacion` y el menú por perfil.** `DESIGN.md` 5 define las pestañas del técnico (Inicio, Incidencias, Mis tareas, Asistente) y del jefe (Incidencias, Asignar, Indicadores, Historial). El código las decide por permiso en `DestinoPrincipal`: Inicio, Incidencias, Mis tareas, Diccionario e Indicadores. Asistente, Asignar e Historial no son destinos hoy, y cambiarlo toca rutas (fuera de alcance). La barra vive en `SigtecRaiz.kt` (compartido). Propuesta: crear `BarraNavegacion` con el indicador `primaryContainer`, usarla en `SigtecRaiz` con los destinos de hoy, y dejar el cambio de pestañas para la HU del Asistente o del jefe. ¿O solo se crea el componente y no se toca `SigtecRaiz`?

**Decisiones de alcance o de valores que `DESIGN.md` no fija:**

- **P-1 · Flujo SDD.** El retema no es una HU. Propongo este `plan.md` en `specs/retema-v2/` (sin número), sin `spec.md` EARS, aprobado por PR como las specs. ¿Te parece, o prefieres otro nombre o carpeta?
- **P-2 · Archivos sin commitear.** `DESIGN.md`, `docs/prototipos/` (18 PNG, unos 5 MB) y el cambio de `CLAUDE.md` están solo en tu carpeta. ¿Los agrego a este PR del plan, para que Nina los vea, o los subes tú en otro PR?
- **P-3 · Peso 800.** Opciones:
  - agregar `inter_extrabold.ttf` (misma familia y licencia OFL; es un recurso, no una dependencia de Gradle);
  - usar Bold (700) donde `DESIGN.md` dice 800.

  Para los altos de línea propongo 1,3 veces el tamaño en títulos y 1,45 en cuerpo. Los estilos que la tabla no nombra quedan proporcionales.
- **P-5 · Nombres.** `FilaCargaTecnico` en lugar de `TecnicoCargaRow`, `TarjetaSugerenciaIA` en lugar de `SugerenciaIA` (choca con el modelo de dominio) y `PestanasSegmentadas` en lugar de `PestañasSegmentadas`. ¿De acuerdo? Si sí, se corrige la tabla de `DESIGN.md` 4.
- **P-7 · Alto de `BotonTexto`.** Propongo 56 dp para principal, secundario y peligro, y 48 dp (`AreaTactilMinima`) para el de texto, que se usa en línea dentro de avisos y filas.
- **P-10 · Texto del aviso de conexión.** Cambiarlo obliga a actualizar `AvisoConexionTest` y el texto de RF-76 en `specs/005-sincronizacion/spec.md` en el mismo PR (constitución 2). ¿Lo hago aquí?
- **P-11 · Campos blancos.** El prototipo 01 muestra campos blancos con borde. ¿Creo un `CampoTexto` compartido (no está en la tabla de la sección 4), o se deja `OutlinedTextField` transparente y cada pantalla lo ajusta en su PR?
- **P-12 · Borde de los campos.** `outlineVariant` sobre blanco da 1,71:1 y WCAG pide 3:1 para el borde de un control. Propongo `outline` (4,5:1) para el borde de los campos y `outlineVariant` solo para divisores y chips sin seleccionar. ¿Aceptas, o se oscurece `outlineVariant`?
- **P-13 · Datos de prueba en las vistas previas.** `DESIGN.md` 7 dice que «Luis Ramírez» o «INC-0124» no se escriben en el código, pero las vistas previas actuales los usan. ¿Las vistas previas nuevas usan nombres genéricos («Técnico de prueba», «INC-0001»), o se mantienen los de siempre por coherencia?
- **P-14 · Uno o tres PR.** Pediste uno. Son unos 17 commits que cambian toda la app; ¿prefieren tres PR apilados (ver la sección 3)?
- **P-15 · «·» en las pantallas.** ¿Un PR aparte (`chore: textos sin punto medio`) para las 139 líneas de `feature/`, o se corrige pantalla por pantalla en sus PR de rediseño?

**Riesgos:**

- **R-1 · Toda la app cambia de golpe.** El fondo teal y las esquinas grandes sobre pantallas planas pueden verse mal mientras no se rediseñe cada una. Mitigación: los ajustes mínimos del commit 16, y capturas del antes y el después en el PR.
- **R-2 · Pruebas que dependen de textos.** `AvisoConexionTest` seguro cambia. Hay otras con «·» que no son de `core/` (`TextosSalidaTest`, `MensajesDictadoTest`, `FiltrarIncidenciasTest` y `ListaIncidenciasViewModelTest`); no cambian en este PR salvo que se apruebe P-15.
- **R-3 · Componentes duplicados.** Los nuevos conviven con las versiones privadas de las pantallas (sección 1.3) hasta que cada pantalla se rediseñe.
- **R-4 · Las tarjetas se distinguen solo por la sombra.** Blanco sobre `#EEF4F5` da 1,11:1. En claro la sombra de 2 dp alcanza; en oscuro no (sección 7).

## 7. Modo oscuro: contrastes de la propuesta de `DESIGN.md`

Calculados con la fórmula de WCAG 2.1. Texto normal pide 4,5:1; bordes, puntos e íconos sin texto, 3:1.

**Pasan (texto):**

| Par | Contraste |
|---|---|
| `onPrimary` `#00323E` sobre `primary` `#5BC8E3` | 7,09:1 |
| `#B3EBFF` sobre `primaryContainer` `#004D5F` | 7,29:1 |
| `onSurface` `#E3ECEF` sobre `surface` `#172327` | 13,41:1 |
| `onSurface` sobre `background` `#0F1A1D` | 14,77:1 |
| `onSurfaceVariant` `#A9BBC0` sobre `surface` | 8,08:1 |
| `onSurfaceVariant` sobre `background` | 8,90:1 |
| `primary` como texto (enlaces) sobre `surface` | 8,28:1 |
| Blanco sobre `hero` `#00212A` | 16,79:1 |
| `sobreAcento` sobre `acento` | 9,59:1 |

**Estados (texto).** Con «fondo al 25 % de su punto» sobre `surface`, y tomando como «texto claro del mismo tono» el fondo claro de cada estado (por ejemplo, `#FFE9CC` para «Sin asignar»), los seis pasan, entre 10,9:1 y 11,4:1. `DESIGN.md` no dice qué color de texto usar; esa es mi propuesta.

**No llegan al mínimo:**

| Par | Contraste | Mínimo | Propuesta |
|---|---|---|---|
| Punto de cada estado sobre su fondo oscuro (los seis) | 2,0 a 2,2:1 | 3:1 | En oscuro, el punto usa un tono claro (el mismo del texto o uno intermedio) |
| Tarjeta `surface` `#172327` sobre `background` `#0F1A1D` | 1,10:1 | 3:1 (para distinguir la tarjeta) | Borde `outlineVariant` oscuro en las tarjetas o `surface` más claro; la sombra no se ve en oscuro |
| Cabecera `hero` `#00212A` sobre `background` | 1,05:1 | 3:1 (borde de la cabecera) | `hero` un poco más claro que el fondo, o se acepta que la cabecera se funda con el fondo |
| Roles sin valor en oscuro (`secondary*`, `tertiary*`, `error*`, `outline*`, `surfaceVariant` y contenedores) | sin calcular | | Los derivo y los mido en el commit 2, antes de usarlos |

**Para comparar, en claro:**
- Todo el texto pasa: el mínimo es `primary` sobre `background`, con 5,83:1.
- Los puntos de estado pasan (5,0 a 5,5:1).
- Fallan tres casos que no son texto:
  - `outlineVariant` como borde de campo: 1,71:1 sobre blanco y 1,54:1 sobre el fondo (P-12);
  - el FAB ámbar sobre el fondo: 1,58:1, aceptable porque lleva texto con 9,59:1;
  - la tarjeta blanca sobre el fondo: 1,11:1 (R-4).
