# Spec 009 — Código del equipo por cámara y evidencia fotográfica (HU-09)

> Estado: aprobada (2026-10-16), con las decisiones D-1 a D-12 respondidas por el usuario (ver «Decisiones»). Queda pendiente D-13.

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-09 (RF-901…). Corresponde a dos requisitos del documento del producto (`SIGTEC_docx.pdf`, sección 11.1, fuera del repo): el **RF-14 del producto** («capturar el código del equipo con la cámara, sin escribirlo manualmente») y el **RF-15 del producto** («adjuntar fotografías de la falla y de la pieza cambiada a una incidencia»). Se citan como «RF-14/RF-15 del producto» para no confundirlos con el RF-14 y el RF-15 de `specs/001-inicio-sesion`. También sigue las secciones 16.1 (fotos en el almacenamiento interno y permisos pedidos al usarse por primera vez) y 17 (lectura del código con ML Kit en el dispositivo, sin conexión) del mismo documento, y los prototipos 13 «Escaneo del equipo» y 14 «Captura de evidencia».

## Contexto y objetivo
Hoy el código del equipo se escribe a mano al registrar una incidencia. Es una cadena larga (`PC-CONT-014`) y el error de transcripción es la causa más común de que una incidencia quede asociada al equipo equivocado; la HU-03 ya rechaza los códigos que no están en el catálogo (RF-41), pero eso solo avisa del error, no lo evita. Además, el técnico no tiene cómo respaldar lo que encontró ni lo que cambió: el informe técnico (HU-08) es solo texto.

Esta HU le permite a cualquier persona del área leer el código desde la etiqueta del equipo con la cámara, en el propio celular y sin conexión, y confirmarlo antes de usarlo. Al técnico asignado le permite fotografiar la falla y la pieza cambiada y dejar esas fotos enlazadas a la incidencia. Las fotos se guardan solo en el almacenamiento interno del celular que las tomó, porque el proyecto debe costar cero (plan Spark de Firebase, sin Cloud Storage).

## Usuarios / actores
- Jefe de Soporte y Sistemas, técnico y personal de sistemas: los tres registran incidencias (`REGISTRAR_INCIDENCIA`) y consultan el historial de un equipo (`CONSULTAR_HISTORIAL`), así que los tres pueden leer el código con la cámara. Leer el código no guarda nada.
- Técnico asignado: el único que agrega y elimina fotos de una incidencia, y solo mientras siga pendiente. Es la misma persona y la misma condición que ya usan «Cerrar con informe» y «Solicitar repuesto» en el detalle (HU-08, `ATENDER_INCIDENCIA`).
- Cualquiera que abra el detalle en el celular donde se tomaron las fotos: las ve. En otro celular no existen.
- Android: concede o niega el permiso de cámara. ML Kit reconoce el texto dentro del celular. Room guarda el enlace entre la foto y la incidencia, y el almacenamiento interno de la app guarda el archivo.

## Historias de usuario
- HU-09: Como técnico, quiero capturar con la cámara el código del equipo y las fotos de la falla, para no transcribir a mano y respaldar mi informe.
- Criterios de aceptación de la planificación (`SIGTEC_Planificación.xlsx`, sprint 2): al enfocar la etiqueta, el código se reconoce y se escribe solo en el formulario; las fotos de la falla y de la pieza cambiada se guardan en el dispositivo y quedan enlazadas a la incidencia.

## Requisitos funcionales (criterios de aceptación en EARS)

