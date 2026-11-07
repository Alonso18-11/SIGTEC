# Tareas 010 — Dictado por voz y linterna (HU-10)

> Salen de `spec.md` y `plan.md`. Una tarea es un commit (20 a 30 minutos como máximo) con su push inmediato.
> Van agrupadas por PR (plan, sección 1) y en el orden de la tabla.
> **Al empezar cada PR:** rama desde su «Base» al día (`git pull origin main` o la rama base), `git merge` de las
> ramas de «Trae» que sigan abiertas, primer commit y push, y PR en borrador con la plantilla llena (HU-10, criterio
> 10 «Recursos», archivos compartidos, qué trae de otros PR).
> **Al terminar cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas,
> el CI en verde, «Cómo probarlo» escrito y «Ready for review».
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` y `app/src/test/java/com/softcorp/sigtec/`.
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

## PR-01 — unir el texto dictado (base `main`)
- [ ] **T01 · `CampoDictado` y `unirTexto`** en `feature/captura/domain/TextoDictado.kt` (plan 3.1). RF-1001, RF-1008, RF-1011.
  Hecho cuando: compila.
- [ ] **T02 · `TextoDictadoTest`**: campo vacío, con texto, con espacio o salto al final, dictado vacío y con espacios. RF-1008, RF-1011.
  Hecho cuando: `./gradlew testDebugUnitTest --tests "*TextoDictadoTest"` pasa.

## PR-02 — permiso genérico (base `main`) — D-7
- [ ] **T03 · `PermisoRecurso.kt`** con `VistaPermiso` de nombres neutros y `vistaPermiso(tieneRecurso, …)` 🔶 (plan 3.2). RF-1023.
  Hecho cuando: compila y `PermisoRecursoTest` (los casos de `PermisoCamaraTest`, renombrados) pasa.
- [ ] **T04 · `ExplicacionPermiso`** recibe ícono, títulos, textos y el rótulo del botón 🔶. RF-1019, RF-1021, RF-1022.
  Hecho cuando: las vistas previas de la cámara se ven igual que antes.
- [ ] **T05 · `ConPermiso(permiso, tieneRecurso, textos, contenido)`** y `ConPermisoCamara` como envoltorio con los textos de siempre 🔶. RF-1019 a RF-1023.
  Hecho cuando: en el celular, «Escanear etiqueta» pide y explica el permiso de cámara igual que en la HU-09.

## PR-03 — manifest (base `main`) — D-1
- [ ] **T06 · `RECORD_AUDIO`, `uses-feature android.hardware.microphone` (`required="false"`) y `<queries>` de `android.speech.RecognitionService` ⚠️.** RF-1017, RF-1019.
  Hecho cuando: compila y `lintDebug` no da advertencias nuevas.

## PR-04 — reconocedor de voz (base PR-01)
- [ ] **T07 · Contrato `ReconocedorVoz`, `EventoDictado` y `ErrorDictado`** en `feature/captura/domain/ReconocedorVoz.kt` (plan 4.1). RF-1004 a RF-1007, RF-1015 a RF-1018.
  Hecho cuando: compila.
- [ ] **T08 · `errorDictadoDe(codigo)`** en `feature/captura/data/ErroresReconocedor.kt` (plan 3.3). RF-1015 a RF-1018.
  Hecho cuando: compila.
- [ ] **T09 · `ErroresReconocedorTest`**: cada código y uno desconocido. RF-1015 a RF-1018.
  Hecho cuando: la prueba pasa.
- [ ] **T10 · `ReconocedorVozAndroid`** con `callbackFlow`, `es-PE`, parciales y preferencia sin conexión (plan 4.2, P-1 a P-3), y su `@Binds` en `CapturaModule`. RF-1004 a RF-1007, RF-1013.
  Hecho cuando: compila.
- [ ] **T11 · `ReconocedorVozFalso`** en `app/src/test/.../feature/captura/data/`. RF: —
  Hecho cuando: compila en las pruebas.

## PR-05 — DictadoViewModel (base PR-04)
- [ ] **T12 · `DictadoViewModel`: estado, argumentos y escuchar** (plan 4.3). RF-1004, RF-1005.
  Hecho cuando: compila.
- [ ] **T13 · Detener, seguir dictando, dejar de ver y usar el texto.** RF-1007 a RF-1010, RF-1012, RF-1014.
  Hecho cuando: compila.
- [ ] **T14 · `DictadoViewModelTest`** con el reconocedor falso (plan, sección 7). RF-1004 a RF-1018.
  Hecho cuando: la prueba pasa.

## PR-06 — pantalla «Dictar» (base PR-05, trae PR-02)
- [ ] **T15 · `mensajeDictado(error)`** en `MensajesDictado.kt` y `MensajesDictadoTest`. RF-1015 a RF-1018.
  Hecho cuando: la prueba pasa.
- [ ] **T16 · `DictarScreen`** con el micrófono animado, el texto transcrito, «Detener» y «Usar este texto» (plan 5.1). RF-1003 a RF-1005, RF-1007, RF-1009.
  Hecho cuando: hay vistas previas en claro y oscuro de escuchando, detenido con texto, error y sin servicio.
- [ ] **T17 · `DictarRoute`** con `ConPermiso` del micrófono y `ON_STOP`. RF-1014, RF-1017, RF-1019 a RF-1022.
  Hecho cuando: hay vistas previas de la explicación del permiso de micrófono.

## PR-07 — navegación (base PR-06, trae PR-03) — D-9
- [ ] **T18 · `DictarTexto(campo, subtitulo)` en `Rutas.kt` ⚠️** y la llamada del registro con los argumentos nuevos (P-4). RF-1003.
  Hecho cuando: compila.
- [ ] **T19 · `CapturaNavigation`** reemplaza la `PantallaEnConstruccion` y define `claveDictado(campo)` (plan 5.2). RF-1010, RF-1012.
  Hecho cuando: en el celular, el micrófono del registro abre «Dictar» y dicta.

## PR-08 — el registro recibe el texto (base PR-07)
- [ ] **T20 · `alRecibirDictado`** en `RegistroIncidenciaViewModel` con `unirTexto`. RF-1002, RF-1011.
  Hecho cuando: compila.
- [ ] **T21 · Casos nuevos de `RegistroIncidenciaViewModelTest`**: vacía, con texto, guardando. RF-1002, RF-1011.
  Hecho cuando: la prueba pasa.
- [ ] **T22 · La Route arma el subtítulo y la navegación entrega el texto** (plan 5.2). RF-1003, RF-1011.
  Hecho cuando: en el celular, lo dictado aparece al final de la descripción y se puede corregir antes de guardar.

## PR-09 — el informe recibe el texto (base PR-07)
- [ ] **T23 · `alRecibirDictado(campo, texto)`** en `InformeTecnicoViewModel` 🔶. RF-1002, RF-1011.
  Hecho cuando: compila.
- [ ] **T24 · Casos nuevos de `InformeTecnicoViewModelTest`**: cada campo, informe confirmado y guardando. RF-1002, RF-1011.
  Hecho cuando: la prueba pasa.
- [ ] **T25 · Micrófonos en «Diagnóstico», «Solución aplicada» y «Observaciones»** 🔶. RF-1001, RF-1002.
  Hecho cuando: las vistas previas del informe muestran los tres micrófonos en claro y oscuro.
- [ ] **T26 · La Route y `AtencionNavigation` abren «Dictar» y entregan el texto** 🔶 (plan 5.2). RF-1003, RF-1011.
  Hecho cuando: en el celular, cada campo recibe su texto y lo escrito en los demás sigue ahí.

## PR-10 — linterna (base `main`)
- [ ] **T27 · Contrato `Linterna`** en `feature/captura/domain/Linterna.kt` (plan 4.4). RF-1025, RF-1028, RF-1029.
  Hecho cuando: compila.
- [ ] **T28 · `LinternaAndroid`** con `CameraManager` y `registerTorchCallback`, y su `@Binds` (P-5). RF-1025, RF-1028, RF-1029.
  Hecho cuando: compila.
- [ ] **T29 · `BotonLinterna`** con sus vistas previas encendida y apagada (plan 5.3). RF-1025.
  Hecho cuando: hay vistas previas en claro y oscuro.
- [ ] **T30 · `LinternaFalsa`** en `app/src/test/.../feature/captura/data/`. RF: —
  Hecho cuando: compila en las pruebas.

## PR-11 — linterna en el detalle (base PR-10)
- [ ] **T31 · `DetalleIncidenciaViewModel`: visible, encendida, error, apagar al dejar de ver y al dejar de cumplir la condición** (plan 4.5). RF-1024 a RF-1029.
  Hecho cuando: compila.
- [ ] **T32 · Casos nuevos de `DetalleIncidenciaViewModelTest`** (plan, sección 7). RF-1024 a RF-1029.
  Hecho cuando: la prueba pasa.
- [ ] **T33 · Botón en la barra del detalle, aviso de error y `ON_STOP`.** RF-1024 a RF-1026, RF-1028.
  Hecho cuando: en el celular, el técnico asignado enciende y apaga la linterna, se apaga al salir y el jefe no ve el botón.

## PR-12 — linterna en las cámaras (base PR-10)
- [ ] **T34 · `LinternaCamara` y `VistaCamara` publica su flash** 🔶 (plan 5.4, P-6). RF-1030 a RF-1032.
  Hecho cuando: compila.
- [ ] **T35 · Linterna en «Escanear etiqueta».** RF-1030 a RF-1032.
  Hecho cuando: en el celular, se enciende, ayuda a escanear y se apaga al salir.
- [ ] **T36 · Linterna en «Evidencia fotográfica».** RF-1030 a RF-1032.
  Hecho cuando: en el celular, se toma una foto con la linterna y se apaga al salir.

## PR-13 — cierre (base: la rama del último PR fusionado)
- [ ] **T37 · Comprobar que nada quedó fuera**: cada archivo de las ramas de la HU coincide con `main`. RF: —
  Hecho cuando: ninguna rama tiene cambios sin fusionar.
- [ ] **T38 · Pruebas manuales de punta a punta** («Criterios de finalización» de la spec) en un celular real. RF-1001 a RF-1032.
  Hecho cuando: cada prueba está anotada con su resultado.
- [ ] **T39 · Spec «completa»**, tareas marcadas y HU-10 «completa» en `MEMORY.md`, después de `git pull origin main`. RF: —
  Hecho cuando: la spec describe el código y no queda ninguna tarea sin explicar.
