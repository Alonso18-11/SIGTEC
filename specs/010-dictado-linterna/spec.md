# Spec 010 — Dictado por voz y linterna (HU-10)

> Estado: completa (2026-11-07), RF-1001 a RF-1032, en #90 a #102. Probada en un celular real (Xiaomi con Android 14) el 2026-11-07: dictado en la descripción y en los tres campos del informe, con pausas, «Detener» y el tope de silencio; permiso de micrófono; linterna en el detalle y en las pantallas de cámara. Ese celular reconoce la voz con `es-US` sin conexión, porque su servicio de voz rechaza `es-PE` (D-5). Aprobada el 2026-11-07, con las decisiones D-1 a D-10 respondidas por el usuario (ver «Decisiones»).

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-10 (RF-1001…). Corresponde al **RF-16 del producto** («dictar por voz la descripción del problema y transcribirla a texto»), del documento del producto (`SIGTEC_docx.pdf`, sección 11.1, fuera del repo). Se cita como «RF-16 del producto» para no confundirlo con el RF-16 de `specs/002-ingreso-huella`. También sigue la sección 16 del mismo documento (micrófono y linterna como recursos del celular), la 16.1 (el permiso de micrófono se pide al usarlo por primera vez, explicando para qué), la 17 (la voz se transcribe con el reconocimiento del propio Android, sin costo) y los prototipos 5 «Detalle de incidencia» (la linterna en la barra superior), 13 y 14 (la linterna en las pantallas de cámara) y 15 «Dictar descripción».

## Contexto y objetivo
El técnico registra y cierra incidencias con el celular en una mano, muchas veces con la otra ocupada o sucia, frente a un equipo abierto. Escribir la descripción del problema o el informe en el teclado del celular es lento, y en la práctica se deja para el final del día, cuando ya olvidó la mitad (sección 16 del producto). Además, para revisar el interior de una máquina necesita luz, y el celular que ya tiene en la mano tiene una linterna.

Esta HU agrega un micrófono junto a los campos de texto largos: el técnico dicta, ve el texto transcrito y lo lleva al campo, donde lo corrige antes de guardar. También agrega un botón para encender y apagar la linterna desde el detalle de la incidencia que está atendiendo y desde las pantallas de cámara de la HU-09. Nada de esto cambia datos por sí solo: el texto dictado se guarda únicamente cuando la persona guarda el formulario como siempre.

## Usuarios / actores
- Jefe, técnico y personal de sistemas: los tres registran incidencias (`REGISTRAR_INCIDENCIA`), así que los tres pueden dictar la descripción del problema en «Nueva incidencia». Los tres también abren «Escanear etiqueta» (HU-09) y ahí pueden usar la linterna.
- Técnico asignado: dicta los campos del informe técnico (HU-08) y usa la linterna del detalle y de «Evidencia fotográfica», mientras la incidencia siga pendiente. Es la misma persona y la misma condición con la que el detalle ya muestra «Cerrar con informe» y «Agregar foto».
- Android: concede o niega el permiso de micrófono, reconoce la voz con su servicio de reconocimiento (el que trae el celular) y enciende el flash de la cámara como linterna.

## Historias de usuario
- HU-10: Como técnico, quiero dictar por voz mis notas y encender la linterna desde la app, para registrar el detalle y revisar el equipo con las manos ocupadas.
- Criterios de aceptación de la planificación (`SIGTEC_Planificación.xlsx`): lo dictado se transcribe a texto en el campo correspondiente y puede corregirse antes de guardar; un control en la pantalla de atención enciende y apaga la linterna.

## Requisitos funcionales (criterios de aceptación en EARS)

