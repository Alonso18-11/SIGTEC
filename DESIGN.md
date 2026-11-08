# DESIGN.md — SIGTEC v2

Guía visual y de UX de la interfaz. Complementa a `CLAUDE.md`: allá están la arquitectura y las reglas de código; aquí, cómo se ve y cómo se comporta cada pantalla.

**Fuentes:**
- Imágenes de las 18 pantallas en `docs/prototipos/` (`01-login.png` … `18-ajustes.png`). Cada pantalla se construye contra su imagen.
- Prototipo navegable: «SIGTEC · Prototipos v2» (lienzo de Claude Design).
- Este archivo manda en valores (colores, medidas, textos de ejemplo). Si una imagen y este archivo difieren, pregunta.

**Estado:** el retema v2 (sección 2) está en el código desde el 2026-11-08: tema en #127, componentes existentes en #128 y componentes nuevos en el PR C (`specs/retema-v2/plan.md`). Las pantallas adoptan los componentes nuevos en el PR de su rediseño.

---

## 1. Principios de UX

1. **El técnico trabaja de pie, con una mano.** Todo elemento tocable mide 48 dp o más. Las acciones principales van abajo, al alcance del pulgar.
2. **Una acción principal por pantalla.** Un solo botón relleno; lo demás es secundario o de texto.
3. **Menos tecleo.** Escanear la etiqueta, dictar la descripción y autocompletar el usuario antes que un campo vacío.
4. **El estado se ve de lejos.** Cada estado tiene color y siempre lleva texto o ícono; nunca solo color.
5. **La IA sugiere, nunca decide.** Todo lo generado va en contenedor propio, con ícono de destello y etiqueta «IA», y requiere aceptar, corregir o descartar.
6. **Sin conexión no es un error.** Se avisa con un banner y la app sigue funcionando con lo guardado.

## 2. Qué cambia respecto al código actual

Es un retema: toca archivos compartidos (`core/theme/*`, `core/ui/components/*`). Va en **su propio PR** (`feat(core): retema v2`), con plan previo y la aprobación de la otra persona del equipo, antes de cualquier pantalla nueva.

| Tema | Hoy (`CLAUDE.md`) | v2 |
|---|---|---|
| Estilo general | Superficies planas, bordes finos, estilo Atlassian | Tarjetas blancas sobre fondo teal muy claro, sombra suave, cabeceras oscuras |
| Esquinas | 3 a 8 dp | Tarjetas 20 dp, cabeceras y hoja inferior 32 dp, chips y botones en forma completa |
| Etiquetas de estado | Mayúsculas | Texto normal, con punto de color (ícono en «Crítico») |
| Paleta | Estilo Atlassian | Teal de marca más un ámbar de acento (sección 3) |
| Tipografía | Inter | Se mantiene Inter (ya está en `res/font`). Los prototipos usan Plus Jakarta Sans con los mismos pesos y tamaños |
| Color dinámico | Desactivado | Sigue desactivado |

Lo que **no** cambia: roles de `MaterialTheme.colorScheme`, `SigtecTheme.estados.*`, `Espaciado.*`, `AreaTactilMinima`, botones de `Botones.kt`, previews en claro y oscuro, cero hexadecimales fuera de `core/theme/`.

## 3. Tokens

Los hexadecimales viven solo en `core/theme/`. Fuera de ahí se usan roles.

### Color (modo claro)

| Rol | Hex | Uso |
|---|---|---|
| `primary` | `#00677E` | Botón principal, elemento seleccionado |
| `onPrimary` | `#FFFFFF` | Texto sobre primary |
| `primaryContainer` | `#B3EBFF` | Pastilla activa del menú inferior |
| `onPrimaryContainer` | `#001F28` | Texto sobre ese contenedor |
| `secondary` | `#4B6269` | Íconos de apoyo |
| `secondaryContainer` | `#CEE7EF` | Avatares de otros técnicos, chips de componente |
| `tertiary` | `#3F4670` | Todo lo generado por IA |
| `tertiaryContainer` | `#E9EAF6` | Tarjetas y chips de sugerencia IA |
| `onTertiaryContainer` | `#1F2447` | Texto sobre sugerencias IA |
| `error` | `#BA1A1A` | Errores y bloqueos |
| `errorContainer` | `#FFDAD6` | Fondo de avisos de error y chip crítico |
| `onErrorContainer` | `#8C1212` | Texto sobre ese contenedor |
| `background` | `#EEF4F5` | Fondo de pantalla |
| `surface` | `#FFFFFF` | Tarjetas |
| `surfaceVariant` | `#E3ECEF` | Pistas de barras, pestañas segmentadas |
| `onSurface` | `#0F1E22` | Texto principal |
| `onSurfaceVariant` | `#4A5B60` | Texto secundario |
| `outline` | `#70787C` | Bordes de campos de texto y bordes con énfasis |
| `outlineVariant` | `#B9C9CD` | Divisores y chips de filtro sin seleccionar (sobre blanco da 1,71:1: no sirve como borde de un control) |

