# Spec 013 — Diccionario de fallas con sugerencias (HU-13)

> Estado: aprobada (2026-11-09), RF-1301 a RF-1331. Decisiones D-1 a D-5 respondidas por Alonso el 2026-11-09 (ver «Decisiones»).

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-13 (RF-1301…). Sale de la planificación (`SIGTEC_Planificación (1).xlsx`, épica EP-06 «Gestión del conocimiento e inteligencia artificial»). Sigue el prototipo 14 «Diccionario de fallas».

## Contexto y objetivo
Hoy, lo que un técnico aprende al resolver una falla se queda en su informe: el siguiente que encuentra el mismo problema vuelve a diagnosticarlo desde cero. La tabla `fallas_diccionario` y la colección `fallas` de Firestore existen desde el esqueleto, pero ninguna pantalla las usa y no se sincronizan.

Con esta HU, el técnico registra una falla con su solución (desde el diccionario o justo después de cerrar una incidencia), cualquiera del área que tenga acceso la busca por síntoma, componente o palabra clave, y al abrir una incidencia pendiente la app muestra las tres fallas registradas más parecidas a su descripción, marcadas como sugerencia. Así el técnico llega al escritorio del usuario con una hipótesis.

## Usuarios / actores
- Técnico (`REGISTRAR_FALLA` y `CONSULTAR_DICCIONARIO`): registra fallas, las busca y ve las sugerencias.
- Personal de sistemas (`CONSULTAR_DICCIONARIO`): busca y ve las sugerencias, pero no registra (regla de `CLAUDE.md`).
- Jefe: no tiene acceso al diccionario ni ve sugerencias (regla de `CLAUDE.md`).

## Historias de usuario
- HU-13: Como técnico, quiero registrar y buscar fallas con su solución, y que el sistema me sugiera las más parecidas al abrir una incidencia, para no diagnosticar dos veces lo mismo.
- Criterio de aceptación de la planificación: la falla se guarda con título, síntomas, componente y solución; la búsqueda responde en menos de 3 segundos; se muestran las 3 fallas más parecidas con su solución, marcadas como sugerencia.

## Requisitos funcionales (criterios de aceptación en EARS)

### Ver y buscar el diccionario (prototipo 14)
- RF-1301: CUANDO alguien con `CONSULTAR_DICCIONARIO` abre la pestaña «Diccionario», EL SISTEMA muestra «Diccionario de fallas», cuántas fallas hay registradas y la lista de fallas ordenada por título, leída de Room.
- RF-1302: Cada falla de la lista muestra su tipo (Hardware, Software o Red) y su componente como etiquetas, el título, «Solución: …» y «Registrada por {autor}».
- RF-1303: CUANDO la persona toca una falla, EL SISTEMA la despliega y muestra también sus síntomas y la fecha de registro; al tocarla otra vez, se pliega.
- RF-1304: CUANDO la persona escribe en el buscador, EL SISTEMA filtra la lista por coincidencia en el título, los síntomas, el componente o la solución, sin distinguir mayúsculas ni tildes, y con todas las palabras escritas (cada una puede estar en un campo distinto).
- RF-1305: CUANDO la persona elige un filtro (Todas, Hardware, Software, Red), EL SISTEMA muestra solo las fallas de ese tipo; el filtro se combina con el buscador.
- RF-1306: EL SISTEMA responde a la búsqueda y al filtro en menos de 3 segundos con el diccionario completo del celular (criterio de la HU), sin consultar la red.
- RF-1307: SI no hay fallas registradas, ENTONCES EL SISTEMA muestra «Aún no hay fallas en el diccionario.» y, a quien puede registrar, «Registra la primera con el botón «Registrar falla».». SI la búsqueda o el filtro no encuentran nada, ENTONCES muestra «No hay fallas que coincidan con la búsqueda.».
- RF-1308: SI la persona no tiene `CONSULTAR_DICCIONARIO` (el jefe), ENTONCES EL SISTEMA no le muestra la pestaña y el caso de uso devuelve `SinPermiso` aunque llegue a la pantalla.

