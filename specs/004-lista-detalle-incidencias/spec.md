# Spec 004 — Lista de incidencias con búsqueda y filtros, y detalle (HU-04)

> Spec escrita después de implementar la HU (PR #8, commit `1940fcb`, con ajustes del PR #9, commit `6420f40`), a partir del código en `main`. RF-42 a RF-57 ya estaban implementados; tres ajustes se terminaron de construir en el plan de esta spec: la búsqueda y los filtros (RF-44) se movieron de la presentación a una función pura de `domain/` (`feature/incidencias/domain/FiltrarIncidencias.kt`), ambos `ViewModel` (lista y detalle) recibieron un temporizador propio para que una etiqueta relativa como «Hoy» no quede desactualizada, y el chip de filtro por marca cambió de nombre para no confundirse con el estado oficial. El cómo está en `plan.md`.
>
> Esta spec pasó por una fase de clarificación de un agente revisor, que evaluó el código contra `CLAUDE.md` y la constitución. El usuario ya decidió qué hacer con cada observación («Dudas abiertas» recoge el resultado); esta versión de la spec ya refleja esas decisiones.

## Contexto y objetivo
Con varias incidencias registradas, el jefe, el técnico y el personal de sistemas necesitan encontrar rápido el caso que buscan y ver todo su historial en un solo lugar. Esta HU agrega la lista general de incidencias —con búsqueda de texto y filtros— y la pantalla de detalle de una incidencia, con sus datos y su línea de tiempo de movimientos. Ambas pantallas solo leen de Room; no deciden reglas de negocio nuevas, aunque dejan ver, según el perfil de quien mira, los puntos de entrada hacia acciones que implementarán otras HU (asignar, atender, agregar foto, ver historial).

## Usuarios / actores
- Jefe de Soporte y Sistemas, técnico y personal de sistemas: los tres perfiles tienen el permiso `CONSULTAR_INCIDENCIAS` (`Perfil.kt`); ninguno queda excluido de ver la lista y el detalle.
- Room, como única fuente de verdad local: ambas pantallas leen de ahí, nunca directo de Firestore.

## Historias de usuario
- HU-04: Como persona del área de Soporte y Sistemas quiero ver la lista de incidencias con búsqueda y filtros, y abrir el detalle de cualquiera de ellas, para encontrar rápido el caso que necesito y conocer su historial completo.

## Requisitos funcionales (criterios de aceptación en EARS)

### Lista de incidencias
- RF-42: CUANDO el usuario abre la lista de incidencias, EL SISTEMA muestra todas las incidencias guardadas en Room (`IncidenciaDao.observarTodas`, `ORDER BY fechaRegistro DESC`), de la más reciente a la más antigua.
  > Observación de comportamiento actual (revisada por QA): ni `ObservarIncidencias` ni `ObservarDetalleIncidencia` llaman a `VerificarPermiso(Permiso.CONSULTAR_INCIDENCIAS)` antes de leer; ambos casos de uso invocan el repositorio directamente. Decisión: se mantiene así. Los tres perfiles tienen el permiso (ver «Usuarios/actores») y la navegación por perfil ya decide quién llega a estas pantallas, así que no hay ningún camino de error real que este caso de uso tenga que validar.
- RF-43: EL SISTEMA permite buscar por texto, comparando sin distinguir mayúsculas ni tildes el código visible de la incidencia (o «Por sincronizar»), el código del equipo y la descripción del problema; por ejemplo, «124» encuentra «INC-0124» aunque no se escriba el prefijo, y «despues» encuentra «después».
- RF-44: EL SISTEMA permite filtrar la lista por marca de seguimiento (chip «Etapa» en la pantalla, para no confundirse ni con el estado oficial Pendiente/Solucionado que deriva de ella ni con la marca del equipo del catálogo), por técnico asignado (incluida la opción «Sin técnico» para las que no tienen ninguno) y por código de equipo; estos filtros se combinan entre sí y con la búsqueda de texto (RF-43), de modo que una incidencia solo aparece si cumple todas las condiciones activas a la vez. La búsqueda (RF-43) y estos filtros viven en una función pura de `domain/` (Kotlin puro, sin acceso a Room, Firestore ni permisos), que el `ViewModel` solo invoca; no es un caso de uso con `Resultado<T>` porque no tiene ningún camino de error que validar contra el dominio, solo transforma una lista ya cargada según lo que la persona escribió o eligió.
  > Decisión: antes la búsqueda y los filtros estaban en `ListaIncidenciasViewModel.filtrar()`, en la capa de presentación. Se movieron a `domain/` (la constitución, regla 3, y `CLAUDE.md` ubican las reglas en `domain/`, en Kotlin puro) y el chip «Estado» se renombró a «Marca» (la trampa «estado vs. marca» que señala `CLAUDE.md`). Se construyó en el plan de esta spec: la función vive en `feature/incidencias/domain/FiltrarIncidencias.kt` y `ListaIncidenciasViewModel` solo la invoca.
- RF-45: EL SISTEMA construye las opciones de los filtros de técnico y de equipo a partir de los datos ya cargados en el celular (los nombres de técnico distintos que aparecen asignados, agregando «Sin técnico» si hay alguna incidencia sin asignar; y los códigos de equipo distintos, en orden alfabético), y no de una lista fija o de un catálogo aparte.
- RF-46: MIENTRAS haya una búsqueda o algún filtro activo, EL SISTEMA muestra cuántas incidencias coinciden sobre el total («N de M incidencias») y ofrece «Limpiar filtros», que borra la búsqueda y los tres filtros de un solo toque.
- RF-47: SI ninguna incidencia coincide con la búsqueda o los filtros activos, ENTONCES EL SISTEMA muestra «Ninguna incidencia coincide con la búsqueda.»; SI en cambio no hay ninguna incidencia guardada en el celular, ENTONCES EL SISTEMA muestra «Aún no hay incidencias. Registra la primera con el botón «Registrar».» en su lugar.
- RF-48: EL SISTEMA muestra, en cada tarjeta de la lista, el código visible de la incidencia (o «Por sincronizar» mientras no tenga número) junto al código del equipo, la fecha de registro («Hoy HH:mm» si es de hoy, o «dd/MM» en otro caso), la descripción, la etiqueta de la incidencia (`EtiquetaIncidencia(marca, critica, diasPendiente)`, que pinta «Asignada» con `surfaceVariant` y siempre va con texto) y el nombre del técnico asignado o «Sin técnico».
- RF-49: CUANDO el usuario toca una tarjeta de la lista, EL SISTEMA abre el detalle de esa incidencia.
- RF-50: EL SISTEMA ofrece en la lista un botón para ir a «Nueva incidencia» (la pantalla de registro de la HU-03).

### Detalle de una incidencia
- RF-51: CUANDO el usuario abre el detalle de una incidencia, EL SISTEMA muestra su estado oficial derivado («Pendiente» o «Solucionado», nunca persistido) y, mientras siga pendiente, además la etiqueta de seguimiento (`EtiquetaIncidencia`) y, si ya se conocen, el tipo de falla y la prioridad.
- RF-52: EL SISTEMA muestra en el detalle el código del equipo (con acceso a «Ver historial del equipo»), el usuario responsable, quién la registró, la fecha y hora completas del registro, y el técnico asignado o «Sin técnico».
- RF-53: EL SISTEMA muestra la línea de tiempo de movimientos de la incidencia (`ObservarDetalleIncidencia`, que combina `observarPorId` con `observarMovimientos` y ordena por fecha descendente), del más reciente al más antiguo, con el tipo de movimiento, su detalle o autor, y la fecha.
- RF-54: SI el identificador de la incidencia no existe en este celular, ENTONCES EL SISTEMA muestra «No se encontró la incidencia en este celular.» en vez de los datos.
- RF-55: CUANDO se llega al detalle recién guardado desde el registro (HU-03, RF-40), EL SISTEMA muestra una sola vez el aviso que deja esa pantalla («Incidencia registrada · Por sincronizar»), leído y retirado de `savedStateHandle` al abrir el detalle.
- RF-56: EL SISTEMA muestra el botón «Asignar técnico» solo si quien mira tiene el permiso `ASIGNAR_INCIDENCIA` y la incidencia sigue pendiente, y los botones «Solicitar repuesto» y «Cerrar con informe» solo si quien mira tiene el permiso `ATENDER_INCIDENCIA`, es la persona asignada a esa incidencia (`tecnicoAsignadoId == usuario.id`) y la incidencia sigue pendiente.
  > Observación de comportamiento actual (revisada por QA): `puedeAsignar` y `puedeAtender` se calculan dentro de `DetalleIncidenciaViewModel`, combinando el permiso del perfil con la condición de «sigue pendiente» (y, para atender, con ser el técnico asignado). Hoy solo sirven para mostrar u ocultar estos dos botones en la pantalla. Decisión: se mantiene así en esta spec; no hay todavía un caso de uso de dominio que valide «asignar» o «atender» como acción porque esa validación de negocio llega con las HU-06 y HU-08, que traerán sus propios casos de uso.
- RF-57: EL SISTEMA muestra también, en el detalle, el botón «Agregar foto» y el enlace «Ver historial del equipo», visibles siempre que hay una incidencia cargada, sin que esta HU defina qué hacen al pulsarlos (son de las futuras HU de captura y de historial); y, en el menú de la barra superior, la opción «Abrir asistente», sin comportamiento propio de esta HU (es de la futura HU del asistente de IA).

## Requisitos no funcionales
- Idioma: textos de UI y mensajes en español.
- Consistencia: Room es la única fuente de la que leen ambas pantallas; ninguna de las dos consulta Firestore directamente.
- Plataforma: Android 8.0 (minSdk 26) o superior.
- Diseño: ninguna marca se representa solo con color; «Asignada» usa siempre `surfaceVariant` acompañado de su texto, como exige `CLAUDE.md`.

## Casos límite
- Incidencia recién registrada, aún sin número: aparece como «Por sincronizar» tanto en la tarjeta de la lista como en el título y los datos del detalle (HU-03, RF-38/RF-40).
- Buscar «124» sin el prefijo «INC-»: encuentra «INC-0124» (RF-43).
- Buscar con mayúsculas o tildes distintas a como está guardado el texto (por ejemplo «CONEXION» encuentra «conexión»): coincide igual (RF-43).
- Filtro «Sin técnico» combinado con una búsqueda de texto: solo quedan las incidencias que cumplen ambas condiciones a la vez (RF-44).
- «Limpiar filtros» con búsqueda y los tres filtros activos a la vez: los quita todos de un solo toque (RF-46).
- Detalle de un id que no existe en este celular (por ejemplo, porque aún no se sincronizó a este dispositivo): se muestra el aviso de no encontrada en vez de quedar cargando indefinidamente (RF-54).
- Incidencia ya cerrada (marca `CERRADA`, estado Solucionado): el detalle no muestra la etiqueta de seguimiento ni los botones de asignar o atender, solo la etiqueta de estado «Solucionado» (RF-51, RF-56).
- Técnico que no es el asignado a una incidencia en atención: no ve «Solicitar repuesto» ni «Cerrar con informe», aunque tenga el permiso `ATENDER_INCIDENCIA` (RF-56).
- Rotación de pantalla con una búsqueda o filtros ya aplicados en la lista: los dos `ViewModel` sobreviven a la rotación y conservan lo elegido.
- Pantalla de lista o de detalle abierta mucho tiempo sin que lleguen cambios nuevos de datos o de usuario: `ListaIncidenciasViewModel` y `DetalleIncidenciaViewModel` agregaron un temporizador propio, igual que la pantalla de registro (HU-03, RF-32), que actualiza `ahora` cada minuto aunque no cambien las incidencias, los movimientos ni el usuario observado, para que una etiqueta relativa como «Hoy» no quede desactualizada si la pantalla queda abierta, por ejemplo, pasada la medianoche. Decisión: se agregó en el plan de esta spec y se verificó manualmente que ninguna de las dos pantallas falla ni se congela tras un minuto abierta.

## Fuera de alcance
- Registrar una incidencia nueva: es la HU-03 (`specs/003-registro-incidencia`); esta spec solo referencia el botón «Registrar» de la lista y el aviso que deja el registro al abrir el detalle (RF-50, RF-55).
- El aviso de conexión (`AvisoConexion`, que la lista muestra sobre el buscador) y la sincronización con Firestore que asigna el número visible: son de la HU-05, aún sin spec propia; esta spec solo referencia ese resultado («Por sincronizar»).
- Qué hacen al pulsarlos los botones «Asignar técnico» (HU-06), «Solicitar repuesto» y «Cerrar con informe» (HU-08, HU-11), «Agregar foto» (HU de captura), «Ver historial del equipo» (HU de historial) y «Abrir asistente» (HU del asistente de IA): esta spec solo documenta cuándo se muestran o habilitan, no su comportamiento al tocarlos.
- Clasificar el tipo de falla o la prioridad, o sugerirlos con IA: son de otras HU; aquí solo se muestran si ya existen.
- Cualquier escritura: esta HU es enteramente de lectura.

## Criterios de finalización
- RF-42 a RF-57 implementados.
- Pruebas: `ListaIncidenciasViewModelTest` cubre RF-42 (orden de la más reciente a la más antigua), RF-43 (búsqueda por código con o sin prefijo, por equipo sin importar mayúsculas, por descripción sin importar tildes), RF-44 (filtro por cada marca, por técnico y por «Sin técnico», por equipo, y la combinación de búsqueda con filtros), RF-45 (las opciones de técnico y equipo salen de los datos cargados) y RF-46 (limpiar quita la búsqueda y los filtros de un toque). `DetalleIncidenciaViewModelTest` cubre RF-53 (la línea de tiempo muestra el movimiento más reciente arriba, y descarta movimientos de otra incidencia), RF-56 (el jefe asigna y solo el técnico asignado atiende; otro técnico no puede atender una incidencia que no es suya; una incidencia solucionada no se asigna ni se atiende) y RF-54 (un id que no existe se informa como no encontrada). `TextosIncidenciaTest` cubre el «Por sincronizar» de RF-42/RF-48/RF-51/RF-55 a nivel de `codigoParaMostrar()`.
- RF-47, RF-48, RF-49, RF-50, RF-55 y RF-57 no tienen prueba automatizada por ser de pantalla (`Compose`); se verifican con las vistas previas (`@Preview` en claro y oscuro de `ListaIncidenciasScreen.kt` y `DetalleIncidenciaScreen.kt`) y de forma manual en el emulador o celular: abrir la lista con y sin incidencias, buscar y filtrar, tocar una tarjeta, abrir el detalle de una incidencia recién registrada y comprobar el aviso, y de una incidencia cerrada.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde.
- Prueba manual en celular: entrar con cada perfil y comprobar qué botones ve cada uno en el detalle de una incidencia sin asignar, asignada a otro técnico y asignada a quien mira; buscar y filtrar en la lista con datos reales; abrir el detalle de una incidencia con número y de una «Por sincronizar»; comprobar el aviso de «Incidencia registrada» al llegar desde el registro y que no reaparece si se vuelve a esa pantalla.

## Dudas abiertas
- [NECESITA ACLARACIÓN] Enlazar RF-42 a RF-57 con los RF de la especificación del producto: falta saber qué RF del producto corresponden a la HU-04 (el documento no está en el repo), igual que en las specs 001 a 003.
- Resuelto: la ausencia de `VerificarPermiso(Permiso.CONSULTAR_INCIDENCIAS)` en `ObservarIncidencias`/`ObservarDetalleIncidencia` (RF-42) se revisó; el usuario decidió no tocarla, queda tal como está.
- Resuelto: `puedeAsignar`/`puedeAtender` (RF-56), calculados dentro de `DetalleIncidenciaViewModel`, se revisaron; el usuario decidió no extraerlos a un caso de uso propio, porque esa validación de negocio llega con las HU-06 y HU-08.
- Resuelto e implementado: se decidió mover la búsqueda (RF-43) y los filtros (RF-44) de `ListaIncidenciasViewModel.filtrar()` a una función pura de `domain/`; se construyó en el plan de esta spec, en `feature/incidencias/domain/FiltrarIncidencias.kt`, con su propia prueba (`FiltrarIncidenciasTest`).
- Resuelto e implementado: se decidió agregar un temporizador propio en `ListaIncidenciasViewModel` y en `DetalleIncidenciaViewModel`, igual que RF-32 de la HU-03, para que «ahora» no dependa solo de que cambien los datos; se construyó y se probó (unitaria con tiempo virtual, y manual en el emulador) en el plan de esta spec.
- Resuelto e implementado: se renombró el chip de filtro «Estado» a «Marca» en `ListaIncidenciasScreen.kt`, para no confundirse con `EstadoIncidencia` (la trampa «estado vs. marca» que señala `CLAUDE.md`).
- Corregido después (2026-10-11, tarea T17): «Marca» se confundía con la marca del equipo (campo `marca` del catálogo de equipos, HU-03), y la palabra «marca» no aparece en ninguna otra parte de la interfaz. El chip pasa a llamarse «Etapa»: describe en qué punto del seguimiento va la incidencia y no choca con ninguno de los dos.
- Hallazgos adicionales de bajo impacto señalados por el revisor, que el usuario no pidió corregir (no llevan tarea en el plan): el orden en que aparece «Sin técnico» entre las opciones del filtro de técnico, la visibilidad de los botones de acción según la sub-marca exacta, y que el filtro de técnico compare por nombre y no por id (`tecnicoAsignadoId`).
