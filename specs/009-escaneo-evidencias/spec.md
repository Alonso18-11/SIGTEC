# Spec 009 — Código del equipo por cámara y evidencia fotográfica (HU-09)

> Estado: borrador (2026-10-15).

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-09 (RF-901…). Corresponde a dos requisitos del documento del producto (`SIGTEC_docx.pdf`, sección 11.1, fuera del repo): el **RF-14 del producto** («capturar el código del equipo con la cámara, sin escribirlo manualmente») y el **RF-15 del producto** («adjuntar fotografías de la falla y de la pieza cambiada a una incidencia»). Se citan como «RF-14/RF-15 del producto» para no confundirlos con el RF-14 y el RF-15 de `specs/001-inicio-sesion`. También sigue las secciones 16.1 (fotos en el almacenamiento interno y permisos pedidos al usarse por primera vez) y 17 (lectura del código con ML Kit en el dispositivo, sin conexión) del mismo documento, y los prototipos 13 «Escaneo del equipo» y 14 «Captura de evidencia».

## Contexto y objetivo
Hoy el código del equipo se escribe a mano al registrar una incidencia. Es una cadena larga (`PC-CONT-014`) y el error de transcripción es la causa más común de que una incidencia quede asociada al equipo equivocado; la HU-03 ya rechaza los códigos que no están en el catálogo (RF-41), pero eso solo avisa del error, no lo evita. Además, el técnico no tiene cómo respaldar lo que encontró ni lo que cambió: el informe técnico (HU-08) es solo texto.

Esta HU le permite a cualquier persona del área leer el código desde la etiqueta del equipo con la cámara, en el propio celular y sin conexión, y confirmarlo antes de usarlo. Al técnico asignado le permite fotografiar la falla y la pieza cambiada y dejar esas fotos enlazadas a la incidencia. Las fotos se guardan solo en el almacenamiento interno del celular que las tomó, porque el proyecto debe costar cero (plan Spark de Firebase, sin Cloud Storage).

## Usuarios / actores
- Jefe de Soporte y Sistemas, técnico y personal de sistemas: los tres registran incidencias (`REGISTRAR_INCIDENCIA`) y consultan el historial de un equipo (`CONSULTAR_HISTORIAL`), así que los tres pueden leer el código con la cámara. Leer el código no guarda nada.
- Técnico asignado: el único que agrega, y si se aprueba elimina, fotos de una incidencia, y solo mientras siga pendiente. Es la misma persona y la misma condición que ya usan «Cerrar con informe» y «Solicitar repuesto» en el detalle (HU-08, `ATENDER_INCIDENCIA`).
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