### Registrar una falla
- RF-1309: MIENTRAS la persona tiene `REGISTRAR_FALLA`, EL SISTEMA muestra en el diccionario el botón flotante «Registrar falla» (en acento, prototipo 14); al personal de sistemas no se lo muestra.
- RF-1310: CUANDO el técnico toca «Registrar falla», EL SISTEMA abre «Registrar falla» con los campos Título, Tipo (Hardware, Software o Red), Componente, Síntomas y Solución, todos obligatorios.
- RF-1311: EL SISTEMA ofrece dictar los síntomas y la solución con «Dictar» (HU-10), que agrega el texto al final de lo escrito.
- RF-1312: MIENTRAS falte algún campo obligatorio (vacío o solo espacios), EL SISTEMA deja deshabilitado «Guardar falla».
- RF-1313: CUANDO el técnico toca «Guardar falla», EL SISTEMA verifica el permiso `REGISTRAR_FALLA`, guarda la falla en Room con un id nuevo, los textos sin espacios en los extremos, el autor (id y nombre de la sesión), la fecha y hora del celular y `pendienteSincronizar = true`, y vuelve al diccionario con el aviso «Falla registrada en el diccionario».
- RF-1314: SI el técnico no tiene `REGISTRAR_FALLA` o no hay sesión, ENTONCES EL SISTEMA no guarda nada y muestra el error con `error.mensaje()`.
- RF-1315: Registrar funciona sin conexión: la falla aparece de inmediato en el diccionario de ese celular y se sube cuando vuelve la red (RF-1321).

### Registrar desde una incidencia cerrada (D-3)
- RF-1316: MIENTRAS una incidencia está Solucionada, su informe está en el celular y quien la ve tiene `REGISTRAR_FALLA`, EL SISTEMA muestra en la sección del informe del detalle el botón «Guardar en el diccionario».
- RF-1317: CUANDO el técnico toca «Guardar en el diccionario», EL SISTEMA abre «Registrar falla» con el título igual a la descripción de la incidencia, los síntomas igual al diagnóstico del informe, la solución igual a la solución aplicada y el tipo de la incidencia si lo tiene; el componente queda vacío. Todo se puede editar antes de guardar.
- RF-1318: CUANDO se guarda una falla abierta desde una incidencia, EL SISTEMA guarda también el id de esa incidencia (`incidenciaOrigenId`).
- RF-1319: SI ya hay en el diccionario una falla con `incidenciaOrigenId` igual a esa incidencia, ENTONCES EL SISTEMA muestra en el detalle «Ya está en el diccionario» en lugar del botón.

### Sincronizar (core/sync)
- RF-1320: EL SISTEMA guarda cada falla en Firestore, en `fallas/{id}`, con título, síntomas, componente, tipo, solución, autor (id y nombre), `incidenciaOrigenId` (o nulo) y fecha de registro.
- RF-1321: CUANDO hay fallas con `pendienteSincronizar = true` y hay conexión, EL SISTEMA las sube con el mismo trabajo de WorkManager que las incidencias y, al confirmarse la subida, las marca como sincronizadas.
- RF-1322: EL SISTEMA baja en tiempo real las fallas de Firestore a Room, como las demás colecciones. Las fallas no se editan (D-4): una falla que ya está en Room se reemplaza con la de Firestore solo si no tiene cambios sin subir (`debeReemplazarLocal`).
- RF-1331: EL SISTEMA suma las fallas sin subir a los «registros sin enviar» que cuenta el aviso de cerrar sesión o de ingresar con otra cuenta (HU-01, RF-13) y el aviso de conexión (HU-05), para que no se pierdan sin señal (D-5).
- RF-1323: Las reglas de Firestore solo dejan crear una falla a un técnico cuyo id sea el autor, con todos los campos obligatorios no vacíos, el tipo válido y sin campos extra; nadie la edita ni la borra.

### Sugerir fallas parecidas (D-1, D-2)
- RF-1324: MIENTRAS una incidencia está Pendiente y quien la ve tiene `CONSULTAR_DICCIONARIO`, EL SISTEMA muestra en su detalle la sección «Fallas parecidas» con hasta 3 fallas del diccionario, de la más parecida a la menos.
- RF-1325: EL SISTEMA calcula el parecido en el celular, sin red ni IA (D-1), comparando las palabras de la descripción de la incidencia con las del título, los síntomas y el componente de cada falla:
  - normaliza el texto (minúsculas, sin tildes ni signos) y descarta palabras de menos de 3 letras y palabras vacías del español («el», «de», «que», «con», «para», «una»…);
  - reduce los plurales simples («impresoras» → «impresora», «luces» → «luz»);
  - suma por cada palabra en común un peso por campo (título 3, componente 2, síntomas 1), multiplicado por lo poco común que es la palabra en el diccionario (una palabra que aparece en todas las fallas casi no cuenta);
  - si dos fallas empatan, va primero la de título alfabéticamente menor.
