# Spec: Inicio (panel del perfil)

> **Estado:** aprobada por Alonso el 2026-11-08 (decisiones I-1 a I-3). Requisitos RF-I1 a RF-I12.
>
> No tiene una HU propia en la planificación. El documento del producto la describe como la pantalla 2, «Inicio (panel del perfil)»: «saludo al usuario, resumen de incidencias pendientes y accesos directos a los módulos habilitados». El diseño es el prototipo 02 de `docs/prototipos/` con los componentes v2 (`DESIGN.md` 4). Reemplaza a la `PantallaEnConstruccion` de `feature/inicio`.

## Contexto y objetivo
Hoy Inicio es una pantalla temporal con cuatro botones. El técnico tiene que entrar a «Mis tareas» para saber cuánto tiene pendiente, y el jefe, a la lista, para ver qué falta asignar. Inicio debe mostrar de un vistazo lo pendiente de cada perfil y llevar a la acción más común con un toque.

## Usuarios
- Técnico (`ATENDER_INCIDENCIA`): sus incidencias y repuestos.
- Jefe (`ASIGNAR_INCIDENCIA`): el estado del área.
- Personal de sistemas (ninguno de los dos permisos anteriores): lo que registró.

El perfil se decide por permisos (`perfil.puede(...)`), nunca comparando el perfil.

## Requisitos funcionales

### Cabecera (todos)
- RF-I1: EL SISTEMA muestra una `CabeceraHero` con el avatar del usuario en `acento`, su cargo y «Buenos días», «Buenas tardes» o «Buenas noches» con su primer nombre, según la hora del celular (antes de las 12, antes de las 19, o después).
- RF-I2: CUANDO el usuario toca su avatar, EL SISTEMA abre «Ajustes de seguridad».
- RF-I3: EL SISTEMA muestra dentro de la cabecera tres `TarjetaMetrica` según el perfil (RF-I4 a RF-I6). La que pide acción va en `acento` y la que avisa de algo demorado, en la variante crítica.

### Métricas por perfil
- RF-I4: Técnico: «Asignadas» (marca Asignada), «En atención» (En atención, en `acento`) y «Esperando repuesto». Cuenta solo las incidencias asignadas a él que no estén cerradas. Tocar cualquiera abre «Mis tareas».
- RF-I5: Jefe: «Sin asignar» (en `acento`), «En atención» y «Críticas» (pendientes hace más de 5 días o con la marca de crítica, la misma regla que el tablero de la HU-15; en la variante crítica si hay alguna). Cuenta todas las incidencias del área. Tocar cualquiera abre la lista de incidencias.
- RF-I6: Personal de sistemas: «Pendientes» (las que registró y no están cerradas, en `acento`), «En atención» (las que registró y alguien está atendiendo) y «Solucionadas» (las que registró y están cerradas). Tocar cualquiera abre la lista.

### Contenido debajo de la cabecera
- RF-I7: SI el técnico tiene incidencias en atención, ENTONCES EL SISTEMA muestra «Atendiendo ahora» con la más antigua en atención, en una `Tarjeta`: código y equipo, descripción, usuario responsable y área (si la hay), su `EtiquetaIncidencia`, y el botón de texto «Continuar», que abre su detalle.
- RF-I8: SI el técnico tiene solicitudes de repuesto sin entregar, ENTONCES EL SISTEMA muestra una tarjeta con la pieza de la más próxima (la que tiene llegada estimada más cercana; si ninguna la tiene, la más antigua) y una línea con su código de incidencia y estado («INC-0116, en camino»). Tocarla abre «Estado de repuestos».
- RF-I9: EL SISTEMA muestra los accesos rápidos que el perfil puede usar y que ya funcionan (decisión I-2): «Registrar» abre «Nueva incidencia» y «Escanear» abre «Nueva incidencia» con la cámara de «Escanear etiqueta» encima, para que el código leído llegue al formulario. Los dos requieren `REGISTRAR_INCIDENCIA`.
- RF-I10: Sin incidencias que contar, las métricas muestran 0 y no aparecen «Atendiendo ahora» ni la tarjeta de repuesto.

### Datos y sin conexión
- RF-I11: EL SISTEMA lee todo de Room (las incidencias y las solicitudes ya sincronizadas o pendientes de subir), así que Inicio funciona sin conexión con lo guardado y se actualiza solo cuando cambian los datos.
- RF-I12: Sin sesión, EL SISTEMA no muestra métricas (la navegación ya lleva al login).

## Requisitos no funcionales
- Diseño: prototipo 02 y `DESIGN.md`: `CabeceraHero`, `TarjetaMetrica`, `Tarjeta`, `Avatar`, `EtiquetaIncidencia`, vistas previas en claro y oscuro de los tres perfiles, y textos con las reglas de redacción (sin «·»).
- Arquitectura: el cálculo de las métricas y de lo que se destaca es una función pura en `feature/inicio/domain` (probada en la JVM). El caso de uso combina los repositorios que ya existen (`IncidenciaRepository` y `AtencionRepository`); no se agregan tablas ni consultas a Room.
- Accesibilidad: cada métrica es un solo elemento para TalkBack («3 Asignadas») y se puede tocar con 48 dp o más.

## Fuera de alcance
- La campana de avisos, «Asistente» y «Fallas» del prototipo (decisión I-2): se agregan con sus HU (17, 14 y 13).
- El tiempo desde que empezó la atención («hace 2 h 10 min») y el paso «Diagnóstico» del prototipo: el modelo no guarda subpasos de la atención.
- Filtrar la lista al tocar una métrica del jefe: la ruta de la lista no recibe filtros hoy.

## Decisiones
- I-1 · Inicio del jefe y del personal de sistemas. **Aprobada el 2026-11-08:** la misma cabecera con métricas propias (RF-I5, RF-I6) y los accesos que permita su perfil.
- I-2 · Accesos que dependen de HU sin hacer. **Aprobada el 2026-11-08:** solo se muestra lo que funciona.
- I-3 · Bloques de IA de los prototipos. **Aprobada el 2026-11-08:** se omiten hasta la HU-14.

## Criterios de finalización
- RF-I1 a RF-I12 implementados.
- Pruebas JVM: el cálculo del resumen para los tres perfiles (conteos, la más antigua en atención, el repuesto más próximo, sin datos) y el saludo según la hora. El `InicioViewModel`, con dobles y `MainDispatcherRule`.
- CI en verde y lint igual que la línea base.
- Prueba en el celular con la cuenta de un técnico y la del jefe, en claro y oscuro.