### Leer el código del equipo con la cámara (RF-14 del producto)
- RF-901: CUANDO el usuario toca el botón de cámara junto a «Código del equipo» en «Nueva incidencia», EL SISTEMA abre «Escanear etiqueta» con la vista de la cámara trasera, un marco guía y la indicación «Encuadra la etiqueta del equipo dentro del marco» (prototipo 13).
- RF-902: MIENTRAS «Escanear etiqueta» esté abierta y no haya un código reconocido, EL SISTEMA reconoce el texto de las imágenes de la cámara dentro del propio celular, sin conexión a internet y sin enviar las imágenes a ningún servicio (sección 17 del producto).
- RF-903: EL SISTEMA busca en el texto reconocido un código con la forma de los códigos de equipo: de 2 a 4 letras, un guion, de 2 a 5 letras, un guion y 3 dígitos (`PC-CONT-014`, `IMP-RRHH-002`). Antes de buscarlo normaliza el texto: lo pasa a mayúsculas, unifica los distintos tipos de guion, quita los espacios alrededor de los guiones y corrige, según la posición, las confusiones típicas entre letras y dígitos (en la parte numérica, O→0, I y L→1, S→5, B→8 y Z→2; en las partes de letras, al revés).
- RF-904: SI el texto contiene más de un código posible, ENTONCES EL SISTEMA prefiere el que existe en el catálogo de equipos del celular; si hay varios en el catálogo, o ninguno, toma el primero en el orden de lectura.
- RF-905: EL SISTEMA da un código por reconocido solo cuando lo lee igual en 3 imágenes seguidas, para no mostrar lecturas sueltas equivocadas mientras la cámara se mueve.
- RF-906: CUANDO reconoce un código, EL SISTEMA deja de analizar imágenes y muestra, debajo de la vista, «Código reconocido · sin conexión», el código y los botones «Volver a escanear» y «Usar este código». No escribe nada en el formulario hasta que el usuario lo confirma (sección 17 del producto: «donde el técnico lo confirma»).
- RF-907: SI el código reconocido no existe en el catálogo de equipos del celular, ENTONCES EL SISTEMA lo muestra con el aviso «No está en el catálogo de este celular» y deja usarlo igual; el registro lo vuelve a validar al guardar (RF-41, HU-03). Ver D-3 en «Dudas abiertas».
- RF-908: CUANDO el usuario pulsa «Volver a escanear», EL SISTEMA descarta el código mostrado y vuelve a analizar imágenes; el conteo de RF-905 empieza de nuevo.
- RF-909: CUANDO el usuario pulsa «Usar este código», EL SISTEMA cierra «Escanear etiqueta» y entrega el código a la pantalla que la abrió.
- RF-910: CUANDO «Nueva incidencia» recibe un código escaneado, EL SISTEMA lo escribe en «Código del equipo», reemplazando lo que hubiera, y muestra debajo «Leído desde la etiqueta con la cámara» (prototipo 4). El resto del formulario no cambia.
- RF-911: SI el usuario edita a mano el código después de recibirlo de la cámara, ENTONCES EL SISTEMA quita «Leído desde la etiqueta con la cámara».
- RF-912: CUANDO el usuario sale de «Escanear etiqueta» con «Volver» o con el botón atrás del sistema sin confirmar un código, EL SISTEMA vuelve a la pantalla anterior sin cambiar nada de lo que tenía.
- RF-913: EL SISTEMA entrega el código confirmado de la misma forma a cualquier pantalla que abra «Escanear etiqueta», para que la búsqueda del historial del equipo (prototipo 11, HU-16) lo use sin cambiar el escaneo. En esta HU solo «Nueva incidencia» lo recibe; el botón de cámara de la búsqueda del historial se conecta en la HU-16. Ver D-4.
- RF-914: EL SISTEMA no guarda las imágenes ni el texto que analiza al escanear: nada de lo que ve la cámara queda en el celular ni sale de él.
- RF-915: MIENTRAS «Escanear etiqueta» no esté a la vista (otra pantalla, app en segundo plano o celular bloqueado), EL SISTEMA no usa la cámara.

### Permiso de cámara (sección 16.1 del producto)
- RF-916: CUANDO el usuario abre una pantalla que usa la cámara («Escanear etiqueta» o «Evidencia fotográfica») y la app todavía no tiene el permiso de cámara, EL SISTEMA muestra primero, en lugar de la vista de la cámara, para qué la usa: «SIGTEC usa la cámara para leer la etiqueta del equipo y para fotografiar la falla y la pieza cambiada. Las fotos se guardan solo en este celular.», con el botón «Permitir cámara». Solo al pulsarlo pide el permiso de Android.
- RF-917: CUANDO el usuario concede el permiso, EL SISTEMA muestra la vista de la cámara de inmediato, sin volver a pasar por la explicación, y no vuelve a pedirlo mientras siga concedido.
- RF-918: SI el usuario niega el permiso, ENTONCES EL SISTEMA no abre la cámara, explica que sin el permiso no puede leer la etiqueta ni tomar fotos y ofrece «Permitir cámara» de nuevo; en «Escanear etiqueta» recuerda además que el código se puede escribir a mano en el formulario.
- RF-919: SI Android ya no muestra su diálogo de permiso (porque el usuario lo negó más de una vez o eligió no volver a preguntar), ENTONCES EL SISTEMA cambia «Permitir cámara» por «Abrir ajustes», que abre los ajustes de SIGTEC en Android para concederlo desde ahí. Al volver con el permiso concedido, muestra la cámara.
- RF-920: SI el celular no tiene cámara, ENTONCES EL SISTEMA muestra «Este celular no tiene cámara.» en lugar de la vista; el resto de la app funciona igual y el código se sigue pudiendo escribir a mano.
- RF-921: SI la cámara no se puede abrir (por ejemplo, porque otra app la está usando), ENTONCES EL SISTEMA muestra «No se pudo abrir la cámara. Inténtalo de nuevo.» con un botón «Reintentar».
- RF-922: SI el usuario quita el permiso desde los ajustes de Android mientras la app está abierta, ENTONCES, al volver a una pantalla con cámara, EL SISTEMA se comporta como en RF-916.

