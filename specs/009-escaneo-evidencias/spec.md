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
