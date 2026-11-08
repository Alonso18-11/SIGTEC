# Tareas 011 — Solicitud de repuestos a Logística (HU-11)

> Salen de `spec.md` y `plan.md`. Una tarea es un commit (20 a 30 minutos como máximo) con su push inmediato.
> Van agrupadas por PR (plan, sección 1) y en el orden de la tabla.
> **Al empezar cada PR:** rama desde su «Base» al día, `git merge` de las ramas de «Trae» que sigan abiertas, PR con la
> plantilla llena (HU-11, criterios 6, 7 y 8, archivos compartidos, qué trae de otros PR).
> **Al terminar cada PR:** `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde y sin advertencias nuevas,
> el CI en verde, «Cómo probarlo» escrito y «Ready for review».
> Rutas relativas a `app/src/main/java/com/softcorp/sigtec/` y `app/src/test/java/com/softcorp/sigtec/`.
> ⚠️ = compartido de `CLAUDE.md`; 🔶 = sensible fuera de esa lista.

## Antes de PR-02 — servidor de prueba (lo hace el usuario)
- [ ] **T00 · Cuenta y proyecto en MockAPI.io** con los recursos `piezas` y `solicitudes` y unas 10 piezas de ejemplo
  (plan 5.1). RF: — Hecho cuando: `GET <base>/piezas?search=fuente` responde en el navegador y la dirección base está
  en el PR-02.

## PR-01 — modelo de la solicitud (base `main`)
- [ ] **T01 · Campos nuevos en `SolicitudRepuesto` y su entidad ⚠️** (plan 3.1), con `actualizadoEn`. RF-1113, RF-1117, RF-1119, RF-1122.
  Hecho cuando: compila.
- [ ] **T02 · Consultas nuevas de `SolicitudRepuestoDao` 🔶** y `version = 4` en `SigtecDatabase` ⚠️. RF-1117, RF-1121.
  Hecho cuando: compila.
- [ ] **T03 · `SolicitudRepuestoDaoTest` (instrumentada).** RF-1117, RF-1119, RF-1121.
  Hecho cuando: `./gradlew connectedDebugAndroidTest` pasa en un celular.

## PR-02 — API de Logística (base PR-01)
- [ ] **T04 · Contrato `LogisticaRepository` y `ErrorLogistica`.** RF-1104, RF-1117, RF-1123. Hecho cuando: compila.
- [ ] **T05 · `LogisticaApi` (Retrofit), DTOs y `LogisticaModule`** con 10 s y la dirección base (P-1, P-2). RF-1104, RF-1106, RF-1108.
  Hecho cuando: compila y el manifest combinado tiene `INTERNET`.
- [ ] **T06 · `MapeoLogistica` y `LogisticaRepositoryImpl`** (plan 3.3 y 4.4). RF-1105, RF-1107, RF-1108, RF-1119. Hecho cuando: compila.
- [ ] **T07 · `MapeoLogisticaTest`.** RF-1105 a RF-1108, RF-1119. Hecho cuando: la prueba pasa.
- [ ] **T08 · `LogisticaRepositoryFalso`.** RF: — Hecho cuando: compila en las pruebas.

## PR-03 — reglas (base PR-01)
- [ ] **T09 · `ReglasRepuesto.kt`** (plan 3.2). RF-1102, RF-1105, RF-1120, RF-1122, RF-1124. Hecho cuando: compila.
- [ ] **T10 · `ReglasRepuestoTest`.** RF-1102, RF-1120, RF-1122, RF-1124. Hecho cuando: la prueba pasa.

## PR-04 — solicitar (base PR-03)
- [ ] **T11 · `solicitarRepuesto` en el repositorio, en una transacción.** RF-1113, RF-1115. Hecho cuando: compila.
- [ ] **T12 · Caso de uso `SolicitarRepuesto`** (plan 4.1). RF-1102, RF-1103, RF-1110, RF-1113. Hecho cuando: compila.
- [ ] **T13 · `SolicitarRepuestoTest`** con el repositorio falso. RF-1102, RF-1103, RF-1110, RF-1113, RF-1115.
  Hecho cuando: la prueba pasa.

## PR-05 — ViewModel de la solicitud (base PR-04, trae PR-02)
- [ ] **T14 · Buscar la pieza mientras se escribe** (plan 4.2). RF-1104 a RF-1109. Hecho cuando: compila.
- [ ] **T15 · Enviar con la confirmación.** RF-1110, RF-1113 a RF-1116. Hecho cuando: compila.
- [ ] **T16 · `SolicitudRepuestoViewModelTest`.** RF-1104 a RF-1116. Hecho cuando: la prueba pasa.

## PR-06 — pantalla de la solicitud (base PR-05)
- [ ] **T17 · `SolicitudRepuestoScreen`** con vistas previas (plan 5.2). RF-1101, RF-1104 a RF-1110.
  Hecho cuando: hay vistas previas en claro y oscuro de buscando, con stock, sin stock, sin respuesta y enviando.
- [ ] **T18 · Navegación y aviso al volver al detalle.** RF-1111, RF-1112, RF-1114.
  Hecho cuando: en el celular se envía con huella y el detalle muestra «Solicitud registrada · Por enviar a Logística».

## PR-07 — registrar en Logística (base PR-04, trae PR-02)
- [ ] **T19 · `RegistrarSolicitudesEnLogistica`** (plan 4.3). RF-1117, RF-1119. Hecho cuando: compila.
- [ ] **T20 · `RegistrarSolicitudesEnLogisticaTest`.** RF-1117, RF-1119. Hecho cuando: la prueba pasa.
- [ ] **T21 · `RegistrarEnLogisticaWorker` y `ProgramadorLogistica`** (P-3). RF-1117.
  Hecho cuando: en el celular, una solicitud hecha en modo avión aparece en MockAPI al volver la conexión.

## PR-08 — sincronización (base PR-01)
- [ ] **T22 · Subir las solicitudes a Firestore 🔶.** RF-1118. Hecho cuando: compila.
- [ ] **T23 · Bajar las solicitudes de Firestore 🔶.** RF-1120. Hecho cuando: compila.
- [ ] **T24 · Regla de `solicitudesRepuesto` en `firestore.rules` ⚠️** (D-8). RF-1118.
  Hecho cuando: probada en el Rules Playground y publicada en la consola.

## PR-09 — sección «Repuestos» del detalle (base PR-01)
- [ ] **T25 · El detalle observa las solicitudes.** RF-1120. Hecho cuando: compila.
- [ ] **T26 · Sección «Repuestos»** con vistas previas. RF-1120. Hecho cuando: hay vistas previas en claro y oscuro.
- [ ] **T27 · Casos nuevos de `DetalleIncidenciaViewModelTest`.** RF-1120. Hecho cuando: la prueba pasa.

## PR-10 — actualizar estados (base PR-04, trae PR-02)
- [ ] **T28 · `aplicarEstado` en el repositorio, en una transacción.** RF-1124. Hecho cuando: compila.
- [ ] **T29 · `ActualizarEstadoRepuestos`** (plan 4.5). RF-1123, RF-1124. Hecho cuando: compila.
- [ ] **T30 · `ActualizarEstadoRepuestosTest`.** RF-1123, RF-1124. Hecho cuando: la prueba pasa.

## PR-11 — «Estado de repuestos» (base PR-10)
- [ ] **T31 · `EstadoRepuestosViewModel`** con filtros y consulta. RF-1121 a RF-1123. Hecho cuando: compila.
- [ ] **T32 · `EstadoRepuestosViewModelTest`.** RF-1121 a RF-1123. Hecho cuando: la prueba pasa.
- [ ] **T33 · `EstadoRepuestosScreen`** con vistas previas (plan 5.3). RF-1121, RF-1122.
  Hecho cuando: hay vistas previas en claro y oscuro de las tres etapas.
- [ ] **T34 · «Repuestos» en «Mis tareas» y en la navegación.** RF-1121.
  Hecho cuando: en el celular, cambiar el estado en MockAPI y deslizar actualiza la tarjeta y la incidencia vuelve a «En atención».

## PR-12 — cierre (base: la rama del último PR fusionado)
- [ ] **T35 · Comprobar que nada quedó fuera de `main`.** RF: — Hecho cuando: cada archivo de las ramas coincide con `main`.
- [ ] **T36 · Pruebas manuales de punta a punta** («Criterios de finalización» de la spec). RF-1101 a RF-1124.
  Hecho cuando: cada prueba está anotada con su resultado.
- [ ] **T37 · Spec «completa», tareas marcadas y HU-11 «completa» en `MEMORY.md`.** RF: —
  Hecho cuando: la spec describe el código.