### Tomar fotos de evidencia (RF-15 del producto)
- RF-923: EL SISTEMA muestra «Agregar foto» en la sección «Evidencias» del detalle solo al técnico asignado y solo mientras la incidencia siga pendiente: la misma condición con la que el detalle ya muestra «Cerrar con informe» (HU-08). Ver D-1.
- RF-924: CUANDO el técnico pulsa «Agregar foto», EL SISTEMA abre «Evidencia fotográfica» (prototipo 14) con el código visible de la incidencia (o «Por sincronizar») y el del equipo, un selector «Falla» / «Pieza cambiada» que empieza en «Falla», la vista de la cámara con «Se guardará como: foto de la falla» o «… foto de la pieza cambiada» según el selector, el disparador, las miniaturas de las fotos que la incidencia ya tiene en este celular, la cantidad («2 fotos») y el botón «Listo».
- RF-925: CUANDO el técnico pulsa el disparador, EL SISTEMA toma la foto, la guarda en el almacenamiento interno de la app y la enlaza a la incidencia con el tipo elegido en el selector y la fecha y hora de la captura. La miniatura aparece en la fila, rotulada con su tipo y su orden («Falla 1», «Pieza 1»), y la cantidad sube.
- RF-926: CUANDO va a enlazar una foto, EL SISTEMA verifica en el dominio, en este orden: el permiso `ATENDER_INCIDENCIA` y que quien tiene la sesión es el técnico asignado (si no, `SinPermiso`); que la incidencia sigue pendiente (si ya se cerró, `IncidenciaCerrada`, el error que ya usa la HU-06); y que no llegó al máximo de fotos (RF-929). Si algo falla, no enlaza la foto y borra su archivo.
- RF-927: MIENTRAS se toma y se guarda una foto, EL SISTEMA mantiene deshabilitado el disparador, para que un doble toque no tome dos fotos.
- RF-928: SI falla la captura, la escritura del archivo o el guardado en Room, ENTONCES EL SISTEMA no deja un archivo sin enlace ni un enlace sin archivo, muestra «Ocurrió un error inesperado. Inténtalo de nuevo.» y deja volver a disparar.
- RF-929: SI la incidencia ya tiene 10 fotos en este celular, ENTONCES EL SISTEMA deshabilita el disparador y muestra «Llegaste al máximo de 10 fotos por incidencia.». Si se intenta por otra vía, el caso de uso lo rechaza con un error de dominio nuevo, `LimiteFotosAlcanzado`. Ver D-2.
- RF-930: CUANDO el técnico pulsa «Listo», o sale con «Volver» o con el botón atrás del sistema, EL SISTEMA vuelve al detalle. Cada foto se guarda al tomarla, no al pulsar «Listo», así que salir no descarta ninguna.
- RF-931: SI, con «Evidencia fotográfica» abierta, la incidencia se cierra o se reasigna a otro técnico (por ejemplo, porque llega el cambio desde otro celular), ENTONCES EL SISTEMA deshabilita el disparador y muestra «Ya no puedes agregar fotos a esta incidencia.»; las fotos ya tomadas se conservan.

