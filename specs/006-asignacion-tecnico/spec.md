# Spec 006 — Asignación de trabajo con carga y límite de atención (HU-06)

> Estado: completa (2026-10-12). Aprobada el 2026-10-11; pruebas de consola y manuales hechas el 2026-10-12 (`tasks.md`, T23 a T45).

> Spec escrita antes del código (principio 2 de la constitución). La HU se entrega en seis PR pequeños (A a F); la división, los archivos y los commits de cada uno están en `plan.md`. Los requisitos usan el bloque propio de la HU-06 (RF-601…). Corresponde a los RF del producto RF-04 (consultar disponibilidad y carga de cada técnico), RF-05 (asignar y reasignar a un técnico disponible) y RF-06 (impedir asignar a quien alcanzó su límite de atención).

## Contexto y objetivo
Hoy el jefe de Soporte y Sistemas reparte el trabajo sin saber cuántos casos tiene cada técnico ni si ya llegó a su límite, y el técnico no se entera de qué le toca hasta que alguien se lo dice. Esta HU deja que el jefe, desde el detalle de una incidencia pendiente, vea a los técnicos con su carga actual y su límite, elija a uno disponible y le asigne (o le reasigne) la incidencia, sin poder pasarse del límite ni siquiera trabajando sin conexión. La asignación se guarda en el celular y la sincronización la sube; al llegar al servidor, la carga del técnico se actualiza en la misma operación y el servidor vuelve a validar el límite, de modo que dos celulares no puedan sobrecargar a la misma persona.

## Usuarios / actores
- Jefe de Soporte y Sistemas: el único perfil con el permiso `ASIGNAR_INCIDENCIA`; asigna y reasigna.
- Técnico: recibe la incidencia en su bandeja; su carga (incidencias activas) y su límite de atención se leen de su perfil en el servidor.
- Personal de sistemas: no participa (no tiene el permiso).
- Room, como fuente de verdad local; la sincronización (HU-05), única parte que habla con Firestore; y Firestore con sus reglas, que repite la validación del límite en el servidor.

## Historias de usuario
- HU-06: Como jefe del área quiero ver la carga y el límite de atención de cada técnico y asignarle (o reasignarle) una incidencia pendiente a uno que esté disponible, para repartir el trabajo sin sobrecargar a nadie y para que la incidencia aparezca en la bandeja del técnico elegido.

## Requisitos funcionales (criterios de aceptación en EARS)

### Ver a los técnicos y su carga (RF-04 del producto)
- RF-601: CUANDO el jefe pulsa «Asignar técnico» en el detalle de una incidencia pendiente, EL SISTEMA abre «Asignar incidencia» con un resumen de la incidencia (código visible, código del equipo y su oficina si está en el catálogo, descripción, marca de seguimiento y, si ya los tiene, tipo de falla y prioridad) y la lista de técnicos ordenada por nombre, cada uno con sus iniciales, su nombre, sus incidencias activas sobre su límite («3 / 5»), una barra de carga y su disponibilidad.
- RF-602: EL SISTEMA calcula las incidencias activas de cada técnico como las que le cuenta el servidor más las asignaciones a ese técnico que este celular guardó y todavía no subió; el técnico está disponible solo si ese total es menor que su límite de atención. La pantalla muestra y valida ese mismo total. (RF-04, RF-06)
- RF-603: MIENTRAS un técnico no esté disponible (RF-602), EL SISTEMA muestra su tarjeta atenuada, con un candado y el texto «Alcanzó su límite de atención», y no deja elegirlo; el motivo nunca se comunica solo con color.
- RF-604: MIENTRAS la incidencia ya tenga un técnico asignado, EL SISTEMA marca a ese técnico con el texto «Asignado ahora» y no deja elegirlo de nuevo.
- RF-605: MIENTRAS no se haya elegido un técnico disponible, o haya una asignación guardándose, EL SISTEMA mantiene deshabilitado el botón principal. Con un técnico elegido, el botón dice «Asignar a <nombre>» (o «Reasignar a <nombre>» si la incidencia ya tenía técnico) y debajo se lee «La incidencia aparecerá en la bandeja de <nombre>».
- RF-606: SI en el celular todavía no hay técnicos descargados (por ejemplo, primera vez sin conexión), ENTONCES EL SISTEMA muestra «Aún no se descargaron los técnicos. Conéctate a internet para verlos.» en lugar de la lista, con el botón deshabilitado.
- RF-607: SI la incidencia ya está cerrada o todavía no tiene número (RF-609, RF-610), ENTONCES EL SISTEMA muestra ese motivo en la pantalla desde que se abre y mantiene el botón deshabilitado, sin esperar a que el jefe lo pulse.