### Dónde se puede dictar
- RF-1001: EL SISTEMA muestra un botón de micrófono, con la descripción «Dictar …» para el lector de pantalla, al final de estos campos: «Descripción del problema» en «Nueva incidencia» (prototipo 4; el botón ya existe sin función), y «Diagnóstico», «Solución aplicada» y «Observaciones» en «Informe técnico». «Piezas cambiadas» no lleva micrófono. Ver D-2.
- RF-1002: MIENTRAS un campo no se pueda editar (por ejemplo, durante el guardado o con el informe ya confirmado), EL SISTEMA deshabilita su micrófono, igual que el campo.
- RF-1003: CUANDO el usuario toca el micrófono de un campo, EL SISTEMA abre «Dictar» con el título del campo («Dictar descripción», «Dictar diagnóstico», «Dictar solución aplicada», «Dictar observaciones») y, debajo, de dónde viene: «Nueva incidencia», seguido del código del equipo si ya está escrito («Nueva incidencia - PC-CONT-014»), o el código visible de la incidencia en el informe («Informe técnico - INC-0124», o «Por sincronizar»). Prototipo 15. Ver D-1.

### Dictar (RF-16 del producto)
- RF-1004: CUANDO «Dictar» se abre con el permiso de micrófono concedido, EL SISTEMA empieza a escuchar de inmediato, sin otro toque, y muestra el micrófono animado con «Escuchando…».
- RF-1005: MIENTRAS escucha, EL SISTEMA muestra en el recuadro «Texto transcrito» lo que va reconociendo, a medida que la persona habla, y debajo «Habla con normalidad. Podrás corregir el texto antes de guardar.».
- RF-1006: EL SISTEMA reconoce la voz en español (D-5) con el servicio de reconocimiento de voz del propio Android, sin costo (sección 17 del producto), y le pide preferir el reconocimiento sin conexión cuando el celular lo tiene (D-6).
- RF-1007: MIENTRAS «Dictar» escuche, EL SISTEMA sigue escuchando aunque la persona haga pausas: cuando Android cierra un tramo, suma lo reconocido al texto y vuelve a escuchar solo, sin que la persona toque nada. Deja de escuchar CUANDO la persona toca «Detener» o pasan 60 segundos sin que reconozca ninguna palabra nueva; entonces conserva el texto reconocido (también lo que se veía si Android entrega el final vacío), muestra «Toca el micrófono para seguir dictando» y cambia «Detener» por el micrófono. Ver D-10.
- RF-1008: CUANDO, ya detenida la escucha, la persona toca el micrófono, EL SISTEMA vuelve a escuchar y agrega lo nuevo al final del texto transcrito, separado por un espacio; lo anterior no se pierde. Lo mismo hace con cada tramo mientras sigue escuchando (RF-1007).
- RF-1009: MIENTRAS el texto transcrito esté vacío, EL SISTEMA mantiene deshabilitado «Usar este texto».
- RF-1010: CUANDO la persona pulsa «Usar este texto», EL SISTEMA deja de escuchar, cierra «Dictar» y entrega el texto transcrito (sin espacios en los extremos) al campo que la abrió.
- RF-1011: CUANDO un campo recibe texto dictado, EL SISTEMA lo agrega al final de lo que el campo ya tenía, separado por un espacio, o lo escribe tal cual si el campo estaba vacío. El resto del formulario no cambia y el texto se puede corregir a mano antes de guardar. Ver D-3.
- RF-1012: CUANDO la persona sale de «Dictar» con «Volver» o con el botón atrás del sistema, EL SISTEMA deja de escuchar y vuelve a la pantalla anterior sin cambiar nada de lo que tenía.
- RF-1013: EL SISTEMA no guarda el audio en el celular ni lo envía a ningún servicio propio. El texto dictado solo se guarda cuando la persona guarda el formulario, con las mismas reglas de siempre (RF-33 a RF-41 de la HU-03; RF-801 en adelante de la HU-08).
- RF-1014: MIENTRAS «Dictar» no esté a la vista (otra pantalla, app en segundo plano o celular bloqueado), EL SISTEMA no usa el micrófono. Al volver, queda detenido con el texto que ya tenía (RF-1007); no vuelve a escuchar solo.

