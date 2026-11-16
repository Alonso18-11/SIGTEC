# Spec 014 — Clasificación, prioridad y borrador del informe con IA (HU-14)

> Estado: aprobada (2026-11-15), RF-1401 a RF-1437. Decisiones D-1 a D-11 aprobadas por Nina el 2026-11-15 y dudas
> respondidas (ver «Decisiones» y «Dudas abiertas»). El cómo está en `plan.md` y las tareas en `tasks.md`.

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

### Redactar el borrador del informe (prototipo 08, D-5)
- RF-1414: EL SISTEMA muestra en «Informe técnico», antes de los campos del informe, el campo «Mis notas» con la ayuda
  «Escribe o dicta notas sueltas; la IA las ordena», el micrófono del dictado de la HU-10 y el botón «Redactar borrador
  con IA». Las notas no forman parte del informe: no se guardan en Room ni se suben.
- RF-1415: MIENTRAS «Mis notas» esté vacío, no haya conexión, se esté redactando un borrador o se esté cerrando la
  incidencia, EL SISTEMA mantiene deshabilitado «Redactar borrador con IA». Sin conexión, debajo del botón dice «La IA
  necesita conexión a internet.».
- RF-1416: CUANDO el técnico toca «Redactar borrador con IA», EL SISTEMA verifica `ATENDER_INCIDENCIA` en el dominio y
  que sea el técnico asignado a la incidencia y que siga pendiente; si no, no llama a Gemini y devuelve `SinPermiso`.
- RF-1417: EL SISTEMA envía a Gemini la descripción del problema, las notas y lo que ya esté escrito en los cuatro
  campos del informe, y le pide en español el diagnóstico, el trabajo realizado, las piezas cambiadas y las
  observaciones, sin agregar datos que no estén en lo enviado.
- RF-1418: MIENTRAS Gemini redacta, EL SISTEMA muestra la tarjeta con «Redactando borrador» y la etiqueta «IA»; los
  campos del informe se pueden seguir editando.
- RF-1419: CUANDO Gemini responde con al menos el diagnóstico y el trabajo realizado, EL SISTEMA guarda el borrador en
  Room (tipo `BORRADOR_INFORME`, sin decidir) y lo muestra en una `TarjetaSugerenciaIA` con el título «Borrador
  generado automáticamente», sus campos con nombre («Diagnóstico: …») y los botones «Descartar» y «Usar borrador».
- RF-1420: CUANDO el técnico toca «Usar borrador», EL SISTEMA reemplaza cada campo del informe por el del borrador (un
  campo que el borrador trae vacío deja el campo como estaba), oculta la tarjeta y registra el borrador como aceptado,
  con quién decidió y cuándo. Debajo de los campos aparece «Texto redactado por IA: revísalo antes de confirmar el
  cierre.» hasta que la incidencia se cierre.
- RF-1421: CUANDO el técnico toca «Descartar», EL SISTEMA oculta la tarjeta, no cambia los campos y registra el
  borrador como no aceptado.
- RF-1422: SI el técnico pide otro borrador mientras el anterior sigue sin decidir, ENTONCES EL SISTEMA registra el
  anterior como no aceptado y muestra solo el nuevo. Cada borrador es una sugerencia aparte.
- RF-1423: SI no hay conexión, o la respuesta no llega en 30 segundos, o Gemini falla, o la respuesta no trae el
  diagnóstico y el trabajo realizado, ENTONCES EL SISTEMA no guarda nada, no cambia los campos y muestra «No se pudo
  redactar el borrador. Puedes escribir el informe a mano.». El informe se completa y se cierra igual que sin IA.
- RF-1424: CUANDO el técnico vuelve a «Informe técnico» de una incidencia que tiene un borrador sin decidir, EL SISTEMA
  muestra ese borrador otra vez, sin pedir uno nuevo.
- RF-1425: El borrador nunca cierra la incidencia ni llena el informe por sí solo: el cierre sigue exigiendo el
  diagnóstico y la confirmación biométrica de la HU-08 (RF-23 del producto) con lo que esté en los campos.
- RF-1426: CUANDO se confirma el cierre con un borrador sin decidir, EL SISTEMA lo registra como no aceptado.

### Guardar y subir la decisión (D-7, D-8)
- RF-1427: Cada sugerencia guarda la incidencia, su tipo, el contenido tal como se mostró, el modelo de Gemini que la
  generó y cuándo se generó; al decidirse, si se aceptó, quién decidió y cuándo; y `actualizadoEn`, como toda entidad.
