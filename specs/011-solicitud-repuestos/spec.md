# Spec 011 — Solicitud de repuestos a Logística (HU-11)

> Estado: completa (2026-11-07), RF-1101 a RF-1124, en #107 a #117. Probado en un celular real (Xiaomi, Android 14) el 2026-11-07: buscar la pieza contra MockAPI.io y enviar con huella; registro en Logística en segundo plano; subida a Firestore con las reglas publicadas; sección «Repuestos» del detalle; y «Estado de repuestos» actualizando el estado cambiado a mano en MockAPI. La solicitud en modo avión, la vista del jefe en otro celular, la vuelta a «En atención» con todas las solicitudes entregadas y `SolicitudRepuestoDaoTest` en un celular se dieron por cumplidas a pedido de Alonso, sin la prueba manual. Aprobada el 2026-11-07, con las decisiones D-1 a D-10 respondidas por el usuario (ver «Decisiones»).

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-11 (RF-1101…). Corresponde a tres requisitos del documento del producto (`SIGTEC_docx.pdf`, sección 11.1, fuera del repo), citados como «del producto» para no confundirlos con los RF de las specs 001 y 002: el **RF-08 del producto** («registrar una solicitud de repuesto dirigida al área de Logística»), el **RF-18 del producto** («consultar a la API de Logística la disponibilidad de un repuesto antes de solicitarlo») y el **RF-23 del producto** (confirmación con huella antes de enviar una solicitud de repuesto, ya en `CLAUDE.md`). También sigue el proceso PN-4 «Abastecimiento de repuestos», la sección 14 (API de Logística consumida con Retrofit y corrutinas, contra un servidor de prueba con la misma estructura), la 16.2 (confirmación con huella o bloqueo de pantalla) y los prototipos 8 «Solicitud de repuesto» y 17 «Estado de repuestos». Cubre el requisito 6 del curso, «Corrutinas y Retrofit para APIs externas».

## Contexto y objetivo
Cuando un equipo necesita una pieza, hoy el técnico la pide a Logística por correo o en persona, sin saber si hay en almacén, y la incidencia queda detenida sin que nadie sepa por qué ni desde cuándo. El jefe ve un caso demorado y no puede distinguir si el retraso es del técnico o de Logística.

Esta HU le permite al técnico asignado consultar desde la incidencia si la pieza está en almacén y cuánto tardaría el envío, y registrar la solicitud confirmándola con su huella. La incidencia sigue Pendiente con la marca «Esperando repuesto» y el conteo de días, de modo que la demora queda atribuida a Logística. El técnico sigue sus piezas en «Estado de repuestos» y, cuando Logística entrega la última, la incidencia vuelve a «En atención» para que retome la reparación.

## Usuarios / actores
- Técnico asignado: el único que solicita repuestos para una incidencia, mientras siga pendiente. Es la misma persona y condición con la que el detalle ya muestra «Solicitar repuesto» y «Cerrar con informe» (HU-08, `ATENDER_INCIDENCIA`). Ve sus solicitudes en «Estado de repuestos».
- Jefe, técnicos y personal de sistemas: ven las solicitudes de una incidencia en su detalle.
- Logística (actor externo): responde la disponibilidad, recibe la solicitud y cambia su estado (por despachar, en camino, entregado). Mientras la empresa no habilite su API real, la reemplaza un servidor de prueba en MockAPI.io con la misma estructura (D-1); los cambios de estado se hacen a mano en ese servidor para simular a Logística.
- Android: verifica la huella o el bloqueo de pantalla (`rememberVerificadorBiometrico()`).

## Historias de usuario
- HU-11: Como técnico, quiero consultar si la pieza está en almacén y registrar la solicitud confirmada con mi huella, para no esperar en vano y que la demora quede atribuida a Logística.
- Criterios de aceptación de la planificación (`SIGTEC_Planificación (1).xlsx`, sprint 3): la app muestra la disponibilidad de la pieza sin congelar la pantalla; la solicitud se envía tras la verificación con huella y se guarda con equipo, pieza, motivo, fecha y hora de confirmación; la incidencia muestra la espera y los días transcurridos.

## Requisitos funcionales (criterios de aceptación en EARS)