### Asignar o reasignar (RF-05 y RF-06 del producto)
- RF-608: CUANDO el jefe pulsa el botón principal, EL SISTEMA verifica en el dominio el permiso `ASIGNAR_INCIDENCIA`; si no hay sesión o el perfil no lo tiene, no guarda nada y devuelve `SinPermiso`, aunque el detalle solo muestre el botón al jefe.
- RF-609: SI la incidencia ya no está Pendiente (fue cerrada), ENTONCES EL SISTEMA no guarda nada y muestra «Esta incidencia ya está cerrada; no se puede asignar.».
- RF-610: SI la incidencia todavía no tiene número visible («Por sincronizar»), ENTONCES EL SISTEMA no guarda nada y muestra «Esta incidencia aún no se sincroniza. Podrás asignarla cuando tenga su número INC.». Por qué: el servidor solo acepta crear incidencias sin asignar, así que una incidencia creada y asignada sin conexión nunca podría subir.
- RF-611: SI el técnico elegido ya es el asignado, ENTONCES EL SISTEMA no guarda nada y muestra «La incidencia ya está asignada a <nombre>.».
- RF-612: SI las incidencias activas del técnico elegido (RF-602) son mayores o iguales que su límite de atención, ENTONCES EL SISTEMA no guarda nada y muestra «<nombre> alcanzó su límite de atención.» (`LimiteAtencionAlcanzado`). (RF-06)
- RF-613: CUANDO todas las validaciones pasan, EL SISTEMA guarda en una sola transacción de Room la incidencia con la marca Asignada, el id y el nombre del técnico, marcada como pendiente de sincronizar y con la hora de la asignación como fecha de actualización, junto con un movimiento `ASIGNADA` (si no tenía técnico) o `REASIGNADA` (si tenía otro), con esa misma hora y a nombre del jefe. Nunca queda uno sin el otro, y nada se escribe directamente en Firestore.
- RF-614: CUANDO se asigna una incidencia Sin asignar, o se reasigna una que estaba Asignada o En atención, EL SISTEMA la deja en Asignada para el técnico nuevo, porque este todavía no inicia la atención.
- RF-625: CUANDO se reasigna una incidencia que estaba Esperando repuesto, EL SISTEMA conserva esa marca para el técnico nuevo, porque la pieza sigue en camino. (Agregado al aprobar la spec; va junto a RF-614.)
- RF-615: EL SISTEMA describe el movimiento en la línea de tiempo del detalle con el técnico y quién asignó: «A Luis Ramírez, por Carlos Mendoza» (`ASIGNADA`) o «De Ana Torres a Luis Ramírez, por Carlos Mendoza» (`REASIGNADA`).
- RF-616: SI Room falla al guardar, ENTONCES EL SISTEMA no guarda ni la incidencia ni el movimiento, muestra «Ocurrió un error inesperado. Inténtalo de nuevo.» debajo del botón, y el jefe puede reintentar sin perder la elección.
- RF-617: CUANDO la asignación se guarda, EL SISTEMA vuelve al detalle de la incidencia, que muestra al técnico asignado, la marca Asignada, el movimiento nuevo y el aviso «Asignada a <nombre>».

