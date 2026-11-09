# Spec 015 — Tablero de indicadores del área (HU-15)

> Estado: borrador (2026-11-09), RF-1501 a RF-1524. Decisiones D-1 a D-4 respondidas por Alonso el 2026-11-09 (ver «Decisiones»). Falta el visto bueno de la spec antes del código.

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-15 (RF-1501…). Sale de la planificación (`SIGTEC_Planificación (1).xlsx`, Sprint 4, semana 12). Sigue el prototipo 10 «Indicadores del área» y cubre el criterio 9 del curso, «Dashboards», con Vico.

## Contexto y objetivo
Hoy el jefe sabe cómo va el área revisando la lista de incidencias una por una o pidiendo reportes. Inicio le muestra tres números (sin asignar, en atención y críticas), pero no cuántos equipos se reparan por día, cuánto tarda una atención, cómo está cargado cada técnico ni qué equipos fallan una y otra vez.

Con esta HU, la pestaña «Indicadores» muestra un tablero con esos datos, calculado en el celular con lo que ya está en Room y actualizado solo cuando llegan cambios de la sincronización.

## Usuarios / actores
- Jefe (`VER_INDICADORES`): el único que ve la pestaña y el tablero.
- Técnicos y personal de sistemas: no tienen acceso.

## Historias de usuario
- HU-15: Como jefe, quiero un tablero con los indicadores del área, para decidir sin pedir reportes a mano.
- Criterio de aceptación de la planificación: muestra equipos reparados por día, incidencias por estado, carga por técnico frente a su límite, tiempo promedio de atención y equipos críticos.

## Requisitos funcionales (criterios de aceptación en EARS)

### Acceso y cabecera
- RF-1501: CUANDO el jefe abre la pestaña «Indicadores», EL SISTEMA muestra «Indicadores del área» en una `CabeceraHero` con su nombre y cargo arriba (prototipo 10).
- RF-1502: EL SISTEMA muestra debajo del título cuándo se bajaron datos por última vez («Actualizado hoy a las 18:05», «Actualizado ayer a las 09:10», «Actualizado el 07/11 a las 08:00»), con la última descarga correcta de la sincronización. Sin ninguna descarga, «Sin datos del servidor todavía».
- RF-1503: SI quien abre la pantalla no tiene `VER_INDICADORES`, ENTONCES el caso de uso devuelve `SinPermiso` y la pantalla dice «No tienes acceso a los indicadores.» (la pestaña ya no se le muestra).
- RF-1504: EL SISTEMA calcula todo en el celular con lo que hay en Room, fuera del hilo principal, y lo recalcula cuando cambian las incidencias, los movimientos o los técnicos (la sincronización en tiempo real los trae). No consulta la red.

### Métricas de la cabecera (prototipo 10)
- RF-1505: EL SISTEMA muestra tres `TarjetaMetrica` sobre la cabecera: «Reparados en 7 días», «Pendientes» y «Críticos (+5 días)».
- RF-1506: «Reparados en 7 días» cuenta las incidencias cerradas (movimiento CERRADA) en los últimos 7 días, contando hoy (D-3).
- RF-1507: «Pendientes» cuenta las incidencias en estado Pendiente (cualquier marca que no sea CERRADA).
- RF-1508: «Críticos (+5 días)» cuenta las incidencias pendientes registradas hace más de 5 días, más las que tengan `critica = true` (que marcará la HU-18) (D-1). La tarjeta va en la variante de alerta solo si el número es mayor que 0.

### Equipos reparados por día
- RF-1509: EL SISTEMA muestra la tarjeta «Equipos reparados por día» con un gráfico de columnas (Vico) de los últimos 7 días, de hace 6 días a hoy, con la inicial del día debajo («Lun», «Mar»…) y el número encima de cada columna (D-3).
- RF-1510: La columna de hoy va en acento (ámbar) y las demás en el color principal, como el prototipo; el día sale en texto, así que no depende solo del color.
- RF-1511: Un cierre cuenta en el día local (zona del celular) de la fecha de su movimiento CERRADA. Una incidencia con varios movimientos CERRADA (no debería pasar) cuenta una sola vez, con el más reciente.
- RF-1512: EL SISTEMA muestra junto al título el tiempo promedio de atención de esas incidencias (RF-1514), por ejemplo «1,8 días de atención».
- RF-1513: SI no hubo cierres en los 7 días, ENTONCES el gráfico muestra las 7 columnas en 0 y el texto «Sin equipos reparados en los últimos 7 días».

### Tiempo promedio de atención
- RF-1514: EL SISTEMA calcula el tiempo promedio de atención como el promedio, en días con un decimal, entre la fecha de registro de cada incidencia y la fecha de su movimiento CERRADA, para las cerradas en los últimos 30 días. Menos de un día se muestra en horas («6 h de atención»). Sin cierres en 30 días, no se muestra.

### Incidencias por estado (D-2)
- RF-1515: EL SISTEMA muestra la tarjeta «Incidencias por estado» con una fila por marca pendiente (Sin asignar, Asignada, En atención, Esperando repuesto) y una de Solucionadas, cada una con su nombre, su número y una barra proporcional al total.
- RF-1516: Cada fila usa el color de su estado (`SigtecTheme.estados.*`) y siempre lleva el texto; las marcas con 0 se muestran igual, con la barra vacía.

### Carga por técnico
- RF-1517: EL SISTEMA muestra la tarjeta «Carga por técnico» con una fila por técnico, ordenada por nombre: primer nombre, una barra de carga y «activas / límite» (prototipo 10).
- RF-1518: La carga es la misma que ve el jefe al asignar: la del servidor más las asignaciones de este celular aún sin subir (`cargaEfectiva`, HU-06).
- RF-1519: SI un técnico llegó a su límite, ENTONCES su barra y su número van en rojo (`error`); el número siempre se ve, así que no depende solo del color.
- RF-1520: Sin técnicos descargados, la tarjeta dice «Aún no se descargaron los técnicos.».

