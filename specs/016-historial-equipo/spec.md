# Spec 016 — Historial de atenciones de un equipo (HU-16)

> Estado: borrador para revisar (2026-11-16). Decisiones D-1 a D-7 propuestas, sin aprobar; ver «Decisiones» y «Dudas
> abiertas».

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-16
> (RF-1601…). Sale de la planificación (`SIGTEC_Planificación.xlsx`, Sprint 4, semana 12, épica EP-07 «Control y
> seguimiento del área»). Sigue el prototipo 11 «Historial del equipo» de `SIGTEC___Prototipos.pdf`
> (`docs/prototipos/11-historial.png`).

## Contexto y objetivo
Cuando un equipo falla una y otra vez, la decisión de repararlo o reemplazarlo se toma hoy sin datos: nadie junta las
incidencias de ese equipo, quién las atendió ni qué se le hizo. La app ya guarda todo eso por incidencia, pero no lo
muestra por equipo. Los accesos ya existen y llevan a una pantalla en construcción: «Ver historial» en el detalle de
una incidencia (HU-04) y la tarjeta del equipo a evaluar del tablero (HU-15, RF-1522).

Con esta HU, «Historial del equipo» muestra, para un código de equipo, sus datos del catálogo, tres números (atenciones
en 3 meses, piezas cambiadas y días promedio de atención), un aviso de «Equipo crítico» cuando falla seguido, y la línea
de tiempo de todas sus incidencias con su problema, su técnico y lo que dice su informe. Todo sale de Room; para que
el informe de un técnico se vea en los demás celulares, la sincronización empieza a bajar los informes (D-1).

## Usuarios / actores
- Jefe, técnico y personal de sistemas: los tres tienen `CONSULTAR_HISTORIAL` (`Perfil.kt`) y ven lo mismo.
- `core/sync`: baja la colección `/informes` (D-1); las pantallas siguen leyendo solo de Room.

## Historias de usuario
- HU-16: Como miembro del área, quiero el historial de atenciones de un equipo, para sustentar si conviene repararlo o
  reemplazarlo.
- Descripción de la planificación: convierte las incidencias cerradas en un argumento con datos para la decisión de
  compra, que hoy se toma sin sustento.
- Criterio de aceptación de la planificación: muestra en orden cronológico todas las incidencias del equipo con su
  problema, técnico e informe.

## Requisitos funcionales (criterios de aceptación en EARS)

### Acceso
- RF-1601: CUANDO la persona toca «Ver historial» en el detalle de una incidencia o la tarjeta del equipo a evaluar en
  «Indicadores», EL SISTEMA abre «Historial del equipo» con ese código (`Ruta.HistorialEquipo(codigoEquipo)`).
- RF-1602: SI quien abre la pantalla no tiene `CONSULTAR_HISTORIAL` o no hay sesión, ENTONCES el caso de uso devuelve
  `SinPermiso` y la pantalla muestra su mensaje (`error.mensaje()`) en lugar del historial.
- RF-1603: CUANDO la pantalla se abre sin código (`codigoEquipo = null`), EL SISTEMA muestra la búsqueda: un campo
  «Código del equipo» con el botón de escanear (HU-09) y el botón «Ver historial» (D-5).
- RF-1604: CUANDO la persona escribe un código y pulsa «Ver historial», EL SISTEMA recorta los espacios, lo pasa a
  mayúsculas (como RF-34) y muestra el historial de ese código en la misma pantalla. Con el campo vacío, el botón queda
  deshabilitado.
- RF-1605: CUANDO el escaneo devuelve un código (`CLAVE_CODIGO_ESCANEADO`), EL SISTEMA lo pone en el campo y muestra su
  historial sin pedir otro toque.
- RF-1606: EL SISTEMA agrega el acceso rápido «Historial» en Inicio, junto a «Registrar» y «Escanear», que abre la
  pantalla sin código (D-5).

### Cabecera (prototipo 11)
- RF-1607: EL SISTEMA muestra una `CabeceraHero` con el botón de volver, la etiqueta «Código del equipo», el código en
  grande y, si el equipo está en el catálogo de Room, dos líneas: su tipo con su marca y modelo, y su oficina con su
  responsable.
- RF-1608: SI el código no está en el catálogo, ENTONCES la cabecera muestra solo el código y la línea «Equipo fuera
  del catálogo», y el historial se muestra igual con las incidencias que haya.
- RF-1609: EL SISTEMA muestra en la cabecera tres `TarjetaMetrica`: «Atenciones en 3 meses», «Piezas cambiadas» y
  «Días promedio de atención» (D-3).