- RF-1428: La IA nunca cambia datos por su cuenta: la incidencia y el informe solo cambian cuando la persona toca
  «Usar» y después asigna o confirma el cierre (principio 5 de la constitución).
- RF-1429: EL SISTEMA guarda las sugerencias y sus decisiones solo en Room, con `pendienteSincronizar = true` al
  decidirse. `core/sync` sube a `sugerenciasIA` solo las sugerencias ya decididas, una vez cada una; las que siguen sin
  decidir no se suben. No se bajan a otros celulares.
- RF-1430: Firestore acepta crear una sugerencia solo con sesión, a nombre de quien la decidió y con `aceptada`
  verdadero o falso; no acepta cambiarla ni borrarla.
- RF-1431: Las sugerencias sin subir no se suman a «registros sin enviar» (D-8). Al cerrar sesión o al entrar otra
  persona se borran con los demás datos locales.

### Asistente (D-1)
- RF-1432: EL SISTEMA no ofrece «Abrir asistente» en el menú del detalle de la incidencia. Hoy esa opción navega a
  `Ruta.Asistente`, que no tiene pantalla desde #144, y el asistente queda fuera de esta HU. `Ruta.Asistente` se
  borra de `Rutas.kt` en su propio PR.

### Ordenar por urgencia (D-11)
- RF-1433: EL SISTEMA ofrece en «Incidencias» dos órdenes: «Más recientes», el de la HU-04 y el que aparece al abrir
  la pantalla, y «Más urgentes».
- RF-1434: CUANDO se elige «Más urgentes», EL SISTEMA muestra primero las pendientes, por prioridad (Alta, Media, Baja
  y al final las que no tienen) y, dentro de cada prioridad, de la más antigua a la más reciente; después, las
  solucionadas, de la más reciente a la más antigua.
- RF-1435: EL SISTEMA aplica el orden después de la búsqueda y los filtros de la HU-04; limpiar los filtros no cambia
  el orden, y el orden se conserva al rotar el celular mientras la pantalla siga abierta.
- RF-1436: El orden usa solo la prioridad guardada en la incidencia, la que eligió el jefe; nunca una sugerencia sin
  decidir (principio 5 de la constitución).
- RF-1437: MIENTRAS el orden sea «Más urgentes», cada fila de una incidencia pendiente muestra su prioridad
  («Prioridad alta», «Prioridad media», «Prioridad baja» o «Sin prioridad»), con texto y no solo con color.

## Requisitos no funcionales
- IA que solo sugiere (principio 5 de la constitución y `CLAUDE.md`): toda salida de Gemini se muestra con la etiqueta
  «IA» y el texto «generada/generado automáticamente», y requiere que la persona la use, la corrija o la descarte.
- Costo cero: Gemini Developer API a través de Firebase AI Logic, que funciona en el plan Spark, con un modelo estable
  que no exige facturación (D-6). Sin Cloud Functions ni servicios nuevos.
- Claves: ninguna en el código ni en el repo. Firebase AI Logic autoriza las llamadas con su propia cuenta de servicio
  («Firebase AI Logic Service Agent»); no se usa ni se crea una clave de Gemini, y no se usa Retrofit para Gemini.
- Cuota: la del nivel gratuito es por proyecto (todos los celulares la comparten) y se reinicia a medianoche, hora del
  Pacífico. Por eso la clasificación se pide una sola vez por incidencia y celular (RF-1405) y el borrador solo cuando
  el técnico lo pide.
- Privacidad: a Gemini solo se envía el texto del problema (descripción, notas y campos del informe). No se envían
  nombres, correos, el responsable ni su área, el técnico ni el código del equipo. En el nivel gratuito, Google puede
  usar lo enviado para mejorar sus productos y personas pueden revisarlo, así que se prueba solo con datos de prueba.
- Respuestas: se pide JSON con un esquema fijo (tipo y prioridad como valores cerrados; los cuatro campos del borrador
  como texto) y en español. El dominio valida la respuesta y recorta cada campo a un largo máximo antes de mostrarla.
- Rendimiento: las llamadas no bloquean la pantalla (RF-1403, RF-1418); límite de 15 segundos para la clasificación y
  30 para el borrador.
- Dependencias: ninguna nueva para Gemini (`firebase-ai` ya está en el BoM 34.19.0). App Check depende de D-9.
- Persistencia: cambia `SugerenciaIAEntity` (y `SugerenciaIA` en `core/domain/model`) con los campos de RF-1427; sube
  `version` de `SigtecDatabase` de 6 a 7 y se dice en el PR (migración destructiva: se pierde lo no sincronizado).