Colores de marca (se exponen como `SigtecTheme.marca`, con el mismo patrón que `SigtecTheme.estados`):

| Token | Hex | Uso |
|---|---|---|
| `hero` | `#002E3A` | Fondo de cabeceras oscuras (Inicio, Detalle, Indicadores, Historial, Login, Dictado) |
| `acento` | `#FFB547` | Solo: botón flotante «Nueva», micrófono, linterna, valor máximo del gráfico, avatar del usuario actual |
| `sobreAcento` | `#2A1A00` | Texto e íconos sobre el acento |

**Regla del ámbar:** nunca para texto sobre fondo claro. Siempre con `sobreAcento` encima.

### Estados de incidencia

| Estado | Fondo | Texto | Punto |
|---|---|---|---|
| Sin asignar | `#FFE9CC` | `#6E3D00` | `#8B5000` |
| Asignada | `#E3ECEF` | `#33474D` | `#4B6269` |
| En atención | `#D5F1F8` | `#00536A` | `#00677E` |
| Esperando repuesto | `#EFE6FA` | `#4F3585` | `#6B4FA0` |
| Solucionado | `#DDF3DF` | `#17521F` | `#206C2F` |
| Crítico | `#FFDAD6` | `#8C1212` | `#BA1A1A` (con ícono de alerta) |

«Esperando repuesto» pasó de ocre a violeta porque se confundía con «Sin asignar».

### Modo oscuro (propuesta, sin validar)

No se diseñó en el lienzo. Derívalo del mismo esquema y comprueba contraste 4.5:1 antes de fusionar.

| Rol | Hex sugerido |
|---|---|
| `background` | `#0F1A1D` |
| `surface` | `#172327` |
| `hero` | `#00212A` |
| `primary` | `#5BC8E3` con `onPrimary` `#00323E` |
| `primaryContainer` | `#004D5F` con texto `#B3EBFF` |
| `onSurface` / `onSurfaceVariant` | `#E3ECEF` / `#A9BBC0` |
| `acento` | Se mantiene `#FFB547` |

Los estados se oscurecen: fondo al 25 % de su punto, texto claro del mismo tono.

### Tipografía (Inter)

Donde la tabla dice 800, la app usa Bold (700): `res/font` trae Inter de 400 a 700.

| Estilo | Tamaño | Peso | Uso |
|---|---|---|---|
| `headlineLarge` | 28 sp | 800 | Título de pantalla de lista |
| `headlineMedium` | 24 sp | 800 | Título dentro de cabecera oscura |
| `titleLarge` | 22 sp | 800 | Barra superior de formularios |
| `titleMedium` | 17 sp | 800 | Título de sección |
| `titleSmall` | 15.5 sp | 700–800 | Título de tarjeta |
| `bodyLarge` | 16 sp | 400 | Texto de campos |
| `bodyMedium` | 14.5 sp | 400–500 | Texto de tarjeta |
| `bodySmall` | 13 sp | 500 | Datos secundarios |
| `labelMedium` | 12.5 sp | 700 | Chips, etiquetas, códigos de incidencia |

### Formas, espaciado y elevación

- Formas: tarjetas de lista 20 dp, tarjetas destacadas 24 dp, cabecera oscura (esquinas inferiores) y hoja inferior (esquinas superiores) 32 dp, campos 16 dp, chips y botones en forma completa.
- Botones principales de 56 dp de alto. Chips de filtro de 40 dp. Controles tocables nunca menos de 48 dp.
- Espaciado: `Espaciado.*` (4/8/12/16/24/32). Los prototipos usan a veces 10, 14, 18 o 20: redondea al valor más cercano de la escala.
- Elevación: tarjetas 2 dp. Botón flotante y barra inferior de acción, sombra más marcada. Nada más lleva sombra.

## 4. Componentes

Antes de crear uno, busca si ya existe en `core/ui/components/`. Los existentes se **modifican**; los nuevos van en la misma carpeta.