### Errores al dictar
- RF-1015: SI la escucha termina sin que Android haya reconocido ninguna palabra (la persona tocó «Detener» o se cumplió el tope de RF-1007 sin haber dicho nada entendible), ENTONCES EL SISTEMA muestra «No se entendió. Toca el micrófono e inténtalo de nuevo.». Un silencio mientras sigue escuchando no es un error: solo vuelve a escuchar.
- RF-1016: SI no hay conexión y el celular no tiene el reconocimiento sin conexión para español, ENTONCES EL SISTEMA se detiene con «Sin conexión, este celular no puede reconocer la voz. Conéctate o escribe el texto en el formulario.».
- RF-1017: SI el celular no tiene un servicio de reconocimiento de voz, ENTONCES EL SISTEMA muestra «Este celular no tiene reconocimiento de voz. Escribe el texto en el formulario.» en lugar del micrófono, y deja salir con «Volver».
- RF-1018: SI el micrófono no se puede usar (por ejemplo, porque otra app lo está usando o durante una llamada) o el reconocimiento falla por cualquier otro motivo, ENTONCES EL SISTEMA se detiene con «No se pudo usar el micrófono. Inténtalo de nuevo.» y conserva el texto que ya había.

### Permiso de micrófono (sección 16.1 del producto)
- RF-1019: CUANDO el usuario abre «Dictar» y la app todavía no tiene el permiso de micrófono, EL SISTEMA muestra primero, en lugar del micrófono, para qué lo usa: «SIGTEC usa el micrófono solo mientras dictas, para convertir tu voz en texto. No guarda el audio.», con el botón «Permitir micrófono». Solo al pulsarlo pide el permiso de Android.
- RF-1020: CUANDO el usuario concede el permiso, EL SISTEMA empieza a escuchar de inmediato (RF-1004), sin volver a pasar por la explicación.
- RF-1021: SI el usuario niega el permiso, ENTONCES EL SISTEMA no escucha, explica «Sin el permiso de micrófono no se puede dictar. Puedes escribir el texto en el formulario.» y ofrece «Permitir micrófono» de nuevo.
- RF-1022: SI Android ya no muestra su diálogo de permiso (el usuario lo negó más de una vez o eligió no volver a preguntar), ENTONCES EL SISTEMA cambia «Permitir micrófono» por «Abrir ajustes», que abre los ajustes de SIGTEC en Android. Al volver con el permiso concedido, empieza a escuchar.
- RF-1023: EL SISTEMA decide qué mostrar según el permiso con la misma regla que la cámara de la HU-09 (RF-916 a RF-922): sin pedir, concedido, negado o no volver a preguntar. La regla se generaliza para que la usen los dos permisos, sin cambiar lo que ya hace con la cámara. Ver D-7.

### Linterna en el detalle (prototipo 5)
- RF-1024: EL SISTEMA muestra un botón de linterna en la barra superior del detalle de la incidencia solo al técnico asignado y solo mientras la incidencia siga pendiente (la misma condición de «Cerrar con informe» y «Agregar foto»), y solo si el celular tiene flash. Ver D-4.
- RF-1025: CUANDO el técnico toca el botón, EL SISTEMA enciende la linterna y el botón cambia a encendido (ícono relleno, descripción «Apagar linterna»); al tocarlo de nuevo, la apaga (descripción «Encender linterna»). El estado del botón siempre coincide con el de la linterna.
- RF-1026: CUANDO el técnico sale del detalle (a otra pantalla o con «Volver»), la app pasa a segundo plano o se bloquea el celular, EL SISTEMA apaga la linterna. Al volver al detalle, sigue apagada.
- RF-1027: SI, con la linterna encendida, la incidencia deja de cumplir la condición de RF-1024 (se cierra o se reasigna a otro técnico), ENTONCES EL SISTEMA apaga la linterna y quita el botón.
- RF-1028: SI la linterna no se puede encender (por ejemplo, porque otra app está usando la cámara), ENTONCES EL SISTEMA deja el botón apagado y muestra «No se pudo encender la linterna.».
- RF-1029: SI la linterna se apaga desde fuera de la app (por ejemplo, desde los ajustes rápidos de Android) con el detalle abierto, ENTONCES EL SISTEMA muestra el botón apagado.

