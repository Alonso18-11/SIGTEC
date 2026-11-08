# Spec 012 — Directorio de colaboradores al registrar (HU-12)

> Estado: completa (2026-11-08), RF-1201 a RF-1218, en #121 a #124. Probada por Alonso en un celular real (Xiaomi, Android 14) el 2026-11-08: completar desde el directorio con área y anexo, guardar y ver «nombre · área» en el detalle, la sugerencia con «Usar», el cambio de código y el modo avión escribiendo a mano. `IncidenciaDaoTest` pasó en el celular. Ver el área desde otro celular no se probó por separado (la sincronización de `areaResponsable` está en `MapeoFirestore`). Aprobada el 2026-11-08, con las decisiones D-1 a D-6 respondidas por Alonso (ver «Decisiones»).

> Spec escrita antes del código (principio 2 de la constitución). Los requisitos usan el bloque propio de la HU-12 (RF-1201…). Sale de la sección 14 del documento del producto (`SIGTEC.docx.pdf`, fuera del repo): la «API del directorio de colaboradores», consumida con Retrofit y corrutinas, devuelve «nombre, área y anexo del trabajador responsable de un equipo» y «autocompleta los datos del usuario al registrar la incidencia, evitando errores de tipeo». Mientras la empresa no habilite el servicio real, se trabaja contra un servidor de prueba con la misma estructura. Sigue el prototipo 3 «Nueva incidencia» («Usuario responsable · completado desde el directorio», con «Contabilidad · Anexo 2314» debajo) y el detalle del prototipo 4 («María Quispe Huamán · Contabilidad»). Cubre el requisito 6 del curso, «Corrutinas y Retrofit para APIs externas», junto con la HU-11.

## Contexto y objetivo
Hoy, al registrar una incidencia, el usuario responsable es texto libre (HU-03). Cada registrador escribe el nombre a su manera, con tildes o sin ellas, completo o abreviado, y a veces se equivoca de persona. Eso ensucia el historial del equipo y obliga al técnico a averiguar a quién buscar y en qué oficina.

Con esta HU, al escribir o escanear el código del equipo, la app consulta el directorio de colaboradores de la empresa y completa sola el nombre del responsable, con su área y su anexo. El registrador solo confirma o corrige. Si el directorio no responde, el formulario funciona igual que hoy.

## Usuarios / actores
- Registrador: quien tiene `REGISTRAR_INCIDENCIA` (jefe, técnicos y personal de sistemas, según `Perfil.kt`). Es quien usa «Nueva incidencia».
- Todos los perfiles: ven el área del responsable en el detalle de la incidencia.
- Directorio de colaboradores (actor externo): responde nombre, área y anexo del responsable de un equipo. Mientras la empresa no habilite su API real, lo reemplaza el recurso `colaboradores` de un proyecto de MockAPI.io aparte del de Logística (D-1).

## Historias de usuario
- HU-12: Como registrador, quiero que los datos del usuario responsable se completen solos, para evitar errores de tipeo.
- Criterio de aceptación de la planificación (`SIGTEC_Planificación (1).xlsx`): al ingresar el código del equipo, la app consulta el directorio de colaboradores y completa el nombre, el área y el anexo del responsable.

## Requisitos funcionales (criterios de aceptación en EARS)

### Consultar el directorio
- RF-1201: CUANDO el registrador deja de escribir el código del equipo durante 0,6 segundos y ese código existe en el catálogo de equipos del celular (el mismo que valida RF-41 al guardar), EL SISTEMA consulta al directorio el responsable de ese equipo. Con códigos incompletos o que no están en el catálogo no consulta nada (D-5).
- RF-1202: CUANDO el registrador confirma un código en «Escanear etiqueta» (HU-09), EL SISTEMA consulta el directorio en ese momento, sin esperar los 0,6 segundos.
- RF-1203: MIENTRAS la consulta está en curso, EL SISTEMA muestra «Buscando en el directorio…» con un indicador de progreso bajo el campo «Usuario responsable», y deja escribir en todos los campos; la pantalla nunca se congela (sección 14 del producto).
- RF-1204: SI el código cambia mientras hay una consulta en curso, ENTONCES EL SISTEMA la cancela y solo cuenta la del último código.
- RF-1205: EL SISTEMA compara el código sin distinguir mayúsculas ni espacios en los extremos, y solo acepta un colaborador cuyo equipo sea exactamente ese código (el servidor de prueba busca por coincidencia parcial).