### Ver, ampliar y eliminar las fotos de una incidencia
- RF-932: EL SISTEMA muestra en la sección «Evidencias» del detalle las fotos que la incidencia tiene en este celular, de la más antigua a la más reciente, cada una rotulada «Foto de la falla» o «Foto de la pieza cambiada» (prototipo 5). Las ve cualquier persona que abra el detalle en este celular, aunque no tenga «Agregar foto».
- RF-933: EL SISTEMA indica en la sección «Evidencias» que «Las fotos se guardan solo en el celular que las tomó.»; SI la incidencia no tiene fotos en este celular, ENTONCES muestra además «Sin fotos en este celular.».
- RF-934: CUANDO alguien toca una miniatura, EL SISTEMA muestra la foto ampliada, con su tipo y la fecha y hora en que se tomó; se cierra con «Volver» o con el botón atrás del sistema.
- RF-935: SI el archivo de una foto enlazada ya no existe o no se puede leer, ENTONCES EL SISTEMA muestra «Foto no disponible» en su lugar, sin fallar.
- RF-936: EL SISTEMA actualiza la sección «Evidencias» sola cuando se agrega o se elimina una foto, sin salir del detalle, igual que el resto del detalle (HU-04).
- RF-937: CUANDO el técnico asignado, mientras la incidencia siga pendiente, pulsa «Eliminar foto» en la foto ampliada y lo confirma («¿Eliminar esta foto? No se puede deshacer.»), EL SISTEMA borra el enlace y el archivo. La regla se verifica en el dominio igual que en RF-926. Con la incidencia cerrada no se puede eliminar ninguna foto: queda como respaldo del informe. Ver D-5.
- RF-938: EL SISTEMA muestra la misma sección «Evidencias», con sus miniaturas y «Agregar foto», en «Informe técnico» (prototipo 9), para que el técnico fotografíe la pieza cambiada sin salir del cierre. Al volver de «Evidencia fotográfica», lo escrito en el informe se conserva. Ver D-6.

### Las fotos viven solo en este celular (costo cero)
- RF-939: EL SISTEMA nunca sube las fotos ni sus enlaces a Firestore ni a ningún otro servicio: no quedan pendientes de sincronizar y, en otro celular, la misma incidencia aparece sin fotos (sección 16.1 del producto, plan Spark).
- RF-940: EL SISTEMA guarda las fotos en el almacenamiento interno de la app, nunca en la galería ni en un almacenamiento compartido: otras apps no pueden verlas y no hace falta ningún permiso de almacenamiento.
- RF-941: EL SISTEMA deja las fotos fuera de la copia de seguridad automática de Android, para que no terminen en la cuenta de Google de quien usa el celular ni se restauren en otro. Ver D-9.
- RF-942: CUANDO se borran los datos locales al cerrar sesión (RF-10, HU-01) o al entrar otra persona en el celular (RF-11, HU-01), EL SISTEMA borra también todas las fotos de evidencia del almacenamiento interno. Esto amplía RF-10 y RF-11 de `specs/001-inicio-sesion`, como dejó pendiente su decisión D-7 (`MEMORY.md`, «Pendientes conocidos»). Ver D-7 de esta spec.
- RF-943: SI el usuario pulsa «Cerrar sesión» y en el celular hay fotos de evidencia, ENTONCES EL SISTEMA pide confirmación aunque no haya cambios sin enviar, y el aviso de RF-13 (HU-01) nombra también las fotos: «Tienes N cambios sin enviar y M fotos de evidencia en este celular. Si cierras sesión ahora, se perderán.»; con fotos y sin cambios, «Tienes M fotos de evidencia en este celular. Si cierras sesión ahora, se perderán.» (con 1 foto, en singular: «Tienes 1 foto de evidencia en este celular. Si cierras sesión ahora, se perderá.»). Las opciones «Cancelar» y «Cerrar de todos modos» y su efecto no cambian. Esto amplía RF-13 de `specs/001-inicio-sesion`, que se actualiza en el mismo PR. Ver D-8.