### Equipo a evaluar (D-4)
- RF-1521: EL SISTEMA busca el equipo con más incidencias registradas en los últimos 90 días; SI tiene 3 o más, ENTONCES muestra una tarjeta de alerta con el código del equipo, su oficina si está en el catálogo, «N incidencias en 3 meses, evaluar reemplazo» y una flecha.
- RF-1522: CUANDO el jefe toca esa tarjeta, EL SISTEMA abre «Historial del equipo» con ese código (hoy pantalla en construcción, se completa en la HU-16).
- RF-1523: Con empate en el número de incidencias, va el equipo con la incidencia más reciente; sin ningún equipo con 3 o más, la tarjeta no aparece.

### General
- RF-1524: El tablero no cambia ningún dato: no escribe en Room ni en Firestore.

## Requisitos no funcionales
- Idioma: textos de UI en español, sin «·»; los números decimales con coma («1,8»).
- Rendimiento: el cálculo es una función pura sobre listas en memoria; con 2 000 incidencias y sus movimientos tarda menos de 1 segundo (prueba JVM).
- Sin conexión: el tablero funciona sin conexión con lo último que bajó; RF-1502 dice desde cuándo.
- Diseño: prototipo 10 con los componentes v2: `CabeceraHero` con los íconos de la barra de estado claros, `TarjetaMetrica` (variante alerta para críticos), `Tarjeta` para cada bloque, colores de `SigtecTheme.estados` y del tema (sin hexadecimales), y el gráfico de Vico con los colores del tema. Vistas previas en claro y oscuro (con datos, sin cierres y sin técnicos).
- Accesibilidad: el gráfico tiene una descripción para TalkBack con los 7 valores («Lunes 5, martes 6…»).
- Persistencia: sin cambios de entidades ni de `version`. Se agrega una consulta a `MovimientoDao` para los movimientos CERRADA.
- Firestore: sin cambios en las reglas.
- Pruebas: el cálculo (`calcularIndicadores`) con pruebas JVM; el caso de uso y el ViewModel con dobles y `MainDispatcherRule`.

## Casos límite
- Recién instalado, sin datos: todas las métricas en 0, el gráfico en 0, sin tarjeta de equipo y «Sin datos del servidor todavía».
- Incidencia cerrada en otro celular: su movimiento CERRADA baja con la sincronización, así que cuenta igual.
- Incidencia cerrada sin movimiento CERRADA en Room (por ejemplo, si el movimiento aún no bajó): no cuenta en reparados ni en el tiempo promedio hasta que llegue.
- Cierre justo a medianoche: cuenta en el día local de su fecha.
- Cambio de día con la pantalla abierta: el ViewModel usa el mismo reloj de 60 segundos que la lista y el detalle, así que «hoy» y los 7 días se corren solos.
- Técnico con límite 0 (mal configurado): se muestra «0 / 0» en rojo sin dividir por cero.
- Equipo que ya no está en el catálogo: la tarjeta muestra solo el código.

## Fuera de alcance
- El historial del equipo y su búsqueda (prototipo 11): HU-16.
- Marcar incidencias como críticas en Room, avisos y el resumen del día: HU-18.
- Filtros por fechas, exportar o compartir el tablero.
- La barra inferior del prototipo 10 (Incidencias, Asignar, Indicadores, Historial): se mantiene la barra por permisos de hoy.
- Cambiar Inicio del jefe.

## Criterios de finalización
- RF-1501 a RF-1524 implementados.
- Pruebas JVM: `calcularIndicadores` (reparados en 7 días y por día con zona, un cierre por incidencia, pendientes, críticos por 5 días y por `critica`, promedio en días y en horas, sin cierres, estados con 0, carga con asignaciones sin subir y límite alcanzado, equipo a evaluar con umbral y empate, 2 000 incidencias en menos de 1 s); `ObservarIndicadores` (sin sesión, técnico, jefe); `IndicadoresViewModel`.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- Prueba manual en un celular real con `cmendoza`, con capturas en claro y oscuro: tablero con datos reales, cerrar una incidencia desde otro celular o cuenta y ver que sube «Reparados», y tocar la tarjeta del equipo.
- La línea de la HU-15 en `MEMORY.md` dice «completa» y se borra `DatosDemo.kt` (`EQUIPO_DEMO`), que solo existía para esta pantalla.

## Decisiones
- D-1 · Qué es crítico (RF-1508). **Aprobada el 2026-11-09:** pendiente hace más de 5 días, como el prototipo («Críticos (+5 días)»), calculado en el celular; también cuentan las que marque `critica` la HU-18. Alternativas descartadas: solo el campo `critica` (hoy siempre 0) y contar equipos con muchas incidencias.
- D-2 · Incidencias por estado (RF-1515, RF-1516). **Aprobada el 2026-11-09:** una tarjeta con una barra por marca y las solucionadas, porque el criterio lo pide aunque el prototipo no lo dibuje. Alternativa descartada: solo las métricas de arriba.
- D-3 · Período del gráfico (RF-1506, RF-1509). **Aprobada el 2026-11-09:** los últimos 7 días contando hoy, con hoy resaltado; así nunca empieza vacío. Alternativa descartada: la semana actual de lunes a domingo, como el prototipo.
- D-4 · Equipo a evaluar (RF-1521 a RF-1523). **Aprobada el 2026-11-09:** el equipo con más incidencias en 90 días, si tiene 3 o más, con acceso al historial (HU-16). Alternativa descartada: dejarlo para la HU-16.

## Dudas abiertas
- Ninguna.