| Componente | Estado | Cambio / especificación |
|---|---|---|
| `BotonPrincipal`, `BotonSecundario`, `BotonTexto`, `BotonPeligro` | Existe | Alto 56 dp (`BotonTexto`, 48 dp, porque va en línea), forma completa. El principal es `primary`; el secundario, borde `primary`; el peligro, borde `error` |
| `BarraSuperior(titulo, alVolver)` | Existe | Se mantiene para formularios y listas secundarias. Sin línea inferior en v2 |
| `EtiquetaEstado` / `EtiquetaIncidencia` | Existe | Forma completa, alto 28 dp, punto de 8 dp + texto en `labelMedium`. «Crítico» con ícono de alerta y los días |
| `FilaIncidencia` | Existe | Pasa a tarjeta: código y hora arriba, título, y abajo etiqueta + avatar con nombre |
| `Avatar` | Existe | Iniciales. Usuario actual en `acento`; los demás en `secondaryContainer` |
| `AvisoConexion` | Existe | Contenedor ámbar claro (`#FFEFD0`, texto `#6E3D00`) con ícono de nube tachada y la hora de la última sincronización |
| `CabeceraHero` | Nuevo | Fondo `hero`, esquinas inferiores 32 dp, círculos decorativos con `Canvas` (solo decoración, `clearAndSetSemantics {}`) |
| `TarjetaMetrica` | Nuevo | Número grande + etiqueta. Variantes: neutra, acento (En atención), crítica |
| `AvanceIncidencia` | Nuevo | 4 pasos: Asignada, En atención, Repuesto, Cierre. Hecho = `primary` con check; actual = `acento`; pendiente = punteado |
| `FilaCargaTecnico` | Nuevo | Un segmento por cada unidad del límite de atención. Si `activas >= limite`: fila bloqueada, en rojo, con candado y el texto «Alcanzó su límite de atención» (RF-06). Etiqueta «Recomendado» en el de menor carga |
| `TarjetaSugerenciaIA` | Nuevo | Contenedor `tertiaryContainer`, ícono de destello, etiqueta «IA». Acciones: «Usar» y «Descartar» o «Pedir cambios» |
| `SeguimientoRepuesto` | Nuevo | Tres puntos con línea: Solicitado, Despachado, Entregado. Violeta mientras llega; verde al entregar |
| `FiltrosChips` | Nuevo | Chips de 40 dp; el activo, relleno `primary` |
| `PestanasSegmentadas` | Nuevo | Pista `surfaceVariant`, opción activa blanca con sombra |
| `FabExtendido` | Nuevo | Fondo `acento`, ícono + texto («Nueva», «Registrar falla») |
| `BarraAccionInferior` | Nuevo | Hoja blanca con esquinas superiores de 32 dp, con la acción principal. Pegada abajo |
| `BarraNavegacion` | Nuevo | `NavigationBar` de Material 3, indicador `primaryContainer`. Pestañas por perfil (sección 5) |
| `LineaTiempo` | Nuevo | Punto + línea vertical. Punto verde = solucionado; ámbar con halo = en curso |
| `CampoTexto` | Nuevo | Campo con etiqueta visible, fondo `surface`, esquinas de 16 dp y borde `outline` |

## 5. Pantallas

Cada fila apunta a su imagen en `docs/prototipos/`. El perfil indica quién la ve (`perfil.puede(...)`, nunca `if (perfil == ...)`).