- RF-1326: EL SISTEMA solo sugiere fallas con al menos una palabra en común; SI ninguna se parece, ENTONCES la sección muestra «No hay fallas parecidas en el diccionario.».
- RF-1327: Cada falla sugerida muestra la etiqueta «Sugerencia», su título, su componente y «Solución: …» completa, sin tener que tocarla (criterio de la HU).
- RF-1328: EL SISTEMA recalcula las sugerencias cuando cambia el diccionario (por ejemplo, llega una falla nueva por la sincronización) o la descripción de la incidencia; el cálculo no bloquea la pantalla.
- RF-1329: Las sugerencias no cambian ningún dato: no se guardan, no se suben y no modifican la incidencia (no es IA, así que no hay aceptar ni descartar, ver D-1).
- RF-1330: EL SISTEMA no muestra «Fallas parecidas» en incidencias Solucionadas ni a quien no tiene `CONSULTAR_DICCIONARIO`.

## Requisitos no funcionales
- Idioma: textos de UI y mensajes en español, sin «·».
- Rendimiento: búsqueda, filtro y sugerencias en memoria sobre lo que hay en Room, fuera del hilo principal; con 500 fallas siguen por debajo de 3 segundos (prueba JVM con 500 fallas generadas).
- Sin conexión: ver, buscar, registrar y recibir sugerencias funcionan sin conexión; solo la subida espera a la red.
- Costo cero: sin Gemini ni servicios nuevos (D-1); sin dependencias nuevas.
- Diseño: el prototipo 14 con los componentes v2: título grande sobre el fondo con el contador debajo, buscador en píldora blanca (como la lista de incidencias), `FiltrosChips`, `Tarjeta` por falla y `FabExtendido` «Registrar falla» en acento. «Registrar falla» usa `BarraSuperior`, `CampoTexto`, `PestanasSegmentadas` para el tipo, el micrófono en acento y `BarraAccionInferior` con «Guardar falla». «Fallas parecidas» usa `Tarjeta` y una etiqueta «Sugerencia» con texto (no solo color). Vistas previas en claro y oscuro de cada estado.
- Pruebas: el cálculo de parecido y el filtro son funciones puras con pruebas JVM; los casos de uso y los ViewModels, con dobles y `MainDispatcherRule`.
- Persistencia: cambia `FallaDiccionarioEntity` (`autorId`, `incidenciaOrigenId`, `actualizadoEn`, que cumple la regla de `CLAUDE.md` «toda entidad tiene `pendienteSincronizar` y `actualizadoEn`»); sube `version` de `SigtecDatabase` de 5 a 6 y se dice en el PR (migración destructiva: se pierde lo no sincronizado al actualizar).
- Firestore: cambia la regla `create` de `fallas` (RF-1323) y se publica a mano en la consola después de revisarla en el PR.

## Casos límite
- El diccionario aún no bajó (primer inicio sin red): la lista y las sugerencias muestran su estado vacío; aparecen solas cuando llega la sincronización (RF-1328).
- Descripción de la incidencia de una sola palabra o con solo palabras vacías: no hay palabras para comparar; la sección muestra «No hay fallas parecidas en el diccionario.».
- Dos técnicos registran la misma falla: quedan dos fallas (no se detectan duplicados).
- «Guardar en el diccionario» en una incidencia cerrada en otro celular: el informe no está en este celular (la HU-08 no baja informes), así que el botón no aparece.
- Se registra una falla desde una incidencia y luego se abre ese detalle sin conexión: ya muestra «Ya está en el diccionario» porque la falla está en Room (RF-1319).
- Cerrar sesión con fallas sin subir: el aviso «tienes N registros sin enviar» de la HU-01 las cuenta (RF-1331); si se confirma, se borran con los datos locales, como las incidencias.
- Palabras con «ñ» o tildes («bateria» y «batería»): coinciden porque se normalizan (RF-1325).
- Una falla muy larga: la tarjeta de la lista muestra la solución completa; el detalle se despliega sin límite de líneas.