### Abrir la solicitud
- RF-1101: CUANDO el técnico asignado toca «Repuesto» (antes «Solicitar repuesto», cambiado en el retema v2) en el detalle de una incidencia pendiente, EL SISTEMA abre «Solicitar repuesto» (prototipo 8) con un recuadro «Para la incidencia» (código visible o «Por sincronizar», código del equipo y descripción), el campo «Pieza requerida *», el campo «Motivo *», el aviso «Mientras llega la pieza, la incidencia seguirá Pendiente con la marca «Esperando repuesto» y el conteo de días.» y el botón «Enviar con huella».
- RF-1102: EL SISTEMA solo deja solicitar repuestos al técnico asignado y mientras la incidencia esté pendiente (marcas Asignada, En atención o Esperando repuesto). Lo verifica en el dominio con `VerificarPermiso(ATENDER_INCIDENCIA)` y comparando el técnico asignado con quien tiene la sesión; si no se cumple, devuelve `SinPermiso` o `IncidenciaCerrada` (errores que ya existen) y no guarda nada.
- RF-1103: Una incidencia puede tener varias solicitudes; pedir una pieza más con otra ya en camino está permitido.

### Consultar la disponibilidad (RF-18 del producto)
- RF-1104: MIENTRAS el técnico escribe en «Pieza requerida», EL SISTEMA consulta el catálogo de piezas de Logística cuando hay al menos 3 letras y el técnico deja de escribir 0,6 segundos, y muestra debajo hasta 5 piezas que coinciden, cada una con su nombre.
- RF-1105: CUANDO el técnico elige una pieza de la lista, EL SISTEMA escribe su nombre en el campo y muestra su disponibilidad: con stock, «Disponible en almacén: N unidades» con el envío estimado («envío estimado en N días», en singular con 1) y la hora de la consulta («Consultado a Logística hace un momento»), en el color de estado verde; sin stock, «Sin stock en almacén, Logística deberá conseguirla», en el color de estado amarillo (D-3).
- RF-1106: MIENTRAS la consulta está en curso, EL SISTEMA muestra «Consultando a Logística…» con un indicador de progreso y deja seguir escribiendo; la pantalla nunca se congela (sección 14 del producto). Si el técnico sigue escribiendo, la consulta anterior se cancela y solo cuenta la última.
- RF-1107: SI ninguna pieza del catálogo coincide, ENTONCES EL SISTEMA muestra «Logística no tiene esa pieza en su catálogo. Puedes solicitarla igual y describirla en el motivo.» y deja enviar.
- RF-1108: SI la consulta falla por falta de conexión o porque Logística no responde en 10 segundos, ENTONCES EL SISTEMA muestra «No se pudo consultar a Logística. Puedes solicitarla igual; la disponibilidad se confirmará después.» con «Reintentar», y deja enviar (D-2).
- RF-1109: SI el técnico edita la pieza después de elegirla, ENTONCES EL SISTEMA quita la disponibilidad mostrada y vuelve a consultar con RF-1104.

### Enviar la solicitud (RF-08 y RF-23 del producto)
- RF-1110: MIENTRAS «Pieza requerida» o «Motivo» estén vacíos (o solo con espacios), o haya un envío en curso, o el celular no tenga huella ni bloqueo de pantalla, EL SISTEMA mantiene deshabilitado «Enviar con huella». Sin seguridad en el celular muestra lo mismo que el informe técnico (HU-08) para configurarla.
- RF-1111: CUANDO el técnico pulsa «Enviar con huella», EL SISTEMA muestra el diálogo de Android con el título «Confirma la solicitud de repuesto» y acepta huella o PIN, patrón o contraseña (`rememberVerificadorBiometrico()`, RF-23 del producto).
- RF-1112: SI el técnico cancela el diálogo o la verificación falla, ENTONCES EL SISTEMA no guarda nada y deja el formulario como estaba.
- RF-1113: CUANDO la verificación es correcta, EL SISTEMA guarda en una sola transacción de Room la solicitud (id UUID generado en el celular, incidencia, pieza y motivo sin espacios en los extremos, fecha de la solicitud, `confirmadoEn` = instante de la verificación, estado «Por despachar» y la disponibilidad consultada, si la hubo), el movimiento `REPUESTO_SOLICITADO` («Fuente de poder ATX 500 W» como detalle) y la marca «Esperando repuesto» en la incidencia, todo con `pendienteSincronizar = true`. Si la incidencia ya estaba esperando otro repuesto, la marca no cambia.
- RF-1114: CUANDO el guardado termina, EL SISTEMA vuelve al detalle y muestra «Solicitud registrada, por enviar a Logística». La solicitud se guarda primero en el celular, así que funciona sin conexión (D-2).
- RF-1115: SI Room falla al guardar, ENTONCES EL SISTEMA no guarda ni la solicitud ni el movimiento ni el cambio de marca, muestra «Ocurrió un error inesperado. Inténtalo de nuevo.» y conserva lo escrito.
- RF-1116: MIENTRAS se guarda, EL SISTEMA mantiene deshabilitado «Enviar con huella», para que un doble toque no registre dos solicitudes.

