# Plan: pantallas con los componentes v2

> **Estado:** decisiones respondidas por Alonso el 2026-11-08. El código avanza en paralelo en PR apilados; si Nina pide cambios en este plan, se ajustan en esos PR.
>
> **Fuentes:** `DESIGN.md` (secciones 4 y 5), `docs/prototipos/`, `specs/retema-v2/plan.md` y `specs/inicio-panel/spec.md`.
>
> **Alcance:** las pantallas que ya existen, más Inicio (que hoy es temporal), pasan a usar los componentes del retema v2. No cambian la lógica, los datos ni las reglas de cada HU, salvo Inicio, que tiene su propia spec.

## Decisiones
- I-1, I-2 e I-3: ver `specs/inicio-panel/spec.md`.
- P-1 · Proceso. **Aprobada el 2026-11-08:** este plan va en un PR para Nina y el código avanza en paralelo, un módulo por PR.
- P-2 · Bloques de IA (prototipos 04, 05, 07 y 08). **Aprobada el 2026-11-08:** se omiten hasta la HU-14 (I-3). Las pantallas quedan listas para agregar `TarjetaSugerenciaIA`.
- P-3 · El ámbar en el detalle (prototipo 05 pinta «Prioridad alta» en `acento`). `DESIGN.md` 3 solo permite el ámbar en el FAB, el micrófono, la linterna, el máximo del gráfico y el avatar propio. Se respeta `DESIGN.md`: la prioridad va como las demás etiquetas de la cabecera.
- P-4 · Pantallas sin HU (Asistente, Indicadores, Historial y Diccionario) siguen como «en construcción».

## Pantallas por PR

| PR | Rama | Pantallas | Componentes que adopta |
|---|---|---|---|
| 1 | `feat/pantallas-v2-inicio` | 02 Inicio (spec propia) | `CabeceraHero`, `TarjetaMetrica`, `Tarjeta`, `Avatar` |
| 2 | `feat/pantallas-v2-incidencias` | 03 Lista, 04 Nueva incidencia, 05 Detalle | `FabExtendido` «Nueva» (los filtros de la lista son menús por etapa, técnico y equipo, no chips de una sola opción, y se mantienen), `CampoTexto`, `BarraAccionInferior`, `CabeceraHero` en el detalle con la linterna, `LineaTiempo`, `Tarjeta` |
| 3 | `feat/pantallas-v2-atencion` | 06 Mis tareas, 08 Informe, 12 Solicitar repuesto, 13 Estado de repuestos | `PestanasSegmentadas` (filtros de repuestos), `SeguimientoRepuesto`, `CampoTexto`, `BarraAccionInferior`, `Tarjeta`, avatar propio en `acento` |
| 4 | `feat/pantallas-v2-asignacion` | 07 Asignar | `FilaCargaTecnico` (con «Recomendado» para el de menor carga entre los que pueden recibir), `BarraAccionInferior` con «Asignar a <nombre>» |
| 5 | `feat/pantallas-v2-acceso` | 01 Login, 18 Ajustes | `CabeceraHero` en el login (logo y nombre), `CampoTexto`, tarjeta de perfil sobre `hero` en Ajustes, avatar propio en `acento` |
| 6 | `feat/pantallas-v2-captura` | 15 Escanear, 16 Evidencia, 17 Dictado | Marco ámbar del escaneo, `PestanasSegmentadas` en evidencia (Falla / Pieza cambiada), fondo `hero` y micrófono en `acento` en el dictado |

Cada PR sale del anterior (apilados). Cada uno:
- tiene vistas previas en claro y oscuro de cada pantalla;
- pasa el CI y lint igual que la línea base;
- se prueba en el celular con capturas en claro y oscuro;
- reemplaza los composables privados que duplican un componente compartido (plan del retema, R-3).

## Textos
Valen las reglas de redacción. Se aplican además los textos de las imágenes aprobados en la sección 5.3 del plan del retema. Por ejemplo:
- en el login se quita el eslogan y «Acceso exclusivo…», y el título es «Iniciar sesión»;
- en Mis tareas, «Asignada» sin «sin iniciar»;
- en el detalle, «Ver historial» sin flecha.

Cualquier texto nuevo que quite información se propone antes en el PR.

## Riesgos
- R-1 · El detalle cambia mucho (cabecera oscura, barra inferior). Se conservan todas sus acciones y permisos; las pruebas del ViewModel no cambian.
- R-2 · «Escanear» desde Inicio abre «Nueva incidencia» y encima la cámara, para que el código vuelva al formulario por `savedStateHandle` como hoy.
- R-3 · El login tiene la oferta de huella y el diálogo de ingreso con datos sin enviar: se mantienen igual.