### Completar el responsable
- RF-1206: CUANDO el directorio devuelve el responsable y el campo «Usuario responsable» está vacío o todavía tiene lo que completó el directorio, EL SISTEMA escribe el nombre en el campo, cambia su texto de ayuda a «Completado desde el directorio» y muestra debajo «{área}, anexo {anexo}» (prototipo 3). Sin anexo en el directorio, muestra solo el área.
- RF-1207: SI el registrador ya escribió un responsable a mano, ENTONCES EL SISTEMA no lo reemplaza: muestra «El directorio indica: {nombre}, {área}» con el botón «Usar», que hace lo mismo que RF-1206 (D-6).
- RF-1208: CUANDO el registrador edita el nombre que completó el directorio, EL SISTEMA lo trata como escrito a mano: vuelve al texto de ayuda «Quien usa el equipo» y quita el área y el anexo (D-4). Pueden volver con «Usar» si el directorio ya respondió.
- RF-1209: CUANDO el registrador cambia el código del equipo y el responsable lo había completado el directorio sin editarlo, EL SISTEMA vacía el campo, quita el área y el anexo, y vuelve a consultar con RF-1201. Lo escrito a mano no se toca.
- RF-1210: EL SISTEMA no deja editar el área ni el anexo: solo los trae el directorio.

### Sin directorio (D-3)
- RF-1211: SI el directorio no tiene responsable para ese equipo, ENTONCES EL SISTEMA muestra «El directorio no tiene un responsable para este equipo. Escríbelo a mano.» y deja el campo libre.
- RF-1212: SI la consulta falla por falta de conexión, porque el directorio no responde en 10 segundos o porque la respuesta no se entiende, ENTONCES EL SISTEMA muestra «No se pudo consultar el directorio. Escribe el responsable a mano.» con «Reintentar», y deja el campo libre. La respuesta ilegible se registra en Logcat.
- RF-1213: El directorio nunca impide guardar: con o sin respuesta, «Guardar incidencia» sigue la regla de HU-03 (código, descripción y responsable no vacíos, y registrador con sesión). Registrar sin conexión funciona igual que hoy.

### Guardar y mostrar
- RF-1214: CUANDO el registrador guarda, EL SISTEMA guarda en la incidencia el nombre del responsable (como hoy) y, si vino del directorio sin editarse, también su área (`areaResponsable`); si se escribió a mano, el área queda vacía. El anexo no se guarda (D-2).
- RF-1215: EL SISTEMA sube `areaResponsable` a Firestore con la incidencia y la baja en los demás celulares (`core/sync`). Una incidencia sin ese campo en Firestore (registrada antes de esta HU o a mano) se lee con el área vacía.
- RF-1216: EL SISTEMA muestra en el detalle de la incidencia «Usuario responsable: {nombre}, {área}» cuando hay área, y solo el nombre cuando no la hay (prototipo 4).
- RF-1217: EL SISTEMA no cambia nada más de la incidencia por el directorio: código, descripción, fecha, registrador, marca y movimiento `REGISTRADA` siguen como en la HU-03.
- RF-1218: El área guardada no cambia si después el directorio cambia: es el dato del momento del registro.

## Requisitos no funcionales
- Idioma: textos de UI y mensajes en español.
- Fluidez: toda llamada al directorio es una corrutina fuera del hilo principal, con 10 segundos de límite; el formulario sigue respondiendo (sección 14 del producto).
- Sin conexión: registrar funciona sin conexión; solo falta el autocompletado (RF-1212).
- Configuración: la dirección del directorio se declara en un solo lugar (`DirectorioModule`); no es secreta, pero no va repartida por el código. Pasar al servicio real es cambiar esa línea.
- Costo cero: MockAPI.io en su plan gratuito; sin dependencias nuevas (Retrofit y el convertidor ya están).
- Diseño: se mantiene el formulario actual de «Nueva incidencia» y su barra; el área y los avisos usan estilos de `MaterialTheme.typography` y roles de `MaterialTheme.colorScheme`, `Espaciado.*`, `BotonTexto` para «Usar» y «Reintentar», y vistas previas en claro y oscuro de cada estado (buscando, completado, sugerencia, sin responsable, sin respuesta).
- Pruebas: el directorio queda detrás de una interfaz con un doble de prueba; la espera de 0,6 segundos se prueba con el reloj virtual de las corrutinas.
- Persistencia: cambia `IncidenciaEntity` (campo `areaResponsable`, nullable); sube `version` de `SigtecDatabase` de 4 a 5 y se dice en el PR (la base usa migración destructiva solo en desarrollo, así que se pierde lo no sincronizado al actualizar).
- Firestore: la regla `create` de `incidencias` no limita los campos, así que **no cambia `firestore.rules`**.

## Casos límite
- El registrador escribe rápido o corrige el código: solo cuenta la última consulta (RF-1204).
- Código que no está en el catálogo del celular (por ejemplo, el catálogo todavía no bajó): no se consulta; al guardar, RF-41 rechaza el código como hoy.
- Dos colaboradores con el mismo equipo en el directorio: se toma el primero que coincide exactamente y se registra en Logcat.
- El directorio responde después de que el registrador escribió el nombre a mano: no se pisa; aparece la sugerencia con «Usar» (RF-1207).
- El registrador escanea, el directorio completa, y luego cambia el código a mano: el responsable completado se vacía y se vuelve a consultar (RF-1209).
- Guardar mientras la consulta está en curso: se permite si el responsable ya está escrito; se guarda sin área.
- Respuesta con nombre vacío: se trata como «no tiene un responsable» (RF-1211).
- Incidencias antiguas o bajadas de otro celular sin `areaResponsable`: el detalle muestra solo el nombre (RF-1215, RF-1216).
- Cerrar sesión con incidencias sin subir: el aviso de RF-13 (HU-01) ya las cuenta; no cambia.