### Registrar la solicitud en Logística
- RF-1117: CUANDO hay conexión, EL SISTEMA registra en Logística cada solicitud que todavía no tiene número de Logística, en segundo plano y aunque la app esté cerrada, con equipo, incidencia, pieza, motivo, fecha y hora de confirmación. Al registrarla guarda el número que devuelve Logística. Si falla, reintenta más tarde sin intervención (WorkManager).
- RF-1118: EL SISTEMA sube la solicitud a Firestore con la sincronización de siempre (`core/sync`), igual que el informe técnico, para que exista en el servidor del área aunque Logística no responda. Firestore exige `confirmadoEn` y que quien la crea sea el técnico asignado (D-8).
- RF-1119: SI Logística rechaza la solicitud con un error permanente (datos inválidos), ENTONCES EL SISTEMA deja de reintentar, la marca para atención y la muestra en «Estado de repuestos» con «Logística rechazó la solicitud»; la incidencia sigue esperando repuesto hasta que el técnico la cierre o pida otra pieza.

### Ver las solicitudes
- RF-1120: EL SISTEMA muestra en el detalle de la incidencia, a todos los perfiles, una sección «Repuestos» con cada solicitud: pieza, estado («Por enviar a Logística», «Por despachar», «En camino», «Entregado» o «Rechazada») con su color y texto, fecha de la solicitud y quién la confirmó con huella. Sin solicitudes, la sección no se muestra.
- RF-1121: CUANDO el técnico toca «Repuestos» en «Mis tareas» (prototipo 7), EL SISTEMA abre «Estado de repuestos» (prototipo 17) con sus solicitudes, de la más reciente a la más antigua, y los filtros «Todos», «Pendientes» y «Entregados».
- RF-1122: EL SISTEMA muestra cada solicitud con la pieza, el código visible de la incidencia y el del equipo, la etiqueta de estado, una línea de avance de tres pasos (Solicitado, Despachado, Entregado) con sus fechas o el envío estimado, y, mientras no se entregue, los días de espera desde la solicitud («4 días de espera»; hasta el retema v2 decía además «la demora se atribuye a Logística», quitado por la regla 2 de redacción).
- RF-1123: CUANDO el técnico abre «Estado de repuestos» o desliza hacia abajo, EL SISTEMA consulta a Logística el estado de sus solicitudes no entregadas y actualiza los que cambiaron; muestra «Consultado a Logística hoy a las HH:mm» con la hora de la última consulta correcta. Sin conexión muestra lo último guardado con esa hora. La consulta automática diaria es de la HU-18.
- RF-1124: CUANDO la consulta trae que una solicitud pasó a «Entregado» y era la última sin entregar de su incidencia, EL SISTEMA, en una sola transacción, cambia la marca de la incidencia de «Esperando repuesto» a «En atención» y guarda el movimiento `REPUESTO_RECIBIDO`, para que el técnico retome la reparación (PN-4). Si la incidencia ya no está esperando repuesto (por ejemplo, ya se cerró), solo actualiza la solicitud.