- RF-1610: «Atenciones en 3 meses» cuenta las incidencias del equipo registradas en los últimos 90 días, contando hoy.
- RF-1611: «Piezas cambiadas» cuenta las incidencias cerradas del equipo cuyo informe tiene el campo «Piezas cambiadas»
  con texto (una por atención, no por pieza escrita) (D-3).
- RF-1612: «Días promedio de atención» es el promedio, con un decimal y coma, entre la fecha de registro de cada
  incidencia cerrada del equipo y la de su movimiento CERRADA, la misma medida del tablero (RF-1514). Menos de un día se
  muestra en horas («6 h»). Sin incidencias cerradas, la tarjeta muestra «-».

### Equipo crítico (D-2)
- RF-1613: SI el equipo tiene 3 o más incidencias registradas en los últimos 90 días (la misma regla que elige el equipo
  a evaluar del tablero, RF-1521), ENTONCES EL SISTEMA muestra la etiqueta «Equipo crítico» con ícono en la cabecera y,
  debajo de ella, el aviso «Este equipo tuvo fallas reiteradas en los últimos tres meses. Estos datos sirven para
  evaluar su reemplazo.».
- RF-1614: SI no llega a ese número, ENTONCES no se muestran ni la etiqueta ni el aviso.

### Atenciones recibidas
- RF-1615: EL SISTEMA muestra bajo el título «Atenciones recibidas» todas las incidencias del equipo, de la más reciente
  a la más antigua por fecha de registro (D-4), en una línea de tiempo.
- RF-1616: Cada atención muestra su fecha de registro (dd/MM/aaaa), su código visible (o «Por sincronizar»), su estado
  en texto («Solucionado» si está cerrada; si no, el nombre de su marca: «Sin asignar», «Asignada», «En atención»,
  «Esperando repuesto») y la descripción del problema como título.
- RF-1617: Debajo del título, una incidencia cerrada muestra el trabajo realizado de su informe y el técnico que la
  atendió; una pendiente muestra el técnico asignado o «Sin técnico» (D-7).
- RF-1618: SI una incidencia cerrada aún no tiene su informe en el celular, ENTONCES muestra «Informe por descargar» en
  lugar del trabajo realizado, con el técnico.
- RF-1619: El punto de la línea de tiempo usa el color de su estado (`SigtecTheme.estados.*`): el de pendiente para las
  pendientes y el de solucionado para las cerradas; el estado siempre va también en texto.
- RF-1620: CUANDO la persona toca una atención, EL SISTEMA abre el detalle de esa incidencia (HU-04), que ya muestra el
  informe completo (RF-827) (D-7).
- RF-1621: SI el equipo no tiene ninguna incidencia en el celular, ENTONCES EL SISTEMA muestra la cabecera con las
  métricas en 0 y «-», y en lugar de la lista el texto «Este equipo no tiene atenciones registradas.».

### Datos y sincronización (D-1)
- RF-1622: EL SISTEMA calcula el historial en el celular con lo que hay en Room (incidencias, movimientos, informes y
  catálogo), fuera del hilo principal, y lo vuelve a calcular cuando cambia cualquiera de ellos. No consulta la red.
- RF-1623: La sincronización en tiempo real baja la colección `/informes` a Room, con la misma escucha que las demás
  colecciones, para que el informe de un técnico se vea en el historial de los demás celulares.
- RF-1624: Al bajar un informe, EL SISTEMA lo guarda solo si el celular no tiene ya un informe para esa incidencia: los
  informes no se editan (la regla de Firestore solo permite crearlos), así que el local, subido o no, nunca se pisa.
- RF-1625: Un informe bajado se guarda como ya sincronizado (`pendienteSincronizar = false`), así no vuelve a subir ni
  cuenta en «registros sin enviar».
- RF-1626: El historial no cambia ningún dato: no escribe en Room (salvo la descarga de RF-1623) ni en Firestore.

## Requisitos no funcionales
- Idioma: textos de UI en español, sin «·» (retema v2); los decimales con coma («2,1»).
- Sin conexión: funciona con lo último que bajó. Una incidencia registrada sin conexión aparece de inmediato con «Por
  sincronizar».
- Rendimiento: el cálculo es una función pura sobre listas en memoria; con 500 incidencias de un equipo tarda menos de
  1 segundo (prueba JVM).