## Fuera de alcance
- Editar o borrar fallas (D-4).
- Usar Gemini para elegir o reordenar las sugerencias: es de la HU-14.
- Sugerencias mientras se escribe la descripción en «Nueva incidencia» (D-2).
- El asistente del prototipo 9 («Asistente de diagnóstico», ruta `Asistente`): es de la HU-14.
- Bajar los informes de otros celulares para «Guardar en el diccionario»: es de la HU-16.
- Valorar fallas («me sirvió») o contar cuántas veces se usaron.
- Cargar fallas de ejemplo: se registran desde la app al probar.

## Criterios de finalización
- RF-1301 a RF-1331 implementados.
- Pruebas unitarias (JVM): `fallasParecidas` (normalización, palabras vacías, plurales, pesos, palabras comunes, empate, máximo 3, sin coincidencias, 500 fallas en menos de 3 s); `filtrarFallas` (palabras en campos distintos, tildes, tipo, combinados); `RegistrarFalla` (sin sesión, sin permiso, personal de sistemas, campos vacíos, camino feliz con origen); `ObservarDiccionario` (sin permiso, jefe); los ViewModels del diccionario, del registro y las sugerencias del detalle, con dobles y `MainDispatcherRule`.
- `FallaDiccionarioDaoTest` (nuevo) pasa en un celular y `version` subió a 6.
- `firestore.rules` revisada en el PR y publicada a mano; probada en el Rules Playground (técnico autor: permite; personal de sistemas, autor distinto, campo vacío o campo extra: rechaza).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- Prueba manual en un celular real, con capturas en claro y oscuro:
  - Como técnico, registrar 3 o 4 fallas (una dictada) y verlas en la lista; buscar con y sin tildes; filtrar por tipo.
  - Registrar una en modo avión y ver que se sube al volver la red (Firestore).
  - Abrir una incidencia pendiente con una descripción parecida y ver las sugerencias con su solución.
  - Cerrar una incidencia, usar «Guardar en el diccionario» y ver «Ya está en el diccionario».
  - Como personal de sistemas, ver el diccionario sin el botón «Registrar falla» y las sugerencias en el detalle.
  - Como jefe, comprobar que no hay pestaña ni sugerencias.
- La línea de la HU-13 en `MEMORY.md` dice «completa».

## Decisiones
- D-1 · Cómo se eligen las fallas parecidas (RF-1324 a RF-1329). **Aprobada el 2026-11-09:** en el celular, comparando palabras con pesos por campo y por rareza, sin red ni costo, al instante y sin señal. Como no es IA, no hay aceptar ni descartar ni se guarda en `sugerencias_ia`. Alternativas descartadas: Gemini (necesita conexión y cuota, y exigiría aceptar o descartar) y combinar ambos (queda para la HU-14, que es la de la IA).
- D-2 · Dónde se muestran las sugerencias (RF-1324, RF-1330). **Aprobada el 2026-11-09:** en el detalle de una incidencia pendiente, para quien tiene `CONSULTAR_DICCIONARIO` (técnico y personal de sistemas). Alternativas descartadas: solo al técnico asignado, y también al escribir la descripción en «Nueva incidencia».
- D-3 · Desde dónde se registra (RF-1309 a RF-1319). **Aprobada el 2026-11-09:** el botón «Registrar falla» del diccionario y «Guardar en el diccionario» en el detalle de una incidencia cerrada, con el formulario lleno desde el informe. Alternativa descartada: solo el botón.
- D-4 · Editar o borrar (RF-1322, RF-1323). **Aprobada el 2026-11-09:** no; las fallas solo se crean, como ya dicen las reglas. Así no hay conflictos al sincronizar. Alternativa descartada: que el autor pueda editar (cambiaría las reglas y la regla de conflictos).

- D-5 · Fallas sin subir al cerrar sesión (RF-1331). **Aprobada el 2026-11-09:** se suman al mismo número de «registros sin enviar». Se hace en `EstadoDatosRepositoryImpl` (`core/sync`), que ya calcula ese número, así que no cambian `feature/acceso` ni los casos de uso de sesión. Alternativa descartada: no contarlas.

## Dudas abiertas
- Ninguna.