### Linterna en las pantallas de cámara (prototipos 13 y 14)
- RF-1030: MIENTRAS «Escanear etiqueta» o «Evidencia fotográfica» (HU-09) muestren la vista de la cámara y esa cámara tenga flash, EL SISTEMA muestra un botón de linterna en su barra superior, que enciende y apaga el flash de esa misma cámara con el comportamiento de RF-1025. Quién la usa no cambia: «Escanear etiqueta» la abren los tres perfiles y «Evidencia fotográfica», solo el técnico asignado (HU-09).
- RF-1031: CUANDO la cámara se suelta (la pantalla deja de estar a la vista, RF-915 de la HU-09) o la persona sale de la pantalla, EL SISTEMA apaga la linterna. Al volver, sigue apagada.
- RF-1032: MIENTRAS la pantalla muestre la explicación del permiso, un error de cámara o «Este celular no tiene cámara» (RF-916 a RF-922 de la HU-09), EL SISTEMA no muestra el botón de linterna.

## Requisitos no funcionales
- Idioma: textos de UI y mensajes en español, escritos en Kotlin.
- Costo cero: el dictado usa el reconocimiento de voz que trae Android, sin servicios de pago ni claves (sección 17 del producto). La linterna no usa nada externo.
- Sin dependencias nuevas: el reconocimiento de voz y el flash son de Android; la cámara ya está en `gradle/libs.versions.toml` (principio 1 de la constitución).
- Permisos: se agrega `RECORD_AUDIO`, que se pide al usarse por primera vez (RF-1019). La linterna del detalle no pide permiso: Android deja encender el flash sin el permiso de cámara. En las pantallas de cámara se usa la cámara que ya está abierta, con el permiso de la HU-09.
- Privacidad: la app no guarda el audio (RF-1013). El reconocimiento lo hace el servicio de voz del celular, que según el celular puede usar internet; eso lo decide Android, no la app. La política de privacidad de Google Play debe mencionar el micrófono (sección 19.1 del producto).
- Batería: el micrófono se suelta en cuanto «Dictar» deja de estar a la vista (RF-1014) o tras 60 segundos sin palabras (RF-1007) y la linterna se apaga al salir de su pantalla (RF-1026, RF-1031); nunca queda encendida con la app en segundo plano.
- Diseño: «Dictar» sigue el rediseño del PR #39: `BarraSuperior` con «Volver», `Scaffold(contentWindowInsets = WindowInsets(0))`, botones de `Botones.kt` («Detener» con `BotonPeligro` o `BotonSecundario` en rojo como el prototipo, «Usar este texto» con `BotonPrincipal`), solo roles de `MaterialTheme.colorScheme`, `Espaciado.*` y vistas previas en claro y oscuro de cada estado (escuchando, detenido con texto, error, permiso).
- Accesibilidad: el micrófono de cada campo, el de «Dictar» y la linterna tienen descripción para el lector de pantalla, y su estado (encendida o apagada, escuchando o detenido) no depende solo del color. Área táctil mínima de 48 dp (`AreaTactilMinima`).
- Rotación y cambio de tema: «Dictar» conserva el texto transcrito y queda detenido; el campo del formulario conserva lo que tenía.
- Pruebas: las reglas (unir el texto dictado al del campo, qué mostrar según el permiso y según los errores del reconocedor, cuándo se muestra la linterna) son funciones o clases puras con pruebas JVM; el reconocedor y la linterna quedan detrás de una interfaz para poder probar el ViewModel con dobles.
- Plataforma: Android 8.0 (minSdk 26) o superior. Desde Android 11, la app declara que consulta el servicio de reconocimiento de voz (visibilidad de paquetes), para que Android le diga si existe.
- Persistencia: nada nuevo en Room ni en Firestore; no hay migración ni cambios en `firestore.rules`.