### Llegada al técnico
- RF-618: CUANDO la asignación se sube y el celular del técnico la descarga, EL SISTEMA la incluye entre las incidencias asignadas a ese técnico y sin cerrar, de la más antigua a la más reciente (la bandeja de la HU-08), y su detalle le muestra las acciones de quien atiende. Mientras «Mis tareas» siga en construcción, se comprueba con el filtro por técnico de la lista (HU-04) y con el detalle.

### Sincronización de técnicos y de su carga (HU-05 ampliada para esta HU)
- RF-619: MIENTRAS haya sesión, EL SISTEMA descarga en tiempo real los usuarios con perfil técnico (id, nombre, límite de atención e incidencias activas) y los guarda en el celular; un usuario que deja de ser técnico o se elimina deja de aparecer, y un documento mal formado se descarta sin detener el resto (como RF-60).
- RF-620: CUANDO se sube el cambio de una incidencia que ya existe en el servidor y su técnico asignado es distinto del que tiene el servidor, EL SISTEMA, en una sola operación atómica, suma 1 a las incidencias activas del técnico nuevo, resta 1 a las del anterior (si lo había) y guarda la incidencia; si una parte falla, no se aplica ninguna.
- RF-621: SI al subir la asignación el técnico nuevo ya llegó a su límite según el servidor (por ejemplo, porque otro celular le asignó antes otra incidencia), ENTONCES EL SISTEMA no sube ese cambio y lo trata como un rechazo permanente: el celular vuelve a mostrar la versión del servidor, el cambio queda marcado como que necesita atención y su movimiento no se sube (comportamiento de rechazo que corrige el PR B en la spec 005).
- RF-622: EL SISTEMA, en las reglas de Firestore, solo acepta un cambio de técnico asignado en una incidencia pendiente si en la misma escritura las incidencias activas del técnico nuevo suben exactamente 1 y las del anterior bajan exactamente 1. Junto con la regla ya existente sobre usuarios (la carga queda entre 0 y el límite), el límite se valida también en el servidor. (RF-06)
- RF-623: EL SISTEMA, en las reglas de Firestore, deja que el técnico asignado actualice su incidencia, pero no que cambie el técnico asignado; solo el jefe asigna o reasigna, igual que en los permisos de la app.

### Conflicto entre el jefe y el técnico anterior
- RF-624: SI el jefe reasigna una incidencia mientras el técnico anterior, sin conexión, inicia su atención, ENTONCES, cuando el técnico anterior recupera la conexión, EL SISTEMA conserva en el servidor la reasignación del jefe (el cambio del técnico se rechaza o pierde frente a la versión más reciente), el celular del técnico anterior termina mostrando la versión del servidor con el aviso de que un cambio necesita atención, y ningún movimiento de su intento (por ejemplo, «Atención iniciada») queda en el servidor ni en la línea de tiempo de los demás.

## Requisitos no funcionales
- Arquitectura: la regla de asignación vive en un caso de uso de dominio en Kotlin puro, que consulta los datos a través del repositorio del módulo, nunca de un DAO; el módulo de asignación escribe solo en Room, y solo la sincronización habla con Firestore.
- Consistencia: el botón principal queda deshabilitado mientras se guarda, así que un doble toque no duplica la asignación ni su movimiento.
- Costo cero (plan Spark): la carga del técnico se mantiene con una transacción desde el celular y las reglas de Firestore; sin Cloud Functions ni notificaciones push.
- Diseño: solo colores y estilos del tema; la disponibilidad siempre va con texto o ícono; las tarjetas de técnico respetan el área táctil mínima; vistas previas en claro y oscuro.
- Firestore: `incidenciasActivas` y `limiteAtencion` se leen y escriben como enteros de 64 bits (`int64`).
- Pruebas: la hora de la asignación la decide quien invoca el caso de uso, para poder fijarla en las pruebas.
- Plataforma: Android 8.0 (minSdk 26) o superior.

