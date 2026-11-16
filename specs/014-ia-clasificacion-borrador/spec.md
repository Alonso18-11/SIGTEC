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