## Requisitos no funcionales
- Idioma: textos de UI y mensajes de error en español; cada error sale de `error.mensaje()`.
- Sin conexión: la lectura del código funciona sin internet desde el primer uso, porque el reconocimiento de texto viene dentro de la app y no se descarga después (sección 17 del producto). Tomar y ver fotos tampoco necesita conexión.
- Fluidez: la vista de la cámara no se congela mientras se analiza el texto. Se analiza solo la imagen más reciente y se descartan las que llegan mientras se procesa la anterior. Con la etiqueta bien encuadrada y con luz, el código aparece en menos de 2 segundos (se verifica a mano).
- Batería: la cámara se suelta en cuanto su pantalla deja de estar a la vista (RF-915).
- Almacenamiento: cada foto se guarda reducida a 1600 píxeles en su lado mayor y comprimida, unos 300 a 500 KB. Con el máximo de 10 fotos (RF-929), una incidencia ocupa unos 5 MB como mucho.
- Privacidad: nada de lo que ve la cámara al escanear se guarda (RF-914); las fotos no salen del celular (RF-939 a RF-941) y se borran con los datos locales (RF-942).
- Diseño: las pantallas nuevas siguen el rediseño del PR #39: `BarraSuperior` clara con «Volver», en lugar de la barra oscura con «X» de los prototipos 13 y 14, que son anteriores a ese rediseño; botones de `Botones.kt`; solo roles de `MaterialTheme.colorScheme`, también para el oscurecido alrededor del marco y para el panel del código reconocido; vistas previas en claro y oscuro, y área táctil mínima en el disparador y en los botones. Los prototipos traen un botón de linterna que esta HU no incluye (es de la HU-10). Ver D-10.
- Accesibilidad: el disparador, las miniaturas y la foto ampliada tienen una descripción para el lector de pantalla («Tomar foto», «Foto de la falla 1»…).
- Pruebas: la fecha y hora de cada foto la decide quien invoca el caso de uso, nunca una llamada a `now()` dentro del dominio, para poder fijarla en las pruebas.
- Stack cerrado: sin dependencias nuevas. La cámara y el reconocimiento de texto ya están en `gradle/libs.versions.toml` desde el esqueleto (principio 1 de la constitución).
- Persistencia local: la tabla de evidencias ya existe, sin campos de sincronización, y no cambia; no hay migración de Room.
- Plataforma: Android 8.0 (minSdk 26) o superior. En todas esas versiones el permiso de cámara se pide mientras la app está en uso.

## Casos límite
- Etiqueta con más texto alrededor («SOFT CORPORATION · ACTIVO FIJO», «Área: Contabilidad»): solo el código cumple la forma de RF-903; el resto se ignora.
- Lectura con confusiones o espacios (`PC-C0NT-O14`, `pc - cont - 014`): se normaliza a `PC-CONT-014` (RF-903).
- Dos etiquetas a la vez en la imagen: gana la que está en el catálogo y, si no, la primera en el orden de lectura (RF-904).
- La cámara se mueve y alterna entre dos lecturas distintas: no se da nada por reconocido hasta tener 3 lecturas iguales seguidas (RF-905).
- Etiqueta borrosa, con reflejos, muy girada o sin luz: no se reconoce nada, la indicación sigue a la vista y el usuario puede volver y escribir el código a mano. La linterna para estos casos llega con la HU-10.
- Catálogo vacío en el celular (primer uso sin haber sincronizado): todo código reconocido sale con el aviso de RF-907 y se puede usar; si sigue fuera del catálogo al guardar, el registro lo rechaza (RF-41).
- El usuario ya había escrito un código y escanea otro: el escaneado lo reemplaza (RF-910). Si sale sin confirmar, queda el que había (RF-912).
- Rotación o cambio de tema con «Escanear etiqueta» o «Evidencia fotográfica» abierta: se conservan el código reconocido, el tipo elegido en el selector y las fotos ya tomadas; la cámara se vuelve a abrir sola.
- Llamada entrante o app en segundo plano con la cámara abierta: la cámara se suelta (RF-915) y se reanuda al volver.
- Permiso negado: no hay cámara, pero el código se escribe a mano y el resto de la app funciona igual (RF-918).
- Celular con una sola cámara, frontal: se usa esa en lugar de la trasera. Sin ninguna cámara, RF-920.
- Foto tomada sin conexión: se guarda igual, porque nunca se sube (RF-939).
- Incidencia «Por sincronizar»: no puede recibir fotos, porque solo el técnico asignado las toma y asignar exige que la incidencia ya tenga número (RF-610, HU-06).
- Doble toque en el disparador: se toma una sola foto (RF-927).
- La incidencia llega a 10 fotos mientras la pantalla está abierta: el disparador se deshabilita con el aviso de RF-929.
- Reasignación con «Evidencia fotográfica» abierta: se aplica RF-931. Las fotos tomadas quedan en el celular del técnico anterior y se siguen viendo en su detalle; el técnico nuevo no las ve.
- Celular sin espacio: la foto no se guarda y se muestra el error genérico (RF-928), sin archivos a medias.
- El archivo de una foto desaparece por fuera de la app: su miniatura dice «Foto no disponible» (RF-935). Si alguien borra los datos de la app desde los ajustes de Android, se pierden las fotos y sus enlaces a la vez.
- Incidencia cerrada: la sección «Evidencias» muestra las fotos, pero sin «Agregar foto» ni «Eliminar foto» (RF-923, RF-937).
- Cerrar sesión o entrar otra persona con fotos en el celular: se borran junto con los demás datos locales (RF-942); antes de cerrar sesión, el aviso las cuenta (RF-943).