- Firestore: cambia la regla de `sugerenciasIA` (RF-1430) y se publica a mano en la consola después de revisarla en el PR.
- Diseño: prototipos 07 y 08 con los componentes v2: `TarjetaSugerenciaIA`, `EtiquetaIA`, los selectores de
  «Clasificación» con `PestanasSegmentadas` (como el tipo en «Registrar falla») y `CampoTexto` con el micrófono para
  «Mis notas». Solo roles del tema; vistas previas en claro y oscuro de cada estado (generando, con sugerencia, sin
  sugerencia, borrador usado). El orden de «Incidencias» es un chip desplegable más (`FiltroDesplegable`, como
  «Etapa», «Técnico» y «Equipo»), y la prioridad de cada fila es un parámetro opcional de `FilaIncidencia`. Textos sin «·».
- Pruebas: la validación de las respuestas son funciones puras con pruebas JVM; los casos de uso, con un doble del
  cliente de IA y del repositorio; los ViewModels, con `MainDispatcherRule`. Ninguna prueba llama a Gemini.

## Casos límite
- Descripción muy corta o vaga («no prende»): Gemini igual propone; si no trae valores válidos, no hay tarjeta
  (RF-1406).
- Gemini devuelve un tipo o una prioridad fuera de los permitidos, o texto en lugar de JSON: se trata como falla
  (RF-1406, RF-1423).
- El jefe abre «Asignar» sin conexión: no hay tarjeta; si vuelve con conexión, se pide entonces (RF-1406).
- El jefe usa la sugerencia y cambia solo la prioridad: se asigna con su corrección y la sugerencia queda no aceptada
  (RF-1413).
- El jefe descarta la sugerencia y elige a mano los mismos valores: queda no aceptada, porque la descartó (RF-1413).
- El jefe sale de «Asignar» sin asignar: la sugerencia queda sin decidir y se muestra al volver (RF-1405).
- La asignación falla por el límite de atención: no se guarda la clasificación y la sugerencia sigue sin decidir
  (RF-1412).
- Reasignar una incidencia ya clasificada: no se pide sugerencia y se ve la clasificación actual, que se puede cambiar
  (RF-1407, RF-1408).
- El técnico rota el celular mientras se redacta el borrador: el `ViewModel` sigue y el borrador llega igual.
- El técnico usa el borrador y después edita los campos: es lo esperado; el borrador queda aceptado y el informe se
  guarda con lo que él dejó (RF-1420, RF-1425).
- El técnico escribe un nombre en sus notas: se envía tal cual (RNF de privacidad); la app no agrega datos personales.
- Gemini inventa una pieza que no aparece en las notas: el técnico la corrige antes de confirmar (RF-1417, RF-1420).
- Cuota agotada, servicio caído o App Check sin configurar (D-9): todo funciona sin IA (RF-1406, RF-1423).
- Cerrar sesión con sugerencias sin subir: se borran con los datos locales; la asignación o el cierre que las decidió
  ya se cuenta en «registros sin enviar» (RF-1431, D-8).

## Fuera de alcance
- El asistente conversacional (prototipo 09, 16 del PDF) y su pestaña (D-1).
- La sugerencia en «Nueva incidencia» (prototipo 04, 4 del PDF) (D-2).
- Usar Gemini para las «Fallas parecidas» de la HU-13, que siguen calculándose en el celular.
- Clasificar fuera de «Asignar incidencia» (por ejemplo, desde el detalle).
- Bajar las sugerencias a otros celulares, y mostrar en el tablero cuántas se aceptaron.
- Guardar las notas del técnico.
- Cambiar el orden por defecto de «Incidencias» (HU-04) o el de «Mis tareas» (HU-08): la urgencia es una opción más
  de «Incidencias» (D-11).

## Configuración en la consola de Firebase
La hace Nina cuando el plan lo pida (antes de la primera prueba en el celular con Gemini), con la opción (a) de App
Check (D-9).

**Estado al 2026-11-16 (Nina):** el proyecto `sigtec` está en el plan Spark; AI Logic quedó activado con la Gemini
Developer API (nivel gratuito) en ese mismo proyecto (pasos 1 y 2), y App Check para AI Logic quedó «Sin aplicar» hasta
el 2027-01-21 (paso 3, opción a). Falta el paso 5: publicar la regla de `sugerenciasIA` al fusionar #170. Antes del
2027-01-21 hay que decidir la opción (b) o volver a aplazar la fecha; si no, AI Logic rechaza toda llamada de la app.
1. Consola de Firebase → el proyecto → AI Services → AI Logic → «Get started».
2. Elegir «Gemini Developer API» (no «Agent Platform Gemini API», que exige el plan Blaze). El flujo habilita
   `generativelanguage.googleapis.com` y `firebasevertexai.googleapis.com`. Desde mediados de 2026 ya no crea una clave
   de Gemini: autoriza con la cuenta de servicio de Firebase AI Logic. No se agrega nada a la app ni cambia
   `google-services.json`.