- Diseño: prototipo 11 con los componentes v2: `CabeceraHero` con `IconosBarraEstadoClaros()` y `FondoBarraEstado` (la
  pantalla se desplaza), `TarjetaMetrica`, `EtiquetaEstado` con `SigtecTheme.estados.critico` y el ícono de alerta, el aviso como en las demás
  pantallas y `LineaTiempo` (si su forma actual no alcanza para las tres líneas de cada atención, el plan dice si se
  extiende o se hace una fila propia). Sin hexadecimales ni `fontSize`. Vistas previas en claro y oscuro: con historial
  crítico, sin atenciones, fuera del catálogo y la búsqueda.
- Accesibilidad: cada atención se lee como un solo elemento («27 de septiembre de 2026, INC-0124, En atención, No
  enciende después del corte de luz, Luis Ramírez») con el rol de botón (RF-1620); toques de 48 dp o más.
- Persistencia: sin entidades nuevas ni cambio de `version`. Se agregan consultas de solo lectura (incidencias de un
  equipo, sus informes y sus movimientos CERRADA) a los DAO existentes.
- Firestore: sin cambios en las reglas; `/informes` ya permite leer a cualquier usuario autenticado.
- Privacidad: el informe ya lo ve cualquier perfil en el detalle (HU-08); bajarlo no expone datos nuevos a otros
  perfiles.
- Pruebas: el cálculo con pruebas JVM; el caso de uso y el ViewModel con dobles y `MainDispatcherRule`; la regla de la
  descarga de informes (RF-1624) como función pura en `ReglasSincronizacion.kt`, con su prueba.

## Casos límite
- Equipo con incidencias de hace más de 3 meses y ninguna reciente: aparecen todas en la lista, «Atenciones en 3 meses»
  es 0 y no es crítico.
- Incidencia cerrada en otro celular: su movimiento CERRADA y, con D-1, su informe bajan solos; mientras el informe no
  llegue, se ve «Informe por descargar» (RF-1618).
- Incidencia cerrada sin movimiento CERRADA en Room: se muestra como cerrada (la marca manda) pero no cuenta en el
  promedio hasta que el movimiento llegue (igual que el tablero).
- Informe con «Piezas cambiadas» en blanco o solo espacios: no cuenta como pieza cambiada.
- Código escrito con minúsculas o espacios (« pc-cont-014 »): se busca «PC-CONT-014».
- Código que no existe en el catálogo ni tiene incidencias: «Equipo fuera del catálogo» y «Este equipo no tiene
  atenciones registradas.».
- Rotación o cambio de tema con la pantalla abierta: el código buscado vive en el ViewModel y no se pierde.
- Cambio de día con la pantalla abierta: el ViewModel usa el mismo reloj de 60 segundos que el tablero, así que los
  90 días se corren solos.
- Otra persona entra en el celular: `core/sync` borra Room antes de volver a escuchar (D-12 de la HU-05), y los informes
  bajan de nuevo con el resto.

## Fuera de alcance
- La pestaña «Historial» en el menú inferior del jefe (DESIGN.md): se mantiene el menú por permisos de hoy.
- Marcar el equipo como crítico en Room o Firestore, o avisar por él: el aviso es solo visual; las alertas son de la
  HU-18.
- Resumir el historial con Gemini (prototipo 09, «Resumir el historial del equipo»): el asistente quedó fuera en la
  HU-14 (D-1 de la 014).
- Mostrar fotos de evidencia o solicitudes de repuesto en el historial: se ven en el detalle de cada incidencia.
- Exportar o compartir el historial, o filtrarlo por fechas.
- Editar el catálogo de equipos: sigue siendo de solo lectura (RF-41).

## Criterios de finalización
- RF-1601 a RF-1626 implementados.
- Pruebas JVM: el cálculo del historial (orden, 90 días con hoy, piezas con texto vacío, promedio en días y en horas, sin
  cerradas, cerrada sin movimiento o sin informe, crítico con 2 y con 3, fuera del catálogo, 500 incidencias en menos de
  1 s); la normalización del código; la regla de descarga de informes (sin local, con local subido, con local
  pendiente); el caso de uso (sin sesión, sin permiso, camino feliz con cada perfil); el ViewModel (con código, búsqueda,
  código escaneado, rotación) con `MainDispatcherRule`.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- Prueba manual en un celular real, con capturas en claro y oscuro:
  - Como técnico, cerrar una incidencia con informe; como jefe en otro celular (o tras cambiar de cuenta), abrir su
    historial desde el detalle y ver el trabajo realizado y el técnico.
  - Abrir el historial desde la tarjeta de «Indicadores» de un equipo con 3 o más incidencias y ver «Equipo crítico».
  - Desde Inicio, «Historial», escanear la etiqueta de un equipo y ver su historial; luego escribir un código a mano.
  - En modo avión, abrir el historial y ver lo último que bajó.
  - Tocar una atención y llegar a su detalle.