## Casos límite
- Campo con texto escrito a mano y luego dictado: el dictado se agrega al final (RF-1011); si no sirve, se borra a mano.
- Dictar dos veces seguidas en el mismo campo: cada «Usar este texto» agrega al final (RF-1011).
- Dictar varias frases con pausas: la escucha sigue (RF-1007, D-10); cada tramo se suma al texto. Entre un tramo y el siguiente hay un instante (unos 0,3 s) en que Android vuelve a abrir el micrófono y una palabra dicha justo ahí se puede perder; en algunos celulares suena un aviso al reabrirlo.
- Salir de «Dictar» sin usar el texto: el campo queda igual (RF-1012).
- Llamada entrante mientras se dicta: el micrófono se suelta (RF-1014) o el reconocedor da error (RF-1018); el texto ya transcrito se conserva.
- Modo avión con el reconocimiento sin conexión instalado: se dicta igual. Sin él: RF-1016. El formulario siempre acepta texto escrito a mano.
- Permiso negado: no se dicta, pero los campos se escriben a mano y el resto de la app funciona igual (RF-1021).
- Celular sin servicio de reconocimiento (algunos celulares sin los servicios de Google): RF-1017.
- Nombres propios o códigos dictados («pe ce guion cont…»): Android los transcribe como puede; por eso el texto se corrige antes de guardar. El código del equipo no se dicta: se escribe o se escanea (HU-09).
- Rotación con «Dictar» escuchando: queda detenido con el texto (requisito no funcional de rotación).
- El informe se confirma en otro celular mientras se dicta: al volver con «Usar este texto», el campo ya no es editable y el texto no se agrega; el informe muestra su estado como en la HU-08.
- Celular sin flash: no aparece el botón de linterna (RF-1024, RF-1030).
- Linterna encendida en el detalle y el técnico toca «Agregar foto»: la linterna del detalle se apaga al salir (RF-1026) y en «Evidencia fotográfica» se enciende con su propio botón (RF-1030); no quedan dos controles sobre el mismo flash.
- Linterna encendida y la incidencia se cierra desde otro celular o se reasigna: RF-1027.
- Otra app usa la cámara: la linterna no enciende y se avisa (RF-1028).
- La linterna se apaga desde los ajustes rápidos de Android: el botón se actualiza (RF-1029).
- Jefe o personal de sistemas en el detalle: no ven la linterna, porque no atienden la incidencia (RF-1024).

## Fuera de alcance
- El campo «Mis notas», el borrador del informe y el asistente con micrófono (prototipos 9 y 16): son de la HU-14 (inteligencia artificial). Cuando exista, podrá reutilizar «Dictar» sin cambiarla.
- Dictar en otras pantallas (motivo de la solicitud de repuesto de la HU-11, diccionario de fallas de la HU-13, búsqueda): se conectan en sus HU, con el mismo «Dictar».
- Dictar el código del equipo o el responsable: el código se escanea o se escribe (HU-09) y el responsable sale del directorio (HU-12).
- Dictar comandos («guardar», «borrar») o puntuación por voz: lo que Android transcriba es lo que llega.
- Guardar o reproducir el audio.
- Regular la intensidad de la linterna o hacerla parpadear.
- Encender la linterna desde otras pantallas que no sean las de RF-1024 y RF-1030.

## Criterios de finalización
- RF-1001 a RF-1032 implementados.
- Pruebas unitarias (JVM): la regla que une el texto dictado al del campo (vacío, con texto, con espacios en los extremos); la regla del permiso generalizada (los mismos casos de la cámara siguen pasando, más los del micrófono); el ViewModel de «Dictar» con un reconocedor falso (escuchar, resultados parciales y finales, detener y seguir sumando, cada error de RF-1015 a RF-1018, salir sin usar); cuándo el detalle muestra la linterna (técnico asignado y pendiente, otro técnico, jefe, cerrada, sin flash) y el ViewModel del detalle con una linterna falsa (encender, apagar, error, la incidencia deja de cumplir la condición).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- Prueba manual en un celular real, con capturas en claro y oscuro:
  - Como personal de sistemas, dictar la descripción en «Nueva incidencia», corregirla a mano y guardar; dictar otra vez sobre un campo con texto y ver que se agrega al final; salir de «Dictar» sin usar el texto.
  - Negar el permiso de micrófono, negarlo de nuevo, concederlo desde «Abrir ajustes» y ver que empieza a escuchar al volver.
  - Como técnico asignado, dictar diagnóstico, solución y observaciones en el informe y cerrarlo con huella.
  - En modo avión, dictar y anotar si el celular lo reconoce sin conexión o muestra RF-1016.
  - Encender y apagar la linterna desde el detalle; salir con la linterna encendida y ver que se apaga; comprobar que otro técnico y el jefe no ven el botón.
  - Encender la linterna en «Escanear etiqueta» y en «Evidencia fotográfica», escanear y tomar una foto con ella, y salir para ver que se apaga.