| N.º | Pantalla | Perfil | Componentes clave | Notas |
|---|---|---|---|---|
| 01 | Login | Todos | Campos con ícono, `BotonPrincipal`, botón de huella | Cabecera oscura con logo. «Ingresar con huella» solo si está activada |
| 02 | Inicio del técnico | Técnico | `CabeceraHero`, 3 `TarjetaMetrica`, tarjeta «Atendiendo ahora», accesos rápidos, `BarraNavegacion` | El avatar abre Ajustes. «Fallas» abre el diccionario |
| 03 | Incidencias | Jefe, técnico, personal de sistemas | Buscador, `FiltrosChips`, `FilaIncidencia`, `FabExtendido` | Lista de más reciente a más antigua |
| 04 | Nueva incidencia | Jefe, técnico, personal de sistemas | Campo de código con botón de cámara, usuario autocompletado, texto con micrófono, `SugerenciaIA` | Fecha y hora automáticas. Se guarda como Pendiente, sin asignar |
| 05 | Detalle | Jefe, técnico, personal de sistemas | `CabeceraHero`, `AvanceIncidencia`, 2 `SugerenciaIA`, evidencias, `BarraAccionInferior` | Linterna en la cabecera. Dos acciones: «Repuesto» y «Cerrar con informe» |
| 06 | Mis tareas | Técnico | `AvisoConexion`, `PestañasSegmentadas`, tarjetas con `SeguimientoRepuesto` | Funciona sin conexión leyendo de Room |
| 07 | Asignar | Jefe | Resumen de incidencia, `TecnicoCargaRow` | Técnico en límite = bloqueado. Botón: «Asignar a <nombre>» |
| 08 | Informe y cierre | Técnico | `SugerenciaIA` (borrador), campos, `BarraAccionInferior` | La hoja de huella del prototipo es ilustración: usa `rememberVerificadorBiometrico()` (RF-23) |
| 09 | Asistente | Técnico | Burbujas de chat, `SugerenciaIA` para el borrador, campo con micrófono | La IA solo sugiere. Falla o cuota agotada: la pantalla sigue sin la sugerencia |
| 10 | Indicadores | Jefe | `TarjetaMetrica`, gráficos Vico, filas de carga | Columnas por día con el máximo en `acento`. Los números salen de Room |
| 11 | Historial del equipo | Jefe, técnico, personal de sistemas | `CabeceraHero`, `LineaTiempo`, aviso de reemplazo | «Equipo crítico» si hay fallas reiteradas en 3 meses |
| 12 | Solicitar repuesto | Técnico | Campos, tarjeta de disponibilidad (API Logística), `BarraAccionInferior` | «Enviar con huella». Guarda `confirmadoEn` |
| 13 | Estado de repuestos | Técnico | `PestañasSegmentadas`, `SeguimientoRepuesto` | Muestra quién confirmó con huella |
| 14 | Diccionario de fallas | Técnico y personal de sistemas (solo lectura); el jefe no entra | Buscador, `FiltrosChips`, tarjetas, `FabExtendido` | «Registrar falla» solo para técnico |
| 15 | Escanear etiqueta | Técnico, jefe, personal de sistemas | `PreviewView` de CameraX + ML Kit, marco ámbar | Devuelve el código con `savedStateHandle` |
| 16 | Evidencia fotográfica | Técnico | CameraX, pestañas Falla / Pieza cambiada, miniaturas | Fotos en almacenamiento interno |
| 17 | Dictado por voz | Todos los que registran | `SpeechRecognizer`, onda de audio, texto transcrito | Devuelve el texto con `savedStateHandle`. El usuario lo corrige antes de guardar |
| 18 | Ajustes de seguridad | Todos | Interruptor de huella, aviso fijo de confirmaciones, privacidad | «Cerrar sesión» desvincula la huella. Interruptor con `Modifier.toggleable` y rol `Switch` |

Menú inferior por perfil:
- Técnico: Inicio, Incidencias, Mis tareas, Asistente.
- Jefe: Incidencias, Asignar, Indicadores, Historial.
- Personal de sistemas: se define al implementar su HU. El prototipo no lo cubre.

## 6. Lo que los prototipos no cubren

Pregunta antes de inventarlo: no hay diseño para estos casos.
- Estados de carga, listas vacías y errores de red por pantalla.
- Inicio del jefe y del personal de sistemas.
- Modo oscuro (solo hay la propuesta de la sección 3).
- Formulario de «Registrar falla» del diccionario.
- Tablets y orientación horizontal.

## 7. Accesibilidad

- Toques de 48 dp o más (`AreaTactilMinima`).
- Contraste de texto 4.5:1 (3:1 desde 24 sp). Si cambias un color, compruébalo.
- El estado nunca depende solo del color.
- Todo ícono que es botón lleva `contentDescription`. Los decorativos, no.
- Campos con etiqueta visible, no solo `placeholder`.
- Textos de ejemplo de los prototipos (Luis Ramírez, INC-0124…) son datos de prueba: no se escriben en el código de producción. Las `@Preview` sí los usan, como el resto de la app.

## 8. Qué no hacer

- Hexadecimales o `fontSize` fuera de `core/theme/`.
- Dibujar la hoja de huella o los diálogos del sistema en Compose.
- Ámbar como color de texto o de fondo extenso.
- Un segundo botón relleno en la misma pantalla.
- Etiquetas de estado con color y sin texto.
- Reactivar el color dinámico.