- `PantallaEnConstruccion` de `Ruta.HistorialEquipo` reemplazada, y la línea de la HU-16 en `MEMORY.md` dice
  «completa».

## Decisiones
Propuestas, pendientes de aprobación.
- D-1 · Bajar los informes (RF-1622 a RF-1625). **Propuesta:** escuchar `/informes` en `SincronizacionTiempoReal` y
  guardarlos solo si no hay uno local para esa incidencia. Sin esto, el historial solo mostraría los informes escritos
  en el propio celular, y el criterio pide el informe. La HU-08 lo dejó para esta HU (D-4 de su plan). Toca
  `core/sync` (`SincronizacionTiempoReal.kt`, `MapeoFirestore.kt`, `ReglasSincronizacion.kt`), que es archivo
  compartido. Alternativa descartada: leer `/informes` directo desde la pantalla (rompe «Room es la fuente de verdad» y
  no funciona sin conexión).
- D-2 · Qué es «Equipo crítico» (RF-1613, RF-1614). **Propuesta:** la misma regla del equipo a evaluar del tablero (3 o
  más incidencias registradas en 90 días), sacada a una función que usen los dos, para que un equipo no sea crítico en
  una pantalla y no en la otra. Alternativa: contar solo las cerradas o solo las de hardware.
- D-3 · Las tres métricas (RF-1609 a RF-1612). **Propuesta:** «Atenciones en 3 meses» con la ventana de 90 días de D-2;
  «Piezas cambiadas» y «Días promedio de atención» sobre todo el historial del equipo, porque sirven para la decisión de
  compra aunque las fallas sean antiguas. «Piezas cambiadas» cuenta informes con ese campo lleno, porque es texto libre
  («RAM de 16 GB y disco SSD») y no se puede contar piezas sin adivinar. Alternativas: las tres en 90 días; contar las
  solicitudes de repuesto entregadas (HU-11), que no incluyen las piezas que el técnico tenía a mano.
- D-4 · Orden (RF-1615). **Propuesta:** de la más reciente a la más antigua, como el prototipo y como la lista de
  incidencias (HU-04). El criterio dice «en orden cronológico», y este orden lo es, solo que invertido. Alternativa: de
  la más antigua a la más reciente.
- D-5 · Buscar sin código (RF-1603 a RF-1606). **Propuesta:** cuando no llega código, la pantalla pide uno (escrito o
  escaneado), y se agrega el acceso rápido «Historial» en Inicio (el prototipo 02 lo muestra como «Historial de
  equipo»). Así el técnico frente a un equipo escanea su etiqueta y ve el historial sin buscar una incidencia; la HU-09
  dejó la clave del escaneo lista para esto. Toca `feature/inicio`. Alternativa: solo los dos accesos que ya existen y
  quitar el `null` de `Ruta.HistorialEquipo` (toca `Rutas.kt`).
- D-6 · Dónde vive el código (para el plan). **Propuesta:** en `feature/indicadores`, con ámbito `indicadores`: la ruta
  ya está registrada en `IndicadoresNavigation.kt`, comparte la regla de D-2 y el prototipo lo pone en «Control del
  área» junto al tablero. No cambia la lista de ámbitos de `CLAUDE.md`. Alternativa: un módulo nuevo
  `feature/historial` con ámbito `historial` (hay que agregarlo a `CLAUDE.md` y mover la ruta).
- D-7 · Qué muestra cada atención (RF-1617, RF-1620). **Propuesta:** el trabajo realizado del informe (como el
  prototipo), no el diagnóstico, y tocar la atención abre su detalle con el informe completo. Alternativa: mostrar
  también el diagnóstico en el historial, que alarga cada fila.

## Dudas abiertas
- ¿Se aprueban D-1 a D-7, en especial D-1 (toca `core/sync`) y D-5 (toca Inicio)?
- D-3: ¿«Piezas cambiadas» y el promedio sobre todo el historial, o los tres números en 3 meses?
- D-5: ¿el acceso rápido «Historial» en Inicio lo ven los tres perfiles? Hoy los accesos rápidos solo se muestran a
  quien puede registrar, que son los tres.