## Fuera de alcance
- La linterna en las pantallas de cámara, aunque aparece en los prototipos 13 y 14: es de la HU-10.
- Dictar texto: es de la HU-10 («Dictar» sigue con `PantallaEnConstruccion`).
- La pantalla del historial del equipo y su búsqueda (prototipo 11): son de la HU-16. Esta HU solo deja el escaneo listo para que esa búsqueda lo use (RF-913).
- Escanear desde otras pantallas que no sean «Nueva incidencia» (por ejemplo, desde la lista de incidencias): no lo pide la HU.
- Subir las fotos a la nube, verlas en otro celular o compartirlas: el proyecto no usa Cloud Storage (costo cero). Si la empresa lo pide más adelante, bastaría con cambiar la capa de datos (sección 16.1 del producto).
- Leer códigos de barras o QR: las etiquetas tienen el código impreso como texto.
- Editar, recortar o anotar una foto, elegir una de la galería o grabar video.
- Fotos al registrar la incidencia (antes de que exista y tenga técnico): solo el técnico asignado toma fotos.
- Cambiar el informe técnico (sus campos o su cierre con huella): la HU-08 queda igual; solo se le agrega la sección «Evidencias» (D-6).

## Criterios de finalización
- RF-901 a RF-943 implementados (RF-937, RF-938 y RF-943 salen de D-5, D-6 y D-8, aprobadas).
- Pruebas unitarias (JVM): la regla que extrae el código del texto (normalización, confusiones según la posición, forma del código, varios candidatos con y sin catálogo, texto sin código); la regla de las 3 lecturas iguales seguidas y su reinicio; la regla que decide qué mostrar según el permiso (sin pedir, concedido, negado, no volver a preguntar, sin cámara); el caso de uso de guardar una foto (sin sesión, otro técnico, incidencia cerrada, máximo alcanzado, guardado correcto con su fecha, fallo de Room que no deja el archivo); el de eliminarla; el manejo de los archivos de fotos sobre una carpeta temporal (crear, borrar una, borrar todas); y los `ViewModel` de «Escanear etiqueta», «Evidencia fotográfica», «Nueva incidencia» (código escaneado e indicación) y del detalle (fotos y «Agregar foto» solo para el técnico asignado mientras siga pendiente).
- Si cambia alguna consulta de Room, su prueba instrumentada pasa en un celular. La entidad no cambia y `version` no sube.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- `specs/001-inicio-sesion/spec.md` actualizada en los mismos PR que implementan RF-942 y RF-943 (RF-10 y RF-11 ya no dicen «las fotos se agregarán cuando exista la HU de captura» y RF-13 cuenta las fotos), y la línea de la HU-09 en `MEMORY.md` dice «completa».
- Prueba manual en un celular real (no solo en el emulador, porque su cámara es simulada), con capturas en claro y oscuro:
  - Como personal de sistemas, escanear una etiqueta de cada uno de los 4 equipos de prueba, impresa o en otra pantalla, en modo avión, y comprobar que el código llega a «Nueva incidencia» con «Leído desde la etiqueta con la cámara»; escanear un código que no está en el catálogo y ver el aviso; salir sin confirmar y ver que el formulario no cambió.
  - Negar el permiso de cámara, negarlo de nuevo, concederlo desde «Abrir ajustes» y ver que la cámara se abre al volver.
  - Como técnico asignado, tomar fotos de falla y de pieza cambiada en una incidencia en atención, verlas en el detalle, ampliarlas, eliminar una (si se aprueba D-5), llegar al máximo de 10 y comprobar que, como jefe en otro celular, la misma incidencia no tiene fotos.
  - Con otro técnico, comprobar que no ve «Agregar foto»; cerrar la incidencia y comprobar que las fotos siguen y ya no se pueden agregar ni eliminar.
  - Cerrar sesión y comprobar que la carpeta de fotos de la app quedó vacía; repetir entrando con otra persona sin cerrar sesión.