3. App Check (D-9). El flujo deja App Check exigida para AI Logic, y sin App Check en la app toda llamada se rechaza.
   - Opción (a): Security → App Check → pestaña «APIs» → fila «Firebase AI Logic» → «Enforces:» (lápiz) → una fecha
     no posterior al 2027-01-21 → «Save».
   - Opción (b): Security → App Check → pestaña «Apps» → registrar la app con Play Integrity (huella SHA-256 del
     certificado de firma) y, por cada celular de prueba, «Manage debug tokens» con el token que muestra Logcat.
4. Revisar la cuota gratuita del proyecto en `aistudio.google.com/rate-limit`: es por proyecto y se reinicia a
   medianoche, hora del Pacífico. AI Logic limita además cada usuario a 100 solicitudes por minuto (cuota de «Firebase
   AI Logic API» en la consola de Google Cloud); no hace falta cambiarla.
5. Al fusionar el PR de la regla de `sugerenciasIA`, publicarla en Firestore → Reglas.

## Criterios de finalización
- RF-1401 a RF-1437 implementados.
- Pruebas JVM: la validación de la clasificación y del borrador (JSON válido, valores fuera de los permitidos, campos
  vacíos, texto en lugar de JSON, recorte); `SugerirClasificacion` (sin sesión, sin permiso, ya clasificada, ya
  sugerida, falla de Gemini, camino feliz); `RedactarBorradorInforme` (sin permiso, otro técnico, incidencia cerrada,
  falla, borrador anterior sin decidir, camino feliz); la decisión de una sugerencia; `AsignarIncidencia` con la
  clasificación; `AsignacionViewModel` e `InformeTecnicoViewModel` (generando, con sugerencia, sin sugerencia, usar,
  descartar, corregir, decisión al asignar o al cerrar, sin conexión) con `MainDispatcherRule`; el orden por urgencia
  (prioridades, sin prioridad, empates por antigüedad, solucionadas al final, combinado con filtros) y
  `ListaIncidenciasViewModel` con el orden elegido.
- `SugerenciaIADaoTest` (nuevo) pasa en un celular y `version` subió a 7.
- `firestore.rules` revisada en el PR, probada en el Rules Playground (crear a nombre propio con `aceptada`: permite;
  a nombre de otro, sin `aceptada`, actualizar o borrar: rechaza) y publicada a mano.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- Prueba manual en un celular real, con capturas en claro y oscuro:
  - Como jefe, abrir «Asignar» en una incidencia sin clasificar, ver «Generando sugerencia» y luego la sugerencia;
    usarla, corregir la prioridad y asignar; ver la clasificación en el detalle y la sugerencia no aceptada en Firestore.
  - Repetir usando la sugerencia sin cambios (aceptada) y descartándola (no aceptada).
  - En modo avión, abrir «Asignar»: no hay tarjeta y la asignación funciona.
  - Como técnico, dictar notas, redactar el borrador, usarlo, corregir un campo y cerrar con huella; ver el informe y
    la sugerencia aceptada en Firestore. Descartar otro borrador y cerrar escribiendo a mano.
  - Sin conexión, ver «La IA necesita conexión a internet.» bajo el botón del borrador.
  - Comprobar que el detalle ya no ofrece «Abrir asistente».
  - En «Incidencias», elegir «Más urgentes» y ver primero las de prioridad alta con su prioridad en la fila; combinarlo
    con un filtro, rotar el celular y volver a «Más recientes».
- La línea de la HU-14 en `MEMORY.md` dice «completa».

## Decisiones
Todas aprobadas por Nina el 2026-11-15.
- D-1 · Asistente conversacional (RF-1432). **Aprobada:** fuera de la HU-14, y se quita «Abrir asistente» del detalle,
  que hoy navega a una ruta sin pantalla. No está en el criterio de la planificación; multiplica las llamadas contra
  una cuota gratuita que comparte todo el proyecto; necesita una pestaña nueva, historial de la conversación y decidir
  qué se guarda; y lo que muestra el prototipo ya lo cubren las «Fallas parecidas» de la HU-13 («¿Qué pruebo
  primero?») y el borrador de esta HU («Redacta el informe»). Alternativa: hacerlo como bloque aparte al final, si
  sobra tiempo.