- La línea de la HU-10 en `MEMORY.md` dice «completa».

## Decisiones
- D-1 · Cómo se dicta (RF-1003 a RF-1023). **Aprobada el 2026-11-07:** pantalla propia «Dictar» con el reconocedor de voz de Android, como el prototipo 15 y la sección 10 del producto, en la ruta `DictarTexto` que ya existe. Exige agregar `RECORD_AUDIO` y la consulta del servicio de reconocimiento al manifest (archivo compartido) y pedir el permiso al usarlo por primera vez. Se descartó el diálogo de voz de Google, que no pide permiso pero no sigue el prototipo y depende de esa app.
- D-2 · Campos con micrófono (RF-1001). **Aprobada el 2026-11-07:** «Descripción del problema» y, en el informe, «Diagnóstico», «Solución aplicada» y «Observaciones». «Piezas cambiadas» no, porque es una lista corta.
- D-3 · Qué hace el texto dictado con lo que ya había (RF-1011). **Aprobada el 2026-11-07:** se agrega al final, para poder dictar por partes sin perder nada.
- D-4 · Dónde va la linterna (RF-1024 a RF-1032). **Aprobada el 2026-11-07:** en el detalle, solo para el técnico asignado mientras la incidencia siga pendiente, y en «Escanear etiqueta» y «Evidencia fotográfica», donde la cámara ya está abierta.
- D-5 · Idioma del reconocimiento (RF-1006). **Aprobada el 2026-11-07, ajustada el mismo día tras probar en un Xiaomi con Android 14:** primero español de Perú (`es-PE`) sin conexión. Si el servicio de voz no lo tiene (ese celular lo rechazaba con `ERROR_LANGUAGE_NOT_SUPPORTED`), se prueban en orden `es-US` y `es-ES` sin conexión y, al final, `es-PE` y `es-US` con conexión, sin que la persona lo note. La primera que funciona se recuerda mientras la app sigue abierta. Si ninguna funciona, se muestra RF-1017.
- D-6 · Sin conexión (RF-1006, RF-1016). **Aprobada el 2026-11-07:** pedirle a Android que prefiera el reconocimiento sin conexión y, si no lo tiene, mostrar RF-1016. No se intenta descargar el paquete de voz desde la app: eso se hace en los ajustes de Android.
- D-7 · La regla del permiso (RF-1023). **Aprobada el 2026-11-07:** generalizar `vistaPermiso` de la HU-09 para que sirva a la cámara y al micrófono, moviéndola a un lugar común dentro de `feature/captura` (la cámara y el micrófono son de ese módulo). Cambia archivos de la HU-09 sin cambiar su comportamiento; sus pruebas deben seguir pasando igual.
- D-8 · Cómo se enciende la linterna en el detalle (RF-1024 a RF-1029). **Aprobada el 2026-11-07:** con el flash de la cámara trasera, sin abrir la cámara ni pedir el permiso de cámara, detrás de una interfaz en `feature/captura` para poder probarla con un doble. En las pantallas de cámara (RF-1030), con el control de la cámara que ya está abierta, porque Android no deja usar el flash por las dos vías a la vez.
- D-9 · Ruta de «Dictar» (RF-1003, RF-1010). **Aprobada el 2026-11-07:** `DictarTexto` pasa de objeto a clase con el campo y el subtítulo como argumentos, y devuelve el texto a la pantalla anterior con el `savedStateHandle` de la entrada anterior y una clave por campo, como el escaneo de la HU-09. Toca `Rutas.kt` (archivo compartido).

- D-10 · Cuándo deja de escuchar (RF-1007, RF-1015). **Aprobada el 2026-11-07, tras probar en el celular:** al tocar «Detener», con un tope de 60 segundos sin palabras nuevas para no dejar el micrófono encendido por olvido. Antes se detenía en cada pausa, lo que obligaba a tocar el micrófono una y otra vez. Se descartaron «solo al tocar Detener», sin tope, y «tras unos segundos de silencio».

## Dudas abiertas
- Ninguna.