## Dudas abiertas
Decisiones que necesitan el visto bueno antes de implementar. Cada una trae la propuesta de esta spec; las marcadas con [NECESITA ACLARACIÓN] no tienen una respuesta clara en el código ni en el documento del producto.
- D-1 · Quién agrega fotos (RF-923, RF-926). Propuesta: solo el técnico asignado, mientras la incidencia siga pendiente, igual que «Cerrar con informe». La HU dice «como técnico… para respaldar mi informe». Alternativa: cualquiera que registra incidencias, para que el personal de sistemas fotografíe la falla al reportarla; se descarta porque la foto quedaría en un celular distinto al del técnico que atiende.
- D-2 · Máximo de 10 fotos por incidencia y un error nuevo, `LimiteFotosAlcanzado` (RF-929). Toca `core/domain/Resultado.kt` y `core/ui/MensajesError.kt`, que son compartidos. Alternativa: sin máximo, con el riesgo de llenar el celular, porque las fotos nunca salen de él.
- D-3 · Código fuera del catálogo (RF-907). Propuesta: mostrarlo con un aviso y dejar usarlo, porque sin conexión el catálogo puede no haber bajado todavía y el registro ya valida al guardar (RF-41). Alternativa: no ofrecer «Usar este código» en ese caso.
- D-4 · Historial del equipo (RF-913). Propuesta: esta HU deja el escaneo listo para cualquier pantalla, con una sola forma de devolver el código, y el botón de cámara de la búsqueda del historial lo conecta quien haga la HU-16 (hoy Alonso). Hay que confirmarlo con él.
- D-5 · Eliminar fotos (RF-937). Propuesta: sí, solo el técnico asignado y mientras siga pendiente, porque una foto equivocada (por ejemplo, con datos personales en la pantalla del usuario) no debería quedar enlazada. Alternativa: no eliminar nunca, que ahorra un PR.
- D-6 · Sección «Evidencias» también en «Informe técnico» (RF-938, prototipo 9). Propuesta: sí, en un PR aparte, porque la pieza cambiada se fotografía justo al cerrar. Toca `InformeTecnicoScreen`, de la HU-08.
- D-7 · Borrar las fotos con los datos locales (RF-942). Toca el modelo de sesión (`SesionRepositoryFirebase.cerrarSesion`) y `core/sync` (`SincronizacionTiempoReal`, el borrado al entrar otra persona, D-12 de la spec 001), y crea en `core` el único lugar que conoce la carpeta de fotos, para que los dos lo usen. `CLAUDE.md` pide coordinar con Alonso cualquier cambio del modelo de sesión: va en un PR aparte, revisado por él.
- D-8 · [NECESITA ACLARACIÓN] Avisar antes de cerrar sesión que se borrarán las fotos. Las fotos nunca se envían, así que cerrar sesión las pierde para siempre, y hoy la confirmación de RF-13 (HU-01) solo cuenta los cambios sin enviar. Opciones: (a) agregar «y N fotos de evidencia» a esa misma confirmación, lo que toca `feature/acceso`; (b) aceptar la pérdida y dejarla documentada. Propuesta: (a), en un PR aparte coordinado con Alonso.
- D-9 · [NECESITA ACLARACIÓN] Copia de seguridad de Android (RF-941). El manifest tiene `allowBackup="true"` con las reglas de ejemplo, así que Android respalda en la cuenta de Google todo el almacenamiento interno, incluidas hoy la base de Room y las preferencias de la huella. Esta HU solo deja fuera la carpeta de fotos. Si también hay que dejar fuera Room y DataStore, o apagar la copia, es otro PR, fuera de esta HU; propongo anotarlo como pendiente para Alonso.
- D-10 · Diseño de las pantallas de cámara. Propuesta: `BarraSuperior` clara con «Volver», del rediseño del #39, en lugar de la barra oscura con «X» de los prototipos, y sin el botón de linterna (HU-10). Alternativa: copiar el prototipo con una barra oscura propia, que se apartaría del rediseño.
- D-11 · `AndroidManifest.xml` (compartido): agregar el permiso de cámara y declarar la cámara como no obligatoria, para que Google Play no excluya los celulares sin cámara trasera (RF-920).
- D-12 · Responsable de la HU-09. `MEMORY.md` la tenía a nombre de Alonso (en el PDF de contexto era de Nina). Esta spec la toma Nina; Alonso debe confirmarlo al revisar este PR.