## Casos límite
- Reasignación de A a B guardada en el celular y todavía sin subir: B ya suma ese cupo (RF-602), pero A sigue mostrando la carga que le cuenta el servidor hasta que la reasignación sube. A puede verse «Alcanzó su límite de atención» aunque ya liberó un cupo. Es conservador: nunca deja pasarse del límite, y se corrige solo al sincronizar. (Agregado en la revisión de los PR #25 y #26, 2026-10-12.)
- La pantalla queda abierta mucho tiempo, por ejemplo pasada la medianoche: los días pendientes del resumen se recalculan cada minuto con un temporizador propio, igual que la lista y el detalle (HU-04), sin esperar a que cambien los datos. (Agregado en la misma revisión.)
- Dos asignaciones sin conexión al mismo técnico que lo llevan a su límite: la segunda se rechaza en el celular, porque RF-602 cuenta la primera aunque todavía no haya subido.
- El mismo jefe asigna desde dos celulares el último cupo de un técnico, ambos sin conexión: los dos pasan la validación local; al subir, el primero se acepta y el segundo se rechaza en el servidor (RF-621).
- Justo después de que una asignación sube, la carga nueva del técnico puede tardar unos segundos en bajar: durante ese intervalo la carga mostrada puede quedar 1 por debajo; si el jefe usa ese cupo, el servidor rechaza el exceso (RF-621).
- Carga desfasada en el servidor: si `incidenciasActivas` de un técnico no coincidiera con sus incidencias pendientes asignadas, la resta al técnico anterior podría dejarla bajo 0 y el servidor rechazaría la reasignación. Antes de publicar la regla de RF-622 se verifica en la consola que coincidan para cada técnico (tarea de consola del plan).
- Reasignar una incidencia en Esperando repuesto: conserva la marca (RF-625); la solicitud de repuesto, cuando exista (HU-11), sigue asociada a la incidencia y no al técnico.
- Un técnico deja de serlo o se elimina mientras la pantalla está abierta: desaparece de la lista y, si estaba elegido, la elección se borra.
- La incidencia se cierra en otro celular mientras la pantalla está abierta: la pantalla pasa a mostrar el motivo de RF-607 y el botón se deshabilita.
- Rotación del celular o cambio de tema con un técnico elegido: la elección se conserva.
- Técnico con límite 0: nunca está disponible.
- El jefe asigna sin conexión: la incidencia queda asignada en su celular, el aviso de conexión muestra el cambio por enviar y sube sola al volver la señal (HU-05).

## Fuera de alcance
- Notificar al técnico cuando se le asigna una incidencia (RF-24 del producto; el prototipo dice «recibirá un aviso»): es la HU-07. Por eso el texto bajo el botón no promete ningún aviso (RF-605).
- La pantalla «Mis tareas» (bandeja del técnico): es la HU-08; aquí solo se exige que la incidencia quede en la consulta de su bandeja (RF-618).
- Restar 1 a la carga del técnico cuando cierra una incidencia: es la HU-08. **Dependencia para Alonso (HU-08):** la transacción de RF-620 debe restar 1 a la carga del técnico cuando la incidencia pasa a Cerrada, y la regla de RF-622 debe exigir esa resta en la misma escritura; no se crea una operación aparte.
- La sugerencia de tipo de falla y prioridad con IA («Sugerido: Red · Prioridad media» en el prototipo): es la HU-14; el resumen solo muestra el tipo y la prioridad si ya existen.
- Clasificar a mano el tipo de falla o la prioridad al asignar, o dejar una incidencia sin técnico (desasignar).
- Corregir los problemas 1 a 5 de la revisión del PR #17 (movimientos que suben aunque su incidencia se rechace, rechazos que no cuentan al cerrar sesión o se pierden en silencio, `FAILED_PRECONDITION` y la comparación de `actualizadoEn`): es el PR B, que actualiza la spec 005. Esta HU depende de esas correcciones (RF-621, RF-624).
- Migraciones reales de Room: ninguna entidad cambia en esta HU.

## Criterios de finalización
- RF-601 a RF-625 implementados en los PR B a F, según `plan.md`. De RF-624 se prueba en esta HU lo que no necesita «Iniciar atención»: en el servidor, el rechazo al técnico anterior en el Rules Playground; en el celular, que un cambio rechazado deja la versión del servidor sin movimiento huérfano (mismo camino que RF-621). La prueba completa se repite en la HU-08.
- Pruebas unitarias (JVM): las reglas puras de asignación (carga efectiva, bloqueos, marca al asignar, tipo y texto del movimiento, opciones de la pantalla), el caso de uso de asignar (sin permiso, cerrada, sin número, mismo técnico, límite contando lo no subido, asignación, reasignación desde Asignada o En atención, reasignación desde Esperando repuesto y fallo de Room), el ViewModel de la pantalla, el mapeo de un técnico desde sus datos y el cálculo del cambio de carga (sin cambio, asignación, reasignación, límite y carga bajo 0).
- Pruebas instrumentadas de las consultas de Room nuevas (asignaciones sin subir y borrado de técnicos), corridas en un celular.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde en cada PR.
- Reglas de Firestore probadas en el Rules Playground (asignar, reasignar, exceder el límite, técnico que intenta cambiar el técnico asignado) y publicadas a mano por el usuario, después de verificar en la consola que la carga de cada técnico coincide con sus incidencias pendientes asignadas.
- Prueba manual en celular, con el jefe y un técnico en dos celulares (o en el emulador y un celular): asignar a un técnico disponible y verlo en el filtro del técnico y en su detalle; intentar asignar a uno en su límite; reasignar y comprobar en la consola que la carga de ambos cambió en 1; asignar sin conexión y verlo subir al volver la señal; abrir la pantalla con una incidencia «Por sincronizar» y comprobar el motivo; capturas en claro y oscuro de la pantalla.

- Verificación final (2026-10-12): reglas publicadas a las 9:10; RF-601 a RF-625 comprobados en el emulador y en un celular real (T40 a T43), y pruebas instrumentadas de Room en verde (11). En el Rules Playground, los casos que cambian el técnico (jefe sin mover la carga y técnico que cambia `tecnicoAsignadoId`) dan «error desconocido» porque la regla usa `getAfter()`, que el Playground no simula; se cubren con la asignación real (T41) y con el rechazo del servidor sobre el límite (T42 c). De RF-624 queda probado el rechazo al técnico anterior en el servidor y que un cambio rechazado no deja movimiento huérfano; el caso completo con «Iniciar atención» se prueba en la HU-08.

## Dudas abiertas
Ninguna. Resueltas al aprobar la spec (2026-10-11):
- Reasignar en Esperando repuesto: conserva la marca porque la pieza sigue en camino; desde Asignada o En atención vuelve a Asignada (RF-614, RF-625).
- Carga actual en Firestore: nadie asignó incidencias a mano. Aun así, antes de publicar la regla de RF-622 se verifica en la consola que `incidenciasActivas` de cada técnico coincida con sus incidencias pendientes asignadas.
- Restar la carga al cerrar: queda como dependencia para Alonso en la HU-08 (ver «Fuera de alcance»).
- RF-624: se prueba ahora lo posible (ver «Criterios de finalización») y el resto en la HU-08.
- Texto bajo el botón: «La incidencia aparecerá en la bandeja de <nombre>» (RF-605), sin prometer el aviso de la HU-07.
- También se aprobaron el texto del movimiento con el técnico (RF-615) y contar solo las asignaciones con movimiento de asignación pendiente (RF-602).
