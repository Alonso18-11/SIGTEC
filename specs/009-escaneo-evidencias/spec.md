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