- D-2 · Dónde se pide la clasificación (RF-1401, RF-1405). **Aprobada:** en «Asignar», en el celular del jefe, una
  sola vez por incidencia. Quien decide es quien la ve, no hay que bajar sugerencias de otros celulares y una incidencia
  registrada sin conexión igual la recibe. Alternativa descartada: pedirla al registrar (prototipo 04), que gasta una
  consulta en cada registro y obliga a bajar las sugerencias al celular del jefe.
- D-3 · Clasificar al asignar (RF-1411). **Aprobada:** opcional. No cambia las reglas de la HU-06 ni bloquea la
  asignación si Gemini falla. Alternativa: exigir el tipo y la prioridad para asignar.
- D-4 · Cuándo una clasificación cuenta como aceptada (RF-1413). **Aprobada:** solo con «Usar» y asignando sin
  cambios. Alternativa: comparar lo asignado con lo sugerido sin mirar los botones (descartar y elegir lo mismo
  contaría como aceptada).
- D-5 · Cómo se pide el borrador (RF-1414 a RF-1426). **Aprobada:** a pedido, desde «Mis notas» (como el prototipo 9
  del PDF), y «Usar borrador» reemplaza los campos que el borrador trae. Las notas no se guardan. Alternativa: pedirlo
  solo al abrir el informe, que gasta cuota antes de que haya notas.
- D-6 · Modelo (RNF de costo). **Aprobada:** `gemini-3.5-flash-lite`, en una sola constante. Al 2026-11-15 es estable,
  no exige facturación, no tiene fecha de retiro publicada y es el modelo al que Google redirige `gemini-2.5-flash`
  desde el 2026-12-01. Alternativa: `gemini-3.8-flash`, también estable y sin facturación, más capaz y probablemente
  con menos cuota gratuita.
- D-7 · Qué se sube (RF-1429, RF-1430). **Aprobada:** solo las sugerencias decididas, una vez, sin bajarlas; la regla
  de `sugerenciasIA` pasa a «solo crear», como los movimientos. La regla actual (actualizar solo `aceptada`) chocaría
  con `actualizadoEn`. Alternativa: subirlas al generarse y actualizarlas al decidir.
- D-8 · «Registros sin enviar» (RF-1431). **Aprobada:** no contar las sugerencias: la asignación o el cierre que las
  decide ya se cuenta, y «1 sugerencia sin enviar» no le dice nada a quien cierra sesión.
- D-9 · App Check (RF-1406). **Aprobada:** opción (a) de la configuración (aplazar la exigencia hasta el 2027-01-21
  como máximo) para desarrollar y probar ya, y decidir la (b) antes de publicar en Google Play. La (b) agrega dos
  dependencias (`firebase-appcheck-playintegrity` y `firebase-appcheck-debug`) en su propio PR (principio 1) y cambia
  `SigtecApp.kt`. La (c), dejarla «sin exigir (solo monitoreo)», la documentación la desaconseja.
- D-10 · Dónde vive el código (para el plan). **Aprobada:** un módulo nuevo `feature/ia/{domain,data,di}` con el
  cliente de Gemini, la validación de las respuestas, el repositorio de sugerencias y los casos de uso; «Asignar» e
  «Informe técnico» los usan, como el detalle usa los del diccionario. Ámbito de commit `ia`, que hay que agregar a
  la lista de `CLAUDE.md`. Alternativa: repartirlo entre `asignacion` y `atencion`, con el cliente de Gemini en `core`.
- D-11 · Lista por urgencia (RF-1433 a RF-1437). **Aprobada:** el orden «Más urgentes» como opción de «Incidencias»,
  sin cambiar el orden por defecto de la HU-04 ni el de «Mis tareas» de la HU-08, que fijan sus propios criterios.
  Alternativa descartada: dejarlo fuera, porque es el «para» de la historia.

## Dudas abiertas
Ninguna. Respondidas por Nina el 2026-11-15:
- App Check: se aplaza la exigencia (D-9, opción a); antes de publicar en Google Play se decide si se agrega a la app.
- Orden por urgencia: sí, en sus propios PR (D-11, RF-1433 a RF-1437).
- Consola: Nina activa Firebase AI Logic y publica la regla cuando el plan lo pida.
- Módulo `feature/ia` con ámbito `ia`: sí (D-10).
- `Ruta.Asistente`: se borra de `Rutas.kt` en su propio PR (RF-1432).
