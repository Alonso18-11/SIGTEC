# Spec 014 — Clasificación, prioridad y borrador del informe con IA (HU-14)

> Estado: borrador para revisión (2026-11-15). Las decisiones D-1 a D-10 son propuestas y las dudas abiertas están sin
> responder; el plan y las tareas se escriben después de que se respondan.

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-14
> (RF-1401…). Sale de la planificación (`SIGTEC_Planificación.xlsx`, épica EP-06 «Gestión del conocimiento e
> inteligencia artificial»). Sigue los prototipos de `docs/prototipos/` 07 «Asignar» y 08 «Informe técnico» (6 y 9 en
> `SIGTEC___Prototipos.pdf`). El 04 «Nueva incidencia» y el 09 «Asistente» (4 y 16 en el PDF) se revisaron y quedan
> fuera (D-1 y D-2).

## Contexto y objetivo
Hoy el jefe asigna sin una clasificación de la falla: `Incidencia.tipoFalla` y `Incidencia.prioridad` existen desde
el esqueleto, se suben y se bajan de Firestore, y el detalle y «Asignar» los muestran cuando existen, pero ninguna
pantalla los llena. El técnico, por su parte, escribe el informe de cierre desde cero, campo por campo, aunque ya haya
anotado o dictado lo que hizo.

Con esta HU, Gemini (Firebase AI Logic) propone el tipo de falla y la prioridad a partir de la descripción, y el jefe
la acepta o la corrige al asignar. Al cerrar, el técnico escribe o dicta notas sueltas y Gemini las ordena en un
borrador del informe que él revisa antes de confirmar con su huella. Toda salida de la IA se muestra marcada como
generada automáticamente, nada cambia sin que la persona la use, y se guarda si se aceptó (`sugerencias_ia`). Si Gemini
no responde, la app funciona igual que hoy, sin la sugerencia.

La base ya existe: la dependencia `firebase-ai`, la tabla `sugerencias_ia` (`SugerenciaIAEntity`, `SugerenciaIADao`,
sin uso todavía), `TipoSugerencia` (`CLASIFICACION`, `BORRADOR_INFORME`), el componente `TarjetaSugerenciaIA` con la
etiqueta «IA» y la regla de Firestore de `sugerenciasIA`.

## Usuarios / actores
- Jefe de Soporte y Sistemas (`ASIGNAR_INCIDENCIA`): recibe la clasificación sugerida en «Asignar incidencia» y la
  acepta, la corrige o la descarta.
- Técnico asignado (`ATENDER_INCIDENCIA`): pide el borrador del informe desde sus notas y lo usa o lo descarta.
- Personal de sistemas: no participa (no asigna ni atiende).
- Gemini Developer API, a través de Firebase AI Logic: solo propone; nunca escribe en Room ni en Firestore.
- `core/sync`: sube las decisiones sobre las sugerencias a Firestore (`sugerenciasIA`).

## Historias de usuario
- HU-14: Como jefe y técnico, queremos que la IA proponga el tipo de falla y la prioridad de cada incidencia, y
  redacte el borrador del informe, para recibir la lista ordenada por urgencia y no escribir el informe desde cero.
- Criterio de aceptación de la planificación: a partir de la descripción sugiere hardware, software o red y una
  prioridad; el borrador aparece marcado como generado automáticamente y se corrige antes de guardar; si el servicio
  no responde, la app sigue funcionando sin mostrar la sugerencia. La IA sugiere, nunca decide: el jefe acepta o
  corrige la clasificación y el técnico revisa el borrador redactado a partir de sus notas o dictado. La app guarda si
  cada sugerencia fue aceptada.

## Requisitos funcionales (criterios de aceptación en EARS)

### Pedir la clasificación (prototipo 07, D-2)
- RF-1401: CUANDO el jefe abre «Asignar incidencia» para una incidencia pendiente sin tipo de falla ni prioridad, y en
  este celular no hay una sugerencia de clasificación para ella, EL SISTEMA pide a Gemini un tipo de falla (Hardware,
  Software o Red) y una prioridad (Baja, Media o Alta) a partir de la descripción del problema.
- RF-1402: EL SISTEMA verifica `ASIGNAR_INCIDENCIA` en el dominio (`VerificarPermiso`) antes de pedir la sugerencia; sin
  sesión o sin el permiso no llama a Gemini y devuelve `SinPermiso`.
- RF-1403: EL SISTEMA pide la sugerencia sin bloquear la pantalla: la lista de técnicos, la elección y «Asignar»
  funcionan mientras Gemini responde. Mientras tanto, la tarjeta de la sugerencia muestra «Generando sugerencia» con la
  etiqueta «IA».
- RF-1404: CUANDO Gemini responde con un tipo y una prioridad válidos, EL SISTEMA guarda la sugerencia en Room (tipo
  `CLASIFICACION`, sin decidir) y la muestra en una `TarjetaSugerenciaIA` con el título «Sugerencia de IA», el texto
  del tipo y la prioridad («Red, prioridad media»), la nota «Generada automáticamente. Tú decides la clasificación.» y
  los botones «Descartar» y «Usar».
- RF-1405: SI este celular ya tiene una sugerencia de clasificación para la incidencia, ENTONCES EL SISTEMA muestra esa
  sugerencia (si sigue sin decidir) sin volver a pedirla: como máximo una consulta por incidencia y celular.
- RF-1406: SI no hay conexión, o la respuesta no llega en 15 segundos, o Gemini falla (cuota agotada, servicio caído,
  App Check, D-9), o la respuesta no trae un tipo y una prioridad válidos, ENTONCES EL SISTEMA no muestra la tarjeta ni
  un mensaje de error, no guarda nada y la asignación funciona igual que sin IA. Se vuelve a intentar la próxima vez
  que se abra la pantalla.
- RF-1407: SI la incidencia ya tiene tipo de falla o prioridad (por ejemplo, en una reasignación), ENTONCES EL SISTEMA
  no pide sugerencia.

### Aceptar, corregir o descartar la clasificación (D-3, D-4)
- RF-1408: EL SISTEMA muestra en «Asignar incidencia», debajo del resumen, la sección «Clasificación» con el tipo de
  falla y la prioridad para elegir. Empiezan con los valores que ya tenga la incidencia, o vacíos si no tiene: la
  sugerencia nunca se elige sola.
- RF-1409: CUANDO el jefe toca «Usar», EL SISTEMA pone el tipo y la prioridad sugeridos en «Clasificación» y oculta la
  tarjeta. El jefe puede cambiarlos antes de asignar (corregir).
- RF-1410: CUANDO el jefe toca «Descartar», EL SISTEMA oculta la tarjeta y no cambia «Clasificación».
- RF-1411: Clasificar es opcional (D-3): «Asignar» se habilita con las mismas condiciones de la HU-06, aunque el tipo o
  la prioridad estén vacíos.
- RF-1412: CUANDO el jefe asigna, EL SISTEMA guarda el tipo de falla y la prioridad elegidos en la incidencia, en la
  misma transacción de Room que la asignación y su movimiento, con `pendienteSincronizar = true`. Si la asignación
  falla (por ejemplo, `LimiteAtencionAlcanzado`), no guarda la clasificación.
- RF-1413: CUANDO la asignación se guarda y había una sugerencia sin decidir, EL SISTEMA registra la decisión (D-4):
  aceptada solo si el jefe tocó «Usar» y asignó exactamente el tipo y la prioridad sugeridos; no aceptada si la
  descartó, si cambió algún valor después de usarla o si asignó sin usarla. Guarda quién decidió y cuándo.