## Fuera de alcance
- Buscar personas por nombre en el directorio (un selector de colaboradores): la HU pide completar a partir del código del equipo.
- Guardar o mostrar el anexo fuera del formulario (D-2).
- Mantener el directorio o los equipos desde la app: solo se consulta.
- Cambiar el catálogo `equipos` de Firestore o su campo `responsableNombre`.
- La sugerencia automática de tipo y prioridad del prototipo 3: es de la HU-14.
- El historial del equipo («Contabilidad · responsable …» del prototipo 18): es de otra HU.
- Mover Retrofit a `core/di` para compartirlo con Logística (ver P-3 del plan).

## Criterios de finalización
- RF-1201 a RF-1218 implementados.
- Pruebas unitarias (JVM): el mapeo del directorio (coincidencia exacta, sin coincidencias, 404, nombre vacío, formato inválido); el repositorio con su doble; `RegistrarIncidencia` con área (guardada sin espacios, vacía → `null`, escrita a mano sin área); `RegistroIncidenciaViewModel` (espera de 0,6 s, código fuera del catálogo, escaneo inmediato, cancelación, completar, no pisar lo escrito a mano y «Usar», editar quita el área, cambiar el código vacía lo completado, sin responsable, sin respuesta y «Reintentar»), con dobles y `MainDispatcherRule`.
- `IncidenciaDaoTest` pasa en un celular con el campo nuevo y `version` subió a 5.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas de lint.
- Prueba manual en un celular real, con capturas en claro y oscuro:
  - Escribir `PC-CONT-014` y ver el nombre, el área y el anexo; guardar y ver «nombre · área» en el detalle y en Firestore.
  - Escanear la etiqueta de otro equipo y ver que se completa al confirmar.
  - Escribir un nombre a mano antes de que responda el directorio y usar «Usar».
  - En modo avión, ver el aviso de RF-1212, escribir a mano y guardar.
  - Ver el detalle de esa incidencia en otro celular (o con otra cuenta) con el área.
- La línea de la HU-12 en `MEMORY.md` dice «completa».

## Decisiones
- D-1 · Servidor de prueba del directorio (RF-1201, RF-1212). **Aprobada el 2026-11-08 y corregida ese mismo día:** un proyecto de MockAPI.io en otra cuenta gratuita, con el recurso `colaboradores` (nombre, área, anexo y el código del equipo que usa). La primera opción era el proyecto de la HU-11, pero al crear el recurso MockAPI avisó que el plan gratuito permite 1 proyecto con 2 recursos, y ese proyecto ya usa `piezas` y `solicitudes`. La cuenta y el proyecto los crea Alonso; el recurso y sus datos, él o el asistente con su visto bueno. Alternativas descartadas: pagar MockAPI (5 USD al mes, contra el costo cero) y un JSON por equipo en GitHub Pages.
- D-2 · Qué se guarda (RF-1214 a RF-1216). **Aprobada el 2026-11-08:** el nombre (como hoy) y el área en la incidencia; el anexo solo se ve en el formulario. Cambia `IncidenciaEntity` y el mapeo a Firestore; no cambia `firestore.rules`. Alternativas descartadas: guardar también el anexo, o solo el nombre.
- D-3 · Sin directorio (RF-1211 a RF-1213). **Aprobada el 2026-11-08:** aviso y campo libre para escribir a mano; se puede guardar igual. Alternativa descartada: usar antes el `responsableNombre` del catálogo `equipos` de Room.
- D-4 · Editar lo completado (RF-1208). **Aprobada el 2026-11-08:** el campo sigue editable, por si otra persona usa el equipo; al editarlo deja de decir «Completado desde el directorio» y se quita el área. Alternativa descartada: bloquear el campo.
- D-5 · Cuándo consultar (RF-1201). **Aprobada el 2026-11-08:** solo cuando el código existe en el catálogo de equipos del celular y tras 0,6 segundos sin escribir (la misma espera de la HU-11), para no consultar con códigos a medio escribir. Alternativa: un botón «Buscar responsable» junto al campo.
- D-6 · Lo escrito a mano antes de la respuesta (RF-1207). **Aprobada el 2026-11-08:** no se pisa; se ofrece «Usar». Alternativa: reemplazarlo siempre con lo del directorio.

## Dudas abiertas
- Ninguna. (El límite del plan gratuito de MockAPI se resolvió con D-1.)