## Requisitos no funcionales
- Idioma: textos de UI y mensajes en español; los errores salen de `error.mensaje()`.
- Fluidez: toda llamada a Logística es una corrutina fuera del hilo principal, con 10 segundos de límite; la pantalla muestra el progreso y sigue respondiendo (sección 14 del producto).
- Sin conexión: solicitar funciona sin conexión (se guarda en Room); consultar disponibilidad y estado necesita conexión, y sin ella se avisa y se muestra lo último guardado.
- Seguridad: la confirmación biométrica de la solicitud es obligatoria (RF-23 del producto, constitución 5); `confirmadoEn` es obligatorio en el modelo, en Room y en Firestore. La dirección del servidor de prueba se declara en un solo lugar; no es secreta, pero no va repartida por el código.
- Costo cero: MockAPI.io en su plan gratuito y Firebase Spark; sin servicios de pago (D-1).
- Diseño: «Solicitar repuesto» y «Estado de repuestos» siguen el rediseño del PR #39: `BarraSuperior` con «Volver», `Scaffold(contentWindowInsets = WindowInsets(0))`, botones de `Botones.kt` («Enviar con huella» con `BotonPrincipal` y el ícono de huella), etiquetas de estado con `SigtecTheme.estados.*` y texto, solo roles de `MaterialTheme.colorScheme`, `Espaciado.*` y vistas previas en claro y oscuro.
- Pruebas: la fecha de la solicitud y `confirmadoEn` las decide quien invoca el caso de uso, nunca un `now()` dentro del dominio; Logística queda detrás de una interfaz con un doble de prueba.
- Persistencia: cambia `SolicitudRepuestoEntity` (número de Logística, disponibilidad consultada, fechas de despacho y entrega, marca de rechazo); sube `version` de `SigtecDatabase` y se dice en el PR (la base usa migración destructiva solo en desarrollo).

## Casos límite
- Sin conexión: no hay lista de piezas ni disponibilidad (RF-1108), pero se puede enviar con huella; la solicitud queda «Por enviar a Logística» y se registra sola al volver la conexión (RF-1117).
- Pieza que no está en el catálogo de Logística: se puede pedir igual (RF-1107).
- Sin stock: se puede pedir igual, con el aviso de RF-1105 (D-3).
- El técnico escribe rápido: solo cuenta la última consulta (RF-1106).
- Doble toque en «Enviar con huella»: una sola solicitud (RF-1116).
- Huella cancelada: no se guarda nada (RF-1112).
- La incidencia se reasigna o se cierra desde otro celular con el formulario abierto: al enviar, el dominio la rechaza con `SinPermiso` o `IncidenciaCerrada` y no guarda nada.
- Varias solicitudes en la misma incidencia: la marca vuelve a «En atención» solo cuando la última se entrega (RF-1124).
- La incidencia se cierra con repuestos todavía en camino: se permite (HU-08, RF-812); las solicitudes siguen su curso en «Estado de repuestos» y su entrega ya no cambia la marca.
- Incidencia «Por sincronizar»: no puede tener técnico asignado (RF-610, HU-06), así que no llega a esta pantalla.
- Logística caída o lenta: la consulta termina a los 10 segundos con RF-1108; el registro se reintenta en segundo plano (RF-1117).
- Respuesta de Logística con un formato inesperado: se trata como si no respondiera (RF-1108) y se registra en Logcat.
- Cerrar sesión con solicitudes sin subir: el aviso de RF-13 (HU-01) ya cuenta las incidencias con cambios sin enviar, y la incidencia queda pendiente con su solicitud.

## Fuera de alcance
- La consulta automática diaria del estado de las solicitudes con WorkManager y su aviso: es de la HU-18. Esta HU deja la consulta lista para que el worker la llame.
- Marcar casos críticos por demora: es de la HU-18.
- La gestión interna de Logística (stock, compras, proveedores): solo se consulta y se registra (sección 4.2 del producto).
- Cancelar o editar una solicitud ya enviada.
- Dictar el motivo con el micrófono: «Dictar» (HU-10) se puede conectar después sin cambiarla.
- El directorio de colaboradores (HU-12), la otra API con Retrofit.
- Cambiar el conteo de días de la etiqueta «Esperando repuesto, N días» de las listas, que hoy cuenta desde el registro (D-9).

## Criterios de finalización
- RF-1101 a RF-1124 implementados.
- Pruebas unitarias (JVM): el caso de uso de solicitar (sin sesión, sin permiso, otro técnico, incidencia cerrada, campos vacíos, el camino feliz con la marca y el movimiento, ya esperando otro repuesto, falla de Room); la regla de qué mostrar según la disponibilidad (con stock, sin stock, sin coincidencias, sin conexión); el repositorio de Logística con un doble (respuestas, error de red, tiempo agotado, formato inválido); la actualización de estados con la vuelta a «En atención» solo con la última entrega; los `ViewModel` de las dos pantallas con dobles y `MainDispatcherRule`.
- Las pruebas instrumentadas de `SolicitudRepuestoDao` pasan en un celular y `version` subió.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- `firebase/firestore.rules` revisado en el PR y publicado a mano en la consola (D-8).
- Prueba manual en un celular real, con capturas en claro y oscuro:
  - Como técnico asignado, buscar una pieza con stock y otra sin stock, enviar con huella y ver la marca «Esperando repuesto», el movimiento y la sección «Repuestos» del detalle; comprobar en MockAPI.io que la solicitud llegó.
  - En modo avión, enviar una solicitud, ver «Por enviar a Logística» y comprobar que se registra sola al volver la conexión.
  - Cambiar a mano el estado en MockAPI.io a «En camino» y luego «Entregado», actualizar «Estado de repuestos» y ver que la incidencia vuelve a «En atención».
  - Como jefe, ver la sección «Repuestos» del detalle y comprobar que no aparece «Repuesto».
- La línea de la HU-11 en `MEMORY.md` dice «completa».

## Decisiones
- D-1 · Servidor de prueba de Logística (RF-1104 a RF-1124). **Aprobada el 2026-11-07:** MockAPI.io en su plan gratuito, con dos recursos: el catálogo de piezas (nombre, stock, días de envío) y las solicitudes (con su estado y fechas). La cuenta y el proyecto los crea el usuario; la app solo conoce la dirección base. Se descartaron my-json-server (ya no responde: error 530 el 2026-11-07) y un JSON en GitHub Pages (solo permite consultar, no registrar).
- D-2 · Sin conexión (RF-1108, RF-1114, RF-1117). **Aprobada el 2026-11-07:** se puede enviar con huella sin haber consultado; la solicitud queda «Por enviar a Logística» y se registra sola al volver la conexión.
- D-3 · Sin stock (RF-1105). **Aprobada el 2026-11-07:** se puede solicitar igual, con el aviso «Sin stock en almacén, Logística deberá conseguirla».
- D-4 · «Estado de repuestos» (RF-1121 a RF-1124). **Aprobada el 2026-11-07:** entra en esta HU, con consulta al abrir y al deslizar; la consulta diaria automática queda para la HU-18.
- D-5 · Quién y cuándo solicita (RF-1102, RF-1103). **Aprobada el 2026-11-07:** solo el técnico asignado, en las marcas Asignada, En atención o Esperando repuesto (las mismas en que el detalle ya muestra el botón), y varias solicitudes por incidencia. Alternativa: exigir «En atención» antes de pedir.
- D-6 · Buscar la pieza (RF-1104, RF-1105). **Aprobada el 2026-11-07:** búsqueda mientras se escribe, con lista de coincidencias para elegir, como el prototipo 8. Alternativa: un botón «Consultar» junto al campo.
- D-7 · Entrega del último repuesto (RF-1124). **Aprobada el 2026-11-07:** vuelve a «En atención» con el movimiento `REPUESTO_RECIBIDO`, que ya existe en el modelo, cuando se entrega la última solicitud pendiente de la incidencia. Alternativa: que el técnico lo marque a mano.
- D-8 · Reglas de Firestore (RF-1118). **Aprobada el 2026-11-07:** `solicitudesRepuesto` solo la crea el técnico asignado a su incidencia (hoy basta con ser técnico) y solo se actualizan el estado de entrega, el número de Logística y sus fechas. Toca `firebase/firestore.rules` (archivo compartido) y se publica a mano en la consola.
- D-9 · Días de espera (RF-1122). **Aprobada el 2026-11-07:** «Estado de repuestos» cuenta los días desde la solicitud; la etiqueta de las listas («Esperando repuesto, N días») sigue contando desde el registro de la incidencia, como hoy, para no tocar `FilaIncidencia` en esta HU.
- D-10 · Dónde vive el código (RF-1101 a RF-1124). **Aprobada el 2026-11-07:** en `feature/atencion` (`domain`, `data`, `di`, `presentation`), donde ya está la ruta `SolicitarRepuesto` y la bandeja; Retrofit y su `LogisticaApi` en `feature/atencion/data`. La subida a Firestore, en `core/sync`, como siempre.

## Dudas abiertas
- Plan gratuito de MockAPI.io: confirmar al crear el proyecto cuántos recursos y registros permite (la HU necesita 2 recursos).
